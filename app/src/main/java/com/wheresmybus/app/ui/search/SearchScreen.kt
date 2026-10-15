package com.wheresmybus.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wheresmybus.app.data.BusRoute
import com.wheresmybus.app.data.Stop
import com.wheresmybus.app.ui.BusViewModel

@Composable
fun SearchScreen(
    viewModel: BusViewModel,
    onStopClick: (String) -> Unit,
    onRouteClick: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val allRoutes by viewModel.routes.collectAsStateWithLifecycle()
    val allStops by viewModel.stops.collectAsStateWithLifecycle()

    val matchingRoutes = remember(query, allRoutes) {
        if (query.isBlank()) emptyList()
        else allRoutes.filter {
            it.shortName.contains(query, ignoreCase = true) ||
                it.longName.contains(query, ignoreCase = true)
        }
    }
    val matchingStops = remember(query, allStops) {
        allStops.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search routes or stops") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (matchingRoutes.isNotEmpty()) {
                items(matchingRoutes) { route -> RouteRow(route) { onRouteClick(route.id) } }
            }
            items(matchingStops) { stop -> StopRow(stop) { onStopClick(stop.id) } }
        }
    }
}

@Composable
private fun RouteRow(route: BusRoute, onClick: () -> Unit) {
    val subtitle = if (route.busName.isNotBlank()) "${route.longName} · ${route.busName}" else route.longName
    ListItem(
        headlineContent = { Text("Route ${route.shortName}") },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(Icons.Filled.DirectionsBus, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@Composable
private fun StopRow(stop: Stop, onClick: () -> Unit) {
    val subtitle = listOfNotNull(
        stop.village.takeIf { it.isNotBlank() },
        stop.city.takeIf { it.isNotBlank() },
        stop.district.takeIf { it.isNotBlank() },
    ).joinToString(", ").ifBlank { "Bus stop" }
    ListItem(
        headlineContent = { Text(stop.name) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(Icons.Filled.PinDrop, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
