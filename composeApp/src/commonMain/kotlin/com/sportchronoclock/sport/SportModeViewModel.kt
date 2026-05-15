package com.sportchronoclock.sport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportchronoclock.ride.RideEvent
import com.sportchronoclock.ride.RideEventBus
import com.sportchronoclock.sensors.MotionData
import com.sportchronoclock.sensors.SensorProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class SportModeViewModel(
    private val sensorProvider: SensorProvider,
    private val rideEventBus: RideEventBus,
) : ViewModel() {

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _motion = MutableStateFlow<MotionData?>(null)
    val motion: StateFlow<MotionData?> = _motion.asStateFlow()

    private val _peakLeanDeg = MutableStateFlow(0f)
    val peakLeanDeg: StateFlow<Float> = _peakLeanDeg.asStateFlow()

    private val _peakLateralG = MutableStateFlow(0f)
    val peakLateralG: StateFlow<Float> = _peakLateralG.asStateFlow()

    private var zeroRoll = 0f
    private var zeroPitch = 0f
    private var lastRaw: MotionData? = null
    private var collectJob: Job? = null

    fun activate() {
        if (_isActive.value) return
        _isActive.value = true
        sensorProvider.start()
        collectJob = viewModelScope.launch {
            sensorProvider.motionFlow.collect { raw ->
                lastRaw = raw
                val adjusted = MotionData(
                    pitchDeg = raw.pitchDeg - zeroPitch,
                    rollDeg = raw.rollDeg - zeroRoll,
                    lateralG = raw.lateralG,
                    longG = raw.longG,
                    timestamp = raw.timestamp,
                )
                _motion.value = adjusted
                val absLean = abs(adjusted.rollDeg)
                if (absLean > _peakLeanDeg.value) _peakLeanDeg.value = absLean
                val absG = abs(adjusted.lateralG)
                if (absG > _peakLateralG.value) _peakLateralG.value = absG

                rideEventBus.tryEmit(
                    RideEvent.MotionSample(
                        pitchDeg = adjusted.pitchDeg,
                        rollDeg = adjusted.rollDeg,
                        lateralG = adjusted.lateralG,
                        longG = adjusted.longG,
                        timestamp = adjusted.timestamp,
                    )
                )
            }
        }
    }

    fun deactivate() {
        if (!_isActive.value) return
        _isActive.value = false
        collectJob?.cancel()
        collectJob = null
        sensorProvider.stop()
        _motion.value = null
    }

    fun calibrate() {
        val raw = lastRaw ?: return
        zeroRoll = raw.rollDeg
        zeroPitch = raw.pitchDeg
        _peakLeanDeg.value = 0f
        _peakLateralG.value = 0f
    }

    override fun onCleared() {
        super.onCleared()
        deactivate()
    }
}
