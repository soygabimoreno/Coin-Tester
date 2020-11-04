package com.appacoustic.cointester.aaa.analyzer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.appacoustic.cointester.aaa.analyzer.AxisTickLabels;
import com.appacoustic.cointester.aaa.analyzer.GridLabel;
import com.appacoustic.cointester.aaa.analyzer.ScreenPhysicalMapping;
import com.appacoustic.cointester.aaa.analyzer.SpectrogramBMP;
import com.appacoustic.cointester.aaa.analyzer.model.AnalyzerParams;
import com.gabrielmorenoibarra.k.util.KLog;

/**
 * The spectrogram plot part of AnalyzerGraphic.
 */
public class SpectrogramPlot {

    private static final String TAG = SpectrogramPlot.class.getSimpleName();

    private boolean showFreqAlongX;

    private SpectrogramBMP spectrogramBMP = new SpectrogramBMP();

    public enum TimeAxisMode {  // java's enum type is inconvenient
        SHIFT(0), OVERWRITE(1);       // 0: moving (shifting) spectrogram, 1: overwriting in loop

        private final int value;

        TimeAxisMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private TimeAxisMode showModeSpectrogram = TimeAxisMode.OVERWRITE;
    private boolean showTimeAxis = true;

    private double timeWatch = 4.0;  // TODO: a bit duplicated, use axisTime
    private volatile int timeMultiplier = 1;  // should be accorded with nFFTAverage in AnalyzerActivity
    private int nFreqPoints;  // TODO: a bit duplicated, use BMP.nFreq
    private int nTimePoints;  // TODO: a bit duplicated, use BMP.nTime
    private double timeInc;

    private Matrix matrixSpectrogram = new Matrix();
    private Paint smoothBmpPaint;
    private Paint backgroundPaint;
    private Paint markerPaint;
    private Paint gridPaint, rulerBrightPaint;
    private Paint labelPaint;
    private Paint markerTimePaint;

    private ScreenPhysicalMapping axisFreq;
    private ScreenPhysicalMapping axisTime;
    private GridLabel fqGridLabel;
    private GridLabel tmGridLabel;
    private double markerFreq;

    private float DPRatio;
    private float gridDensity = 1 / 85f;  // every 85 pixel one grid line, on average
    private int canvasHeight = 0, canvasWidth = 0;
    private float labelBeginX, labelBeginY;

    public SpectrogramPlot(Context _context) {
        DPRatio = _context.getResources().getDisplayMetrics().density;

        gridPaint = new Paint();
        gridPaint.setColor(Color.DKGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(0.6f * DPRatio);

        markerPaint = new Paint(gridPaint);
        markerPaint.setColor(Color.parseColor("#00CD00"));

        markerTimePaint = new Paint(markerPaint);
        markerTimePaint.setStyle(Paint.Style.STROKE);
        markerTimePaint.setStrokeWidth(0);

        rulerBrightPaint = new Paint();
        rulerBrightPaint.setColor(Color.rgb(99, 99, 99));  // 99: between Color.DKGRAY and Color.GRAY
        rulerBrightPaint.setStyle(Paint.Style.STROKE);
        rulerBrightPaint.setStrokeWidth(1);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.GRAY);
        labelPaint.setTextSize(14.0f * DPRatio);
        labelPaint.setTypeface(Typeface.MONOSPACE);  // or Typeface.SANS_SERIF

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);

        markerFreq = 0f;

        fqGridLabel = new GridLabel(GridLabel.Type.FREQ, canvasWidth * gridDensity / DPRatio);
        tmGridLabel = new GridLabel(GridLabel.Type.TIME, canvasHeight * gridDensity / DPRatio);

        axisFreq = new ScreenPhysicalMapping(0, 0, 0, ScreenPhysicalMapping.Type.LINEAR);
        axisTime = new ScreenPhysicalMapping(0, 0, 0, ScreenPhysicalMapping.Type.LINEAR);
    }

    // Before calling this, axes should be initialized.
    public void setupSpectrogram(AnalyzerParams params) {
        int sampleRate = params.getSampleRate();
        int fftLen = params.getFftLength();
        int hopLen = params.getHopLength();
        int nAve = params.getNFftAverage();
        double timeDurationE = params.getSpectrogramDuration();

        timeWatch = timeDurationE;
        timeMultiplier = nAve;
        timeInc = (double) hopLen / sampleRate;  // time of each slice
        nFreqPoints = fftLen / 2;           // no direct current term
        nTimePoints = (int) Math.ceil(timeWatch / timeInc);
        spectrogramBMP.init(nFreqPoints, nTimePoints, axisFreq);
        KLog.i(
                "sampleRate    = " + sampleRate +
                        "\n  fFTLength        = " + fftLen +
                        "\n  timeDurationE = " + timeDurationE + " * " + nAve + "  (" + nTimePoints + " points)" +
                        "\n  canvas size freq= " + axisFreq.getNCanvasPx() + " time=" + axisTime.getNCanvasPx());
    }

    public void setCanvas(int _canvasWidth, int _canvasHeight, double[] axisBounds) {
        canvasWidth = _canvasWidth;
        canvasHeight = _canvasHeight;
        if (canvasHeight > 1 && canvasWidth > 1) {
            updateDrawingWindowSize();
        }
        if (axisBounds != null) {
            if (showFreqAlongX) {
                axisFreq.setBounds(axisBounds[0], axisBounds[2]);
                axisTime.setBounds(axisBounds[1], axisBounds[3]);
            } else {
                axisTime.setBounds(axisBounds[0], axisBounds[2]);
                axisFreq.setBounds(axisBounds[1], axisBounds[3]);
            }
            if (showModeSpectrogram == TimeAxisMode.SHIFT) {
                double b1 = axisTime.getLowerBound();
                double b2 = axisTime.getUpperBound();
                axisTime.setBounds(b2, b1);
            }
        }
        fqGridLabel.setDensity(axisFreq.getNCanvasPx() * gridDensity / DPRatio);
        tmGridLabel.setDensity(axisTime.getNCanvasPx() * gridDensity / DPRatio);

        spectrogramBMP.updateAxis(axisFreq);
    }

    public void setZooms(double xZoom, double xShift, double yZoom, double yShift) {
        //KLog.Companion.i("setZooms():  xZoom=" + xZoom + "  xShift=" + xShift + "  yZoom=" + yZoom + "  yShift=" + yShift);
        if (showFreqAlongX) {
            axisFreq.setZoomShift(xZoom, xShift);
            axisTime.setZoomShift(yZoom, yShift);
        } else {
            axisFreq.setZoomShift(yZoom, yShift);
            axisTime.setZoomShift(xZoom, xShift);
        }
        spectrogramBMP.updateZoom();
    }

    // Linear or Logarithmic frequency axis
    public void setFreqAxisMode(ScreenPhysicalMapping.Type mapType, double freq_lower_bound_for_log, GridLabel.Type gridType) {
        axisFreq.setMappingType(mapType, freq_lower_bound_for_log);
        fqGridLabel.setGridType(gridType);
        spectrogramBMP.updateAxis(axisFreq);
    }

    public void setColorMap(String colorMapName) {
        spectrogramBMP.setColorMap(colorMapName);
    }

    public void setSpectrogramDBLowerBound(double b) {
        spectrogramBMP.setdBLowerBound(b);
    }

    public void setMarker(double x, double y) {
        if (showFreqAlongX) {
            //markerFreq = axisBounds.width() * (xShift + (x-labelBeginX)/(canvasWidth-labelBeginX)/xZoom);  // frequency
            markerFreq = axisFreq.valueFromPx(x - labelBeginX);
        } else {
            //markerFreq = axisBounds.width() * (1 - yShift - y/labelBeginY/yZoom);  // frequency
            markerFreq = axisFreq.valueFromPx(y);
        }
        if (markerFreq < 0) {
            markerFreq = 0;
        }
    }

    public TimeAxisMode getSpectrogramMode() {
        return showModeSpectrogram;
    }

    public double getMarkerFreq() {
        return canvasWidth == 0 ? 0 : markerFreq;
    }

    public void hideMarker() {
        markerFreq = 0;
    }

    public void setTimeMultiplier(int nAve) {
        timeMultiplier = nAve;
        if (showModeSpectrogram == TimeAxisMode.SHIFT) {
            axisTime.setLowerBound(timeWatch * timeMultiplier);
        } else {
            axisTime.setUpperBound(timeWatch * timeMultiplier);
        }
        // keep zoom shift
        axisTime.setZoomShift(axisTime.getZoom(), axisTime.getShift());
    }

    public void setShowTimeAxis(boolean showTimeAxis) {
        this.showTimeAxis = showTimeAxis;
    }

    public void setSpectrogramModeShifting(boolean b) {
        if ((showModeSpectrogram == TimeAxisMode.SHIFT) != b) {
            // mode change, swap time bounds.
            double b1 = axisTime.getLowerBound();
            double b2 = axisTime.getUpperBound();
            axisTime.setBounds(b2, b1);
        }
        if (b) {
            showModeSpectrogram = TimeAxisMode.SHIFT;
            setPause(isPaused);  // update time estimation
        } else {
            showModeSpectrogram = TimeAxisMode.OVERWRITE;
        }
    }

    public void setShowFreqAlongX(boolean b) {
        if (showFreqAlongX != b) {
            // Set (swap) canvas size
            double t = axisFreq.getNCanvasPx();
            axisFreq.setNCanvasPx(axisTime.getNCanvasPx());
            axisTime.setNCanvasPx(t);
            // swap bounds of freq axis
            axisFreq.reverseBounds();

            fqGridLabel.setDensity(axisFreq.getNCanvasPx() * gridDensity / DPRatio);
            tmGridLabel.setDensity(axisTime.getNCanvasPx() * gridDensity / DPRatio);
        }
        showFreqAlongX = b;
    }

    public void setSmoothRender(boolean b) {
        if (b) {
            smoothBmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        } else {
            smoothBmpPaint = null;
        }
    }

    private double timeLastSample = 0;
    private boolean updateTimeDiff = false;

    public void prepare() {
        if (showModeSpectrogram == TimeAxisMode.SHIFT)
            setPause(isPaused);
    }

    public void setPause(boolean p) {
        if (!p) {
            timeLastSample = System.currentTimeMillis() / 1000.0;
        }
        isPaused = p;
    }

    // Will be called in another thread (SamplingLoopThread)
    // db.length == 2^n + 1
    public void saveRowSpectrumAsColor(final double[] db) {
        // For time compensate in shifting mode
        double tNow = System.currentTimeMillis() / 1000.0;
        updateTimeDiff = true;
        if (Math.abs(timeLastSample - tNow) > 0.5) {
            timeLastSample = tNow;
        } else {
            timeLastSample += timeInc * timeMultiplier;
            timeLastSample += (tNow - timeLastSample) * 1e-2;  // track current time
        }

        spectrogramBMP.fill(db);
    }

    public float getLabelBeginY() {
        float textHeigh = labelPaint.getFontMetrics(null);
        float labelLargeLen = 0.5f * textHeigh;
        if (!showFreqAlongX && !showTimeAxis) {
            return canvasHeight;
        } else {
            return canvasHeight - 0.6f * labelLargeLen - textHeigh;
        }
    }

    // Left margin for ruler
    public float getLabelBeginX() {
        float textHeight = labelPaint.getFontMetrics(null);
        float labelLaegeLen = 0.5f * textHeight;
        if (showFreqAlongX) {
            if (showTimeAxis) {
                int j = 3;
                for (int i = 0; i < tmGridLabel.strings.length; i++) {
                    if (j < tmGridLabel.strings[i].length()) {
                        j = tmGridLabel.strings[i].length();
                    }
                }
                return 0.6f * labelLaegeLen + j * 0.5f * textHeight;
            } else {
                return 0;
            }
        } else {
            return 0.6f * labelLaegeLen + 2.5f * textHeight;
        }
    }

    private float labelBeginXOld = 0;
    private float labelBeginYOld = 0;

    private void updateDrawingWindowSize() {
        labelBeginX = getLabelBeginX();  // this seems will make the scaling gesture inaccurate
        labelBeginY = getLabelBeginY();
        if (labelBeginX != labelBeginXOld || labelBeginY != labelBeginYOld) {
            if (showFreqAlongX) {
                axisFreq.setNCanvasPx(canvasWidth - labelBeginX);
                axisTime.setNCanvasPx(labelBeginY);
            } else {
                axisTime.setNCanvasPx(canvasWidth - labelBeginX);
                axisFreq.setNCanvasPx(labelBeginY);
            }
            labelBeginXOld = labelBeginX;
            labelBeginYOld = labelBeginY;
        }
    }

    private void drawFreqMarker(Canvas c) {
        if (markerFreq == 0) return;
        float cX, cY;
        // Show only the frequency marker
        if (showFreqAlongX) {
            cX = (float) axisFreq.pxFromValue(markerFreq) + labelBeginX;
            c.drawLine(cX, 0, cX, labelBeginY, markerPaint);
        } else {
            cY = (float) axisFreq.pxFromValue(markerFreq);
            c.drawLine(labelBeginX, cY, canvasWidth, cY, markerPaint);
        }
    }

    // Draw time axis for spectrogram
    // Working in the original canvas frame
    private void drawTimeAxis(Canvas c, float labelBeginX, float labelBeginY, boolean drawOnXAxis) {
        if (drawOnXAxis) {
            AxisTickLabels.draw(c, axisTime, tmGridLabel,
                    labelBeginX, labelBeginY, 0, 1,
                    labelPaint, gridPaint, rulerBrightPaint);
        } else {
            AxisTickLabels.draw(c, axisTime, tmGridLabel,
                    labelBeginX, 0, 1, -1,
                    labelPaint, gridPaint, rulerBrightPaint);
        }
    }

    // Draw frequency axis for spectrogram
    // Working in the original canvas frame
    private void drawFreqAxis(Canvas c, float labelBeginX, float labelBeginY, boolean drawOnXAxis) {
        if (drawOnXAxis) {
            AxisTickLabels.draw(c, axisFreq, fqGridLabel,
                    labelBeginX, labelBeginY, 0, 1,
                    labelPaint, gridPaint, rulerBrightPaint);
        } else {
            AxisTickLabels.draw(c, axisFreq, fqGridLabel,
                    labelBeginX, 0, 1, -1,
                    labelPaint, gridPaint, rulerBrightPaint);
        }
    }

    private double pixelTimeCompensate = 0;
    private volatile boolean isPaused = false;

    // Plot spectrogram with axis and ticks on the whole canvas c
    public void drawSpectrogramPlot(Canvas c) {
        if (canvasWidth == 0 || canvasHeight == 0) {
            return;
        }
        updateDrawingWindowSize();
        fqGridLabel.setDensity(axisFreq.getNCanvasPx() * gridDensity / DPRatio);
        tmGridLabel.setDensity(axisTime.getNCanvasPx() * gridDensity / DPRatio);
        fqGridLabel.updateGridLabels(axisFreq.getLowerViewBound(), axisFreq.getUpperViewBound());
        tmGridLabel.updateGridLabels(axisTime.getLowerViewBound(), axisTime.getUpperViewBound());

        // show Spectrogram
        double halfFreqResolutionShift;  // move the color patch to match the center frequency
        if (axisFreq.getMapType() == ScreenPhysicalMapping.Type.LINEAR) {
            halfFreqResolutionShift = axisFreq.getZoom() * axisFreq.getNCanvasPx() / nFreqPoints / 2;
        } else {
            halfFreqResolutionShift = 0;  // the correction is included in log axis render algo.
        }
        matrixSpectrogram.reset();
        if (showFreqAlongX) {
            // when xZoom== 1: nFreqPoints -> canvasWidth; 0 -> labelBeginX
            matrixSpectrogram.postScale((float) (axisFreq.getZoom() * axisFreq.getNCanvasPx() / nFreqPoints),
                    (float) (axisTime.getZoom() * axisTime.getNCanvasPx() / nTimePoints));
            matrixSpectrogram.postTranslate((float) (labelBeginX - axisFreq.getShift() * axisFreq.getZoom() * axisFreq.getNCanvasPx() + halfFreqResolutionShift),
                    (float) (-axisTime.getShift() * axisTime.getZoom() * axisTime.getNCanvasPx()));
        } else {
            // postRotate() will make c.drawBitmap about 20% slower, don't know why
            matrixSpectrogram.postRotate(-90);
            matrixSpectrogram.postScale((float) (axisTime.getZoom() * axisTime.getNCanvasPx() / nTimePoints),
                    (float) (axisFreq.getZoom() * axisFreq.getNCanvasPx() / nFreqPoints));
            // (1-yShift) is relative position of shift (after rotation)
            // yZoom*labelBeginY is canvas length in frequency direction in pixel unit
            matrixSpectrogram.postTranslate((float) (labelBeginX - axisTime.getShift() * axisTime.getZoom() * axisTime.getNCanvasPx()),
                    (float) ((1 - axisFreq.getShift()) * axisFreq.getZoom() * axisFreq.getNCanvasPx() - halfFreqResolutionShift));
        }
        c.save();
        c.concat(matrixSpectrogram);

        // Time compensate to make it smoother shifting.
        // But if user pressed pause, stop compensate.
        if (!isPaused && updateTimeDiff) {
            double timeCurrent = System.currentTimeMillis() / 1000.0;
            pixelTimeCompensate = (timeLastSample - timeCurrent) / (timeInc * timeMultiplier * nTimePoints) * nTimePoints;
            updateTimeDiff = false;
//            KLog.Companion.i(" time diff = " + (timeLastSample - timeCurrent));
        }
        if (showModeSpectrogram == TimeAxisMode.SHIFT) {
            c.translate(0.0f, (float) pixelTimeCompensate);
        }

        if (axisFreq.getMapType() == ScreenPhysicalMapping.Type.LOG &&
                spectrogramBMP.logAxisMode == SpectrogramBMP.LogAxisPlotMode.REPLOT) {
            // Revert the effect of axisFreq.getZoom() axisFreq.getShift() for the mode REPLOT
            c.scale((float) (1 / axisFreq.getZoom()), 1f);
            if (showFreqAlongX) {
                c.translate((float) (nFreqPoints * axisFreq.getShift() * axisFreq.getZoom()), 0.0f);
            } else {
                c.translate((float) (nFreqPoints * (1f - axisFreq.getShift() - 1f / axisFreq.getZoom()) * axisFreq.getZoom()), 0.0f);
            }
        }

        spectrogramBMP.draw(c, axisFreq.getMapType(), showModeSpectrogram, smoothBmpPaint, markerTimePaint);
        c.restore();

        drawFreqMarker(c);

        if (showFreqAlongX) {
            c.drawRect(0, labelBeginY, canvasWidth, canvasHeight, backgroundPaint);
            drawFreqAxis(c, labelBeginX, labelBeginY, showFreqAlongX);
            if (labelBeginX > 0) {
                c.drawRect(0, 0, labelBeginX, labelBeginY, backgroundPaint);
                drawTimeAxis(c, labelBeginX, labelBeginY, !showFreqAlongX);
            }
        } else {
            c.drawRect(0, 0, labelBeginX, labelBeginY, backgroundPaint);
            drawFreqAxis(c, labelBeginX, labelBeginY, showFreqAlongX);
            if (labelBeginY != canvasHeight) {
                c.drawRect(0, labelBeginY, canvasWidth, canvasHeight, backgroundPaint);
                drawTimeAxis(c, labelBeginX, labelBeginY, !showFreqAlongX);
            }
        }
    }

    public SpectrogramBMP getSpectrogramBMP() {
        return spectrogramBMP;
    }

    public boolean isShowFreqAlongX() {
        return showFreqAlongX;
    }

    public int getNFreqPoints() {
        return nFreqPoints;
    }

    public void setNFreqPoints(int nFreqPoints) {
        this.nFreqPoints = nFreqPoints;
    }

    public int getNTimePoints() {
        return nTimePoints;
    }

    public void setNTimePoints(int nTimePoints) {
        this.nTimePoints = nTimePoints;
    }

    public ScreenPhysicalMapping getAxisFreq() {
        return axisFreq;
    }

    public void setAxisFreq(ScreenPhysicalMapping axisFreq) {
        this.axisFreq = axisFreq;
    }

    public ScreenPhysicalMapping getAxisTime() {
        return axisTime;
    }

    public void setAxisTime(ScreenPhysicalMapping axisTime) {
        this.axisTime = axisTime;
    }

    public void setMarkerFreq(double markerFreq) {
        this.markerFreq = markerFreq;
    }

    public void setLabelBeginX(float labelBeginX) {
        this.labelBeginX = labelBeginX;
    }

    public void setLabelBeginY(float labelBeginY) {
        this.labelBeginY = labelBeginY;
    }
}
