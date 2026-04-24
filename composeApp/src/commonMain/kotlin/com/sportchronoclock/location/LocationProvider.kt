package com.sportchronoclock.location

import kotlinx.coroutines.flow.Flow

interface LocationProvider {
    val locationFlow: Flow<LocationData>
    fun startTracking()
    fun stopTracking()
}
