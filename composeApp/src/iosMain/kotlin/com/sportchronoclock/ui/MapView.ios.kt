@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.sportchronoclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.*
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSSelectorFromString
import platform.MapKit.*
import platform.UIKit.*
import platform.darwin.NSObject

private class MapDelegate(
    var onDirectionsRequested: () -> Unit = {},
    var onLongPress: (Double, Double) -> Unit = { _, _ -> }
) : NSObject(), MKMapViewDelegateProtocol {

    override fun mapView(
        mapView: MKMapView,
        rendererForOverlay: MKOverlayProtocol
    ): MKOverlayRenderer {
        if (rendererForOverlay is MKPolyline) {
            return MKPolylineRenderer(rendererForOverlay as MKPolyline).apply {
                strokeColor = UIColor(red = 0.118, green = 0.533, blue = 0.898, alpha = 1.0)
                lineWidth = 6.0
            }
        }
        return MKOverlayRenderer(rendererForOverlay)
    }

    override fun mapView(
        mapView: MKMapView,
        viewForAnnotation: MKAnnotationProtocol
    ): MKAnnotationView? {
        if (viewForAnnotation is MKUserLocation) return null
        return MKPinAnnotationView(
            annotation = viewForAnnotation,
            reuseIdentifier = "pin"
        ).apply {
            canShowCallout = true
            pinTintColor = UIColor.redColor
            rightCalloutAccessoryView = (UIButton.buttonWithType(UIButtonTypeSystem) as UIButton).apply {
                    setTitle("Get Directions", forState = UIControlStateNormal)
                    sizeToFit()
                }
        }
    }

    override fun mapView(
        mapView: MKMapView,
        annotationView: MKAnnotationView,
        calloutAccessoryControlTapped: UIControl
    ) {
        onDirectionsRequested()
    }

    @ObjCAction
    fun handleLongPress(recognizer: UILongPressGestureRecognizer) {
        if (recognizer.state == UIGestureRecognizerStateBegan) {
            val mapView = recognizer.view as? MKMapView ?: return
            val point = recognizer.locationInView(mapView)
            val coordinate = mapView.convertPoint(point, toCoordinateFromView = mapView)
            coordinate.useContents { onLongPress(latitude, longitude) }
        }
    }
}

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
    val delegate = remember { MapDelegate() }
    val currentOnLongPress = rememberUpdatedState(onLongPress)
    val currentOnDirectionsRequested = rememberUpdatedState(onDirectionsRequested)

    UIKitView(
        modifier = modifier,
        factory = {
            MKMapView().also { map ->
                map.delegate = delegate
                map.showsUserLocation = true
                map.pitchEnabled = false
                map.rotateEnabled = false
                val recognizer = UILongPressGestureRecognizer(
                    target = delegate,
                    action = NSSelectorFromString("handleLongPress:")
                ).apply { minimumPressDuration = 0.5 }
                map.addGestureRecognizer(recognizer)
            }
        },
        update = { map ->
            // Keep delegate callbacks current without re-running factory
            delegate.onLongPress = { lat, lng -> currentOnLongPress.value(lat, lng) }
            delegate.onDirectionsRequested = { currentOnDirectionsRequested.value() }

            // Camera
            val coordinate = CLLocationCoordinate2DMake(latitude, longitude)
            val camera = MKMapCamera.cameraLookingAtCenterCoordinate(
                centerCoordinate = coordinate,
                fromDistance = 500.0,
                pitch = 0.0,
                heading = bearing.toDouble()
            )
            map.setCamera(camera, animated = true)

            // Route overlays
            map.overlays.filterIsInstance<MKPolyline>().forEach { map.removeOverlay(it) }
            if (routePoints.isNotEmpty()) {
                memScoped {
                    val coords = allocArray<CLLocationCoordinate2D>(routePoints.size)
                    routePoints.forEachIndexed { i, (lat, lon) ->
                        coords[i].latitude = lat
                        coords[i].longitude = lon
                    }
                    map.addOverlay(
                        MKPolyline.polylineWithCoordinates(coords, routePoints.size.toULong())
                    )
                }
            }

            // Pin annotation — remove any existing, add new one if set
            map.annotations.filterIsInstance<MKPointAnnotation>().forEach {
                map.removeAnnotation(it)
            }
            if (pinLocation != null) {
                val (lat, lng) = pinLocation
                val annotation = MKPointAnnotation()
                annotation.setCoordinate(CLLocationCoordinate2DMake(lat, lng))
                annotation.title = "Pin"
                map.addAnnotation(annotation)
            }
        }
    )
}
