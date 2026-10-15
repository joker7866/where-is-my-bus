package com.wheresmybus.app.data

import com.wheresmybus.app.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

enum class SyncState { IDLE, SYNCING, SYNCED, FAILED }

/**
 * In-memory stand-in for a real transit backend, modeled on real Tamil Nadu
 * town-bus routes. LIVE routes have simulated buses walking their stop
 * sequence so ETAs and the route timeline count down for real; RECENT and
 * TIMETABLE routes use fixed mock values, mirroring how the real app would
 * fall back when passenger sharing is stale or unavailable for a route.
 *
 * Stops and routes start from a bundled seed (so the app works offline out of
 * the box) but can be refreshed from the admin panel's backend via
 * [refreshFromBackend], so edits made there reach the app on request.
 */
object MockBusRepository {

    private const val TICK_MS = 3_000L

    /** Assumed scheduled (timetable) pace, used to derive "Sched." times for any route. */
    private const val SCHEDULED_SPEED_METERS_PER_SEC = 6.5

    private val seedStops: List<Stop> = listOf(
        Stop("GPM", "Gandhipuram", "காந்திபுரம்", 11.0168, 76.9558, district = "Coimbatore", city = "Coimbatore"),
        Stop("CCR", "Cross Cut Road", "குறுக்குவெட்டி சாலை", 11.0040, 76.9603, district = "Coimbatore", city = "Coimbatore"),
        Stop("FM", "Flower Market", "பூ சந்தை", 11.0010, 76.9650, district = "Coimbatore", city = "Coimbatore"),
        Stop("RS", "Railway Station", "ரயில் நிலையம்", 10.9985, 76.9660, district = "Coimbatore", city = "Coimbatore"),
        Stop("TH", "Town Hall", "டவுன் ஹால்", 10.9980, 76.9625, district = "Coimbatore", city = "Coimbatore"),
        Stop("UKK", "Ukkadam", "உக்கடம்", 10.9946, 76.9558, district = "Coimbatore", city = "Coimbatore"),
        Stop("SGN", "Singanallur", "சிங்காநல்லூர்", 11.0023, 77.0304, district = "Coimbatore", city = "Coimbatore"),
        Stop("SVP", "Saravanampatti", "சரவணம்பட்டி", 11.0729, 77.0075, district = "Coimbatore", city = "Coimbatore"),
        Stop("OND", "Ondipudur", "ஒண்டிப்புதூர்", 11.0100, 77.0450, district = "Coimbatore", city = "Coimbatore"),
        Stop("TPT", "Tirupattur", "திருப்பத்தூர்", 10.1667, 78.5667, district = "Sivaganga", city = "Tirupattur"),
        Stop("PLP", "Pillayarpatti", "பிள்ளையார்பட்டி", 10.1000, 78.6300, district = "Sivaganga", city = "Karaikudi", village = "Pillayarpatti"),
        Stop("KKD", "Karaikudi", "காரைக்குடி", 10.0767, 78.7708, district = "Sivaganga", city = "Karaikudi"),
    )

    private val seedRoutes: List<BusRoute> = listOf(
        BusRoute(
            id = "R70A",
            shortName = "70A",
            longName = "Gandhipuram → Ukkadam",
            colorHex = 0xFF145C45,
            stopIds = listOf("GPM", "CCR", "FM", "RS", "TH", "UKK"),
            liveMode = LiveMode.LIVE,
            busName = "Gandhipuram Town Service",
            sharerCount = 3,
            delayMinutes = 5,
            routeType = RouteType.LOCAL,
            distanceKm = 7.5,
        ),
        BusRoute(
            id = "R45",
            shortName = "45",
            longName = "Gandhipuram → Saravanampatti",
            colorHex = 0xFF1E8E5A,
            stopIds = listOf("GPM", "SVP"),
            liveMode = LiveMode.RECENT,
            busName = "Saravanampatti Shuttle",
            lastSeenMinutesAgo = 3,
            routeType = RouteType.LOCAL,
            distanceKm = 9.0,
        ),
        BusRoute(
            id = "R12",
            shortName = "12",
            longName = "Ukkadam → Singanallur",
            colorHex = 0xFF6B7280,
            stopIds = listOf("UKK", "SGN"),
            liveMode = LiveMode.TIMETABLE,
            busName = "Singanallur Link",
            timetableEtaMinutes = 12,
            routeType = RouteType.LOCAL,
            distanceKm = 5.0,
        ),
        BusRoute(
            id = "R96",
            shortName = "96",
            longName = "Town Hall → Ondipudur",
            colorHex = 0xFF6B7280,
            stopIds = listOf("TH", "OND"),
            liveMode = LiveMode.TIMETABLE,
            busName = "Ondipudur Connector",
            timetableEtaMinutes = 18,
            routeType = RouteType.LOCAL,
            distanceKm = 6.5,
        ),
        BusRoute(
            id = "R515",
            shortName = "515",
            longName = "Tirupattur → Karaikudi",
            colorHex = 0xFF145C45,
            stopIds = listOf("TPT", "PLP", "KKD"),
            liveMode = LiveMode.LIVE,
            busName = "Sivaganga Express",
            sharerCount = 2,
            delayMinutes = 0,
            routeType = RouteType.LONG_DISTANCE,
            distanceKm = 22.0,
            viaTowns = "via Pillayarpatti",
        ),
    )

    private val _stops = MutableStateFlow(seedStops)
    val stops: StateFlow<List<Stop>> = _stops.asStateFlow()

    private val _routes = MutableStateFlow(seedRoutes)
    val routes: StateFlow<List<BusRoute>> = _routes.asStateFlow()

    private var stopsById: Map<String, Stop> = seedStops.associateBy { it.id }
    private var routesById: Map<String, BusRoute> = seedRoutes.associateBy { it.id }

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Every route gets a notional schedule anchored to when the app session started,
    // so "Sched." times are stable for the session instead of jumping around.
    private val sessionStartEpochMillis: Long = run {
        val fiveMinMillis = 5 * 60 * 1000L
        (System.currentTimeMillis() / fiveMinMillis) * fiveMinMillis
    }

    private data class BusSimState(
        val routeId: String,
        val segmentIndex: Int,
        val progress: Double,
        val speedMetersPerSec: Double,
    )

    // Only LIVE routes get simulated buses; RECENT/TIMETABLE routes report
    // fixed mock values instead of a moving position.
    private val busSimStates: MutableMap<String, BusSimState> = buildInitialBusStates(seedRoutes)
    private var lastTickAtMillis: Long = System.currentTimeMillis()

    private fun buildInitialBusStates(routes: List<BusRoute>): MutableMap<String, BusSimState> {
        val states = mutableMapOf<String, BusSimState>()
        routes.filter { it.liveMode == LiveMode.LIVE }.forEach { route ->
            states["${route.id}-A"] = BusSimState(route.id, 0, 0.1, speedMetersPerSec = 8.5)
        }
        return states
    }

    private fun segmentLengthMeters(route: BusRoute, segmentIndex: Int): Double {
        val from = stopsById.getValue(route.stopIds[segmentIndex])
        val to = stopsById.getValue(route.stopIds[segmentIndex + 1])
        return Geo.distanceMeters(from.lat, from.lng, to.lat, to.lng)
    }

    private fun locationOf(busId: String, state: BusSimState): BusLocation {
        val route = routesById.getValue(state.routeId)
        val from = stopsById.getValue(route.stopIds[state.segmentIndex])
        val to = stopsById.getValue(route.stopIds[state.segmentIndex + 1])
        val lat = Geo.lerp(from.lat, to.lat, state.progress)
        val lng = Geo.lerp(from.lng, to.lng, state.progress)
        val heading = Geo.bearingDegrees(from.lat, from.lng, to.lat, to.lng)
        return BusLocation(busId, state.routeId, lat, lng, heading)
    }

    private fun computeLocations(): List<BusLocation> =
        busSimStates.map { (busId, state) -> locationOf(busId, state) }

    private val simScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _busLocations = MutableStateFlow(computeLocations())
    val busLocations: StateFlow<List<BusLocation>> = _busLocations.asStateFlow()

    private val _favoriteStopIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteStopIds: StateFlow<Set<String>> = _favoriteStopIds.asStateFlow()

    private val _favoriteRouteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteRouteIds: StateFlow<Set<String>> = _favoriteRouteIds.asStateFlow()

    private const val MAX_HISTORY_ENTRIES = 20
    private val _journeyHistory = MutableStateFlow<List<JourneyHistoryEntry>>(emptyList())
    val journeyHistory: StateFlow<List<JourneyHistoryEntry>> = _journeyHistory.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    init {
        simScope.launch {
            while (isActive) {
                delay(TICK_MS)
                advanceBuses()
                lastTickAtMillis = System.currentTimeMillis()
                _busLocations.value = computeLocations()
            }
        }
    }

    /**
     * Pulls the current catalog from the admin panel's backend and, if it looks
     * valid, replaces the in-memory stops/routes and resets bus simulation state
     * to match. Falls back to leaving everything as-is (bundled seed, or the
     * last successful sync) if the backend is unreachable.
     */
    suspend fun refreshFromBackend(): Boolean {
        _syncState.value = SyncState.SYNCING
        val remote = ApiClient.fetchCatalog()
        if (remote == null) {
            _syncState.value = SyncState.FAILED
            return false
        }

        val remoteStopIds = remote.stops.map { it.id }.toSet()
        val validRoutes = remote.routes.filter { route ->
            route.stopIds.size >= 2 && route.stopIds.all { it in remoteStopIds }
        }
        if (remote.stops.isEmpty() || validRoutes.isEmpty()) {
            _syncState.value = SyncState.FAILED
            return false
        }

        stopsById = remote.stops.associateBy { it.id }
        routesById = validRoutes.associateBy { it.id }
        _stops.value = remote.stops
        _routes.value = validRoutes

        busSimStates.clear()
        busSimStates.putAll(buildInitialBusStates(validRoutes))
        _busLocations.value = computeLocations()

        ApiClient.fetchAnnouncements()?.let { fetched ->
            _announcements.value = fetched.sortedByDescending { it.createdAtEpochMillis }
        }

        _syncState.value = SyncState.SYNCED
        return true
    }

    /** Submits a piece of app feedback to the backend. Returns true if it was accepted. */
    suspend fun submitFeedback(message: String): Boolean =
        ApiClient.submitReport(type = "FEEDBACK", message = message)

    /** From→To journey search (direct + one-transfer connecting), delegated to the backend. */
    suspend fun searchJourney(from: String, to: String) = ApiClient.searchJourney(from, to)

    private fun advanceBuses() {
        val tickSeconds = TICK_MS / 1000.0
        busSimStates.keys.toList().forEach { busId ->
            val state = busSimStates[busId] ?: return@forEach
            val route = routesById[state.routeId] ?: return@forEach
            val lastSegment = route.stopIds.size - 2

            var segmentIndex = state.segmentIndex
            var progress = state.progress
            var segLen = segmentLengthMeters(route, segmentIndex)
            val remainingMeters = state.speedMetersPerSec * tickSeconds

            progress += remainingMeters / segLen
            while (progress >= 1.0) {
                if (segmentIndex >= lastSegment) {
                    // Reached the terminus; loop the bus back to the start for the demo.
                    segmentIndex = 0
                    progress = 0.1
                    segLen = segmentLengthMeters(route, segmentIndex)
                    break
                }
                progress -= 1.0
                segmentIndex += 1
                segLen = segmentLengthMeters(route, segmentIndex)
            }

            busSimStates[busId] = state.copy(segmentIndex = segmentIndex, progress = progress)
        }
    }

    fun toggleFavorite(stopId: String) {
        _favoriteStopIds.update { current ->
            if (stopId in current) current - stopId else current + stopId
        }
    }

    fun toggleFavoriteRoute(routeId: String) {
        _favoriteRouteIds.update { current ->
            if (routeId in current) current - routeId else current + routeId
        }
    }

    /** Records a completed journey search for the History tab, newest first, capped and deduped
     * against the immediately previous entry (repeated searches while browsing results). */
    fun recordJourneySearch(fromStopName: String, toStopName: String) {
        _journeyHistory.update { current ->
            if (current.firstOrNull()?.let { it.fromStopName == fromStopName && it.toStopName == toStopName } == true) {
                return@update current
            }
            val entry = JourneyHistoryEntry(fromStopName, toStopName, System.currentTimeMillis())
            (listOf(entry) + current).take(MAX_HISTORY_ENTRIES)
        }
    }

    fun stopById(stopId: String): Stop? = stopsById[stopId]

    fun routeById(routeId: String): BusRoute? = routesById[routeId]

    fun routesForStop(stopId: String): List<BusRoute> = routes.value.filter { stopId in it.stopIds }

    /** The stop in [stops] closest to the given coordinates, with its distance in meters. */
    fun nearestStop(lat: Double, lng: Double): Pair<Stop, Int> {
        val nearest = stops.value.minBy { Geo.distanceMeters(lat, lng, it.lat, it.lng) }
        val distance = Geo.distanceMeters(lat, lng, nearest.lat, nearest.lng).roundToInt()
        return nearest to distance
    }

    /** Minutes remaining, per route serving [stopId], for the next bus to reach it. */
    fun etasForStop(stopId: String): Flow<List<Eta>> = busLocations.map { _ ->
        routesForStop(stopId).mapNotNull { route -> etaFor(route, targetStopId = stopId) }
            .sortedBy { it.minutesAway }
    }

    private fun etaFor(route: BusRoute, targetStopId: String): Eta? {
        val destinationId = route.stopIds.last()
        return when (route.liveMode) {
            LiveMode.TIMETABLE -> Eta(
                routeId = route.id,
                routeShortName = route.shortName,
                destinationStopId = destinationId,
                colorHex = route.colorHex,
                liveMode = route.liveMode,
                minutesAway = route.timetableEtaMinutes,
            )
            LiveMode.RECENT -> Eta(
                routeId = route.id,
                routeShortName = route.shortName,
                destinationStopId = destinationId,
                colorHex = route.colorHex,
                liveMode = route.liveMode,
                minutesAway = route.lastSeenMinutesAgo + 4,
                lastSeenMinutesAgo = route.lastSeenMinutesAgo,
            )
            LiveMode.LIVE -> {
                val targetIndex = route.stopIds.indexOf(targetStopId)
                if (targetIndex == -1) return null
                val busesOnRoute = busSimStates.filterValues { it.routeId == route.id }
                if (busesOnRoute.isEmpty()) return null
                val bestMinutes = busesOnRoute.values.minOf { state ->
                    remainingMinutesToIndex(route, targetIndex, state)
                }
                Eta(
                    routeId = route.id,
                    routeShortName = route.shortName,
                    destinationStopId = destinationId,
                    colorHex = route.colorHex,
                    liveMode = route.liveMode,
                    minutesAway = bestMinutes,
                    sharerCount = route.sharerCount,
                )
            }
        }
    }

    private fun remainingMinutesToIndex(route: BusRoute, targetIndex: Int, state: BusSimState): Int {
        if (targetIndex <= state.segmentIndex) return 0 // already passed
        var remainingMeters = segmentLengthMeters(route, state.segmentIndex) * (1 - state.progress)
        for (i in (state.segmentIndex + 1) until targetIndex) {
            remainingMeters += segmentLengthMeters(route, i)
        }
        val metersPerMinute = state.speedMetersPerSec * 60.0
        return max(1, ceil(remainingMeters / metersPerMinute).toInt())
    }

    /** One ETA per route, each measured to that route's own destination stop. Drives the home screen. */
    val allRouteEtas: Flow<List<Eta>> = busLocations.map { _ ->
        routes.value.mapNotNull { route -> etaFor(route, targetStopId = route.stopIds.last()) }
    }

    /** Full stop-by-stop timeline for a route, matching the live route-tracking screen. */
    fun timelineForRoute(routeId: String): Flow<RouteTimeline?> = busLocations.map { _ ->
        val route = routesById[routeId] ?: return@map null
        buildTimeline(route)
    }

    private fun buildTimeline(route: BusRoute): RouteTimeline {
        val scheduledOffsetsSec = scheduleOffsetsSeconds(route)
        val busState = busSimStates.values.firstOrNull { it.routeId == route.id }

        val entries = route.stopIds.mapIndexed { index, stopId ->
            val stop = stopsById.getValue(stopId)
            val scheduled = sessionStartEpochMillis + (scheduledOffsetsSec[index] * 1000L).toLong()
            val expected = scheduled + route.delayMinutes * 60_000L

            val status = when {
                busState == null -> StopStatus.UPCOMING
                index <= busState.segmentIndex -> StopStatus.PASSED
                index == busState.segmentIndex + 1 -> StopStatus.NEXT
                else -> StopStatus.UPCOMING
            }
            val minutesAway = if (busState != null && status != StopStatus.PASSED) {
                remainingMinutesToIndex(route, index, busState)
            } else {
                0
            }

            StopTimelineEntry(stop, scheduled, expected, status, minutesAway)
        }

        val distanceToNext = if (busState != null) {
            (segmentLengthMeters(route, busState.segmentIndex) * (1 - busState.progress)).roundToInt()
        } else {
            0
        }
        val secondsAgo = ((System.currentTimeMillis() - lastTickAtMillis) / 1000).toInt().coerceAtLeast(0)

        return RouteTimeline(
            route = route,
            entries = entries,
            busInTransit = busState != null,
            distanceToNextStopMeters = distanceToNext,
            lastUpdatedSecondsAgo = secondsAgo,
        )
    }

    private fun scheduleOffsetsSeconds(route: BusRoute): List<Double> {
        val offsets = mutableListOf(0.0)
        var cumulative = 0.0
        for (i in 0 until route.stopIds.size - 1) {
            cumulative += segmentLengthMeters(route, i) / SCHEDULED_SPEED_METERS_PER_SEC
            offsets += cumulative
        }
        return offsets
    }
}
