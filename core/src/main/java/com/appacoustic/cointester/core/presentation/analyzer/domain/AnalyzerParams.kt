package com.appacoustic.cointester.core.presentation.analyzer.domain

import com.appacoustic.cointester.coreAnalytics.error.ErrorTrackerComponent

class AnalyzerParams(
    val audioSourceNames: Array<String>,
    audioSourceIdsString: Array<String>,
    val windowFunctionNames: Array<String>,
    val errorTrackerComponent: ErrorTrackerComponent
) {

    companion object {
        const val RECORDER_AGC_OFF = 0 // MediaRecorder.AudioSource.DEFAULT
        const val BYTES_PER_SAMPLE = 2
        const val SAMPLE_VALUE_MAX = 32767.0
        const val N_MIC_SOURCES = 7
    }

    val audioSourceIds = IntArray(audioSourceIdsString.size)
    var idTestSignal1: Int
    var idTestSignal2: Int
    var idTestSignalWhiteNoise: Int

    init {
        for (i in audioSourceIdsString.indices) {
            audioSourceIds[i] = audioSourceIdsString[i].toInt()
        }

        idTestSignal1 = audioSourceIds[N_MIC_SOURCES]
        idTestSignal2 = audioSourceIds[N_MIC_SOURCES + 1]
        idTestSignalWhiteNoise = audioSourceIds[N_MIC_SOURCES + 2]
    }

    var sampleRate = 16000
    var fftLength = 2048
    var hopLength = 1024
    var overlapPercent = ((1 - hopLength / fftLength) * 100).toDouble()
    var windowFunctionName: String? = null
    var nFftAverage = 2
    var dbaWeighting = false
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
        errorTrackerComponent.trackError(Throwable("ANALYZER_PARAMS_NON_STANDARD_ENTRY"))
        return id.toString()
    }
}
