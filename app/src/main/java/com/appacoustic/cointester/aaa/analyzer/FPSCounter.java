package com.appacoustic.cointester.aaa.analyzer;

import android.os.SystemClock;

import com.gabrielmorenoibarra.k.util.KLog;

/**
 * Frames Per Second Counter.
 */
public class FPSCounter {
    private long frameCount;
    private long timeOld, timeUpdateInterval;  // in ms
    private double fps;
    private String tag;

    public FPSCounter(String tag) {
        timeUpdateInterval = 2000;
        this.tag = tag;
        frameCount = 0;
        timeOld = SystemClock.uptimeMillis();
    }

    // call this when number of frames plus one
    public void increment() {
        frameCount++;
        long timeNow = SystemClock.uptimeMillis();
        if (timeOld + timeUpdateInterval <= timeNow) {
            fps = 1000 * (double) frameCount / (timeNow - timeOld);
            KLog.Companion.d(": FPS: " + Math.round(100 * fps) / 100.0 +
                    " (" + frameCount + "/" + (timeNow - timeOld) + " ms)");
            timeOld = timeNow;
            frameCount = 0;
        }
    }

    public double getFPS() {
        return fps;
    }
}
