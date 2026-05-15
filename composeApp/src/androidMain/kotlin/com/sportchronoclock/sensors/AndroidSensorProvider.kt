package com.sportchronoclock.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.PI

private const val GRAVITY = 9.80665f
private const val RAD_TO_DEG = (180.0 / PI).toFloat()

class AndroidSensorProvider(context: Context) : SensorProvider {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val linearAccelSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private val rotationMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)
    private var latestPitch: Float = 0f
    private var latestRoll: Float = 0f
    private var latestLatG: Float = 0f
    private var latestLongG: Float = 0f
    private var latestTimestampMs: Long = 0L

    private val _flow = MutableSharedFlow<MotionData>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val motionFlow: Flow<MotionData> = _flow.asSharedFlow()

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            latestTimestampMs = System.currentTimeMillis()
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationValues)
                    latestPitch = orientationValues[1] * RAD_TO_DEG
                    latestRoll = orientationValues[2] * RAD_TO_DEG
                }
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    latestLatG = event.values[0] / GRAVITY
                    latestLongG = event.values[2] / GRAVITY
                }
            }
            _flow.tryEmit(
                MotionData(
                    pitchDeg = latestPitch,
                    rollDeg = latestRoll,
                    lateralG = latestLatG,
                    longG = latestLongG,
                    timestamp = latestTimestampMs,
                )
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun start() {
        rotationVectorSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
        linearAccelSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun stop() {
        sensorManager.unregisterListener(listener)
    }
}
