package com.sportchronoclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import platform.CoreLocation.*
import platform.darwin.NSObject

@Composable
actual fun RequestLocationPermission(onGranted: () -> Unit, onDenied: () -> Unit) {
    val delegate = remember {
        object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                when (manager.authorizationStatus) {
                    kCLAuthorizationStatusAuthorizedAlways,
                    kCLAuthorizationStatusAuthorizedWhenInUse -> onGranted()
                    kCLAuthorizationStatusDenied,
                    kCLAuthorizationStatusRestricted -> onDenied()
                    else -> {}
                }
            }
        }
    }

    val locationManager = remember {
        CLLocationManager().also { it.delegate = delegate }
    }

    DisposableEffect(Unit) {
        when (locationManager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> onGranted()
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> onDenied()
            else -> locationManager.requestAlwaysAuthorization()
        }
        onDispose {}
    }
}
