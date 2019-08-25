package com.appacoustic.cointester.analyzer.model

import android.media.AudioFormat
import android.media.AudioRecord
import android.os.SystemClock
import com.appacoustic.cointester.AnalyzerFragment
import com.appacoustic.cointester.analyzer.RecorderMonitor
import com.appacoustic.cointester.analyzer.SineGenerator
import com.appacoustic.cointester.analyzer.WavWriter
import com.appacoustic.cointester.analyzer.processing.STFT
import com.appacoustic.cointester.utils.Tools
import com.gabrielmorenoibarra.k.util.KLog
import java.util.*

/**
 * Read a snapshot of audio data at a regular interval and compute the FFT.
 */
class SamplingLoop(private val analyzerFragment: AnalyzerFragment,
                   private val params: AnalyzerParams)
    : Thread() {

    companion object {
        private const val TEST_SIGNAL_1_FREQ_1 = 440.0
        private const val TEST_SIGNAL_1_DB_1 = -6.0
        private const val TEST_SIGNAL_2_FREQ_1 = 625.0
        private const val TEST_SIGNAL_2_DB_1 = -6.0
        private const val TEST_SIGNAL_2_FREQ_2 = 1875.0
        private const val TEST_SIGNAL_2_DB_2 = -12.0
    }

    @Volatile
    private var running = true
    @Volatile
    var pause: Boolean = false

    private lateinit var stft: STFT

    private var sineGenerator0: SineGenerator
    private val sineGenerator1: SineGenerator
    private var spectrumDBCopy: DoubleArray? = null // Transfer data from SamplingLoop to AnalyzerGraphic

    private val bytesPerSample = AnalyzerParams.BYTES_PER_SAMPLE
    private val sampleValueMax = AnalyzerParams.SAMPLE_VALUE_MAX
    private val sampleRate = params.sampleRate
    private val audioSourceId = params.audioSourceId
    private val fftLength = params.fftLength
    private val nFftAverage = params.nFftAverage
    private val analyzerViews = analyzerFragment.analyzerViews

    @Volatile
    var wavSecondsRemain = 0.0
    @Volatile
    var wavSeconds = 0.0

    private var baseTimeMs = SystemClock.uptimeMillis().toDouble()
    private var data: DoubleArray? = null

    init {
        pause = analyzerFragment.tvRun.value == "stop"
        val amp0 = Tools.dBToLinear(TEST_SIGNAL_1_DB_1)
        val amp1 = Tools.dBToLinear(TEST_SIGNAL_2_DB_1)
        val amp2 = Tools.dBToLinear(TEST_SIGNAL_2_DB_2)
        sineGenerator0 = if (audioSourceId == AnalyzerParams.idTestSignal1) {
            SineGenerator(TEST_SIGNAL_1_FREQ_1, sampleRate, sampleValueMax * amp0)
        } else {
            SineGenerator(TEST_SIGNAL_2_FREQ_1, sampleRate, sampleValueMax * amp1)
        }
        sineGenerator1 = SineGenerator(TEST_SIGNAL_2_FREQ_2, sampleRate, sampleValueMax * amp2)
    }

    override fun run() {
        val METHOD_NAME = Thread.currentThread().stackTrace[2].methodName
        val record: AudioRecord
        val timeStart = SystemClock.uptimeMillis()
        try {
            analyzerFragment.graphInit.join()  // TODO: Seems not working as intended
        } catch (e: InterruptedException) {
            KLog.w("$METHOD_NAME: analyzerFragment.graphInit.join() failed")
        }

        val timeEnd = SystemClock.uptimeMillis()
        val time = timeEnd - timeStart
        if (time < 500) {
            val timeWaiting = 500 - time
            KLog.i(METHOD_NAME + "Wait " + timeWaiting + " ms more...")
            try {
                sleep(timeWaiting)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }

        val minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT)
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            KLog.e("$METHOD_NAME: Invalid AudioRecord parameters")
            return
        }

        var readChunkSize = params.hopLength // Every hopLength one fft result (overlapped analyze window)
        readChunkSize = Math.min(readChunkSize, 2048) // Smaller chunk, smaller delay
        var bufferSampleSize = Math.max(minBufferSize / bytesPerSample, fftLength / 2) * 2
        bufferSampleSize = Math.ceil(1.0 * sampleRate / bufferSampleSize).toInt() * bufferSampleSize // Tolerate up to about 1 sec

        // Use the mic with AGC (Automatic Gain Control) turned off
        // The buffer size here seems not relate to the delay.
        // So choose a larger size (~1sec) so that overrun is unlikely.
        try {
            if (audioSourceId < AnalyzerParams.idTestSignal1) {
                record = AudioRecord(audioSourceId, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bytesPerSample * bufferSampleSize)
            } else {
                record = AudioRecord(AnalyzerParams.RECORDER_AGC_OFF, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bytesPerSample * bufferSampleSize)
            }
        } catch (e: IllegalArgumentException) {
            KLog.e(METHOD_NAME + "Fail to initialize recorder")
            analyzerViews.notifyToast("Illegal recorder argument: change source")
            return
        }

        KLog.i(METHOD_NAME + ": Starting recorder... \n" +
                "Source: " + params.audioSourceName + "\n" +
                String.format("Sample rate: %d Hz (requested %d Hz)\n", record.sampleRate, sampleRate) +
                String.format("Min buffer size: %d samples, %d bytes\n", minBufferSize / bytesPerSample, minBufferSize) +
                String.format("Buffer size: %d samples, %d bytes\n", bufferSampleSize, bytesPerSample * bufferSampleSize) +
                String.format("Read chunk size: %d samples, %d bytes\n", readChunkSize, bytesPerSample * readChunkSize) +
                String.format("FFT length: %d\n", fftLength) +
                String.format("N FFT average: %d\n", nFftAverage))
        params.sampleRate = record.sampleRate

        if (record.state == AudioRecord.STATE_UNINITIALIZED) {
            KLog.e("$METHOD_NAME: Fail initializing the AudioRecord")
            analyzerViews.notifyToast("Fail initializing the recorder.")
            return
        }

        val audioSamples = ShortArray(readChunkSize)
        var numOfReadShort: Int

        stft = STFT(params)
        if (spectrumDBCopy == null || spectrumDBCopy!!.size != fftLength / 2 + 1) {
            spectrumDBCopy = DoubleArray(fftLength / 2 + 1)
        }

        val recorderMonitor = RecorderMonitor(sampleRate, bufferSampleSize, "SamplingLoop::run()")
        recorderMonitor.start()

        //      FPSCounter fpsCounter = new FPSCounter("SamplingLoop::run()"); // ERASE ???

        val wavWriter = WavWriter(sampleRate)
        val saveWav = analyzerFragment.saveWav // Change of saveWav during loop will only affect next enter
        if (saveWav) {
            wavWriter.start()
            wavSecondsRemain = wavWriter.secondsLeft()
            wavSeconds = 0.0
            KLog.i(METHOD_NAME + "PCM write to file '" + wavWriter.path + "'")
        }

        try {
            record.startRecording()
        } catch (e: IllegalStateException) {
            val error = "Fail start recording"
            KLog.e("$METHOD_NAME: $error")
            analyzerViews.notifyToast(error)
            return
        }

        // COMMENT: Main loop
        // When running in this loop (including when paused), you can not change properties
        // related to recorder: e.g. audioSourceId, sampleRate, bufferSampleSize
        // TODO: allow change of FFT length on the fly
        while (running) {
            if (audioSourceId >= AnalyzerParams.idTestSignal1) {
                numOfReadShort = readTestData(audioSamples, 0, readChunkSize, audioSourceId)
            } else {
                numOfReadShort = record.read(audioSamples, 0, readChunkSize)   // Read
            }
            if (recorderMonitor.updateState(numOfReadShort)) {  // performed a check
                if (recorderMonitor.lastCheckOverrun)
                    analyzerViews.notifyOverrun()
                if (saveWav)
                    wavSecondsRemain = wavWriter.secondsLeft()
            }
            if (saveWav) {
                wavWriter.pushAudioShort(audioSamples, numOfReadShort)  // Maybe move this to another thread?
                wavSeconds = wavWriter.secondsWritten()
                analyzerViews.updateRec(wavSeconds)
            }
            if (pause) {
                //          fpsCounter.increment();
                // keep reading data, for overrun checker and for write wav data
                continue
            }

            stft.feedData(audioSamples, numOfReadShort) // Stream

            // If there is new spectrum data, do plot
            if (stft.nElemSpectrumAmp() >= nFftAverage) {
                // Update spectrum or spectrogram
                val spectrumDB = stft.spectrumAmpDB
                System.arraycopy(spectrumDB, 0, spectrumDBCopy, 0, spectrumDB.size)
                analyzerViews.update(spectrumDBCopy) // Update
                //          fpsCounter.increment();

                stft.calculatePeak()
                analyzerFragment.maxAmpFreq = stft.maxAmplitudeFreq
                analyzerFragment.maxAmpDB = stft.maxAmplitudeDB

                // get RMS
                analyzerFragment.dtRMS = stft.rms
                analyzerFragment.dtRMSFromFT = stft.rmsFromFT
            }
        }
        KLog.i(METHOD_NAME + ": Actual sample rate: " + recorderMonitor.sampleRate)
        KLog.i("$METHOD_NAME: Stopping and releasing recorder.")
        record.stop()
        record.release()
        if (saveWav) {
            KLog.i("$METHOD_NAME: Ending saved wav.")
            wavWriter.stop()
            analyzerViews.notifyWAVSaved(wavWriter.relativeDir)
        }
    }

    private fun readTestData(a: ShortArray, offsetInShorts: Int, sizeInShorts: Int, id: Int): Int {
        if (data == null || data!!.size != sizeInShorts) {
            data = DoubleArray(sizeInShorts)
        }
        Arrays.fill(data, 0.0)
        when (id - AnalyzerParams.idTestSignal1) {
            1 -> {
                sineGenerator1.getSamples(data)
                sineGenerator0.addSamples(data)
                for (i in 0 until sizeInShorts) {
                    a[offsetInShorts + i] = Math.round(data!![i]).toShort()
                }
            }
            // No break, so values of data added
            0 -> {
                sineGenerator0.addSamples(data)
                for (i in 0 until sizeInShorts) {
                    a[offsetInShorts + i] = Math.round(data!![i]).toShort()
                }
            }
            2 -> for (i in 0 until sizeInShorts) {
                a[i] = (sampleValueMax * (2.0 * Math.random() - 1)).toShort()
            }
            else -> KLog.w(Thread.currentThread().stackTrace[2].methodName + ": this id has no source: " + audioSourceId)
        }
        limitFrameRate(1000.0 * sizeInShorts / sampleRate) // Block this thread, so that behave as if read from real device
        return sizeInShorts
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

    fun setAWeighting(isAWeighting: Boolean) {
        stft.setDBAWeighting(isAWeighting)
    }

    fun finish() {
        running = false
        interrupt()
    }
}
