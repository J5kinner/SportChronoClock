@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.sportchronoclock.location

import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.CoreLocation.*
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

private class LocationDelegate(
    private val onLocation: (LocationData) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        val (lat, lon) = location.coordinate.useContents { latitude to longitude }
        // CLLocation.speed is -1 when the platform has no speed measurement
        val speedValid = location.speed >= 0.0
        onLocation(
            LocationData(
                latitude = lat,
                longitude = lon,
                speed = if (speedValid) location.speed.toFloat() else 0f,
                bearing = maxOf(0f, location.course.toFloat()),
                timestamp = (location.timestamp.timeIntervalSince1970 * 1000).toLong(),
                hasSpeed = speedValid
            )
        )
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: platform.Foundation.NSError
    ) {
        // Silently ignore; flow simply won't emit
    }
}

class IOSLocationProvider : LocationProvider {
    private val _locationFlow = MutableSharedFlow<LocationData>(replay = 1, extraBufferCapacity = 64)
    override val locationFlow: Flow<LocationData> = _locationFlow.asSharedFlow()

    private val delegate = LocationDelegate { _locationFlow.tryEmit(it) }

    private val locationManager = CLLocationManager().also {
        it.delegate = delegate
        it.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        it.activityType = CLActivityTypeAutomotiveNavigation
        it.distanceFilter = kCLDistanceFilterNone
        it.pausesLocationUpdatesAutomatically = false
        it.allowsBackgroundLocationUpdates = true
    }

    override fun startTracking() = locationManager.startUpdatingLocation()

    override fun stopTracking() = locationManager.stopUpdatingLocation()
}
