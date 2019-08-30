package com.appacoustic.cointester.analyzer.processing

import com.appacoustic.cointester.analyzer.BesselCal
import com.appacoustic.cointester.analyzer.model.AnalyzerParams
import com.appacoustic.cointester.fft.RealDoubleFFT
import com.gabrielmorenoibarra.k.util.KLog

import java.util.Arrays

import java.lang.Math.PI
import java.lang.Math.abs
import java.lang.Math.asin
import java.lang.Math.cos
import java.lang.Math.exp
import java.lang.Math.log10
import java.lang.Math.pow
import java.lang.Math.round
import java.lang.Math.sin
import java.lang.Math.sqrt

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
    private var spectrumAmplitudeCount: Int = 0
    private val spectrumAmplitudeFFT: RealDoubleFFT

    private lateinit var window: DoubleArray
    private val windowFunctionName: String?
    private val windowFunctionNames: Array<String>
    private var windowEnergyFactor = 1.0 // Used to keep energy invariant under different window

    private val sampleRate: Int
    private val fFTLength: Int
    private val hopLength: Int
    private var nAnalysed: Int = 0
    private var dBAWeighting: Boolean = false

    private var rMSCumulative: Double = 0.toDouble()
    private var rMSCount: Int = 0
    private var rMSOut: Double = 0.toDouble()

    private lateinit var dBAFactor: DoubleArray // Multiply to power spectrum to get A-weighting
    private val micGain: DoubleArray?

    var maxAmplitudeFreq = java.lang.Double.NaN
        private set
    var maxAmplitudeDB = java.lang.Double.NaN
        private set

    // Per 2 to normalize to sine wave.
    val rms: Double
        get() {
            if (rMSCount > 8000 / 30) {
                rMSOut = sqrt(rMSCumulative / rMSCount * 2.0)
                rMSCumulative = 0.0
                rMSCount = 0
            }
            return rMSOut
        }

    val rmsFromFT: Double
        get() {
            spectrumAmplitudeDB
            var s = 0.0
            for (i in 1 until spectrumAmplitudeOut.size) {
                s += spectrumAmplitudeOut[i]
            }
            return sqrt(s * windowEnergyFactor)
        }

    val spectrumAmplitudeDB: DoubleArray
        get() {
            spectrumAmplitude
            return spectrumAmplitudeOutDB
        }

    internal// No new result
    // No correction to phase or DC
    val spectrumAmplitude: DoubleArray
        get() {
            if (nAnalysed != 0) {
                val outLen = spectrumAmplitudeOut.size
                val sAOC = spectrumAmplitudeOutCumulative
                for (j in 0 until outLen) {
                    sAOC[j] /= nAnalysed.toDouble()
                }
                if (micGain != null && micGain.size + 1 == sAOC.size) {
                    for (j in 1 until outLen) {
                        sAOC[j] /= micGain[j - 1]
                    }
                }
                if (dBAWeighting) {
                    for (j in 0 until outLen) {
                        sAOC[j] *= dBAFactor!![j]
                    }
                }
                System.arraycopy(sAOC, 0, spectrumAmplitudeOut, 0, outLen)
                Arrays.fill(sAOC, 0.0)
                nAnalysed = 0
                for (i in 0 until outLen) {
                    spectrumAmplitudeOutDB[i] = 10.0 * log10(spectrumAmplitudeOut[i])
                }
            }
            return spectrumAmplitudeOut
        }

    init {
        this.sampleRate = params.sampleRate
        this.fFTLength = params.fftLength
        this.hopLength = params.hopLength // 50% overlap by default
        this.windowFunctionName = params.windowFunctionName
        this.windowFunctionNames = params.windowFunctionNames

        require(params.nFftAverage > 0) { "nFftAverage <= 0" }
        require(-fFTLength and fFTLength == fFTLength) { "FFT length is not a power of 2" }

        spectrumAmplitudeOutCumulative = DoubleArray(fFTLength / 2 + 1)
        spectrumAmplitudeOutTemp = DoubleArray(fFTLength / 2 + 1)
        spectrumAmplitudeOut = DoubleArray(fFTLength / 2 + 1)
        spectrumAmplitudeOutDB = DoubleArray(fFTLength / 2 + 1)
        spectrumAmplitudeIn = DoubleArray(fFTLength)
        spectrumAmplitudeInTemp = DoubleArray(fFTLength)
        spectrumAmplitudeFFT = RealDoubleFFT(spectrumAmplitudeIn.size)

        initWindowFunction()
        initDBAFactor()
        clear()

        micGain = params.micGainDB
        if (micGain != null) {
            KLog.i("Calibration load")
            for (i in micGain.indices) {
                micGain[i] = pow(10.0, micGain[i] / 10.0) // COMMENT: dB --> Linear No sería dividir entre 20 en vez de 10 ???
            }
        } else {
            KLog.w("No calibration")
        }
        setDBAWeighting(params.dBAWeighting)
    }

    private fun initWindowFunction() {
        window = DoubleArray(fFTLength)
        val length = window!!.size
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
                window[i] = (0.35875 - 0.48829 * cos(2.0 * PI * i.toDouble() / (length - 1)) + 0.14128 * cos(4.0 * PI * i.toDouble() / (length - 1)) - 0.01168 * cos(6.0 * PI * i.toDouble() / (length - 1))) * 2
            }
        } else if (windowFunctionName == windowFunctionNames[5]) { // Kaiser, a=2.0
            kaisser(2.0, length)
        } else if (windowFunctionName == windowFunctionNames[6]) { // Kaiser, a=3.0
            kaisser(3.0, length)
        } else if (windowFunctionName == windowFunctionNames[7]) { // Kaiser, a=4.0
            kaisser(4.0, length)
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
            gaussian(3.0, length)
        } else if (windowFunctionName == windowFunctionNames[11]) { // Gaussian, b=5.0
            gaussian(5.0, length)
        } else if (windowFunctionName == windowFunctionNames[12]) { // Gaussian, b=6.0
            gaussian(6.0, length)
        } else if (windowFunctionName == windowFunctionNames[13]) { // Gaussian, b=7.0
            gaussian(7.0, length)
        } else if (windowFunctionName == windowFunctionNames[14]) { // Gaussian, b=8.0
            gaussian(8.0, length)
        } else {
            for (i in 0 until length) { // Default: Rectangular
                window[i] = 1.0
            }

        }

        var normalizeFactor = 0.0
        for (aWindow in window!!) {
            normalizeFactor += aWindow
        }
        normalizeFactor = length / normalizeFactor
        windowEnergyFactor = 0.0
        for (i in 0 until length) {
            window[i] *= normalizeFactor
            windowEnergyFactor += window!![i] * window!![i]
        }
        windowEnergyFactor = length / windowEnergyFactor
    }

    private fun kaisser(a: Double, length: Int) {
        val dn = BesselCal.i0(PI * a)
        for (i in 0 until length) {
            window[i] = BesselCal.i0(PI * a * sqrt(1 - (2.0 * i / (length - 1) - 1.0) * (2.0 * i / (length - 1) - 1.0))) / dn
        }
    }

    private fun gaussian(beta: Double, length: Int) {
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
        dBAFactor = DoubleArray(fFTLength / 2 + 1)
        for (i in 0 until fFTLength / 2 + 1) {
            val f = i.toDouble() / fFTLength * sampleRate
            val r = sqr(12200.0) * sqr(sqr(f)) / ((f * f + sqr(20.6)) * sqrt((f * f + sqr(107.7)) * (f * f + sqr(737.9))) * (f * f + sqr(12200.0)))
            dBAFactor[i] = r * r * 1.58489319246111  // 1.58489319246111 = 10^(1/5)
        }
    }

    private fun clear() {
        spectrumAmplitudeCount = 0
        Arrays.fill(spectrumAmplitudeOut, 0.0)
        Arrays.fill(spectrumAmplitudeOutDB, log10(0.0))
        Arrays.fill(spectrumAmplitudeOutCumulative, 0.0)
    }

    /**
     * Square.
     * Clarification -> sqr: square, sqrt: root square
     */
    private fun sqr(x: Double): Double {
        return x * x
    }

    fun feedData(data: ShortArray, length: Int) {
        var length = length
        if (length > data.size) {
            KLog.w("Trying to read more samples than there are in the buffer")
            length = data.size
        }
        val inLength = spectrumAmplitudeIn.size
        val outLength = spectrumAmplitudeOut.size
        var i = 0 // Input data point to be read
        while (i < length) {
            while (spectrumAmplitudeCount < 0 && i < length) { // Skip data when hopLength > fFTLength
                val s = data[i++] / 32768.0
                spectrumAmplitudeCount++
                rMSCumulative += s * s
                rMSCount++
            }
            while (spectrumAmplitudeCount < inLength && i < length) {
                val s = data[i++] / 32768.0
                spectrumAmplitudeIn[spectrumAmplitudeCount++] = s
                rMSCumulative += s * s
                rMSCount++
            }
            if (spectrumAmplitudeCount == inLength) { // Enough data for one FFT
                for (j in 0 until inLength) {
                    spectrumAmplitudeInTemp[j] = spectrumAmplitudeIn[j] * window!![j]
                }
                spectrumAmplitudeFFT.ft(spectrumAmplitudeInTemp)
                fFTToAmplitude(spectrumAmplitudeOutTemp, spectrumAmplitudeInTemp)
                for (j in 0 until outLength) {
                    spectrumAmplitudeOutCumulative[j] += spectrumAmplitudeOutTemp[j]
                }
                nAnalysed++
                if (hopLength < fFTLength) {
                    System.arraycopy(spectrumAmplitudeIn, hopLength, spectrumAmplitudeIn, 0, fFTLength - hopLength)
                }
                spectrumAmplitudeCount = fFTLength - hopLength // Can be positive and negative
            }
        }
    }

    /**
     * Convert complex amplitudes to absolute amplitudes.
     */
    private fun fFTToAmplitude(dataOut: DoubleArray, data: DoubleArray) {
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
        spectrumAmplitudeDB
        // Find and show peak amplitude
        maxAmplitudeDB = 20 * log10(0.125 / 32768)
        maxAmplitudeFreq = 0.0
        for (i in 1 until spectrumAmplitudeOutDB.size) { // Skip the direct current term
            if (spectrumAmplitudeOutDB[i] > maxAmplitudeDB) {
                maxAmplitudeDB = spectrumAmplitudeOutDB[i]
                maxAmplitudeFreq = i.toDouble()
            }
        }
        maxAmplitudeFreq = maxAmplitudeFreq * sampleRate / fFTLength

        // Slightly better peak finder
        // The peak around spectrumDB should look like quadratic curve after good window function
        // a*x^2 + b*x + c = y
        // a - b + c = x1
        //         c = x2
        // a + b + c = x3
        if (sampleRate / fFTLength < maxAmplitudeFreq && maxAmplitudeFreq < sampleRate / 2 - sampleRate / fFTLength) {
            val id = round(maxAmplitudeFreq / sampleRate * fFTLength).toInt()
            val x1 = spectrumAmplitudeOutDB[id - 1]
            val x2 = spectrumAmplitudeOutDB[id]
            val x3 = spectrumAmplitudeOutDB[id + 1]
            val a = (x3 + x1) / 2 - x2
            val b = (x3 - x1) / 2
            if (a < 0) {
                val xPeak = -b / (2 * a)
                if (abs(xPeak) < 1) {
                    maxAmplitudeFreq += xPeak * sampleRate / fFTLength
                    maxAmplitudeDB = (4.0 * a * x2 - b * b) / (4 * a)
                }
            }
        }
    }

    fun setDBAWeighting(dBAWeighting: Boolean) {
        this.dBAWeighting = dBAWeighting
    }

    fun nElemSpectrumAmp(): Int {
        return nAnalysed
    }
}