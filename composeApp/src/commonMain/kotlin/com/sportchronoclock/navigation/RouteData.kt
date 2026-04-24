package com.sportchronoclock.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NominatimPlace(
    val lat: String,
    val lon: String,
    @SerialName("display_name") val displayName: String
)

@Serializable
data class OsrmResponse(
    val code: String = "",
    val routes: List<OsrmRoute> = emptyList()
)

@Serializable
data class OsrmRoute(
    val geometry: OsrmGeometry,
    val duration: Double,
    val distance: Double
)

@Serializable
data class OsrmGeometry(
    val coordinates: List<List<Double>>   // GeoJSON order: [longitude, latitude]
)

data class RouteResult(
    val destinationName: String,
    val points: List<Pair<Double, Double>>, // (latitude, longitude)
    val distanceMeters: Double,
    val durationSeconds: Double
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
    val durationMinutes: Int get() = (durationSeconds / 60).toInt()
}
