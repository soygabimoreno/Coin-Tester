package com.appacoustic.cointester.presentation.analyzer.view

import android.content.Context
import android.graphics.*
import com.appacoustic.cointester.framework.ScreenPhysicalMapping
import com.appacoustic.cointester.libFramework.KLog.Companion.i
import com.appacoustic.cointester.presentation.analyzer.domain.AnalyzerParams
import com.appacoustic.cointester.presentation.analyzer.view.AxisTickLabels.draw

/**
 * The spectrogram plot part of AnalyzerGraphic.
 */
class SpectrogramPlot(_context: Context) {
    private var showFreqAlongX = false
    val spectrogramBMP = SpectrogramBMP()

    enum class TimeAxisMode(  // 0: moving (shifting) spectrogram, 1: overwriting in loop
        val value: Int
    ) {
        // java's enum type is inconvenient
        SHIFT(0), OVERWRITE(1);
    }

    var spectrogramMode = TimeAxisMode.OVERWRITE
        private set
    private var showTimeAxis = true
    private var timeWatch = 4.0 // TODO: a bit duplicated, use axisTime

    @Volatile
    private var timeMultiplier = 1 // should be accorded with nFFTAverage in AnalyzerActivity
    var nFreqPoints // TODO: a bit duplicated, use BMP.nFreq
        = 0
    var nTimePoints // TODO: a bit duplicated, use BMP.nTime
        = 0
    private var timeInc = 0.0
    private val matrixSpectrogram = Matrix()
    private var smoothBmpPaint: Paint? = null
    private val backgroundPaint: Paint
    private val markerPaint: Paint
    private val gridPaint: Paint
    private val rulerBrightPaint: Paint
    private val labelPaint: Paint
    private val markerTimePaint: Paint
    var axisFreq: ScreenPhysicalMapping
    var axisTime: ScreenPhysicalMapping
    private val fqGridLabel: GridLabel
    private val tmGridLabel: GridLabel
    private var markerFreq: Double
    private val density: Float
    private val gridDensity = 1 / 85f // every 85 pixel one grid line, on average
    private var canvasHeight = 0
    private var canvasWidth = 0
    private var labelBeginX = 0f
    private var labelBeginY = 0f

    // Before calling this, axes should be initialized.
    fun setupSpectrogram(params: AnalyzerParams) {
        val sampleRate = params.sampleRate
        val fftLen = params.fftLength
        val hopLen = params.hopLength
        val nAve = params.nFftAverage
        val timeDurationE = params.spectrogramDuration
        timeWatch = timeDurationE
        timeMultiplier = nAve
        timeInc = hopLen.toDouble() / sampleRate // time of each slice
        nFreqPoints = fftLen / 2 // no direct current term
        nTimePoints = Math.ceil(timeWatch / timeInc).toInt()
        spectrogramBMP.init(
            nFreqPoints,
            nTimePoints,
            axisFreq
        )
        i(
            """sampleRate    = $sampleRate
  fFTLength        = $fftLen
  timeDurationE = $timeDurationE * $nAve  ($nTimePoints points)
  canvas size freq= ${axisFreq.nCanvasPx} time=${axisTime.nCanvasPx}"""
        )
    }

    fun setCanvas(
        _canvasWidth: Int,
        _canvasHeight: Int,
        axisBounds: DoubleArray?
    ) {
        canvasWidth = _canvasWidth
        canvasHeight = _canvasHeight
        if (canvasHeight > 1 && canvasWidth > 1) {
            updateDrawingWindowSize()
        }
        if (axisBounds != null) {
            if (showFreqAlongX) {
                axisFreq.setBounds(
                    axisBounds[0],
                    axisBounds[2]
                )
                axisTime.setBounds(
                    axisBounds[1],
                    axisBounds[3]
                )
            } else {
                axisTime.setBounds(
                    axisBounds[0],
                    axisBounds[2]
                )
                axisFreq.setBounds(
                    axisBounds[1],
                    axisBounds[3]
                )
            }
            if (spectrogramMode == TimeAxisMode.SHIFT) {
                val b1 = axisTime.lowerBound
                val b2 = axisTime.upperBound
                axisTime.setBounds(
                    b2,
                    b1
                )
            }
        }
        fqGridLabel.setDensity(axisFreq.nCanvasPx * gridDensity / density)
        tmGridLabel.setDensity(axisTime.nCanvasPx * gridDensity / density)
        spectrogramBMP.updateAxis(axisFreq)
    }

    fun setZooms(
        xZoom: Double,
        xShift: Double,
        yZoom: Double,
        yShift: Double
    ) {
        //KLog.Companion.i("setZooms():  xZoom=" + xZoom + "  xShift=" + xShift + "  yZoom=" + yZoom + "  yShift=" + yShift);
        if (showFreqAlongX) {
            axisFreq.setZoomShift(
                xZoom,
                xShift
            )
            axisTime.setZoomShift(
                yZoom,
                yShift
            )
        } else {
            axisFreq.setZoomShift(
                yZoom,
                yShift
            )
            axisTime.setZoomShift(
                xZoom,
                xShift
            )
        }
        spectrogramBMP.updateZoom()
    }

    // Linear or Logarithmic frequency axis
    fun setFreqAxisMode(
        mapType: ScreenPhysicalMapping.Type?,
        freq_lower_bound_for_log: Double,
        gridType: GridLabel.Type?
    ) {
        axisFreq.setMappingType(
            mapType!!,
            freq_lower_bound_for_log
        )
        fqGridLabel.setGridType(gridType)
        spectrogramBMP.updateAxis(axisFreq)
    }

    fun setColorMap(colorMapName: String?) {
        spectrogramBMP.setColorMap(colorMapName)
    }

    fun setSpectrogramDBLowerBound(b: Double) {
        spectrogramBMP.setdBLowerBound(b)
    }

    fun setMarker(
        x: Double,
        y: Double
    ) {
        markerFreq = if (showFreqAlongX) {
            //markerFreq = axisBounds.width() * (xShift + (x-labelBeginX)/(canvasWidth-labelBeginX)/xZoom);  // frequency
            axisFreq.valueFromPx(x - labelBeginX)
        } else {
            //markerFreq = axisBounds.width() * (1 - yShift - y/labelBeginY/yZoom);  // frequency
            axisFreq.valueFromPx(y)
        }
        if (markerFreq < 0) {
            markerFreq = 0.0
        }
    }

    fun getMarkerFreq(): Double {
        return if (canvasWidth == 0) 0.0 else markerFreq
    }

    fun hideMarker() {
        markerFreq = 0.0
    }

    fun setTimeMultiplier(nAve: Int) {
        timeMultiplier = nAve
        if (spectrogramMode == TimeAxisMode.SHIFT) {
            axisTime.lowerBound = timeWatch * timeMultiplier
        } else {
            axisTime.upperBound = timeWatch * timeMultiplier
        }
        // keep zoom shift
        axisTime.setZoomShift(
            axisTime.zoom,
            axisTime.shift
        )
    }

    fun setShowTimeAxis(showTimeAxis: Boolean) {
        this.showTimeAxis = showTimeAxis
    }

    fun setSpectrogramModeShifting(b: Boolean) {
        if (spectrogramMode == TimeAxisMode.SHIFT != b) {
            // mode change, swap time bounds.
            val b1 = axisTime.lowerBound
            val b2 = axisTime.upperBound
            axisTime.setBounds(
                b2,
                b1
            )
        }
        if (b) {
            spectrogramMode = TimeAxisMode.SHIFT
            setPause(isPaused) // update time estimation
        } else {
            spectrogramMode = TimeAxisMode.OVERWRITE
        }
    }

    fun setShowFreqAlongX(b: Boolean) {
        if (showFreqAlongX != b) {
            // Set (swap) canvas size
            val t = axisFreq.nCanvasPx
            axisFreq.nCanvasPx = axisTime.nCanvasPx
            axisTime.nCanvasPx = t
            // swap bounds of freq axis
            axisFreq.reverseBounds()
            fqGridLabel.setDensity(axisFreq.nCanvasPx * gridDensity / density)
            tmGridLabel.setDensity(axisTime.nCanvasPx * gridDensity / density)
        }
        showFreqAlongX = b
    }

    fun setSmoothRender(b: Boolean) {
        smoothBmpPaint = if (b) {
            Paint(Paint.FILTER_BITMAP_FLAG)
        } else {
            null
        }
    }

    private var timeLastSample = 0.0
    private var updateTimeDiff = false
    fun prepare() {
        if (spectrogramMode == TimeAxisMode.SHIFT) setPause(isPaused)
    }

    fun setPause(p: Boolean) {
        if (!p) {
            timeLastSample = System.currentTimeMillis() / 1000.0
        }
        isPaused = p
    }

    // Will be called in another thread (SamplingLoopThread)
    // db.length == 2^n + 1
    fun saveRowSpectrumAsColor(db: DoubleArray?) {
        // For time compensate in shifting mode
        val tNow = System.currentTimeMillis() / 1000.0
        updateTimeDiff = true
        if (Math.abs(timeLastSample - tNow) > 0.5) {
            timeLastSample = tNow
        } else {
            timeLastSample += timeInc * timeMultiplier
            timeLastSample += (tNow - timeLastSample) * 1e-2 // track current time
        }
        spectrogramBMP.fill(db)
    }

    fun getLabelBeginY(): Float {
        val textHeigh = labelPaint.getFontMetrics(null)
        val labelLargeLen = 0.5f * textHeigh
        return if (!showFreqAlongX && !showTimeAxis) {
            canvasHeight.toFloat()
        } else {
            canvasHeight - 0.6f * labelLargeLen - textHeigh
        }
    }

    // Left margin for ruler
    fun getLabelBeginX(): Float {
        val textHeight = labelPaint.getFontMetrics(null)
        val labelLaegeLen = 0.5f * textHeight
        return if (showFreqAlongX) {
            if (showTimeAxis) {
                var j = 3
                for (i in tmGridLabel.strings.indices) {
                    if (j < tmGridLabel.strings[i].length) {
                        j = tmGridLabel.strings[i].length
                    }
                }
                0.6f * labelLaegeLen + j * 0.5f * textHeight
            } else {
                0f
            }
        } else {
            0.6f * labelLaegeLen + 2.5f * textHeight
        }
    }

    private var labelBeginXOld = 0f
    private var labelBeginYOld = 0f
    private fun updateDrawingWindowSize() {
        labelBeginX = getLabelBeginX() // this seems will make the scaling gesture inaccurate
        labelBeginY = getLabelBeginY()
        if (labelBeginX != labelBeginXOld || labelBeginY != labelBeginYOld) {
            if (showFreqAlongX) {
                axisFreq.nCanvasPx = (canvasWidth - labelBeginX).toDouble()
                axisTime.nCanvasPx = labelBeginY.toDouble()
            } else {
                axisTime.nCanvasPx = (canvasWidth - labelBeginX).toDouble()
                axisFreq.nCanvasPx = labelBeginY.toDouble()
            }
            labelBeginXOld = labelBeginX
            labelBeginYOld = labelBeginY
        }
    }

    private fun drawFreqMarker(c: Canvas) {
        if (markerFreq == 0.0) return
        val cX: Float
        val cY: Float
        // Show only the frequency marker
        if (showFreqAlongX) {
            cX = axisFreq.pxFromValue(markerFreq).toFloat() + labelBeginX
            c.drawLine(
                cX,
                0f,
                cX,
                labelBeginY,
                markerPaint
            )
        } else {
            cY = axisFreq.pxFromValue(markerFreq).toFloat()
            c.drawLine(
                labelBeginX,
                cY,
                canvasWidth.toFloat(),
                cY,
                markerPaint
            )
        }
    }

    // Draw time axis for spectrogram
    // Working in the original canvas frame
    private fun drawTimeAxis(
        c: Canvas,
        labelBeginX: Float,
        labelBeginY: Float,
        drawOnXAxis: Boolean
    ) {
        if (drawOnXAxis) {
            draw(
                c,
                axisTime,
                tmGridLabel,
                labelBeginX,
                labelBeginY,
                0,
                1,
                labelPaint,
                gridPaint,
                rulerBrightPaint
            )
        } else {
            draw(
                c,
                axisTime,
                tmGridLabel,
                labelBeginX,
                0f,
                1,
                -1,
                labelPaint,
                gridPaint,
                rulerBrightPaint
            )
        }
    }

    // Draw frequency axis for spectrogram
    // Working in the original canvas frame
    private fun drawFreqAxis(
        c: Canvas,
        labelBeginX: Float,
        labelBeginY: Float,
        drawOnXAxis: Boolean
    ) {
        if (drawOnXAxis) {
            draw(
                c,
                axisFreq,
                fqGridLabel,
                labelBeginX,
                labelBeginY,
                0,
                1,
                labelPaint,
                gridPaint,
                rulerBrightPaint
            )
        } else {
            draw(
                c,
                axisFreq,
                fqGridLabel,
                labelBeginX,
                0f,
                1,
                -1,
                labelPaint,
                gridPaint,
                rulerBrightPaint
            )
        }
    }

    private var pixelTimeCompensate = 0.0

    @Volatile
    private var isPaused = false

    // Plot spectrogram with axis and ticks on the whole canvas c
    fun drawSpectrogramPlot(c: Canvas) {
        if (canvasWidth == 0 || canvasHeight == 0) {
            return
        }
        updateDrawingWindowSize()
        fqGridLabel.setDensity(axisFreq.nCanvasPx * gridDensity / density)
        tmGridLabel.setDensity(axisTime.nCanvasPx * gridDensity / density)
        fqGridLabel.updateGridLabels(
            axisFreq.lowerViewBound,
            axisFreq.upperViewBound
        )
        tmGridLabel.updateGridLabels(
            axisTime.lowerViewBound,
            axisTime.upperViewBound
        )

        // show Spectrogram
        val halfFreqResolutionShift: Double // move the color patch to match the center frequency
        halfFreqResolutionShift = if (axisFreq.mapType === ScreenPhysicalMapping.Type.LINEAR) {
            axisFreq.zoom * axisFreq.nCanvasPx / nFreqPoints / 2
        } else {
            0.0 // the correction is included in log axis render algo.
        }
        matrixSpectrogram.reset()
        if (showFreqAlongX) {
            // when xZoom== 1: nFreqPoints -> canvasWidth; 0 -> labelBeginX
            matrixSpectrogram.postScale(
                (axisFreq.zoom * axisFreq.nCanvasPx / nFreqPoints).toFloat(),
                (axisTime.zoom * axisTime.nCanvasPx / nTimePoints).toFloat()
            )
            matrixSpectrogram.postTranslate(
                (labelBeginX - axisFreq.shift * axisFreq.zoom * axisFreq.nCanvasPx + halfFreqResolutionShift).toFloat(),
                (-axisTime.shift * axisTime.zoom * axisTime.nCanvasPx).toFloat()
            )
        } else {
            // postRotate() will make c.drawBitmap about 20% slower, don't know why
            matrixSpectrogram.postRotate(-90f)
            matrixSpectrogram.postScale(
                (axisTime.zoom * axisTime.nCanvasPx / nTimePoints).toFloat(),
                (axisFreq.zoom * axisFreq.nCanvasPx / nFreqPoints).toFloat()
            )
            // (1-yShift) is relative position of shift (after rotation)
            // yZoom*labelBeginY is canvas length in frequency direction in pixel unit
            matrixSpectrogram.postTranslate(
                (labelBeginX - axisTime.shift * axisTime.zoom * axisTime.nCanvasPx).toFloat(),
                ((1 - axisFreq.shift) * axisFreq.zoom * axisFreq.nCanvasPx - halfFreqResolutionShift).toFloat()
            )
        }
        c.save()
        c.concat(matrixSpectrogram)

        // Time compensate to make it smoother shifting.
        // But if user pressed pause, stop compensate.
        if (!isPaused && updateTimeDiff) {
            val timeCurrent = System.currentTimeMillis() / 1000.0
            pixelTimeCompensate = (timeLastSample - timeCurrent) / (timeInc * timeMultiplier * nTimePoints) * nTimePoints
            updateTimeDiff = false
            //            KLog.Companion.i(" time diff = " + (timeLastSample - timeCurrent));
        }
        if (spectrogramMode == TimeAxisMode.SHIFT) {
            c.translate(
                0.0f,
                pixelTimeCompensate.toFloat()
            )
        }
        if (axisFreq.mapType === ScreenPhysicalMapping.Type.LOG &&
            spectrogramBMP.logAxisMode == SpectrogramBMP.LogAxisPlotMode.REPLOT
        ) {
            // Revert the effect of axisFreq.getZoom() axisFreq.getShift() for the mode REPLOT
            c.scale(
                (1 / axisFreq.zoom).toFloat(),
                1f
            )
            if (showFreqAlongX) {
                c.translate(
                    (nFreqPoints * axisFreq.shift * axisFreq.zoom).toFloat(),
                    0.0f
                )
            } else {
                c.translate(
                    (nFreqPoints * (1f - axisFreq.shift - 1f / axisFreq.zoom) * axisFreq.zoom).toFloat(),
                    0.0f
                )
            }
        }
        spectrogramBMP.draw(
            c,
            axisFreq.mapType,
            spectrogramMode,
            smoothBmpPaint,
            markerTimePaint
        )
        c.restore()
        drawFreqMarker(c)
        if (showFreqAlongX) {
            c.drawRect(
                0f,
                labelBeginY,
                canvasWidth.toFloat(),
                canvasHeight.toFloat(),
                backgroundPaint
            )
            drawFreqAxis(
                c,
                labelBeginX,
                labelBeginY,
                showFreqAlongX
            )
            if (labelBeginX > 0) {
                c.drawRect(
                    0f,
                    0f,
                    labelBeginX,
                    labelBeginY,
                    backgroundPaint
                )
                drawTimeAxis(
                    c,
                    labelBeginX,
                    labelBeginY,
                    !showFreqAlongX
                )
            }
        } else {
            c.drawRect(
                0f,
                0f,
                labelBeginX,
                labelBeginY,
                backgroundPaint
            )
            drawFreqAxis(
                c,
                labelBeginX,
                labelBeginY,
                showFreqAlongX
            )
            if (labelBeginY != canvasHeight.toFloat()) {
                c.drawRect(
                    0f,
                    labelBeginY,
                    canvasWidth.toFloat(),
                    canvasHeight.toFloat(),
                    backgroundPaint
                )
                drawTimeAxis(
                    c,
                    labelBeginX,
                    labelBeginY,
                    !showFreqAlongX
                )
            }
        }
    }

    fun isShowFreqAlongX(): Boolean {
        return showFreqAlongX
    }

    fun setMarkerFreq(markerFreq: Double) {
        this.markerFreq = markerFreq
    }

    fun setLabelBeginX(labelBeginX: Float) {
        this.labelBeginX = labelBeginX
    }

    fun setLabelBeginY(labelBeginY: Float) {
        this.labelBeginY = labelBeginY
    }

    companion object {
        private val TAG = SpectrogramPlot::class.java.simpleName
    }

    init {
        density = _context.resources.displayMetrics.density
        gridPaint = Paint()
        gridPaint.color = Color.DKGRAY
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 0.6f * density
        markerPaint = Paint(gridPaint)
        markerPaint.color = Color.parseColor("#00CD00")
        markerTimePaint = Paint(markerPaint)
        markerTimePaint.style = Paint.Style.STROKE
        markerTimePaint.strokeWidth = 0f
        rulerBrightPaint = Paint()
        rulerBrightPaint.color = Color.rgb(
            99,
            99,
            99
        ) // 99: between Color.DKGRAY and Color.GRAY
        rulerBrightPaint.style = Paint.Style.STROKE
        rulerBrightPaint.strokeWidth = 1f
        labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        labelPaint.color = Color.GRAY
        labelPaint.textSize = 14.0f * density
        labelPaint.typeface = Typeface.MONOSPACE // or Typeface.SANS_SERIF
        backgroundPaint = Paint()
        backgroundPaint.color = Color.BLACK
        markerFreq = 0.0
        fqGridLabel = GridLabel(
            GridLabel.Type.FREQ,
            (canvasWidth * gridDensity / density).toDouble()
        )
        tmGridLabel = GridLabel(
            GridLabel.Type.TIME,
            (canvasHeight * gridDensity / density).toDouble()
        )
        axisFreq = ScreenPhysicalMapping(
            0.0,
            0.0,
            0.0,
            ScreenPhysicalMapping.Type.LINEAR
        )
        axisTime = ScreenPhysicalMapping(
            0.0,
            0.0,
            0.0,
            ScreenPhysicalMapping.Type.LINEAR
        )
    }
}
