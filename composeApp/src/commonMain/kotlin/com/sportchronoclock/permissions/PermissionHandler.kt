package com.sportchronoclock.permissions

expect class PermissionHandler {
    fun hasLocationPermission(): Boolean
}
