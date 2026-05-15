package com.sportchronoclock.sensors

import kotlinx.coroutines.flow.Flow

interface SensorProvider {
    val motionFlow: Flow<MotionData>
    fun start()
    fun stop()
}
