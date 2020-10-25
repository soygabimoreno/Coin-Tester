/* Copyright 2014 Eddy Xiao <bewantbe@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appacoustic.cointester.analyzer.processing;

import com.appacoustic.cointester.analyzer.BesselCal;
import com.appacoustic.cointester.analyzer.model.AnalyzerParams;
import com.appacoustic.cointester.fft.RealDoubleFFT;
import com.gabrielmorenoibarra.k.util.KLog;

import java.util.Arrays;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.exp;
import static java.lang.Math.log10;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

/**
 * Short Time Fourier Transform.
 */
public class STFT_OLD {

    private double[] spectrumAmplitudeOutCumulative;
    private double[] spectrumAmplitudeOutTemp;
    private double[] spectrumAmplitudeOut;
    private double[] spectrumAmplitudeOutDB;
    private double[] spectrumAmplitudeIn;
    private double[] spectrumAmplitudeInTemp;
    private int spectrumAmplitudeCount;
    private RealDoubleFFT spectrumAmplitudeFFT;

    private double[] window;
    private String windowFunctionName;
    private String[] windowFunctionNames;
    private double windowEnergyFactor = 1; // Used to keep energy invariant under different window

    private int sampleRate;
    private int fFTLength;
    private int hopLength;
    private int nAnalysed;
    private boolean dBAWeighting;

    private double rMSCumulative;
    private int rMSCount;
    private double rMSOut;

    private double[] dBAFactor; // Multiply to power spectrum to get A-weighting
    private double[] micGain;

    private double maxAmplitudeFreq = Double.NaN;
    private double maxAmplitudeDB = Double.NaN;

    public STFT_OLD(AnalyzerParams params) {
        this.sampleRate = params.getSampleRate();
        this.fFTLength = params.getFftLength();
        this.hopLength = params.getHopLength(); // 50% overlap by default
        this.windowFunctionName = params.getWindowFunctionName();
        this.windowFunctionNames = params.getWindowFunctionNames();

        if (params.getNFftAverage() <= 0) {
            throw new IllegalArgumentException("nFftAverage <= 0");
        }
        if (((-fFTLength) & fFTLength) != fFTLength) {
            throw new IllegalArgumentException("FFT length is not a power of 2");
        }

        spectrumAmplitudeOutCumulative = new double[fFTLength / 2 + 1];
        spectrumAmplitudeOutTemp = new double[fFTLength / 2 + 1];
        spectrumAmplitudeOut = new double[fFTLength / 2 + 1];
        spectrumAmplitudeOutDB = new double[fFTLength / 2 + 1];
        spectrumAmplitudeIn = new double[fFTLength];
        spectrumAmplitudeInTemp = new double[fFTLength];
        spectrumAmplitudeFFT = new RealDoubleFFT(spectrumAmplitudeIn.length);

        initWindowFunction();
        initDBAFactor();
        clear();

        micGain = params.getMicGainDB();
        if (micGain != null) {
            KLog.Companion.i("Calibration load");
            for (int i = 0; i < micGain.length; i++) {
                micGain[i] = pow(10, micGain[i] / 10.0); // COMMENT: dB --> Linear No sería dividir entre 20 en vez de 10 ???
            }
        } else {
            KLog.Companion.w("No calibration");
        }
        setDBAWeighting(params.getDbaWeighting());
    }

    private void initWindowFunction() {
        window = new double[fFTLength];
        int length = window.length;
        if (windowFunctionName.equals(windowFunctionNames[1])) { // Bartlett
            for (int i = 0; i < length; i++) {
                window[i] = asin(sin(PI * i / length)) / PI * 2;
            }
        } else if (windowFunctionName.equals(windowFunctionNames[2])) { // Hanning, hw=1
            for (int i = 0; i < length; i++) {
                window[i] = 0.5 * (1 - cos(2 * PI * i / (length - 1.))) * 2;
            }
        } else if (windowFunctionName.equals(windowFunctionNames[3])) { // Blackman, hw=2
            for (int i = 0; i < length; i++) {
                window[i] = 0.42 - 0.5 * cos(2 * PI * i / (length - 1)) + 0.08 * cos(4 * PI * i / (length - 1));
            }
        } else if (windowFunctionName.equals(windowFunctionNames[4])) { // Blackman_Harris, hw=3
            for (int i = 0; i < length; i++) {
                window[i] = (0.35875 - 0.48829 * cos(2 * PI * i / (length - 1)) + 0.14128 * cos(4 * PI * i / (length - 1)) - 0.01168 * cos(6 * PI * i / (length - 1))) * 2;
            }
        } else if (windowFunctionName.equals(windowFunctionNames[5])) { // Kaiser, a=2.0
            kaisser(2.0, length);
        } else if (windowFunctionName.equals(windowFunctionNames[6])) { // Kaiser, a=3.0
            kaisser(3.0, length);
        } else if (windowFunctionName.equals(windowFunctionNames[7])) { // Kaiser, a=4.0
            kaisser(4.0, length);
            // 7 more window functions (by james34602, https://github.com/bewantbe/audio-analyzer-for-android/issues/14 )
        } else if (windowFunctionName.equals(windowFunctionNames[8])) { // Flat-top
            for (int i = 0; i < length; i++) {
                double f = 2 * PI * i / (length - 1);
                window[i] = 1 - 1.93 * cos(f) + 1.29 * cos(2 * f) - 0.388 * cos(3 * f) + 0.028 * cos(4 * f);
            }
        } else if (windowFunctionName.equals(windowFunctionNames[9])) { // Nuttall
            double a0 = 0.355768;
            double a1 = 0.487396;
            double a2 = 0.144232;
            double a3 = 0.012604;
            for (int i = 0; i < length; i++) {
                double scale = PI * i / (length - 1);
                window[i] = a0 - a1 * cos(2.0 * scale) + a2 * cos(4.0 * scale) - a3 * cos(6.0 * scale);
            }
        } else if (windowFunctionName.equals(windowFunctionNames[10])) { // Gaussian, b=3.0
            gaussian(3.0, length);
        } else if (windowFunctionName.equals(windowFunctionNames[11])) { // Gaussian, b=5.0
            gaussian(5.0, length);
        } else if (windowFunctionName.equals(windowFunctionNames[12])) { // Gaussian, b=6.0
            gaussian(6.0, length);
        } else if (windowFunctionName.equals(windowFunctionNames[13])) { // Gaussian, b=7.0
            gaussian(7.0, length);
        } else if (windowFunctionName.equals(windowFunctionNames[14])) { // Gaussian, b=8.0
            gaussian(8.0, length);
        } else {
            for (int i = 0; i < length; i++) { // Default: Rectangular
                window[i] = 1;
            }

        }

        double normalizeFactor = 0;
        for (double aWindow : window) {
            normalizeFactor += aWindow;
        }
        normalizeFactor = length / normalizeFactor;
        windowEnergyFactor = 0;
        for (int i = 0; i < length; i++) {
            window[i] *= normalizeFactor;
            windowEnergyFactor += window[i] * window[i];
        }
        windowEnergyFactor = length / windowEnergyFactor;
    }

    private void kaisser(double a, int length) {
        double dn = BesselCal.i0(PI * a);
        for (int i = 0; i < length; i++) {
            window[i] = BesselCal.i0(PI * a * sqrt(1 - (2.0 * i / (length - 1) - 1.0) * (2.0 * i / (length - 1) - 1.0))) / dn;
        }
    }

    private void gaussian(double beta, int length) {
        double arg;
        for (int i = 0; i < length; i++) {
            arg = (beta * (1.0 - ((double) i / (double) length) * 2.0));
            window[i] = exp(-0.5 * (arg * arg));
        }
    }

    /**
     * Generate multiplier for A-weighting.
     */
    private void initDBAFactor() {
        dBAFactor = new double[fFTLength / 2 + 1];
        for (int i = 0; i < fFTLength / 2 + 1; i++) {
            double f = (double) i / fFTLength * sampleRate;
            double r = sqr(12200) * sqr(sqr(f)) / ((f * f + sqr(20.6)) * sqrt((f * f + sqr(107.7)) * (f * f + sqr(737.9))) * (f * f + sqr(12200)));
            dBAFactor[i] = r * r * 1.58489319246111;  // 1.58489319246111 = 10^(1/5)
        }
    }

    private void clear() {
        spectrumAmplitudeCount = 0;
        Arrays.fill(spectrumAmplitudeOut, 0.0);
        Arrays.fill(spectrumAmplitudeOutDB, log10(0));
        Arrays.fill(spectrumAmplitudeOutCumulative, 0.0);
    }

    /**
     * Square.
     * Clarification -> sqr: square, sqrt: root square
     */
    private double sqr(double x) {
        return x * x;
    }

    public void feedData(short[] data, int length) {
        if (length > data.length) {
            KLog.Companion.w("Trying to read more samples than there are in the buffer");
            length = data.length;
        }
        int inLength = spectrumAmplitudeIn.length;
        int outLength = spectrumAmplitudeOut.length;
        int i = 0; // Input data point to be read
        while (i < length) {
            while (spectrumAmplitudeCount < 0 && i < length) { // Skip data when hopLength > fFTLength
                double s = data[i++] / 32768.0;
                spectrumAmplitudeCount++;
                rMSCumulative += s * s;
                rMSCount++;
            }
            while (spectrumAmplitudeCount < inLength && i < length) {
                double s = data[i++] / 32768.0;
                spectrumAmplitudeIn[spectrumAmplitudeCount++] = s;
                rMSCumulative += s * s;
                rMSCount++;
            }
            if (spectrumAmplitudeCount == inLength) { // Enough data for one FFT
                for (int j = 0; j < inLength; j++) {
                    spectrumAmplitudeInTemp[j] = spectrumAmplitudeIn[j] * window[j];
                }
                spectrumAmplitudeFFT.ft(spectrumAmplitudeInTemp);
                fFTToAmplitude(spectrumAmplitudeOutTemp, spectrumAmplitudeInTemp);
                for (int j = 0; j < outLength; j++) {
                    spectrumAmplitudeOutCumulative[j] += spectrumAmplitudeOutTemp[j];
                }
                nAnalysed++;
                if (hopLength < fFTLength) {
                    System.arraycopy(spectrumAmplitudeIn, hopLength, spectrumAmplitudeIn, 0, fFTLength - hopLength);
                }
                spectrumAmplitudeCount = fFTLength - hopLength; // Can be positive and negative
            }
        }
    }

    /**
     * Convert complex amplitudes to absolute amplitudes.
     */
    private void fFTToAmplitude(double[] dataOut, double[] data) {
        int length = data.length; // It should be a even number
        double scaler = 2.0 * 2.0 / (length * length); // Per 2 due to there are positive and negative frequency part
        dataOut[0] = data[0] * data[0] * scaler / 4.0;
        int j = 1;
        for (int i = 1; i < length - 1; i += 2, j++) {
            dataOut[j] = (data[i] * data[i] + data[i + 1] * data[i + 1]) * scaler;
        }
        dataOut[j] = data[length - 1] * data[length - 1] * scaler / 4.0;
    }

    public double getRMS() {
        if (rMSCount > 8000 / 30) {
            rMSOut = sqrt(rMSCumulative / rMSCount * 2.0); // Per 2 to normalize to sine wave.
            rMSCumulative = 0;
            rMSCount = 0;
        }
        return rMSOut;
    }

    public double getRMSFromFT() {
        getSpectrumAmplitudeDB();
        double s = 0;
        for (int i = 1; i < spectrumAmplitudeOut.length; i++) {
            s += spectrumAmplitudeOut[i];
        }
        return sqrt(s * windowEnergyFactor);
    }

    public final double[] getSpectrumAmplitudeDB() {
        getSpectrumAmplitude();
        return spectrumAmplitudeOutDB;
    }

    final double[] getSpectrumAmplitude() {
        if (nAnalysed != 0) { // No new result
            int outLen = spectrumAmplitudeOut.length;
            double[] sAOC = spectrumAmplitudeOutCumulative;
            for (int j = 0; j < outLen; j++) {
                sAOC[j] /= nAnalysed;
            }
            if (micGain != null && micGain.length + 1 == sAOC.length) { // No correction to phase or DC
                for (int j = 1; j < outLen; j++) {
                    sAOC[j] /= micGain[j - 1];
                }
            }
            if (dBAWeighting) {
                for (int j = 0; j < outLen; j++) {
                    sAOC[j] *= dBAFactor[j];
                }
            }
            System.arraycopy(sAOC, 0, spectrumAmplitudeOut, 0, outLen);
            Arrays.fill(sAOC, 0.0);
            nAnalysed = 0;
            for (int i = 0; i < outLen; i++) {
                spectrumAmplitudeOutDB[i] = 10.0 * log10(spectrumAmplitudeOut[i]);
            }
        }
        return spectrumAmplitudeOut;
    }

    public void calculatePeak() {
        getSpectrumAmplitudeDB();
        // Find and show peak amplitude
        maxAmplitudeDB = 20 * log10(0.125 / 32768);
        maxAmplitudeFreq = 0;
        for (int i = 1; i < spectrumAmplitudeOutDB.length; i++) { // Skip the direct current term
            if (spectrumAmplitudeOutDB[i] > maxAmplitudeDB) {
                maxAmplitudeDB = spectrumAmplitudeOutDB[i];
                maxAmplitudeFreq = i;
            }
        }
        maxAmplitudeFreq = maxAmplitudeFreq * sampleRate / fFTLength;

        // Slightly better peak finder
        // The peak around spectrumDB should look like quadratic curve after good window function
        // a*x^2 + b*x + c = y
        // a - b + c = x1
        //         c = x2
        // a + b + c = x3
        if (sampleRate / fFTLength < maxAmplitudeFreq && maxAmplitudeFreq < sampleRate / 2 - sampleRate / fFTLength) {
            int id = (int) (round(maxAmplitudeFreq / sampleRate * fFTLength));
            double x1 = spectrumAmplitudeOutDB[id - 1];
            double x2 = spectrumAmplitudeOutDB[id];
            double x3 = spectrumAmplitudeOutDB[id + 1];
            double c = x2;
            double a = (x3 + x1) / 2 - x2;
            double b = (x3 - x1) / 2;
            if (a < 0) {
                double xPeak = -b / (2 * a);
                if (abs(xPeak) < 1) {
                    maxAmplitudeFreq += xPeak * sampleRate / fFTLength;
                    maxAmplitudeDB = (4 * a * c - b * b) / (4 * a);
                }
            }
        }
    }

    public double getMaxAmplitudeFreq() {
        return maxAmplitudeFreq;
    }

    public double getMaxAmplitudeDB() {
        return maxAmplitudeDB;
    }

    public void setDBAWeighting(boolean dBAWeighting) {
        this.dBAWeighting = dBAWeighting;
    }

    public int nElemSpectrumAmp() {
        return nAnalysed;
    }
}