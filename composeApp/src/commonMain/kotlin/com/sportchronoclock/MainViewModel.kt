package com.sportchronoclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportchronoclock.location.LocationData
import com.sportchronoclock.location.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _speedKmh = MutableStateFlow(0f)
    val speedKmh: StateFlow<Float> = _speedKmh.asStateFlow()

    private val _locationData = MutableStateFlow<LocationData?>(null)
    val locationData: StateFlow<LocationData?> = _locationData.asStateFlow()

    // Low-pass filter: higher alpha = more responsive, lower = smoother
    private val alpha = 0.15f
    private var filteredSpeed = 0f

    init {
        viewModelScope.launch {
            locationProvider.locationFlow.collect { data ->
                _locationData.value = data
                val rawKmh = maxOf(0f, data.speed) * 3.6f
                filteredSpeed = alpha * rawKmh + (1f - alpha) * filteredSpeed
                _speedKmh.value = filteredSpeed
            }
        }
    }

    fun startTracking() = locationProvider.startTracking()

    fun stopTracking() = locationProvider.stopTracking()

    override fun onCleared() {
        super.onCleared()
        locationProvider.stopTracking()
    }
}
