package com.sportchronoclock.navigation

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class DirectionsService(private val httpClient: HttpClient) {

    suspend fun searchAndRoute(
        query: String,
        fromLat: Double,
        fromLon: Double
    ): Result<RouteResult> = runCatching {
        val place = geocode(query) ?: error("No results found for $query")
        val route = getRoute(fromLat, fromLon, place.first, place.second, place.third)
            ?: error("Could not calculate a route")
        route
    }

    private suspend fun geocode(query: String): Triple<Double, Double, String>? {
        val results = httpClient.get("https://nominatim.openstreetmap.org/search") {
            parameter("q", query)
            parameter("format", "json")
            parameter("limit", "1")
            // Nominatim ToS requires a descriptive User-Agent
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
        }.body<OsrmResponse>()

        val route = response.routes.firstOrNull() ?: return null
        return RouteResult(
            destinationName = destinationName,
            // GeoJSON coordinates are [lon, lat] — flip to (lat, lon) pairs
            points = route.geometry.coordinates.map { it[1] to it[0] },
            distanceMeters = route.distance,
            durationSeconds = route.duration
        )
    }
}
