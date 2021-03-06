package com.appacoustic.cointester.core.framework.processing

import com.appacoustic.cointester.core.presentation.analyzer.domain.AnalyzerParams
import com.appacoustic.cointester.libFramework.KLog
import com.appacoustic.libprocessing.BesselCal
import com.appacoustic.libprocessing.fft.RealDoubleFFT
import com.appacoustic.libprocessing.linearToDB
import java.util.*
import kotlin.math.*

/**
 * Short Time Fourier Transform.
 */
class STFT(params: AnalyzerParams) {

    private val spectrumAmplitudeOutCumulative: DoubleArray
    private val spectrumAmplitudeOutTemp: DoubleArray
    private val spectrumAmplitudeOut: DoubleArray
    private val spectrumAmplitudeOutDB: DoubleArray
    private val spectrumAmplitudeIn: DoubleArray
    private val spectrumAmplitudeInTemp: DoubleArray
    private var spectrumAmplitudeCount = 0
    private val spectrumAmplitudeFFT: RealDoubleFFT

    private lateinit var window: DoubleArray
    private val windowFunctionName = params.windowFunctionName
    private val windowFunctionNames = params.windowFunctionNames
    private var windowEnergyFactor = 1.0 // Used to keep energy invariant under different window

    private val sampleRate = params.sampleRate
    private val fftLength = params.fftLength
    private val hopLength = params.hopLength // 50% overlap by default
    private var nAnalysed = 0
    private var dbaWeighting = false

    private var rmsCumulative = 0.0
    private var rmsCount = 0
    private var rmsOut = 0.0

    private lateinit var dbaFactor: DoubleArray // Multiply to power spectrum to get A-weighting
    private lateinit var micGain: DoubleArray

    var maxAmplitudeFreq = Double.NaN
        private set
    var maxAmplitudeDB = Double.NaN
        private set

    init {
        require(params.nFftAverage > 0) { "nFftAverage <= 0" }
        require(-fftLength and fftLength == fftLength) { "FFT length is not a power of 2" }

        val outSize = fftLength / 2 + 1
        spectrumAmplitudeOutCumulative = DoubleArray(outSize)
        spectrumAmplitudeOutTemp = DoubleArray(outSize)
        spectrumAmplitudeOut = DoubleArray(outSize)
        spectrumAmplitudeOutDB = DoubleArray(outSize)

        val inSize = fftLength
        spectrumAmplitudeIn = DoubleArray(inSize)
        spectrumAmplitudeInTemp = DoubleArray(inSize)
        spectrumAmplitudeFFT = RealDoubleFFT(spectrumAmplitudeIn.size)

        initWindowFunction()
        initDBAFactor()
        clear()

        initMicGain(params.micGainDB)
        setDbaWeighting(params.dbaWeighting)
    }

    private fun initWindowFunction() {
        window = DoubleArray(fftLength)
        val length = window.size
        if (windowFunctionName == windowFunctionNames[1]) { // Bartlett
            for (i in 0 until length) {
                window[i] = asin(sin(PI * i / length)) / PI * 2
            }
        } else if (windowFunctionName == windowFunctionNames[2]) { // Hanning, hw=1
            for (i in 0 until length) {
                window[i] = 0.5 * (1 - cos(2.0 * PI * i.toDouble() / (length - 1.0))) * 2.0
            }
        } else if (windowFunctionName == windowFunctionNames[3]) { // Blackman, hw=2
            for (i in 0 until length) {
                window[i] = 0.42 - 0.5 * cos(2.0 * PI * i.toDouble() / (length - 1)) + 0.08 * cos(4.0 * PI * i.toDouble() / (length - 1))
            }
        } else if (windowFunctionName == windowFunctionNames[4]) { // Blackman_Harris, hw=3
            for (i in 0 until length) {
                window[i] =
                    (0.35875 - 0.48829 * cos(2.0 * PI * i.toDouble() / (length - 1)) + 0.14128 * cos(4.0 * PI * i.toDouble() / (length - 1)) - 0.01168 * cos(6.0 * PI * i.toDouble() / (length - 1))) * 2
            }
        } else if (windowFunctionName == windowFunctionNames[5]) { // Kaiser, a=2.0
            kaisser(
                2.0,
                length
            )
        } else if (windowFunctionName == windowFunctionNames[6]) { // Kaiser, a=3.0
            kaisser(
                3.0,
                length
            )
        } else if (windowFunctionName == windowFunctionNames[7]) { // Kaiser, a=4.0
            kaisser(
                4.0,
                length
            )
            // 7 more window functions (by james34602, https://github.com/bewantbe/audio-analyzer-for-android/issues/14 )
        } else if (windowFunctionName == windowFunctionNames[8]) { // Flat-top
            for (i in 0 until length) {
                val f = 2.0 * PI * i.toDouble() / (length - 1)
                window[i] = 1 - 1.93 * cos(f) + 1.29 * cos(2 * f) - 0.388 * cos(3 * f) + 0.028 * cos(4 * f)
            }
        } else if (windowFunctionName == windowFunctionNames[9]) { // Nuttall
            val a0 = 0.355768
            val a1 = 0.487396
            val a2 = 0.144232
            val a3 = 0.012604
            for (i in 0 until length) {
                val scale = PI * i / (length - 1)
                window[i] = a0 - a1 * cos(2.0 * scale) + a2 * cos(4.0 * scale) - a3 * cos(6.0 * scale)
            }
        } else if (windowFunctionName == windowFunctionNames[10]) { // Gaussian, b=3.0
            gaussian(
                3.0,
                length
            )
        } else if (windowFunctionName == windowFunctionNames[11]) { // Gaussian, b=5.0
            gaussian(
                5.0,
                length
            )
        } else if (windowFunctionName == windowFunctionNames[12]) { // Gaussian, b=6.0
            gaussian(
                6.0,
                length
            )
        } else if (windowFunctionName == windowFunctionNames[13]) { // Gaussian, b=7.0
            gaussian(
                7.0,
                length
            )
        } else if (windowFunctionName == windowFunctionNames[14]) { // Gaussian, b=8.0
            gaussian(
                8.0,
                length
            )
        } else {
            for (i in 0 until length) { // Default: Rectangular
                window[i] = 1.0
            }
        }

        var normalizeFactor = 0.0
        for (aWindow in window) {
            normalizeFactor += aWindow
        }
        normalizeFactor = length / normalizeFactor
        windowEnergyFactor = 0.0
        for (i in 0 until length) {
            window[i] *= normalizeFactor
            windowEnergyFactor += window[i] * window[i]
        }
        windowEnergyFactor = length / windowEnergyFactor
    }

    private fun kaisser(
        a: Double,
        length: Int
    ) {
        val dn = BesselCal.i0(PI * a)
        for (i in 0 until length) {
            window[i] = BesselCal.i0(PI * a * sqrt(1 - (2.0 * i / (length - 1) - 1.0) * (2.0 * i / (length - 1) - 1.0))) / dn
        }
    }

    private fun gaussian(
        beta: Double,
        length: Int
    ) {
        var arg: Double
        for (i in 0 until length) {
            arg = beta * (1.0 - i.toDouble() / length.toDouble() * 2.0)
            window[i] = exp(-0.5 * (arg * arg))
        }
    }

    /**
     * Generate multiplier for A-weighting.
     */
    private fun initDBAFactor() {
        dbaFactor = DoubleArray(fftLength / 2 + 1)
        for (i in 0 until fftLength / 2 + 1) {
            val f = i.toDouble() / fftLength * sampleRate
            val r = sqr(12200.0) * sqr(sqr(f)) / ((f * f + sqr(20.6)) * sqrt((f * f + sqr(107.7)) * (f * f + sqr(737.9))) * (f * f + sqr(12200.0)))
            dbaFactor[i] = r * r * 1.58489319246111  // 1.58489319246111 = 10^(1/5)
        }
    }

    private fun clear() {
        spectrumAmplitudeCount = 0
        Arrays.fill(
            spectrumAmplitudeOut,
            0.0
        )
        Arrays.fill(
            spectrumAmplitudeOutDB,
            log10(0.0)
        )
        Arrays.fill(
            spectrumAmplitudeOutCumulative,
            0.0
        )
    }

    private fun initMicGain(micGainDB: DoubleArray?) {
        if (micGainDB != null) {
            micGain = micGainDB
            KLog.i("Calibration load")
            for (i in micGain.indices) {
                micGain[i] = 10.0.pow(micGain[i] / 10.0) // COMMENT: (dB --> Linear) It wouldn't be divide per 20 instead of 10 ???
            }
        } else {
            KLog.w("No calibration")
        }
    }

    fun calculateRms(): Double {
        if (rmsCount > 8000 / 30) {
            rmsOut = sqrt(rmsCumulative / rmsCount * 2.0) // Per 2 to normalize to sine wave
            rmsCumulative = 0.0
            rmsCount = 0
        }
        return rmsOut
    }

    fun calculateRmsFromFT(): Double {
        calculateSpectrumAmplitudeDB()
        var s = 0.0
        for (i in 1 until spectrumAmplitudeOut.size) {
            s += spectrumAmplitudeOut[i]
        }
        return sqrt(s * windowEnergyFactor)
    }

    fun calculateSpectrumAmplitudeDB(): DoubleArray {
        calculateSpectrumAmplitude()
        return spectrumAmplitudeOutDB
    }

    private fun calculateSpectrumAmplitude(): DoubleArray {
        if (nAnalysed != 0) { // If no new result
            val outLength = spectrumAmplitudeOut.size
            val spectrumAmplitudeOutCumulative = spectrumAmplitudeOutCumulative
            for (j in 0 until outLength) {
                spectrumAmplitudeOutCumulative[j] /= nAnalysed.toDouble()
            }
            if (::micGain.isInitialized && micGain.size + 1 == spectrumAmplitudeOutCumulative.size) { // No correction to phase or DC
                for (j in 1 until outLength) {
                    spectrumAmplitudeOutCumulative[j] /= micGain[j - 1]
                }
            }
            if (dbaWeighting) {
                for (j in 0 until outLength) {
                    spectrumAmplitudeOutCumulative[j] *= dbaFactor[j]
                }
            }
            System.arraycopy(
                spectrumAmplitudeOutCumulative,
                0,
                spectrumAmplitudeOut,
                0,
                outLength
            )
            Arrays.fill(
                spectrumAmplitudeOutCumulative,
                0.0
            )
            nAnalysed = 0
            for (i in 0 until outLength) {
                spectrumAmplitudeOutDB[i] = 10.0 * log10(spectrumAmplitudeOut[i])
            }
        }
        return spectrumAmplitudeOut
    }

    /**
     * Square.
     * Clarification -> sqr: square, sqrt: root square
     */
    private fun sqr(x: Double): Double {
        return x * x
    }

    fun feedData(
        data: ShortArray,
        length: Int
    ) {
        var length = length
        if (length > data.size) {
            KLog.w("Trying to read more samples than there are in the buffer")
            length = data.size
        }
        val inLength = spectrumAmplitudeIn.size
        val outLength = spectrumAmplitudeOut.size
        var i = 0 // Input data point to be read
        while (i < length) {
            while (spectrumAmplitudeCount < 0 && i < length) { // Skip data when hopLength > fftLength
                val s = data[i++] / 32768.0
                spectrumAmplitudeCount++
                rmsCumulative += s * s
                rmsCount++
            }
            while (spectrumAmplitudeCount < inLength && i < length) {
                val s = data[i++] / 32768.0
                spectrumAmplitudeIn[spectrumAmplitudeCount++] = s
                rmsCumulative += s * s
                rmsCount++
            }
            if (spectrumAmplitudeCount == inLength) { // Enough data for one FFT
                for (j in 0 until inLength) {
                    spectrumAmplitudeInTemp[j] = spectrumAmplitudeIn[j] * window[j]
                }
                spectrumAmplitudeFFT.ft(spectrumAmplitudeInTemp)
                fFTToAmplitude(
                    spectrumAmplitudeOutTemp,
                    spectrumAmplitudeInTemp
                )
                for (j in 0 until outLength) {
                    spectrumAmplitudeOutCumulative[j] += spectrumAmplitudeOutTemp[j]
                }
                nAnalysed++
                if (hopLength < fftLength) {
                    System.arraycopy(
                        spectrumAmplitudeIn,
                        hopLength,
                        spectrumAmplitudeIn,
                        0,
                        fftLength - hopLength
                    )
                }
                spectrumAmplitudeCount = fftLength - hopLength // Can be positive and negative
            }
        }
    }

    /**
     * Convert complex amplitudes to absolute amplitudes.
     */
    private fun fFTToAmplitude(
        dataOut: DoubleArray,
        data: DoubleArray
    ) {
        val length = data.size // It should be a even number
        val scaler = 2.0 * 2.0 / (length * length) // Per 2 due to there are positive and negative frequency part
        dataOut[0] = data[0] * data[0] * scaler / 4.0
        var j = 1
        var i = 1
        while (i < length - 1) {
            dataOut[j] = (data[i] * data[i] + data[i + 1] * data[i + 1]) * scaler
            i += 2
            j++
        }
        dataOut[j] = data[length - 1] * data[length - 1] * scaler / 4.0
    }

    fun calculatePeak() {
        calculateSpectrumAmplitudeDB()
        // Find and show peak amplitude
        maxAmplitudeDB = linearToDB(0.125 / 32768)
        maxAmplitudeFreq = 0.0
        for (i in 1 until spectrumAmplitudeOutDB.size) { // Skip the direct current term
            if (spectrumAmplitudeOutDB[i] > maxAmplitudeDB) {
                maxAmplitudeDB = spectrumAmplitudeOutDB[i]
                maxAmplitudeFreq = i.toDouble()
            }
        }
        maxAmplitudeFreq = maxAmplitudeFreq * sampleRate / fftLength

        // Slightly better peak finder
        // The peak around spectrumDB should look like quadratic curve after good window function
        // a*x^2 + b*x + c = y
        // a - b + c = x1
        //         c = x2
        // a + b + c = x3
        if (sampleRate / fftLength < maxAmplitudeFreq && maxAmplitudeFreq < sampleRate / 2 - sampleRate / fftLength) {
            val id = round(maxAmplitudeFreq / sampleRate * fftLength).toInt()
            val x1 = spectrumAmplitudeOutDB[id - 1]
            val x2 = spectrumAmplitudeOutDB[id]
            val x3 = spectrumAmplitudeOutDB[id + 1]
            val a = (x3 + x1) / 2 - x2
            val b = (x3 - x1) / 2
            if (a < 0) {
                val xPeak = -b / (2 * a)
                if (abs(xPeak) < 1) {
                    maxAmplitudeFreq += xPeak * sampleRate / fftLength
                    maxAmplitudeDB = (4.0 * a * x2 - b * b) / (4 * a)
                }
            }
        }
    }

    fun setDbaWeighting(dbaWeighting: Boolean) {
        this.dbaWeighting = dbaWeighting
    }

    fun nElemSpectrumAmp(): Int {
        return nAnalysed
    }
}
