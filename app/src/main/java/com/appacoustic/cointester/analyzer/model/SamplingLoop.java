package com.appacoustic.cointester.analyzer.model;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.SystemClock;

import com.appacoustic.cointester.AnalyzerFragment;
import com.appacoustic.cointester.analyzer.RecorderMonitor;
import com.appacoustic.cointester.analyzer.SineGenerator;
import com.appacoustic.cointester.analyzer.WavWriter;
import com.appacoustic.cointester.analyzer.processing.STFT;
import com.appacoustic.cointester.analyzer.view.AnalyzerViews;
import com.gabrielmorenoibarra.k.util.KLog;
import com.appacoustic.cointester.utils.Tools;

import java.util.Arrays;

/**
 * Read a snapshot of audio data at a regular interval and compute the FFT.
 */
public class SamplingLoop extends Thread {

    public static final String TAG = SamplingLoop.class.getSimpleName();

    private static final double TEST_SIGNAL_1_FREQ_1 = 440.0;
    private static final double TEST_SIGNAL_1_DB_1 = -6.0;
    private static final double TEST_SIGNAL_2_FREQ_1 = 625.0;
    private static final double TEST_SIGNAL_2_DB_1 = -6.0;
    private static final double TEST_SIGNAL_2_FREQ_2 = 1875.0;
    private static final double TEST_SIGNAL_2_DB_2 = -12.0;

    private volatile boolean running = true;
    private volatile boolean paused;
    private STFT sTFT;

    private SineGenerator sineGenerator0;
    private SineGenerator sineGenerator1;
    private double[] spectrumDBCopy; // Transfer data from SamplingLoop to AnalyzerGraphic

    private final AnalyzerFragment analyzerFragment;
    private final AnalyzerParams params;
    private final int bytesPerSample;
    private final double sampleValueMax;
    private final int sampleRate;
    private final int audioSourceId;
    private int fFTLength;
    private int nFFTAverage;
    private final AnalyzerViews analyzerViews;

    private volatile double wavSecondsRemain;
    private volatile double wavSeconds;

    private double baseTimeMs = SystemClock.uptimeMillis();
    private double[] data;

    public SamplingLoop(AnalyzerFragment analyzerFragment, AnalyzerParams params) {
        this.analyzerFragment = analyzerFragment;
        this.params = params;
        bytesPerSample = AnalyzerParams.Companion.getBYTES_PER_SAMPLE();
        sampleValueMax = AnalyzerParams.Companion.getSAMPLE_VALUE_MAX();
        audioSourceId = params.getAudioSourceId();
        sampleRate = params.getSampleRate();
        fFTLength = params.getFftLength();
        nFFTAverage = params.getNFftAverage();
        analyzerViews = analyzerFragment.getAnalyzerViews();

        paused = analyzerFragment.getTvRun().getValue().equals("stop");
        double amp0 = Tools.dBToLinear(TEST_SIGNAL_1_DB_1);
        double amp1 = Tools.dBToLinear(TEST_SIGNAL_2_DB_1);
        double amp2 = Tools.dBToLinear(TEST_SIGNAL_2_DB_2);
        if (audioSourceId == AnalyzerParams.Companion.getID_TEST_SIGNAL_1()) {
            sineGenerator0 = new SineGenerator(TEST_SIGNAL_1_FREQ_1, sampleRate, sampleValueMax * amp0);
        } else {
            sineGenerator0 = new SineGenerator(TEST_SIGNAL_2_FREQ_1, sampleRate, sampleValueMax * amp1);
        }
        sineGenerator1 = new SineGenerator(TEST_SIGNAL_2_FREQ_2, sampleRate, sampleValueMax * amp2);
    }

    @Override
    public void run() {
        final String METHOD_NAME = Thread.currentThread().getStackTrace()[2].getMethodName();
        AudioRecord record;
        long timeStart = SystemClock.uptimeMillis();
        try {
            analyzerFragment.graphInit.join();  // TODO: Seems not working as intended
        } catch (InterruptedException e) {
            KLog.Companion.w(METHOD_NAME + ": analyzerFragment.graphInit.join() failed");
        }
        long timeEnd = SystemClock.uptimeMillis();
        long time = timeEnd - timeStart;
        if (time < 500) {
            long timeWaiting = 500 - time;
            KLog.Companion.i(METHOD_NAME + "Wait " + timeWaiting + " ms more...");
            try {
                Thread.sleep(timeWaiting);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            KLog.Companion.e(METHOD_NAME + ": Invalid AudioRecord parameters");
            return;
        }

        int readChunkSize = params.getHopLength(); // Every hopLength one fft result (overlapped analyze window)
        readChunkSize = Math.min(readChunkSize, 2048); // Smaller chunk, smaller delay
        int bufferSampleSize = Math.max(minBufferSize / bytesPerSample, fFTLength / 2) * 2;
        bufferSampleSize = (int) Math.ceil(1.0 * sampleRate / bufferSampleSize) * bufferSampleSize; // Tolerate up to about 1 sec

        // Use the mic with AGC (Automatic Gain Control) turned off
        // The buffer size here seems not relate to the delay.
        // So choose a larger size (~1sec) so that overrun is unlikely.
        try {
            if (audioSourceId < AnalyzerParams.Companion.getID_TEST_SIGNAL_1()) {
                record = new AudioRecord(audioSourceId, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bytesPerSample * bufferSampleSize);
            } else {
                record = new AudioRecord(AnalyzerParams.Companion.getRECORDER_AGC_OFF(), sampleRate, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bytesPerSample * bufferSampleSize);
            }
        } catch (IllegalArgumentException e) {
            KLog.Companion.e(METHOD_NAME + "Fail to initialize recorder");
            analyzerViews.notifyToast("Illegal recorder argument: change source");
            return;
        }
        KLog.Companion.i(METHOD_NAME + ": Starting recorder... \n" +
                "Source: " + params.getAudioSourceName() + "\n" +
                String.format("Sample rate: %d Hz (requested %d Hz)\n", record.getSampleRate(), sampleRate) +
                String.format("Min buffer size: %d samples, %d bytes\n", minBufferSize / bytesPerSample, minBufferSize) +
                String.format("Buffer size: %d samples, %d bytes\n", bufferSampleSize, bytesPerSample * bufferSampleSize) +
                String.format("Read chunk size: %d samples, %d bytes\n", readChunkSize, bytesPerSample * readChunkSize) +
                String.format("FFT length: %d\n", fFTLength) +
                String.format("N FFT average: %d\n", nFFTAverage));
        params.setSampleRate(record.getSampleRate());

        if (record.getState() == AudioRecord.STATE_UNINITIALIZED) {
            KLog.Companion.e(METHOD_NAME + ": Fail initializing the AudioRecord");
            analyzerViews.notifyToast("Fail initializing the recorder.");
            return;
        }

        short[] audioSamples = new short[readChunkSize];
        int numOfReadShort;

        sTFT = new STFT(params);
        if (spectrumDBCopy == null || spectrumDBCopy.length != fFTLength / 2 + 1) {
            spectrumDBCopy = new double[fFTLength / 2 + 1];
        }

        RecorderMonitor recorderMonitor = new RecorderMonitor(sampleRate, bufferSampleSize, TAG + "::run()");
        recorderMonitor.start();

//      FPSCounter fpsCounter = new FPSCounter("SamplingLoop::run()"); // ERASE ???

        WavWriter wavWriter = new WavWriter(sampleRate);
        boolean saveWav = analyzerFragment.saveWav; // Change of saveWav during loop will only affect next enter
        if (saveWav) {
            wavWriter.start();
            wavSecondsRemain = wavWriter.secondsLeft();
            wavSeconds = 0;
            KLog.Companion.i(METHOD_NAME + "PCM write to file '" + wavWriter.getPath() + "'");
        }

        try {
            record.startRecording();
        } catch (IllegalStateException e) {
            String error = "Fail start recording";
            KLog.Companion.e(METHOD_NAME + ": " + error);
            analyzerViews.notifyToast(error);
            return;
        }

        // COMMENT: Main loop
        // When running in this loop (including when paused), you can not change properties
        // related to recorder: e.g. audioSourceId, sampleRate, bufferSampleSize
        // TODO: allow change of FFT length on the fly
        while (running) {
            if (audioSourceId >= AnalyzerParams.Companion.getID_TEST_SIGNAL_1()) {
                numOfReadShort = readTestData(audioSamples, 0, readChunkSize, audioSourceId);
            } else {
                numOfReadShort = record.read(audioSamples, 0, readChunkSize);   // COMMENT: read
            }
            if (recorderMonitor.updateState(numOfReadShort)) {  // performed a check
                if (recorderMonitor.getLastCheckOverrun())
                    analyzerViews.notifyOverrun();
                if (saveWav)
                    wavSecondsRemain = wavWriter.secondsLeft();
            }
            if (saveWav) {
                wavWriter.pushAudioShort(audioSamples, numOfReadShort);  // Maybe move this to another thread?
                wavSeconds = wavWriter.secondsWritten();
                analyzerViews.updateRec(wavSeconds);
            }
            if (paused) {
//          fpsCounter.increment();
                // keep reading data, for overrun checker and for write wav data
                continue;
            }

            sTFT.feedData(audioSamples, numOfReadShort); // COMMENT: stream

            // If there is new spectrum data, do plot
            if (sTFT.nElemSpectrumAmp() >= nFFTAverage) {
                // Update spectrum or spectrogram
                final double[] spectrumDB = sTFT.getSpectrumAmpDB();
                System.arraycopy(spectrumDB, 0, spectrumDBCopy, 0, spectrumDB.length);
                analyzerViews.update(spectrumDBCopy); // COMMENT: update
//          fpsCounter.increment();

                sTFT.calculatePeak();
                analyzerFragment.setMaxAmpFreq(sTFT.getMaxAmplitudeFreq());
                analyzerFragment.setMaxAmpDB(sTFT.getMaxAmplitudeDB());

                // get RMS
                analyzerFragment.dtRMS = sTFT.getRMS();
                analyzerFragment.setDtRMSFromFT(sTFT.getRMSFromFT());
            }
        }
        KLog.Companion.i(METHOD_NAME + ": Actual sample rate: " + recorderMonitor.getSampleRate());
        KLog.Companion.i(METHOD_NAME + ": Stopping and releasing recorder.");
        record.stop();
        record.release();
        if (saveWav) {
            KLog.Companion.i(METHOD_NAME + ": Ending saved wav.");
            wavWriter.stop();
            analyzerViews.notifyWAVSaved(wavWriter.relativeDir);
        }
    }

    private int readTestData(short[] a, int offsetInShorts, int sizeInShorts, int id) {
        if (data == null || data.length != sizeInShorts) {
            data = new double[sizeInShorts];
        }
        Arrays.fill(data, 0.0);
        switch (id - AnalyzerParams.Companion.getID_TEST_SIGNAL_1()) {
            case 1:
                sineGenerator1.getSamples(data);
                // No break, so values of data added
            case 0:
                sineGenerator0.addSamples(data);
                for (int i = 0; i < sizeInShorts; i++) {
                    a[offsetInShorts + i] = (short) Math.round(data[i]);
                }
                break;
            case 2:
                for (int i = 0; i < sizeInShorts; i++) {
                    a[i] = (short) (sampleValueMax * (2.0 * Math.random() - 1));
                }
                break;
            default:
                KLog.Companion.w(Thread.currentThread().getStackTrace()[2].getMethodName() + ": this id has no source: " + audioSourceId);
        }
        limitFrameRate(1000.0 * sizeInShorts / sampleRate); // Block this thread, so that behave as if read from real device
        return sizeInShorts;
    }

    /**
     * Limit the frame rate waiting some milliseconds.
     */
    private void limitFrameRate(double updateMs) {
        baseTimeMs += updateMs;
        long delay = (int) (baseTimeMs - SystemClock.uptimeMillis());
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            baseTimeMs -= delay;  // Get the current time
        }
    }

    public void setAWeighting(boolean isAWeighting) {
        if (sTFT != null) {
            sTFT.setDBAWeighting(isAWeighting);
        }
    }

    public void setPause(boolean pause) {
        this.paused = pause;
    }

    public boolean getPause() {
        return this.paused;
    }

    public void finish() {
        running = false;
        interrupt();
    }

    public double getWavSecondsRemain() {
        return wavSecondsRemain;
    }

    public void setWavSecondsRemain(double wavSecondsRemain) {
        this.wavSecondsRemain = wavSecondsRemain;
    }

    public double getWavSeconds() {
        return wavSeconds;
    }

    public void setWavSeconds(double wavSeconds) {
        this.wavSeconds = wavSeconds;
    }
}