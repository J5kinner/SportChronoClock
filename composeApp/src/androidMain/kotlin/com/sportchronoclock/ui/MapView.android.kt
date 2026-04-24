package com.sportchronoclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
actual fun MapView(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    modifier: Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(latitude, longitude), 17f)
    }

    LaunchedEffect(latitude, longitude, bearing) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(latitude, longitude))
                    .zoom(17f)
                    .bearing(bearing)
                    .tilt(30f)
                    .build()
            ),
            durationMs = 800
        )
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState
    )
}
