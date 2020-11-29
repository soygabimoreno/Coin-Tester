package com.appacoustic.cointester.core.presentation.analyzer.view

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import com.appacoustic.cointester.core.R
import com.appacoustic.cointester.core.framework.AnalyzerUtil
import com.appacoustic.cointester.core.presentation.analyzer.AnalyzerFragment
import com.appacoustic.cointester.libFramework.KLog.Companion.d
import com.appacoustic.cointester.libFramework.KLog.Companion.w
import java.text.DecimalFormat

/**
 * For showing and setting plot ranges,
 * including frequency (Hz) and loudness (dB).
 */
class RangeViewDialogC(
    private val activity: Activity,
    private val analyzerFragment: AnalyzerFragment,
    private val graphView: AnalyzerGraphicView
) {

    init {
        buildDialog(activity)
    }

    private var rangeViewDialog: AlertDialog? = null
    private lateinit var rangeViewView: View

    // Watch if there is change in the EditText
    private inner class MyTextWatcher(private val mEditText: EditText) : TextWatcher {
        override fun beforeTextChanged(
            charSequence: CharSequence,
            i: Int,
            i1: Int,
            i2: Int
        ) {
        }

        override fun onTextChanged(
            s: CharSequence,
            start: Int,
            before: Int,
            count: Int
        ) {
            mEditText.tag = true // flag that indicate range been changed
        }

        override fun afterTextChanged(editable: Editable) {}
    }

    private fun setRangeView(loadSaved: Boolean) {
        if (rangeViewDialog == null) {
            return
        }
        val vals = graphView.viewPhysicalRange
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
        val isLock = sharedPref.getBoolean(
            "view_range_lock",
            false
        )
        // If locked, load the saved value
        if (isLock || loadSaved) {
            var rr: DoubleArray? = DoubleArray(AnalyzerGraphicView.VIEW_RANGE_DATA_LENGTH)
            for (i in rr!!.indices) {
                rr[i] = AnalyzerUtil.getDouble(
                    sharedPref,
                    "view_range_rr_$i",
                    0.0 / 0.0
                )
                if (java.lang.Double.isNaN(rr[i])) {  // not properly initialized
                    w("rr is not properly initialized")
                    rr = null
                    break
                }
            }
            if (rr != null) System.arraycopy(
                rr,
                0,
                vals,
                0,
                rr.size
            )
        }
        val df = DecimalFormat("#.##")
        (rangeViewView.findViewById<View>(R.id.etRangesFreqLowerBound) as EditText)
            .setText(df.format(vals[0]))
        (rangeViewView.findViewById<View>(R.id.etRangesFreqUpperBound) as EditText)
            .setText(df.format(vals[1]))
        (rangeViewView.findViewById<View>(R.id.etRangesDBLowerBound) as EditText)
            .setText(df.format(vals[2]))
        (rangeViewView.findViewById<View>(R.id.etRangesDBUpperBound) as EditText)
            .setText(df.format(vals[3]))
        (rangeViewView.findViewById<View>(R.id.tvRangesFreqFromTo) as TextView).text = activity.getString(
            R.string.ranges_freq_from_to,
            vals[6],
            vals[7]
        )
        (rangeViewView.findViewById<View>(R.id.tvRangesDBInstructions) as TextView).text = activity.getString(R.string.set_lower_and_upper_db_bound)
        (rangeViewView.findViewById<View>(R.id.tvRangesDBFromTo) as TextView).text = activity.getString(
            R.string.ranges_db_from_to,
            vals[8],
            vals[9]
        )
        (rangeViewView.findViewById<View>(R.id.cbRangesLockRanges) as CheckBox).isChecked = isLock
    }

    fun show() {
        setRangeView(false)

        // Listener for test if a field is modified
        val resList = intArrayOf(
            R.id.etRangesFreqLowerBound,
            R.id.etRangesFreqUpperBound,
            R.id.etRangesDBLowerBound,
            R.id.etRangesDBUpperBound
        )
        for (id in resList) {
            val et = rangeViewView.findViewById<View>(id) as EditText
            et.tag = false // false = no modified
            et.addTextChangedListener(MyTextWatcher(et)) // Am I need to remove previous Listener first?
        }
        rangeViewDialog!!.show()
    }

    @SuppressLint("InflateParams")
    private fun buildDialog(context: Context) {
        val inflater = LayoutInflater.from(context)
        rangeViewView = inflater.inflate(
            R.layout.dialog_ranges,
            null
        ) // null because there is no parent. https://possiblemobile.com/2013/05/layout-inflation-as-intended/
        rangeViewView.findViewById<View>(R.id.btnRangesLoadPrevious).setOnClickListener { setRangeView(true) }
        val freqDialogBuilder = AlertDialog.Builder(context)
        freqDialogBuilder
            .setView(rangeViewView)
            .setPositiveButton(R.string.ok) { dialog, id ->
                val isLock = (rangeViewView.findViewById<View>(R.id.cbRangesLockRanges) as CheckBox).isChecked
                val rangeDefault = graphView.viewPhysicalRange
                var rr = DoubleArray(rangeDefault.size / 2)
                val resList = intArrayOf(
                    R.id.etRangesFreqLowerBound,
                    R.id.etRangesFreqUpperBound,
                    R.id.etRangesDBLowerBound,
                    R.id.etRangesDBUpperBound
                )
                for (i in resList.indices) {
                    val et = rangeViewView.findViewById<View>(resList[i]) as? EditText
                    if (et == null) d("EditText[$i] == null")
                    if (et == null) continue
                    if (et.tag == null) d("EditText[$i].getTag == null")
                    if (et.tag == null || et.tag as Boolean || isLock) {
                        rr[i] = AnalyzerUtil.parseDouble(et.text.toString())
                    } else {
                        rr[i] = rangeDefault[i]
                        d("EditText[" + i + "] not change. rr[i] = " + rr[i])
                    }
                }
                // Save setting to preference, after sanitized.
                rr = graphView.setViewRange(
                    rr,
                    rangeDefault
                )
                saveViewRange(
                    rr,
                    isLock
                )
                if (isLock) {
                    analyzerFragment.stickToMeasureMode()
                    analyzerFragment.viewRangeArray = rr
                } else {
                    analyzerFragment.stickToMeasureModeCancel()
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, id -> d("rangeViewDialog: Canceled") }
        //    freqDialogBuilder
//            .setTitle("dialog_title");
        rangeViewDialog = freqDialogBuilder.create()
    }

    private fun saveViewRange(
        rr: DoubleArray,
        isLock: Boolean
    ) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
        val editor = sharedPref.edit()
        for (i in rr.indices) {
            AnalyzerUtil.putDouble(
                editor,
                "view_range_rr_$i",
                rr[i]
            ) // no editor.putDouble ? kidding me?
        }
        editor.putBoolean(
            "view_range_lock",
            isLock
        )
        editor.apply()
    }
}
