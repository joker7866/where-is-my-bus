package com.wheresmybus.app.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wheresmybus.app.data.BusRoute
import com.wheresmybus.app.data.Eta
import com.wheresmybus.app.data.Stop
import com.wheresmybus.app.ui.BusViewModel
import com.wheresmybus.app.ui.strings.AppLanguage
import com.wheresmybus.app.ui.strings.stringsFor
import com.wheresmybus.app.ui.theme.HeaderGreen

@Composable
fun FavoritesScreen(
    viewModel: BusViewModel,
    language: AppLanguage,
    onStopClick: (String) -> Unit,
    onRouteClick: (String) -> Unit,
) {
    val strings = stringsFor(language)
    val favoriteStopIds by viewModel.favoriteStopIds.collectAsStateWithLifecycle()
    val favoriteStops = remember(favoriteStopIds) {
        favoriteStopIds.mapNotNull { viewModel.stopById(it) }
    }
    val favoriteRouteIds by viewModel.favoriteRouteIds.collectAsStateWithLifecycle()
    val favoriteRoutes = remember(favoriteRouteIds) {
        favoriteRouteIds.mapNotNull { viewModel.routeById(it) }
    }

    if (favoriteStops.isEmpty() && favoriteRoutes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(strings.favoritesEmpty)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        if (favoriteRoutes.isNotEmpty()) {
            item {
                Text(
                    "Buses",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = HeaderGreen,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            items(favoriteRoutes, key = { "route-${it.id}" }) { route ->
                FavoriteRouteRow(
                    route = route,
                    onClick = { onRouteClick(route.id) },
                    onUnfavorite = { viewModel.toggleFavoriteRoute(route.id) },
                )
            }
        }
        if (favoriteStops.isNotEmpty()) {
            item {
                Text(
                    "Stops",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = HeaderGreen,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            items(favoriteStops, key = { "stop-${it.id}" }) { stop ->
                FavoriteStopRow(
                    stop = stop,
                    viewModel = viewModel,
                    onClick = { onStopClick(stop.id) },
                    onUnfavorite = { viewModel.toggleFavorite(stop.id) },
                )
            }
        }
    }
}

@Composable
private fun FavoriteRouteRow(route: BusRoute, onClick: () -> Unit, onUnfavorite: () -> Unit) {
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        leadingContent = {
            Box(
                modifier = Modifier
                    .background(Color(0xFFDCEFE2), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(route.shortName, color = HeaderGreen, fontWeight = FontWeight.Bold)
            }
        },
        headlineContent = { Text(route.busName.ifBlank { route.longName }) },
        supportingContent = { Text(route.longName) },
        trailingContent = {
            IconButton(onClick = onUnfavorite) {
                Icon(Icons.Filled.Star, contentDescription = "Unfavorite")
            }
        },
    )
}

@Composable
private fun FavoriteStopRow(
    stop: Stop,
    viewModel: BusViewModel,
    onClick: () -> Unit,
    onUnfavorite: () -> Unit,
) {
    val etas by remember(stop.id) { viewModel.etasForStop(stop.id) }
        .collectAsStateWithLifecycle(initialValue = emptyList<Eta>())

    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        headlineContent = { Text(stop.name) },
        supportingContent = { Text(etaSummary(etas)) },
        trailingContent = {
            IconButton(onClick = onUnfavorite) {
                Icon(Icons.Filled.Star, contentDescription = "Unfavorite")
            }
        },
    )
}

private fun etaSummary(etas: List<Eta>): String {
    if (etas.isEmpty()) return "No buses currently serving this stop"
    return etas.joinToString(separator = "  •  ") { "Route ${it.routeShortName}: ${it.minutesAway} min" }
}
