package com.sportchronoclock.di

import org.koin.core.module.Module
import org.koin.dsl.module

// ViewModel is created directly in DashboardScreen via lifecycle's viewModel { }
// to avoid a binary incompatibility between koin-compose-viewmodel and
// lifecycle-viewmodel-compose 2.10.x
val commonModule = module {}

expect val platformModule: Module
