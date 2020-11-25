package com.appacoustic.cointester.aaa.analyzer

import android.os.SystemClock
import com.appacoustic.cointester.libFramework.KLog.Companion.d

/**
 * Frames Per Second Counter.
 */
class FPSCounter(private val tag: String) {
    private var frameCount: Long = 0
    private var timeOld: Long
    private val timeUpdateInterval // in ms
        : Long = 2000
    var fPS = 0.0
        private set

    // call this when number of frames plus one
    fun increment() {
        frameCount++
        val timeNow = SystemClock.uptimeMillis()
        if (timeOld + timeUpdateInterval <= timeNow) {
            fPS = 1000 * frameCount.toDouble() / (timeNow - timeOld)
            d(
                ": FPS: " + Math.round(100 * fPS) / 100.0 +
                    " (" + frameCount + "/" + (timeNow - timeOld) + " ms)"
            )
            timeOld = timeNow
            frameCount = 0
        }
    }

    init {
        timeOld = SystemClock.uptimeMillis()
    }
}
