package com.appacoustic.cointester.core.presentation.analyzer.view

import android.graphics.*
import com.appacoustic.cointester.core.framework.ScreenPhysicalMapping
import com.appacoustic.cointester.core.presentation.analyzer.view.AxisTickLabels.draw
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The spectrum plot part of AnalyzerGraphic.
 */
class SpectrumPlot(
    private val density: Float
) {

    var showLines = false
    private val linePaint: Paint = Paint()
    private val linePaintLight: Paint
    private val markerPaint: Paint
    private val gridPaint: Paint = Paint()
    private val labelPaint: Paint
    private var canvasHeight = 0
    private var canvasWidth = 0
    private val frequencyGridLabel: GridLabel
    private val dBGridLabel: GridLabel
    private val gridDensity = 1 / 85.0 // Every 85 pixel one grid line (on average)
    private var markerFrequency: Double
    private var markerDB: Double
    val axisX: ScreenPhysicalMapping
    val axisY: ScreenPhysicalMapping

    init {
        linePaint.color = Color.parseColor("#6a4c93")
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 1f
        linePaintLight = Paint(linePaint)
        linePaintLight.color = Color.parseColor("#FFFFFF")
        linePaintLight.strokeWidth = 2f
        gridPaint.color = Color.DKGRAY
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 0.6f * density
        markerPaint = Paint(gridPaint)
        markerPaint.color = Color.parseColor("#1982c4")
        labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        labelPaint.color = Color.WHITE
        labelPaint.textSize = 14.0f * density
        labelPaint.typeface = Typeface.MONOSPACE
        markerDB = 0.0
        markerFrequency = 0.0
        frequencyGridLabel = GridLabel(
            GridLabel.Type.FREQ,
            canvasWidth * gridDensity / density
        )
        dBGridLabel = GridLabel(
            GridLabel.Type.DB,
            canvasHeight * gridDensity / density
        )
        axisX = ScreenPhysicalMapping(
            nCanvasPx = 0.0,
            lowerBound = 0.0,
            upperBound = 0.0,
            mapType = ScreenPhysicalMapping.Type.LINEAR
        )
        axisY = ScreenPhysicalMapping(
            nCanvasPx = 0.0,
            lowerBound = 0.0,
            upperBound = 0.0,
            mapType = ScreenPhysicalMapping.Type.LINEAR
        )
    }

    fun setCanvas(
        canvasWidth: Int,
        canvasHeight: Int,
        axisBounds: DoubleArray?
    ) {
        this.canvasWidth = canvasWidth
        this.canvasHeight = canvasHeight

        frequencyGridLabel.setDensity(this.canvasWidth * gridDensity / density)
        dBGridLabel.setDensity(this.canvasHeight * gridDensity / density)
        axisX.nCanvasPx = this.canvasWidth.toDouble()
        axisY.nCanvasPx = this.canvasHeight.toDouble()
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

    fun setFrequencyAxisMode(
        mapType: ScreenPhysicalMapping.Type,
        freqLowerBoundForLog: Double,
        gridType: GridLabel.Type?
    ) {
        axisX.setMappingType(
            mapType,
            freqLowerBoundForLog
        )
        frequencyGridLabel.gridType = gridType
    }

    private fun drawGridLines(canvas: Canvas) {
        for (i in frequencyGridLabel.values.indices) {
            val xPos = axisX.pxFromValue(frequencyGridLabel.values[i]).toFloat()
            canvas.drawLine(
                xPos,
                0f,
                xPos,
                canvasHeight.toFloat(),
                gridPaint
            )
        }
        for (i in dBGridLabel.values.indices) {
            val yPos = axisY.pxFromValue(dBGridLabel.values[i]).toFloat()
            canvas.drawLine(
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

    private fun drawSpectrumOnCanvas(
        canvas: Canvas,
        savedDBSpectrum: DoubleArray?
    ) {
        if (canvasHeight < 1 || savedDBSpectrum == null || savedDBSpectrum.isEmpty()) {
            return
        }
        synchronized(savedDBSpectrum) {
            // TODO: need lock on tmpDBSpectrum, but how?
            if (dBCache == null || dBCache!!.size != savedDBSpectrum.size) {
                dBCache = DoubleArray(savedDBSpectrum.size)
            }
            System.arraycopy(
                savedDBSpectrum,
                0,
                dBCache,
                0,
                savedDBSpectrum.size
            )
        }
        val canvasMinFreq = axisX.lowerViewBound
        val canvasMaxFreq = axisX.upperViewBound
        // There are savedDBSpectrum.length frequency points, including DC component
        val nFreqPointsTotal = dBCache!!.size - 1
        val freqDelta = axisX.upperBound / nFreqPointsTotal
        var beginFreqPt = floor(canvasMinFreq / freqDelta).toInt() // pointer to tmpLineXY
        var endFreqPt = ceil(canvasMaxFreq / freqDelta).toInt() + 1
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
        if (!showLines) {
            canvas.save()
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
                canvas.concat(matrix)
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
                canvas.drawLines(
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
                matrix.setTranslate(
                    (-axisX.shift * nFreqPointsTotal * pixelStep - extraPixelAlignOffset).toFloat(),
                    (-axisY.shift * canvasHeight).toFloat()
                )
                matrix.postScale(
                    (canvasWidth / ((canvasMaxFreq - canvasMinFreq) / freqDelta * pixelStep)).toFloat(),
                    axisY.zoom.toFloat()
                )
                canvas.concat(matrix)
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
                canvas.drawLines(
                    tmpLineXY,
                    4 * beginFreqPt,
                    4 * (endFreqPt - beginFreqPt),
                    linePaint
                )
            }
            canvas.restore()
        }

        // spectrum line
        canvas.save()
        matrix.reset()
        matrix.setTranslate(
            0f,
            (-axisY.shift * canvasHeight).toFloat()
        )
        matrix.postScale(
            1f,
            axisY.zoom.toFloat()
        )
        canvas.concat(matrix)
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
        canvas.drawLines(
            tmpLineXY,
            4 * (beginFreqPt + 1),
            4 * (endFreqPt - beginFreqPt - 1),
            linePaintLight
        )
        canvas.restore()
    }

    // x, y is in pixel unit
    fun setMarker(
        x: Double,
        y: Double
    ) {
        markerFrequency = axisX.valueFromPx(x) // frequency
        markerDB = axisY.valueFromPx(y) // decibel
    }

    fun getMarkerFrequency(): Double {
        return if (canvasWidth == 0) 0.0 else markerFrequency
    }

    fun setMarkerFrequency(markerFreq: Double) {
        this.markerFrequency = markerFreq
    }

    fun getMarkerDB(): Double {
        return if (canvasHeight == 0) 0.0 else markerDB
    }

    fun setMarkerDB(markerDB: Double) {
        this.markerDB = markerDB
    }

    fun hideMarker() {
        markerFrequency = 0.0
        markerDB = 0.0
    }

    private fun drawMarker(canvas: Canvas) {
        if (markerFrequency == 0.0) {
            return
        }
        val cX: Float = axisX.pxFromValue(markerFrequency).toFloat()
        val cY: Float = axisY.pxFromValue(markerDB).toFloat()
        canvas.drawLine(
            cX,
            0f,
            cX,
            canvasHeight.toFloat(),
            markerPaint
        )
        canvas.drawLine(
            0f,
            cY,
            canvasWidth.toFloat(),
            cY,
            markerPaint
        )
    }

    fun drawSpectrumPlot(
        canvas: Canvas,
        savedDBSpectrum: DoubleArray?
    ) {
        frequencyGridLabel.updateGridLabels(
            axisX.lowerViewBound,
            axisX.upperViewBound
        )
        dBGridLabel.updateGridLabels(
            axisY.lowerViewBound,
            axisY.upperViewBound
        )
        drawGridLines(canvas)
        drawSpectrumOnCanvas(
            canvas,
            savedDBSpectrum
        )
        drawMarker(canvas)
        draw(
            canvas,
            axisX,
            frequencyGridLabel,
            0f,
            0f,
            0,
            1,
            labelPaint,
            gridPaint,
            gridPaint
        )
        draw(
            canvas,
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
}
