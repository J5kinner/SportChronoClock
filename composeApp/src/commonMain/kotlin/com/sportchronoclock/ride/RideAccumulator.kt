package com.sportchronoclock.ride

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal class RideAccumulator(
    val rideId: Long,
    val startedAtMs: Long,
) {
    var distanceMeters: Double = 0.0
        private set
    var maxSpeedKmh: Float = 0f
        private set
    var maxLeanDeg: Float? = null
        private set
    var maxLateralG: Float? = null
        private set
    var elevationGainMeters: Double = 0.0
        private set

    private var lastLat: Double? = null
    private var lastLng: Double? = null
    private var lastAltitude: Double? = null
    private var lastSpeedMs: Long? = null
    private var movingSpeedSumKmh: Double = 0.0
    private var movingSpeedCount: Long = 0
    private val polylinePoints = mutableListOf<Pair<Double, Double>>()

    val avgMovingSpeedKmh: Float
        get() = if (movingSpeedCount == 0L) 0f
        else (movingSpeedSumKmh / movingSpeedCount).toFloat()

    fun applySpeed(sample: RideEvent.SpeedSample) {
        if (sample.speedKmh > maxSpeedKmh) maxSpeedKmh = sample.speedKmh
        if (sample.speedKmh >= MOVING_THRESHOLD_KMH) {
            movingSpeedSumKmh += sample.speedKmh
            movingSpeedCount += 1
        }
        lastSpeedMs = sample.timestamp
    }

    fun applyLocation(sample: RideEvent.LocationSample) {
        val pLat = lastLat
        val pLng = lastLng
        if (pLat != null && pLng != null) {
            distanceMeters += haversineMeters(pLat, pLng, sample.lat, sample.lng)
        }
        sample.altitudeMeters?.let { alt ->
            val prev = lastAltitude
            if (prev != null && alt > prev) {
                elevationGainMeters += (alt - prev)
            }
            lastAltitude = alt
        }
        lastLat = sample.lat
        lastLng = sample.lng
        polylinePoints.add(sample.lat to sample.lng)
    }

    fun applyMotion(sample: RideEvent.MotionSample) {
        val lean = abs(sample.rollDeg)
        val current = maxLeanDeg
        if (current == null || lean > current) maxLeanDeg = lean
        val g = abs(sample.lateralG)
        val currentG = maxLateralG
        if (currentG == null || g > currentG) maxLateralG = g
    }

    fun durationSecondsAt(nowMs: Long): Long = ((nowMs - startedAtMs) / 1000L).coerceAtLeast(0L)

    fun snapshot(nowMs: Long): LiveRideStats = LiveRideStats(
        rideId = rideId,
        startedAtMs = startedAtMs,
        distanceMeters = distanceMeters,
        durationSeconds = durationSecondsAt(nowMs),
        maxSpeedKmh = maxSpeedKmh,
        avgMovingSpeedKmh = avgMovingSpeedKmh,
        maxLeanDeg = maxLeanDeg,
        maxLateralG = maxLateralG,
    )

    fun polylineGeoJson(): String? {
        if (polylinePoints.size < 2) return null
        val coords = polylinePoints.joinToString(",") { (lat, lng) -> "[$lng,$lat]" }
        return "{\"type\":\"LineString\",\"coordinates\":[$coords]}"
    }

    fun toSummary(endedAtMs: Long): RideSummary = RideSummary(
        rideId = rideId,
        distanceMeters = distanceMeters,
        durationSeconds = durationSecondsAt(endedAtMs),
        maxSpeedKmh = maxSpeedKmh,
        avgMovingSpeedKmh = avgMovingSpeedKmh,
        maxLeanDeg = maxLeanDeg,
        maxLateralG = maxLateralG,
        elevationGainMeters = elevationGainMeters,
    )

    companion object {
        const val MOVING_THRESHOLD_KMH = 5f
    }
}

private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val toRad = PI / 180.0
    val phi1 = lat1 * toRad
    val phi2 = lat2 * toRad
    val dPhi = (lat2 - lat1) * toRad
    val dLambda = (lon2 - lon1) * toRad
    val sinHalfDPhi = sin(dPhi / 2)
    val sinHalfDLambda = sin(dLambda / 2)
    val a = sinHalfDPhi * sinHalfDPhi + cos(phi1) * cos(phi2) * sinHalfDLambda * sinHalfDLambda
    return r * 2 * atan2(sqrt(a), sqrt(1.0 - a))
}
