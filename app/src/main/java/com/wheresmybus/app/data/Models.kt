package com.wheresmybus.app.data

enum class PlaceType { VILLAGE, TOWN, CITY, BUS_STAND, LANDMARK }

data class Stop(
    val id: String,
    val name: String,
    val tamilName: String,
    val lat: Double,
    val lng: Double,
    /** Admin-assigned region, e.g. "Coimbatore", "Sivaganga". */
    val district: String = "",
    /** Town/city this stop is in, e.g. "Coimbatore", "Karaikudi". */
    val city: String = "",
    /** Village or locality, for rural stops — empty for stops directly in a city. */
    val village: String = "",
    val placeType: PlaceType = PlaceType.TOWN,
)

enum class LiveMode { LIVE, RECENT, TIMETABLE }

enum class RouteType { LOCAL, LONG_DISTANCE }

enum class OperatorType { GOVERNMENT, PRIVATE }

enum class BusType { VILLAGE, TOWN, MOFUSSIL, EXPRESS, DELUXE, ULTRA_DELUXE, MINI, SPECIAL }

enum class AcType { AC, NON_AC }

/** DEMO data must never be presented as real/factual TNSTC information — always show it
 * with a visible "demo" badge until an admin marks it OFFICIAL/COMMUNITY and verified. */
enum class DataSource { DEMO, OFFICIAL, COMMUNITY }

data class BusRoute(
    val id: String,
    val shortName: String,
    val longName: String,
    val colorHex: Long,
    val stopIds: List<String>,
    val liveMode: LiveMode,
    /** Named bus/service, e.g. "Sivaganga Express" — distinct from the From → To description. */
    val busName: String = "",
    /** Mock sharer count shown for LIVE routes, e.g. "Live · 3 sharing". */
    val sharerCount: Int = 0,
    /** Mock "seen N min ago" value shown for RECENT routes. */
    val lastSeenMinutesAgo: Int = 0,
    /** Fixed schedule-based ETA for TIMETABLE routes (no live tracking). */
    val timetableEtaMinutes: Int = 0,
    /** Minutes behind schedule for LIVE routes, e.g. "5 min late". */
    val delayMinutes: Int = 0,
    /** Point-to-point town route vs. a longer intercity/inter-town service. */
    val routeType: RouteType = RouteType.LOCAL,
    /** End-to-end distance in km — most meaningful for LONG_DISTANCE routes. */
    val distanceKm: Double = 0.0,
    /** Free text, e.g. "via Pillayarpatti" — shown for LONG_DISTANCE routes. */
    val viaTowns: String = "",
    /** Operating company name, e.g. "TNSTC", "Sivaganga Travels". */
    val operator: String = "",
    val operatorType: OperatorType = OperatorType.GOVERNMENT,
    val busType: BusType = BusType.TOWN,
    val acType: AcType = AcType.NON_AC,
    val source: DataSource = DataSource.DEMO,
    /** Admin-confirmed as real/accurate — false means this must be shown as unverified. */
    val verified: Boolean = false,
)

/** One leg of a journey search result: ride a single route between two of its stops. */
data class JourneyLeg(
    val routeId: String,
    val routeShortName: String,
    val routeLongName: String,
    val busName: String,
    val colorHex: Long,
    val liveMode: LiveMode,
    val operator: String,
    val operatorType: OperatorType,
    val busType: BusType,
    val acType: AcType,
    val source: DataSource,
    val verified: Boolean,
    val fromStopId: String,
    val toStopId: String,
    val departureMin: Int,
    val arrivalMin: Int,
    val departureTime: String,
    val arrivalTime: String,
)

data class DirectJourneyOption(val leg: JourneyLeg, val durationMin: Int)

/** A one-transfer journey: ride [legs][0] to [transferStopId], wait [waitMin], then [legs][1]. */
data class ConnectingJourneyOption(
    val transferStopId: String,
    val waitMin: Int,
    val totalDurationMin: Int,
    val legs: List<JourneyLeg>,
)

data class JourneySearchResult(
    val from: Stop,
    val to: Stop,
    val direct: List<DirectJourneyOption>,
    val connecting: List<ConnectingJourneyOption>,
)

data class BusLocation(
    val busId: String,
    val routeId: String,
    val lat: Double,
    val lng: Double,
    val headingDegrees: Float,
)

data class Eta(
    val routeId: String,
    val routeShortName: String,
    val destinationStopId: String,
    val colorHex: Long,
    val liveMode: LiveMode,
    val minutesAway: Int,
    val sharerCount: Int = 0,
    val lastSeenMinutesAgo: Int = 0,
)

enum class StopStatus { PASSED, NEXT, UPCOMING }

data class StopTimelineEntry(
    val stop: Stop,
    val scheduledEpochMillis: Long,
    val expectedEpochMillis: Long,
    val status: StopStatus,
    val minutesAway: Int,
)

data class RouteTimeline(
    val route: BusRoute,
    val entries: List<StopTimelineEntry>,
    /** True while a bus is actively tracked between two stops (LIVE routes only). */
    val busInTransit: Boolean,
    val distanceToNextStopMeters: Int,
    val lastUpdatedSecondsAgo: Int,
)

/** One past From→To search, newest first, for the History tab. */
data class JourneyHistoryEntry(
    val fromStopName: String,
    val toStopName: String,
    val searchedAtEpochMillis: Long,
)

enum class AnnouncementSeverity { INFO, WARNING }

data class Announcement(
    val id: String,
    val title: String,
    val body: String,
    val severity: AnnouncementSeverity,
    val createdAtEpochMillis: Long,
)
