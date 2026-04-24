package com.sportchronoclock.di

import com.sportchronoclock.location.AndroidLocationProvider
import com.sportchronoclock.location.LocationProvider
import com.sportchronoclock.permissions.PermissionHandler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<LocationProvider> { AndroidLocationProvider(androidContext()) }
    single { PermissionHandler(androidContext()) }
}
