package com.appacoustic.cointester.aaa.analyzer.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.appacoustic.cointester.R;
import com.appacoustic.cointester.aaa.analyzer.AnalyzerUtil;
import com.appacoustic.cointester.aaa.analyzer.SBNumFormat;
import com.appacoustic.cointester.aaa.analyzer.model.AnalyzerParams;
import com.appacoustic.cointester.presentation.AnalyzerFragment;
import com.appacoustic.cointester.presentation.MainActivity;

import butterknife.BindArray;
import butterknife.BindDimen;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Operate the views in the UI here.
 * Should run on UI thread in general.
 */
public class AnalyzerViews {

    @BindView(R.id.agvAnalyzer)
    AnalyzerGraphicView agv;
    @BindView(R.id.tvAnalyzerRMS)
    TextView tvRMS;
    @BindView(R.id.tvAnalyzerMarker)
    TextView tvMarker;
    @BindView(R.id.tvAnalyzerPeak)
    TextView tvPeak;
    @BindView(R.id.tvAnalyzerRec)
    TextView tvRec;
    @BindView(R.id.cbAnalyzerCheckerStuckTab)
    CheckBox cbStuckTab;

    @BindString(R.string.tv_rms_text)
    String tvRMSText;
    @BindString(R.string.tv_marker_text)
    String tvMarkerText;
    @BindString(R.string.tv_peak_text)
    String tvPeakText;
    @BindString(R.string.tv_rec_text)
    String tvRecText;

    @BindArray(R.array.sample_rates)
    String[] sampleRates;
    @BindArray(R.array.fft_lengths)
    String[] fFFTLengths;
    @BindArray(R.array.fft_averages)
    String[] fFFTAverages;

    @BindDimen(R.dimen.btn_text_font_size)
    float listItemTextSize;
    @BindDimen(R.dimen.btn_text_font_size_small)
    float listItemTitleTextSize;

    private final Activity activity;
    private final AnalyzerFragment analyzerFragment;
    private final View rootView;

    private float dpRatio;
    private double fpsLimit = 8;

    private StringBuilder sbRMS = new StringBuilder("");
    private StringBuilder sbMarker = new StringBuilder("");
    private StringBuilder sbPeak = new StringBuilder("");
    private StringBuilder sbRec = new StringBuilder("");
    private char[] charRMS;
    private char[] charMarker;
    private char[] charPeak;
    private char[] charRec;

    private PopupWindow popupMenuSampleRate;
    private PopupWindow popupMenuFFTLen;
    private PopupWindow popupMenuFFTAverage;

    private boolean warnOverrun = true;

    public AnalyzerViews(final Activity activity, AnalyzerFragment analyzerFragment, View rootView) {
        ButterKnife.bind(this, rootView);
        this.activity = activity;
        this.analyzerFragment = analyzerFragment;
        this.rootView = rootView;

        dpRatio = activity.getResources().getDisplayMetrics().density;

        charRMS = new char[tvRMSText.length()];
        charMarker = new char[tvMarkerText.length()];
        charPeak = new char[tvPeakText.length()];
        charRec = new char[tvRecText.length()];

        /// initialize pop up window items list
        // http://www.codeofaninja.com/2013/04/show-listview-as-drop-down-android.html
        popupMenuSampleRate = popupMenuCreate(AnalyzerUtil.validateAudioRates(sampleRates), R.id.btnAnalyzerSampleRate);
        popupMenuFFTLen = popupMenuCreate(fFFTLengths, R.id.btnAnalyzerFFTLength);
        popupMenuFFTAverage = popupMenuCreate(fFFTAverages, R.id.btnAnalyzerAverage);

        setTextViewFontSize();

        cbStuckTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) activity).getVp().setStuck(((AppCompatCheckBox) v).isChecked());
            }
        });
    }

    // Set text font size of textview_marker and tvAnalyzerPeak
    // according to space left
    //@SuppressWarnings("deprecation")
    private void setTextViewFontSize() {
        // At this point tv.getWidth(), tv.getLineCount() will return 0

        Display display = activity.getWindowManager().getDefaultDisplay();
        // pixels left
        float px = display.getWidth() - activity.getResources().getDimension(R.dimen.tv_RMS_layout_width) - 5;

        float fs = tvMarker.getTextSize();  // size in pixel

        // shrink font size if it can not fit in one line.
        final String text = activity.getString(R.string.tv_peak_text);
        // note: mTestPaint.measureText(text) do not scale like sp.
        Paint mTestPaint = new Paint();
        mTestPaint.setTextSize(fs);
        mTestPaint.setTypeface(Typeface.MONOSPACE);
        while (mTestPaint.measureText(text) > px && fs > 5) {
            fs -= 0.5;
            mTestPaint.setTextSize(fs);
        }

        tvMarker.setTextSize(TypedValue.COMPLEX_UNIT_PX, fs);
        tvPeak.setTextSize(TypedValue.COMPLEX_UNIT_PX, fs);
    }

    // Prepare the spectrum and spectrogram plot (from scratch or full reset)
    // Should be called before samplingThread starts.
    public void setupView(AnalyzerParams params) {
        agv.setupPlot(params);
    }

    // Will be called by SamplingLoopThread (in another thread)
    public void update(final double[] spectrumDBcopy) {
        agv.saveSpectrum(spectrumDBcopy);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // data will get out of synchronize here
                invalidateGraphView();
            }
        });
    }

    private double wavSecOld = 0;      // used to reduce frame rate

    public void updateRec(double wavSec) {
        if (wavSecOld > wavSec) {
            wavSecOld = wavSec;
        }
        if (wavSec - wavSecOld < 0.1) {
            return;
        }
        wavSecOld = wavSec;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // data will get out of synchronize here
                invalidateGraphView(AnalyzerViews.VIEW_MASK_RecTimeLable);
            }
        });
    }

    public void notifyWAVSaved(final String path) {
        String s = String.format(activity.getString(R.string.audio_saved_to_x), "'" + path + "'");
        notifyToast(s);
    }

    public void notifyToast(final String s) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Context context = activity.getApplicationContext();
                Toast toast = Toast.makeText(context, s, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    public void notifyToast(@StringRes final int resId) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Context context = activity.getApplicationContext();
                Toast toast = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    private long lastTimeNotifyOverrun = 0;

    public void notifyOverrun() {
        if (!warnOverrun) {
            return;
        }
        long time = SystemClock.uptimeMillis();
        if (time - lastTimeNotifyOverrun > 6000) {
            lastTimeNotifyOverrun = time;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Context context = activity.getApplicationContext();
                    Toast toast = Toast.makeText(context, R.string.error_recorder_buffer_overrun, Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
    }

    public void showInstructions() {
        TextView tv = new TextView(activity);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setText(fromHtml(activity.getString(R.string.instructions_text)));
        PackageInfo pInfo = null;
        String version = "\n" + activity.getString(R.string.app_name) + "  Version: ";
        try {
            pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            version += pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version += "(Unknown)";
        }
        tv.append(version);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.instructions_title)
                .setView(tv)
                .setNegativeButton(R.string.dismiss, null)
                .create().show();
    }

    public void showPermissionExplanation(int resId) {
        TextView tv = new TextView(activity);
        tv.setMovementMethod(new ScrollingMovementMethod());
        tv.setText(fromHtml(activity.getString(resId)));
        new AlertDialog.Builder(activity)
                .setTitle(R.string.permission_explanation_title)
                .setView(tv)
                .setNegativeButton(R.string.dismiss, null)
                .create().show();
    }

    // Thanks http://stackoverflow.com/questions/37904739/html-fromhtml-deprecated-in-android-n
    @SuppressWarnings("deprecation")
    public static android.text.Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY); // or Html.FROM_HTML_MODE_COMPACT
        } else {
            return Html.fromHtml(source);
        }
    }

    public void enableSaveWavView(boolean bSaveWav) {
        if (bSaveWav) {
            tvRec.setHeight((int) (19 * dpRatio));
        } else {
            tvRec.setHeight((int) (0 * dpRatio));
        }
    }

    @SuppressWarnings("deprecation")
    public void showPopupMenu(View view) {
        int[] wl = new int[2];
        view.getLocationInWindow(wl);
        int x_left = wl[0];
        int y_bottom = activity.getWindowManager().getDefaultDisplay().getHeight() - wl[1];
        int gravity = android.view.Gravity.START | android.view.Gravity.BOTTOM;

        switch (view.getId()) {
            case R.id.btnAnalyzerSampleRate:
                popupMenuSampleRate.showAtLocation(view, gravity, x_left, y_bottom);
                break;
            case R.id.btnAnalyzerFFTLength:
                popupMenuFFTLen.showAtLocation(view, gravity, x_left, y_bottom);
                break;
            case R.id.btnAnalyzerAverage:
                popupMenuFFTAverage.showAtLocation(view, gravity, x_left, y_bottom);
                break;
        }
    }

    // Maybe put this PopupWindow into a class
    private PopupWindow popupMenuCreate(String[] popUpContents, int resId) {

        // initialize a pop up window type
        PopupWindow popupWindow = new PopupWindow(activity);

        // the drop down list is a list view
        ListView listView = new ListView(activity);

        // set our adapter and pass our pop up window contents
        ArrayAdapter<String> aa = popupMenuAdapter(popUpContents);
        listView.setAdapter(aa);

        // set the item click listener
        listView.setOnItemClickListener(analyzerFragment);

        listView.setTag(resId);  // button res ID, so we can trace back which button is pressed

        // get max text width
        Paint mTestPaint = new Paint();
        mTestPaint.setTextSize(listItemTextSize);
        float w = 0;
        float wi;      // max text width in pixel
        for (String popUpContent : popUpContents) {
            String sts[] = popUpContent.split("::");
            String st = sts[0];
            if (sts.length == 2 && sts[1].equals("0")) {
                mTestPaint.setTextSize(listItemTitleTextSize);
                wi = mTestPaint.measureText(st);
                mTestPaint.setTextSize(listItemTextSize);
            } else {
                wi = mTestPaint.measureText(st);
            }
            if (w < wi) {
                w = wi;
            }
        }

        // left and right padding, at least +7, or the whole app will stop respond, don't know why
        w = w + 20 * dpRatio;
        if (w < 60) {
            w = 60;
        }

        // some other visual settings
        popupWindow.setFocusable(true);
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        // Set window width according to max text width
        popupWindow.setWidth((int) w);
        // also set button width
        ((Button) rootView.findViewById(resId)).setWidth((int) (w + 4 * dpRatio));
        // Set the text on button in loadPreferenceForView()

        // set the list view as pop up window content
        popupWindow.setContentView(listView);

        return popupWindow;
    }

    /*
     * adapter where the list values will be set
     */
    private ArrayAdapter<String> popupMenuAdapter(String itemTagArray[]) {
        return new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, itemTagArray) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                // setting the ID and text for every items in the list
                String item = getItem(position);
                String[] itemArr = item.split("::");
                String text = itemArr[0];
                String id = itemArr[1];

                // visual settings for the list item
                TextView listItem = new TextView(activity);

                if (id.equals("0")) {
                    listItem.setText(text);
                    listItem.setTag(id);
                    listItem.setTextSize(listItemTitleTextSize / dpRatio);
                    listItem.setPadding(5, 5, 5, 5);
                    listItem.setTextColor(Color.GREEN);
                    listItem.setGravity(android.view.Gravity.CENTER);
                } else {
                    listItem.setText(text);
                    listItem.setTag(id);
                    listItem.setTextSize(listItemTextSize / dpRatio);
                    listItem.setPadding(5, 5, 5, 5);
                    listItem.setTextColor(Color.WHITE);
                    listItem.setGravity(android.view.Gravity.CENTER);
                }

                return listItem;
            }
        };
    }

    private void refreshMarkerLabel() {
        double f1 = agv.getMarkerFreq();

        sbMarker.setLength(0);
        sbMarker.append(activity.getString(R.string.tv_marker_text_empty));
        SBNumFormat.fillInNumFixedWidthPositive(sbMarker, f1, 5, 1);
        sbMarker.append("Hz(");
        AnalyzerUtil.freq2Cent(sbMarker, f1, " ");
        sbMarker.append(") ");
        SBNumFormat.fillInNumFixedWidth(sbMarker, agv.getMarkerDB(), 3, 1);
        sbMarker.append("dB");
        sbMarker.getChars(0, Math.min(sbMarker.length(), charMarker.length), charMarker, 0);

        tvMarker.setText(charMarker, 0, Math.min(sbMarker.length(), charMarker.length));
    }

    private void refreshRMSLabel(double dtRMSFromFT) {
        sbRMS.setLength(0);
        sbRMS.append("RMS:dB \n");
        SBNumFormat.fillInNumFixedWidth(sbRMS, 20 * Math.log10(dtRMSFromFT), 3, 1);
        sbRMS.getChars(0, Math.min(sbRMS.length(), charRMS.length), charRMS, 0);

        tvRMS.setText(charRMS, 0, charRMS.length);
        tvRMS.invalidate();
    }

    private void refreshPeakLabel(double maxAmpFreq, double maxAmpDB) {
        sbPeak.setLength(0);
        sbPeak.append(activity.getString(R.string.tv_peak_text_empty));
        SBNumFormat.fillInNumFixedWidthPositive(sbPeak, maxAmpFreq, 5, 1);
        sbPeak.append("Hz(");
        AnalyzerUtil.freq2Cent(sbPeak, maxAmpFreq, " ");
        sbPeak.append(") ");
        SBNumFormat.fillInNumFixedWidth(sbPeak, maxAmpDB, 3, 1);
        sbPeak.append("dB");
        sbPeak.getChars(0, Math.min(sbPeak.length(), charPeak.length), charPeak, 0);

        tvPeak.setText(charPeak, 0, charPeak.length);
        tvPeak.invalidate();
    }

    private void refreshRecTimeLable(double wavSec, double wavSecRemain) {
        // consist with @string/textview_rec_text
        sbRec.setLength(0);
        sbRec.append(activity.getString(R.string.tv_rec_text_empty));
        SBNumFormat.fillTime(sbRec, wavSec, 1);
        sbRec.append(activity.getString(R.string.tv_rec_remain_text));
        SBNumFormat.fillTime(sbRec, wavSecRemain, 0);
        sbRec.getChars(0, Math.min(sbRec.length(), charRec.length), charRec, 0);
        tvRec.setText(charRec, 0, Math.min(sbRec.length(), charRec.length));
//        tvRec.invalidate(); // COMMENT: No haría falta ???
    }

    private long timeToUpdate = SystemClock.uptimeMillis();
    private volatile boolean isInvalidating = false;

    // Invalidate analyzerGraphic in a limited frame rate
    public void invalidateGraphView() {
        invalidateGraphView(-1);
    }

    private static final int VIEW_MASK_graphView = 1 << 0;
    private static final int VIEW_MASK_textview_RMS = 1 << 1;
    private static final int VIEW_MASK_textview_peak = 1 << 2;
    private static final int VIEW_MASK_MarkerLabel = 1 << 3;
    private static final int VIEW_MASK_RecTimeLable = 1 << 4;

    private void invalidateGraphView(int viewMask) {
        if (isInvalidating) {
            return;
        }
        isInvalidating = true;
        long frameTime;                      // time delay for next frame
        if (agv.getShowMode() != AnalyzerGraphicView.PlotMode.SPECTRUM) {
            frameTime = (long) (1000 / fpsLimit);  // use a much lower frame rate for spectrogram
        } else {
            frameTime = 1000 / 60;
        }
        long t = SystemClock.uptimeMillis();
        if (t >= timeToUpdate) {    // limit frame rate
            timeToUpdate += frameTime;
            if (timeToUpdate < t) {            // catch up current time
                timeToUpdate = t + frameTime;
            }
            idPaddingInvalidate = false;
            // Take care of synchronization of analyzerGraphic.spectrogramColors and iTimePointer,
            // and then just do invalidate() here.
            if ((viewMask & VIEW_MASK_graphView) != 0)
                agv.invalidate();
            // RMS
            if ((viewMask & VIEW_MASK_textview_RMS) != 0)
                refreshRMSLabel(analyzerFragment.getDtRMSFromFT());
            // peak frequency
            if ((viewMask & VIEW_MASK_textview_peak) != 0)
                refreshPeakLabel(analyzerFragment.getMaxAmplitudeFreq(), analyzerFragment.getMaxAmpDB());
            if ((viewMask & VIEW_MASK_MarkerLabel) != 0)
                refreshMarkerLabel();
            if ((viewMask & VIEW_MASK_RecTimeLable) != 0 && analyzerFragment.getSamplingThread() != null)
                refreshRecTimeLable(analyzerFragment.getSamplingThread().getWavSeconds(), analyzerFragment.getSamplingThread().getWavSecondsRemain());
        } else {
            if (!idPaddingInvalidate) {
                idPaddingInvalidate = true;
                paddingViewMask = viewMask;
                paddingInvalidateHandler.postDelayed(paddingInvalidateRunnable, timeToUpdate - t + 1);
            } else {
                paddingViewMask |= viewMask;
            }
        }
        isInvalidating = false;
    }

    public void setFpsLimit(double _fpsLimit) {
        fpsLimit = _fpsLimit;
    }

    private volatile boolean idPaddingInvalidate = false;
    private volatile int paddingViewMask = -1;
    private Handler paddingInvalidateHandler = new Handler();

    // Am I need to use runOnUiThread() ?
    private final Runnable paddingInvalidateRunnable = new Runnable() {
        @Override
        public void run() {
            if (idPaddingInvalidate) {
                // It is possible that t-timeToUpdate <= 0 here, don't know why
                invalidateGraphView(paddingViewMask);
            }
        }
    };

    public AnalyzerGraphicView getAgv() {
        return agv;
    }

    public PopupWindow getPopupMenuSampleRate() {
        return popupMenuSampleRate;
    }

    public void setPopupMenuSampleRate(PopupWindow popupMenuSampleRate) {
        this.popupMenuSampleRate = popupMenuSampleRate;
    }

    public PopupWindow getPopupMenuFFTLen() {
        return popupMenuFFTLen;
    }

    public void setPopupMenuFFTLen(PopupWindow popupMenuFFTLen) {
        this.popupMenuFFTLen = popupMenuFFTLen;
    }

    public PopupWindow getPopupMenuFFTAverage() {
        return popupMenuFFTAverage;
    }

    public void setPopupMenuFFTAverage(PopupWindow popupMenuFFTAverage) {
        this.popupMenuFFTAverage = popupMenuFFTAverage;
    }

    public boolean isWarnOverrun() {
        return warnOverrun;
    }

    public void setWarnOverrun(boolean warnOverrun) {
        this.warnOverrun = warnOverrun;
    }
}