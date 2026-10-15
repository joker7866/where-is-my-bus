package com.wheresmybus.app.ui.routedetail

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wheresmybus.app.data.LiveMode
import com.wheresmybus.app.data.RouteTimeline
import com.wheresmybus.app.data.RouteType
import com.wheresmybus.app.data.StopStatus
import com.wheresmybus.app.data.StopTimelineEntry
import com.wheresmybus.app.location.DeviceLocation
import com.wheresmybus.app.notification.NotificationHelper
import com.wheresmybus.app.ui.BusViewModel
import com.wheresmybus.app.ui.strings.AppLanguage
import com.wheresmybus.app.ui.strings.UiStrings
import com.wheresmybus.app.ui.strings.localizedName
import com.wheresmybus.app.ui.strings.stringsFor
import com.wheresmybus.app.ui.theme.AccentYellow
import com.wheresmybus.app.ui.theme.BgCream
import com.wheresmybus.app.ui.theme.HeaderGreen
import com.wheresmybus.app.ui.theme.HeaderGreenDark
import com.wheresmybus.app.ui.theme.StatusLive
import com.wheresmybus.app.ui.theme.StatusRecent
import com.wheresmybus.app.ui.theme.StatusTimetable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed class TimelineRow {
    data class Stop(val entry: StopTimelineEntry, val isLast: Boolean) : TimelineRow()
    data class BusHere(val distanceMeters: Int, val nextStopName: String) : TimelineRow()
}

@Composable
fun RouteDetailScreen(
    viewModel: BusViewModel,
    routeId: String,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit,
) {
    val strings = stringsFor(language)
    val context = LocalContext.current
    val timeline by remember(routeId) { viewModel.timelineForRoute(routeId) }
        .collectAsStateWithLifecycle(initialValue = null)

    var alertEnabled by remember { mutableStateOf(false) }
    var imOnIt by remember { mutableStateOf(false) }
    var alertedForStopId by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* if denied, the alarm stays armed but silently won't notify */ }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.any { it }) imOnIt = true
    }

    fun onImOnItToggle() {
        if (imOnIt) {
            imOnIt = false
            return
        }
        if (DeviceLocation.hasPermission(context)) {
            imOnIt = true
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    fun onAlertToggle() {
        val turningOn = !alertEnabled
        alertEnabled = turningOn
        if (turningOn) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            Toast.makeText(context, strings.alarmSetToast, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, strings.alarmOffToast, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(alertEnabled, timeline) {
        val current = timeline ?: return@LaunchedEffect
        val next = current.entries.firstOrNull { it.status == StopStatus.NEXT } ?: return@LaunchedEffect
        if (alertEnabled && next.minutesAway <= 1 && alertedForStopId != next.stop.id) {
            NotificationHelper.showArrivalAlert(
                context,
                title = "${current.route.shortName} is arriving",
                text = "Arriving at ${next.stop.localizedName(language)} in about ${next.minutesAway} min",
            )
            alertedForStopId = next.stop.id
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgCream)) {
        if (timeline == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.noBusesServing)
            }
            return@Column
        }
        val current = timeline!!

        RouteHeader(current, language, onLanguageChange, onBack)
        ColumnHeaderRow(strings.schedHeader, strings.expectedHeader)

        val rows = buildTimelineRows(current)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(rows) { row ->
                when (row) {
                    is TimelineRow.Stop -> StopRowView(row.entry, isLast = row.isLast, language)
                    is TimelineRow.BusHere -> BusHereRowView(row.distanceMeters, row.nextStopName, language)
                }
            }
        }

        ActionBar(
            alertEnabled = alertEnabled,
            onAlertToggle = { onAlertToggle() },
            imOnIt = imOnIt,
            onImOnItToggle = { onImOnItToggle() },
            onShare = {
                val message = String.format(
                    strings.shareMessageFormat,
                    current.route.shortName,
                    current.route.longName,
                )
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
            },
            language = language,
        )
    }
}

private fun buildTimelineRows(timeline: RouteTimeline): List<TimelineRow> {
    val rows = mutableListOf<TimelineRow>()
    val nextIndex = timeline.entries.indexOfFirst { it.status == StopStatus.NEXT }
    val lastIndex = timeline.entries.lastIndex
    timeline.entries.forEachIndexed { index, entry ->
        if (timeline.busInTransit && index == nextIndex && nextIndex > 0) {
            rows += TimelineRow.BusHere(timeline.distanceToNextStopMeters, entry.stop.name)
        }
        rows += TimelineRow.Stop(entry, isLast = index == lastIndex)
    }
    return rows
}

private fun formatKm(km: Double): String =
    if (km == km.toInt().toDouble()) km.toInt().toString() else "%.1f".format(km)

private fun routeSubtitle(
    routeType: RouteType,
    stopCount: Int,
    distanceKm: Double,
    viaTowns: String,
    strings: UiStrings,
): String {
    if (routeType != RouteType.LONG_DISTANCE) {
        return String.format(strings.townBusStopsFormat, stopCount)
    }
    return buildString {
        append(strings.longDistanceLabel)
        if (distanceKm > 0) append(" · ${formatKm(distanceKm)} ${strings.kmSuffix}")
        if (viaTowns.isNotBlank()) append(" · $viaTowns")
    }
}

@Composable
private fun RouteHeader(
    timeline: RouteTimeline,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit,
) {
    val strings = stringsFor(language)
    val route = timeline.route
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HeaderGreenDark)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            LanguageToggle(language, onLanguageChange)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentYellow)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(route.shortName, color = HeaderGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(route.longName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (route.busName.isNotBlank()) {
                    Text(route.busName, color = Color(0xFFE3EFE9), fontSize = 13.sp, fontStyle = FontStyle.Italic)
                }
                Text(
                    text = routeSubtitle(route.routeType, route.stopIds.size, route.distanceKm, route.viaTowns, strings),
                    color = Color(0xFFBFD8CE),
                    fontSize = 13.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (route.liveMode == LiveMode.LIVE) {
                StatusPill(
                    text = strings.liveLabel,
                    dotColor = StatusLive,
                    containerColor = Color.White,
                    textColor = HeaderGreen,
                )
                Spacer(modifier = Modifier.width(8.dp))
                val delayText = if (route.delayMinutes > 0) {
                    String.format(strings.delayLateFormat, route.delayMinutes)
                } else {
                    strings.onTime
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (route.delayMinutes > 0) Color(0xFFF4E3C1) else Color(0xFFD7EEDF))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(delayText, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF7A5200))
                }
            }
        }

        if (route.liveMode == LiveMode.LIVE) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format(
                    strings.updatedSecAgoFormat,
                    timeline.lastUpdatedSecondsAgo,
                    route.sharerCount,
                ),
                color = Color(0xFFBFD8CE),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun LanguageToggle(language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(HeaderGreenDark)
            .padding(3.dp),
    ) {
        AppLanguage.entries.forEach { entry ->
            val selected = entry == language
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onLanguageChange(entry) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = entry.label,
                    color = if (selected) HeaderGreen else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, dotColor: Color, containerColor: Color, textColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

@Composable
private fun ColumnHeaderRow(schedLabel: String, expectedLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEDEBE4))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Text(schedLabel, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(70.dp))
        Text(expectedLabel, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(70.dp))
    }
}

private val timeFormatter = SimpleDateFormat("h:mm", Locale.getDefault())

@Composable
private fun StopRowView(entry: StopTimelineEntry, isLast: Boolean, language: AppLanguage) {
    val strings = stringsFor(language)
    val (dotColor, lineColor) = when (entry.status) {
        StopStatus.PASSED -> StatusLive to StatusLive
        StopStatus.NEXT -> StatusLive to StatusTimetable
        StopStatus.UPCOMING -> StatusTimetable to StatusTimetable
    }
    val subText = when (entry.status) {
        StopStatus.PASSED -> strings.passed
        StopStatus.NEXT -> String.format(strings.nextStopInFormat, entry.minutesAway)
        StopStatus.UPCOMING -> String.format(strings.inMinFormat, entry.minutesAway)
    }
    val subColor = when (entry.status) {
        StopStatus.PASSED -> Color.Gray
        StopStatus.NEXT -> StatusLive
        StopStatus.UPCOMING -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp),
    ) {
        TimelineRail(dotColor = dotColor, lineColor = lineColor, showLine = !isLast, filled = entry.status != StopStatus.UPCOMING)

        Column(modifier = Modifier.weight(1f).padding(vertical = 14.dp)) {
            Text(entry.stop.localizedName(language), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subText, fontSize = 13.sp, color = subColor)
        }

        Text(
            text = timeFormatter.format(Date(entry.scheduledEpochMillis)),
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.width(70.dp).padding(top = 14.dp),
        )
        Text(
            text = timeFormatter.format(Date(entry.expectedEpochMillis)),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (entry.expectedEpochMillis > entry.scheduledEpochMillis) StatusRecent else Color.Black,
            modifier = Modifier.width(70.dp).padding(top = 14.dp),
        )
    }
}

@Composable
private fun BusHereRowView(distanceMeters: Int, nextStopName: String, language: AppLanguage) {
    val strings = stringsFor(language)
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AccentYellow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.DirectionsBus, contentDescription = null, tint = HeaderGreen)
            }
        }
        Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)) {
            Text(strings.busIsHere, fontWeight = FontWeight.Bold, color = Color(0xFF7A5200), fontSize = 14.sp)
            Text(
                String.format(strings.distanceToStopFormat, distanceMeters, nextStopName),
                fontSize = 12.sp,
                color = Color.Gray,
            )
        }
    }
}

@Composable
private fun TimelineRail(dotColor: Color, lineColor: Color, showLine: Boolean, filled: Boolean) {
    Box(
        modifier = Modifier
            .width(32.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (showLine) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .padding(top = 20.dp)
                    .background(lineColor),
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(if (filled) dotColor else Color.White),
        )
    }
}

@Composable
private fun ActionBar(
    alertEnabled: Boolean,
    onAlertToggle: () -> Unit,
    imOnIt: Boolean,
    onImOnItToggle: () -> Unit,
    onShare: () -> Unit,
    language: AppLanguage,
) {
    val strings = stringsFor(language)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onAlertToggle, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.alertMe, fontSize = 13.sp)
        }
        OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.share, fontSize = 13.sp)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (imOnIt) StatusLive else HeaderGreen)
                .clickable(onClick = onImOnItToggle),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.imOnIt, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}
