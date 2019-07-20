package com.appacoustic.cointester.analyzer;

import android.os.SystemClock;

import com.gabrielmorenoibarra.g.GLog;

/**
 * Monitor for recording.
 */
public class RecorderMonitor {

    private static final String TAG0 = RecorderMonitor.class.getSimpleName();

    private final String TAG;
    private long timeUpdateOld, timeUpdateInterval, timeStarted;  // in ms
    private long lastOverrunTime;
    private long nSamplesRead;
    private int sampleRate;
    private int bufferSampleSize;
    private double sampleRateReal;
    private boolean lastCheckOverrun = false;

    public RecorderMonitor(int sampleRateIn, int bufferSampleSizeIn, String TAG1) {
        sampleRate = sampleRateIn;
        bufferSampleSize = bufferSampleSizeIn;
        timeUpdateInterval = 2000;
        TAG = TAG1 + TAG0;
    }

    // When start recording, call this
    public void start() {
        nSamplesRead = 0;
        lastOverrunTime = 0;
        timeStarted = SystemClock.uptimeMillis();
        timeUpdateOld = timeStarted;
        sampleRateReal = sampleRate;
    }

    // Input number of audio frames that read
    // Return true if an overrun check is performed, otherwise false.
    public boolean updateState(int numOfReadShort) {
        final String METHOD_NAME = Thread.currentThread().getStackTrace()[2].getMethodName();
        long timeNow = SystemClock.uptimeMillis();
        if (nSamplesRead == 0) {      // get overrun checker synchronized
            timeStarted = timeNow - numOfReadShort * 1000 / sampleRate;
        }
        nSamplesRead += numOfReadShort;
        if (timeUpdateOld + timeUpdateInterval > timeNow) {
            return false;  // do the checks below every timeUpdateInterval ms
        }
        timeUpdateOld += timeUpdateInterval;
        if (timeUpdateOld + timeUpdateInterval <= timeNow) {
            timeUpdateOld = timeNow;  // catch up the time (so that at most one output per timeUpdateInterval)
        }
        long nSamplesFromTime = (long) ((timeNow - timeStarted) * sampleRateReal / 1000);
        double f1 = (double) nSamplesRead / sampleRateReal;
        double f2 = (double) nSamplesFromTime / sampleRateReal;
//    GLog.i(TAG, "Buffer"
//        + " should read " + nSamplesFromTime + " (" + Math.round(f2*1000)/1000.0 + "s),"
//        + " actual read " + nSamplesRead + " (" + Math.round(f1*1000)/1000.0 + "s)\n"
//        + " diff " + (nSamplesFromTime-nSamplesRead) + " (" + Math.round((f2-f1)*1000)/1e3 + "s)"
//        + " sampleRate = " + Math.round(sampleRateReal*100)/100.0);
        // Check if buffer overrun occur
        if (nSamplesFromTime > bufferSampleSize + nSamplesRead) {
            GLog.w(TAG, METHOD_NAME + ": Buffer Overrun occurred !\n"
                    + " should read " + nSamplesFromTime + " (" + Math.round(f2 * 1000) / 1000.0 + "s),"
                    + " actual read " + nSamplesRead + " (" + Math.round(f1 * 1000) / 1000.0 + "s)\n"
                    + " diff " + (nSamplesFromTime - nSamplesRead) + " (" + Math.round((f2 - f1) * 1000) / 1e3 + "s)"
                    + " sampleRate = " + Math.round(sampleRateReal * 100) / 100.0
                    + "\n Overrun counter reset.");
            lastOverrunTime = timeNow;
            nSamplesRead = 0;  // start over
        }
        // Update actual sample rate
        if (nSamplesRead > 10 * sampleRate) {
            sampleRateReal = 0.9 * sampleRateReal + 0.1 * (nSamplesRead * 1000.0 / (timeNow - timeStarted));
            if (Math.abs(sampleRateReal - sampleRate) > 0.0145 * sampleRate) {  // 0.0145 = 25 cent
                GLog.w(TAG, METHOD_NAME + ": Sample rate inaccurate, possible hardware problem !\n"
                        + " should read " + nSamplesFromTime + " (" + Math.round(f2 * 1000) / 1000.0 + "s),"
                        + " actual read " + nSamplesRead + " (" + Math.round(f1 * 1000) / 1000.0 + "s)\n"
                        + " diff " + (nSamplesFromTime - nSamplesRead) + " (" + Math.round((f2 - f1) * 1000) / 1e3 + "s)"
                        + " sampleRate = " + Math.round(sampleRateReal * 100) / 100.0
                        + "\n Overrun counter reset.");
                nSamplesRead = 0;
            }
        }
        lastCheckOverrun = lastOverrunTime == timeNow;
        return true;  // state updated during this check
    }

    public boolean getLastCheckOverrun() {
        return lastCheckOverrun;
    }

    long getLastOverrunTime() {
        return lastOverrunTime;
    }

    public double getSampleRate() {
        return sampleRateReal;
    }
}
