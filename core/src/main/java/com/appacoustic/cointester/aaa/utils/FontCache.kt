package com.appacoustic.cointester.aaa.utils

import android.content.Context
import android.graphics.Typeface
import java.util.*

object FontCache {
    private val fontCache: MutableMap<String, Typeface?> = HashMap(2)

    @JvmStatic
    fun getTypeface(
        context: Context,
        fontName: String
    ): Typeface? {
        var typeface = fontCache[fontName] // We get the Typeface if it has been loaded
        if (typeface == null) {
            typeface = try {
                Typeface.createFromAsset(
                    context.assets,
                    "fonts/$fontName"
                ) // We obtain (the first time) the Typeface from 'assets/fonts/'
            } catch (e: Exception) {
                e.printStackTrace()
                return null // If doesn't exists the specific resource
            }
            fontCache[fontName] = typeface // We store the Typeface (avoiding repetitions)
        }
        return typeface
    }
}
