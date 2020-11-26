package com.appacoustic.cointester.aaa.analyzer.view

import android.content.Context
import android.graphics.*
import com.appacoustic.cointester.aaa.analyzer.AxisTickLabels.draw
import com.appacoustic.cointester.aaa.analyzer.GridLabel
import com.appacoustic.cointester.aaa.analyzer.ScreenPhysicalMapping
import com.appacoustic.cointester.aaa.analyzer.view.AnalyzerGraphicView

/**
 * The spectrum plot part of AnalyzerGraphic.
 */
class SpectrumPlot(_context: Context) {
    var isShowLines = false
    private val linePaint: Paint
    private val linePaintLight: Paint
    private val markerPaint: Paint
    private val gridPaint: Paint
    private val labelPaint: Paint
    private var canvasHeight = 0
    private var canvasWidth = 0
    private val freqGridLabel: GridLabel
    private val dBGridLabel: GridLabel
    private val dPRatio: Float
    private val gridDensity = 1 / 85.0 // Every 85 pixel one grid line (on average)
    private var markerFreq: Double
    private var markerDB // Marker location
        : Double
    val axisX // For frequency axis
        : ScreenPhysicalMapping
    val axisY // For dB axis
        : ScreenPhysicalMapping

    fun setCanvas(
        _canvasWidth: Int,
        _canvasHeight: Int,
        axisBounds: DoubleArray?
    ) {
//        GLog.i("SpectrumPlot", "setCanvas: W="+_canvasWidth+"  H="+_canvasHeight);
        canvasWidth = _canvasWidth
        canvasHeight = _canvasHeight
        freqGridLabel.setDensity(canvasWidth * gridDensity / dPRatio)
        dBGridLabel.setDensity(canvasHeight * gridDensity / dPRatio)
        axisX.nCanvasPx = canvasWidth.toDouble()
        axisY.nCanvasPx = canvasHeight.toDouble()
        if (axisBounds != null) {
            axisX.setBounds(
                axisBounds[0],
                axisBounds[2]
            )
            axisY.setBounds(
                axisBounds[1],
                axisBounds[3]
            )
        }
    }

    fun setZooms(
        xZoom: Double,
        xShift: Double,
        yZoom: Double,
        yShift: Double
    ) {
        axisX.setZoomShift(
            xZoom,
            xShift
        )
        axisY.setZoomShift(
            yZoom,
            yShift
        )
    }

    // Linear or Logarithmic frequency axis
    fun setFreqAxisMode(
        mapType: ScreenPhysicalMapping.Type?,
        freq_lower_bound_for_log: Double,
        gridType: GridLabel.Type?
    ) {
        axisX.setMappingType(
            mapType!!,
            freq_lower_bound_for_log
        )
        freqGridLabel.setGridType(gridType)
    }

    private fun drawGridLines(c: Canvas) {
        for (i in freqGridLabel.values.indices) {
            val xPos = axisX.pxFromValue(freqGridLabel.values[i]).toFloat()
            c.drawLine(
                xPos,
                0f,
                xPos,
                canvasHeight.toFloat(),
                gridPaint
            )
        }
        for (i in dBGridLabel.values.indices) {
            val yPos = axisY.pxFromValue(dBGridLabel.values[i]).toFloat()
            c.drawLine(
                0f,
                yPos,
                canvasWidth.toFloat(),
                yPos,
                gridPaint
            )
        }
    }

    private fun clampDB(value: Double): Double {
        var value = value
        if (value < AnalyzerGraphicView.MIN_DB || java.lang.Double.isNaN(value)) {
            value = AnalyzerGraphicView.MIN_DB
        }
        return value
    }

    private val matrix = Matrix()
    private var tmpLineXY = FloatArray(0) // cache line data for drawing
    private var dBCache: DoubleArray? = null

    // Plot the spectrum into the Canvas c
    private fun drawSpectrumOnCanvas(
        c: Canvas,
        _db: DoubleArray?
    ) {
        if (canvasHeight < 1 || _db == null || _db.size == 0) {
            return
        }
        synchronized(_db) {
            // TODO: need lock on tmpDBSpectrum, but how?
            if (dBCache == null || dBCache!!.size != _db.size) {
                dBCache = DoubleArray(_db.size)
            }
            System.arraycopy(
                _db,
                0,
                dBCache,
                0,
                _db.size
            )
        }
        val canvasMinFreq = axisX.lowerViewBound
        val canvasMaxFreq = axisX.upperViewBound
        // There are db.length frequency points, including DC component
        val nFreqPointsTotal = dBCache!!.size - 1
        val freqDelta = axisX.upperBound / nFreqPointsTotal
        var beginFreqPt = Math.floor(canvasMinFreq / freqDelta).toInt() // pointer to tmpLineXY
        var endFreqPt = Math.ceil(canvasMaxFreq / freqDelta).toInt() + 1
        val minYCanvas = axisY.pxNoZoomFromValue(AnalyzerGraphicView.MIN_DB)

        // add one more boundary points
        if (beginFreqPt == 0 && axisX.mapType === ScreenPhysicalMapping.Type.LOG) {
            beginFreqPt++
        }
        if (endFreqPt > dBCache!!.size) {
            endFreqPt = dBCache!!.size // just in case canvasMaxFreq / freqDelta > nFreqPointsTotal
        }
        if (tmpLineXY.size != 4 * dBCache!!.size) {
            tmpLineXY = FloatArray(4 * dBCache!!.size)
        }

        // spectrum bar
        if (!isShowLines) {
            c.save()
            // If bars are very close to each other, draw bars as lines
            // Otherwise, zoom in so that lines look like bars.
            if (endFreqPt - beginFreqPt >= axisX.nCanvasPx / 2
                || axisX.mapType !== ScreenPhysicalMapping.Type.LINEAR
            ) {
                matrix.reset()
                matrix.setTranslate(
                    0f,
                    (-axisY.shift * canvasHeight).toFloat()
                )
                matrix.postScale(
                    1f,
                    axisY.zoom.toFloat()
                )
                c.concat(matrix)
                //      float barWidthInPixel = 0.5f * freqDelta / (canvasMaxFreq - canvasMinFreq) * canvasWidth;
                //      if (barWidthInPixel > 2) {
                //        linePaint.setStrokeWidth(barWidthInPixel);
                //      } else {
                //        linePaint.setStrokeWidth(0);
                //      }
                // plot directly to the canvas
                for (i in beginFreqPt until endFreqPt) {
                    val x = axisX.pxFromValue(i * freqDelta).toFloat()
                    val y = axisY.pxNoZoomFromValue(clampDB(dBCache!![i])).toFloat()
                    if (y != canvasHeight.toFloat()) { // ...forgot why
                        tmpLineXY[4 * i] = x
                        tmpLineXY[4 * i + 1] = minYCanvas.toFloat()
                        tmpLineXY[4 * i + 2] = x
                        tmpLineXY[4 * i + 3] = y
                    }
                }
                c.drawLines(
                    tmpLineXY,
                    4 * beginFreqPt,
                    4 * (endFreqPt - beginFreqPt),
                    linePaint
                )
            } else {
                // for zoomed linear scale
                val pixelStep = 2 // each bar occupy this virtual pixel
                matrix.reset()
                val extraPixelAlignOffset = 0.0
                //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//          // There is an shift for Android 4.4, while no shift for Android 2.3
//          // I guess that is relate to GL ES acceleration
//          if (c.isHardwareAccelerated()) {
//            extraPixelAlignOffset = 0.5f;
//          }
//        }
                matrix.setTranslate(
                    (-axisX.shift * nFreqPointsTotal * pixelStep - extraPixelAlignOffset).toFloat(),
                    (-axisY.shift * canvasHeight).toFloat()
                )
                matrix.postScale(
                    (canvasWidth / ((canvasMaxFreq - canvasMinFreq) / freqDelta * pixelStep)).toFloat(),
                    axisY.zoom.toFloat()
                )
                c.concat(matrix)
                // fill interval same as canvas pixel width.
                for (i in beginFreqPt until endFreqPt) {
                    val x = (i * pixelStep).toFloat()
                    val y = axisY.pxNoZoomFromValue(clampDB(dBCache!![i])).toFloat()
                    if (y != canvasHeight.toFloat()) {
                        tmpLineXY[4 * i] = x
                        tmpLineXY[4 * i + 1] = minYCanvas.toFloat()
                        tmpLineXY[4 * i + 2] = x
                        tmpLineXY[4 * i + 3] = y
                    }
                }
                c.drawLines(
                    tmpLineXY,
                    4 * beginFreqPt,
                    4 * (endFreqPt - beginFreqPt),
                    linePaint
                )
            }
            c.restore()
        }

        // spectrum line
        c.save()
        matrix.reset()
        matrix.setTranslate(
            0f,
            (-axisY.shift * canvasHeight).toFloat()
        )
        matrix.postScale(
            1f,
            axisY.zoom.toFloat()
        )
        c.concat(matrix)
        var o_x = axisX.pxFromValue(beginFreqPt * freqDelta).toFloat()
        var o_y = axisY.pxNoZoomFromValue(clampDB(dBCache!![beginFreqPt])).toFloat()
        for (i in beginFreqPt + 1 until endFreqPt) {
            val x = axisX.pxFromValue(i * freqDelta).toFloat()
            val y = axisY.pxNoZoomFromValue(clampDB(dBCache!![i])).toFloat()
            tmpLineXY[4 * i] = o_x
            tmpLineXY[4 * i + 1] = o_y
            tmpLineXY[4 * i + 2] = x
            tmpLineXY[4 * i + 3] = y
            o_x = x
            o_y = y
        }
        c.drawLines(
            tmpLineXY,
            4 * (beginFreqPt + 1),
            4 * (endFreqPt - beginFreqPt - 1),
            linePaintLight
        )
        c.restore()
    }

    // x, y is in pixel unit
    fun setMarker(
        x: Double,
        y: Double
    ) {
        markerFreq = axisX.valueFromPx(x) // frequency
        markerDB = axisY.valueFromPx(y) // decibel
    }

    fun getMarkerFreq(): Double {
        return if (canvasWidth == 0) 0.0 else markerFreq
    }

    fun getMarkerDB(): Double {
        return if (canvasHeight == 0) 0.0 else markerDB
    }

    fun hideMarker() {
        markerFreq = 0.0
        markerDB = 0.0
    }

    private fun drawMarker(c: Canvas) {
        if (markerFreq == 0.0) {
            return
        }
        val cX: Float
        val cY: Float
        cX = axisX.pxFromValue(markerFreq).toFloat()
        cY = axisY.pxFromValue(markerDB).toFloat()
        c.drawLine(
            cX,
            0f,
            cX,
            canvasHeight.toFloat(),
            markerPaint
        )
        c.drawLine(
            0f,
            cY,
            canvasWidth.toFloat(),
            cY,
            markerPaint
        )
    }

    // Plot spectrum with axis and ticks on the whole canvas c
    fun drawSpectrumPlot(
        c: Canvas,
        savedDBSpectrum: DoubleArray?
    ) {
        freqGridLabel.updateGridLabels(
            axisX.lowerViewBound,
            axisX.upperViewBound
        )
        dBGridLabel.updateGridLabels(
            axisY.lowerViewBound,
            axisY.upperViewBound
        )
        drawGridLines(c)
        drawSpectrumOnCanvas(
            c,
            savedDBSpectrum
        )
        drawMarker(c)
        draw(
            c,
            axisX,
            freqGridLabel,
            0f,
            0f,
            0,
            1,
            labelPaint,
            gridPaint,
            gridPaint
        )
        draw(
            c,
            axisY,
            dBGridLabel,
            0f,
            0f,
            1,
            1,
            labelPaint,
            gridPaint,
            gridPaint
        )
    }

    fun setMarkerFreq(markerFreq: Double) {
        this.markerFreq = markerFreq
    }

    fun setMarkerDB(markerDB: Double) {
        this.markerDB = markerDB
    }

    companion object {
        private val TAG = SpectrumPlot::class.java.simpleName
    }

    init {
        dPRatio = _context.resources.displayMetrics.density
        linePaint = Paint()
        linePaint.color = Color.parseColor("#0D2C6D")
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 1f
        linePaintLight = Paint(linePaint)
        linePaintLight.color = Color.parseColor("#3AB3E2")
        gridPaint = Paint()
        gridPaint.color = Color.DKGRAY
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 0.6f * dPRatio
        markerPaint = Paint(gridPaint)
        markerPaint.color = Color.parseColor("#00CD00")
        labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        labelPaint.color = Color.GRAY
        labelPaint.textSize = 14.0f * dPRatio
        labelPaint.typeface = Typeface.MONOSPACE // or Typeface.SANS_SERIF
        markerDB = 0.0
        markerFreq = markerDB
        freqGridLabel = GridLabel(
            GridLabel.Type.FREQ,
            canvasWidth * gridDensity / dPRatio
        )
        dBGridLabel = GridLabel(
            GridLabel.Type.DB,
            canvasHeight * gridDensity / dPRatio
        )
        axisX = ScreenPhysicalMapping(
            0.0,
            0.0,
            0.0,
            ScreenPhysicalMapping.Type.LINEAR
        )
        axisY = ScreenPhysicalMapping(
            0.0,
            0.0,
            0.0,
            ScreenPhysicalMapping.Type.LINEAR
        )
    }
}
