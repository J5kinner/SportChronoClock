package com.sportchronoclock.ride

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Listens to [RideEventBus] and persists rides automatically. Starts a ride when speed exceeds
 * [START_THRESHOLD_KMH] for [START_DELAY_MS]; ends it when stopped for [STOP_DELAY_MS].
 */
class RideRecorder(
    private val rideEventBus: RideEventBus,
    private val rideRepository: RideRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var collectJob: Job? = null

    private val _liveStats = MutableStateFlow<LiveRideStats?>(null)
    val liveStats: StateFlow<LiveRideStats?> = _liveStats.asStateFlow()

    private var accumulator: RideAccumulator? = null
    private var pendingStartMs: Long? = null
    private var lastMovingMs: Long? = null

    fun start() {
        if (collectJob?.isActive == true) return
        sweepCrashed()
        collectJob = scope.launch {
            rideEventBus.events.collect { event ->
                handle(event)
                accumulator?.let { acc -> _liveStats.value = acc.snapshot(event.timestamp) }
            }
        }
    }

    fun stop() {
        collectJob?.cancel()
        collectJob = null
    }

    private fun sweepCrashed() {
        val active = rideRepository.loadActive() ?: return
        rideRepository.markCrashed(active.id, endedAtMs = active.startedAt)
    }

    private suspend fun handle(event: RideEvent) {
        when (event) {
            is RideEvent.SpeedSample -> onSpeed(event)
            is RideEvent.LocationSample -> accumulator?.applyLocation(event)
            is RideEvent.MotionSample -> accumulator?.applyMotion(event)
            else -> Unit
        }
    }

    private suspend fun onSpeed(event: RideEvent.SpeedSample) {
        val moving = event.speedKmh >= START_THRESHOLD_KMH
        val now = event.timestamp
        val acc = accumulator
        if (acc == null) {
            if (moving) {
                val pending = pendingStartMs
                if (pending == null) {
                    pendingStartMs = now
                } else if (now - pending >= START_DELAY_MS) {
                    startRide(now)
                }
            } else {
                pendingStartMs = null
            }
        } else {
            acc.applySpeed(event)
            if (moving) {
                lastMovingMs = now
            } else {
                val lastMove = lastMovingMs ?: now
                if (now - lastMove >= STOP_DELAY_MS) {
                    finishRide(acc, now, crashed = false)
                }
            }
        }
    }

    private suspend fun startRide(now: Long) {
        val id = rideRepository.startRide(now)
        accumulator = RideAccumulator(rideId = id, startedAtMs = now)
        pendingStartMs = null
        lastMovingMs = now
        rideEventBus.emit(RideEvent.RideStarted(rideId = id, timestamp = now))
    }

    private suspend fun finishRide(acc: RideAccumulator, endedAtMs: Long, crashed: Boolean) {
        val summary = acc.toSummary(endedAtMs)
        rideRepository.finalizeRide(
            id = acc.rideId,
            endedAtMs = endedAtMs,
            distanceMeters = summary.distanceMeters,
            durationSeconds = summary.durationSeconds,
            maxSpeedKmh = summary.maxSpeedKmh.toDouble(),
            avgMovingSpeedKmh = summary.avgMovingSpeedKmh.toDouble(),
            maxLeanDeg = summary.maxLeanDeg?.toDouble(),
            maxLateralG = summary.maxLateralG?.toDouble(),
            elevationGainMeters = summary.elevationGainMeters,
            polylineGeojson = acc.polylineGeoJson(),
            crashed = crashed,
        )
        accumulator = null
        lastMovingMs = null
        pendingStartMs = null
        _liveStats.value = null
        rideEventBus.emit(RideEvent.RideEnded(rideId = acc.rideId, summary = summary, timestamp = endedAtMs))
    }

    companion object {
        const val START_THRESHOLD_KMH = 5f
        const val START_DELAY_MS = 10_000L
        const val STOP_DELAY_MS = 60_000L
    }
}
