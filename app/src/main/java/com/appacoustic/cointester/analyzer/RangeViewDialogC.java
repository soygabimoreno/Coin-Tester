package com.appacoustic.cointester.analyzer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.appacoustic.cointester.AnalyzerFragment;
import com.appacoustic.cointester.R;
import com.appacoustic.cointester.analyzer.view.AnalyzerGraphicView;
import com.appacoustic.cointester.framework.KLog;

import java.text.DecimalFormat;

/**
 * For showing and setting plot ranges,
 * including frequency (Hz) and loudness (dB).
 */
public class RangeViewDialogC {

    private static final String TAG = RangeViewDialogC.class.getSimpleName();

    private AlertDialog rangeViewDialog = null;
    private View rangeViewView;

    private final Activity activity;
    private final AnalyzerFragment analyzerFragment;
    private final AnalyzerGraphicView graphView;

    public RangeViewDialogC(Activity activity, AnalyzerFragment analyzerFragment, AnalyzerGraphicView _graphView) {
        this.activity = activity;
        this.analyzerFragment = analyzerFragment;
        graphView = _graphView;
        buildDialog(activity);
    }

    // Watch if there is change in the EditText
    private class MyTextWatcher implements TextWatcher {
        private EditText mEditText;

        MyTextWatcher(EditText editText) {
            mEditText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mEditText.setTag(true);  // flag that indicate range been changed
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    private void SetRangeView(boolean loadSaved) {
        final String METHOD_NAME = Thread.currentThread().getStackTrace()[2].getMethodName();
        if (rangeViewDialog == null) {
            KLog.Companion.d(METHOD_NAME + ": rangeViewDialog is not prepared.");
            return;
        }
        double[] vals = graphView.getViewPhysicalRange();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean isLock = sharedPref.getBoolean("view_range_lock", false);
        // If locked, load the saved value
        if (isLock || loadSaved) {
            double[] rr = new double[AnalyzerGraphicView.VIEW_RANGE_DATA_LENGTH];
            for (int i = 0; i < rr.length; i++) {
                rr[i] = AnalyzerUtil.getDouble(sharedPref, "view_range_rr_" + i, 0.0 / 0.0);
                if (Double.isNaN(rr[i])) {  // not properly initialized
                    KLog.Companion.w(METHOD_NAME + ": rr is not properly initialized");
                    rr = null;
                    break;
                }
            }
            if (rr != null)
                System.arraycopy(rr, 0, vals, 0, rr.length);
        }

        DecimalFormat df = new DecimalFormat("#.##");
        ((EditText) rangeViewView.findViewById(R.id.etRangesFreqLowerBound))
                .setText(df.format(vals[0]));
        ((EditText) rangeViewView.findViewById(R.id.etRangesFreqUpperBound))
                .setText(df.format(vals[1]));
        ((EditText) rangeViewView.findViewById(R.id.etRangesDBLowerBound))
                .setText(df.format(vals[2]));
        ((EditText) rangeViewView.findViewById(R.id.etRangesDBUpperBound))
                .setText(df.format(vals[3]));
        ((TextView) rangeViewView.findViewById(R.id.tvRangesFreqFromTo))
                .setText(activity.getString(R.string.ranges_freq_from_to, vals[6], vals[7]));
        ((TextView) rangeViewView.findViewById(R.id.tvRangesDBInstructions))
                .setText(activity.getString(R.string.set_lower_and_upper_db_bound));
        ((TextView) rangeViewView.findViewById(R.id.tvRangesDBFromTo))
                .setText(activity.getString(R.string.ranges_db_from_to, vals[8], vals[9]));

        ((CheckBox) rangeViewView.findViewById(R.id.cbRangesLockRanges)).setChecked(isLock);
    }

    public void ShowRangeViewDialog() {
        SetRangeView(false);

        // Listener for test if a field is modified
        int[] resList = {R.id.etRangesFreqLowerBound, R.id.etRangesFreqUpperBound,
                R.id.etRangesDBLowerBound, R.id.etRangesDBUpperBound};
        for (int id : resList) {
            EditText et = (EditText) rangeViewView.findViewById(id);
            et.setTag(false);                                     // false = no modified
            et.addTextChangedListener(new MyTextWatcher(et));     // Am I need to remove previous Listener first?
        }

        rangeViewDialog.show();
    }

    @SuppressLint("InflateParams")
    private void buildDialog(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        rangeViewView = inflater.inflate(R.layout.dialog_ranges, null);  // null because there is no parent. https://possiblemobile.com/2013/05/layout-inflation-as-intended/
        rangeViewView.findViewById(R.id.btnRangesLoadPrevious).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        SetRangeView(true);
                    }
                }
        );
        AlertDialog.Builder freqDialogBuilder = new AlertDialog.Builder(context);
        freqDialogBuilder
                .setView(rangeViewView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        boolean isLock = ((CheckBox) rangeViewView.findViewById(R.id.cbRangesLockRanges)).isChecked();
                        double[] rangeDefault = graphView.getViewPhysicalRange();
                        double[] rr = new double[rangeDefault.length / 2];
                        int[] resList = {R.id.etRangesFreqLowerBound, R.id.etRangesFreqUpperBound,
                                R.id.etRangesDBLowerBound, R.id.etRangesDBUpperBound};
                        for (int i = 0; i < resList.length; i++) {
                            EditText et = (EditText) rangeViewView.findViewById(resList[i]);
                            if (et == null) KLog.Companion.d("EditText[" + i + "] == null");
                            if (et == null) continue;
                            if (et.getTag() == null) KLog.Companion.d("EditText[" + i + "].getTag == null");
                            if (et.getTag() == null || (boolean) et.getTag() || isLock) {
                                rr[i] = AnalyzerUtil.parseDouble(et.getText().toString());
                            } else {
                                rr[i] = rangeDefault[i];
                                KLog.Companion.d("EditText[" + i + "] not change. rr[i] = " + rr[i]);
                            }
                        }
                        // Save setting to preference, after sanitized.
                        rr = graphView.setViewRange(rr, rangeDefault);
                        SaveViewRange(rr, isLock);
                        if (isLock) {
                            analyzerFragment.stickToMeasureMode();
                            analyzerFragment.viewRangeArray = rr;
                        } else {
                            analyzerFragment.stickToMeasureModeCancel();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        KLog.Companion.d("rangeViewDialog: Canceled");
                    }
                });
//    freqDialogBuilder
//            .setTitle("dialog_title");
        rangeViewDialog = freqDialogBuilder.create();
    }

    private void SaveViewRange(double[] rr, boolean isLock) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPref.edit();
        for (int i = 0; i < rr.length; i++) {
            AnalyzerUtil.putDouble(editor, "view_range_rr_" + i, rr[i]);  // no editor.putDouble ? kidding me?
        }
        editor.putBoolean("view_range_lock", isLock);
        editor.commit();
    }
}
