package com.sportchronoclock

import androidx.compose.ui.window.ComposeUIViewController
import com.sportchronoclock.di.commonModule
import com.sportchronoclock.di.platformModule
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            modules(commonModule, platformModule)
        }
    }
    App()
}
