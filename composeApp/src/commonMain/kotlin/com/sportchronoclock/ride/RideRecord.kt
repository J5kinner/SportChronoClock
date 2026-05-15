package com.sportchronoclock.ride

data class RideRecord(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long?,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val maxSpeedKmh: Double,
    val avgMovingSpeedKmh: Double,
    val maxLeanDeg: Double?,
    val maxLateralG: Double?,
    val elevationGainMeters: Double?,
    val polylineGeojson: String?,
    val crashed: Boolean,
) {
    val isComplete: Boolean get() = endedAt != null
}
