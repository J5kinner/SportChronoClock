package com.sportchronoclock.location

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow

class AndroidLocationProvider(private val context: Context) : LocationProvider {
    override val locationFlow: Flow<LocationData> = LocationForegroundService.locationFlow.asSharedFlow()

    override fun startTracking() = LocationForegroundService.start(context)

    override fun stopTracking() = LocationForegroundService.stop(context)
}
