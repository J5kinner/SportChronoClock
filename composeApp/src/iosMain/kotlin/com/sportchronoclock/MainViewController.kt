package com.sportchronoclock

import androidx.compose.ui.window.ComposeUIViewController
import com.sportchronoclock.di.commonModule
import com.sportchronoclock.di.platformModule
import org.koin.core.context.startKoin

private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        koinStarted = true
        startKoin {
            modules(commonModule, platformModule)
        }
    }
    App()
}
