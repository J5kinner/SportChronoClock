package com.sportchronoclock.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import com.sportchronoclock.location.IOSLocationProvider
import com.sportchronoclock.location.LocationProvider
import com.sportchronoclock.media.IosMediaController
import com.sportchronoclock.media.MediaController
import com.sportchronoclock.permissions.PermissionHandler
import com.sportchronoclock.ride.SqlDriverFactory
import com.sportchronoclock.sensors.IosSensorProvider
import com.sportchronoclock.sensors.SensorProvider
import com.sportchronoclock.tts.TtsEngine
import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformModule: Module = module {
    single<LocationProvider> { IOSLocationProvider() }
    single { PermissionHandler() }
    single { SqlDriverFactory() }
    single<SensorProvider> { IosSensorProvider() }
    single<Settings> {
        NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }
    single<MediaController> { IosMediaController() }
    single { TtsEngine() }
    single {
        HttpClient(Darwin) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}
