package com.appacoustic.cointester.analyzer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.appacoustic.cointester.analyzer.AxisTickLabels;
import com.appacoustic.cointester.analyzer.GridLabel;
import com.appacoustic.cointester.analyzer.ScreenPhysicalMapping;
import com.gabrielmorenoibarra.g.GLog;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;

/**
 * The spectrum plot part of AnalyzerGraphic.
 */
public class SpectrumPlot {

    private static final String TAG = SpectrumPlot.class.getSimpleName();

    private boolean showLines;
    private Paint linePaint, linePaintLight;
    private Paint markerPaint;
    private Paint gridPaint;
    private Paint labelPaint;
    private int canvasHeight = 0, canvasWidth = 0;

    private GridLabel freqGridLabel;
    private GridLabel dBGridLabel;
    private float dPRatio;
    private double gridDensity = 1 / 85.0; // Every 85 pixel one grid line (on average)

    private double markerFreq, markerDB; // Marker location
    private ScreenPhysicalMapping axisX;  // For frequency axis
    private ScreenPhysicalMapping axisY;  // For dB axis

    public SpectrumPlot(Context _context) {
        dPRatio = _context.getResources().getDisplayMetrics().density;

        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#0D2C6D"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1);

        linePaintLight = new Paint(linePaint);
        linePaintLight.setColor(Color.parseColor("#3AB3E2"));

        gridPaint = new Paint();
        gridPaint.setColor(Color.DKGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(0.6f * dPRatio);

        markerPaint = new Paint(gridPaint);
        markerPaint.setColor(Color.parseColor("#00CD00"));

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.GRAY);
        labelPaint.setTextSize(14.0f * dPRatio);
        labelPaint.setTypeface(Typeface.MONOSPACE);  // or Typeface.SANS_SERIF

        markerFreq = markerDB = 0f;

        freqGridLabel = new GridLabel(GridLabel.Type.FREQ, canvasWidth * gridDensity / dPRatio);
        dBGridLabel = new GridLabel(GridLabel.Type.DB, canvasHeight * gridDensity / dPRatio);

        axisX = new ScreenPhysicalMapping(0, 0, 0, ScreenPhysicalMapping.Type.LINEAR);
        axisY = new ScreenPhysicalMapping(0, 0, 0, ScreenPhysicalMapping.Type.LINEAR);
    }

    public void setCanvas(int _canvasWidth, int _canvasHeight, double[] axisBounds) {
//        GLog.i("SpectrumPlot", "setCanvas: W="+_canvasWidth+"  H="+_canvasHeight);
        canvasWidth = _canvasWidth;
        canvasHeight = _canvasHeight;
        freqGridLabel.setDensity(canvasWidth * gridDensity / dPRatio);
        dBGridLabel.setDensity(canvasHeight * gridDensity / dPRatio);
        getAxisX().setNCanvasPx(canvasWidth);
        axisY.setNCanvasPx(canvasHeight);
        if (axisBounds != null) {
            getAxisX().setBounds(axisBounds[0], axisBounds[2]);
            axisY.setBounds(axisBounds[1], axisBounds[3]);
        }
    }

    public void setZooms(double xZoom, double xShift, double yZoom, double yShift) {
        getAxisX().setZoomShift(xZoom, xShift);
        axisY.setZoomShift(yZoom, yShift);
    }

    // Linear or Logarithmic frequency axis
    public void setFreqAxisMode(ScreenPhysicalMapping.Type mapType, double freq_lower_bound_for_log, GridLabel.Type gridType) {
        getAxisX().setMappingType(mapType, freq_lower_bound_for_log);
        freqGridLabel.setGridType(gridType);
        GLog.i(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": set to mode " + mapType + " axisX.vL=" + getAxisX().getLowerBound() + "  freq_lower_bound_for_log = " + freq_lower_bound_for_log);
    }

    private void drawGridLines(Canvas c) {
        for (int i = 0; i < freqGridLabel.values.length; i++) {
            float xPos = (float) getAxisX().pxFromValue(freqGridLabel.values[i]);
            c.drawLine(xPos, 0, xPos, canvasHeight, gridPaint);
        }
        for (int i = 0; i < dBGridLabel.values.length; i++) {
            float yPos = (float) axisY.pxFromValue(dBGridLabel.values[i]);
            c.drawLine(0, yPos, canvasWidth, yPos, gridPaint);
        }
    }

    private double clampDB(double value) {
        if (value < AnalyzerGraphicView.MIN_DB || Double.isNaN(value)) {
            value = AnalyzerGraphicView.MIN_DB;
        }
        return value;
    }

    private Matrix matrix = new Matrix();
    private float[] tmpLineXY = new float[0];  // cache line data for drawing
    private double[] dBCache = null;

    // Plot the spectrum into the Canvas c
    private void drawSpectrumOnCanvas(Canvas c, final double[] _db) {
        if (canvasHeight < 1 || _db == null || _db.length == 0) {
            return;
        }
        synchronized (_db) {  // TODO: need lock on tmpDBSpectrum, but how?
            if (dBCache == null || dBCache.length != _db.length) {
                GLog.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": new dBCache");
                dBCache = new double[_db.length];
            }
            System.arraycopy(_db, 0, dBCache, 0, _db.length);
        }

        double canvasMinFreq = getAxisX().getLowerViewBound();
        double canvasMaxFreq = getAxisX().getUpperViewBound();
        // There are db.length frequency points, including DC component
        int nFreqPointsTotal = dBCache.length - 1;
        double freqDelta = getAxisX().getUpperBound() / nFreqPointsTotal;
        int beginFreqPt = (int) floor(canvasMinFreq / freqDelta);    // pointer to tmpLineXY
        int endFreqPt = (int) ceil(canvasMaxFreq / freqDelta) + 1;
        final double minYCanvas = axisY.pxNoZoomFromValue(AnalyzerGraphicView.MIN_DB);

        // add one more boundary points
        if (beginFreqPt == 0 && getAxisX().getMapType() == ScreenPhysicalMapping.Type.LOG) {
            beginFreqPt++;
        }
        if (endFreqPt > dBCache.length) {
            endFreqPt = dBCache.length;  // just in case canvasMaxFreq / freqDelta > nFreqPointsTotal
        }

        if (tmpLineXY.length != 4 * (dBCache.length)) {
            GLog.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + ": new tmpLineXY");
            tmpLineXY = new float[4 * (dBCache.length)];
        }

        // spectrum bar
        if (!showLines) {
            c.save();
            // If bars are very close to each other, draw bars as lines
            // Otherwise, zoom in so that lines look like bars.
            if (endFreqPt - beginFreqPt >= getAxisX().getNCanvasPx() / 2
                    || getAxisX().getMapType() != ScreenPhysicalMapping.Type.LINEAR) {
                matrix.reset();
                matrix.setTranslate(0, (float) (-axisY.getShift() * canvasHeight));
                matrix.postScale(1, (float) axisY.getZoom());
                c.concat(matrix);
                //      float barWidthInPixel = 0.5f * freqDelta / (canvasMaxFreq - canvasMinFreq) * canvasWidth;
                //      if (barWidthInPixel > 2) {
                //        linePaint.setStrokeWidth(barWidthInPixel);
                //      } else {
                //        linePaint.setStrokeWidth(0);
                //      }
                // plot directly to the canvas
                for (int i = beginFreqPt; i < endFreqPt; i++) {
                    float x = (float) getAxisX().pxFromValue(i * freqDelta);
                    float y = (float) axisY.pxNoZoomFromValue(clampDB(dBCache[i]));
                    if (y != canvasHeight) { // ...forgot why
                        tmpLineXY[4 * i] = x;
                        tmpLineXY[4 * i + 1] = (float) minYCanvas;
                        tmpLineXY[4 * i + 2] = x;
                        tmpLineXY[4 * i + 3] = y;
                    }
                }
                c.drawLines(tmpLineXY, 4 * beginFreqPt, 4 * (endFreqPt - beginFreqPt), linePaint);
            } else {
                // for zoomed linear scale
                int pixelStep = 2;  // each bar occupy this virtual pixel
                matrix.reset();
                double extraPixelAlignOffset = 0.0f;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//          // There is an shift for Android 4.4, while no shift for Android 2.3
//          // I guess that is relate to GL ES acceleration
//          if (c.isHardwareAccelerated()) {
//            extraPixelAlignOffset = 0.5f;
//          }
//        }
                matrix.setTranslate((float) (-getAxisX().getShift() * nFreqPointsTotal * pixelStep - extraPixelAlignOffset),
                        (float) (-axisY.getShift() * canvasHeight));
                matrix.postScale((float) (canvasWidth / ((canvasMaxFreq - canvasMinFreq) / freqDelta * pixelStep)), (float) axisY.getZoom());
                c.concat(matrix);
                // fill interval same as canvas pixel width.
                for (int i = beginFreqPt; i < endFreqPt; i++) {
                    float x = i * pixelStep;
                    float y = (float) axisY.pxNoZoomFromValue(clampDB(dBCache[i]));
                    if (y != canvasHeight) {
                        tmpLineXY[4 * i] = x;
                        tmpLineXY[4 * i + 1] = (float) minYCanvas;
                        tmpLineXY[4 * i + 2] = x;
                        tmpLineXY[4 * i + 3] = y;
                    }
                }
                c.drawLines(tmpLineXY, 4 * beginFreqPt, 4 * (endFreqPt - beginFreqPt), linePaint);
            }
            c.restore();
        }

        // spectrum line
        c.save();
        matrix.reset();
        matrix.setTranslate(0, (float) (-axisY.getShift() * canvasHeight));
        matrix.postScale(1, (float) axisY.getZoom());
        c.concat(matrix);
        float o_x = (float) getAxisX().pxFromValue(beginFreqPt * freqDelta);
        float o_y = (float) axisY.pxNoZoomFromValue(clampDB(dBCache[beginFreqPt]));
        for (int i = beginFreqPt + 1; i < endFreqPt; i++) {
            float x = (float) getAxisX().pxFromValue(i * freqDelta);
            float y = (float) axisY.pxNoZoomFromValue(clampDB(dBCache[i]));
            tmpLineXY[4 * i] = o_x;
            tmpLineXY[4 * i + 1] = o_y;
            tmpLineXY[4 * i + 2] = x;
            tmpLineXY[4 * i + 3] = y;
            o_x = x;
            o_y = y;
        }
        c.drawLines(tmpLineXY, 4 * (beginFreqPt + 1), 4 * (endFreqPt - beginFreqPt - 1), linePaintLight);
        c.restore();
    }

    // x, y is in pixel unit
    public void setMarker(double x, double y) {
        markerFreq = getAxisX().valueFromPx(x);  // frequency
        markerDB = axisY.valueFromPx(y);  // decibel
    }

    public double getMarkerFreq() {
        return canvasWidth == 0 ? 0 : markerFreq;
    }

    public double getMarkerDB() {
        return canvasHeight == 0 ? 0 : markerDB;
    }

    public void hideMarker() {
        markerFreq = 0;
        markerDB = 0;
    }

    private void drawMarker(Canvas c) {
        if (markerFreq == 0) {
            return;
        }
        float cX, cY;
        cX = (float) getAxisX().pxFromValue(markerFreq);
        cY = (float) axisY.pxFromValue(markerDB);
        c.drawLine(cX, 0, cX, canvasHeight, markerPaint);
        c.drawLine(0, cY, canvasWidth, cY, markerPaint);
    }

    // Plot spectrum with axis and ticks on the whole canvas c
    public void drawSpectrumPlot(Canvas c, double[] savedDBSpectrum) {
        freqGridLabel.updateGridLabels(getAxisX().getLowerViewBound(), getAxisX().getUpperViewBound());
        dBGridLabel.updateGridLabels(axisY.getLowerViewBound(), axisY.getUpperViewBound());
        drawGridLines(c);
        drawSpectrumOnCanvas(c, savedDBSpectrum);
        drawMarker(c);
        AxisTickLabels.draw(c, getAxisX(), freqGridLabel,
                0f, 0f, 0, 1,
                labelPaint, gridPaint, gridPaint);
        AxisTickLabels.draw(c, axisY, dBGridLabel,
                0f, 0f, 1, 1,
                labelPaint, gridPaint, gridPaint);
    }

    public ScreenPhysicalMapping getAxisX() {
        return axisX;
    }

    public ScreenPhysicalMapping getAxisY() {
        return axisY;
    }

    public boolean isShowLines() {
        return showLines;
    }

    public void setShowLines(boolean showLines) {
        this.showLines = showLines;
    }

    public void setMarkerFreq(double markerFreq) {
        this.markerFreq = markerFreq;
    }

    public void setMarkerDB(double markerDB) {
        this.markerDB = markerDB;
    }
}
