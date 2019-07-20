package com.appacoustic.cointester;

import android.content.Context;
import android.content.res.Resources;

/**
 * Common resources of the application.
 * Created by Gabriel Moreno on 2017-09-30.
 */
@Deprecated()
public class AppResources {

    private Resources resources;

    private static final AppResources ourInstance = new AppResources();

    public static AppResources getInstance() {
        return ourInstance;
    }

    private AppResources() {
    }

    public void init(Context context) {
        resources = context.getResources();
    }

    public String getString(int resId) {
        return resources.getString(resId);
    }

    public String[] getStringArray(int resId) {
        return resources.getStringArray(resId);
    }
}