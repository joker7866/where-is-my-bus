package com.wheresmybus.app.ui.stopdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wheresmybus.app.data.Eta
import com.wheresmybus.app.notification.NotificationHelper
import com.wheresmybus.app.ui.BusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetailScreen(
    viewModel: BusViewModel,
    stopId: String,
    onBack: () -> Unit,
    onRouteClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val stop = remember(stopId) { viewModel.stopById(stopId) }
    val favorites by viewModel.favoriteStopIds.collectAsStateWithLifecycle()
    val isFavorite = stopId in favorites

    val etas by remember(stopId) { viewModel.etasForStop(stopId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Demo alert: once a favorited stop has a bus about to arrive, fire a local
    // notification for that route (once per route per visit to avoid spam).
    val alreadyAlerted = remember(stopId) { mutableStateOf(setOf<String>()) }
    LaunchedEffect(isFavorite, etas) {
        if (!isFavorite || stop == null) return@LaunchedEffect
        etas.filter { it.minutesAway <= 1 && it.routeId !in alreadyAlerted.value }.forEach { eta ->
            NotificationHelper.showArrivalAlert(
                context,
                title = "Route ${eta.routeShortName} is arriving",
                text = "Arriving at ${stop.name} in about ${eta.minutesAway} min",
            )
            alreadyAlerted.value = alreadyAlerted.value + eta.routeId
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stop?.name ?: "Stop") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite(stopId) }) {
                        Icon(
                            if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (etas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No buses currently serving this stop")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(etas) { eta -> EtaCard(eta) { onRouteClick(eta.routeId) } }
            }
        }
    }
}

@Composable
private fun EtaCard(eta: Eta, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        ListItem(
            leadingContent = {
                Box(
                    modifier = Modifier
                        .background(Color(eta.colorHex), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(eta.routeShortName, color = Color.White)
                }
            },
            headlineContent = { Text(eta.routeShortName.let { "Route $it" }) },
            trailingContent = {
                Row {
                    Text(
                        text = if (eta.minutesAway == 0) "Arriving" else "${eta.minutesAway} min",
                    )
                }
            },
        )
    }
}
