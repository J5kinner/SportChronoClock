package com.sportchronoclock.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

// OpenFreeMap — completely free, no API key required
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

@Composable
actual fun MapView(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    modifier: Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    // Load style once on first composition
    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(STYLE_URL))
            map.uiSettings.apply {
                isRotateGesturesEnabled = false
                isCompassEnabled = false
            }
        }
    }

    // Animate camera whenever position or bearing changes
    LaunchedEffect(latitude, longitude, bearing) {
        mapView.getMapAsync { map ->
            val target = CameraPosition.Builder()
                .target(LatLng(latitude, longitude))
                .zoom(16.0)
                .bearing(bearing.toDouble())
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(target), 800)
        }
    }

    // Forward Android lifecycle events so tiles load/unload correctly
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(modifier = modifier, factory = { mapView })
}
