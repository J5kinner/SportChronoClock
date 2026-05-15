package com.sportchronoclock.ride

data class LiveRideStats(
    val rideId: Long,
    val startedAtMs: Long,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val maxSpeedKmh: Float,
    val avgMovingSpeedKmh: Float,
    val maxLeanDeg: Float?,
    val maxLateralG: Float?,
)
