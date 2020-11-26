package com.appacoustic.cointester.aaa.utils

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.widget.TextView
import com.appacoustic.cointester.aaa.utils.FontCache.getTypeface

object Tools {
    fun setTextViewTypeFace(
        context: Context?,
        typefaceName: String?,
        tV: TextView
    ) {
        val typeface = getTypeface(
            context!!,
            typefaceName!!
        )
        tV.typeface = typeface
    }

    /**
     * @param activity Related activity.
     * @return the height of the device screen in pixels.
     */
    fun getHeightScreen(activity: Activity): Int {
        val metrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(metrics)
        return metrics.heightPixels
    }

    fun dBToLinear(dBValue: Double): Double {
        return Math.pow(
            10.0,
            dBValue / 20
        )
    }
}
