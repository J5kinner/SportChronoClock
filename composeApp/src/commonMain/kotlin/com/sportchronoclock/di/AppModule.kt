package com.sportchronoclock.di

import com.sportchronoclock.navigation.DirectionsService
import org.koin.core.module.Module
import org.koin.dsl.module

val commonModule = module {
    single { DirectionsService(get()) }
}

expect val platformModule: Module
