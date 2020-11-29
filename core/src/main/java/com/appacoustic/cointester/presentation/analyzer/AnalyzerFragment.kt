package com.appacoustic.cointester.presentation.analyzer

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.preference.PreferenceManager
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.*
import android.widget.AdapterView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import com.appacoustic.cointester.BuildConfig
import com.appacoustic.cointester.R
import com.appacoustic.cointester.framework.AnalyzerUtil
import com.appacoustic.cointester.framework.sampling.SamplingLoopThread
import com.appacoustic.cointester.libFramework.KLog
import com.appacoustic.cointester.libFramework.extension.exhaustive
import com.appacoustic.cointester.libbase.fragment.BaseFragment
import com.appacoustic.cointester.presentation.analyzer.domain.AnalyzerParams
import com.appacoustic.cointester.presentation.analyzer.view.AnalyzerGraphicView
import com.appacoustic.cointester.presentation.analyzer.view.AnalyzerGraphicView.OnReadyListener
import com.appacoustic.cointester.presentation.analyzer.view.AnalyzerViews
import com.appacoustic.cointester.presentation.analyzer.view.RangeViewDialogC
import com.appacoustic.cointester.presentation.analyzer.view.SelectorText
import com.appacoustic.cointester.presentation.audiosourceschecker.AudioSourcesCheckerActivity
import com.appacoustic.cointester.presentation.mypreference.MyPreferenceActivity
import com.appacoustic.libprocessingandroid.calibration.CalibrationLoad
import kotlinx.android.synthetic.main.fragment_analyzer.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class AnalyzerFragment : BaseFragment<
    AnalyzerViewModel.ViewState,
    AnalyzerViewModel.ViewEvents,
    AnalyzerViewModel
    >(),
    View.OnLongClickListener,
    View.OnClickListener,
    AdapterView.OnItemClickListener,
    OnReadyListener {

    companion object {
        private const val REQUEST_CODE_LOAD_CALIBRATION = 1001

        private const val STATE_DT_RMS = "STATE_DT_RMS"
        private const val STATE_DT_RMS_FROM_FT = "STATE_DT_RMS_FROM_FT"
        private const val STATE_MAX_AMPLITUDE_DB = "STATE_MAX_AMPLITUDE_DB"
        private const val STATE_MAX_AMPLITUDE_FREQUENCY = "STATE_MAX_AMPLITUDE_FREQUENCY"

        private const val REC = "Rec"
        private const val STOP = "stop"

        fun newInstance() = AnalyzerFragment()

        private const val MIN_VALUE = Double.MIN_VALUE
    }

    override val layoutResId = R.layout.fragment_analyzer
    override val viewModel: AnalyzerViewModel by viewModel()

    override fun initUI() {
    }

    override fun renderViewState(viewState: AnalyzerViewModel.ViewState) {
        when (viewState) {
            is AnalyzerViewModel.ViewState.Content -> showContent()
        }.exhaustive
    }

    private fun showContent() {
//        debugToast("showContent")
    }

    override fun handleViewEvent(viewEvent: AnalyzerViewModel.ViewEvents) {
        when (viewEvent) {
            is AnalyzerViewModel.ViewEvents.UpdateRMS -> updateRMS(viewEvent.rmsString)
        }.exhaustive
    }

    private fun updateRMS(rmsString: String) {
        tvCustomRMS.text = rmsString
    }

    private lateinit var rootView: View
    private lateinit var analyzerViews: AnalyzerViews

    private lateinit var rangeViewDialogC: RangeViewDialogC
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var analyzerParams: AnalyzerParams

    private var dtRMS = 0.0
    var dtRMSFromFT = 0.0
    var maxAmplitudeDB = 0.0
    private var maxAmplitudeFreq = 0.0

    var viewRangeArray: DoubleArray? = null
    private var isMeasure = false
    private var isLockViewRange = false

    @Volatile
    var saveWav = false
    private var calibrationLoad = CalibrationLoad()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(
            inflater,
            container,
            savedInstanceState
        )
        rootView = inflater.inflate(
            layoutResId,
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

        analyzerParams = viewModel.analyzerParams

        PreferenceManager.setDefaultValues(
            requireActivity(),
            R.xml.preferences,
            false
        )

        // Read preferences and set corresponding variables
        loadPreferenceForView()
        analyzerViews = AnalyzerViews(
            activity = requireActivity(),
            analyzerFragment = this,
            agv = agv
        )

        // travel Views, and attach ClickListener to the views that contain android:tag="select"
        visit(
            agv.rootView as ViewGroup,
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
            requireActivity(),
            this,
            agv
        )

        gestureDetector = GestureDetectorCompat(
            requireActivity(),
            AnalyzerGestureListener()
        )

        rootView.setOnTouchListener { v, event ->
            if (isInGraphView(
                    event.getX(0),
                    event.getY(0)
                )
            ) {
                gestureDetector.onTouchEvent(event)
                if (isMeasure) {
                    measureEvent(event)
                } else {
                    scaleEvent(event)
                }
                analyzerViews.invalidateGraphView()
                // Go to scaling mode when user release finger in measure mode.
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    v.performClick()
                    if (isMeasure) {
                        switchMeasureAndScaleMode()
                    }
                }
            } else {
                // When finger is outside the plot, hide the marker and go to scaling mode.
                if (isMeasure) {
                    agv.hideMarker()
                    switchMeasureAndScaleMode()
                }
            }
            true
        }
        btnSampleRate.setOnClickListener { v -> analyzerViews.showPopupMenu(v) }
        btnFFTLength.setOnClickListener { v -> analyzerViews.showPopupMenu(v) }
        btnAverage.setOnClickListener { v -> analyzerViews.showPopupMenu(v) }
    }

    override fun onResume() {
        super.onResume()
        loadPreferences()
        agv.setReady(this) // TODO: move this earlier?
        analyzerViews.enableSaveWavView(saveWav)

        // Used to prevent extra calling to restartSampling() (e.g. in LoadPreferences())
        bSamplingPreparation = true

        // Start sampling
        restartSampling(analyzerParams)
    }

    override fun onPause() {
        bSamplingPreparation = false
        viewModel.finishSampling()
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putDouble(
            STATE_DT_RMS,
            dtRMS
        )
        savedInstanceState.putDouble(
            STATE_DT_RMS_FROM_FT,
            dtRMSFromFT
        )
        savedInstanceState.putDouble(
            STATE_MAX_AMPLITUDE_DB,
            maxAmplitudeDB
        )
        savedInstanceState.putDouble(
            STATE_MAX_AMPLITUDE_FREQUENCY,
            maxAmplitudeFreq
        )
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) { // Equivalent to onRestoreInstanceState()
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            dtRMS = savedInstanceState.getDouble(STATE_DT_RMS)
            dtRMSFromFT = savedInstanceState.getDouble(STATE_DT_RMS_FROM_FT)
            maxAmplitudeDB = savedInstanceState.getDouble(STATE_MAX_AMPLITUDE_DB)
            maxAmplitudeFreq = savedInstanceState.getDouble("maxAmplitudeFreq")
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == REQUEST_CODE_LOAD_CALIBRATION && resultCode == Activity.RESULT_OK) {
            val uri = data!!.data!!
            calibrationLoad.loadFile(
                uri,
                requireActivity()
            )
            fillFftCalibration(
                analyzerParams,
                calibrationLoad
            )
        }
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        requireActivity().menuInflater.inflate(
            R.menu.menu_main,
            menu
        )
        menu.findItem(R.id.menuMainInstructions).isVisible = BuildConfig.DEBUG
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuMainInstructions -> {
                showInstructions()
                true
            }
            R.id.menuMainPreferences -> {
                MyPreferenceActivity.launch(
                    requireContext(),
                    analyzerParams.audioSourceIds,
                    analyzerParams.audioSourceNames
                )
                true
            }
            R.id.menuMainAudioSourcesChecker -> {
                AudioSourcesCheckerActivity.launch(requireContext())
                true
            }
            R.id.menuMainRanges -> {
                rangeViewDialogC.show()
                true
            }
            R.id.menuMainCalibration -> {
                selectFile()
                super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showInstructions() {
        val tv = TextView(requireContext())
        tv.movementMethod = LinkMovementMethod.getInstance()
        tv.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            15f
        )
        tv.text = AnalyzerViews.fromHtml(getString(R.string.instructions_text))
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.instructions_title)
            .setView(tv)
            .setNegativeButton(
                R.string.instructions_dismiss,
                null
            )
            .create()
            .show()
    }

    fun getSamplingThread(): SamplingLoopThread? = viewModel.samplingThread

    private fun selectFile() {
        // https://developer.android.com/guide/components/intents-common.html#Storage
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(
                intent,
                REQUEST_CODE_LOAD_CALIBRATION
            )
        } else {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(
                requireActivity(),
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
        val buttonView = rootView.findViewById<View>(buttonId) as Button
        buttonView.text = selectedItemText
        val b_need_restart_audio: Boolean

        // Save the choosen preference
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val editor = sharedPref.edit()

        // so change of sample rate do not change view range
        if (!isLockViewRange) {
            viewRangeArray = agv.viewPhysicalRange as DoubleArray
            // if range is align at boundary, extend the range.
            if (viewRangeArray!!.get(0) == viewRangeArray!!.get(6)) {
                viewRangeArray!!.set(
                    0,
                    0.0
                )
            }
        }
        when (buttonId) {
            R.id.btnSampleRate -> {
                analyzerViews.popupMenuSampleRate.dismiss()
                if (!isLockViewRange) {
                    if (viewRangeArray!![1] == viewRangeArray!![6 + 1]) {
                        viewRangeArray!![1] = (selectedItemTag.toInt() / 2).toDouble()
                    }
                }
                analyzerParams.sampleRate = selectedItemTag.toInt()
                b_need_restart_audio = true
                editor.putInt(
                    "button_sample_rate",
                    analyzerParams.sampleRate
                )
            }
            R.id.btnFFTLength -> {
                analyzerViews.popupMenuFFTLen.dismiss()
                analyzerParams.fftLength = selectedItemTag.toInt()
                analyzerParams.hopLength = (analyzerParams.fftLength * (1 - analyzerParams.overlapPercent / 100) + 0.5).toInt()
                b_need_restart_audio = true
                editor.putInt(
                    "button_fftlen",
                    analyzerParams.fftLength
                )
                fillFftCalibration(
                    analyzerParams,
                    calibrationLoad
                )
            }
            R.id.btnAverage -> {
                analyzerViews.popupMenuFFTAverage.dismiss()
                analyzerParams.nFftAverage = selectedItemTag.toInt()
                agv.setTimeMultiplier(analyzerParams.nFftAverage)
                b_need_restart_audio = false
                editor.putInt(
                    "button_average",
                    analyzerParams.nFftAverage
                )
            }
            else -> {
                b_need_restart_audio = false
            }
        }
        editor.apply()
        if (b_need_restart_audio) {
            restartSampling(analyzerParams)
        }
    }

    // Load preferences for Views
    // When this function is called, the SamplingLoopThread must not running in the meanwhile.
    private fun loadPreferenceForView() {
        // load preferences for buttons
        // list-buttons
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        analyzerParams.sampleRate = sharedPref.getInt(
            "button_sample_rate",
            44100
        )
        analyzerParams.fftLength = sharedPref.getInt(
            "button_fftlen",
            4096
        )
        analyzerParams.nFftAverage = sharedPref.getInt(
            "button_average",
            1
        )
        // toggle-buttons
        analyzerParams.dbaWeighting = sharedPref.getBoolean(
            "dbA",
            true
        )
        if (analyzerParams.dbaWeighting) {
            stDBDBA.nextValue()
        }
        val isSpam = sharedPref.getBoolean(
            "spectrum_spectrogram_mode",
            true
        )
        if (!isSpam) {
            stSpectrumSpectrogramMode.nextValue()
        }
        val axisMode = sharedPref.getString(
            "freq_scaling_mode",
            "linear"
        )
        stLinearLogNote.setValue(axisMode!!)
        KLog.i("sampleRate = ${analyzerParams.sampleRate}, fFTLength = ${analyzerParams.fftLength}, nFFTAverage = ${analyzerParams.nFftAverage}")
        btnSampleRate.text = analyzerParams.sampleRate.toString()
        btnFFTLength.text = analyzerParams.fftLength.toString()
        btnAverage.text = analyzerParams.nFftAverage.toString()
    }

    private fun loadPreferences() {
        // Load preferences for recorder and views, beside loadPreferenceForView()
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val keepScreenOn = sharedPref.getBoolean(
            "keepScreenOn",
            true
        )
        if (keepScreenOn) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        analyzerParams.audioSourceId = sharedPref.getString(
            "audioSource",
            Integer.toString(AnalyzerParams.RECORDER_AGC_OFF)
        )!!.toInt()
        analyzerParams.windowFunctionName = sharedPref.getString(
            "windowFunction",
            "Hanning"
        )
        analyzerParams.spectrogramDuration = sharedPref.getString(
            "spectrogramDuration",
            java.lang.Double.toString(6.0)
        )!!.toDouble()
        analyzerParams.overlapPercent = sharedPref.getString(
            "fft_overlap_percent",
            "50.0"
        )!!.toDouble()
        analyzerParams.hopLength = (analyzerParams.fftLength * (1 - analyzerParams.overlapPercent / 100) + 0.5).toInt()

        // Settings of graph view
        // spectrum
        agv.setShowLines(
            sharedPref.getBoolean(
                "showLines",
                false
            )
        )
        // set spectrum show range
        agv.setSpectrumDBLowerBound(
            sharedPref.getString(
                "spectrumRange",
                java.lang.Double.toString(AnalyzerGraphicView.MIN_DB)
            )!!.toFloat().toDouble()
        )

        // spectrogram
        agv.setSpectrogramModeShifting(
            sharedPref.getBoolean(
                "spectrogramShifting",
                false
            )
        )
        agv.setShowTimeAxis(
            sharedPref.getBoolean(
                "spectrogramTimeAxis",
                true
            )
        )
        agv.setShowFreqAlongX(
            sharedPref.getBoolean(
                "spectrogramShowFreqAlongX",
                true
            )
        )
        agv.setSmoothRender(
            sharedPref.getBoolean(
                "spectrogramSmoothRender",
                false
            )
        )
        agv.setColorMap(
            sharedPref.getString(
                "spectrogramColorMap",
                "Hot"
            )
        )
        // set spectrogram show range
        agv.setSpectrogramDBLowerBound(
            sharedPref.getString(
                "spectrogramRange",
                java.lang.Double.toString(agv.spectrogramPlot!!.spectrogramBMP.getdBLowerBound())
            )!!.toFloat().toDouble()
        )
        agv.setLogAxisMode(
            sharedPref.getBoolean(
                "spectrogramLogPlotMethod",
                true
            )
        )
        analyzerViews.isWarnOverrun = sharedPref.getBoolean(
            "warnOverrun",
            false
        )
        analyzerViews.setFpsLimit(
            sharedPref.getString(
                "spectrogramFPS",
                getString(R.string.spectrogram_fps_default)
            )!!.toDouble()
        )

        // Apply settings by travel the views with android:tag="select":
        visit(
            agv.rootView as ViewGroup,
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
            // Set view range and stick to measure mode
            var rr: DoubleArray? = DoubleArray(AnalyzerGraphicView.VIEW_RANGE_DATA_LENGTH)
            for (i in rr!!.indices) {
                rr[i] = AnalyzerUtil.getDouble(
                    sharedPref,
                    "view_range_rr_$i",
                    0.0 / 0.0
                )
                if (java.lang.Double.isNaN(rr[i])) {  // not properly initialized
                    rr = null
                    break
                }
            }
            viewRangeArray = rr
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
        agv.getLocationInWindow(windowLocation)
        return x >= windowLocation[0] && y >= windowLocation[1] && x < windowLocation[0] + (agv.width
            ?: 0) && y < windowLocation[1] + (agv.height
            ?: 0)
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
                agv.resetViewScale()
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
                    agv.xShift = agv.xShift - shiftingComponentX * shiftingPixel / agv.canvasWidth / agv.xZoom
                    agv.yShift = agv.yShift - shiftingComponentY * shiftingPixel / agv.canvasHeight / agv.yZoom
                    // Am I need to use runOnUiThread() ?
                    analyzerViews.invalidateGraphView()
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
            1 -> agv.setMarker(
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
    private var xShift0 = MIN_VALUE
    private var yShift0 = MIN_VALUE
    private var x0 = 0.0
    private var y0 = 0.0
    private val windowLocation = IntArray(2)
    private fun scaleEvent(event: MotionEvent) {
        if (event.action != MotionEvent.ACTION_MOVE) {
            xShift0 = MIN_VALUE
            yShift0 = MIN_VALUE
            isPinching = false
            // KLog.Companion.i("scaleEvent(): Skip event " + event.getAction());
            return
        }
        // KLog.Companion.i("scaleEvent(): switch " + event.getAction());
        when (event.pointerCount) {
            2 -> {
                if (isPinching) {
                    agv.setShiftScale(
                        event.getX(0).toDouble(),
                        event.getY(0).toDouble(),
                        event.getX(1).toDouble(),
                        event.getY(1).toDouble()
                    )
                } else {
                    agv.setShiftScaleBegin(
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
                agv.getLocationInWindow(windowLocation)
                // KLog.Companion.i("scaleEvent(): xy=" + x + " " + y + "  wc = " + wc[0] + " " + wc[1]);
                if (isPinching || xShift0 == MIN_VALUE) {
                    xShift0 = agv.xShift
                    x0 = x.toDouble()
                    yShift0 = agv.yShift
                    y0 = y.toDouble()
                } else {
                    // when close to the axis, scroll that axis only
                    if (x0 < windowLocation[0] + 50) {
                        agv.yShift = yShift0 + (y0 - y) / agv.canvasHeight / agv.yZoom
                    } else if (y0 < windowLocation[1] + 50) {
                        agv.xShift = xShift0 + (x0 - x) / agv.canvasWidth / agv.xZoom
                    } else {
                        agv.xShift = xShift0 + (x0 - x) / agv.canvasWidth / agv.xZoom
                        agv.yShift = yShift0 + (y0 - y) / agv.canvasHeight / agv.yZoom
                    }
                }
                isPinching = false
            }
            else -> KLog.i("Invalid touch count")
        }
    }

    override fun onLongClick(view: View): Boolean {
        vibrate(300)
        return true
    }

    // Responds to layout with android:tag="select"
    // Called from SelectorText.super.performClick()
    override fun onClick(v: View) {
        if (processClick(v)) {
            restartSampling(analyzerParams)
        }
        analyzerViews.invalidateGraphView()
    }

    private val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1 // just a number
    private val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2
    var graphInit: Thread? = null
    private var bSamplingPreparation = false

    private fun restartSampling(params: AnalyzerParams) {
        viewModel.releaseSampling()
        if (viewRangeArray != null) {
            agv.setupAxes(this.analyzerParams)
            val rangeDefault = agv.viewPhysicalRange
            agv.setViewRange(
                viewRangeArray,
                rangeDefault
            )
            if (!isLockViewRange) viewRangeArray = null // do not conserve
        }

        // Set the view for incoming data
        graphInit = Thread { analyzerViews.setupView(params) }
        graphInit!!.start()

        // Check and request permissions
        if (!checkAndRequestPermissions()) return
        if (!bSamplingPreparation) return

        // Start sampling
        val samplingThread = SamplingLoopThread(
            params,
            analyzerViews,
            stRunStop.value == STOP,
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
                    this@AnalyzerFragment.maxAmplitudeDB = maxAmplitudeDB
                }

                override fun onUpdateRms(
                    rms: Double,
                    rmsFromFT: Double
                ) {
                    viewModel.onUpdateRMS(rms)
                    dtRMS = rms
                    dtRMSFromFT = rmsFromFT
                }
            })
        viewModel.startSampling(samplingThread)
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
                requireActivity(),
                Manifest.permission.RECORD_AUDIO
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.RECORD_AUDIO
                ) &&
                count_permission_explanation < 1
            ) {
                analyzerViews.showPermissionExplanation(R.string.permission_explanation_recorder)
                count_permission_explanation++
            } else {
                if (count_permission_request < 3) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        MY_PERMISSIONS_REQUEST_RECORD_AUDIO
                    )
                    count_permission_explanation = 0
                    count_permission_request++
                } else {
                    requireActivity().runOnUiThread {
                        val context = requireContext().applicationContext
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
                requireActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            stMonitorRecord.nextValue()
            saveWav = false
            analyzerViews.enableSaveWavView(saveWav)
            //      ((SelectorText) findViewById(R.id.tvAnalyzerRecording)).performClick();
            ActivityCompat.requestPermissions(
                requireActivity(),
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
            }
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!saveWav) {
                        requireActivity().runOnUiThread {
                            stMonitorRecord.nextValue()
                            saveWav = true
                            analyzerViews.enableSaveWavView(saveWav)
                        }
                    }
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
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val editor = sharedPref.edit()
        val value: String
        value = if (v is SelectorText) {
            v.value!!
        } else {
            (v as TextView).text.toString()
        }
        return when (v.id) {
            R.id.stMonitorRecord -> {
                saveWav = value == REC
                //  SelectorText st = (SelectorText) findViewById(R.id.run);
                //  if (saveWav && ! st.getText().toString().equals(STOP)) {
                //    st.nextValue();
                //    if (samplingThread != null) {
                //      samplingThread.setPaused(true);
                //    }
                //  }
                analyzerViews.enableSaveWavView(saveWav)
                true
            }
            R.id.stRunStop -> {
                val paused = value == STOP
                viewModel.setSamplingPaused(paused)
                agv.spectrogramPlot?.setPause(paused)
                false
            }
            R.id.stLinearLogNote -> {
                agv.setAxisModeLinear(value)
                editor.putString(
                    "freq_scaling_mode",
                    value
                )
                editor.apply()
                false
            }
            R.id.stDBDBA -> {
                analyzerParams.dbaWeighting = value != "dB"
                viewModel.setSamplingDbaWeighting(analyzerParams.dbaWeighting)
                editor.putBoolean(
                    "dbA",
                    analyzerParams.dbaWeighting
                )
                editor.commit()
                false
            }
            R.id.stSpectrumSpectrogramMode -> {
                if (value == "spum") {
                    agv.switch2Spectrum()
                } else {
                    agv.switch2Spectrogram()
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
        analyzerViews.invalidateGraphView()
    }
}
