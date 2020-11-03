package com.appacoustic.cointester.presentation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.preference.PreferenceManager
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.AdapterView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import com.appacoustic.cointester.R
import com.appacoustic.cointester.aaa.analyzer.AnalyzerUtil
import com.appacoustic.cointester.aaa.analyzer.RangeViewDialogC
import com.appacoustic.cointester.aaa.analyzer.SamplingLoopThread
import com.appacoustic.cointester.aaa.analyzer.model.AnalyzerParams
import com.appacoustic.cointester.aaa.analyzer.model.AnalyzerParams.Companion.idTestSignal1
import com.appacoustic.cointester.aaa.analyzer.model.AnalyzerParams.Companion.idTestSignal2
import com.appacoustic.cointester.aaa.analyzer.model.AnalyzerParams.Companion.idTestSignalWhiteNoise
import com.appacoustic.cointester.aaa.analyzer.settings.AudioSourcesCheckerActivity
import com.appacoustic.cointester.aaa.analyzer.settings.CalibrationLoad
import com.appacoustic.cointester.aaa.analyzer.settings.MyPreferencesActivity
import com.appacoustic.cointester.aaa.analyzer.view.AnalyzerGraphicView
import com.appacoustic.cointester.aaa.analyzer.view.AnalyzerGraphicView.OnReadyListener
import com.appacoustic.cointester.aaa.analyzer.view.AnalyzerViews
import com.appacoustic.cointester.aaa.analyzer.view.SelectorText
import com.appacoustic.cointester.coredomain.Coin
import com.appacoustic.cointester.presentation.AnalyzerFragment
import com.gabrielmorenoibarra.k.util.KLog.Companion.d
import com.gabrielmorenoibarra.k.util.KLog.Companion.e
import com.gabrielmorenoibarra.k.util.KLog.Companion.i
import com.gabrielmorenoibarra.k.util.KLog.Companion.w
import kotlinx.android.synthetic.main.fragment_analyzer.*

/**
 * Main fragment.
 * Created by Gabriel Moreno on 2017-01-22.
 */
class AnalyzerFragment : Fragment(), View.OnLongClickListener, View.OnClickListener, AdapterView.OnItemClickListener, OnReadyListener {

    private val magnitudeTextSize = 0
    var analyzerViews: AnalyzerViews? = null
        private set
    var samplingThread: SamplingLoopThread? = null
    private var rangeViewDialogC: RangeViewDialogC? = null
    private var mDetector: GestureDetectorCompat? = null
    private var params: AnalyzerParams? = null
    var dtRMS = 0.0
    var dtRMSFromFT = 0.0
    var maxAmpDB = 0.0
    private var maxAmplitudeFreq = 0.0

    @JvmField
    var viewRangeArray: DoubleArray? = null
    private var isMeasure = false
    private var isLockViewRange = false

    @Volatile
    var saveWav = false
    var calibLoad = CalibrationLoad() // data for calibration of spectrum
    private var rootView: View? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(
            R.layout.fragment_analyzer,
            container,
            false
        )
        return rootView
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )
        fabAnalyzerChecker.setOnClickListener {
            //                canvasSpectrum.drawColor(Color.WHITE);
//                recordPerform();
        }
        val audioSourcesString = resources.getStringArray(R.array.audio_sources)
        val audioSources = IntArray(audioSourcesString.size)
        for (i in audioSourcesString.indices) {
            audioSources[i] = audioSourcesString[i].toInt()
        }
        val sourcesCounter = AnalyzerParams.N_MIC_SOURCES
        idTestSignal1 = audioSources[sourcesCounter]
        idTestSignal2 = audioSources[sourcesCounter + 1]
        idTestSignalWhiteNoise = audioSources[sourcesCounter + 2]
        val windowFunctions = resources.getStringArray(R.array.window_functions)
        params = AnalyzerParams(
            resources.getStringArray(R.array.audio_sources_entries),
            audioSources,
            windowFunctions
        )

        // Initialized preferences by default values
        PreferenceManager.setDefaultValues(
            activity,
            R.xml.preferences,
            false
        )
        // Read preferences and set corresponding variables
        loadPreferenceForView()
        analyzerViews = AnalyzerViews(
            activity,
            this,
            rootView
        )

        // travel Views, and attach ClickListener to the views that contain android:tag="select"
        visit(
            analyzerViews!!.agv.rootView as ViewGroup,
            object : Visit {
                override fun exec(view: View) {
                    view.setOnLongClickListener(this@AnalyzerFragment)
                    view.setOnClickListener(this@AnalyzerFragment)
                    (view as TextView).freezesText = true
                }
            },
            "select"
        )
        rangeViewDialogC = RangeViewDialogC(
            activity,
            this,
            analyzerViews!!.agv
        )
        mDetector = GestureDetectorCompat(
            activity,
            AnalyzerGestureListener()
        )
        rootView!!.setOnTouchListener(View.OnTouchListener { v, event ->
            if (isInGraphView(
                    event.getX(0),
                    event.getY(0)
                )
            ) {
                mDetector!!.onTouchEvent(event)
                if (isMeasure) {
                    measureEvent(event)
                } else {
                    scaleEvent(event)
                }
                analyzerViews!!.invalidateGraphView()
                // Go to scaling mode when user release finger in measure mode.
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    if (isMeasure) {
                        switchMeasureAndScaleMode()
                    }
                }
            } else {
                // When finger is outside the plot, hide the marker and go to scaling mode.
                if (isMeasure) {
                    analyzerViews!!.agv.hideMarker()
                    switchMeasureAndScaleMode()
                }
            }
            true
        })
        btnAnalyzerSampleRate.setOnClickListener { v -> analyzerViews!!.showPopupMenu(v) }
        btnAnalyzerFFTLength.setOnClickListener { v -> analyzerViews!!.showPopupMenu(v) }
        btnAnalyzerAverage.setOnClickListener { v -> analyzerViews!!.showPopupMenu(v) }
    }

    override fun onResume() {
        d(Thread.currentThread().stackTrace[2].methodName + " " + hashCode())
        super.onResume()
        LoadPreferences()
        analyzerViews!!.agv.setReady(this) // TODO: move this earlier?
        analyzerViews!!.enableSaveWavView(saveWav)

        // Used to prevent extra calling to restartSampling() (e.g. in LoadPreferences())
        bSamplingPreparation = true

        // Start sampling
        restartSampling(params)
    }

    override fun onPause() {
        d(Thread.currentThread().stackTrace[2].methodName + " " + hashCode())
        bSamplingPreparation = false
        if (samplingThread != null) {
            samplingThread!!.finish()
        }
        activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        d(Thread.currentThread().stackTrace[2].methodName + " " + hashCode())
        savedInstanceState.putDouble(
            "dtRMS",
            dtRMS
        )
        savedInstanceState.putDouble(
            "dtRMSFromFT",
            dtRMSFromFT
        )
        savedInstanceState.putDouble(
            "maxAmpDB",
            maxAmpDB
        )
        savedInstanceState.putDouble(
            "maxAmplitudeFreq",
            maxAmplitudeFreq
        )
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) { // Equivalent to onRestoreInstanceState()
        super.onActivityCreated(savedInstanceState)
        d(Thread.currentThread().stackTrace[2].methodName + " " + hashCode())
        if (savedInstanceState != null) {
            dtRMS = savedInstanceState.getDouble("dtRMS")
            dtRMSFromFT = savedInstanceState.getDouble("dtRMSFromFT")
            maxAmpDB = savedInstanceState.getDouble("maxAmpDB")
            maxAmplitudeFreq = savedInstanceState.getDouble("maxAmplitudeFreq")
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == REQUEST_CALIB_LOAD && resultCode == Activity.RESULT_OK) {
            val uri = data!!.data
            calibLoad.loadFile(
                uri,
                activity
            )
            w("mime:" + activity!!.contentResolver.getType(uri!!))
            fillFftCalibration(
                params,
                calibLoad
            )
        } else if (requestCode == REQUEST_AUDIO_GET) {
            w("requestCode == REQUEST_AUDIO_GET")
        }
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        super.onCreateOptionsMenu(
            menu,
            inflater
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuMainAbout -> false
            R.id.menuMainContact -> false
            R.id.menuMainUserManual -> {
                analyzerViews!!.showInstructions()
                true
            }
            R.id.menuMainPreferences -> {
                val settings = Intent(
                    activity!!.baseContext,
                    MyPreferencesActivity::class.java
                )
                settings.putExtra(
                    MY_PREFERENCES_MSG_SOURCE_ID,
                    params!!.audioSourceIds
                )
                settings.putExtra(
                    MY_PREFERENCES_MSG_SOURCE_NAME,
                    params!!.audioSourceNames
                )
                activity!!.startActivity(settings)
                true
            }
            R.id.menuMainAudioSourcesChecker -> {
                val int_info_rec = Intent(
                    activity,
                    AudioSourcesCheckerActivity::class.java
                )
                activity!!.startActivity(int_info_rec)
                true
            }
            R.id.menuMainRanges -> {
                rangeViewDialogC!!.ShowRangeViewDialog()
                true
            }
            R.id.menuMainCalibration -> {
                selectFile(REQUEST_CALIB_LOAD)
                super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun selectFile(requestType: Int) {
        // https://developer.android.com/guide/components/intents-common.html#Storage
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        if (requestType == REQUEST_AUDIO_GET) {
            intent.type = "audio/*"
        } else {
            intent.type = "*/*"
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        if (intent.resolveActivity(activity!!.packageManager) != null) {
            startActivityForResult(
                intent,
                requestType
            )
        } else {
            e("No file chooser found!.")

            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(
                activity,
                "Please install a File Manager.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun fillFftCalibration(
        params: AnalyzerParams?,
        _calibLoad: CalibrationLoad
    ) {
        if (_calibLoad.freq == null || _calibLoad.freq.size == 0 || params == null) {
            return
        }
        val freqTick = DoubleArray(params.fftLength / 2)
        for (i in freqTick.indices) {
            freqTick[i] = (i + 1.0) / params.fftLength * params.sampleRate
        }
        params.micGainDB = AnalyzerUtil.interpLinear(
            _calibLoad.freq,
            _calibLoad.gain,
            freqTick
        )
        //        for (int i = 0; i < _analyzerParam.micGainDB.length; i++) {
//            KLog.Companion.i("calib: " + freqTick[i] + "Hz : " + _analyzerParam.micGainDB[i]);
//        }
    } // Popup menu click listener

    // Read chosen preference, save the preference, set the state.
    override fun onItemClick(
        parent: AdapterView<*>,
        v: View,
        position: Int,
        id: Long
    ) {
        // get the tag, which is the value we are going to use
        val selectedItemTag = v.tag.toString()
        // if tag() is "0" then do not update anything (it is a title)
        if (selectedItemTag == "0") {
            return
        }

        // get the text and set it as the button text
        val selectedItemText = (v as TextView).text.toString()
        val buttonId = parent.tag.toString().toInt()
        val buttonView = rootView!!.findViewById<View>(buttonId) as Button
        buttonView.text = selectedItemText
        val b_need_restart_audio: Boolean

        // Save the choosen preference
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
        val editor = sharedPref.edit()

        // so change of sample rate do not change view range
        if (!isLockViewRange) {
            viewRangeArray = analyzerViews!!.agv.viewPhysicalRange as DoubleArray
            // if range is align at boundary, extend the range.
            i("set sampling rate:a " + viewRangeArray!!.get(0) + " ==? " + viewRangeArray!!.get(6))
            if (viewRangeArray!!.get(0) == viewRangeArray!!.get(6)) {
                viewRangeArray!!.set(
                    0,
                    0.0
                )
            }
        }
        when (buttonId) {
            R.id.btnAnalyzerSampleRate -> {
                analyzerViews!!.popupMenuSampleRate.dismiss()
                if (!isLockViewRange) {
                    i("set sampling rate:b " + viewRangeArray!![1] + " ==? " + viewRangeArray!![6 + 1])
                    if (viewRangeArray!![1] == viewRangeArray!![6 + 1]) {
                        viewRangeArray!![1] = (selectedItemTag.toInt() / 2).toDouble()
                    }
                    i("onItemClick(): viewRangeArray saved. " + viewRangeArray!![0] + " ~ " + viewRangeArray!![1])
                }
                params!!.sampleRate = selectedItemTag.toInt()
                b_need_restart_audio = true
                editor.putInt(
                    "button_sample_rate",
                    params!!.sampleRate
                )
            }
            R.id.btnAnalyzerFFTLength -> {
                analyzerViews!!.popupMenuFFTLen.dismiss()
                params!!.fftLength = selectedItemTag.toInt()
                params!!.hopLength = (params!!.fftLength * (1 - params!!.overlapPercent / 100) + 0.5).toInt()
                b_need_restart_audio = true
                editor.putInt(
                    "button_fftlen",
                    params!!.fftLength
                )
                fillFftCalibration(
                    params,
                    calibLoad
                )
            }
            R.id.btnAnalyzerAverage -> {
                analyzerViews!!.popupMenuFFTAverage.dismiss()
                params!!.nFftAverage = selectedItemTag.toInt()
                if (analyzerViews!!.agv != null) {
                    analyzerViews!!.agv.setTimeMultiplier(params!!.nFftAverage)
                }
                b_need_restart_audio = false
                editor.putInt(
                    "button_average",
                    params!!.nFftAverage
                )
            }
            else -> {
                w("onItemClick(): no this button")
                b_need_restart_audio = false
            }
        }
        editor.apply()
        if (b_need_restart_audio) {
            restartSampling(params)
        }
    }

    // Load preferences for Views
    // When this function is called, the SamplingLoopThread must not running in the meanwhile.
    private fun loadPreferenceForView() {
        // load preferences for buttons
        // list-buttons
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
        params!!.sampleRate = sharedPref.getInt(
            "button_sample_rate",
            8000
        )
        params!!.fftLength = sharedPref.getInt(
            "button_fftlen",
            1024
        )
        params!!.nFftAverage = sharedPref.getInt(
            "button_average",
            1
        )
        // toggle-buttons
        params!!.dbaWeighting = sharedPref.getBoolean(
            "dbA",
            false
        )
        if (params!!.dbaWeighting) {
            (rootView!!.findViewById<View>(R.id.tvAnalyzerDBDBA) as SelectorText).nextValue()
        }
        val isSpam = sharedPref.getBoolean(
            "spectrum_spectrogram_mode",
            true
        )
        if (!isSpam) {
            (rootView!!.findViewById<View>(R.id.tvAnalyzerSpectrumSpectrogramMode) as SelectorText).nextValue()
        }
        val axisMode = sharedPref.getString(
            "freq_scaling_mode",
            "linear"
        )
        val st = rootView!!.findViewById<View>(R.id.tvAnalyzerLinearLogNote) as SelectorText
        st.value = axisMode
        i(
            """${Thread.currentThread().stackTrace[2].methodName}:
  sampleRate  = ${params!!.sampleRate}
  fFTLength      = ${params!!.fftLength}
  nFFTAverage = ${params!!.nFftAverage}"""
        )
        (rootView!!.findViewById<View>(R.id.btnAnalyzerSampleRate) as Button).text =
            Integer.toString(params!!.sampleRate)
        (rootView!!.findViewById<View>(R.id.btnAnalyzerFFTLength) as Button).text =
            Integer.toString(params!!.fftLength)
        (rootView!!.findViewById<View>(R.id.btnAnalyzerAverage) as Button).text = Integer.toString(params!!.nFftAverage)
    }

    private fun LoadPreferences() {
        // Load preferences for recorder and views, beside loadPreferenceForView()
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
        val keepScreenOn = sharedPref.getBoolean(
            "keepScreenOn",
            true
        )
        if (keepScreenOn) {
            activity!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        params!!.audioSourceId = sharedPref.getString(
            "audioSource",
            Integer.toString(AnalyzerParams.RECORDER_AGC_OFF)
        )!!.toInt()
        params!!.windowFunctionName = sharedPref.getString(
            "windowFunction",
            "Hanning"
        )
        params!!.spectrogramDuration = sharedPref.getString(
            "spectrogramDuration",
            java.lang.Double.toString(6.0)
        )!!.toDouble()
        params!!.overlapPercent = sharedPref.getString(
            "fft_overlap_percent",
            "50.0"
        )!!.toDouble()
        params!!.hopLength = (params!!.fftLength * (1 - params!!.overlapPercent / 100) + 0.5).toInt()

        // Settings of graph view
        // spectrum
        analyzerViews!!.agv.setShowLines(
            sharedPref.getBoolean(
                "showLines",
                false
            )
        )
        // set spectrum show range
        analyzerViews!!.agv.setSpectrumDBLowerBound(
            sharedPref.getString(
                "spectrumRange",
                java.lang.Double.toString(AnalyzerGraphicView.MIN_DB)
            )!!.toFloat().toDouble()
        )

        // spectrogram
        analyzerViews!!.agv.setSpectrogramModeShifting(
            sharedPref.getBoolean(
                "spectrogramShifting",
                false
            )
        )
        analyzerViews!!.agv.setShowTimeAxis(
            sharedPref.getBoolean(
                "spectrogramTimeAxis",
                true
            )
        )
        analyzerViews!!.agv.setShowFreqAlongX(
            sharedPref.getBoolean(
                "spectrogramShowFreqAlongX",
                true
            )
        )
        analyzerViews!!.agv.setSmoothRender(
            sharedPref.getBoolean(
                "spectrogramSmoothRender",
                false
            )
        )
        analyzerViews!!.agv.setColorMap(
            sharedPref.getString(
                "spectrogramColorMap",
                "Hot"
            )
        )
        // set spectrogram show range
        analyzerViews!!.agv.setSpectrogramDBLowerBound(
            sharedPref.getString(
                "spectrogramRange",
                java.lang.Double.toString(analyzerViews!!.agv.spectrogramPlot.spectrogramBMP.getdBLowerBound())
            )!!.toFloat().toDouble()
        )
        analyzerViews!!.agv.setLogAxisMode(
            sharedPref.getBoolean(
                "spectrogramLogPlotMethod",
                true
            )
        )
        analyzerViews!!.isWarnOverrun = sharedPref.getBoolean(
            "warnOverrun",
            false
        )
        analyzerViews!!.setFpsLimit(
            sharedPref.getString(
                "spectrogramFPS",
                getString(R.string.spectrogram_fps_default)
            )!!.toDouble()
        )

        // Apply settings by travel the views with android:tag="select":
        visit(
            analyzerViews!!.agv.rootView as ViewGroup,
            object : Visit {
                override fun exec(v: View) {
                    processClick(v)
                }
            },
            "select"
        )

        // Get view range setting
        val isLock = sharedPref.getBoolean(
            "view_range_lock",
            false
        )
        if (isLock) {
            i(Thread.currentThread().stackTrace[2].methodName + ": isLocked")
            // Set view range and stick to measure mode
            var rr: DoubleArray? = DoubleArray(AnalyzerGraphicView.VIEW_RANGE_DATA_LENGTH)
            for (i in rr!!.indices) {
                rr[i] = AnalyzerUtil.getDouble(
                    sharedPref,
                    "view_range_rr_$i",
                    0.0 / 0.0
                )
                if (java.lang.Double.isNaN(rr[i])) {  // not properly initialized
                    w(Thread.currentThread().stackTrace[2].methodName + ": rr is not properly initialized")
                    rr = null
                    break
                }
            }
            if (rr != null) {
                viewRangeArray = rr
            }
            stickToMeasureMode()
        } else {
            stickToMeasureModeCancel()
        }
    }

    fun stickToMeasureMode() {
        isLockViewRange = true
        switchMeasureAndScaleMode() // Force set to Measure mode
    }

    fun stickToMeasureModeCancel() {
        isLockViewRange = false
        if (isMeasure) {
            switchMeasureAndScaleMode() // Force set to ScaleMode
        }
    }

    private fun isInGraphView(
        x: Float,
        y: Float
    ): Boolean {
        analyzerViews!!.agv.getLocationInWindow(windowLocation)
        return x >= windowLocation[0] && y >= windowLocation[1] && x < windowLocation[0] + analyzerViews!!.agv.width && y < windowLocation[1] + analyzerViews!!.agv.height
    }

    fun getMaxAmplitudeFreq(): Double {
        return maxAmplitudeFreq
    }

    fun setMaxAmplitudeFreq(maxAmplitudeFreq: Double) {
        this.maxAmplitudeFreq = maxAmplitudeFreq
    }

    /**
     * Gesture Listener for graphView (and possibly other views)
     * How to attach these events to the graphView?
     *
     * @author xyy
     */
    private inner class AnalyzerGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(event: MotionEvent): Boolean {  // enter here when down action happen
            flyingMoveHandler.removeCallbacks(flyingMoveRunnable)
            return true
        }

        override fun onLongPress(event: MotionEvent) {
            if (isInGraphView(
                    event.getX(0),
                    event.getY(0)
                )
            ) {
                if (!isMeasure) {  // go from "scale" mode to "marker" mode
                    switchMeasureAndScaleMode()
                }
            }
            measureEvent(event) // force insert this event
        }

        override fun onDoubleTap(event: MotionEvent): Boolean {
            if (!isMeasure) {
                scaleEvent(event) // ends scale mode
                analyzerViews!!.agv.resetViewScale()
            }
            return true
        }

        override fun onFling(
            event1: MotionEvent,
            event2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (isMeasure) {
                // seems never reach here...
                return true
            }
            // Fly the canvas in graphView when in scale mode
            shiftingVelocity = Math.sqrt(velocityX * velocityX + velocityY * velocityY.toDouble())
            shiftingComponentX = velocityX / shiftingVelocity
            shiftingComponentY = velocityY / shiftingVelocity
            val DPRatio = resources.displayMetrics.density
            flyAcceleration = 1200 * DPRatio.toDouble()
            timeFlingStart = SystemClock.uptimeMillis()
            flyingMoveHandler.postDelayed(
                flyingMoveRunnable,
                0
            )
            return true
        }

        var flyingMoveHandler = Handler()
        var timeFlingStart // Prevent from running forever
            : Long = 0
        var flyDt = 1 / 20.0 // delta t of refresh
        var shiftingVelocity // fling velocity
            = 0.0
        var shiftingComponentX // fling direction x
            = 0.0
        var shiftingComponentY // fling direction y
            = 0.0
        var flyAcceleration = 1200.0 // damping acceleration of fling, pixels/second^2
        var flyingMoveRunnable: Runnable = object : Runnable {
            override fun run() {
                var shiftingVelocityNew = shiftingVelocity - flyAcceleration * flyDt
                if (shiftingVelocityNew < 0) shiftingVelocityNew = 0.0
                // Number of pixels that should move in this time step
                val shiftingPixel = (shiftingVelocityNew + shiftingVelocity) / 2 * flyDt
                shiftingVelocity = shiftingVelocityNew
                if (shiftingVelocity > 0f
                    && SystemClock.uptimeMillis() - timeFlingStart < 10000
                ) {
                    // KLog.Companion.i("  fly pixels x=" + shiftingPixelX + " y=" + shiftingPixelY);
                    val graphView = analyzerViews!!.agv
                    graphView.xShift = graphView.xShift - shiftingComponentX * shiftingPixel / graphView.canvasWidth / graphView.xZoom
                    graphView.yShift = graphView.yShift - shiftingComponentY * shiftingPixel / graphView.canvasHeight / graphView.yZoom
                    // Am I need to use runOnUiThread() ?
                    analyzerViews!!.invalidateGraphView()
                    flyingMoveHandler.postDelayed(
                        this,
                        (1000 * flyDt).toLong()
                    )
                }
            }
        }
    }

    private fun switchMeasureAndScaleMode() {
        if (isLockViewRange) {
            isMeasure = true
            return
        }
        isMeasure = !isMeasure
    }

    /**
     * Manage marker for measurement.
     */
    private fun measureEvent(event: MotionEvent) {
        when (event.pointerCount) {
            1 -> analyzerViews!!.agv.setMarker(
                event.x,
                event.y
            )
            2 -> if (isInGraphView(
                    event.getX(1),
                    event.getY(1)
                )
            ) {
                switchMeasureAndScaleMode()
            }
        }
    }

    private var isPinching = false
    private var xShift0 = INIT
    private var yShift0 = INIT
    private var x0 = 0.0
    private var y0 = 0.0
    private val windowLocation = IntArray(2)
    private fun scaleEvent(event: MotionEvent) {
        if (event.action != MotionEvent.ACTION_MOVE) {
            xShift0 = INIT
            yShift0 = INIT
            isPinching = false
            // KLog.Companion.i("scaleEvent(): Skip event " + event.getAction());
            return
        }
        // KLog.Companion.i("scaleEvent(): switch " + event.getAction());
        val graphView = analyzerViews!!.agv
        when (event.pointerCount) {
            2 -> {
                if (isPinching) {
                    graphView.setShiftScale(
                        event.getX(0).toDouble(),
                        event.getY(0).toDouble(),
                        event.getX(1).toDouble(),
                        event.getY(1).toDouble()
                    )
                } else {
                    graphView.setShiftScaleBegin(
                        event.getX(0).toDouble(),
                        event.getY(0).toDouble(),
                        event.getX(1).toDouble(),
                        event.getY(1).toDouble()
                    )
                }
                isPinching = true
            }
            1 -> {
                val x = event.getX(0)
                val y = event.getY(0)
                graphView.getLocationInWindow(windowLocation)
                // KLog.Companion.i("scaleEvent(): xy=" + x + " " + y + "  wc = " + wc[0] + " " + wc[1]);
                if (isPinching || xShift0 == INIT) {
                    xShift0 = graphView.xShift
                    x0 = x.toDouble()
                    yShift0 = graphView.yShift
                    y0 = y.toDouble()
                } else {
                    // when close to the axis, scroll that axis only
                    if (x0 < windowLocation[0] + 50) {
                        graphView.yShift = yShift0 + (y0 - y) / graphView.canvasHeight / graphView.yZoom
                    } else if (y0 < windowLocation[1] + 50) {
                        graphView.xShift = xShift0 + (x0 - x) / graphView.canvasWidth / graphView.xZoom
                    } else {
                        graphView.xShift = xShift0 + (x0 - x) / graphView.canvasWidth / graphView.xZoom
                        graphView.yShift = yShift0 + (y0 - y) / graphView.canvasHeight / graphView.yZoom
                    }
                }
                isPinching = false
            }
            else -> i("Invalid touch count")
        }
    }

    override fun onLongClick(view: View): Boolean {
        vibrate(300)
        i("long click: $view")
        return true
    }

    // Responds to layout with android:tag="select"
    // Called from SelectorText.super.performClick()
    override fun onClick(v: View) {
        if (processClick(v)) {
            restartSampling(params)
        }
        analyzerViews!!.invalidateGraphView()
    }

    private val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1 // just a number
    private val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2
    var graphInit: Thread? = null
    private var bSamplingPreparation = false
    private fun restartSampling(params: AnalyzerParams?) {
        // Stop previous sampler if any.
        if (samplingThread != null) {
            samplingThread!!.finish()
            try {
                samplingThread!!.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            samplingThread = null
        }
        if (viewRangeArray != null) {
            analyzerViews!!.agv.setupAxes(this.params)
            val rangeDefault = analyzerViews!!.agv.viewPhysicalRange
            i(Thread.currentThread().stackTrace[2].methodName + ": setViewRange: " + viewRangeArray!![0] + " ~ " + viewRangeArray!![1])
            analyzerViews!!.agv.setViewRange(
                viewRangeArray,
                rangeDefault
            )
            if (!isLockViewRange) viewRangeArray = null // do not conserve
        }

        // Set the view for incoming data
        graphInit = Thread { analyzerViews!!.setupView(params) }
        graphInit!!.start()

        // Check and request permissions
        if (!checkAndRequestPermissions()) return
        if (!bSamplingPreparation) return

        // Start sampling
        samplingThread = SamplingLoopThread(
            params!!,
            analyzerViews!!,
            tvAnalyzerRunStop.value == "stop",
            saveWav,
            object : SamplingLoopThread.Listener {
                override fun onInitGraphs() {
                    try {
                        graphInit!!.join() // TODO: Seems not working as intended
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }

                override fun onUpdateAmplitude(
                    maxAmplitudeFreq: Double,
                    maxAmplitudeDB: Double
                ) {
                    this@AnalyzerFragment.maxAmplitudeFreq = maxAmplitudeFreq
                    maxAmpDB = maxAmplitudeDB
                }

                override fun onUpdateRms(
                    rms: Double,
                    rmsFromFT: Double
                ) {
                    dtRMS = rms
                    dtRMSFromFT = rmsFromFT
                }
            })
        samplingThread!!.start()
    }

    // For call requestPermissions() after each showPermissionExplanation()
    private var count_permission_explanation = 0

    // For preventing infinity loop: onResume() -> requestPermissions() -> onRequestPermissionsResult() -> onResume()
    private var count_permission_request = 0

    // Test and try to gain permissions.
    // Return true if it is OK to proceed.
    // Ref.
    //   https://developer.android.com/training/permissions/requesting.html
    //   https://developer.android.com/guide/topics/permissions/requesting.html
    private fun checkAndRequestPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(
                activity!!,
                Manifest.permission.RECORD_AUDIO
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            w("Permission RECORD_AUDIO denied. Trying  to request...")
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity!!,
                    Manifest.permission.RECORD_AUDIO
                ) &&
                count_permission_explanation < 1
            ) {
                w("  Show explanation here....")
                analyzerViews!!.showPermissionExplanation(R.string.permission_explanation_recorder)
                count_permission_explanation++
            } else {
                w("  Requesting...")
                if (count_permission_request < 3) {
                    ActivityCompat.requestPermissions(
                        activity!!,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        MY_PERMISSIONS_REQUEST_RECORD_AUDIO
                    )
                    count_permission_explanation = 0
                    count_permission_request++
                } else {
                    activity!!.runOnUiThread {
                        val context = context!!.applicationContext
                        val text = "Permission denied."
                        val toast = Toast.makeText(
                            context,
                            text,
                            Toast.LENGTH_LONG
                        )
                        toast.show()
                    }
                }
            }
            return false
        }
        if (saveWav &&
            ContextCompat.checkSelfPermission(
                activity!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            w("Permission WRITE_EXTERNAL_STORAGE denied. Trying  to request...")
            (rootView!!.findViewById<View>(R.id.tvAnalyzerMonitorRecord) as SelectorText).nextValue()
            saveWav = false
            analyzerViews!!.enableSaveWavView(saveWav)
            //      ((SelectorText) findViewById(R.id.tvAnalyzerRecording)).performClick();
            ActivityCompat.requestPermissions(
                activity!!,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
            )
            // Still possible to proceed with saveWav == false
            // simulate a view click, so that saveWav = false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_RECORD_AUDIO -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    w("RECORD_AUDIO Permission granted by user.")
                } else {
                    w("RECORD_AUDIO Permission denied by user.")
                }
            }
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    w("WRITE_EXTERNAL_STORAGE Permission granted by user.")
                    if (!saveWav) {
                        w("... saveWav == true")
                        activity!!.runOnUiThread {
                            (rootView!!.findViewById<View>(R.id.tvAnalyzerMonitorRecord) as SelectorText).nextValue()
                            saveWav = true
                            analyzerViews!!.enableSaveWavView(saveWav)
                        }
                    } else {
                        w("... saveWav == false")
                    }
                } else {
                    w("WRITE_EXTERNAL_STORAGE Permission denied by user.")
                }
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
    fun processClick(v: View): Boolean {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
        val editor = sharedPref.edit()
        val value: String
        value = if (v is SelectorText) {
            v.value
        } else {
            (v as TextView).text.toString()
        }
        return when (v.id) {
            R.id.tvAnalyzerMonitorRecord -> {
                saveWav = value == "Rec"
                //  SelectorText st = (SelectorText) findViewById(R.id.run);
                //  if (saveWav && ! st.getText().toString().equals("stop")) {
                //    st.nextValue();
                //    if (samplingThread != null) {
                //      samplingThread.setPaused(true);
                //    }
                //  }
                analyzerViews!!.enableSaveWavView(saveWav)
                true
            }
            R.id.tvAnalyzerRunStop -> {
                val pause = value == "stop"
                if (samplingThread != null && samplingThread!!.paused != pause) {
                    samplingThread!!.paused = pause
                }
                analyzerViews!!.agv.spectrogramPlot.setPause(pause)
                false
            }
            R.id.tvAnalyzerLinearLogNote -> {
                d(Thread.currentThread().stackTrace[2].methodName + " freq_scaling_mode = " + value)
                analyzerViews!!.agv.setAxisModeLinear(value)
                editor.putString(
                    "freq_scaling_mode",
                    value
                )
                editor.apply()
                false
            }
            R.id.tvAnalyzerDBDBA -> {
                params!!.dbaWeighting = value != "dB"
                if (samplingThread != null) {
                    samplingThread!!.setDbaWeighting(params!!.dbaWeighting)
                }
                editor.putBoolean(
                    "dbA",
                    params!!.dbaWeighting
                )
                editor.commit()
                false
            }
            R.id.tvAnalyzerSpectrumSpectrogramMode -> {
                if (value == "spum") {
                    analyzerViews!!.agv.switch2Spectrum()
                } else {
                    analyzerViews!!.agv.switch2Spectrogram()
                }
                editor.putBoolean(
                    "spectrum_spectrogram_mode",
                    value == "spum"
                )
                editor.commit()
                false
            }
            else -> true
        }
    }

    private fun vibrate(ms: Int) {
//        ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(ms);
    }

    /**
     * Visit all subviews of this view group and run command
     *
     * @param group  The parent view group
     * @param cmd    The command to run for each view
     * @param select The tag value that must match. Null implies all views
     */
    private fun visit(
        group: ViewGroup,
        cmd: Visit,
        select: String
    ) {
        exec(
            group,
            cmd,
            select
        )
        for (i in 0 until group.childCount) {
            val c = group.getChildAt(i)
            if (c is ViewGroup) {
                visit(
                    c,
                    cmd,
                    select
                )
            } else {
                exec(
                    c,
                    cmd,
                    select
                )
            }
        }
    }

    private fun exec(
        v: View,
        cmd: Visit,
        select: String?
    ) {
        if (select == null || select == v.tag) {
            cmd.exec(v)
        }
    }

    /**
     * Interface for view hierarchy visitor
     */
    internal interface Visit {
        fun exec(view: View)
    }

    /**
     * The graph view size has been determined.
     * By the way, the the labels are updated accordingly.
     */
    override fun ready() {
        d(Thread.currentThread().stackTrace[2].methodName + " " + hashCode())
        analyzerViews!!.invalidateGraphView()
    }

    fun loadCoin(item: Coin) {
        tvAnalyzerCheckerCoinName.text = item.name
        tvAnalyzerCheckerCoinPlace.text = item.place
        fabAnalyzerChecker.setImageResource(item.head)
    }

    companion object {
        val TAG = AnalyzerFragment::class.java.simpleName

        // for pass audioSourceIDs and audioSourcesEntries to MyPreferencesActivity
        @JvmField
        val MY_PREFERENCES_MSG_SOURCE_ID = TAG + ".SOURCE_ID"

        @JvmField
        val MY_PREFERENCES_MSG_SOURCE_NAME = TAG + ".SOURCE_NAME"
        fun newInstance(): AnalyzerFragment {
            return AnalyzerFragment()
        }

        const val REQUEST_AUDIO_GET = 1
        const val REQUEST_CALIB_LOAD = 2
        fun getMimeType(url: String?): String? {
            var type: String? = null
            val extension = MimeTypeMap.getFileExtensionFromUrl(url)
            if (extension != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
            return type
        }

        /**
         * Manage scroll and zoom
         */
        private const val INIT = Double.MIN_VALUE
    }
}
