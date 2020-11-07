package com.appacoustic.cointester

import android.app.Application
import com.appacoustic.cointester.di.serviceLocator
import com.appacoustic.cointester.libFramework.KLog
import org.koin.android.ext.koin.androidContext
import org.koin.android.logger.AndroidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        KLog.launch(BuildConfig.DEBUG)
        initKoin()
    }

    private fun initKoin() {
        startKoin {
            if (BuildConfig.DEBUG) logger(AndroidLogger(Level.ERROR))
            androidContext(this@App)
            modules(serviceLocator)
        }
    }
}
