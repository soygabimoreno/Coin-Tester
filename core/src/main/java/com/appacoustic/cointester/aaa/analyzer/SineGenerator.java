package com.appacoustic.cointester.aaa.analyzer;

/**
 * Recursive sine wave generator.
 * - compute parameters using double()
 * <p>
 * y[n] = 2cos(w)y[n-1] - y[n-2]
 * w = 2 pi f / fs
 */
public class SineGenerator {
    private double fs;    // sampling frequency
    private double k;    // recursion constant
    private double n0, n1;    // first (next) 2 samples

    /**
     * Create a sine wave generator:
     *
     * @param f  frequency of the sine wave (hz)
     * @param fs Sampling rate (hz)
     * @param a  Amplitude
     */

    public SineGenerator(double f, int fs, double a) {
        this.fs = fs;
        double w = 2.0 * Math.PI * f / fs;
        this.n0 = 0d;
        this.n1 = a * Math.cos(w + Math.PI / 2.0);
        this.k = 2.0 * Math.cos(w);
    }

    /**
     * Set the new frequency, maintaining the phase
     *
     * @param f New frequency, hz
     */

    public void setF(double f) {
        double w = 2.0 * Math.PI * f / fs;
        k = getK(f);

        double theta = Math.acos(n0);
        if (n1 > n0) theta = 2 * Math.PI - theta;
        n0 = Math.cos(theta);
        n1 = Math.cos(w + theta);
    }

    /**
     * Compute the recursion coefficient "k"
     */
    private double getK(double f) {
        double w = 2.0 * Math.PI * f / fs;
        return 2.0 * Math.cos(w);
    }

    /**
     * Generate the next batch of samples.
     *
     * @param samples Where to put the samples
     * @param start   Start sample
     * @param count   # of samples (must be even)
     */

    private void getSamples(double[] samples, int start, int count) {
        for (int cnt = start; cnt < count; cnt += 2) {
            samples[cnt] = n0 = (k * n1) - n0;
            samples[cnt + 1] = n1 = (k * n0) - n1;
        }
    }

    /**
     * Fill the supplied (even length) array with samples.
     */

    public void getSamples(double[] samples) {
        getSamples(samples, 0, samples.length);
    }

    /**
     * Add samples to an existing buffer
     *
     * @param samples Where to put the samples
     * @param start   Start sample
     * @param count   # of samples (must be even)
     */
    private void addSamples(double[] samples, int start, int count) {
        for (int cnt = start; cnt < count; cnt += 2) {
            samples[cnt] += n0 = (k * n1) - n0;
            samples[cnt + 1] += n1 = (k * n0) - n1;
        }
    }

    /**
     * Add samples to the supplied (even length) array.
     */
    public void addSamples(double[] samples) {
        addSamples(samples, 0, samples.length);
    }

    /**
     * Get the current sampling frequency.
     */

    public double getFs() {
        return fs;
    }
}