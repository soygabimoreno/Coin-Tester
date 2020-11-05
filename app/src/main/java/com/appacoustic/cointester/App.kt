package com.appacoustic.cointester

import android.app.Application
import com.appacoustic.cointester.aaa.AppResources
import com.appacoustic.cointester.libFramework.KLog

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        KLog.launch(BuildConfig.DEBUG)
        AppResources.getInstance().init(this)
    }
}
