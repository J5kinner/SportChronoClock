package com.sportchronoclock.navigation

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class DirectionsService(private val httpClient: HttpClient) {

    suspend fun findPlace(query: String): Result<PlaceSuggestion> = runCatching {
        val results = httpClient.get("https://nominatim.openstreetmap.org/search") {
            parameter("q", query)
            parameter("format", "json")
            parameter("limit", "1")
            header("User-Agent", "SportChronoClock/1.0 (sport-clock-app)")
        }.body<List<NominatimPlace>>()
        val place = results.firstOrNull() ?: error("No results found for \"$query\"")
        PlaceSuggestion(place.displayName, place.lat.toDouble(), place.lon.toDouble())
    }

    suspend fun suggest(query: String): List<PlaceSuggestion> {
        if (query.length < 2) return emptyList()
        return runCatching {
            httpClient.get("https://nominatim.openstreetmap.org/search") {
                parameter("q", query)
                parameter("format", "json")
                parameter("limit", "5")
                header("User-Agent", "SportChronoClock/1.0 (sport-clock-app)")
            }.body<List<NominatimPlace>>().map {
                PlaceSuggestion(it.displayName, it.lat.toDouble(), it.lon.toDouble())
            }
        }.getOrDefault(emptyList())
    }

    suspend fun searchAndRoute(
        query: String,
        fromLat: Double,
        fromLon: Double
    ): Result<RouteResult> = runCatching {
        val place = geocode(query) ?: error("No results found for $query")
        getRoute(fromLat, fromLon, place.first, place.second, place.third)
            ?: error("Could not calculate a route")
    }

    suspend fun routeToSuggestion(
        suggestion: PlaceSuggestion,
        fromLat: Double,
        fromLon: Double
    ): Result<RouteResult> = runCatching {
        getRoute(fromLat, fromLon, suggestion.lat, suggestion.lon, suggestion.name)
            ?: error("Could not calculate a route")
    }

    suspend fun routeToCoordinates(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double
    ): Result<RouteResult> = runCatching {
        val destinationName = "$toLat, $toLon"
        getRoute(fromLat, fromLon, toLat, toLon, destinationName)
            ?: error("Could not calculate a route")
    }

    private suspend fun geocode(query: String): Triple<Double, Double, String>? {
        val results = httpClient.get("https://nominatim.openstreetmap.org/search") {
            parameter("q", query)
            parameter("format", "json")
            parameter("limit", "1")
            header("User-Agent", "SportChronoClock/1.0 (sport-clock-app)")
        }.body<List<NominatimPlace>>()

        val place = results.firstOrNull() ?: return null
        return Triple(place.lat.toDouble(), place.lon.toDouble(), place.displayName)
    }

    private suspend fun getRoute(
        fromLat: Double, fromLon: Double,
        toLat: Double, toLon: Double,
        destinationName: String
    ): RouteResult? {
        val response = httpClient.get(
            "https://router.project-osrm.org/route/v1/driving/$fromLon,$fromLat;$toLon,$toLat"
        ) {
            parameter("overview", "full")
            parameter("geometries", "geojson")
            parameter("steps", "true")
        }.body<OsrmResponse>()

        val route = response.routes.firstOrNull() ?: return null
        val steps = route.legs.flatMap { leg ->
            leg.steps.map { step -> step.toNavigationStep() }
        }
        return RouteResult(
            destinationName = destinationName,
            points = route.geometry.coordinates.map { it[1] to it[0] },
            steps = steps,
            distanceMeters = route.distance,
            durationSeconds = route.duration
        )
    }

    private fun OsrmStep.toNavigationStep() = NavigationStep(
        instruction = buildInstruction(this),
        distanceMeters = distance,
        maneuverType = maneuver.type,
        maneuverModifier = maneuver.modifier,
        coordinate = if (maneuver.location.size >= 2)
            maneuver.location[1] to maneuver.location[0]
        else
            0.0 to 0.0
    )

    private fun buildInstruction(step: OsrmStep): String {
        val modifier = step.maneuver.modifier
        val street = step.name.ifBlank { null }
        return when (step.maneuver.type) {
            "depart" -> if (street != null) "Head on $street" else "Start"
            "arrive" -> "You have arrived"
            "turn" -> when (modifier) {
                "left" -> if (street != null) "Turn left onto $street" else "Turn left"
                "right" -> if (street != null) "Turn right onto $street" else "Turn right"
                "slight left" -> if (street != null) "Bear left onto $street" else "Bear left"
                "slight right" -> if (street != null) "Bear right onto $street" else "Bear right"
                "sharp left" -> if (street != null) "Sharp left onto $street" else "Sharp left"
                "sharp right" -> if (street != null) "Sharp right onto $street" else "Sharp right"
                "uturn" -> "Make a U-turn"
                else -> if (street != null) "Continue on $street" else "Continue"
            }
            "merge" -> if (street != null) "Merge onto $street" else "Merge"
            "on ramp", "ramp" -> if (street != null) "Take the ramp onto $street" else "Take the ramp"
            "off ramp" -> if (street != null) "Take the exit onto $street" else "Take the exit"
            "fork" -> when (modifier) {
                "left" -> "Keep left at the fork"
                "right" -> "Keep right at the fork"
                else -> "Keep straight at the fork"
            }
            "roundabout", "rotary" -> "Enter the roundabout"
            "roundabout turn", "exit roundabout" -> if (street != null) "Exit onto $street" else "Exit the roundabout"
            "continue", "new name" -> if (street != null) "Continue onto $street" else "Continue straight"
            else -> if (street != null) "Continue on $street" else "Continue"
        }
    }
}
