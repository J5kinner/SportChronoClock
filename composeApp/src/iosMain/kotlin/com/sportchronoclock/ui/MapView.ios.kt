@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.sportchronoclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.*
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.UIKit.UIColor
import platform.darwin.NSObject

private class RouteMapDelegate : NSObject(), MKMapViewDelegateProtocol {
    override fun mapView(mapView: MKMapView, rendererForOverlay: MKOverlayProtocol): MKOverlayRenderer {
        if (rendererForOverlay is MKPolyline) {
            return MKPolylineRenderer(rendererForOverlay as MKPolyline).apply {
                strokeColor = UIColor(red = 0.118, green = 0.533, blue = 0.898, alpha = 1.0)
                lineWidth = 6.0
            }
        }
        return MKOverlayRenderer(rendererForOverlay)
    }
}

@Composable
actual fun MapView(
    latitude: Double,
    longitude: Double,
    bearing: Float,
    routePoints: List<Pair<Double, Double>>,
    modifier: Modifier
) {
    // Keep delegate alive for the lifetime of this composable
    val delegate = remember { RouteMapDelegate() }

    UIKitView(
        modifier = modifier,
        factory = {
            MKMapView().also { map ->
                map.delegate = delegate
                map.showsUserLocation = true
                map.pitchEnabled = false
                map.rotateEnabled = false
            }
        },
        update = { map ->
            // Update camera
            val coordinate = CLLocationCoordinate2DMake(latitude, longitude)
            val camera = MKMapCamera.cameraLookingAtCenterCoordinate(
                centerCoordinate = coordinate,
                fromDistance = 500.0,
                pitch = 0.0,
                heading = bearing.toDouble()
            )
            map.setCamera(camera, animated = true)

            // Remove existing route overlays
            map.overlays.filterIsInstance<MKPolyline>().forEach {
                map.removeOverlay(it)
            }

            // Draw new route if we have points
            if (routePoints.isNotEmpty()) {
                memScoped {
                    val coords = allocArray<CLLocationCoordinate2D>(routePoints.size)
                    routePoints.forEachIndexed { i, (lat, lon) ->
                        coords[i].latitude = lat
                        coords[i].longitude = lon
                    }
                    val polyline = MKPolyline.polylineWithCoordinates(coords, routePoints.size.toULong())
                    map.addOverlay(polyline)
                }
            }
        }
    )
}
