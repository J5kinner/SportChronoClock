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

    private val _distanceToNextTurnMeters = MutableStateFlow<Double?>(null)
    val distanceToNextTurnMeters: StateFlow<Double?> = _distanceToNextTurnMeters.asStateFlow()

    private val alpha = 0.8f
    private var filteredSpeed = 0f
    private var currentStepIndex = 0
    private var suggestionJob: Job? = null

    private var routeDestination: Pair<Double, Double>? = null
    private var lastRerouteMs: Long = 0
    private var isRerouting: Boolean = false

    init {
        viewModelScope.launch {
            locationProvider.locationFlow.collect { data ->
                _locationData.value = data
                if (data.hasSpeed) {
                    val rawKmh = maxOf(0f, data.speed) * 3.6f
                    filteredSpeed = alpha * rawKmh + (1f - alpha) * filteredSpeed
                    _speedKmh.value = filteredSpeed
                }
                // Step advancement, distance, and reroute run on every tick regardless of speed fix
                advanceStepIfNeeded(data)
                updateDistanceToNextTurn(data)
                checkOffRoute(data)
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
                        routeDestination = result.points.last()
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
                        routeDestination = result.points.last()
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
        _distanceToNextTurnMeters.value = null
        routeDestination = null
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
                    routeDestination = result.points.last()
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
        _distanceToNextTurnMeters.value = null
        routeDestination = null
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
        if (dist < 50.0) {
            currentStepIndex++
            _currentStep.value = steps.getOrNull(currentStepIndex)
        }
    }

    private fun updateDistanceToNextTurn(location: LocationData) {
        val steps = _routeResult.value?.steps
        if (steps == null) {
            _distanceToNextTurnMeters.value = null
            return
        }
        val nextIndex = currentStepIndex + 1
        if (nextIndex >= steps.size) {
            _distanceToNextTurnMeters.value = null
            return
        }
        val next = steps[nextIndex]
        if (next.coordinate.first == 0.0 && next.coordinate.second == 0.0) {
            _distanceToNextTurnMeters.value = null
            return
        }
        _distanceToNextTurnMeters.value = haversineMeters(
            location.latitude, location.longitude,
            next.coordinate.first, next.coordinate.second
        )
    }

    private fun checkOffRoute(location: LocationData) {
        val dest = routeDestination ?: return
        if (_routeResult.value == null || isRerouting) return
        if (distanceToRouteMeters(location) > 50.0 &&
            (location.timestamp - lastRerouteMs) > 15_000L) {
            triggerReroute(location, dest)
        }
    }

    private fun triggerReroute(location: LocationData, destination: Pair<Double, Double>) {
        isRerouting = true
        lastRerouteMs = location.timestamp
        viewModelScope.launch {
            directionsService.routeToCoordinates(
                location.latitude, location.longitude,
                destination.first, destination.second
            ).onSuccess { result ->
                _routeResult.value = result
                routeDestination = result.points.last()
                currentStepIndex = 0
                _currentStep.value = result.steps.firstOrNull()
            }.also {
                isRerouting = false
            }
        }
    }

    private fun distanceToRouteMeters(location: LocationData): Double {
        val points = _routeResult.value?.points ?: return 0.0
        if (points.size < 2) return 0.0
        return points.zipWithNext { a, b ->
            distanceToSegmentMeters(
                location.latitude, location.longitude,
                a.first, a.second,
                b.first, b.second
            )
        }.minOrNull() ?: Double.MAX_VALUE
    }

    private fun distanceToSegmentMeters(
        px: Double, py: Double,
        ax: Double, ay: Double,
        bx: Double, by: Double
    ): Double {
        val dx = bx - ax
        val dy = by - ay
        if (dx == 0.0 && dy == 0.0) return haversineMeters(px, py, ax, ay)
        val t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)
        val clamped = t.coerceIn(0.0, 1.0)
        return haversineMeters(px, py, ax + clamped * dx, ay + clamped * dy)
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
