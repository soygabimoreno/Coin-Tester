package com.appacoustic.cointester

import android.support.multidex.MultiDexApplication

import com.gabrielmorenoibarra.g.GLog

class App : MultiDexApplication() {

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        GLog.init(BuildConfig.DEBUG)
        AppResources.getInstance().init(this)
    }
}
