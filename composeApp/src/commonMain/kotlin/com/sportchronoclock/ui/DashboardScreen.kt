package com.sportchronoclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.sportchronoclock.MainViewModel
import com.sportchronoclock.permissions.PermissionHandler
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel = koinViewModel(),
    permissionHandler: PermissionHandler = koinInject()
) {
    val speedKmh by viewModel.speedKmh.collectAsState()
    val locationData by viewModel.locationData.collectAsState()

    var hasPermission by remember { mutableStateOf(permissionHandler.hasLocationPermission()) }
    var permissionDenied by remember { mutableStateOf(false) }

    KeepScreenOn()

    // Start tracking immediately if permission is already granted
    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.startTracking()
    }

    // Request permission when not yet granted
    if (!hasPermission && !permissionDenied) {
        RequestLocationPermission(
            onGranted = {
                hasPermission = true
            },
            onDenied = {
                permissionDenied = true
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        SpeedometerGauge(
            speedKmh = speedKmh,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
        )

        val data = locationData
        if (data != null) {
            MapView(
                latitude = data.latitude,
                longitude = data.longitude,
                bearing = maxOf(0f, data.bearing),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                val message = when {
                    permissionDenied -> "Location permission denied.\nPlease enable it in Settings."
                    !hasPermission -> "Requesting location permission…"
                    else -> "Acquiring GPS signal…"
                }
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}
