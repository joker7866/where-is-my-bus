package com.wheresmybus.app.remote

import com.wheresmybus.app.data.AcType
import com.wheresmybus.app.data.Announcement
import com.wheresmybus.app.data.AnnouncementSeverity
import com.wheresmybus.app.data.BusRoute
import com.wheresmybus.app.data.BusType
import com.wheresmybus.app.data.ConnectingJourneyOption
import com.wheresmybus.app.data.DataSource
import com.wheresmybus.app.data.DirectJourneyOption
import com.wheresmybus.app.data.JourneyLeg
import com.wheresmybus.app.data.JourneySearchResult
import com.wheresmybus.app.data.LiveMode
import com.wheresmybus.app.data.OperatorType
import com.wheresmybus.app.data.PlaceType
import com.wheresmybus.app.data.RouteType
import com.wheresmybus.app.data.Stop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

data class RemoteCatalog(val stops: List<Stop>, val routes: List<BusRoute>)

/** Outcome of a from/to journey search: either a result (possibly with zero options) or a
 * reason it couldn't run at all (unknown stop name, network failure, etc). */
sealed class JourneySearchOutcome {
    data class Success(val result: JourneySearchResult) : JourneySearchOutcome()
    data class Error(val message: String) : JourneySearchOutcome()
}

/**
 * Talks to the admin panel's backend (see /backend) over plain HTTP using only
 * the platform's HttpURLConnection + org.json — no extra Gradle dependency.
 * Read calls return null/empty on any failure so callers can fall back to the
 * bundled seed data; the app always works offline.
 */
object ApiClient {

    suspend fun fetchCatalog(): RemoteCatalog? = withContext(Dispatchers.IO) {
        try {
            val stops = fetchStops()
            val routes = fetchRoutes()
            if (stops.isEmpty() || routes.isEmpty()) null else RemoteCatalog(stops, routes)
        } catch (e: Exception) {
            null
        }
    }

    /** Null means the fetch failed (caller should keep whatever it already had); an
     * empty list means the backend was reached and genuinely has no announcements. */
    suspend fun fetchAnnouncements(): List<Announcement>? = withContext(Dispatchers.IO) {
        try {
            val json = getJsonArray("${RemoteConfig.BASE_URL}/announcements")
            (0 until json.length()).map { i ->
                val o = json.getJSONObject(i)
                Announcement(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    body = o.getString("body"),
                    severity = runCatching { AnnouncementSeverity.valueOf(o.getString("severity")) }
                        .getOrDefault(AnnouncementSeverity.INFO),
                    createdAtEpochMillis = parseIsoToEpochMillis(o.getString("createdAt")),
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Submits user feedback/a report to the backend. Returns true if it was accepted. */
    suspend fun submitReport(
        type: String,
        message: String,
        routeId: String? = null,
        stopId: String? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("type", type)
                put("message", message)
                if (routeId != null) put("routeId", routeId)
                if (stopId != null) put("stopId", stopId)
            }
            val connection = URL("${RemoteConfig.BASE_URL}/reports").openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            connection.disconnect()
            code in 200..299
        } catch (e: Exception) {
            false
        }
    }

    /** Submits a from/to journey search. Null `JourneySearchOutcome` is never returned —
     * failures come back as [JourneySearchOutcome.Error] so the screen can show why. */
    suspend fun searchJourney(from: String, to: String): JourneySearchOutcome = withContext(Dispatchers.IO) {
        try {
            val url = "${RemoteConfig.BASE_URL}/search" +
                "?from=${URLEncoder.encode(from, "UTF-8")}&to=${URLEncoder.encode(to, "UTF-8")}"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.requestMethod = "GET"
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream.use { it.bufferedReader().readText() }
            connection.disconnect()
            val body = JSONObject(text)
            if (code !in 200..299) {
                return@withContext JourneySearchOutcome.Error(
                    body.optString("error", "Search failed"),
                )
            }
            JourneySearchOutcome.Success(parseJourneyResult(body))
        } catch (e: Exception) {
            JourneySearchOutcome.Error("Could not reach the server. Check your connection and try again.")
        }
    }

    private fun parseJourneyResult(o: JSONObject): JourneySearchResult = JourneySearchResult(
        from = parseStop(o.getJSONObject("from")),
        to = parseStop(o.getJSONObject("to")),
        direct = o.getJSONArray("direct").let { arr ->
            (0 until arr.length()).map { i ->
                val leg = arr.getJSONObject(i)
                DirectJourneyOption(leg = parseLeg(leg), durationMin = leg.optInt("durationMin", 0))
            }
        },
        connecting = o.getJSONArray("connecting").let { arr ->
            (0 until arr.length()).map { i ->
                val c = arr.getJSONObject(i)
                val legsJson = c.getJSONArray("legs")
                ConnectingJourneyOption(
                    transferStopId = c.getString("transferStopId"),
                    waitMin = c.optInt("waitMin", 0),
                    totalDurationMin = c.optInt("totalDurationMin", 0),
                    legs = (0 until legsJson.length()).map { parseLeg(legsJson.getJSONObject(it)) },
                )
            }
        },
    )

    private fun parseLeg(o: JSONObject): JourneyLeg {
        val route = o.getJSONObject("route")
        return JourneyLeg(
            routeId = route.getString("id"),
            routeShortName = route.optString("shortName", ""),
            routeLongName = route.optString("longName", ""),
            busName = route.optString("busName", ""),
            colorHex = parseColor(route.optString("colorHex", "#145C45")),
            liveMode = runCatching { LiveMode.valueOf(route.getString("liveMode")) }
                .getOrDefault(LiveMode.TIMETABLE),
            operator = route.optString("operator", ""),
            operatorType = runCatching { OperatorType.valueOf(route.getString("operatorType")) }
                .getOrDefault(OperatorType.GOVERNMENT),
            busType = runCatching { BusType.valueOf(route.getString("busType")) }
                .getOrDefault(BusType.TOWN),
            acType = runCatching { AcType.valueOf(route.getString("acType")) }
                .getOrDefault(AcType.NON_AC),
            source = runCatching { DataSource.valueOf(route.getString("source")) }
                .getOrDefault(DataSource.DEMO),
            verified = route.optBoolean("verified", false),
            fromStopId = o.getString("fromStopId"),
            toStopId = o.getString("toStopId"),
            departureMin = o.optInt("departureMin", 0),
            arrivalMin = o.optInt("arrivalMin", 0),
            departureTime = o.optString("departureTime", ""),
            arrivalTime = o.optString("arrivalTime", ""),
        )
    }

    private fun parseStop(o: JSONObject): Stop = Stop(
        id = o.getString("id"),
        name = o.getString("name"),
        tamilName = o.optString("tamilName", ""),
        lat = o.getDouble("lat"),
        lng = o.getDouble("lng"),
        district = o.optString("district", ""),
        city = o.optString("city", ""),
        village = o.optString("village", ""),
        placeType = runCatching { PlaceType.valueOf(o.getString("placeType")) }
            .getOrDefault(PlaceType.TOWN),
    )

    private fun fetchStops(): List<Stop> {
        val json = getJsonArray("${RemoteConfig.BASE_URL}/stops")
        return (0 until json.length()).map { i -> parseStop(json.getJSONObject(i)) }
    }

    private fun fetchRoutes(): List<BusRoute> {
        val json = getJsonArray("${RemoteConfig.BASE_URL}/routes")
        return (0 until json.length()).map { i ->
            val o = json.getJSONObject(i)
            val stopIdsJson = o.getJSONArray("stopIds")
            val stopIds = (0 until stopIdsJson.length()).map { stopIdsJson.getString(it) }
            BusRoute(
                id = o.getString("id"),
                shortName = o.getString("shortName"),
                longName = o.getString("longName"),
                colorHex = parseColor(o.optString("colorHex", "#145C45")),
                stopIds = stopIds,
                liveMode = runCatching { LiveMode.valueOf(o.getString("liveMode")) }
                    .getOrDefault(LiveMode.TIMETABLE),
                busName = o.optString("busName", ""),
                sharerCount = o.optInt("sharerCount", 0),
                lastSeenMinutesAgo = o.optInt("lastSeenMinutesAgo", 0),
                timetableEtaMinutes = o.optInt("timetableEtaMinutes", 0),
                delayMinutes = o.optInt("delayMinutes", 0),
                routeType = runCatching { RouteType.valueOf(o.getString("routeType")) }
                    .getOrDefault(RouteType.LOCAL),
                distanceKm = o.optDouble("distanceKm", 0.0),
                viaTowns = o.optString("viaTowns", ""),
                operator = o.optString("operator", ""),
                operatorType = runCatching { OperatorType.valueOf(o.getString("operatorType")) }
                    .getOrDefault(OperatorType.GOVERNMENT),
                busType = runCatching { BusType.valueOf(o.getString("busType")) }
                    .getOrDefault(BusType.TOWN),
                acType = runCatching { AcType.valueOf(o.getString("acType")) }
                    .getOrDefault(AcType.NON_AC),
                source = runCatching { DataSource.valueOf(o.getString("source")) }
                    .getOrDefault(DataSource.DEMO),
                verified = o.optBoolean("verified", false),
            )
        }
    }

    private fun parseColor(hex: String): Long {
        val cleaned = hex.removePrefix("#").ifBlank { "145C45" }
        return "FF$cleaned".toLong(16)
    }

    /** Backend timestamps are ISO-8601, sometimes with a zone/millis (admin-created) and
     * sometimes without (seed data) — fall back from Instant to a bare LocalDateTime-as-UTC. */
    private fun parseIsoToEpochMillis(iso: String): Long = try {
        Instant.parse(iso).toEpochMilli()
    } catch (e: DateTimeParseException) {
        try {
            LocalDateTime.parse(iso).toInstant(ZoneOffset.UTC).toEpochMilli()
        } catch (e2: DateTimeParseException) {
            System.currentTimeMillis()
        }
    }

    private fun getJsonArray(urlString: String): JSONArray {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.connectTimeout = 4000
        connection.readTimeout = 4000
        connection.requestMethod = "GET"
        return connection.inputStream.use { stream ->
            JSONArray(stream.bufferedReader().readText())
        }
    }
}
