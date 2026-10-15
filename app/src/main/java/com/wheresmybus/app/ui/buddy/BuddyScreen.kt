package com.wheresmybus.app.ui.buddy

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wheresmybus.app.data.BusRoute
import com.wheresmybus.app.location.DeviceLocation
import com.wheresmybus.app.ui.BusViewModel
import com.wheresmybus.app.ui.strings.AppLanguage
import com.wheresmybus.app.ui.strings.stringsFor
import com.wheresmybus.app.ui.theme.BgCream
import com.wheresmybus.app.ui.theme.ChipMint
import com.wheresmybus.app.ui.theme.HeaderGreen
import com.wheresmybus.app.ui.theme.StatusLive

/** Formerly the "I'm on a bus" card on Home — now its own tab so sharing your live
 * location for a route has a permanent, dedicated place instead of a transient card. */
@Composable
fun BuddyScreen(viewModel: BusViewModel, language: AppLanguage) {
    val strings = stringsFor(language)
    val context = LocalContext.current
    val routes by viewModel.routes.collectAsStateWithLifecycle()

    var hasLocationPermission by remember { mutableStateOf(DeviceLocation.hasPermission(context)) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasLocationPermission = DeviceLocation.hasPermission(context)
    }
    fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        )
    }

    var sharingRouteId by remember { mutableStateOf<String?>(null) }
    var showWhichBusDialog by remember { mutableStateOf(false) }
    val sharingRoute = sharingRouteId?.let { viewModel.routeById(it) }

    Column(modifier = Modifier.fillMaxSize().background(BgCream)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderGreen)
                .padding(16.dp),
        ) {
            Text(strings.navBuddy, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings.buddySubtitle, color = Color.Gray, fontSize = 13.sp)

            ImOnABusCard(
                sharingRoute = sharingRoute,
                language = language,
                onClick = {
                    when {
                        sharingRouteId != null -> sharingRouteId = null
                        !hasLocationPermission -> requestLocationPermission()
                        else -> showWhichBusDialog = true
                    }
                },
            )
        }
    }

    if (showWhichBusDialog) {
        WhichBusDialog(
            routes = routes,
            language = language,
            onPick = { route ->
                sharingRouteId = route.id
                showWhichBusDialog = false
            },
            onDismiss = { showWhichBusDialog = false },
        )
    }
}

@Composable
private fun ImOnABusCard(sharingRoute: BusRoute?, language: AppLanguage, onClick: () -> Unit) {
    val strings = stringsFor(language)
    val isSharing = sharingRoute != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSharing) ChipMint else Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isSharing) StatusLive else ChipMint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = if (isSharing) Color.White else HeaderGreen,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = strings.imOnABus, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = if (sharingRoute != null) {
                    String.format(strings.sharingActiveForRouteFormat, sharingRoute.shortName)
                } else {
                    strings.shareLocationSubtitle
                },
                color = if (isSharing) StatusLive else Color.Gray,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun WhichBusDialog(
    routes: List<BusRoute>,
    language: AppLanguage,
    onPick: (BusRoute) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = stringsFor(language)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.whichBusTitle) },
        text = {
            Column {
                routes.forEach { route ->
                    ListItem(
                        modifier = Modifier.clickable { onPick(route) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ChipMint)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(route.shortName, color = HeaderGreen, fontWeight = FontWeight.Bold)
                            }
                        },
                        headlineContent = { Text(route.longName) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}
