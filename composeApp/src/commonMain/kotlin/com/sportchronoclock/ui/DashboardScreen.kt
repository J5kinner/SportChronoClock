package com.sportchronoclock.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportchronoclock.MainViewModel
import com.sportchronoclock.SearchState
import com.sportchronoclock.location.LocationData
import com.sportchronoclock.navigation.NavigationStep
import com.sportchronoclock.navigation.PlaceSuggestion
import com.sportchronoclock.navigation.RouteResult
import com.sportchronoclock.media.MediaControlViewModel
import com.sportchronoclock.permissions.PermissionHandler
import com.sportchronoclock.ride.RideStatsViewModel
import com.sportchronoclock.settings.SettingsViewModel
import com.sportchronoclock.settings.SpeedUnits
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen(
    onOpenHistory: () -> Unit,
    onOpenSport: () -> Unit,
    onOpenSettings: () -> Unit,
    permissionHandler: PermissionHandler = koinInject(),
) {
    val viewModel: MainViewModel = koinViewModel()
    val statsViewModel: RideStatsViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val mediaViewModel: MediaControlViewModel = koinViewModel()
    val liveStats by statsViewModel.liveStats.collectAsState()
    val settings by settingsViewModel.state.collectAsState()
    val mediaInfo by mediaViewModel.info.collectAsState()
    val mediaAccess by mediaViewModel.accessState.collectAsState()

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

    var autoSweepDone by remember { mutableStateOf(false) }
    val sweepAnimatable = remember { Animatable(0f) }

    // One-shot sweep animation on first composition
    LaunchedEffect(Unit) {
        sweepAnimatable.animateTo(200f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
        sweepAnimatable.animateTo(0f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
        autoSweepDone = true
    }

    val displaySpeed = if (!autoSweepDone) sweepAnimatable.value else speedKmh

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
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(top = 0.dp)) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    SpeedometerPanel(
                        displaySpeed = displaySpeed,
                        units = settings.speedUnits,
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
                // Portrait: map HUD on top, speedo below — matches motorbike dashboard convention
                Column(modifier = Modifier.fillMaxSize()) {
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
                    SpeedometerPanel(
                        displaySpeed = displaySpeed,
                        units = settings.speedUnits,
                        modifier = Modifier.fillMaxWidth().weight(0.4f)
                    )
                }
            }
        }

        // Top-center stack: TripHudChip + MediaTile (each conditional)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp, start = 80.dp, end = 80.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            liveStats?.let { stats -> TripHudChip(stats = stats) }
            if (mediaInfo.hasSession || mediaAccess.needsOnboarding) {
                MediaTile()
            }
        }

        // Top-left controls: history + sport mode
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xCC000000), CircleShape)
                    .clickable { onOpenHistory() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "≡",
                    color = Color(0xFF00B4D8),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                modifier = Modifier
                    .background(Color(0xCC000000), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFC8102E), RoundedCornerShape(20.dp))
                    .clickable { onOpenSport() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SPORT",
                    color = Color(0xFFC8102E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
            }
            Box(
                modifier = Modifier
                    .background(Color(0xCC000000), CircleShape)
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚙",
                    color = Color(0xFF00B4D8),
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun SpeedometerPanel(
    displaySpeed: Float,
    units: SpeedUnits,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        SpeedometerGauge(
            speedKmh = displaySpeed,
            units = units,
            modifier = Modifier.fillMaxSize(),
        )
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
