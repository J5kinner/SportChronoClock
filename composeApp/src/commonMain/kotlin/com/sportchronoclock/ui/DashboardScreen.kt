package com.sportchronoclock.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sportchronoclock.MainViewModel
import com.sportchronoclock.SearchState
import com.sportchronoclock.location.LocationData
import com.sportchronoclock.location.LocationProvider
import com.sportchronoclock.navigation.DirectionsService
import com.sportchronoclock.navigation.RouteResult
import com.sportchronoclock.permissions.PermissionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun DashboardScreen(
    permissionHandler: PermissionHandler = koinInject()
) {
    val locationProvider = koinInject<LocationProvider>()
    val directionsService = koinInject<DirectionsService>()
    val viewModel: MainViewModel = viewModel { MainViewModel(locationProvider, directionsService) }

    val speedKmh by viewModel.speedKmh.collectAsState()
    val locationData by viewModel.locationData.collectAsState()
    val routeResult by viewModel.routeResult.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val pinLocation by viewModel.pinLocation.collectAsState()

    var hasPermission by remember { mutableStateOf(permissionHandler.hasLocationPermission()) }
    var permissionDenied by remember { mutableStateOf(false) }

    // Demo state — lives here, not in the gauge
    val demoScope = rememberCoroutineScope()
    var demoSpeed by remember { mutableStateOf(0f) }
    var isDemoActive by remember { mutableStateOf(false) }
    var demoRampJob by remember { mutableStateOf<Job?>(null) }
    var autoSweepDone by remember { mutableStateOf(false) }
    val sweepAnimatable = remember { Animatable(0f) }

    // One-shot sweep animation on first composition
    LaunchedEffect(Unit) {
        sweepAnimatable.animateTo(200f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
        sweepAnimatable.animateTo(0f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
        autoSweepDone = true
    }

    val displaySpeed = when {
        isDemoActive || demoSpeed > 0f -> demoSpeed
        !autoSweepDone -> sweepAnimatable.value
        else -> speedKmh
    }

    KeepScreenOn()

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.startTracking()
    }

    if (!hasPermission && !permissionDenied) {
        RequestLocationPermission(
            onGranted = { hasPermission = true },
            onDenied = { permissionDenied = true }
        )
    }

    val statusMessage = when {
        permissionDenied -> "Location permission denied.\nPlease enable it in Settings."
        !hasPermission -> "Requesting location permission…"
        else -> "Acquiring GPS signal…"
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight

            val onDemoPress: () -> Unit = {
                demoRampJob?.cancel()
                isDemoActive = true
                demoSpeed = 0f
                demoRampJob = demoScope.launch {
                    while (demoSpeed < 200f) {
                        demoSpeed = (demoSpeed + 0.5f).coerceAtMost(200f)
                        delay(16)
                    }
                }
            }
            val onDemoRelease: () -> Unit = {
                demoRampJob?.cancel()
                isDemoActive = false
                demoRampJob = demoScope.launch {
                    while (demoSpeed > 0f) {
                        demoSpeed = (demoSpeed - 0.5f).coerceAtLeast(0f)
                        delay(16)
                    }
                }
            }

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    SpeedometerPanel(
                        displaySpeed = displaySpeed,
                        onDemoPress = onDemoPress,
                        onDemoRelease = onDemoRelease,
                        modifier = Modifier.fillMaxHeight().weight(0.4f)
                    )
                    MapPanel(
                        locationData = locationData,
                        routeResult = routeResult,
                        searchState = searchState,
                        pinLocation = pinLocation,
                        onSearch = { viewModel.searchRoute(it) },
                        onClear = { viewModel.clearRoute() },
                        onLongPress = { lat, lng -> viewModel.setPinLocation(lat, lng) },
                        onDirectionsRequested = {
                            pinLocation?.let { (lat, lng) -> viewModel.routeToPin(lat, lng) }
                        },
                        statusMessage = statusMessage,
                        modifier = Modifier.fillMaxHeight().weight(0.6f)
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    SpeedometerPanel(
                        displaySpeed = displaySpeed,
                        onDemoPress = onDemoPress,
                        onDemoRelease = onDemoRelease,
                        modifier = Modifier.fillMaxWidth().weight(0.4f)
                    )
                    MapPanel(
                        locationData = locationData,
                        routeResult = routeResult,
                        searchState = searchState,
                        pinLocation = pinLocation,
                        onSearch = { viewModel.searchRoute(it) },
                        onClear = { viewModel.clearRoute() },
                        onLongPress = { lat, lng -> viewModel.setPinLocation(lat, lng) },
                        onDirectionsRequested = {
                            pinLocation?.let { (lat, lng) -> viewModel.routeToPin(lat, lng) }
                        },
                        statusMessage = statusMessage,
                        modifier = Modifier.fillMaxWidth().weight(0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedometerPanel(
    displaySpeed: Float,
    onDemoPress: () -> Unit,
    onDemoRelease: () -> Unit,
    modifier: Modifier
) {
    val currentOnDemoPress = rememberUpdatedState(onDemoPress)
    val currentOnDemoRelease = rememberUpdatedState(onDemoRelease)

    Box(modifier = modifier) {
        SpeedometerGauge(speedKmh = displaySpeed, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF0057B8), Color(0xFF003d8a))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(1.dp, Color(0xFF0066cc), RoundedCornerShape(24.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                currentOnDemoPress.value()
                                tryAwaitRelease()
                                currentOnDemoRelease.value()
                            }
                        )
                    }
                    .padding(horizontal = 28.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "HOLD TO DEMO",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
private fun MapPanel(
    locationData: LocationData?,
    routeResult: RouteResult?,
    searchState: SearchState,
    pinLocation: Pair<Double, Double>?,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onLongPress: (Double, Double) -> Unit,
    onDirectionsRequested: () -> Unit,
    statusMessage: String,
    modifier: Modifier
) {
    if (locationData != null) {
        Box(modifier = modifier) {
            MapView(
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                bearing = maxOf(0f, locationData.bearing),
                routePoints = routeResult?.points ?: emptyList(),
                pinLocation = pinLocation,
                onLongPress = onLongPress,
                onDirectionsRequested = onDirectionsRequested,
                modifier = Modifier.fillMaxSize()
            )
            // Search bar at bottom — replaced by route summary when navigating
            if (routeResult != null) {
                RouteSummaryBar(
                    routeResult = routeResult,
                    onClear = onClear,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            } else {
                SearchBar(
                    searchState = searchState,
                    onSearch = onSearch,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            }
        }
    } else {
        Box(modifier = modifier.background(Color(0xFF111111)), contentAlignment = Alignment.Center) {
            Text(text = statusMessage, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun SearchBar(
    searchState: SearchState,
    onSearch: (String) -> Unit,
    modifier: Modifier
) {
    var query by remember { mutableStateOf("") }
    val isLoading = searchState is SearchState.Loading
    val errorMessage = (searchState as? SearchState.Error)?.message

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xCC000000), RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search destination…", fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    if (query.isNotBlank()) onSearch(query)
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF00B4D8),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray
                ),
                shape = RoundedCornerShape(8.dp)
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color(0xFF00B4D8),
                    strokeWidth = 2.dp
                )
            } else {
                TextButton(
                    onClick = { if (query.isNotBlank()) onSearch(query) },
                    enabled = query.isNotBlank()
                ) {
                    Text("Go", color = Color(0xFF00B4D8), fontWeight = FontWeight.Bold)
                }
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color(0xFFFF5252),
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun RouteSummaryBar(
    routeResult: RouteResult,
    onClear: () -> Unit,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xE6000000), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF1E88E5), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "%.1f km · %d min".format(routeResult.distanceKm, routeResult.durationMinutes),
                color = Color(0xFF1E88E5),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = routeResult.destinationName.take(50),
                color = Color(0xFF445566),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
        TextButton(onClick = onClear) {
            Text(
                text = "✕ CLEAR",
                color = Color(0xFFC8102E),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
