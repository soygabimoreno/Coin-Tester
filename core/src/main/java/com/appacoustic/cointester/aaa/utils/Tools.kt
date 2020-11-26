package com.appacoustic.cointester.aaa.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.widget.TextView;

public class Tools {

    public static void setTextViewTypeFace(Context context, String typefaceName, TextView tV) {
        Typeface typeface = FontCache.getTypeface(context, typefaceName);
        tV.setTypeface(typeface);
    }

    /**
     * @param activity Related activity.
     * @return the height of the device screen in pixels.
     */
    public static int getHeightScreen(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    public static double dBToLinear(double dBValue) {
        return Math.pow(10, dBValue / 20);
    }
}