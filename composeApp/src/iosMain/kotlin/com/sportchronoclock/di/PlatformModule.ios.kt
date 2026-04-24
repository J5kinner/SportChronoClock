package com.sportchronoclock.di

import com.sportchronoclock.location.IOSLocationProvider
import com.sportchronoclock.location.LocationProvider
import com.sportchronoclock.permissions.PermissionHandler
import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<LocationProvider> { IOSLocationProvider() }
    single { PermissionHandler() }
    single {
        HttpClient(Darwin) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}
