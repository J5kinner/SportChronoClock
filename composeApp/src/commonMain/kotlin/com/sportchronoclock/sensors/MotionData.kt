package com.sportchronoclock.sensors

data class MotionData(
    val pitchDeg: Float,
    val rollDeg: Float,
    val lateralG: Float,
    val longG: Float,
    val timestamp: Long,
)
