package com.appacoustic.cointester

import android.support.multidex.MultiDexApplication
import com.crashlytics.android.Crashlytics
import com.gabrielmorenoibarra.g.GLog
import io.fabric.sdk.android.Fabric

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

        if (BuildConfig.BUG_TRACKERS_ENABLED) {
            Fabric.with(this, Crashlytics())
        }
    }
}
