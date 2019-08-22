package com.appacoustic.cointester.analyzer.model

import android.media.MediaRecorder
import com.appacoustic.cointester.framework.KLog

class AnalyzerParams(val audioSourceNames: Array<String>,
                     val audioSourceIds: IntArray,
                     val windowFunctionNames: Array<String>) {

    companion object {
        val RECORDER_AGC_OFF = MediaRecorder.AudioSource.DEFAULT
        val BYTES_PER_SAMPLE = 2
        val SAMPLE_VALUE_MAX = 32767.0

        val N_MIC_SOURCES = 7
        var ID_TEST_SIGNAL_1: Int = 0
        var ID_TEST_SIGNAL_2: Int = 0
        var ID_TEST_SIGNAL_WHITE_NOISE: Int = 0
    }

    var sampleRate = 16000
    var fftLength = 2048
    var hopLength = 1024
    var overlapPercent = ((1 - hopLength / fftLength) * 100).toDouble()
    var windowFunctionName: String? = null
    var nFftAverage = 2
    var isDBAWeighting = false
    var spectrogramDuration = 4.0
    var micGainDB: DoubleArray? = null // Should have fFTLength/2 elements
    var audioSourceId = RECORDER_AGC_OFF

    val audioSourceName: String
        get() = getAudioSourceNameFromId(audioSourceId)

    private fun getAudioSourceNameFromId(id: Int): String {
        for (i in audioSourceNames.indices) {
            if (audioSourceIds[i] == id) {
                return audioSourceNames[i]
            }
        }
        KLog.Companion.e("getAudioSourceNameFromId(): non-standard entry")
        return id.toString()
    }
}
