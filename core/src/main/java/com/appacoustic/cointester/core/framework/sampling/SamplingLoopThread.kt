package com.appacoustic.cointester.core.framework.sampling

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.SystemClock
import com.appacoustic.cointester.core.R
import com.appacoustic.cointester.core.framework.RecorderMonitor
import com.appacoustic.cointester.core.framework.processing.STFT
import com.appacoustic.cointester.core.presentation.analyzer.domain.AnalyzerParams
import com.appacoustic.cointester.core.presentation.analyzer.view.AnalyzerViews
import com.appacoustic.cointester.libFramework.KLog
import com.appacoustic.libprocessing.SineGenerator
import com.appacoustic.libprocessing.dBToLinear
import com.appacoustic.libprocessingandroid.wav.WavWriter
import java.util.Arrays
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Read a snapshot of audio data at a regular interval and compute the FFT.
 */
class SamplingLoopThread(
    private val params: AnalyzerParams,
    private val analyzerViews: AnalyzerViews,
    @Volatile var paused: Boolean,
    private val saveWav: Boolean,
    private val listener: Listener
) : Thread() {

    companion object {
        private const val TEST_SIGNAL_1_FREQ_1 = 440.0
        private const val TEST_SIGNAL_1_DB_1 = -6.0
        private const val TEST_SIGNAL_2_FREQ_1 = 625.0
        private const val TEST_SIGNAL_2_DB_1 = -6.0
        private const val TEST_SIGNAL_2_FREQ_2 = 1875.0
        private const val TEST_SIGNAL_2_DB_2 = -12.0
    }

    interface Listener {
        fun onInitGraphs()
        fun onUpdateAmplitude(
            maxAmplitudeFreq: Double,
            maxAmplitudeDB: Double
        )

        fun onUpdateRms(
            rms: Double,
            rmsFromFT: Double
        )
    }

    @Volatile
    private var running = true

    private lateinit var stft: STFT

    private var sineGenerator0: SineGenerator
    private val sineGenerator1: SineGenerator
    private var spectrumAmplitudeDBCopy: DoubleArray? = null // Transfer data from SamplingLoopThread to AnalyzerGraphic
    private var cumulative: DoubleArray? = null

    private val bytesPerSample = AnalyzerParams.BYTES_PER_SAMPLE
    private val sampleValueMax = AnalyzerParams.SAMPLE_VALUE_MAX
    private val sampleRate = params.sampleRate
    private val audioSourceId = params.audioSourceId
    private val fftLength = params.fftLength
    private val nFftAverage = params.nFftAverage

    @Volatile
    var wavSecondsRemain = 0.0

    @Volatile
    var wavSeconds = 0.0

    private var baseTimeMs = SystemClock.uptimeMillis().toDouble()
    private var data: DoubleArray? = null

    init {
        val amp0 = dBToLinear(TEST_SIGNAL_1_DB_1)
        val amp1 = dBToLinear(TEST_SIGNAL_2_DB_1)
        val amp2 = dBToLinear(TEST_SIGNAL_2_DB_2)
        sineGenerator0 = if (audioSourceId == params.idTestSignal1) {
            SineGenerator(
                TEST_SIGNAL_1_FREQ_1,
                sampleRate,
                sampleValueMax * amp0
            )
        } else {
            SineGenerator(
                TEST_SIGNAL_2_FREQ_1,
                sampleRate,
                sampleValueMax * amp1
            )
        }
        sineGenerator1 = SineGenerator(
            TEST_SIGNAL_2_FREQ_2,
            sampleRate,
            sampleValueMax * amp2
        )
    }

    @SuppressLint("MissingPermission")
    override fun run() {
        val timeStart = SystemClock.uptimeMillis()
        listener.onInitGraphs()
        val timeEnd = SystemClock.uptimeMillis()
        val time = timeEnd - timeStart
        val timeMin = 500
        if (time < timeMin) {
            val timeWaiting = timeMin - time
            KLog.i("Waiting $timeWaiting ms more...")
            try {
                sleep(timeWaiting)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            analyzerViews.notifyToast(R.string.error_invalid_audio_record_parameters)
            return
        }

        var readChunkSize = params.hopLength // Every hopLength one fft result (overlapped analyze window)
        readChunkSize = min(
            readChunkSize,
            2048
        ) // Smaller chunk, smaller delay
        var bufferSize = max(
            minBufferSize / bytesPerSample,
            fftLength / 2
        ) * 2
        bufferSize *= ceil(1.0 * sampleRate / bufferSize).toInt() // Tolerate up to about 1 sec

        // Use the mic with AGC (Automatic Gain Control) turned off
        // The buffer size here seems not relate to the delay.
        // So choose a larger size (~1sec) so that overrun is unlikely.
        val record = try {
            if (audioSourceId < params.idTestSignal1) {
                AudioRecord(
                    audioSourceId,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bytesPerSample * bufferSize
                )
            } else {
                AudioRecord(
                    AnalyzerParams.RECORDER_AGC_OFF,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bytesPerSample * bufferSize
                )
            }
        } catch (e: IllegalArgumentException) {
            analyzerViews.notifyToast(R.string.error_illegal_recorder_argument)
            e.printStackTrace()
            return
        }

        KLog.i(
            "Starting recorder... \n" +
                "Source: " + params.audioSourceName + "\n" +
                String.format(
                    "Sample rate: %d Hz (requested %d Hz)\n",
                    record.sampleRate,
                    sampleRate
                ) +
                String.format(
                    "Min buffer size: %d samples, %d bytes\n",
                    minBufferSize / bytesPerSample,
                    minBufferSize
                ) +
                String.format(
                    "Buffer size: %d samples, %d bytes\n",
                    bufferSize,
                    bytesPerSample * bufferSize
                ) +
                String.format(
                    "Read chunk size: %d samples, %d bytes\n",
                    readChunkSize,
                    bytesPerSample * readChunkSize
                ) +
                String.format(
                    "FFT length: %d\n",
                    fftLength
                ) +
                String.format(
                    "N FFT average: %d\n",
                    nFftAverage
                )
        )
        params.sampleRate = record.sampleRate

        if (record.state == AudioRecord.STATE_UNINITIALIZED) {
            KLog.e("Fail initializing the AudioRecord")
            analyzerViews.notifyToast(R.string.error_initializing_recorder)
            return
        }

        val audioSamples = ShortArray(readChunkSize)


        stft = STFT(params)
        val arraySize = fftLength / 2 + 1
        if (spectrumAmplitudeDBCopy == null || spectrumAmplitudeDBCopy?.size != arraySize) {
            spectrumAmplitudeDBCopy = DoubleArray(arraySize)
            cumulative = DoubleArray(arraySize)
            cumulative?.forEachIndexed { index, _ ->
                cumulative!![index] = Double.NEGATIVE_INFINITY
            }
        }

        val recorderMonitor = RecorderMonitor(
            sampleRate,
            bufferSize,
            "SamplingLoopThread::run()"
        )
        recorderMonitor.start()

        val wavWriter = WavWriter(sampleRate)
        if (saveWav) {
            wavWriter.start()
            wavSecondsRemain = wavWriter.secondsLeft()
            wavSeconds = 0.0
            KLog.i("PCM write to file '" + wavWriter.path + "'")
        }

        try {
            record.startRecording()
        } catch (e: IllegalStateException) {
            analyzerViews.notifyToast(R.string.error_start_recording)
            e.printStackTrace()
            return
        }

        // When running in this loop (including when paused), you cannot change properties
        // related to recorder: e.g. audioSourceId, sampleRate, bufferSampleSize
        while (running) {
            val numOfReadShort = if (audioSourceId >= params.idTestSignal1) {
                readTestData(
                    audioSamples,
                    0,
                    readChunkSize,
                    audioSourceId
                )
            } else {
                record.read(
                    audioSamples,
                    0,
                    readChunkSize
                )
            }
            if (recorderMonitor.updateState(numOfReadShort)) {
                if (recorderMonitor.lastCheckOverrun) {
                    analyzerViews.notifyOverrun()
                }
                if (saveWav) {
                    wavSecondsRemain = wavWriter.secondsLeft()
                }
            }
            if (saveWav) {
                wavWriter.pushAudioShort(
                    audioSamples,
                    numOfReadShort
                )
                wavSeconds = wavWriter.secondsWritten()
                analyzerViews.updateRec(wavSeconds)
            }
            if (paused) {
                // keep reading data, for overrun checker and for write wav data
                continue
            }

            stft.feedData(
                audioSamples,
                numOfReadShort
            ) // Stream

            // If there is new spectrum data, do plot
            if (stft.nElemSpectrumAmp() >= nFftAverage) {
                val spectrumAmplitudeDB = stft.calculateSpectrumAmplitudeDB()
                System.arraycopy(
                    spectrumAmplitudeDB,
                    0,
                    spectrumAmplitudeDBCopy,
                    0,
                    spectrumAmplitudeDB.size
                )

                ensureMaxValue(spectrumAmplitudeDBCopy)

                analyzerViews.update(cumulative) // Update

                stft.calculatePeak()
                val maxAmplitudeFreq = stft.maxAmplitudeFreq
                val maxAmplitudeDB = stft.maxAmplitudeDB
                listener.onUpdateAmplitude(
                    maxAmplitudeFreq,
                    maxAmplitudeDB
                )

                val rms = stft.calculateRms()
                val rmsFromFT = stft.calculateRmsFromFT()
                listener.onUpdateRms(
                    rms,
                    rmsFromFT
                )
            }
        }
        KLog.i("Actual sample rate: " + recorderMonitor.sampleRate)
        KLog.i("Stopping and releasing recorder...")
        record.stop()
        record.release()
        if (saveWav) {
            KLog.i("Ending saved wav")
            wavWriter.stop()
            analyzerViews.notifyWAVSaved(wavWriter.relativeDir)
        }
    }

    private fun readTestData(
        shorts: ShortArray,
        offsetInShorts: Int,
        sizeInShorts: Int,
        id: Int
    ): Int {
        if (data == null || data?.size != sizeInShorts) {
            data = DoubleArray(sizeInShorts)
        }
        Arrays.fill(
            data,
            0.0
        )
        when (id - params.idTestSignal1) {
            1 -> {
                sineGenerator1.getSamples(data!!)
                sineGenerator0.addSamples(data!!)
                loopShorts(
                    shorts,
                    sizeInShorts,
                    offsetInShorts
                )
            }
            // No break, so values of data added
            0 -> {
                sineGenerator0.addSamples(data!!)
                loopShorts(
                    shorts,
                    sizeInShorts,
                    offsetInShorts
                )
            }
            2 -> for (i in 0 until sizeInShorts) {
                shorts[i] = (sampleValueMax * (2.0 * Math.random() - 1)).toInt()
                    .toShort()
            }
            else -> KLog.w("This id has no source: $audioSourceId")
        }
        limitFrameRate(1000.0 * sizeInShorts / sampleRate) // Block this thread, so that behave as if read from real device
        return sizeInShorts
    }

    private fun loopShorts(
        shorts: ShortArray,
        sizeInShorts: Int,
        offsetInShorts: Int
    ) {
        for (i in 0 until sizeInShorts) {
            shorts[offsetInShorts + i] = round(data!![i]).toInt()
                .toShort()
        }
    }

    /**
     * Limit the frame rate waiting some milliseconds.
     */
    private fun limitFrameRate(updateMs: Double) {
        baseTimeMs += updateMs
        val delay = (baseTimeMs - SystemClock.uptimeMillis()).toInt().toLong()
        if (delay > 0) {
            try {
                sleep(delay)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        } else {
            baseTimeMs -= delay.toDouble()  // Get the current time
        }
    }

    fun setDbaWeighting(dbaWeighting: Boolean) {
        stft.setDbaWeighting(dbaWeighting)
    }

    fun finish() {
        running = false
        interrupt()
    }

    private fun ensureMaxValue(spectrumAmplitudeDBCopy: DoubleArray?) {
        if (spectrumAmplitudeDBCopy != null) {
            if (cumulative != null) {
                spectrumAmplitudeDBCopy.forEachIndexed { index, element ->
                    if (element > cumulative!![index]) {
                        cumulative!![index] = element
                    }
                }
            } else {
                cumulative = spectrumAmplitudeDBCopy
            }
        } else {
            cumulative = spectrumAmplitudeDBCopy
        }
    }
}
