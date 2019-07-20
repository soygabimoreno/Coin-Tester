package com.appacoustic.cointester.utils;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

public class FontCache {

    private static Map<String, Typeface> fontCache = new HashMap<>(2);

    public static Typeface getTypeface(Context context, String fontName) {
        Typeface typeface = fontCache.get(fontName); // We get the Typeface if it has been loaded
        if (typeface == null) {
            try {
                typeface = Typeface.createFromAsset(context.getAssets(), "fonts/" + fontName); // We obtain (the first time) the Typeface from 'assets/fonts/'
            } catch (Exception e) {
                e.printStackTrace();
                return null; // If doesn't exists the specific resource
            }
            fontCache.put(fontName, typeface); // We store the Typeface (avoiding repetitions)
        }
        return typeface;
    }
}