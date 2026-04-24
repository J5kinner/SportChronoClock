package com.sportchronoclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun MapView(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    routePoints: List<Pair<Double, Double>> = emptyList(),
    pinLocation: Pair<Double, Double>? = null,
    onLongPress: (lat: Double, lng: Double) -> Unit = { _, _ -> },
    onDirectionsRequested: () -> Unit = {},
    modifier: Modifier = Modifier
)
