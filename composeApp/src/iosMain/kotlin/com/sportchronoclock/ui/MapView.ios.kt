@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.sportchronoclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*

@Composable
actual fun MapView(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    modifier: Modifier
) {
    UIKitView(
        modifier = modifier,
        factory = {
            MKMapView().also { map ->
                map.showsUserLocation = true
                map.pitchEnabled = false
                map.rotateEnabled = false
            }
        },
        update = { map ->
            val coordinate = CLLocationCoordinate2DMake(latitude, longitude)
            val camera = MKMapCamera.cameraLookingAtCenterCoordinate(
                centerCoordinate = coordinate,
                fromDistance = 500.0,
                pitch = 0.0,
                heading = bearing.toDouble()
            )
            map.setCamera(camera, animated = true)
        }
    )
}
