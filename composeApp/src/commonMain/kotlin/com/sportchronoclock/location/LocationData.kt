package com.sportchronoclock.location

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val speed: Float,    // m/s; negative means unavailable
    val bearing: Float,  // degrees 0–360; negative means unavailable
    val timestamp: Long  // milliseconds since epoch
)
