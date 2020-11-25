package com.appacoustic.cointester.aaa.analyzer

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
class ScreenPhysicalMapping {
    var mapType // Linear or Log
        : Type
        private set
    var nCanvasPx: Double
    var lowerBound: Double
    var upperBound // Physical limits
        : Double
    var lowerViewBound: Double
        private set
    var upperViewBound // View bounds
        : Double
        private set
    var zoom = 1.0
        private set
    var shift = 0.0 // zoom==1: no zooming, shift=0: no shift
        private set

    enum class Type(val value: Int) {
        LINEAR(0), LINEAR_ON(1), LOG(2);
    }

    constructor(
        nCanvasPx: Double,
        lowerBound: Double,
        upperBound: Double,
        mapType: Type
    ) {
        this.nCanvasPx = nCanvasPx
        this.lowerBound = lowerBound
        lowerViewBound = this.lowerBound
        this.upperBound = upperBound
        upperViewBound = this.upperBound
        this.mapType = mapType
    }

    constructor(axis: ScreenPhysicalMapping) {
        mapType = axis.mapType
        nCanvasPx = axis.nCanvasPx
        lowerBound = axis.lowerBound
        upperBound = axis.upperBound
        lowerViewBound = axis.lowerViewBound
        upperViewBound = axis.upperViewBound
        zoom = axis.zoom
        shift = axis.shift
    }

    fun setBounds(
        lowerBound: Double,
        upperBound: Double
    ) {
        this.lowerBound = lowerBound
        this.upperBound = upperBound
        // Reset view range, preserve zoom and shift
        setZoomShift(
            zoom,
            shift
        )
        if (AnalyzerUtil.isAlmostInteger(lowerViewBound)) { // Dirty fix...
            lowerViewBound = Math.round(lowerViewBound).toDouble()
        }
        if (AnalyzerUtil.isAlmostInteger(upperViewBound)) {
            upperViewBound = Math.round(upperViewBound).toDouble()
        }
        setViewBounds(
            lowerViewBound,
            upperViewBound
        ) // Refine zoom shift
    }

    fun setZoomShift(
        zoom: Double,
        shift: Double
    ) {
        this.zoom = zoom
        this.shift = shift
        lowerViewBound = valueFromUnitPosition(
            0.0,
            zoom,
            shift
        )
        upperViewBound = valueFromUnitPosition(
            1.0,
            zoom,
            shift
        )
    }

    /**
     * Set zoom and shift from physical value bounds.
     */
    fun setViewBounds(
        lowerViewBound: Double,
        upperViewBound: Double
    ) {
        if (lowerViewBound == upperViewBound) {
            return  // Or throw an exception?
        }
        val lowerPosition = unitPositionFromValue(
            lowerViewBound,
            lowerBound,
            upperBound
        )
        val upperPosition = unitPositionFromValue(
            upperViewBound,
            lowerBound,
            upperBound
        )
        zoom = 1 / (upperPosition - lowerPosition)
        shift = lowerPosition
        this.lowerViewBound = lowerViewBound
        this.upperViewBound = upperViewBound
    }

    private fun unitPositionFromValue(
        value: Double,
        lowerBound: Double,
        upperBound: Double
    ): Double {
        if (lowerBound == upperBound) return 0.0
        return if (mapType == Type.LINEAR) {
            (value - lowerBound) / (upperBound - lowerBound)
        } else {
            Math.log(value / lowerBound) / Math.log(upperBound / lowerBound)
        }
    }

    private fun valueFromUnitPosition(
        unit: Double,
        zoom: Double,
        shift: Double
    ): Double {
        if (zoom == 0.0) return 0.0
        return if (mapType == Type.LINEAR) {
            (unit / zoom + shift) * (upperBound - lowerBound) + lowerBound
        } else {
            Math.exp((unit / zoom + shift) * Math.log(upperBound / lowerBound)) * lowerBound
        }
    }

    fun pxFromValue(value: Double): Double {
        return unitPositionFromValue(
            value,
            lowerViewBound,
            upperViewBound
        ) * nCanvasPx
    }

    fun valueFromPx(px: Double): Double {
        return if (nCanvasPx == 0.0) lowerViewBound else valueFromUnitPosition(
            px / nCanvasPx,
            zoom,
            shift
        )
    }

    fun pxNoZoomFromValue(value: Double): Double {
        return unitPositionFromValue(
            value,
            lowerBound,
            upperBound
        ) * nCanvasPx
    }

    val boundsRange: Double
        get() = upperBound - lowerBound

    fun reverseBounds() {
        val oldLowerViewBound = lowerViewBound
        val oldUpperViewBound = upperViewBound
        setBounds(
            upperBound,
            lowerBound
        )
        setViewBounds(
            oldUpperViewBound,
            oldLowerViewBound
        )
    }

    fun setMappingType(
        mapType: Type,
        freqLowerBoundForLog: Double
    ) {
        // Set internal variables if possible
        var lowerViewBound = lowerViewBound
        var upperViewBound = upperViewBound
        if (mapType == Type.LOG) {
            if (lowerBound == 0.0) lowerBound = freqLowerBoundForLog
            if (upperBound == 0.0) upperBound = freqLowerBoundForLog
        } else {
            if (lowerBound == freqLowerBoundForLog) lowerBound = 0.0
            if (upperBound == freqLowerBoundForLog) upperBound = 0.0
        }
        val needUpdateZoomShift = this.mapType != mapType
        this.mapType = mapType
        if (!needUpdateZoomShift || nCanvasPx == 0.0 || lowerBound == upperBound) {
            return
        }

        // Update zoom and shift
        // freqLowerBoundForLog is for how to map zero
        // Only support non-negative bounds
        if (mapType == Type.LOG) {
            if (lowerViewBound < 0 || upperViewBound < 0) {
                return
            }
            if (lowerViewBound < freqLowerBoundForLog) lowerViewBound = freqLowerBoundForLog
            if (upperViewBound < freqLowerBoundForLog) upperViewBound = freqLowerBoundForLog
        } else {
            if (lowerViewBound <= freqLowerBoundForLog) lowerViewBound = 0.0
            if (upperViewBound <= freqLowerBoundForLog) upperViewBound = 0.0
        }
        setViewBounds(
            lowerViewBound,
            upperViewBound
        )
    }
}
