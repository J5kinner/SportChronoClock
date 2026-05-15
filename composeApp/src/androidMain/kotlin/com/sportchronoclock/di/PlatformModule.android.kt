package com.sportchronoclock.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.sportchronoclock.location.AndroidLocationProvider
import com.sportchronoclock.location.LocationProvider
import com.sportchronoclock.media.AndroidMediaController
import com.sportchronoclock.media.MediaController
import com.sportchronoclock.permissions.PermissionHandler
import com.sportchronoclock.ride.SqlDriverFactory
import com.sportchronoclock.sensors.AndroidSensorProvider
import com.sportchronoclock.sensors.SensorProvider
import com.sportchronoclock.tts.TtsEngine
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<LocationProvider> { AndroidLocationProvider(androidContext()) }
    single { PermissionHandler(androidContext()) }
    single { SqlDriverFactory(androidContext()) }
    single<SensorProvider> { AndroidSensorProvider(androidContext()) }
    single<Settings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("sportchronoclock", Context.MODE_PRIVATE)
        )
    }
    single<MediaController> { AndroidMediaController(androidContext()) }
    single { TtsEngine(androidContext()) }
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}
