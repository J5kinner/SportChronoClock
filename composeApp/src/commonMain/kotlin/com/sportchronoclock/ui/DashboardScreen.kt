package com.sportchronoclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sportchronoclock.MainViewModel
import com.sportchronoclock.SearchState
import com.sportchronoclock.location.LocationData
import com.sportchronoclock.location.LocationProvider
import com.sportchronoclock.navigation.DirectionsService
import com.sportchronoclock.navigation.RouteResult
import com.sportchronoclock.permissions.PermissionHandler
import org.koin.compose.koinInject

private val NeonYellow = Color(0xFFFFFF00)

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

    var hasPermission by remember { mutableStateOf(permissionHandler.hasLocationPermission()) }
    var permissionDenied by remember { mutableStateOf(false) }

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
        // Main content — portrait column / landscape row
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    SpeedometerGauge(speedKmh = speedKmh, modifier = Modifier.fillMaxHeight().weight(0.4f))
                    MapOrPlaceholder(locationData, routeResult, statusMessage, Modifier.fillMaxHeight().weight(0.6f))
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    SpeedometerGauge(speedKmh = speedKmh, modifier = Modifier.fillMaxWidth().weight(0.4f))
                    MapOrPlaceholder(locationData, routeResult, statusMessage, Modifier.fillMaxWidth().weight(0.6f))
                }
            }
        }

        // Floating search bar overlay
        SearchOverlay(
            routeResult = routeResult,
            searchState = searchState,
            onSearch = { viewModel.searchRoute(it) },
            onClear = { viewModel.clearRoute() },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun MapOrPlaceholder(
    locationData: LocationData?,
    routeResult: RouteResult?,
    statusMessage: String,
    modifier: Modifier
) {
    if (locationData != null) {
        MapView(
            latitude = locationData.latitude,
            longitude = locationData.longitude,
            bearing = maxOf(0f, locationData.bearing),
            routePoints = routeResult?.points ?: emptyList(),
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(Color(0xFF111111)), contentAlignment = Alignment.Center) {
            Text(text = statusMessage, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun SearchOverlay(
    routeResult: RouteResult?,
    searchState: SearchState,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier
) {
    var query by remember { mutableStateOf("") }
    val isLoading = searchState is SearchState.Loading
    val errorMessage = (searchState as? SearchState.Error)?.message

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xCC000000), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (routeResult != null) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "%.1f km · %d min".format(routeResult.distanceKm, routeResult.durationMinutes),
                        color = NeonYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = routeResult.destinationName.take(50),
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                TextButton(onClick = onClear) {
                    Text("Clear", color = NeonYellow, fontSize = 13.sp)
                }
            } else {
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
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NeonYellow,
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
                        color = NeonYellow,
                        strokeWidth = 2.dp
                    )
                } else {
                    TextButton(
                        onClick = { if (query.isNotBlank()) onSearch(query) },
                        enabled = query.isNotBlank()
                    ) {
                        Text("Go", color = NeonYellow, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color(0xFFFF5252),
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
