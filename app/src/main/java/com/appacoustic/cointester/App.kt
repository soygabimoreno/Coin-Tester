package com.appacoustic.cointester

import android.support.multidex.MultiDexApplication

import com.gabrielmorenoibarra.g.GLog

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        GLog.init(BuildConfig.DEBUG)
        AppResources.getInstance().init(this)
    }
}
