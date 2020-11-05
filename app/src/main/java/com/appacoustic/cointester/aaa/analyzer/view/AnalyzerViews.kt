package com.appacoustic.cointester.aaa.analyzer.view

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.text.Html
import android.text.Spanned
import android.text.method.ScrollingMovementMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatCheckBox
import com.appacoustic.cointester.R
import com.appacoustic.cointester.aaa.analyzer.AnalyzerUtil
import com.appacoustic.cointester.aaa.analyzer.SBNumFormat
import com.appacoustic.cointester.aaa.analyzer.model.AnalyzerParams
import com.appacoustic.cointester.presentation.AnalyzerFragment
import com.appacoustic.cointester.presentation.MainActivity
import kotlinx.android.synthetic.main.fragment_analyzer.*

/**
 * Operate the views in the UI here.
 * Should run on UI thread in general.
 */
class AnalyzerViews(
    activity: Activity,
    analyzerFragment: AnalyzerFragment,
    rootView: View
) {

    val agvAnalyzer = activity.agvAnalyzer

    private val activity: Activity
    private val analyzerFragment: AnalyzerFragment
    private val rootView: View
    private val dpRatio: Float
    private var fpsLimit = 8.0
    private val sbRMS = StringBuilder("")
    private val sbMarker = StringBuilder("")
    private val sbPeak = StringBuilder("")
    private val sbRec = StringBuilder("")
    private val charRMS: CharArray
    private val charMarker: CharArray
    private val charPeak: CharArray
    private val charRec: CharArray
    var popupMenuSampleRate: PopupWindow
    var popupMenuFFTLen: PopupWindow
    var popupMenuFFTAverage: PopupWindow
    var isWarnOverrun = true

    // Set text font size of textview_marker and tvAnalyzerPeak
    // according to space left
    //@SuppressWarnings("deprecation")
    private fun setTextViewFontSize() {
        // At this point tv.getWidth(), tv.getLineCount() will return 0
        val display = activity.windowManager.defaultDisplay
        // pixels left
        val px = display.width - activity.resources.getDimension(R.dimen.tv_RMS_layout_width) - 5
        var fs = activity.tvAnalyzerMarker!!.textSize // size in pixel

        // shrink font size if it can not fit in one line.
        val text = activity.getString(R.string.tv_peak_text)
        // note: mTestPaint.measureText(text) do not scale like sp.
        val mTestPaint = Paint()
        mTestPaint.textSize = fs
        mTestPaint.typeface = Typeface.MONOSPACE
        while (mTestPaint.measureText(text) > px && fs > 5) {
            fs -= 0.5f
            mTestPaint.textSize = fs
        }
        activity.tvAnalyzerMarker!!.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            fs
        )
        activity.tvAnalyzerPeak!!.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            fs
        )
    }

    // Prepare the spectrum and spectrogram plot (from scratch or full reset)
    // Should be called before samplingThread starts.
    fun setupView(params: AnalyzerParams?) {
        activity.agvAnalyzer!!.setupPlot(params)
    }

    // Will be called by SamplingLoopThread (in another thread)
    fun update(spectrumDBcopy: DoubleArray?) {
        activity.agvAnalyzer!!.saveSpectrum(spectrumDBcopy)
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

    fun enableSaveWavView(bSaveWav: Boolean) {
        if (bSaveWav) {
            activity.tvAnalyzerRec!!.height = (19 * dpRatio).toInt()
        } else {
            activity.tvAnalyzerRec!!.height = (0 * dpRatio).toInt()
        }
    }

    fun showPopupMenu(view: View) {
        val wl = IntArray(2)
        view.getLocationInWindow(wl)
        val x_left = wl[0]
        val y_bottom = activity.windowManager.defaultDisplay.height - wl[1]
        val gravity = Gravity.START or Gravity.BOTTOM
        when (view.id) {
            R.id.btnAnalyzerSampleRate -> popupMenuSampleRate.showAtLocation(
                view,
                gravity,
                x_left,
                y_bottom
            )
            R.id.btnAnalyzerFFTLength -> popupMenuFFTLen.showAtLocation(
                view,
                gravity,
                x_left,
                y_bottom
            )
            R.id.btnAnalyzerAverage -> popupMenuFFTAverage.showAtLocation(
                view,
                gravity,
                x_left,
                y_bottom
            )
        }
    }

    // Maybe put this PopupWindow into a class
    private fun popupMenuCreate(
        popUpContents: Array<String>,
        resId: Int
    ): PopupWindow {

        // initialize a pop up window type
        val popupWindow = PopupWindow(activity)

        // the drop down list is a list view
        val listView = ListView(activity)

        // set our adapter and pass our pop up window contents
        val aa = popupMenuAdapter(popUpContents)
        listView.adapter = aa

        // set the item click listener
        listView.onItemClickListener = analyzerFragment
        listView.tag = resId // button res ID, so we can trace back which button is pressed

        // get max text width
        val mTestPaint = Paint()
        val listItemTextSize = activity.resources.getDimension(R.dimen.btn_text_font_size)
        mTestPaint.textSize = listItemTextSize
        var w = 0f
        var wi: Float // max text width in pixel
        for (popUpContent in popUpContents) {
            val sts = popUpContent.split("::").toTypedArray()
            val st = sts[0]
            if (sts.size == 2 && sts[1] == "0") {
                mTestPaint.textSize = activity.resources.getDimension(R.dimen.btn_text_font_size_small)
                wi = mTestPaint.measureText(st)
                mTestPaint.textSize = listItemTextSize
            } else {
                wi = mTestPaint.measureText(st)
            }
            if (w < wi) {
                w = wi
            }
        }

        // left and right padding, at least +7, or the whole app will stop respond, don't know why
        w = w + 20 * dpRatio
        if (w < 60) {
            w = 60f
        }

        // some other visual settings
        popupWindow.isFocusable = true
        popupWindow.height = WindowManager.LayoutParams.WRAP_CONTENT
        // Set window width according to max text width
        popupWindow.width = w.toInt()
        // also set button width
        (rootView.findViewById<View>(resId) as Button).width = (w + 4 * dpRatio).toInt()
        // Set the text on button in loadPreferenceForView()

        // set the list view as pop up window content
        popupWindow.contentView = listView
        return popupWindow
    }

    /*
     * adapter where the list values will be set
     */
    private fun popupMenuAdapter(itemTagArray: Array<String>): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(
            activity,
            android.R.layout.simple_list_item_1,
            itemTagArray
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                // setting the ID and text for every items in the list
                val item = getItem(position)
                val itemArr = item!!.split("::").toTypedArray()
                val text = itemArr[0]
                val id = itemArr[1]

                // visual settings for the list item
                val listItem = TextView(activity)
                if (id == "0") {
                    listItem.text = text
                    listItem.tag = id
                    listItem.textSize = activity.resources.getDimension(R.dimen.btn_text_font_size_small) / dpRatio
                    listItem.setPadding(
                        5,
                        5,
                        5,
                        5
                    )
                    listItem.setTextColor(Color.GREEN)
                    listItem.gravity = Gravity.CENTER
                } else {
                    listItem.text = text
                    listItem.tag = id
                    listItem.textSize = activity.resources.getDimension(R.dimen.btn_text_font_size) / dpRatio
                    listItem.setPadding(
                        5,
                        5,
                        5,
                        5
                    )
                    listItem.setTextColor(Color.WHITE)
                    listItem.gravity = Gravity.CENTER
                }
                return listItem
            }
        }
    }

    private fun refreshMarkerLabel() {
        val f1 = activity.agvAnalyzer!!.markerFreq
        sbMarker.setLength(0)
        sbMarker.append(activity.getString(R.string.tv_marker_text_empty))
        SBNumFormat.fillInNumFixedWidthPositive(
            sbMarker,
            f1,
            5,
            1
        )
        sbMarker.append("Hz(")
        AnalyzerUtil.freq2Cent(
            sbMarker,
            f1,
            " "
        )
        sbMarker.append(") ")
        SBNumFormat.fillInNumFixedWidth(
            sbMarker,
            activity.agvAnalyzer!!.markerDB,
            3,
            1
        )
        sbMarker.append("dB")
        sbMarker.getChars(
            0,
            Math.min(
                sbMarker.length,
                charMarker.size
            ),
            charMarker,
            0
        )
        activity.tvAnalyzerMarker!!.setText(
            charMarker,
            0,
            Math.min(
                sbMarker.length,
                charMarker.size
            )
        )
    }

    private fun refreshRMSLabel(dtRMSFromFT: Double) {
        sbRMS.setLength(0)
        sbRMS.append("RMS:dB \n")
        SBNumFormat.fillInNumFixedWidth(
            sbRMS,
            20 * Math.log10(dtRMSFromFT),
            3,
            1
        )
        sbRMS.getChars(
            0,
            Math.min(
                sbRMS.length,
                charRMS.size
            ),
            charRMS,
            0
        )
        activity.tvAnalyzerRMS!!.setText(
            charRMS,
            0,
            charRMS.size
        )
        activity.tvAnalyzerRMS!!.invalidate()
    }

    private fun refreshPeakLabel(
        maxAmpFreq: Double,
        maxAmpDB: Double
    ) {
        sbPeak.setLength(0)
        sbPeak.append(activity.getString(R.string.tv_peak_text_empty))
        SBNumFormat.fillInNumFixedWidthPositive(
            sbPeak,
            maxAmpFreq,
            5,
            1
        )
        sbPeak.append("Hz(")
        AnalyzerUtil.freq2Cent(
            sbPeak,
            maxAmpFreq,
            " "
        )
        sbPeak.append(") ")
        SBNumFormat.fillInNumFixedWidth(
            sbPeak,
            maxAmpDB,
            3,
            1
        )
        sbPeak.append("dB")
        sbPeak.getChars(
            0,
            Math.min(
                sbPeak.length,
                charPeak.size
            ),
            charPeak,
            0
        )
        activity.tvAnalyzerPeak!!.setText(
            charPeak,
            0,
            charPeak.size
        )
        activity.tvAnalyzerPeak!!.invalidate()
    }

    private fun refreshRecTimeLable(
        wavSec: Double,
        wavSecRemain: Double
    ) {
        // consist with @string/textview_rec_text
        sbRec.setLength(0)
        sbRec.append(activity.getString(R.string.tv_rec_text_empty))
        SBNumFormat.fillTime(
            sbRec,
            wavSec,
            1
        )
        sbRec.append(activity.getString(R.string.tv_rec_remain_text))
        SBNumFormat.fillTime(
            sbRec,
            wavSecRemain,
            0
        )
        sbRec.getChars(
            0,
            Math.min(
                sbRec.length,
                charRec.size
            ),
            charRec,
            0
        )
        activity.tvAnalyzerRec!!.setText(
            charRec,
            0,
            Math.min(
                sbRec.length,
                charRec.size
            )
        )
        //        tvRec.invalidate(); // COMMENT: No haría falta ???
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
        frameTime = if (activity.agvAnalyzer!!.showMode != AnalyzerGraphicView.PlotMode.SPECTRUM) {
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
            if (viewMask and VIEW_MASK_graphView != 0) activity.agvAnalyzer!!.invalidate()
            // RMS
            if (viewMask and VIEW_MASK_textview_RMS != 0) refreshRMSLabel(analyzerFragment.dtRMSFromFT)
            // peak frequency
            if (viewMask and VIEW_MASK_textview_peak != 0) refreshPeakLabel(
                analyzerFragment.getMaxAmplitudeFreq(),
                analyzerFragment.maxAmpDB
            )
            if (viewMask and VIEW_MASK_MarkerLabel != 0) refreshMarkerLabel()
            if (viewMask and VIEW_MASK_RecTimeLable != 0 && analyzerFragment.samplingThread != null) refreshRecTimeLable(
                analyzerFragment.samplingThread!!.wavSeconds,
                analyzerFragment.samplingThread!!.wavSecondsRemain
            )
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

    companion object {
        // Thanks http://stackoverflow.com/questions/37904739/html-fromhtml-deprecated-in-android-n
        fun fromHtml(source: String?): Spanned {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(
                    source,
                    Html.FROM_HTML_MODE_LEGACY
                ) // or Html.FROM_HTML_MODE_COMPACT
            } else {
                Html.fromHtml(source)
            }
        }

        private const val VIEW_MASK_graphView = 1 shl 0
        private const val VIEW_MASK_textview_RMS = 1 shl 1
        private const val VIEW_MASK_textview_peak = 1 shl 2
        private const val VIEW_MASK_MarkerLabel = 1 shl 3
        private const val VIEW_MASK_RecTimeLable = 1 shl 4
    }

    init {
        this.activity = activity
        this.analyzerFragment = analyzerFragment
        this.rootView = rootView
        dpRatio = activity.resources.displayMetrics.density
        charRMS = CharArray(activity.resources.getString(R.string.tv_rms_text).length)
        charMarker = CharArray(activity.resources.getString(R.string.tv_marker_text).length)
        charPeak = CharArray(activity.resources.getString(R.string.tv_peak_text).length)
        charRec = CharArray(activity.resources.getString(R.string.tv_rec_text).length)

        /// initialize pop up window items list
        // http://www.codeofaninja.com/2013/04/show-listview-as-drop-down-android.html
        popupMenuSampleRate = popupMenuCreate(
            AnalyzerUtil.validateAudioRates(activity.resources.getStringArray(R.array.sample_rates)),
            R.id.btnAnalyzerSampleRate
        )
        popupMenuFFTLen = popupMenuCreate(
            activity.resources.getStringArray(R.array.fft_lengths),
            R.id.btnAnalyzerFFTLength
        )
        popupMenuFFTAverage = popupMenuCreate(
            activity.resources.getStringArray(R.array.fft_averages),
            R.id.btnAnalyzerAverage
        )
        setTextViewFontSize()
        activity.cbAnalyzerCheckerStuckTab!!.setOnClickListener { v -> (activity as MainActivity).vp.stuck = (v as AppCompatCheckBox).isChecked }
    }
}
