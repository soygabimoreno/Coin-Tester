package com.appacoustic.cointester;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.appacoustic.cointester.analyzer.AnalyzerUtil;
import com.appacoustic.cointester.analyzer.RangeViewDialogC;
import com.appacoustic.cointester.analyzer.model.AnalyzerParams;
import com.appacoustic.cointester.analyzer.model.SamplingLoopThread;
import com.appacoustic.cointester.analyzer.settings.AudioSourcesCheckerActivity;
import com.appacoustic.cointester.analyzer.settings.CalibrationLoad;
import com.appacoustic.cointester.analyzer.settings.MyPreferencesActivity;
import com.appacoustic.cointester.analyzer.view.AnalyzerGraphicView;
import com.appacoustic.cointester.analyzer.view.AnalyzerViews;
import com.appacoustic.cointester.analyzer.view.SelectorText;
import com.appacoustic.cointester.domain.Coin;
import com.gabrielmorenoibarra.k.util.KLog;

import butterknife.BindArray;
import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Main fragment.
 * Created by Gabriel Moreno on 2017-01-22.
 */
public class AnalyzerFragment extends Fragment implements View.OnLongClickListener,
        View.OnClickListener, AdapterView.OnItemClickListener, AnalyzerGraphicView.OnReadyListener {

    public static final String TAG = AnalyzerFragment.class.getSimpleName();

    @BindView(R.id.tvAnalyzerCheckerCoinName)
    TextView tvCoinName;
    @BindView(R.id.tvAnalyzerCheckerCoinPlace)
    TextView tvCoinPlace;
    @BindView(R.id.fabAnalyzerChecker)
    FloatingActionButton fab;

    @BindView(R.id.rlAnalyzerSpectrum)
    RelativeLayout rlSpectrum;
    @BindView(R.id.tvAnalyzerRunStop)
    SelectorText tvRun;
    @BindView(R.id.btnAnalyzerSampleRate)
    Button btnSampleRate;
    @BindView(R.id.btnAnalyzerFFTLength)
    Button btnFFTLength;
    @BindView(R.id.btnAnalyzerAverage)
    Button btnAverage;

    @BindColor(R.color.cadet)
    int cadet;
    @BindColor(R.color.cadet_1)
    int cadet1;

    @BindArray(R.array.audio_sources_entries)
    String[] audioSourcesEntries;
    @BindArray(R.array.audio_sources)
    String[] audioSourcesString;
    @BindArray(R.array.window_functions)
    String[] windowFunctions;

    private int magnitudeTextSize;

    // for pass audioSourceIDs and audioSourcesEntries to MyPreferencesActivity
    public final static String MY_PREFERENCES_MSG_SOURCE_ID = TAG + ".SOURCE_ID";
    public final static String MY_PREFERENCES_MSG_SOURCE_NAME = TAG + ".SOURCE_NAME";

    private AnalyzerViews analyzerViews;
    private SamplingLoopThread samplingThread = null;
    private RangeViewDialogC rangeViewDialogC;
    private GestureDetectorCompat mDetector;

    private AnalyzerParams params = null;

    public double dtRMS = 0;
    private double dtRMSFromFT = 0;
    private double maxAmpDB;
    private double maxAmplitudeFreq;
    public double[] viewRangeArray = null;

    private boolean isMeasure = false;
    private boolean isLockViewRange = false;
    public volatile boolean saveWav;

    CalibrationLoad calibLoad = new CalibrationLoad();  // data for calibration of spectrum
    private View rootView;

    public AnalyzerFragment() {
    }

    public static AnalyzerFragment newInstance() {
        return new AnalyzerFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_analyzer, container, false);
        ButterKnife.bind(this, rootView);

        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                canvasSpectrum.drawColor(Color.WHITE);
//                recordPerform();
            }
        });

        int[] audioSources = new int[audioSourcesString.length];
        for (int i = 0; i < audioSourcesString.length; i++) {
            audioSources[i] = Integer.parseInt(audioSourcesString[i]);
        }
        int sourcesCounter = AnalyzerParams.N_MIC_SOURCES;
        AnalyzerParams.Companion.setIdTestSignal1(audioSources[sourcesCounter]);
        AnalyzerParams.Companion.setIdTestSignal2(audioSources[sourcesCounter + 1]);
        AnalyzerParams.Companion.setIdTestSignalWhiteNoise(audioSources[sourcesCounter + 2]);

        params = new AnalyzerParams(audioSourcesEntries, audioSources, windowFunctions);

        // Initialized preferences by default values
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        // Read preferences and set corresponding variables
        loadPreferenceForView();

        analyzerViews = new AnalyzerViews(getActivity(), this, rootView);

        // travel Views, and attach ClickListener to the views that contain android:tag="select"
        visit((ViewGroup) analyzerViews.getAgv().getRootView(), new AnalyzerFragment.Visit() {
            @Override
            public void exec(View view) {
                view.setOnLongClickListener(AnalyzerFragment.this);
                view.setOnClickListener(AnalyzerFragment.this);
                ((TextView) view).setFreezesText(true);
            }
        }, "select");

        rangeViewDialogC = new RangeViewDialogC(getActivity(), this, analyzerViews.getAgv());

        mDetector = new GestureDetectorCompat(getActivity(), new AnalyzerFragment.AnalyzerGestureListener());

        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isInGraphView(event.getX(0), event.getY(0))) {
                    mDetector.onTouchEvent(event);
                    if (isMeasure) {
                        measureEvent(event);
                    } else {
                        scaleEvent(event);
                    }
                    analyzerViews.invalidateGraphView();
                    // Go to scaling mode when user release finger in measure mode.
                    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        if (isMeasure) {
                            switchMeasureAndScaleMode();
                        }
                    }
                } else {
                    // When finger is outside the plot, hide the marker and go to scaling mode.
                    if (isMeasure) {
                        analyzerViews.getAgv().hideMarker();
                        switchMeasureAndScaleMode();
                    }
                }
                return true;
            }
        });

        btnSampleRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyzerViews.showPopupMenu(v);
            }
        });

        btnFFTLength.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyzerViews.showPopupMenu(v);
            }
        });

        btnAverage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyzerViews.showPopupMenu(v);
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        KLog.Companion.d(Thread.currentThread().getStackTrace()[2].getMethodName() + " " + hashCode());
        super.onResume();

        LoadPreferences();
        analyzerViews.getAgv().setReady(this);  // TODO: move this earlier?
        analyzerViews.enableSaveWavView(saveWav);

        // Used to prevent extra calling to restartSampling() (e.g. in LoadPreferences())
        bSamplingPreparation = true;

        // Start sampling
        restartSampling(params);
    }

    @Override
    public void onPause() {
        KLog.Companion.d(Thread.currentThread().getStackTrace()[2].getMethodName() + " " + hashCode());
        bSamplingPreparation = false;
        if (samplingThread != null) {
            samplingThread.finish();
        }
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        KLog.Companion.d(Thread.currentThread().getStackTrace()[2].getMethodName() + " " + hashCode());
        savedInstanceState.putDouble("dtRMS", dtRMS);
        savedInstanceState.putDouble("dtRMSFromFT", dtRMSFromFT);
        savedInstanceState.putDouble("maxAmpDB", maxAmpDB);
        savedInstanceState.putDouble("maxAmplitudeFreq", maxAmplitudeFreq);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) { // Equivalent to onRestoreInstanceState()
        super.onActivityCreated(savedInstanceState);
        KLog.Companion.d(Thread.currentThread().getStackTrace()[2].getMethodName() + " " + hashCode());
        if (savedInstanceState != null) {
            dtRMS = savedInstanceState.getDouble("dtRMS");
            dtRMSFromFT = savedInstanceState.getDouble("dtRMSFromFT");
            maxAmpDB = savedInstanceState.getDouble("maxAmpDB");
            maxAmplitudeFreq = savedInstanceState.getDouble("maxAmplitudeFreq");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CALIB_LOAD && resultCode == Activity.RESULT_OK) {
            final Uri uri = data.getData();
            calibLoad.loadFile(uri, getActivity());
            KLog.Companion.w("mime:" + getActivity().getContentResolver().getType(uri));
            fillFftCalibration(params, calibLoad);
        } else if (requestCode == REQUEST_AUDIO_GET) {
            KLog.Companion.w("requestCode == REQUEST_AUDIO_GET");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuMainAbout:
                return false;
            case R.id.menuMainContact:
                return false;
            case R.id.menuMainUserManual:
                analyzerViews.showInstructions();
                return true;
            case R.id.menuMainPreferences:
                Intent settings = new Intent(getActivity().getBaseContext(), MyPreferencesActivity.class);
                settings.putExtra(MY_PREFERENCES_MSG_SOURCE_ID, params.getAudioSourceIds());
                settings.putExtra(MY_PREFERENCES_MSG_SOURCE_NAME, params.getAudioSourceNames());
                getActivity().startActivity(settings);
                return true;
            case R.id.menuMainAudioSourcesChecker:
                Intent int_info_rec = new Intent(getActivity(), AudioSourcesCheckerActivity.class);
                getActivity().startActivity(int_info_rec);
                return true;
            case R.id.menuMainRanges:
                rangeViewDialogC.ShowRangeViewDialog();
                return true;
            case R.id.menuMainCalibration:
                selectFile(REQUEST_CALIB_LOAD);
                // return true TODO: ???
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    static final int REQUEST_AUDIO_GET = 1;
    static final int REQUEST_CALIB_LOAD = 2;

    public void selectFile(int requestType) {
        // https://developer.android.com/guide/components/intents-common.html#Storage
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        if (requestType == REQUEST_AUDIO_GET) {
            intent.setType("audio/*");
        } else {
            intent.setType("*/*");
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(intent, requestType);
        } else {
            KLog.Companion.e("No file chooser found!.");

            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(getActivity(), "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    void fillFftCalibration(AnalyzerParams params, CalibrationLoad _calibLoad) {
        if (_calibLoad.freq == null || _calibLoad.freq.length == 0 || params == null) {
            return;
        }
        double[] freqTick = new double[params.getFftLength() / 2];
        for (int i = 0; i < freqTick.length; i++) {
            freqTick[i] = (i + 1.0) / params.getFftLength() * params.getSampleRate();
        }
        params.setMicGainDB(AnalyzerUtil.interpLinear(_calibLoad.freq, _calibLoad.gain, freqTick));
//        for (int i = 0; i < _analyzerParam.micGainDB.length; i++) {
//            KLog.Companion.i("calib: " + freqTick[i] + "Hz : " + _analyzerParam.micGainDB[i]);
//        }
    }// Popup menu click listener

    // Read chosen preference, save the preference, set the state.
    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        // get the tag, which is the value we are going to use
        String selectedItemTag = v.getTag().toString();
        // if tag() is "0" then do not update anything (it is a title)
        if (selectedItemTag.equals("0")) {
            return;
        }

        // get the text and set it as the button text
        String selectedItemText = ((TextView) v).getText().toString();

        int buttonId = Integer.parseInt((parent.getTag().toString()));
        Button buttonView = (Button) rootView.findViewById(buttonId);
        buttonView.setText(selectedItemText);

        boolean b_need_restart_audio;

        // Save the choosen preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPref.edit();

        // so change of sample rate do not change view range
        if (!isLockViewRange) {
            viewRangeArray = analyzerViews.getAgv().getViewPhysicalRange();
            // if range is align at boundary, extend the range.
            KLog.Companion.i("set sampling rate:a " + viewRangeArray[0] + " ==? " + viewRangeArray[6]);
            if (viewRangeArray[0] == viewRangeArray[6]) {
                viewRangeArray[0] = 0;
            }
        }

        // dismiss the pop up
        switch (buttonId) {
            case R.id.btnAnalyzerSampleRate:
                analyzerViews.getPopupMenuSampleRate().dismiss();
                if (!isLockViewRange) {
                    KLog.Companion.i("set sampling rate:b " + viewRangeArray[1] + " ==? " + viewRangeArray[6 + 1]);
                    if (viewRangeArray[1] == viewRangeArray[6 + 1]) {
                        viewRangeArray[1] = Integer.parseInt(selectedItemTag) / 2;
                    }
                    KLog.Companion.i("onItemClick(): viewRangeArray saved. " + viewRangeArray[0] + " ~ " + viewRangeArray[1]);
                }
                params.setSampleRate(Integer.parseInt(selectedItemTag));
                b_need_restart_audio = true;
                editor.putInt("button_sample_rate", params.getSampleRate());
                break;
            case R.id.btnAnalyzerFFTLength:
                analyzerViews.getPopupMenuFFTLen().dismiss();
                params.setFftLength(Integer.parseInt(selectedItemTag));
                params.setHopLength((int) (params.getFftLength() * (1 - params.getOverlapPercent() / 100) + 0.5));
                b_need_restart_audio = true;
                editor.putInt("button_fftlen", params.getFftLength());
                fillFftCalibration(params, calibLoad);
                break;
            case R.id.btnAnalyzerAverage:
                analyzerViews.getPopupMenuFFTAverage().dismiss();
                params.setNFftAverage(Integer.parseInt(selectedItemTag));
                if (analyzerViews.getAgv() != null) {
                    analyzerViews.getAgv().setTimeMultiplier(params.getNFftAverage());
                }
                b_need_restart_audio = false;
                editor.putInt("button_average", params.getNFftAverage());
                break;
            default:
                KLog.Companion.w("onItemClick(): no this button");
                b_need_restart_audio = false;
        }

        editor.apply();

        if (b_need_restart_audio) {
            restartSampling(params);
        }
    }

    // Load preferences for Views
    // When this function is called, the SamplingLoopThread must not running in the meanwhile.
    private void loadPreferenceForView() {
        // load preferences for buttons
        // list-buttons
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        params.setSampleRate(sharedPref.getInt("button_sample_rate", 8000));
        params.setFftLength(sharedPref.getInt("button_fftlen", 1024));
        params.setNFftAverage(sharedPref.getInt("button_average", 1));
        // toggle-buttons
        params.setDBAWeighting(sharedPref.getBoolean("dbA", false));
        if (params.getDBAWeighting()) {
            ((SelectorText) rootView.findViewById(R.id.tvAnalyzerDBDBA)).nextValue();
        }
        boolean isSpam = sharedPref.getBoolean("spectrum_spectrogram_mode", true);
        if (!isSpam) {
            ((SelectorText) rootView.findViewById(R.id.tvAnalyzerSpectrumSpectrogramMode)).nextValue();
        }
        String axisMode = sharedPref.getString("freq_scaling_mode", "linear");
        SelectorText st = (SelectorText) rootView.findViewById(R.id.tvAnalyzerLinearLogNote);
        st.setValue(axisMode);

        KLog.Companion.i(Thread.currentThread().getStackTrace()[2].getMethodName() + ":" +
                "\n  sampleRate  = " + params.getSampleRate() +
                "\n  fFTLength      = " + params.getFftLength() +
                "\n  nFFTAverage = " + params.getNFftAverage());
        ((Button) rootView.findViewById(R.id.btnAnalyzerSampleRate)).setText(Integer.toString(params.getSampleRate()));
        ((Button) rootView.findViewById(R.id.btnAnalyzerFFTLength)).setText(Integer.toString(params.getFftLength()));
        ((Button) rootView.findViewById(R.id.btnAnalyzerAverage)).setText(Integer.toString(params.getNFftAverage()));
    }

    private void LoadPreferences() {
        // Load preferences for recorder and views, beside loadPreferenceForView()
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        boolean keepScreenOn = sharedPref.getBoolean("keepScreenOn", true);
        if (keepScreenOn) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        params.setAudioSourceId(Integer.parseInt(sharedPref.getString("audioSource", Integer.toString(AnalyzerParams.RECORDER_AGC_OFF))));
        params.setWindowFunctionName(sharedPref.getString("windowFunction", "Hanning"));
        params.setSpectrogramDuration(Double.parseDouble(sharedPref.getString("spectrogramDuration", Double.toString(6.0))));
        params.setOverlapPercent(Double.parseDouble(sharedPref.getString("fft_overlap_percent", "50.0")));
        params.setHopLength((int) (params.getFftLength() * (1 - params.getOverlapPercent() / 100) + 0.5));

        // Settings of graph view
        // spectrum
        analyzerViews.getAgv().setShowLines(sharedPref.getBoolean("showLines", false));
        // set spectrum show range
        analyzerViews.getAgv().setSpectrumDBLowerBound(
                Float.parseFloat(sharedPref.getString("spectrumRange", Double.toString(AnalyzerGraphicView.MIN_DB)))
        );

        // spectrogram
        analyzerViews.getAgv().setSpectrogramModeShifting(sharedPref.getBoolean("spectrogramShifting", false));
        analyzerViews.getAgv().setShowTimeAxis(sharedPref.getBoolean("spectrogramTimeAxis", true));
        analyzerViews.getAgv().setShowFreqAlongX(sharedPref.getBoolean("spectrogramShowFreqAlongX", true));
        analyzerViews.getAgv().setSmoothRender(sharedPref.getBoolean("spectrogramSmoothRender", false));
        analyzerViews.getAgv().setColorMap(sharedPref.getString("spectrogramColorMap", "Hot"));
        // set spectrogram show range
        analyzerViews.getAgv().setSpectrogramDBLowerBound(Float.parseFloat(sharedPref.getString("spectrogramRange", Double.toString(analyzerViews.getAgv().getSpectrogramPlot().getSpectrogramBMP().getdBLowerBound()))));
        analyzerViews.getAgv().setLogAxisMode(sharedPref.getBoolean("spectrogramLogPlotMethod", true));

        analyzerViews.setbWarnOverrun(sharedPref.getBoolean("warnOverrun", false));
        analyzerViews.setFpsLimit(Double.parseDouble(
                sharedPref.getString("spectrogramFPS", getString(R.string.spectrogram_fps_default))));

        // Apply settings by travel the views with android:tag="select":
        visit((ViewGroup) analyzerViews.getAgv().getRootView(), new AnalyzerFragment.Visit() {
            @Override
            public void exec(View v) {
                processClick(v);
            }
        }, "select");

        // Get view range setting
        boolean isLock = sharedPref.getBoolean("view_range_lock", false);
        if (isLock) {
            KLog.Companion.i(Thread.currentThread().getStackTrace()[2].getMethodName() + ": isLocked");
            // Set view range and stick to measure mode
            double[] rr = new double[AnalyzerGraphicView.VIEW_RANGE_DATA_LENGTH];
            for (int i = 0; i < rr.length; i++) {
                rr[i] = AnalyzerUtil.getDouble(sharedPref, "view_range_rr_" + i, 0.0 / 0.0);
                if (Double.isNaN(rr[i])) {  // not properly initialized
                    KLog.Companion.w(Thread.currentThread().getStackTrace()[2].getMethodName() + ": rr is not properly initialized");
                    rr = null;
                    break;
                }
            }
            if (rr != null) {
                viewRangeArray = rr;
            }
            stickToMeasureMode();
        } else {
            stickToMeasureModeCancel();
        }
    }

    public void stickToMeasureMode() {
        isLockViewRange = true;
        switchMeasureAndScaleMode();  // Force set to Measure mode
    }

    public void stickToMeasureModeCancel() {
        isLockViewRange = false;
        if (isMeasure) {
            switchMeasureAndScaleMode();  // Force set to ScaleMode
        }
    }

    private boolean isInGraphView(float x, float y) {
        analyzerViews.getAgv().getLocationInWindow(windowLocation);
        return x >= windowLocation[0] && y >= windowLocation[1] &&
                x < windowLocation[0] + analyzerViews.getAgv().getWidth() &&
                y < windowLocation[1] + analyzerViews.getAgv().getHeight();
    }

    public double getDtRMSFromFT() {
        return dtRMSFromFT;
    }

    public void setDtRMSFromFT(double dtRMSFromFT) {
        this.dtRMSFromFT = dtRMSFromFT;
    }

    public double getMaxAmpDB() {
        return maxAmpDB;
    }

    public void setMaxAmpDB(double maxAmpDB) {
        this.maxAmpDB = maxAmpDB;
    }

    public double getMaxAmplitudeFreq() {
        return maxAmplitudeFreq;
    }

    public void setMaxAmplitudeFreq(double maxAmplitudeFreq) {
        this.maxAmplitudeFreq = maxAmplitudeFreq;
    }

    public SamplingLoopThread getSamplingThread() {
        return samplingThread;
    }

    public void setSamplingThread(SamplingLoopThread samplingThread) {
        this.samplingThread = samplingThread;
    }

    /**
     * Gesture Listener for graphView (and possibly other views)
     * How to attach these events to the graphView?
     *
     * @author xyy
     */
    private class AnalyzerGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {  // enter here when down action happen
            flyingMoveHandler.removeCallbacks(flyingMoveRunnable);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            if (isInGraphView(event.getX(0), event.getY(0))) {
                if (!isMeasure) {  // go from "scale" mode to "marker" mode
                    switchMeasureAndScaleMode();
                }
            }
            measureEvent(event);  // force insert this event
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            if (!isMeasure) {
                scaleEvent(event);            // ends scale mode
                analyzerViews.getAgv().resetViewScale();
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            if (isMeasure) {
                // seems never reach here...
                return true;
            }
            // Fly the canvas in graphView when in scale mode
            shiftingVelocity = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
            shiftingComponentX = velocityX / shiftingVelocity;
            shiftingComponentY = velocityY / shiftingVelocity;
            float DPRatio = getResources().getDisplayMetrics().density;
            flyAcceleration = 1200 * DPRatio;
            timeFlingStart = SystemClock.uptimeMillis();
            flyingMoveHandler.postDelayed(flyingMoveRunnable, 0);
            return true;
        }

        Handler flyingMoveHandler = new Handler();
        long timeFlingStart;                     // Prevent from running forever
        double flyDt = 1 / 20.;                     // delta t of refresh
        double shiftingVelocity;                  // fling velocity
        double shiftingComponentX;                // fling direction x
        double shiftingComponentY;                // fling direction y
        double flyAcceleration = 1200.;           // damping acceleration of fling, pixels/second^2

        Runnable flyingMoveRunnable = new Runnable() {
            @Override
            public void run() {
                double shiftingVelocityNew = shiftingVelocity - flyAcceleration * flyDt;
                if (shiftingVelocityNew < 0) shiftingVelocityNew = 0;
                // Number of pixels that should move in this time step
                double shiftingPixel = (shiftingVelocityNew + shiftingVelocity) / 2 * flyDt;
                shiftingVelocity = shiftingVelocityNew;
                if (shiftingVelocity > 0f
                        && SystemClock.uptimeMillis() - timeFlingStart < 10000) {
                    // KLog.Companion.i("  fly pixels x=" + shiftingPixelX + " y=" + shiftingPixelY);
                    AnalyzerGraphicView graphView = analyzerViews.getAgv();
                    graphView.setXShift(graphView.getXShift() - shiftingComponentX * shiftingPixel / graphView.getCanvasWidth() / graphView.getXZoom());
                    graphView.setYShift(graphView.getYShift() - shiftingComponentY * shiftingPixel / graphView.getCanvasHeight() / graphView.getYZoom());
                    // Am I need to use runOnUiThread() ?
                    analyzerViews.invalidateGraphView();
                    flyingMoveHandler.postDelayed(flyingMoveRunnable, (int) (1000 * flyDt));
                }
            }
        };
    }

    private void switchMeasureAndScaleMode() {
        if (isLockViewRange) {
            isMeasure = true;
            return;
        }
        isMeasure = !isMeasure;
    }

    /**
     * Manage marker for measurement.
     */
    private void measureEvent(MotionEvent event) {
        switch (event.getPointerCount()) {
            case 1:
                analyzerViews.getAgv().setMarker(event.getX(), event.getY());
                // TODO: if touch point is very close to boundary for a long time, move the view
                break;
            case 2:
                if (isInGraphView(event.getX(1), event.getY(1))) {
                    switchMeasureAndScaleMode();
                }
        }
    }

    /**
     * Manage scroll and zoom
     */
    final private static double INIT = Double.MIN_VALUE;
    private boolean isPinching = false;
    private double xShift0 = INIT, yShift0 = INIT;
    private double x0, y0;
    private int[] windowLocation = new int[2];

    private void scaleEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_MOVE) {
            xShift0 = INIT;
            yShift0 = INIT;
            isPinching = false;
            // KLog.Companion.i("scaleEvent(): Skip event " + event.getAction());
            return;
        }
        // KLog.Companion.i("scaleEvent(): switch " + event.getAction());
        AnalyzerGraphicView graphView = analyzerViews.getAgv();
        switch (event.getPointerCount()) {
            case 2:
                if (isPinching) {
                    graphView.setShiftScale(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                } else {
                    graphView.setShiftScaleBegin(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                }
                isPinching = true;
                break;
            case 1:
                float x = event.getX(0);
                float y = event.getY(0);
                graphView.getLocationInWindow(windowLocation);
                // KLog.Companion.i("scaleEvent(): xy=" + x + " " + y + "  wc = " + wc[0] + " " + wc[1]);
                if (isPinching || xShift0 == INIT) {
                    xShift0 = graphView.getXShift();
                    x0 = x;
                    yShift0 = graphView.getYShift();
                    y0 = y;
                } else {
                    // when close to the axis, scroll that axis only
                    if (x0 < windowLocation[0] + 50) {
                        graphView.setYShift(yShift0 + (y0 - y) / graphView.getCanvasHeight() / graphView.getYZoom());
                    } else if (y0 < windowLocation[1] + 50) {
                        graphView.setXShift(xShift0 + (x0 - x) / graphView.getCanvasWidth() / graphView.getXZoom());
                    } else {
                        graphView.setXShift(xShift0 + (x0 - x) / graphView.getCanvasWidth() / graphView.getXZoom());
                        graphView.setYShift(yShift0 + (y0 - y) / graphView.getCanvasHeight() / graphView.getYZoom());
                    }
                }
                isPinching = false;
                break;
            default:
                KLog.Companion.i("Invalid touch count");
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        vibrate(300);
        KLog.Companion.i("long click: " + view.toString());
        return true;
    }

    // Responds to layout with android:tag="select"
    // Called from SelectorText.super.performClick()
    @Override
    public void onClick(View v) {
        if (processClick(v)) {
            restartSampling(params);
        }
        analyzerViews.invalidateGraphView();
    }

    private final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;  // just a number
    private final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    public Thread graphInit;
    private boolean bSamplingPreparation = false;

    private void restartSampling(final AnalyzerParams params) {
        // Stop previous sampler if any.
        if (samplingThread != null) {
            samplingThread.finish();
            try {
                samplingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            samplingThread = null;
        }

        if (viewRangeArray != null) {
            analyzerViews.getAgv().setupAxes(this.params);
            double[] rangeDefault = analyzerViews.getAgv().getViewPhysicalRange();
            KLog.Companion.i(Thread.currentThread().getStackTrace()[2].getMethodName() + ": setViewRange: " + viewRangeArray[0] + " ~ " + viewRangeArray[1]);
            analyzerViews.getAgv().setViewRange(viewRangeArray, rangeDefault);
            if (!isLockViewRange) viewRangeArray = null;  // do not conserve
        }

        // Set the view for incoming data
        graphInit = new Thread(new Runnable() {
            public void run() {
                analyzerViews.setupView(params);
            }
        });
        graphInit.start();

        // Check and request permissions
        if (!checkAndRequestPermissions())
            return;

        if (!bSamplingPreparation)
            return;

        // Start sampling
        samplingThread = new SamplingLoopThread(
                params,
                analyzerViews,
                tvRun.getValue().equals("stop"),
                saveWav,
                new SamplingLoopThread.Listener() {
                    @Override
                    public void onInitGraphs() {
                        try {
                            graphInit.join(); // TODO: Seems not working as intended
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onUpdateAmplitude(double maxAmplitudeFreq, double maxAmplitudeDB) {
                        AnalyzerFragment.this.maxAmplitudeFreq = maxAmplitudeFreq;
                        maxAmpDB = maxAmplitudeDB;
                    }

                    @Override
                    public void onUpdateRms(double rms, double rmsFromFT) {
                        dtRMS = rms;
                        dtRMSFromFT = rmsFromFT;
                    }
                });
        samplingThread.start();
    }

    // For call requestPermissions() after each showPermissionExplanation()
    private int count_permission_explanation = 0;

    // For preventing infinity loop: onResume() -> requestPermissions() -> onRequestPermissionsResult() -> onResume()
    private int count_permission_request = 0;

    // Test and try to gain permissions.
    // Return true if it is OK to proceed.
    // Ref.
    //   https://developer.android.com/training/permissions/requesting.html
    //   https://developer.android.com/guide/topics/permissions/requesting.html
    private boolean checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            KLog.Companion.w("Permission RECORD_AUDIO denied. Trying  to request...");
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.RECORD_AUDIO) &&
                    count_permission_explanation < 1) {
                KLog.Companion.w("  Show explanation here....");
                analyzerViews.showPermissionExplanation(R.string.permission_explanation_recorder);
                count_permission_explanation++;
            } else {
                KLog.Companion.w("  Requesting...");
                if (count_permission_request < 3) {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
                    count_permission_explanation = 0;
                    count_permission_request++;
                } else {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Context context = getContext().getApplicationContext();
                            String text = "Permission denied.";
                            Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
                }
            }
            return false;
        }
        if (saveWav &&
                ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            KLog.Companion.w("Permission WRITE_EXTERNAL_STORAGE denied. Trying  to request...");
            ((SelectorText) rootView.findViewById(R.id.tvAnalyzerMonitorRecord)).nextValue();
            saveWav = false;
            analyzerViews.enableSaveWavView(saveWav);
//      ((SelectorText) findViewById(R.id.tvAnalyzerRecording)).performClick();
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            // Still possible to proceed with saveWav == false
            // simulate a view click, so that saveWav = false
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    KLog.Companion.w("RECORD_AUDIO Permission granted by user.");
                } else {
                    KLog.Companion.w("RECORD_AUDIO Permission denied by user.");
                }
                break;
            }
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    KLog.Companion.w("WRITE_EXTERNAL_STORAGE Permission granted by user.");
                    if (!saveWav) {
                        KLog.Companion.w("... saveWav == true");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((SelectorText) rootView.findViewById(R.id.tvAnalyzerMonitorRecord)).nextValue();
                                saveWav = true;
                                analyzerViews.enableSaveWavView(saveWav);
                            }
                        });
                    } else {
                        KLog.Companion.w("... saveWav == false");
                    }
                } else {
                    KLog.Companion.w("WRITE_EXTERNAL_STORAGE Permission denied by user.");
                }
                break;
            }
        }
        // Then onResume() will be called.
    }

    /**
     * Process a click on one of our selectors.
     *
     * @param v The view that was clicked
     * @return true if we need to update the graph
     */

    public boolean processClick(View v) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPref.edit();
        String value;
        if (v instanceof SelectorText) {
            value = ((SelectorText) v).getValue();
        } else {
            value = ((TextView) v).getText().toString();
        }
        switch (v.getId()) {
            case R.id.tvAnalyzerMonitorRecord:
                saveWav = value.equals("Rec");
                //  SelectorText st = (SelectorText) findViewById(R.id.run);
                //  if (saveWav && ! st.getText().toString().equals("stop")) {
                //    st.nextValue();
                //    if (samplingThread != null) {
                //      samplingThread.setPaused(true);
                //    }
                //  }
                analyzerViews.enableSaveWavView(saveWav);
                return true;
            case R.id.tvAnalyzerRunStop:
                boolean pause = value.equals("stop");
                if (samplingThread != null && samplingThread.getPaused() != pause) {
                    samplingThread.setPaused(pause);
                }
                analyzerViews.getAgv().getSpectrogramPlot().setPause(pause);
                return false;
            case R.id.tvAnalyzerLinearLogNote: {
                KLog.Companion.d(Thread.currentThread().getStackTrace()[2].getMethodName() + " freq_scaling_mode = " + value);
                analyzerViews.getAgv().setAxisModeLinear(value);
                editor.putString("freq_scaling_mode", value);
                editor.apply();
                return false;
            }
            case R.id.tvAnalyzerDBDBA:
                params.setDBAWeighting(!value.equals("dB"));
                if (samplingThread != null) {
                    samplingThread.setAWeighting(params.getDBAWeighting());
                }
                editor.putBoolean("dbA", params.getDBAWeighting());
                editor.commit();
                return false;
            case R.id.tvAnalyzerSpectrumSpectrogramMode:
                if (value.equals("spum")) {
                    analyzerViews.getAgv().switch2Spectrum();
                } else {
                    analyzerViews.getAgv().switch2Spectrogram();
                }
                editor.putBoolean("spectrum_spectrogram_mode", value.equals("spum"));
                editor.commit();
                return false;
            default:
                return true;
        }
    }

    private void vibrate(int ms) {
//        ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(ms);
    }

    /**
     * Visit all subviews of this view group and run command
     *
     * @param group  The parent view group
     * @param cmd    The command to run for each view
     * @param select The tag value that must match. Null implies all views
     */

    private void visit(ViewGroup group, AnalyzerFragment.Visit cmd, String select) {
        exec(group, cmd, select);
        for (int i = 0; i < group.getChildCount(); i++) {
            View c = group.getChildAt(i);
            if (c instanceof ViewGroup) {
                visit((ViewGroup) c, cmd, select);
            } else {
                exec(c, cmd, select);
            }
        }
    }

    private void exec(View v, AnalyzerFragment.Visit cmd, String select) {
        if (select == null || select.equals(v.getTag())) {
            cmd.exec(v);
        }
    }

    /**
     * Interface for view hierarchy visitor
     */
    interface Visit {
        void exec(View view);
    }

    /**
     * The graph view size has been determined.
     * By the way, the the labels are updated accordingly.
     */
    @Override
    public void ready() {
        KLog.Companion.d(Thread.currentThread().getStackTrace()[2].getMethodName() + " " + hashCode());
        analyzerViews.invalidateGraphView();
    }

    public void loadCoin(Coin item) {
        tvCoinName.setText(item.getName());
        tvCoinPlace.setText(item.getPlace());
        fab.setImageResource(item.getHead());
    }

    public SelectorText getTvRun() {
        return tvRun;
    }

    public AnalyzerViews getAnalyzerViews() {
        return analyzerViews;
    }
}