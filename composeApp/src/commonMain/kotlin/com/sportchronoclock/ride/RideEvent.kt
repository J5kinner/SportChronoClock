package com.sportchronoclock.ride

sealed interface RideEvent {
    val timestamp: Long

    data class SpeedSample(
        val speedMps: Float,
        val speedKmh: Float,
        override val timestamp: Long,
    ) : RideEvent

    data class LocationSample(
        val lat: Double,
        val lng: Double,
        val altitudeMeters: Double?,
        val accuracyMeters: Float?,
        override val timestamp: Long,
    ) : RideEvent

    data class MotionSample(
        val pitchDeg: Float,
        val rollDeg: Float,
        val lateralG: Float,
        val longG: Float,
        override val timestamp: Long,
    ) : RideEvent

    data class TurnApproaching(
        val instruction: String,
        val distanceMeters: Double,
        override val timestamp: Long,
    ) : RideEvent

    data class RideStarted(
        val rideId: Long,
        override val timestamp: Long,
    ) : RideEvent

    data class RideEnded(
        val rideId: Long,
        val summary: RideSummary?,
        override val timestamp: Long,
    ) : RideEvent
}

data class RideSummary(
    val rideId: Long,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val maxSpeedKmh: Float,
    val avgMovingSpeedKmh: Float,
    val maxLeanDeg: Float?,
    val maxLateralG: Float?,
    val elevationGainMeters: Double?,
)
