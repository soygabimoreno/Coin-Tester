package com.appacoustic.cointester.core.framework

import android.os.SystemClock
import com.appacoustic.cointester.libFramework.KLog

/**
 * Monitor for recording.
 */
class RecorderMonitor(
    sampleRateIn: Int,
    bufferSampleSizeIn: Int,
    TAG1: String
) {
    private val TAG: String
    private var timeUpdateOld: Long = 0
    private val timeUpdateInterval: Long
    private var timeStarted // in ms
        : Long = 0
    var lastOverrunTime: Long = 0
        private set
    private var nSamplesRead: Long = 0
    private val bufferSampleSize: Int
    var sampleRate = 0.0
    var lastCheckOverrun = false
        private set

    // When start recording, call this
    fun start() {
        nSamplesRead = 0
        lastOverrunTime = 0
        timeStarted = SystemClock.uptimeMillis()
        timeUpdateOld = timeStarted
        this.sampleRate = sampleRate
    }

    // Input number of audio frames that read
    // Return true if an overrun check is performed, otherwise false.
    fun updateState(numOfReadShort: Int): Boolean {
        val timeNow = SystemClock.uptimeMillis()
        if (nSamplesRead == 0L) {      // get overrun checker synchronized
            timeStarted = (timeNow - numOfReadShort * 1000 / sampleRate).toLong()
        }
        nSamplesRead += numOfReadShort.toLong()
        if (timeUpdateOld + timeUpdateInterval > timeNow) {
            return false // do the checks below every timeUpdateInterval ms
        }
        timeUpdateOld += timeUpdateInterval
        if (timeUpdateOld + timeUpdateInterval <= timeNow) {
            timeUpdateOld = timeNow // catch up the time (so that at most one output per timeUpdateInterval)
        }
        val nSamplesFromTime = (timeNow - timeStarted) * this.sampleRate / 1000
        val f1: Double = nSamplesRead.toDouble() / this.sampleRate
        val f2: Double = nSamplesFromTime.toDouble() / this.sampleRate
        //    KLog.Companion.i("Buffer"
//        + " should read " + nSamplesFromTime + " (" + Math.round(f2*1000)/1000.0 + "s),"
//        + " actual read " + nSamplesRead + " (" + Math.round(f1*1000)/1000.0 + "s)\n"
//        + " diff " + (nSamplesFromTime-nSamplesRead) + " (" + Math.round((f2-f1)*1000)/1e3 + "s)"
//        + " sampleRate = " + Math.round(sampleRateReal*100)/100.0);
        // Check if buffer overrun occur
        if (nSamplesFromTime > bufferSampleSize + nSamplesRead) {
            KLog.w(
                """Buffer Overrun occurred !
 should read $nSamplesFromTime (${Math.round(f2 * 1000) / 1000.0}s), actual read $nSamplesRead (${Math.round(f1 * 1000) / 1000.0}s)
 diff ${nSamplesFromTime - nSamplesRead} (${Math.round((f2 - f1) * 1000) / 1e3}s) sampleRate = ${Math.round(this.sampleRate * 100) / 100.0}
 Overrun counter reset."""
            )
            lastOverrunTime = timeNow
            nSamplesRead = 0 // start over
        }
        // Update actual sample rate
        if (nSamplesRead > 10 * sampleRate) {
            this.sampleRate = 0.9 * this.sampleRate + 0.1 * (nSamplesRead * 1000.0 / (timeNow - timeStarted))
            if (Math.abs(this.sampleRate - sampleRate) > 0.0145 * sampleRate) {  // 0.0145 = 25 cent
                KLog.w(
                    """Sample rate inaccurate, possible hardware problem !
 should read $nSamplesFromTime (${Math.round(f2 * 1000) / 1000.0}s), actual read $nSamplesRead (${Math.round(f1 * 1000) / 1000.0}s)
 diff ${nSamplesFromTime - nSamplesRead} (${Math.round((f2 - f1) * 1000) / 1e3}s) sampleRate = ${Math.round(this.sampleRate * 100) / 100.0}
 Overrun counter reset."""
                )
                nSamplesRead = 0
            }
        }
        lastCheckOverrun = lastOverrunTime == timeNow
        return true // state updated during this check
    }

    companion object {
        private val TAG0 = RecorderMonitor::class.java.simpleName
    }

    init {
        sampleRate = sampleRateIn.toDouble()
        bufferSampleSize = bufferSampleSizeIn
        timeUpdateInterval = 2000
        TAG = TAG1 + TAG0
    }
}
