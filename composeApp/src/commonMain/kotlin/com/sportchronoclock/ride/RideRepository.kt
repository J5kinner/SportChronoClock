package com.sportchronoclock.ride

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.sportchronoclock.db.Ride
import com.sportchronoclock.db.RideDatabase
import com.sportchronoclock.db.SelectCompleted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RideRepository(private val db: RideDatabase) {
    private val queries = db.rideQueries

    fun observeCompleted(): Flow<List<RideRecord>> =
        queries.selectCompleted().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toRecord() }
        }

    fun observeById(id: Long): Flow<RideRecord?> =
        queries.selectById(id).asFlow().mapToOneOrNull(Dispatchers.Default).map { row ->
            row?.toRecord()
        }

    fun loadActive(): RideRecord? = queries.selectActive().executeAsOneOrNull()?.toRecord()

    fun startRide(startedAtMs: Long): Long {
        var newId = 0L
        queries.transaction {
            queries.insertRide(startedAtMs)
            newId = queries.lastInsertRowId().executeAsOne()
        }
        return newId
    }

    fun finalizeRide(
        id: Long,
        endedAtMs: Long,
        distanceMeters: Double,
        durationSeconds: Long,
        maxSpeedKmh: Double,
        avgMovingSpeedKmh: Double,
        maxLeanDeg: Double?,
        maxLateralG: Double?,
        elevationGainMeters: Double?,
        polylineGeojson: String?,
        crashed: Boolean,
    ) {
        queries.finalizeRide(
            endedAt = endedAtMs,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            maxSpeedKmh = maxSpeedKmh,
            avgMovingSpeedKmh = avgMovingSpeedKmh,
            maxLeanDeg = maxLeanDeg,
            maxLateralG = maxLateralG,
            elevationGainMeters = elevationGainMeters,
            polylineGeojson = polylineGeojson,
            crashedFlag = if (crashed) 1L else 0L,
            id = id,
        )
    }

    fun markCrashed(id: Long, endedAtMs: Long) {
        queries.markCrashed(endedAtMs, id)
    }

    fun delete(id: Long) {
        queries.deleteRide(id)
    }
}

private fun Ride.toRecord() = RideRecord(
    id = id,
    startedAt = started_at,
    endedAt = ended_at,
    distanceMeters = distance_meters,
    durationSeconds = duration_seconds,
    maxSpeedKmh = max_speed_kmh,
    avgMovingSpeedKmh = avg_moving_speed_kmh,
    maxLeanDeg = max_lean_deg,
    maxLateralG = max_lateral_g,
    elevationGainMeters = elevation_gain_meters,
    polylineGeojson = polyline_geojson,
    crashed = crashed_flag != 0L,
)

private fun SelectCompleted.toRecord() = RideRecord(
    id = id,
    startedAt = started_at,
    endedAt = ended_at,
    distanceMeters = distance_meters,
    durationSeconds = duration_seconds,
    maxSpeedKmh = max_speed_kmh,
    avgMovingSpeedKmh = avg_moving_speed_kmh,
    maxLeanDeg = max_lean_deg,
    maxLateralG = max_lateral_g,
    elevationGainMeters = elevation_gain_meters,
    polylineGeojson = polyline_geojson,
    crashed = crashed_flag != 0L,
)
