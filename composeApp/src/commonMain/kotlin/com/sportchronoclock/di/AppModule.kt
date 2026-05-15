package com.sportchronoclock.di

import com.sportchronoclock.MainViewModel
import com.sportchronoclock.db.RideDatabase
import com.sportchronoclock.media.MediaControlViewModel
import com.sportchronoclock.navigation.DirectionsService
import com.sportchronoclock.ride.RideEventBus
import com.sportchronoclock.ride.RideRecorder
import com.sportchronoclock.ride.RideRepository
import com.sportchronoclock.ride.RideStatsViewModel
import com.sportchronoclock.ride.SqlDriverFactory
import com.sportchronoclock.settings.SettingsRepository
import com.sportchronoclock.settings.SettingsViewModel
import com.sportchronoclock.sport.SportModeViewModel
import com.sportchronoclock.tts.VoiceNavController
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    single { DirectionsService(get()) }
    single { RideEventBus() }
    single { RideDatabase(get<SqlDriverFactory>().create()) }
    single { RideRepository(get()) }
    single { RideRecorder(get(), get()) }
    single { SettingsRepository(get()) }
    single { VoiceNavController(get(), get()) }
    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { RideStatsViewModel(get(), get()) }
    viewModel { SportModeViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { MediaControlViewModel(get()) }
}

expect val platformModule: Module
