package com.appacoustic.cointester.analyzer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import com.appacoustic.cointester.AppResources;
import com.appacoustic.cointester.R;
import com.appacoustic.cointester.analyzer.FPSCounter;
import com.appacoustic.cointester.analyzer.GridLabel;
import com.appacoustic.cointester.analyzer.ScreenPhysicalMapping;
import com.appacoustic.cointester.analyzer.SpectrogramBMP;
import com.appacoustic.cointester.analyzer.model.AnalyzerParams;
import com.gabrielmorenoibarra.k.util.KLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Custom view to draw the FFT graph.
 */
public class AnalyzerGraphicView extends View {

    private static final String TAG = AnalyzerGraphicView.class.getSimpleName();

    public static final double MIN_DB = -144;
    public static final double MAX_DB = 12;
    private static final double MAX_DB_RANGE = -150;
    private static final String FILE_NAME_SPECTROGRAM_SHORT = "spectrogram_short.raw";

    private Context context;
    private double xZoom, yZoom; // Horizontal and vertical scaling
    private double xShift, yShift; // Horizontal and vertical translation, in unit 1 unit

    private int canvasWidth, canvasHeight; // Size of my canvas
    private int[] myLocation = {0, 0}; // Window location on screen
    private double freqLowerBoundForLog;
    private double[] tmpDBSpectrum = new double[0];
    public static final int VIEW_RANGE_DATA_LENGTH = 6;

    private SpectrumPlot spectrumPlot;
    private SpectrogramPlot spectrogramPlot;

    private PlotMode showMode = PlotMode.SPECTRUM;

    private final FPSCounter fpsCounter = new FPSCounter(TAG);

    public enum PlotMode {
        SPECTRUM(0), SPECTROGRAM(1);

        private final int value;

        PlotMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Callback to caller when rendering is complete.
     */
    private OnReadyListener onReadyListener;

    public interface OnReadyListener {
        void ready();
    }

    public AnalyzerGraphicView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public AnalyzerGraphicView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AnalyzerGraphicView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        xZoom = yZoom = 1f;
        xShift = yShift = 0f;
        canvasWidth = canvasHeight = 0;

        spectrumPlot = new SpectrumPlot(context);
        spectrogramPlot = new SpectrogramPlot(context);

        spectrumPlot.setCanvas(canvasWidth, canvasHeight, null);
        spectrogramPlot.setCanvas(canvasWidth, canvasHeight, null);

        spectrumPlot.setZooms(xZoom, xShift, yZoom, yShift);
        spectrogramPlot.setZooms(xZoom, xShift, yZoom, yShift);

        spectrumPlot.getAxisY().setLowerBound(MAX_DB_RANGE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        fpsCounter.increment();
        if (showMode == PlotMode.SPECTRUM) {
            spectrumPlot.drawSpectrumPlot(canvas, tmpDBSpectrum);
        } else {
            spectrogramPlot.drawSpectrogramPlot(canvas);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        KLog.Companion.i(Thread.currentThread().getStackTrace()[2].getMethodName() +
                ": canvas (" + oldWidth + "," + oldHeight + ") -> (" + width + "," + height + ")");
        this.canvasWidth = width;
        this.canvasHeight = height;
        spectrumPlot.setCanvas(width, height, null);
        spectrogramPlot.setCanvas(width, height, null);
        if (height > 0 && onReadyListener != null) {
            onReadyListener.ready();
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        KLog.Companion.i(Thread.currentThread().getStackTrace()[2].getMethodName() +
                ": xShift = " + xShift + "  xZoom = " + xZoom + "  yShift = " + yShift + "  yZoom = " + yZoom);
        Parcelable parentState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(parentState);
        savedState.freqAxisAlongX = spectrogramPlot.isShowFreqAlongX() ? 1 : 0;
        savedState.markerFreqSpectrum = spectrumPlot.getMarkerFreq();
        savedState.markerFreqSpectrogram = spectrogramPlot.getMarkerFreq();
        savedState.markerDB = spectrumPlot.getMarkerDB();
        savedState.xZoom = xZoom;
        savedState.xShift = xShift;
        savedState.yZoom = yZoom;
        savedState.yShift = yShift;
        savedState.spectrumXZoom = spectrumPlot.getAxisX().getZoom();
        savedState.spectrumXShift = spectrumPlot.getAxisX().getShift();
        savedState.spectrumYZoom = spectrumPlot.getAxisY().getZoom();
        savedState.spectrumYShift = spectrumPlot.getAxisY().getShift();
        savedState.spectrogramFreqZoom = spectrogramPlot.getAxisFreq().getZoom();
        savedState.spectrogramFreqShift = spectrogramPlot.getAxisFreq().getShift();
        savedState.spectrogramTimeZoom = spectrogramPlot.getAxisTime().getZoom();
        savedState.spectrogramTimeShift = spectrogramPlot.getAxisTime().getShift();
        savedState.tmpDBSpectrum = tmpDBSpectrum;
        savedState.nFreqPoints = spectrogramPlot.getNFreqPoints();
        savedState.nTimePoints = spectrogramPlot.getNTimePoints();
        savedState.iTimePointer = spectrogramPlot.getSpectrogramBMP().getSpectrumStore().getITimePointer();

        final short[] tmpDBSpectrogram = spectrogramPlot.getSpectrogramBMP().getSpectrumStore().getDBShortArray();
        int length = tmpDBSpectrogram.length;
        byte[] input = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            input[2 * i] = (byte) (tmpDBSpectrogram[i] & 0xff);
            input[2 * i + 1] = (byte) (tmpDBSpectrogram[i] >> 8);
        }

        // Save spectrogram to a file:
        File tmpDBSpectrogramPath = new File(context.getCacheDir(), FILE_NAME_SPECTROGRAM_SHORT);
        try {
            OutputStream fos = new FileOutputStream(tmpDBSpectrogramPath);
            fos.write(input);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        final String METHOD_NAME = Thread.currentThread().getStackTrace()[2].getMethodName();
        if (state instanceof SavedState) {
            SavedState restoredState = (SavedState) state;
            super.onRestoreInstanceState(restoredState.getSuperState());

            spectrogramPlot.setShowFreqAlongX(restoredState.freqAxisAlongX == 1);
            spectrumPlot.setMarkerFreq(restoredState.markerFreqSpectrum);
            spectrogramPlot.setMarkerFreq(restoredState.markerFreqSpectrogram);
            spectrumPlot.setMarkerDB(restoredState.markerDB);
            xZoom = restoredState.xZoom;
            xShift = restoredState.xShift;
            yZoom = restoredState.yZoom;
            yShift = restoredState.yShift;
            spectrumPlot.getAxisX().setZoomShift(restoredState.spectrumXZoom, restoredState.spectrumXShift);
            spectrumPlot.getAxisY().setZoomShift(restoredState.spectrumYZoom, restoredState.spectrumYShift);
            spectrogramPlot.getAxisFreq().setZoomShift(restoredState.spectrogramFreqZoom, restoredState.spectrogramFreqShift);
            spectrogramPlot.getAxisTime().setZoomShift(restoredState.spectrogramTimeZoom, restoredState.spectrogramTimeShift);
            tmpDBSpectrum = restoredState.tmpDBSpectrum;
            spectrogramPlot.setNFreqPoints(restoredState.nFreqPoints);
            spectrogramPlot.setNTimePoints(restoredState.nTimePoints);

            final SpectrogramBMP spectrogramBMP = spectrogramPlot.getSpectrogramBMP();
            final SpectrogramBMP.SpectrumCompressedStored spectrumCompressedStored = spectrogramBMP.getSpectrumStore();
            spectrumCompressedStored.setNFreq(restoredState.nFreqPoints); // Prevent reinitialize of LogFreqSpectrogramBMP
            spectrumCompressedStored.setNTime(restoredState.nTimePoints);
            spectrumCompressedStored.setITimePointer(restoredState.iTimePointer);

            // Restore temporary saved spectrogram:
            byte[] input = new byte[(restoredState.nFreqPoints + 1) * restoredState.nTimePoints * 2]; // Length of spectrumStore.dbShortArray
            int bytesRead = -1;
            File tmpDBSpectrogramPath = new File(context.getCacheDir(), FILE_NAME_SPECTROGRAM_SHORT);
            try {
                InputStream fis = new FileInputStream(tmpDBSpectrogramPath);
                bytesRead = fis.read(input);
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (bytesRead != input.length) { // Fail to get saved spectrogram, have a new start
                spectrumCompressedStored.setNFreq(0);
                spectrumCompressedStored.setNTime(0);
                spectrumCompressedStored.setITimePointer(0);
            } else { // We have data!
                short[] tmpDBSpectrogram = new short[input.length / 2];
                for (int i = 0; i < tmpDBSpectrogram.length; i++) {
                    tmpDBSpectrogram[i] = (short) (input[2 * i] + (input[2 * i + 1] << 8));
                }
                spectrumCompressedStored.setDbShortArray(tmpDBSpectrogram);
                spectrogramBMP.rebuildLinearBMP();
            }

            KLog.Companion.i(METHOD_NAME + ": xShift = " + xShift + "  xZoom = " + xZoom + "  yShift = " + yShift + "  yZoom = " + yZoom);
        } else {
            KLog.Companion.i(METHOD_NAME + ": not SavedState?!");
            super.onRestoreInstanceState(state);
        }
    }

    /**
     * Call this when settings changed.
     */
    public void setupPlot(AnalyzerParams params) {
        setupAxes(params);
        spectrogramPlot.setupSpectrogram(params);
    }

    public void setupAxes(AnalyzerParams params) {
        int sampleRate = params.getSampleRate();
        freqLowerBoundForLog = (double) sampleRate / params.getFftLength();
        double freqLowerBoundLocal = 0;
        if (spectrumPlot.getAxisX().getMapType() == ScreenPhysicalMapping.Type.LOG) {
            freqLowerBoundLocal = freqLowerBoundForLog;
        }

        // Spectrum:
        double axisBounds[] = new double[]{freqLowerBoundLocal, 0.0, sampleRate / 2.0, spectrumPlot.getAxisY().getUpperBound()};
        spectrumPlot.setCanvas(canvasWidth, canvasHeight, axisBounds);

        // Spectrogram:
        freqLowerBoundLocal = 0;
        if (spectrogramPlot.getAxisFreq().getMapType() == ScreenPhysicalMapping.Type.LOG) {
            freqLowerBoundLocal = freqLowerBoundForLog;
        }
        int nFFTAverage = params.getNFftAverage();
        double spectrogramDuration = params.getSpectrogramDuration();
        if (spectrogramPlot.isShowFreqAlongX()) {
            axisBounds = new double[]{freqLowerBoundLocal, 0.0, sampleRate / 2.0, spectrogramDuration * nFFTAverage};
        } else {
            axisBounds = new double[]{0.0, sampleRate / 2.0, spectrogramDuration * nFFTAverage, freqLowerBoundLocal};
        }
        spectrogramPlot.setCanvas(canvasWidth, canvasHeight, axisBounds);
    }

    public void setAxisModeLinear(String mode) {
        AppResources appResources = AppResources.getInstance();
        boolean linear = mode.equals(appResources.getString(R.string.linear));
        ScreenPhysicalMapping.Type mapType;
        GridLabel.Type gridType;
        if (linear) {
            mapType = ScreenPhysicalMapping.Type.LINEAR;
            gridType = GridLabel.Type.FREQ;
        } else {
            mapType = ScreenPhysicalMapping.Type.LOG;
            gridType = mode.equals(appResources.getString(R.string.note)) ? GridLabel.Type.FREQ_NOTE : GridLabel.Type.FREQ_LOG;
        }
        spectrumPlot.setFreqAxisMode(mapType, freqLowerBoundForLog, gridType);
        spectrogramPlot.setFreqAxisMode(mapType, freqLowerBoundForLog, gridType);
        if (showMode == PlotMode.SPECTRUM) {
            xZoom = spectrumPlot.getAxisX().getZoom();
            xShift = spectrumPlot.getAxisX().getShift();
        } else if (showMode == PlotMode.SPECTROGRAM) {
            if (spectrogramPlot.isShowFreqAlongX()) {
                xZoom = spectrogramPlot.getAxisFreq().getZoom();
                xShift = spectrogramPlot.getAxisFreq().getShift();
            } else {
                yZoom = spectrogramPlot.getAxisFreq().getZoom();
                yShift = spectrogramPlot.getAxisFreq().getShift();
            }
        }
    }

    public void setShowFreqAlongX(boolean b) {
        spectrogramPlot.setShowFreqAlongX(b);

        if (showMode == PlotMode.SPECTRUM) return;

        if (spectrogramPlot.isShowFreqAlongX()) {
            xZoom = spectrogramPlot.getAxisFreq().getZoom();
            xShift = spectrogramPlot.getAxisFreq().getShift();
            yZoom = spectrogramPlot.getAxisTime().getZoom();
            yShift = spectrogramPlot.getAxisTime().getShift();
        } else {
            xZoom = spectrogramPlot.getAxisTime().getZoom();
            xShift = spectrogramPlot.getAxisTime().getShift();
            yZoom = spectrogramPlot.getAxisFreq().getZoom();
            yShift = spectrogramPlot.getAxisFreq().getShift();
        }
    }

    public void switch2Spectrum() {
        if (showMode == PlotMode.SPECTRUM) return;
        showMode = PlotMode.SPECTRUM;
        xZoom = spectrogramPlot.getAxisFreq().getZoom();
        xShift = spectrogramPlot.getAxisFreq().getShift();
        if (!spectrogramPlot.isShowFreqAlongX()) {
            xShift = 1 - 1 / xZoom - xShift;
        }
        spectrumPlot.getAxisX().setZoomShift(xZoom, xShift);

        yZoom = spectrumPlot.getAxisY().getZoom();
        yShift = spectrumPlot.getAxisY().getShift();
    }

    public void switch2Spectrogram() {
        KLog.Companion.d(Thread.currentThread().getStackTrace()[2].getMethodName() + " " + hashCode());
        if (showMode == PlotMode.SPECTRUM && canvasHeight > 0) { // canvasHeight==0 means the program is just start
            if (spectrogramPlot.isShowFreqAlongX()) {
                // No need to change x scaling
                yZoom = spectrogramPlot.getAxisTime().getZoom();
                yShift = spectrogramPlot.getAxisTime().getShift();
                spectrogramPlot.getAxisFreq().setZoomShift(xZoom, xShift);
            } else {
                // noinspection SuspiciousNameCombination
                yZoom = xZoom;
                yShift = 1 - 1 / xZoom - xShift; // axisFreq is reverted
                xZoom = spectrogramPlot.getAxisTime().getZoom();
                xShift = spectrogramPlot.getAxisTime().getShift();
                spectrogramPlot.getAxisFreq().setZoomShift(yZoom, yShift);
            }
        }
        spectrogramPlot.getSpectrogramBMP().updateAxis(spectrogramPlot.getAxisFreq());
        spectrogramPlot.prepare();
        showMode = PlotMode.SPECTROGRAM;
    }

    public double[] setViewRange(double[] ranges, double[] rangesDefault) {
        final String METHOD_NAME = Thread.currentThread().getStackTrace()[2].getMethodName();
        if (ranges.length < VIEW_RANGE_DATA_LENGTH) {
            KLog.Companion.w(METHOD_NAME + ": invalid input: ranges.length < VIEW_RANGE_DATA_LENGTH");
            return null;
        }

        double[] rangesResult = new double[VIEW_RANGE_DATA_LENGTH];
        System.arraycopy(ranges, 0, rangesResult, 0, VIEW_RANGE_DATA_LENGTH);

        if (rangesDefault != null) {
            if (rangesDefault.length != 2 * VIEW_RANGE_DATA_LENGTH) {
                KLog.Companion.w(METHOD_NAME + ": invalid input: rangesDefault.length != 2 * VIEW_RANGE_DATA_LENGTH");
                return null;
            }
            for (int i = 0; i < 6; i += 2) {
                if (rangesResult[i] > rangesResult[i + 1]) { // Order reversed
                    double t = rangesResult[i];
                    rangesResult[i] = rangesResult[i + 1];
                    rangesResult[i + 1] = t;
                }
                if (rangesResult[i] < rangesDefault[i + 6]) rangesResult[i] = rangesDefault[i + 6]; // Lower than lower bound
                if (rangesResult[i + 1] < rangesDefault[i + 6]) rangesResult[i + 1] = rangesDefault[i + 7]; // All lower than lower bound ???
                if (rangesResult[i] > rangesDefault[i + 7]) rangesResult[i] = rangesDefault[i + 6]; // All higher than upper bound ???
                if (rangesResult[i + 1] > rangesDefault[i + 7]) rangesResult[i + 1] = rangesDefault[i + 7]; // Higher than upper bound
                if (rangesResult[i] == rangesResult[i + 1] || Double.isNaN(rangesResult[i]) || Double.isNaN(rangesResult[i + 1])) { // Invalid input value
                    rangesResult[i] = rangesDefault[i];
                    rangesResult[i + 1] = rangesDefault[i + 1];
                }
            }
        }

        // Set range:
        if (showMode == PlotMode.SPECTRUM) {
            spectrumPlot.getAxisX().setViewBounds(rangesResult[0], rangesResult[1]);
            spectrumPlot.getAxisY().setViewBounds(rangesResult[3], rangesResult[2]); // Reversed
        } else if (showMode == PlotMode.SPECTROGRAM) {
            if (spectrogramPlot.getSpectrogramMode() == SpectrogramPlot.TimeAxisMode.SHIFT) {
                spectrogramPlot.getAxisTime().setViewBounds(rangesResult[5], rangesResult[4]);
            } else {
                spectrogramPlot.getAxisTime().setViewBounds(rangesResult[4], rangesResult[5]);
            }
            if (spectrogramPlot.isShowFreqAlongX()) {
                spectrogramPlot.getAxisFreq().setViewBounds(rangesResult[0], rangesResult[1]);
            } else {
                spectrogramPlot.getAxisFreq().setViewBounds(rangesResult[1], rangesResult[0]);
            }
            spectrogramPlot.getSpectrogramBMP().updateAxis(spectrogramPlot.getAxisFreq());
        }

        // Set zoom shift for view:
        if (showMode == PlotMode.SPECTRUM) {
            xZoom = spectrumPlot.getAxisX().getZoom();
            xShift = spectrumPlot.getAxisX().getShift();
            yZoom = spectrumPlot.getAxisY().getZoom();
            yShift = spectrumPlot.getAxisY().getShift();
        } else if (showMode == PlotMode.SPECTROGRAM) {
            if (spectrogramPlot.isShowFreqAlongX()) {
                xZoom = spectrogramPlot.getAxisFreq().getZoom();
                xShift = spectrogramPlot.getAxisFreq().getShift();
                yZoom = spectrogramPlot.getAxisTime().getZoom();
                yShift = spectrogramPlot.getAxisTime().getShift();
            } else {
                yZoom = spectrogramPlot.getAxisFreq().getZoom();
                yShift = spectrogramPlot.getAxisFreq().getShift();
                xZoom = spectrogramPlot.getAxisTime().getZoom();
                xShift = spectrogramPlot.getAxisTime().getShift();
            }
        }
        return rangesResult;
    }

    private void updateAxisZoomShift() {
        if (showMode == PlotMode.SPECTRUM) {
            spectrumPlot.setZooms(xZoom, xShift, yZoom, yShift);
        } else {
            spectrogramPlot.setZooms(xZoom, xShift, yZoom, yShift);
        }
    }

    public void saveSpectrum(double[] spectrumDB) {
        synchronized (tmpDBSpectrum) { // TODO: need lock on tmpDBSpectrum, but how?
            if (tmpDBSpectrum == null || tmpDBSpectrum.length != spectrumDB.length) {
                tmpDBSpectrum = new double[spectrumDB.length];
            }
            System.arraycopy(spectrumDB, 0, tmpDBSpectrum, 0, spectrumDB.length);
        }
        // TODO: Run on another thread? Lock on data ? Or use CompletionService?
        if (showMode == PlotMode.SPECTROGRAM) {
            spectrogramPlot.saveRowSpectrumAsColor(tmpDBSpectrum);
        }
    }

    public void setSpectrumDBLowerBound(double b) {
        spectrumPlot.getAxisY().setUpperBound(b);
    }

    public void setSpectrogramDBLowerBound(double b) {
        spectrogramPlot.setSpectrogramDBLowerBound(b);
    }

    public void setShowLines(boolean b) {
        spectrumPlot.setShowLines(b);
    }

    private boolean intersects(float x, float y) {
        getLocationOnScreen(myLocation);
        return x >= myLocation[0] &&
                y >= myLocation[1] &&
                x < myLocation[0] + getWidth() &&
                y < myLocation[1] + getHeight();
    }

    /**
     * @return true if the coordinate (x,y) is inside graphView.
     */
    public boolean setMarker(float x, float y) {
        if (intersects(x, y)) {
            x = x - myLocation[0];
            y = y - myLocation[1];
            // Convert to coordinate in axis:
            if (showMode == PlotMode.SPECTRUM) {
                spectrumPlot.setMarker(x, y);
            } else {
                spectrogramPlot.setMarker(x, y);
            }
            return true;
        } else {
            return false;
        }
    }

    public double getMarkerFreq() {
        if (showMode == PlotMode.SPECTRUM) {
            return spectrumPlot.getMarkerFreq();
        } else {
            return spectrogramPlot.getMarkerFreq();
        }
    }

    public double getMarkerDB() {
        if (showMode == PlotMode.SPECTRUM) {
            return spectrumPlot.getMarkerDB();
        } else {
            return 0;
        }
    }

    public void hideMarker() {
        spectrumPlot.hideMarker();
        spectrogramPlot.hideMarker();
    }

    public double[] getViewPhysicalRange() {
        double[] range = new double[12];
        if (getShowMode() == AnalyzerGraphicView.PlotMode.SPECTRUM) {
            // fL, fU, dBL dBU, time L, time U
            range[0] = spectrumPlot.getAxisX().getLowerViewBound();
            range[1] = spectrumPlot.getAxisX().getUpperViewBound();
            range[2] = spectrumPlot.getAxisY().getUpperViewBound(); // Reversed
            range[3] = spectrumPlot.getAxisY().getLowerViewBound();
            range[4] = 0;
            range[5] = 0;

            // Limits of fL, fU, dBL dBU, time L, time U
            range[6] = spectrumPlot.getAxisX().getLowerBound();
            range[7] = spectrumPlot.getAxisX().getUpperBound();
            range[8] = AnalyzerGraphicView.MIN_DB;
            range[9] = AnalyzerGraphicView.MAX_DB;
            range[10] = 0;
            range[11] = 0;
        } else {
            range[0] = spectrogramPlot.getAxisFreq().getLowerViewBound();
            range[1] = spectrogramPlot.getAxisFreq().getUpperViewBound();
            if (range[0] > range[1]) {
                double t = range[0];
                range[0] = range[1];
                range[1] = t;
            }
            range[2] = spectrogramPlot.getSpectrogramBMP().getdBLowerBound();
            range[3] = spectrogramPlot.getSpectrogramBMP().getdBUpperBound();
            range[4] = spectrogramPlot.getAxisTime().getLowerViewBound();
            range[5] = spectrogramPlot.getAxisTime().getUpperViewBound();

            range[6] = spectrogramPlot.getAxisFreq().getLowerBound();
            range[7] = spectrogramPlot.getAxisFreq().getUpperBound();
            if (range[6] > range[7]) {
                double t = range[6];
                range[6] = range[7];
                range[7] = t;
            }
            range[8] = AnalyzerGraphicView.MIN_DB;
            range[9] = AnalyzerGraphicView.MAX_DB;
            range[10] = spectrogramPlot.getAxisTime().getLowerBound();
            range[11] = spectrogramPlot.getAxisTime().getUpperBound();
        }
        for (int i = 6; i < range.length; i += 2) {
            if (range[i] > range[i + 1]) {
                double t = range[i];
                range[i] = range[i + 1];
                range[i + 1] = t;
            }
        }
        return range;
    }

    public double getXZoom() {
        return xZoom;
    }

    public double getYZoom() {
        return yZoom;
    }

    public double getXShift() {
        return xShift;
    }

    public double getYShift() {
        return yShift;
    }

    public double getCanvasWidth() {
        if (showMode == PlotMode.SPECTRUM) {
            return canvasWidth;
        } else {
            return canvasWidth - spectrogramPlot.getLabelBeginX();
        }
    }

    public double getCanvasHeight() {
        if (showMode == PlotMode.SPECTRUM) {
            return canvasHeight;
        } else {
            return spectrogramPlot.getLabelBeginY();
        }
    }

    public void setXShift(double offset) {
        xShift = clampXShift(offset);
        updateAxisZoomShift();
    }

    public void setYShift(double offset) {
        yShift = clampYShift(offset);
        updateAxisZoomShift();
    }

    private double clampXShift(double offset) {
        return clamp(offset, 0f, 1 - 1 / xZoom);
    }

    private double clampYShift(double offset) {
        if (showMode == PlotMode.SPECTRUM) {
            // Limit view to MIN_DB ~ MAX_DB, assume linear in dB scale
            return clamp(offset, (MAX_DB - spectrumPlot.getAxisY().getLowerBound()) / spectrumPlot.getAxisY().getBoundsRange(),
                    (MIN_DB - spectrumPlot.getAxisY().getLowerBound()) / spectrumPlot.getAxisY().getBoundsRange() - 1 / yZoom);
        } else {
            // Strict restrict, 'y' can be frequency or time.
            return clamp(offset, 0f, 1 - 1 / yZoom);
        }
    }

    private double clamp(double x, double min, double max) {
        if (x > max) {
            return max;
        } else if (x < min) {
            return min;
        } else {
            return x;
        }
    }

    public void resetViewScale() {
        xShift = 0;
        xZoom = 1;
        yShift = 0;
        yZoom = 1;
        updateAxisZoomShift();
    }

    private double xMidOld = 100;
    private double xDiffOld = 100;
    private double xZoomOld = 1;
    private double xShiftOld = 0;
    private double yMidOld = 100;
    private double yDiffOld = 100;
    private double yZoomOld = 1;
    private double yShiftOld = 0;

    /**
     * Record the coordinate frame state when starting scaling.
     */
    public void setShiftScaleBegin(double x1, double y1, double x2, double y2) {
        xMidOld = (x1 + x2) / 2f;
        xDiffOld = Math.abs(x1 - x2);
        xZoomOld = xZoom;
        xShiftOld = xShift;
        yMidOld = (y1 + y2) / 2f;
        yDiffOld = Math.abs(y1 - y2);
        yZoomOld = yZoom;
        yShiftOld = yShift;
    }

    /**
     * Do the scaling according to the motion event getX() and getY() (getPointerCount()==2).
     */
    public void setShiftScale(double x1, double y1, double x2, double y2) {
        double limitXZoom;
        double limitYZoom;
        if (showMode == PlotMode.SPECTRUM) {
            limitXZoom = spectrumPlot.getAxisX().getBoundsRange() / 200f; // Limit to 200 Hz a screen
            limitYZoom = -spectrumPlot.getAxisY().getBoundsRange() / 6f; // Limit to 6 dB a screen
        } else {
            int nTimePoints = spectrogramPlot.getNTimePoints();
            if (spectrogramPlot.isShowFreqAlongX()) {
                limitXZoom = spectrogramPlot.getAxisFreq().getBoundsRange() / 200f;
                limitYZoom = nTimePoints > 10 ? nTimePoints / 10 : 1;
            } else {
                limitXZoom = nTimePoints > 10 ? nTimePoints / 10 : 1;
                limitYZoom = spectrogramPlot.getAxisFreq().getBoundsRange() / 200f;
            }
        }
        limitXZoom = Math.abs(limitXZoom);
        limitYZoom = Math.abs(limitYZoom);
        if (canvasWidth * 0.13f < xDiffOld) { // If fingers are not very close in x direction, do scale in x direction
            xZoom = clamp(xZoomOld * Math.abs(x1 - x2) / xDiffOld, 1f, limitXZoom);
        }
        xShift = clampXShift(xShiftOld + (xMidOld / xZoomOld - (x1 + x2) / 2f / xZoom) / canvasWidth);
        if (canvasHeight * 0.13f < yDiffOld) { // If fingers are not very close in y direction, do scale in y direction
            yZoom = clamp(yZoomOld * Math.abs(y1 - y2) / yDiffOld, 1f, limitYZoom);
        }
        yShift = clampYShift(yShiftOld + (yMidOld / yZoomOld - (y1 + y2) / 2f / yZoom) / canvasHeight);
        updateAxisZoomShift();
    }

    public void setSmoothRender(boolean smoothRender) {
        spectrogramPlot.setSmoothRender(smoothRender);
    }

    public void setTimeMultiplier(int nFFTAverage) {
        spectrogramPlot.setTimeMultiplier(nFFTAverage);
    }

    public void setShowTimeAxis(boolean showTimeAxis) {
        spectrogramPlot.setShowTimeAxis(showTimeAxis);
    }

    public void setSpectrogramModeShifting(boolean spectrogramModeShifting) {
        spectrogramPlot.setSpectrogramModeShifting(spectrogramModeShifting);
    }

    public void setLogAxisMode(boolean logAxisMode) {
        SpectrogramBMP.LogAxisPlotMode mode = SpectrogramBMP.LogAxisPlotMode.REPLOT;
        if (!logAxisMode) mode = SpectrogramBMP.LogAxisPlotMode.SEGMENT;
        spectrogramPlot.getSpectrogramBMP().setLogAxisMode(mode);
    }

    public void setColorMap(String colorMapName) {
        spectrogramPlot.setColorMap(colorMapName);
    }

    public PlotMode getShowMode() {
        return showMode;
    }

    public void setReady(OnReadyListener onReadyListener) {
        this.onReadyListener = onReadyListener;
    }

    public SpectrogramPlot getSpectrogramPlot() {
        return spectrogramPlot;
    }

    private static class SavedState extends BaseSavedState {
        int freqAxisAlongX;
        double markerFreqSpectrum;
        double markerFreqSpectrogram;
        double markerDB;
        double xZoom;
        double xShift;
        double yZoom;
        double yShift;
        double spectrumXZoom;
        double spectrumXShift;
        double spectrumYZoom;
        double spectrumYShift;
        double spectrogramFreqZoom;
        double spectrogramFreqShift;
        double spectrogramTimeZoom;
        double spectrogramTimeShift;

        double[] tmpDBSpectrum;

        int nFreqPoints;
        int nTimePoints;
        int iTimePointer;

        SavedState(Parcelable state) {
            super(state);
        }

        private SavedState(Parcel in) {
            super(in);
            freqAxisAlongX = in.readInt();
            markerFreqSpectrum = in.readDouble();
            markerFreqSpectrogram = in.readDouble();
            markerDB = in.readDouble();
            xZoom = in.readDouble();
            xShift = in.readDouble();
            yZoom = in.readDouble();
            yShift = in.readDouble();
            spectrumXZoom = in.readDouble();
            spectrumXShift = in.readDouble();
            spectrumYZoom = in.readDouble();
            spectrumYShift = in.readDouble();
            spectrogramFreqZoom = in.readDouble();
            spectrogramFreqShift = in.readDouble();
            spectrogramTimeZoom = in.readDouble();
            spectrogramTimeShift = in.readDouble();

            tmpDBSpectrum = in.createDoubleArray();

            nFreqPoints = in.readInt();
            nTimePoints = in.readInt();
            iTimePointer = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(freqAxisAlongX);
            out.writeDouble(markerFreqSpectrum);
            out.writeDouble(markerFreqSpectrogram);
            out.writeDouble(markerDB);
            out.writeDouble(xZoom);
            out.writeDouble(xShift);
            out.writeDouble(yZoom);
            out.writeDouble(yShift);
            out.writeDouble(spectrumXZoom);
            out.writeDouble(spectrumXShift);
            out.writeDouble(spectrumYZoom);
            out.writeDouble(spectrumYShift);
            out.writeDouble(spectrogramFreqZoom);
            out.writeDouble(spectrogramFreqShift);
            out.writeDouble(spectrogramTimeZoom);
            out.writeDouble(spectrogramTimeShift);

            out.writeDoubleArray(tmpDBSpectrum);

            out.writeInt(nFreqPoints);
            out.writeInt(nTimePoints);
            out.writeInt(iTimePointer);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}