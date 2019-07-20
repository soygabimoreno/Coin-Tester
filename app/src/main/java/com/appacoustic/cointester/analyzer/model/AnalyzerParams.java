package com.appacoustic.cointester.analyzer.model;

import android.media.MediaRecorder;

import com.gabrielmorenoibarra.g.GLog;

/**
 * Basic properties of Analyzer.
 */
public class AnalyzerParams {

    public static final String TAG = AnalyzerParams.class.getSimpleName();

    //    public static final int RECORDER_AGC_OFF = MediaRecorder.AudioSource.VOICE_RECOGNITION; // ERASE: OLD
    public static final int RECORDER_AGC_OFF = MediaRecorder.AudioSource.DEFAULT;
    public static final int BYTES_PER_SAMPLE = 2; // COMMENT: formerly named BYTE_OF_SAMPLE
    public static final double SAMPLE_VALUE_MAX = 32767.0;

    public static final int N_MIC_SOURCES = 7;
    public static int ID_TEST_SIGNAL_1;
    public static int ID_TEST_SIGNAL_2;
    public static int ID_TEST_SIGNAL_WHITE_NOISE;

    private int sampleRate = 16000;
    private int fFTLength = 2048;
    private int hopLength = 1024;
    private double overlapPercent = (1 - hopLength / fFTLength) * 100;
    private String windowFunctionName;
    private String[] windowFunctionNames;
    private int nFFTAverage = 2;
    private boolean dBAWeighting;
    private double spectrogramDuration = 4.0;
    private double[] micGainDB; // Should have fFTLength/2 elements
    private String[] audioSourceNames;
    private int[] audioSourceIds;
    private int audioSourceId = RECORDER_AGC_OFF;

    public AnalyzerParams(String[] audioSourceNames, int[] audioSourceIds, String[] windowFunctionNames) {
        this.audioSourceNames = audioSourceNames;
        this.audioSourceIds = audioSourceIds;
        this.windowFunctionNames = windowFunctionNames;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getFFTLength() {
        return fFTLength;
    }

    public void setFFTLength(int fFTLength) {
        this.fFTLength = fFTLength;
    }

    public int getHopLength() {
        return hopLength;
    }

    public void setHopLength(int hopLength) {
        this.hopLength = hopLength;
    }

    public double getOverlapPercent() {
        return overlapPercent;
    }

    public void setOverlapPercent(double overlapPercent) {
        this.overlapPercent = overlapPercent;
    }

    public String getWindowFunctionName() {
        return windowFunctionName;
    }

    public void setWindowFunctionName(String windowFunctionName) {
        this.windowFunctionName = windowFunctionName;
    }

    public String[] getWindowFunctionNames() {
        return windowFunctionNames;
    }

    public int getNFFTAverage() {
        return nFFTAverage;
    }

    public void setNFFTAverage(int nFFTAverage) {
        this.nFFTAverage = nFFTAverage;
    }

    public boolean isDBAWeighting() {
        return dBAWeighting;
    }

    public void setDBAWeighting(boolean dBAWeighting) {
        this.dBAWeighting = dBAWeighting;
    }

    public double getSpectrogramDuration() {
        return spectrogramDuration;
    }

    public void setSpectrogramDuration(double spectrogramDuration) {
        this.spectrogramDuration = spectrogramDuration;
    }

    public double[] getMicGainDB() {
        return micGainDB;
    }

    public void setMicGainDB(double[] micGainDB) {
        this.micGainDB = micGainDB;
    }

    public String[] getAudioSourceNames() {
        return audioSourceNames;
    }

    public int[] getAudioSourceIds() {
        return audioSourceIds;
    }

    public String getAudioSourceName() {
        return getAudioSourceNameFromId(audioSourceId);
    }

    private String getAudioSourceNameFromId(int id) {
        for (int i = 0; i < audioSourceNames.length; i++) {
            if (audioSourceIds[i] == id) {
                return audioSourceNames[i];
            }
        }
        GLog.e(TAG, "getAudioSourceNameFromId(): non-standard entry");
        return String.valueOf(id);
    }

    public int getAudioSourceId() {
        return audioSourceId;
    }

    public void setAudioSourceId(int audioSourceId) {
        this.audioSourceId = audioSourceId;
    }
}