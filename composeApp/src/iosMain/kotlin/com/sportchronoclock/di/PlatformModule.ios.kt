package com.sportchronoclock.di

import com.sportchronoclock.location.IOSLocationProvider
import com.sportchronoclock.location.LocationProvider
import com.sportchronoclock.permissions.PermissionHandler
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<LocationProvider> { IOSLocationProvider() }
    single { PermissionHandler() }
}
