package com.appacoustic.cointester.aaa.analyzer;

import static java.lang.Math.exp;
import static java.lang.Math.log;

//    | lower bound  ...  higher bound |  physical unit
//    | 0            ...             1 |  "unit 1" (Mapping can be linear or logarithmic)

// In LINEAR mode (default):
//    |lower value  ...    higher value|  physical unit
//    | shift       ... shift + 1/zoom |  "unit 1", 0=lowerBound, 1=upperBound
//    | 0 | 1 |     ...          | n-1 |  px

// In LINEAR_ON mode (not implemented):
//      |lower value ...    higher value|     physical unit
//      | shift      ... shift + 1/zoom |     "unit 1" window
//    | 0 | 1 |      ...             | n-1 |  px

/**
 * Mapping between physical value and screen pixel position
 */
public class ScreenPhysicalMapping {

    private static final String TAG = ScreenPhysicalMapping.class.getSimpleName();

    private Type mapType; // Linear or Log
    private double nCanvasPx;
    private double lowerBound, upperBound; // Physical limits
    private double lowerViewBound, upperViewBound; // View bounds
    private double zoom = 1, shift = 0; // zoom==1: no zooming, shift=0: no shift

    public enum Type {
        LINEAR(0), LINEAR_ON(1), LOG(2);
        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public ScreenPhysicalMapping(double nCanvasPx, double lowerBound, double upperBound, ScreenPhysicalMapping.Type mapType) {
        this.nCanvasPx = nCanvasPx;
        lowerViewBound = this.lowerBound = lowerBound;
        upperViewBound = this.upperBound = upperBound;
        this.mapType = mapType;
    }

    public ScreenPhysicalMapping(ScreenPhysicalMapping axis) {
        mapType = axis.mapType;
        nCanvasPx = axis.nCanvasPx;
        lowerBound = axis.lowerBound;
        upperBound = axis.upperBound;
        lowerViewBound = axis.lowerViewBound;
        upperViewBound = axis.upperViewBound;
        zoom = axis.zoom;
        shift = axis.shift;
    }

    public void setBounds(double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        // Reset view range, preserve zoom and shift
        setZoomShift(zoom, shift);
        if (AnalyzerUtil.isAlmostInteger(lowerViewBound)) { // Dirty fix...
            lowerViewBound = Math.round(lowerViewBound);
        }
        if (AnalyzerUtil.isAlmostInteger(upperViewBound)) {
            upperViewBound = Math.round(upperViewBound);
        }
        setViewBounds(lowerViewBound, upperViewBound); // Refine zoom shift
    }

    public void setZoomShift(double zoom, double shift) {
        this.zoom = zoom;
        this.shift = shift;
        lowerViewBound = valueFromUnitPosition(0, zoom, shift);
        upperViewBound = valueFromUnitPosition(1, zoom, shift);
    }

    /**
     * Set zoom and shift from physical value bounds.
     */
    public void setViewBounds(double lowerViewBound, double upperViewBound) {
        if (lowerViewBound == upperViewBound) {
            return; // Or throw an exception?
        }
        double lowerPosition = unitPositionFromValue(lowerViewBound, lowerBound, upperBound);
        double upperPosition = unitPositionFromValue(upperViewBound, lowerBound, upperBound);
        zoom = 1 / (upperPosition - lowerPosition);
        shift = lowerPosition;
        this.lowerViewBound = lowerViewBound;
        this.upperViewBound = upperViewBound;
    }

    private double unitPositionFromValue(double value, double lowerBound, double upperBound) {
        if (lowerBound == upperBound) return 0;
        if (mapType == Type.LINEAR) {
            return (value - lowerBound) / (upperBound - lowerBound);
        } else {
            return log(value / lowerBound) / log(upperBound / lowerBound);
        }
    }

    private double valueFromUnitPosition(double unit, double zoom, double shift) {
        if (zoom == 0) return 0;
        if (mapType == Type.LINEAR) {
            return (unit / zoom + shift) * (upperBound - lowerBound) + lowerBound;
        } else {
            return exp((unit / zoom + shift) * log(upperBound / lowerBound)) * lowerBound;
        }
    }

    public double pxFromValue(double value) {
        return unitPositionFromValue(value, lowerViewBound, upperViewBound) * nCanvasPx;
    }

    public double valueFromPx(double px) {
        if (nCanvasPx == 0) return lowerViewBound;
        return valueFromUnitPosition(px / nCanvasPx, zoom, shift);
    }

    public double getLowerViewBound() {
        return lowerViewBound;
    }

    public double getUpperViewBound() {
        return upperViewBound;
    }

    public double pxNoZoomFromValue(double value) {
        return unitPositionFromValue(value, lowerBound, upperBound) * nCanvasPx;
    }

    public double getBoundsRange() {
        return upperBound - lowerBound;
    }

    public void reverseBounds() {
        double oldLowerViewBound = lowerViewBound;
        double oldUpperViewBound = upperViewBound;
        setBounds(upperBound, lowerBound);
        setViewBounds(oldUpperViewBound, oldLowerViewBound);
    }

    public void setMappingType(ScreenPhysicalMapping.Type mapType, double freqLowerBoundForLog) {
        // Set internal variables if possible
        double lowerViewBound = getLowerViewBound();
        double upperViewBound = getUpperViewBound();
        if (mapType == Type.LOG) {
            if (lowerBound == 0) lowerBound = freqLowerBoundForLog;
            if (upperBound == 0) upperBound = freqLowerBoundForLog;
        } else {
            if (lowerBound == freqLowerBoundForLog) lowerBound = 0;
            if (upperBound == freqLowerBoundForLog) upperBound = 0;
        }
        boolean needUpdateZoomShift = this.mapType != mapType;
        this.mapType = mapType;
        if (!needUpdateZoomShift || nCanvasPx == 0 || lowerBound == upperBound) {
            return;
        }

        // Update zoom and shift
        // freqLowerBoundForLog is for how to map zero
        // Only support non-negative bounds
        if (mapType == Type.LOG) {
            if (lowerViewBound < 0 || upperViewBound < 0) {
                return;
            }
            if (lowerViewBound < freqLowerBoundForLog) lowerViewBound = freqLowerBoundForLog;
            if (upperViewBound < freqLowerBoundForLog) upperViewBound = freqLowerBoundForLog;
        } else {
            if (lowerViewBound <= freqLowerBoundForLog) lowerViewBound = 0;
            if (upperViewBound <= freqLowerBoundForLog) upperViewBound = 0;
        }
        setViewBounds(lowerViewBound, upperViewBound);
    }

    public Type getMapType() {
        return mapType;
    }

    public double getNCanvasPx() {
        return nCanvasPx;
    }

    public void setNCanvasPx(double nCanvasPx) {
        this.nCanvasPx = nCanvasPx;
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    public double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    public double getZoom() {
        return zoom;
    }

    public double getShift() {
        return shift;
    }
}
