package com.appacoustic.cointester

import android.app.Application
import com.appacoustic.cointester.libFramework.KLog

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        KLog.launch(BuildConfig.DEBUG)
    }
}
