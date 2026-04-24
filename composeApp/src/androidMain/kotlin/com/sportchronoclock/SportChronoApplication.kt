package com.sportchronoclock

import android.app.Application
import com.sportchronoclock.di.commonModule
import com.sportchronoclock.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SportChronoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SportChronoApplication)
            modules(commonModule, platformModule)
        }
    }
}
