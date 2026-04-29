package com.sportchronoclock.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.round
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.sportchronoclock.navigation.NavigationStep
import com.sportchronoclock.navigation.PlaceSuggestion
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
    val suggestions by viewModel.suggestions.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val distanceToNextTurn by viewModel.distanceToNextTurnMeters.collectAsState()

    var hasPermission by remember { mutableStateOf(permissionHandler.hasLocationPermission()) }
    var permissionDenied by remember { mutableStateOf(false) }

    // Demo state — lives here, not in the gauge
    val demoScope = rememberCoroutineScope()
    var demoSpeed by remember { mutableStateOf(0f) }
    var isDemoActive by remember { mutableStateOf(false) }
    val demoRampJob = remember { arrayOfNulls<Job>(1) }
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
                demoRampJob[0]?.cancel()
                isDemoActive = true
                demoSpeed = 0f
                demoRampJob[0] = demoScope.launch {
                    while (demoSpeed < 200f) {
                        demoSpeed = (demoSpeed + 0.5f).coerceAtMost(200f)
                        delay(16)
                    }
                }
            }
            val onDemoRelease: () -> Unit = {
                demoRampJob[0]?.cancel()
                isDemoActive = false
                demoRampJob[0] = demoScope.launch {
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
                        currentStep = currentStep,
                        distanceToNextTurn = distanceToNextTurn,
                        searchState = searchState,
                        suggestions = suggestions,
                        pinLocation = pinLocation,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onSearch = { viewModel.searchRoute(it) },
                        onSuggestionSelected = { viewModel.routeToSuggestion(it) },
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
                        currentStep = currentStep,
                        distanceToNextTurn = distanceToNextTurn,
                        searchState = searchState,
                        suggestions = suggestions,
                        pinLocation = pinLocation,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onSearch = { viewModel.searchRoute(it) },
                        onSuggestionSelected = { viewModel.routeToSuggestion(it) },
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
    currentStep: NavigationStep?,
    distanceToNextTurn: Double?,
    searchState: SearchState,
    suggestions: List<PlaceSuggestion>,
    pinLocation: Pair<Double, Double>?,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSuggestionSelected: (PlaceSuggestion) -> Unit,
    onClear: () -> Unit,
    onLongPress: (Double, Double) -> Unit,
    onDirectionsRequested: () -> Unit,
    statusMessage: String,
    modifier: Modifier
) {
    var isFollowingRider by remember { mutableStateOf(true) }

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
                isFollowingRider = isFollowingRider,
                onUserInteraction = { isFollowingRider = false },
                modifier = Modifier.fillMaxSize()
            )
            if (!isFollowingRider) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color(0xCC000000), CircleShape)
                        .clickable { isFollowingRider = true }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⊙", color = Color(0xFF00B4D8), fontSize = 20.sp)
                }
            }
            if (routeResult != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (currentStep != null) {
                        NavigationStepBar(step = currentStep, distanceToNextTurn = distanceToNextTurn)
                    }
                    RouteSummaryBar(
                        routeResult = routeResult,
                        onClear = onClear
                    )
                }
            } else {
                SearchBar(
                    searchState = searchState,
                    suggestions = suggestions,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    onSuggestionSelected = onSuggestionSelected,
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
private fun NavigationStepBar(step: NavigationStep, distanceToNextTurn: Double?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xF0071420), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF00B4D8), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = maneuverIcon(step.maneuverType, step.maneuverModifier),
            fontSize = 26.sp,
            color = Color(0xFF00B4D8)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.instruction,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
            if (distanceToNextTurn != null && distanceToNextTurn <= 500.0) {
                val rounded = ((distanceToNextTurn / 10.0).roundToInt() * 10)
                Text(
                    text = "in ${rounded}m",
                    color = Color(0xFF00B4D8),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchState: SearchState,
    suggestions: List<PlaceSuggestion>,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSuggestionSelected: (PlaceSuggestion) -> Unit,
    modifier: Modifier
) {
    var query by remember { mutableStateOf("") }
    val isLoading = searchState is SearchState.Loading
    val errorMessage = (searchState as? SearchState.Error)?.message

    Column(modifier = modifier.fillMaxWidth()) {
        if (suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xF0111111), RoundedCornerShape(12.dp))
                    .padding(vertical = 4.dp)
            ) {
                suggestions.forEachIndexed { index, suggestion ->
                    val displayName = suggestion.name.split(",")
                        .take(2).joinToString(", ").trim()
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                query = ""
                                onSuggestionSelected(suggestion)
                            }
                            .padding(horizontal = 16.dp, vertical = 11.dp)
                    )
                    if (index < suggestions.lastIndex) {
                        HorizontalDivider(
                            color = Color(0xFF2A2A2A),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color(0xFFFF5252),
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

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
                onValueChange = {
                    query = it
                    onQueryChange(it)
                },
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
    }
}

@Composable
private fun RouteSummaryBar(
    routeResult: RouteResult,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xE6000000), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF1E88E5), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "${round(routeResult.distanceKm * 10) / 10} km · ${routeResult.durationMinutes} min",
                color = Color(0xFF1E88E5),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = routeResult.destinationName.split(",").take(2).joinToString(", ").trim().take(50),
                color = Color(0xFF8899AA),
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

private fun maneuverIcon(type: String, modifier: String?): String = when (type) {
    "depart" -> "▶"
    "arrive" -> "⬤"
    "turn" -> when (modifier) {
        "left" -> "↰"
        "right" -> "↱"
        "slight left" -> "↖"
        "slight right" -> "↗"
        "sharp left" -> "↩"
        "sharp right" -> "↪"
        "uturn" -> "↩"
        else -> "↑"
    }
    "merge" -> "⤵"
    "on ramp", "ramp" -> "↗"
    "off ramp" -> "↘"
    "fork" -> when (modifier) {
        "left" -> "↰"
        "right" -> "↱"
        else -> "↑"
    }
    "roundabout", "rotary", "roundabout turn", "exit roundabout" -> "↻"
    else -> "↑"
}

private fun formatDistance(meters: Double): String = when {
    meters >= 1000 -> "${round(meters / 100) / 10} km"
    else -> "${meters.toInt()} m"
}
