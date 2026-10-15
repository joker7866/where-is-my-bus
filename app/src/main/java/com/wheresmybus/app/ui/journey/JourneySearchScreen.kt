package com.wheresmybus.app.ui.journey

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wheresmybus.app.data.BusType
import com.wheresmybus.app.data.ConnectingJourneyOption
import com.wheresmybus.app.data.DataSource
import com.wheresmybus.app.data.DirectJourneyOption
import com.wheresmybus.app.data.JourneyLeg
import com.wheresmybus.app.data.Stop
import com.wheresmybus.app.remote.JourneySearchOutcome
import com.wheresmybus.app.ui.BusViewModel
import com.wheresmybus.app.ui.strings.AppLanguage
import com.wheresmybus.app.ui.strings.stringsFor
import com.wheresmybus.app.ui.theme.AccentYellow
import com.wheresmybus.app.ui.theme.BgCream
import com.wheresmybus.app.ui.theme.ChipMint
import com.wheresmybus.app.ui.theme.HeaderGreen
import com.wheresmybus.app.ui.theme.StatusLive

private sealed class JourneyUiState {
    object Idle : JourneyUiState()
    object Loading : JourneyUiState()
    data class Result(val outcome: JourneySearchOutcome) : JourneyUiState()
}

private enum class SortMode { DEPARTURE, DURATION }

/**
 * The From → To journey search: find direct and one-transfer connecting DEMO bus
 * services between two places, by stop id, English name, Tamil name, or city/village.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneySearchScreen(
    viewModel: BusViewModel,
    initialFrom: String,
    initialTo: String = "",
    language: AppLanguage = AppLanguage.ENGLISH,
    onBack: () -> Unit,
    onRouteClick: (String) -> Unit,
) {
    val strings = stringsFor(language)
    val context = LocalContext.current
    val allStops by viewModel.stops.collectAsStateWithLifecycle()
    val favoriteRouteIds by viewModel.favoriteRouteIds.collectAsStateWithLifecycle()

    var fromText by remember { mutableStateOf(initialFrom) }
    var toText by remember { mutableStateOf(initialTo) }
    var activeField by remember { mutableStateOf<Field?>(null) }
    var uiState by remember { mutableStateOf<JourneyUiState>(JourneyUiState.Idle) }
    var busTypeFilter by remember { mutableStateOf<BusType?>(null) }
    var sortMode by remember { mutableStateOf(SortMode.DEPARTURE) }
    var filterMenuOpen by remember { mutableStateOf(false) }

    val suggestions = remember(activeField, fromText, toText, allStops) {
        val query = when (activeField) {
            Field.FROM -> fromText
            Field.TO -> toText
            null -> ""
        }
        if (activeField == null || query.isBlank()) emptyList()
        else allStops.filter { stop ->
            stop.name.contains(query, ignoreCase = true) ||
                stop.tamilName.contains(query) ||
                stop.city.contains(query, ignoreCase = true) ||
                stop.village.contains(query, ignoreCase = true)
        }.take(6)
    }

    fun runSearch() {
        if (fromText.isBlank() || toText.isBlank()) return
        activeField = null
        uiState = JourneyUiState.Loading
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is JourneyUiState.Loading) {
            val outcome = viewModel.searchJourney(fromText, toText)
            if (outcome is JourneySearchOutcome.Success) {
                viewModel.recordJourneySearch(outcome.result.from.name, outcome.result.to.name)
            }
            uiState = JourneyUiState.Result(outcome)
        }
    }

    // Re-run automatically when both fields arrive pre-filled (tapping a History entry).
    LaunchedEffect(Unit) {
        if (initialFrom.isNotBlank() && initialTo.isNotBlank()) runSearch()
    }

    Column(modifier = Modifier.fillMaxSize().background(BgCream)) {
        TopAppBar(
            title = { Text("Find a bus") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderGreen, titleContentColor = Color.White),
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = fromText,
                        onValueChange = { fromText = it; uiState = JourneyUiState.Idle },
                        modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) activeField = Field.FROM },
                        label = { Text("From") },
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = toText,
                        onValueChange = { toText = it; uiState = JourneyUiState.Idle },
                        modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) activeField = Field.TO },
                        label = { Text("To") },
                        singleLine = true,
                    )
                }
                IconButton(
                    onClick = {
                        val swap = fromText
                        fromText = toText
                        toText = swap
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = ChipMint),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Icon(Icons.Filled.SwapVert, contentDescription = "Swap", tint = HeaderGreen)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (suggestions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White),
                ) {
                    suggestions.forEach { stop ->
                        StopSuggestionRow(stop) {
                            when (activeField) {
                                Field.FROM -> fromText = stop.name
                                Field.TO -> toText = stop.name
                                null -> {}
                            }
                            activeField = null
                        }
                    }
                }
            } else {
                Column {
                    Text(strings.busTypeFilterLabel, fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .clickable { filterMenuOpen = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(busTypeFilter?.name?.replace('_', ' ') ?: strings.allBusTypes, fontSize = 14.sp)
                            Icon(Icons.Filled.FilterList, contentDescription = null, tint = Color.Gray)
                        }
                        DropdownMenu(expanded = filterMenuOpen, onDismissRequest = { filterMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(strings.allBusTypes) },
                                onClick = { busTypeFilter = null; filterMenuOpen = false },
                            )
                            BusType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name.replace('_', ' ')) },
                                    onClick = { busTypeFilter = type; filterMenuOpen = false },
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { runSearch() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = fromText.isNotBlank() && toText.isNotBlank(),
                    ) {
                        Text("Search buses")
                    }
                }
            }
        }

        when (val state = uiState) {
            is JourneyUiState.Idle -> {}
            is JourneyUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is JourneyUiState.Result -> when (val outcome = state.outcome) {
                is JourneySearchOutcome.Error -> {
                    Text(
                        text = outcome.message,
                        color = Color(0xFFB3261E),
                        modifier = Modifier.padding(16.dp),
                    )
                }
                is JourneySearchOutcome.Success -> {
                    val direct = remember(outcome, busTypeFilter, sortMode) {
                        outcome.result.direct
                            .filter { busTypeFilter == null || it.leg.busType == busTypeFilter }
                            .let { list ->
                                when (sortMode) {
                                    SortMode.DEPARTURE -> list.sortedBy { it.leg.departureMin }
                                    SortMode.DURATION -> list.sortedBy { it.durationMin }
                                }
                            }
                    }
                    val connecting = remember(outcome, busTypeFilter, sortMode) {
                        outcome.result.connecting
                            .filter { opt -> busTypeFilter == null || opt.legs.any { it.busType == busTypeFilter } }
                            .let { list ->
                                when (sortMode) {
                                    SortMode.DEPARTURE -> list.sortedBy { it.legs.first().departureMin }
                                    SortMode.DURATION -> list.sortedBy { it.totalDurationMin }
                                }
                            }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        JourneyResultsList(
                            direct = direct,
                            connecting = connecting,
                            favoriteRouteIds = favoriteRouteIds,
                            onRouteClick = onRouteClick,
                            onToggleFavorite = { viewModel.toggleFavoriteRoute(it) },
                            onShare = { leg -> shareLeg(context, leg) },
                            modifier = Modifier.weight(1f),
                        )
                        ResultsToolbar(
                            strings = strings,
                            sortMode = sortMode,
                            onSortClick = {
                                sortMode = if (sortMode == SortMode.DEPARTURE) SortMode.DURATION else SortMode.DEPARTURE
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun shareLeg(context: android.content.Context, leg: JourneyLeg) {
    val text = "${leg.busName.ifBlank { leg.routeShortName }} (${leg.routeLongName}): " +
        "${leg.departureTime} → ${leg.arrivalTime}. Tracked on Where is my Bus."
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

@Composable
private fun ResultsToolbar(
    strings: com.wheresmybus.app.ui.strings.UiStrings,
    sortMode: SortMode,
    onSortClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(ChipMint)
                .clickable(onClick = onSortClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = HeaderGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "${strings.sortLabel}: ${if (sortMode == SortMode.DEPARTURE) strings.sortByDeparture else strings.sortByDuration}",
                fontSize = 12.sp,
                color = HeaderGreen,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private enum class Field { FROM, TO }

@Composable
private fun StopSuggestionRow(stop: Stop, onClick: () -> Unit) {
    val subtitle = listOfNotNull(
        stop.village.takeIf { it.isNotBlank() },
        stop.city.takeIf { it.isNotBlank() },
        stop.district.takeIf { it.isNotBlank() },
    ).joinToString(", ")
    ListItem(
        headlineContent = { Text(stop.name) },
        supportingContent = if (subtitle.isNotBlank()) {
            { Text(subtitle, fontSize = 12.sp) }
        } else null,
        leadingContent = { Icon(Icons.Filled.PinDrop, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@Composable
private fun JourneyResultsList(
    direct: List<DirectJourneyOption>,
    connecting: List<ConnectingJourneyOption>,
    favoriteRouteIds: Set<String>,
    onRouteClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onShare: (JourneyLeg) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (direct.isEmpty() && connecting.isEmpty()) {
            item {
                Text(
                    text = "No DEMO bus services found between these stops yet.",
                    color = Color.Gray,
                )
            }
        }
        if (direct.isNotEmpty()) {
            item {
                Text("Direct buses", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HeaderGreen)
            }
            items(direct) { option ->
                DirectCard(
                    option = option,
                    isFavorite = option.leg.routeId in favoriteRouteIds,
                    onClick = { onRouteClick(option.leg.routeId) },
                    onToggleFavorite = { onToggleFavorite(option.leg.routeId) },
                    onShare = { onShare(option.leg) },
                )
            }
        }
        if (connecting.isNotEmpty()) {
            item {
                Text("Connecting buses", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HeaderGreen)
            }
            items(connecting) { option ->
                ConnectingCard(
                    option = option,
                    favoriteRouteIds = favoriteRouteIds,
                    onRouteClick = onRouteClick,
                    onToggleFavorite = onToggleFavorite,
                    onShare = onShare,
                )
            }
        }
    }
}

@Composable
private fun DemoBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AccentYellow)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text("DEMO · unverified", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A5200))
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ChipMint)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, fontSize = 11.sp, color = HeaderGreen, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LegSummary(
    leg: JourneyLeg,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ChipMint)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(leg.routeShortName, color = HeaderGreen, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = leg.busName.ifBlank { leg.routeLongName },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Text(leg.routeLongName, fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) AccentYellow else Color.Gray,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (leg.operator.isNotBlank()) InfoChip(leg.operator)
            InfoChip(leg.busType.name.replace('_', ' '))
            InfoChip(leg.acType.name.replace('_', ' '))
            if (leg.source == DataSource.DEMO || !leg.verified) DemoBadge()
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${leg.departureTime} → ${leg.arrivalTime}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (leg.liveMode.name == "LIVE") {
                Spacer(modifier = Modifier.width(8.dp))
                Text("LIVE", fontSize = 11.sp, color = StatusLive, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DirectCard(
    option: DirectJourneyOption,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        LegSummary(option.leg, isFavorite, onToggleFavorite, onShare)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Duration: ${formatDuration(option.durationMin)}", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun ConnectingCard(
    option: ConnectingJourneyOption,
    favoriteRouteIds: Set<String>,
    onRouteClick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onShare: (JourneyLeg) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp),
    ) {
        option.legs.forEachIndexed { index, leg ->
            Box(modifier = Modifier.clickable { onRouteClick(leg.routeId) }) {
                LegSummary(
                    leg = leg,
                    isFavorite = leg.routeId in favoriteRouteIds,
                    onToggleFavorite = { onToggleFavorite(leg.routeId) },
                    onShare = { onShare(leg) },
                )
            }
            if (index == 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Change at this stop · wait ${option.waitMin} min",
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Total: ${formatDuration(option.totalDurationMin)}", fontSize = 12.sp, color = Color.Gray)
    }
}

private fun formatDuration(min: Int): String {
    val h = min / 60
    val m = min % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
