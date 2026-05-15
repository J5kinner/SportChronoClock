@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.sportchronoclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.*
import platform.CoreGraphics.CGAffineTransformMakeRotation
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSSelectorFromString
import platform.MapKit.*
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.math.PI

private class UserArrowAnnotation : MKPointAnnotation()

private class MapDelegate(
    var onDirectionsRequested: () -> Unit = {},
    var onLongPress: (Double, Double) -> Unit = { _, _ -> },
    var onUserInteraction: () -> Unit = {}
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
        if (viewForAnnotation is UserArrowAnnotation) {
            val view = MKAnnotationView(
                annotation = viewForAnnotation,
                reuseIdentifier = "user-arrow"
            )
            view.image = createArrowImage()
            view.canShowCallout = false
            return view
        }
        return MKPinAnnotationView(
            annotation = viewForAnnotation,
            reuseIdentifier = "pin"
        ).apply {
            canShowCallout = true
            pinTintColor = UIColor.redColor
            rightCalloutAccessoryView = (UIButton.buttonWithType(UIButtonTypeSystem) as UIButton).also {
                it.setTitle("Get Directions", forState = UIControlStateNormal)
                it.sizeToFit()
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

    @ObjCAction
    fun handleUserPan(recognizer: UIPanGestureRecognizer) {
        if (recognizer.state == UIGestureRecognizerStateBegan) {
            onUserInteraction()
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
    isFollowingRider: Boolean,
    onUserInteraction: () -> Unit,
    modifier: Modifier
) {
    val delegate = remember { MapDelegate() }
    val currentOnLongPress = rememberUpdatedState(onLongPress)
    val currentOnDirectionsRequested = rememberUpdatedState(onDirectionsRequested)
    val currentOnUserInteraction = rememberUpdatedState(onUserInteraction)
    val userArrowAnnotation = remember { UserArrowAnnotation() }

    UIKitView(
        modifier = modifier,
        factory = {
            MKMapView().also { map ->
                delegate.onLongPress = { lat, lng -> currentOnLongPress.value(lat, lng) }
                delegate.onDirectionsRequested = { currentOnDirectionsRequested.value() }
                delegate.onUserInteraction = { currentOnUserInteraction.value() }
                map.delegate = delegate
                map.pitchEnabled = false
                map.rotateEnabled = false
                val recognizer = UILongPressGestureRecognizer(
                    target = delegate,
                    action = NSSelectorFromString("handleLongPress:")
                ).apply { minimumPressDuration = 0.5 }
                map.addGestureRecognizer(recognizer)
                val panRecognizer = UIPanGestureRecognizer(
                    target = delegate,
                    action = NSSelectorFromString("handleUserPan:")
                ).apply { cancelsTouchesInView = false }
                map.addGestureRecognizer(panRecognizer)
                map.addAnnotation(userArrowAnnotation)
            }
        },
        update = { map ->

            // Camera — skip when rider has panned away to inspect the route
            if (isFollowingRider) {
                val coordinate = CLLocationCoordinate2DMake(latitude, longitude)
                val camera = MKMapCamera.cameraLookingAtCenterCoordinate(
                    centerCoordinate = coordinate,
                    fromDistance = 500.0,
                    pitch = 0.0,
                    heading = bearing.toDouble()
                )
                map.setCamera(camera, animated = true)
            }

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
            map.annotations
                .filterIsInstance<MKPointAnnotation>()
                .filter { it !is UserArrowAnnotation }
                .forEach { map.removeAnnotation(it) }
            if (pinLocation != null) {
                val (lat, lng) = pinLocation
                val annotation = MKPointAnnotation()
                annotation.setCoordinate(CLLocationCoordinate2DMake(lat, lng))
                annotation.setTitle("Pin")
                map.addAnnotation(annotation)
            }

            // Update arrow position and bearing
            userArrowAnnotation.setCoordinate(CLLocationCoordinate2DMake(latitude, longitude))
            val arrowView = map.viewForAnnotation(userArrowAnnotation)
            if (arrowView != null) {
                arrowView.transform = CGAffineTransformMakeRotation(bearing.toDouble() * PI / 180.0)
            }
        }
    )
}

private fun createArrowImage(): UIImage {
    val size = CGSizeMake(32.0, 32.0)
    UIGraphicsBeginImageContextWithOptions(size, false, 0.0)
    val path = UIBezierPath()
    path.moveToPoint(CGPointMake(16.0, 2.0))
    path.addLineToPoint(CGPointMake(28.0, 28.0))
    path.addLineToPoint(CGPointMake(16.0, 20.0))
    path.addLineToPoint(CGPointMake(4.0, 28.0))
    path.closePath()
    UIColor.whiteColor.setFill()
    path.fill()
    UIColor(red = 0.05, green = 0.11, blue = 0.16, alpha = 1.0).setStroke()
    path.lineWidth = 2.0
    path.stroke()
    val image = UIGraphicsGetImageFromCurrentImageContext()!!
    UIGraphicsEndImageContext()
    return image
}
