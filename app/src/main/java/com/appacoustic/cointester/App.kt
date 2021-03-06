package com.appacoustic.cointester

import android.app.Application
import com.amplitude.api.AmplitudeClient
import com.appacoustic.cointester.core.di.serviceLocator
import com.appacoustic.cointester.libFramework.KLog
import com.google.firebase.FirebaseApp
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.logger.AndroidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        KLog.launch(BuildConfig.DEBUG)
        initKoin()
        initFirebase()
        initAmplitude()
    }

    private fun initKoin() {
        startKoin {
            if (BuildConfig.DEBUG) logger(AndroidLogger(Level.ERROR))
            androidContext(this@App)
            modules(serviceLocator)
        }
    }

    private fun initFirebase() {
        FirebaseApp.initializeApp(this)
    }

    private fun initAmplitude() {
        val amplitudeClient: AmplitudeClient by inject()
        val amplitudeApiKey = ApiKeyRetriever.getAmplitudeApiKey()
        amplitudeClient.initialize(
            this,
            amplitudeApiKey
        )
    }
}
