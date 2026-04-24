package com.sportchronoclock.di

import com.sportchronoclock.MainViewModel
import org.koin.core.module.Module
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    viewModel { MainViewModel(get()) }
}

expect val platformModule: Module
