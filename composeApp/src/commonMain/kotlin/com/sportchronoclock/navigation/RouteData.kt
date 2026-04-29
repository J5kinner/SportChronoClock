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
    val legs: List<OsrmLeg> = emptyList(),
    val duration: Double,
    val distance: Double
)

@Serializable
data class OsrmGeometry(
    val coordinates: List<List<Double>>   // GeoJSON order: [longitude, latitude]
)

@Serializable
data class OsrmLeg(
    val steps: List<OsrmStep> = emptyList()
)

@Serializable
data class OsrmStep(
    val distance: Double = 0.0,
    val name: String = "",
    val maneuver: OsrmManeuver
)

@Serializable
data class OsrmManeuver(
    val type: String = "",
    val modifier: String? = null,
    val location: List<Double> = emptyList()   // GeoJSON order: [longitude, latitude]
)

data class NavigationStep(
    val instruction: String,
    val distanceMeters: Double,
    val maneuverType: String,
    val maneuverModifier: String?,
    val coordinate: Pair<Double, Double>   // (latitude, longitude)
)

data class PlaceSuggestion(
    val name: String,
    val lat: Double,
    val lon: Double
)

data class RouteResult(
    val destinationName: String,
    val points: List<Pair<Double, Double>>,   // (latitude, longitude)
    val steps: List<NavigationStep> = emptyList(),
    val distanceMeters: Double,
    val durationSeconds: Double
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
    val durationMinutes: Int get() = (durationSeconds / 60).toInt()
}
