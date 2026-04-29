package com.sportchronoclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportchronoclock.location.LocationData
import com.sportchronoclock.location.LocationProvider
import com.sportchronoclock.navigation.DirectionsService
import com.sportchronoclock.navigation.NavigationStep
import com.sportchronoclock.navigation.PlaceSuggestion
import com.sportchronoclock.navigation.RouteResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

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

    private val _suggestions = MutableStateFlow<List<PlaceSuggestion>>(emptyList())
    val suggestions: StateFlow<List<PlaceSuggestion>> = _suggestions.asStateFlow()

    private val _currentStep = MutableStateFlow<NavigationStep?>(null)
    val currentStep: StateFlow<NavigationStep?> = _currentStep.asStateFlow()

    private val alpha = 0.8f
    private var filteredSpeed = 0f
    private var currentStepIndex = 0
    private var suggestionJob: Job? = null

    init {
        viewModelScope.launch {
            locationProvider.locationFlow.collect { data ->
                _locationData.value = data
                // Only use speed when the platform signals a valid GPS speed fix
                if (data.hasSpeed) {
                    val rawKmh = maxOf(0f, data.speed) * 3.6f
                    filteredSpeed = alpha * rawKmh + (1f - alpha) * filteredSpeed
                    _speedKmh.value = filteredSpeed
                    advanceStepIfNeeded(data)
                }
            }
        }
    }

    fun startTracking() = locationProvider.startTracking()

    fun stopTracking() = locationProvider.stopTracking()

    fun updateSearchQuery(query: String) {
        suggestionJob?.cancel()
        if (query.length < 2) {
            _suggestions.value = emptyList()
            return
        }
        suggestionJob = viewModelScope.launch {
            delay(300)
            _suggestions.value = directionsService.suggest(query)
        }
    }

    fun routeToSuggestion(suggestion: PlaceSuggestion) {
        _suggestions.value = emptyList()
        suggestionJob?.cancel()
        val location = _locationData.value
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            if (location != null) {
                directionsService.routeToSuggestion(suggestion, location.latitude, location.longitude)
                    .onSuccess { result ->
                        _routeResult.value = result
                        currentStepIndex = 0
                        _currentStep.value = result.steps.firstOrNull()
                        _searchState.value = SearchState.Idle
                    }
                    .onFailure { e ->
                        _searchState.value = SearchState.Error(e.message ?: "Could not find route")
                    }
            } else {
                _pinLocation.value = suggestion.lat to suggestion.lon
                _searchState.value = SearchState.Idle
            }
        }
    }

    fun searchRoute(query: String) {
        _suggestions.value = emptyList()
        suggestionJob?.cancel()
        val location = _locationData.value
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            if (location != null) {
                directionsService.searchAndRoute(query, location.latitude, location.longitude)
                    .onSuccess { result ->
                        _routeResult.value = result
                        currentStepIndex = 0
                        _currentStep.value = result.steps.firstOrNull()
                        _searchState.value = SearchState.Idle
                    }
                    .onFailure { e ->
                        _searchState.value = SearchState.Error(e.message ?: "Could not find route")
                    }
            } else {
                directionsService.findPlace(query)
                    .onSuccess { place ->
                        _pinLocation.value = place.lat to place.lon
                        _searchState.value = SearchState.Idle
                    }
                    .onFailure { e ->
                        _searchState.value = SearchState.Error(e.message ?: "Could not find \"$query\"")
                    }
            }
        }
    }

    fun setPinLocation(lat: Double, lng: Double) {
        _pinLocation.value = lat to lng
        _routeResult.value = null
        _currentStep.value = null
        currentStepIndex = 0
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
                    currentStepIndex = 0
                    _currentStep.value = result.steps.firstOrNull()
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
        _currentStep.value = null
        currentStepIndex = 0
        _suggestions.value = emptyList()
        _searchState.value = SearchState.Idle
    }

    private fun advanceStepIfNeeded(location: LocationData) {
        val steps = _routeResult.value?.steps ?: return
        if (steps.isEmpty() || currentStepIndex >= steps.size - 1) return

        val next = steps[currentStepIndex + 1]
        if (next.coordinate.first == 0.0 && next.coordinate.second == 0.0) return

        val dist = haversineMeters(
            location.latitude, location.longitude,
            next.coordinate.first, next.coordinate.second
        )
        if (dist < 30.0) {
            currentStepIndex++
            _currentStep.value = steps.getOrNull(currentStepIndex)
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val toRad = PI / 180.0
        val phi1 = lat1 * toRad
        val phi2 = lat2 * toRad
        val dPhi = (lat2 - lat1) * toRad
        val dLambda = (lon2 - lon1) * toRad
        val sinHalfDPhi = sin(dPhi / 2)
        val sinHalfDLambda = sin(dLambda / 2)
        val a = sinHalfDPhi * sinHalfDPhi + cos(phi1) * cos(phi2) * sinHalfDLambda * sinHalfDLambda
        return r * 2 * atan2(sqrt(a), sqrt(1.0 - a))
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
