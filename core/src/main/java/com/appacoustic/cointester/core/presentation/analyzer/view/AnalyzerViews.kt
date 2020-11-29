package com.appacoustic.cointester.core.presentation.analyzer.view

import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import android.os.SystemClock
import android.text.Html
import android.text.Spanned
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import com.appacoustic.cointester.core.R
import com.appacoustic.cointester.core.presentation.analyzer.domain.AnalyzerParams

/**
 * Operate the views in the UI here.
 * Should run on UI thread in general.
 */
class AnalyzerViews(
    private val activity: Activity,
    private val agv: AnalyzerGraphicView
) {

    companion object {
        fun fromHtml(source: String?): Spanned {
            return Html.fromHtml(
                source,
                Html.FROM_HTML_MODE_LEGACY
            )
        }

        private const val VIEW_MASK_graphView = 1 shl 0
        private const val VIEW_MASK_textview_RMS = 1 shl 1
        private const val VIEW_MASK_textview_peak = 1 shl 2
        private const val VIEW_MASK_MarkerLabel = 1 shl 3
        private const val VIEW_MASK_RecTimeLable = 1 shl 4
    }

    private val dpRatio: Float = activity.resources.displayMetrics.density
    private var fpsLimit = 8.0
    private val sbRMS = StringBuilder("")
    private val sbMarker = StringBuilder("")
    private val sbPeak = StringBuilder("")
    private val sbRec = StringBuilder("")
    private val charRMS: CharArray = CharArray(activity.resources.getString(R.string.tv_rms_text).length)
    private val charMarker: CharArray = CharArray(activity.resources.getString(R.string.tv_marker_text).length)
    private val charPeak: CharArray = CharArray(activity.resources.getString(R.string.tv_peak_text).length)
    private val charRec: CharArray = CharArray(activity.resources.getString(R.string.tv_rec_text).length)
    var isWarnOverrun = true

    // Prepare the spectrum and spectrogram plot (from scratch or full reset)
    // Should be called before samplingThread starts.
    fun setupView(params: AnalyzerParams?) {
        agv.setupPlot(params)
    }

    // Will be called by SamplingLoopThread (in another thread)
    fun update(spectrumDBcopy: DoubleArray?) {
        agv.saveSpectrum(spectrumDBcopy)
        activity.runOnUiThread { // data will get out of synchronize here
            invalidateGraphView()
        }
    }

    private var wavSecOld = 0.0 // used to reduce frame rate
    fun updateRec(wavSec: Double) {
        if (wavSecOld > wavSec) {
            wavSecOld = wavSec
        }
        if (wavSec - wavSecOld < 0.1) {
            return
        }
        wavSecOld = wavSec
        activity.runOnUiThread { // data will get out of synchronize here
            invalidateGraphView(VIEW_MASK_RecTimeLable)
        }
    }

    fun notifyWAVSaved(path: String) {
        val s = String.format(
            activity.getString(R.string.audio_saved_to_x),
            "'$path'"
        )
        notifyToast(s)
    }

    fun notifyToast(s: String?) {
        activity.runOnUiThread {
            val context = activity.applicationContext
            val toast = Toast.makeText(
                context,
                s,
                Toast.LENGTH_SHORT
            )
            toast.show()
        }
    }

    fun notifyToast(@StringRes resId: Int) {
        activity.runOnUiThread {
            val context = activity.applicationContext
            val toast = Toast.makeText(
                context,
                resId,
                Toast.LENGTH_SHORT
            )
            toast.show()
        }
    }

    private var lastTimeNotifyOverrun: Long = 0
    fun notifyOverrun() {
        if (!isWarnOverrun) {
            return
        }
        val time = SystemClock.uptimeMillis()
        if (time - lastTimeNotifyOverrun > 6000) {
            lastTimeNotifyOverrun = time
            activity.runOnUiThread {
                val context = activity.applicationContext
                val toast = Toast.makeText(
                    context,
                    R.string.error_recorder_buffer_overrun,
                    Toast.LENGTH_LONG
                )
                toast.show()
            }
        }
    }

    fun showPermissionExplanation(resId: Int) {
        val tv = TextView(activity)
        tv.movementMethod = ScrollingMovementMethod()
        tv.text = fromHtml(activity.getString(resId))
        AlertDialog.Builder(activity)
            .setTitle(R.string.permission_explanation_title)
            .setView(tv)
            .setNegativeButton(
                R.string.close,
                null
            )
            .create().show()
    }

    private var timeToUpdate = SystemClock.uptimeMillis()

    @Volatile
    private var isInvalidating = false

    // Invalidate analyzerGraphic in a limited frame rate
    fun invalidateGraphView() {
        invalidateGraphView(-1)
    }

    private fun invalidateGraphView(viewMask: Int) {
        if (isInvalidating) {
            return
        }
        isInvalidating = true
        val frameTime: Long // time delay for next frame
        frameTime = if (agv.showMode != AnalyzerGraphicView.PlotMode.SPECTRUM) {
            (1000 / fpsLimit).toLong() // use a much lower frame rate for spectrogram
        } else {
            1000 / 60.toLong()
        }
        val t = SystemClock.uptimeMillis()
        if (t >= timeToUpdate) {    // limit frame rate
            timeToUpdate += frameTime
            if (timeToUpdate < t) {            // catch up current time
                timeToUpdate = t + frameTime
            }
            idPaddingInvalidate = false
            // Take care of synchronization of analyzerGraphic.spectrogramColors and iTimePointer,
            // and then just do invalidate() here.
            if (viewMask and VIEW_MASK_graphView != 0) agv.invalidate()
        } else {
            if (!idPaddingInvalidate) {
                idPaddingInvalidate = true
                paddingViewMask = viewMask
                paddingInvalidateHandler.postDelayed(
                    paddingInvalidateRunnable,
                    timeToUpdate - t + 1
                )
            } else {
                paddingViewMask = paddingViewMask or viewMask
            }
        }
        isInvalidating = false
    }

    fun setFpsLimit(_fpsLimit: Double) {
        fpsLimit = _fpsLimit
    }

    @Volatile
    private var idPaddingInvalidate = false

    @Volatile
    private var paddingViewMask = -1
    private val paddingInvalidateHandler = Handler()

    // Am I need to use runOnUiThread() ?
    private val paddingInvalidateRunnable = Runnable {
        if (idPaddingInvalidate) {
            // It is possible that t-timeToUpdate <= 0 here, don't know why
            invalidateGraphView(paddingViewMask)
        }
    }
}
