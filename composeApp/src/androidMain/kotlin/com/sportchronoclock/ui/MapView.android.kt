package com.sportchronoclock.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val ROUTE_SOURCE = "route-source"
private const val ROUTE_LAYER = "route-layer"

@Composable
actual fun MapView(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    routePoints: List<Pair<Double, Double>>,
    pinLocation: Pair<Double, Double>?,
    onLongPress: (lat: Double, lng: Double) -> Unit,
    onDirectionsRequested: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnLongPress = rememberUpdatedState(onLongPress)
    val currentOnDirectionsRequested = rememberUpdatedState(onDirectionsRequested)
    val pinMarkerRef = remember { arrayOfNulls<org.maplibre.android.annotations.Marker>(1) }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    // Load style + register listeners once
    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(STYLE_URL))
            map.uiSettings.apply {
                isRotateGesturesEnabled = false
                isCompassEnabled = false
            }
            map.addOnMapLongClickListener { latLng ->
                currentOnLongPress.value(latLng.latitude, latLng.longitude)
                true
            }
            map.setOnInfoWindowClickListener { _ ->
                currentOnDirectionsRequested.value()
                true
            }
        }
    }

    // Animate camera to current position
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

    // Draw or clear route polyline
    LaunchedEffect(routePoints) {
        mapView.getMapAsync { map ->
            map.getStyle { style ->
                style.removeLayer(ROUTE_LAYER)
                style.removeSource(ROUTE_SOURCE)
                if (routePoints.isNotEmpty()) {
                    val coords = routePoints.joinToString(",") { (lat, lon) -> "[$lon,$lat]" }
                    val geoJson = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coords]},"properties":{}}]}"""
                    style.addSource(GeoJsonSource(ROUTE_SOURCE, geoJson))
                    style.addLayer(
                        LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                            PropertyFactory.lineColor("#1E88E5"),
                            PropertyFactory.lineWidth(6f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                        )
                    )
                }
            }
        }
    }

    // Add or remove the pin marker; tapping its info-window fires onDirectionsRequested
    LaunchedEffect(pinLocation) {
        mapView.getMapAsync { map ->
            pinMarkerRef[0]?.let { map.removeMarker(it) }
            pinMarkerRef[0] = null
            if (pinLocation != null) {
                val (lat, lng) = pinLocation
                pinMarkerRef[0] = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(lat, lng))
                        .title("Get Directions")
                )
            }
        }
    }

    // Forward Android lifecycle events
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
