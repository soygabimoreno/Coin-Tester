package com.appacoustic.cointester;

import android.support.multidex.MultiDexApplication;

import com.gabrielmorenoibarra.g.GLog;

public class App extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        GLog.init(BuildConfig.DEBUG);
        AppResources.getInstance().init(this);
    }
}
