package com.sportchronoclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportchronoclock.location.LocationData
import com.sportchronoclock.location.LocationProvider
import com.sportchronoclock.navigation.DirectionsService
import com.sportchronoclock.navigation.RouteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val locationProvider: LocationProvider,
    private val directionsService: DirectionsService
) : ViewModel() {

    private val _speedKmh = MutableStateFlow(0f)
    val speedKmh: StateFlow<Float> = _speedKmh.asStateFlow()

    private val _locationData = MutableStateFlow<LocationData?>(null)
    val locationData: StateFlow<LocationData?> = _locationData.asStateFlow()

    private val _routeResult = MutableStateFlow<RouteResult?>(null)
    val routeResult: StateFlow<RouteResult?> = _routeResult.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _pinLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val pinLocation: StateFlow<Pair<Double, Double>?> = _pinLocation.asStateFlow()

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

    fun searchRoute(query: String) {
        val location = _locationData.value ?: run {
            _searchState.value = SearchState.Error("Waiting for GPS fix — try again in a moment")
            return
        }
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            directionsService.searchAndRoute(query, location.latitude, location.longitude)
                .onSuccess { result ->
                    _routeResult.value = result
                    _searchState.value = SearchState.Idle
                }
                .onFailure { e ->
                    _searchState.value = SearchState.Error(e.message ?: "Could not find route")
                }
        }
    }

    fun setPinLocation(lat: Double, lng: Double) {
        _pinLocation.value = lat to lng
        _routeResult.value = null
        _searchState.value = SearchState.Idle
    }

    fun routeToPin(toLat: Double, toLng: Double) {
        val location = _locationData.value ?: run {
            _searchState.value = SearchState.Error("Waiting for GPS fix — try again in a moment")
            return
        }
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            directionsService.routeToCoordinates(location.latitude, location.longitude, toLat, toLng)
                .onSuccess { result ->
                    _routeResult.value = result
                    _searchState.value = SearchState.Idle
                }
                .onFailure { e ->
                    _searchState.value = SearchState.Error(e.message ?: "Could not find route")
                }
        }
    }

    fun clearRoute() {
        _routeResult.value = null
        _pinLocation.value = null
        _searchState.value = SearchState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        locationProvider.stopTracking()
    }
}

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Error(val message: String) : SearchState
}
