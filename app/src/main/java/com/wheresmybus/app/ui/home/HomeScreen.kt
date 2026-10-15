package com.wheresmybus.app.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wheresmybus.app.data.Eta
import com.wheresmybus.app.data.LiveMode
import com.wheresmybus.app.data.Stop
import com.wheresmybus.app.data.SyncState
import com.wheresmybus.app.location.DeviceLocation
import com.wheresmybus.app.ui.BusViewModel
import com.wheresmybus.app.ui.strings.AppLanguage
import com.wheresmybus.app.ui.strings.localizedName
import com.wheresmybus.app.ui.strings.stringsFor
import com.wheresmybus.app.ui.theme.AccentYellow
import com.wheresmybus.app.ui.theme.BgCream
import com.wheresmybus.app.ui.theme.ChipMint
import com.wheresmybus.app.ui.theme.HeaderGreen
import com.wheresmybus.app.ui.theme.HeaderGreenDark
import com.wheresmybus.app.ui.theme.StatusLive
import com.wheresmybus.app.ui.theme.StatusRecent
import com.wheresmybus.app.ui.theme.StatusTimetable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf

/** Default stop shown before a real location fix arrives (or if permission was denied). */
private const val DEFAULT_STOP_ID = "GPM"

@Composable
fun HomeScreen(
    viewModel: BusViewModel,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onSearchClick: () -> Unit,
    onRouteClick: (String) -> Unit,
    onJourneySearchClick: (fromName: String) -> Unit,
) {
    val strings = stringsFor(language)
    val context = LocalContext.current
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    var hasLocationPermission by remember { mutableStateOf(DeviceLocation.hasPermission(context)) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasLocationPermission = DeviceLocation.hasPermission(context)
    }
    // Catches the case where the user grants location from system Settings and comes back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLocationPermission = DeviceLocation.hasPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        )
    }

    var nearest by remember {
        mutableStateOf(viewModel.stopById(DEFAULT_STOP_ID)?.let { it to 0 })
    }
    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@LaunchedEffect
        DeviceLocation.updates(context)
            .catch { /* no fix available; keep the default stop */ }
            .collect { location ->
                nearest = viewModel.nearestStop(location.latitude, location.longitude)
            }
    }
    val nearbyStop: Stop? = nearest?.first
    val nearbyDistanceMeters: Int = nearest?.second ?: 0

    val etas by remember(nearbyStop?.id) {
        nearbyStop?.let { viewModel.etasForStop(it.id) } ?: flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCream),
    ) {
        HeaderSection(
            language = language,
            onLanguageChange = onLanguageChange,
            fromLabel = nearbyStop?.localizedName(language) ?: "",
            onSearchClick = onSearchClick,
            syncState = syncState,
            onRefresh = { viewModel.refresh() },
            onJourneySearchClick = { onJourneySearchClick(nearbyStop?.name ?: "") },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!hasLocationPermission) {
                item {
                    EnableLocationBanner(language = language, onEnable = { requestLocationPermission() })
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = strings.busesNearYou,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeaderGreen,
                    )
                    if (nearbyStop != null) {
                        Text(
                            text = String.format(
                                strings.nearbyContextFormat,
                                nearbyStop.localizedName(language),
                                nearbyDistanceMeters,
                            ),
                            fontSize = 12.sp,
                            color = Color.Gray,
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    if (etas.isEmpty()) {
                        Text(
                            text = strings.noBusesServing,
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    Column {
                        etas.forEachIndexed { index, eta ->
                            BusEtaRow(
                                eta = eta,
                                destinationName = viewModel.stopById(eta.destinationStopId)
                                    ?.localizedName(language) ?: eta.routeShortName,
                                language = language,
                                onClick = { onRouteClick(eta.routeId) },
                            )
                            if (index != etas.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(
                                    color = Color(0xFFEFEFEF),
                                )
                            }
                        }
                    }
                }
            }

            item { LegendRow(language) }
        }
    }
}

@Composable
private fun HeaderSection(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    fromLabel: String,
    onSearchClick: () -> Unit,
    syncState: SyncState,
    onRefresh: () -> Unit,
    onJourneySearchClick: () -> Unit,
) {
    val strings = stringsFor(language)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGreen)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentYellow),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.DirectionsBus, contentDescription = null, tint = HeaderGreen)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = strings.appName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            LanguageToggle(language, onLanguageChange)
        }

        Spacer(modifier = Modifier.height(10.dp))
        SyncStatusRow(syncState = syncState, onRefresh = onRefresh)

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .clickable(onClick = onSearchClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = strings.searchHint, color = Color.Gray, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FromToPill(
                label = strings.from,
                value = fromLabel,
                modifier = Modifier.weight(1f).clickable(onClick = onJourneySearchClick),
            )
            FromToPill(
                label = strings.to,
                value = strings.whereTo,
                modifier = Modifier.weight(1f).clickable(onClick = onJourneySearchClick),
            )
        }
    }
}

@Composable
private fun SyncStatusRow(syncState: SyncState, onRefresh: () -> Unit) {
    val (label, color) = when (syncState) {
        SyncState.IDLE -> "Using bundled data" to Color(0xFFBFD8CE)
        SyncState.SYNCING -> "Syncing…" to Color(0xFFBFD8CE)
        SyncState.SYNCED -> "Synced with server" to Color(0xFF9BE8B8)
        SyncState.FAILED -> "Offline — using local data" to AccentYellow
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = color, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onRefresh)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(text = "Refresh", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
private fun FromToPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HeaderGreenDark)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text = label, color = Color(0xFFBFD8CE), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EnableLocationBanner(language: AppLanguage, onEnable: () -> Unit) {
    val strings = stringsFor(language)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFCEFD8))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.LocationOff, contentDescription = null, tint = Color(0xFF7A5200))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(strings.enableLocationTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(strings.enableLocationSubtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onEnable) {
            Text(strings.enableLocationButton)
        }
    }
}

@Composable
private fun BusEtaRow(
    eta: Eta,
    destinationName: String,
    language: AppLanguage,
    onClick: () -> Unit,
) {
    val strings = stringsFor(language)
    val statusColor = when (eta.liveMode) {
        LiveMode.LIVE -> StatusLive
        LiveMode.RECENT -> StatusRecent
        LiveMode.TIMETABLE -> StatusTimetable
    }
    val statusText = when (eta.liveMode) {
        LiveMode.LIVE -> String.format(strings.liveSharingFormat, eta.sharerCount)
        LiveMode.RECENT -> String.format(strings.seenMinAgo, eta.lastSeenMinutesAgo)
        LiveMode.TIMETABLE -> strings.timetable
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(ChipMint)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(text = eta.routeShortName, color = HeaderGreen, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = destinationName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = statusText, color = statusColor, fontSize = 13.sp)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = eta.minutesAway.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(text = strings.minutesSuffix, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun LegendRow(language: AppLanguage) {
    val strings = stringsFor(language)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LegendItem(StatusLive, strings.legendLive)
        LegendItem(StatusRecent, strings.legendRecent)
        LegendItem(StatusTimetable, strings.legendTimetable)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}
