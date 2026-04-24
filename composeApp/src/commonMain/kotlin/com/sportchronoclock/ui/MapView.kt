package com.sportchronoclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun MapView(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    modifier: Modifier = Modifier
)
