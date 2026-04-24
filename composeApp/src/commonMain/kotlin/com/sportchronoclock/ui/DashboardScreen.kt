package com.sportchronoclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sportchronoclock.MainViewModel
import com.sportchronoclock.location.LocationData
import com.sportchronoclock.location.LocationProvider
import com.sportchronoclock.permissions.PermissionHandler
import org.koin.compose.koinInject

@Composable
fun DashboardScreen(
    permissionHandler: PermissionHandler = koinInject()
) {
    val locationProvider = koinInject<LocationProvider>()
    val viewModel: MainViewModel = viewModel { MainViewModel(locationProvider) }

    val speedKmh by viewModel.speedKmh.collectAsState()
    val locationData by viewModel.locationData.collectAsState()

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                SpeedometerGauge(
                    speedKmh = speedKmh,
                    modifier = Modifier.fillMaxHeight().weight(0.4f)
                )
                MapOrPlaceholder(
                    locationData = locationData,
                    statusMessage = statusMessage,
                    modifier = Modifier.fillMaxHeight().weight(0.6f)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                SpeedometerGauge(
                    speedKmh = speedKmh,
                    modifier = Modifier.fillMaxWidth().weight(0.4f)
                )
                MapOrPlaceholder(
                    locationData = locationData,
                    statusMessage = statusMessage,
                    modifier = Modifier.fillMaxWidth().weight(0.6f)
                )
            }
        }
    }
}

@Composable
private fun MapOrPlaceholder(
    locationData: LocationData?,
    statusMessage: String,
    modifier: Modifier
) {
    if (locationData != null) {
        MapView(
            latitude = locationData.latitude,
            longitude = locationData.longitude,
            bearing = maxOf(0f, locationData.bearing),
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF111111)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = statusMessage, color = Color.White, fontSize = 16.sp)
        }
    }
}
