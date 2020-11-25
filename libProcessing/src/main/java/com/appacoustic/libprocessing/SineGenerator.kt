package com.appacoustic.libprocessing

/**
 * Recursive sine wave generator.
 * - compute parameters using double()
 *
 *
 * y[n] = 2cos(w)y[n-1] - y[n-2]
 * w = 2 pi f / fs
 */
class SineGenerator(
    f: Double,
    fs: Int,
    a: Double
) {
    /**
     * Get the current sampling frequency.
     */
    val fs // sampling frequency
        : Double
    private var k // recursion constant
        : Double
    private var n0: Double
    private var n1 // first (next) 2 samples
        : Double

    /**
     * Set the new frequency, maintaining the phase
     *
     * @param f New frequency, hz
     */
    fun setF(f: Double) {
        val w = 2.0 * Math.PI * f / fs
        k = getK(f)
        var theta = Math.acos(n0)
        if (n1 > n0) theta = 2 * Math.PI - theta
        n0 = Math.cos(theta)
        n1 = Math.cos(w + theta)
    }

    /**
     * Compute the recursion coefficient "k"
     */
    private fun getK(f: Double): Double {
        val w = 2.0 * Math.PI * f / fs
        return 2.0 * Math.cos(w)
    }

    /**
     * Generate the next batch of samples.
     *
     * @param samples Where to put the samples
     * @param start   Start sample
     * @param count   # of samples (must be even)
     */
    private fun getSamples(
        samples: DoubleArray,
        start: Int,
        count: Int
    ) {
        var cnt = start
        while (cnt < count) {
            n0 = k * n1 - n0
            samples[cnt] = n0
            n1 = k * n0 - n1
            samples[cnt + 1] = n1
            cnt += 2
        }
    }

    /**
     * Fill the supplied (even length) array with samples.
     */
    fun getSamples(samples: DoubleArray) {
        getSamples(
            samples,
            0,
            samples.size
        )
    }

    /**
     * Add samples to an existing buffer
     *
     * @param samples Where to put the samples
     * @param start   Start sample
     * @param count   # of samples (must be even)
     */
    private fun addSamples(
        samples: DoubleArray,
        start: Int,
        count: Int
    ) {
        var cnt = start
        while (cnt < count) {
            n0 = k * n1 - n0
            samples[cnt] += n0
            n1 = k * n0 - n1
            samples[cnt + 1] += n1
            cnt += 2
        }
    }

    /**
     * Add samples to the supplied (even length) array.
     */
    fun addSamples(samples: DoubleArray) {
        addSamples(
            samples,
            0,
            samples.size
        )
    }

    /**
     * Create a sine wave generator:
     *
     * @param f  frequency of the sine wave (hz)
     * @param fs Sampling rate (hz)
     * @param a  Amplitude
     */
    init {
        this.fs = fs.toDouble()
        val w = 2.0 * Math.PI * f / fs
        n0 = 0.0
        n1 = a * Math.cos(w + Math.PI / 2.0)
        k = 2.0 * Math.cos(w)
    }
}
