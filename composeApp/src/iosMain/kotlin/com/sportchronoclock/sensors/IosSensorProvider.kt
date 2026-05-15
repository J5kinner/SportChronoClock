package com.sportchronoclock.sensors

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSDate
import platform.Foundation.NSOperationQueue
import platform.Foundation.timeIntervalSince1970
import kotlin.math.PI

private const val RAD_TO_DEG = (180.0 / PI).toFloat()

@OptIn(ExperimentalForeignApi::class)
class IosSensorProvider : SensorProvider {

    private val motionManager = CMMotionManager().apply {
        deviceMotionUpdateInterval = 0.02
    }
    private val operationQueue = NSOperationQueue()

    private val _flow = MutableSharedFlow<MotionData>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val motionFlow: Flow<MotionData> = _flow.asSharedFlow()

    override fun start() {
        if (!motionManager.deviceMotionAvailable) return
        motionManager.startDeviceMotionUpdatesToQueue(operationQueue) { motion, _ ->
            motion ?: return@startDeviceMotionUpdatesToQueue
            val attitude = motion.attitude
            val nowMs = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
            val latG = motion.userAcceleration.useContents { x.toFloat() }
            val longG = motion.userAcceleration.useContents { z.toFloat() }
            _flow.tryEmit(
                MotionData(
                    pitchDeg = (attitude.pitch * RAD_TO_DEG).toFloat(),
                    rollDeg = (attitude.roll * RAD_TO_DEG).toFloat(),
                    lateralG = latG,
                    longG = longG,
                    timestamp = nowMs,
                )
            )
        }
    }

    override fun stop() {
        if (motionManager.deviceMotionActive) {
            motionManager.stopDeviceMotionUpdates()
        }
    }
}
