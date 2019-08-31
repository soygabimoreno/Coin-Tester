package com.appacoustic.cointester.analyzer.model

import android.media.MediaRecorder
import com.gabrielmorenoibarra.k.util.KLog

class AnalyzerParams(val audioSourceNames: Array<String>,
                     val audioSourceIds: IntArray,
                     val windowFunctionNames: Array<String>) {

    companion object {
        const val RECORDER_AGC_OFF = MediaRecorder.AudioSource.DEFAULT
        const val BYTES_PER_SAMPLE = 2
        const val SAMPLE_VALUE_MAX = 32767.0
        const val N_MIC_SOURCES = 7

        var idTestSignal1 = 0
        var idTestSignal2 = 0
        var idTestSignalWhiteNoise = 0
    }

    var sampleRate = 16000
    var fftLength = 2048
    var hopLength = 1024
    var overlapPercent = ((1 - hopLength / fftLength) * 100).toDouble()
    var windowFunctionName: String? = null
    var nFftAverage = 2
    var dBAWeighting = false
    var spectrogramDuration = 4.0
    var micGainDB: DoubleArray? = null // Should have (fftLength / 2) elements
    var audioSourceId = RECORDER_AGC_OFF

    val audioSourceName: String
        get() = getAudioSourceNameFromId(audioSourceId)

    private fun getAudioSourceNameFromId(id: Int): String {
        for (i in audioSourceNames.indices) {
            if (audioSourceIds[i] == id) {
                return audioSourceNames[i]
            }
        }
        KLog.Companion.e("Non-standard entry")
        return id.toString()
    }
}
