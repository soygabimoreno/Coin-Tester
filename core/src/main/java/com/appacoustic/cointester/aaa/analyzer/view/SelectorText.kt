package com.appacoustic.cointester.aaa.analyzer.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.appcompat.widget.AppCompatTextView
import com.appacoustic.cointester.R
import com.appacoustic.cointester.libFramework.KLog.Companion.w

/**
 * TextView that toggles through a set of values.
 */
class SelectorText : AppCompatTextView {

    companion object {
        private val TAG = SelectorText::class.java.simpleName
        private var DPRatio = 0f
        private const val ANIMATION_DELAY = 70
        private fun getValue(
            a: TypedArray,
            index: Int,
            dflt: String
        ): String {
            val result = a.getString(index)
            return result ?: dflt
        }
    }

    private var value_id = 0
    var values = arrayOfNulls<String>(0)
        private set
    private var valuesDisplay = arrayOfNulls<String>(0)
    private var paint: Paint? = null
    private var bg: Paint? = null
    private val rect = RectF()
    private val bgRect = RectF()
    private var r = 0f

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(
        context,
        attrs,
        defStyle
    ) {
        setup(
            context,
            attrs
        )
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(
        context,
        attrs
    ) {
        setup(
            context,
            attrs
        )
    }

    constructor(context: Context) : super(context) {
        setup(
            context,
            null
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun performClick(): Boolean {
        text = text // fix the no-animation bug
        //setText(valuesDisplay[value_id]);
        val an = createAnimation(
            true,
            ANIMATION_DELAY
        )
        an.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation) {
                nextValue()
                super@SelectorText.performClick()
                createAnimation(
                    false,
                    ANIMATION_DELAY
                ).start()
            }

            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationStart(animation: Animation) {}
        })
        an.start()
        return true
    }

    /**
     * Choose an arbitrary animation for the text view.
     *
     * @param start  If true, animate the old value "out", otherwise animate the old value in
     * @param millis Animation time for this step, ms
     */
    private fun createAnimation(
        start: Boolean,
        millis: Int
    ): Animation {
        val ra = RotateAnimation(
            if (start) 0f else 180f,
            if (start) 180f else 360f,
            (width / 2).toFloat(),
            (height / 2).toFloat()
        )
        ra.duration = millis.toLong()
        animation = ra
        return ra
    }

    /**
     * Compute the value of our "select" indicator.
     */
    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int
    ) {
        rect[2f * DPRatio, h / 2 - 5f * DPRatio, 12f * DPRatio] = h / 2 + 7f * DPRatio
        bgRect[1f * DPRatio, 1f * DPRatio, w - 2f * DPRatio] = h - 1f * DPRatio
    }

    /**
     * Draw the selector, then the selected text.
     */
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawRoundRect(
            rect,
            r,
            r,
            paint!!
        )
        c.drawRoundRect(
            bgRect,
            r,
            r,
            bg!!
        )
    }

    /**
     * Initialize our selector.  We could make most of the features customizable via XML.
     */
    private fun setup(
        context: Context,
        attrs: AttributeSet?
    ) {
        DPRatio = resources.displayMetrics.density
        r = 3 * DPRatio
        bg = Paint()
        bg!!.strokeWidth = 2 * DPRatio
        bg!!.color = Color.GRAY
        bg!!.style = Paint.Style.STROKE
        paint = Paint(bg)
        paint!!.color = Color.GREEN
        isClickable = true
        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.SelectorText
            )
            val items = a.getString(R.styleable.SelectorText_items)
            val delim = getValue(
                a,
                R.styleable.SelectorText_itemsSeparator,
                " "
            )
            val itemsDisplay = a.getString(R.styleable.SelectorText_itemsDisplay)
            if (items != null) {
//        GLog.i(AnalyzerActivity.TAG, "items: " + items);
                if (itemsDisplay != null && itemsDisplay.length > 0) {
                    setValues(
                        items.split(delim).toTypedArray(),
                        itemsDisplay.split("::").toTypedArray()
                    )
                } else {
                    setValues(
                        items.split(delim).toTypedArray(),
                        items.split(delim).toTypedArray()
                    )
                }
            }
            a.recycle()
        }
        if (valuesDisplay.size > 0) {
            text = valuesDisplay[0]
        }
    }

    fun setValues(
        values: Array<String?>,
        valuesDisplay: Array<String?>
    ) {
        this.values = values
        if (values.size == valuesDisplay.size) {
            this.valuesDisplay = valuesDisplay
        } else {
            w("values.length != valuesDisplay.length")
            this.valuesDisplay = values
        }
        adjustWidth()
        invalidate()
    }

    val value: String?
        get() = values[value_id]

    fun setValue(v: String) {
        for (i in values.indices) {
            if (v != values[value_id]) {
                nextValue()
            }
        }
    }

    fun nextValue(): String? {
        if (values.size != 0) {
            value_id++
            if (value_id >= values.size) {
                value_id = 0
            }
            text = valuesDisplay[value_id]
            return valuesDisplay[value_id]
        }
        return text.toString()
    }

    private fun adjustWidth() {
        val p: Paint = getPaint()
        val adj = paddingLeft + paddingRight
        var width = 0
        for (s in valuesDisplay) {
            width = Math.max(
                width,
                Math.round(p.measureText(s))
            )
        }
        minWidth = width + adj + (4 * DPRatio).toInt()
    }
}
