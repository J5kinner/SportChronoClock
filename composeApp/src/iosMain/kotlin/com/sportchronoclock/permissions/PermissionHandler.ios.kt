package com.sportchronoclock.permissions

import platform.CoreLocation.*

actual class PermissionHandler {
    actual fun hasLocationPermission(): Boolean {
        val status = CLLocationManager().authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedAlways ||
                status == kCLAuthorizationStatusAuthorizedWhenInUse
    }
}
