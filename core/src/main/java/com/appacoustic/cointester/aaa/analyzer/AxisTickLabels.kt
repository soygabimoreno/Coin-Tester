package com.appacoustic.cointester.aaa.analyzer

import android.graphics.Canvas
import android.graphics.Paint

/**
 * Axis Tick labels.
 */
object AxisTickLabels {
    private val axisNames = arrayOf(
        "Hz",
        "dB",
        "Sec",
        "Hz",
        "Pitch"
    ) // See GridLabel.Type

    // Draw ticks and labels for a axisMap.
    // labelPaint should use fixed width font
    @JvmStatic
    fun draw(
        c: Canvas,
        axisMap: ScreenPhysicalMapping,
        gridLabel: GridLabel,
        axisBeginX: Float,
        axisBeginY: Float,
        directionI: Int,
        labelSide: Int,
        labelPaint: Paint,
        gridPaint: Paint,
        rulerBrightPaint: Paint?
    ) {
        val axisName = axisNames[gridLabel.gridType.value]
        val textHeight = labelPaint.getFontMetrics(null)
        val labelLargeLen = 0.7f * textHeight
        val labelSmallLen = 0.6f * labelLargeLen

        // directionI: 0:+x, 1:+y, 2:-x, 3:-y
        // labelSide:  1: label at positive side of axis, -1: otherwise
        val drawOnXAxis = directionI % 2 == 0
        val directionSign = if (directionI <= 1) 1 else -1

        // Plot axis marks
        var posAlongAxis: Float
        for (k in 0..1) {
            val labelLen = (if (k == 0) labelSmallLen else labelLargeLen) * labelSide
            val tickPainter = if (k == 0) gridPaint else rulerBrightPaint!!
            val values = if (k == 0) gridLabel.ticks else gridLabel.values
            for (i in values.indices) {
                posAlongAxis = axisMap.pxFromValue(values[i]).toFloat() * directionSign
                if (drawOnXAxis) {
                    c.drawLine(
                        axisBeginX + posAlongAxis,
                        axisBeginY,
                        axisBeginX + posAlongAxis,
                        axisBeginY + labelLen,
                        tickPainter
                    )
                } else {
                    c.drawLine(
                        axisBeginX,
                        axisBeginY + posAlongAxis,
                        axisBeginX + labelLen,
                        axisBeginY + posAlongAxis,
                        tickPainter
                    )
                }
            }
        }
        // Straight line
        if (drawOnXAxis) {
            c.drawLine(
                axisBeginX,
                axisBeginY,
                axisBeginX + axisMap.nCanvasPx.toFloat() * (1 - directionI),
                axisBeginY,
                labelPaint
            )
        } else {
            c.drawLine(
                axisBeginX,
                axisBeginY,
                axisBeginX,
                axisBeginY + axisMap.nCanvasPx.toFloat() * (2 - directionI),
                labelPaint
            )
        }

        // Plot labels
        val widthDigit = labelPaint.measureText("0")
        val widthAxisName = widthDigit * axisName.length
        val widthAxisNameExt = widthAxisName + .5f * widthDigit // with a safe boundary

        // For drawOnXAxis == true
        val axisNamePosX = if (directionSign == 1) -widthAxisNameExt + axisMap.nCanvasPx.toFloat() else -widthAxisNameExt
        // For drawOnXAxis == false
        // always show y-axis name at the smaller (in pixel) position.
        var axisNamePosY = if (directionSign == 1) textHeight else textHeight - axisMap.nCanvasPx.toFloat()
        if (gridLabel.gridType == GridLabel.Type.DB) {
            // For dB axis, show axis name far from 0dB (directionSign==1)
            axisNamePosY = axisMap.nCanvasPx.toFloat() - 0.8f * widthDigit
        }
        val labelPosY = axisBeginY + if (labelSide == 1) 0.1f * labelLargeLen + textHeight else -0.3f * labelLargeLen
        var labelPosX: Float
        var notShowNextLabel = 0
        for (i in gridLabel.strings.indices) {
            posAlongAxis = axisMap.pxFromValue(gridLabel.values[i]).toFloat() * directionSign
            val thisDigitWidth = if (drawOnXAxis) widthDigit * gridLabel.strings[i].length + 0.3f * widthDigit else -textHeight
            val axisNamePos = if (drawOnXAxis) axisNamePosX else axisNamePosY
            val axisNameLen = if (drawOnXAxis) widthAxisNameExt else -textHeight

            // Avoid label overlap:
            // (1) No overlap to axis name like "Hz";
            // (2) If no (1), no overlap to important label 1, 10, 100, 1000, 10000, 1k, 10k;
            // (3) If no (1) and (2), no overlap to previous label.
            if (isIntvOverlap(
                    posAlongAxis,
                    thisDigitWidth,
                    axisNamePos,
                    axisNameLen
                )
            ) {
                continue  // case (1)
            }
            if (notShowNextLabel > 0) {
                notShowNextLabel--
                continue  // case (3)
            }
            var j = i + 1
            while (j < gridLabel.strings.size) {
                val nextDigitPos = axisMap.pxFromValue(gridLabel.values[j]).toFloat() * directionSign
                val nextDigitWidth = if (drawOnXAxis) widthDigit * gridLabel.strings[j].length + 0.3f * widthDigit else -textHeight
                if (!isIntvOverlap(
                        posAlongAxis,
                        thisDigitWidth,
                        nextDigitPos,
                        nextDigitWidth
                    )
                ) {
                    break // no overlap of case (3)
                }
                notShowNextLabel++
                if (gridLabel.isImportantLabel(j)) {
                    // do not show label i (case (2))
                    // but also check case (1) for label j
                    if (!isIntvOverlap(
                            nextDigitPos,
                            nextDigitWidth,
                            axisNamePos,
                            axisNameLen
                        )
                    ) {
                        notShowNextLabel = -1
                        break
                    }
                }
                j++
            }
            if (notShowNextLabel == -1) {
                notShowNextLabel = j - i - 1 // show the label in case (2)
                continue
            }

            // Now safe to draw label
            if (drawOnXAxis) {
                c.drawText(
                    gridLabel.chars[i],
                    0,
                    gridLabel.strings[i].length,
                    axisBeginX + posAlongAxis,
                    labelPosY,
                    labelPaint
                )
            } else {
                labelPosX = if (labelSide == -1) axisBeginX - 0.5f * labelLargeLen - widthDigit * gridLabel.strings[i].length else axisBeginX + 0.5f * labelLargeLen
                c.drawText(
                    gridLabel.chars[i],
                    0,
                    gridLabel.strings[i].length,
                    labelPosX,
                    axisBeginY + posAlongAxis,
                    labelPaint
                )
            }
        }
        if (drawOnXAxis) {
            c.drawText(
                axisName,
                axisBeginX + axisNamePosX,
                labelPosY,
                labelPaint
            )
        } else {
            labelPosX = if (labelSide == -1) axisBeginX - 0.5f * labelLargeLen - widthAxisName else axisBeginX + 0.5f * labelLargeLen
            c.drawText(
                axisName,
                labelPosX,
                axisBeginY + axisNamePosY,
                labelPaint
            )
        }
    }

    // Return true if two intervals [pos1, pos1+len1] [pos2, pos2+len2] overlaps.
    private fun isIntvOverlap(
        pos1: Float,
        len1: Float,
        pos2: Float,
        len2: Float
    ): Boolean {
        var pos1 = pos1
        var len1 = len1
        var pos2 = pos2
        var len2 = len2
        if (len1 < 0) {
            pos1 -= len1
            len1 = -len1
        }
        if (len2 < 0) {
            pos2 -= len2
            len2 = -len2
        }
        return pos1 <= pos2 && pos2 <= pos1 + len1 || pos2 <= pos1 && pos1 <= pos2 + len2
    }
}
