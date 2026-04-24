package com.sportchronoclock.ui

import androidx.compose.runtime.Composable

/**
 * Side-effect composable that triggers a system location permission dialog.
 * onGranted fires once when all required permissions are approved.
 * onDenied fires if the user declines.
 */
@Composable
expect fun RequestLocationPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit = {}
)
