package com.wheresmybus.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wheresmybus.app.data.Announcement
import com.wheresmybus.app.data.BusRoute
import com.wheresmybus.app.data.Eta
import com.wheresmybus.app.data.JourneyHistoryEntry
import com.wheresmybus.app.data.MockBusRepository
import com.wheresmybus.app.data.RouteTimeline
import com.wheresmybus.app.data.Stop
import com.wheresmybus.app.data.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BusViewModel : ViewModel() {

    val stops: StateFlow<List<Stop>> = MockBusRepository.stops
    val routes: StateFlow<List<BusRoute>> = MockBusRepository.routes

    val favoriteStopIds: StateFlow<Set<String>> = MockBusRepository.favoriteStopIds
    val favoriteRouteIds: StateFlow<Set<String>> = MockBusRepository.favoriteRouteIds
    val journeyHistory: StateFlow<List<JourneyHistoryEntry>> = MockBusRepository.journeyHistory
    val allRouteEtas: Flow<List<Eta>> = MockBusRepository.allRouteEtas
    val syncState: StateFlow<SyncState> = MockBusRepository.syncState
    val announcements: StateFlow<List<Announcement>> = MockBusRepository.announcements

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { MockBusRepository.refreshFromBackend() }
    }

    fun toggleFavorite(stopId: String) = MockBusRepository.toggleFavorite(stopId)

    fun toggleFavoriteRoute(routeId: String) = MockBusRepository.toggleFavoriteRoute(routeId)

    fun recordJourneySearch(fromStopName: String, toStopName: String) =
        MockBusRepository.recordJourneySearch(fromStopName, toStopName)

    fun stopById(stopId: String): Stop? = MockBusRepository.stopById(stopId)

    fun routeById(routeId: String): BusRoute? = MockBusRepository.routeById(routeId)

    fun routesForStop(stopId: String): List<BusRoute> = MockBusRepository.routesForStop(stopId)

    fun etasForStop(stopId: String): Flow<List<Eta>> = MockBusRepository.etasForStop(stopId)

    fun timelineForRoute(routeId: String): Flow<RouteTimeline?> =
        MockBusRepository.timelineForRoute(routeId)

    /** Nearest known stop to [lat]/[lng], with its distance in meters. */
    fun nearestStop(lat: Double, lng: Double): Pair<Stop, Int> =
        MockBusRepository.nearestStop(lat, lng)

    fun submitFeedback(message: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(MockBusRepository.submitFeedback(message))
        }
    }

    suspend fun searchJourney(from: String, to: String) = MockBusRepository.searchJourney(from, to)
}
