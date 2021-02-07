package com.appacoustic.cointester.core.presentation.analyzer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.preference.PreferenceManager
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import com.appacoustic.cointester.core.R
import com.appacoustic.cointester.core.databinding.FragmentAnalyzerBinding
import com.appacoustic.cointester.core.framework.AnalyzerUtil
import com.appacoustic.cointester.core.framework.sampling.SamplingLoopThread
import com.appacoustic.cointester.core.presentation.analyzer.domain.AnalyzerParams
import com.appacoustic.cointester.core.presentation.analyzer.view.AnalyzerGraphicView
import com.appacoustic.cointester.core.presentation.analyzer.view.AnalyzerGraphicView.OnReadyListener
import com.appacoustic.cointester.core.presentation.analyzer.view.AnalyzerViews
import com.appacoustic.cointester.core.presentation.analyzer.view.RangeViewDialogC
import com.appacoustic.cointester.libFramework.KLog
import com.appacoustic.cointester.libFramework.extension.exhaustive
import com.appacoustic.cointester.libFramework.extension.hideKeyboard
import com.appacoustic.cointester.libFramework.extension.isFilled
import com.appacoustic.cointester.libFramework.extension.setOnTextChangedListener
import com.appacoustic.cointester.libbase.fragment.BaseFragment
import kotlinx.android.synthetic.main.fragment_analyzer.*
import kotlinx.android.synthetic.main.fragment_analyzer.agv
import kotlinx.android.synthetic.main.fragment_legacy_analyzer.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class AnalyzerFragment : BaseFragment<
    FragmentAnalyzerBinding,
    AnalyzerViewModel.ViewState,
    AnalyzerViewModel.ViewEvents,
    AnalyzerViewModel
    >(),
    OnReadyListener {

    companion object {
        private const val STATE_DT_RMS = "STATE_DT_RMS"
        private const val STATE_DT_RMS_FROM_FT = "STATE_DT_RMS_FROM_FT"
        private const val STATE_MAX_AMPLITUDE_DB = "STATE_MAX_AMPLITUDE_DB"
        private const val STATE_MAX_AMPLITUDE_FREQUENCY = "STATE_MAX_AMPLITUDE_FREQUENCY"

        fun newInstance() = AnalyzerFragment()

        private const val MIN_VALUE = Double.MIN_VALUE
    }

    override val viewBinding: (LayoutInflater, ViewGroup?) -> FragmentAnalyzerBinding = { layoutInflater, viewGroup ->
        FragmentAnalyzerBinding.inflate(
            layoutInflater,
            viewGroup,
            false
        )
    }

    override val viewModel: AnalyzerViewModel by viewModel()

    override fun initUI() {
        initEditText()
        initFab()
    }

    private fun initEditText() {
        etCursor1.setText(AnalyzerViewModel.FREQUENCY_1.toString())
        viewModel.handleCursor1Changed(AnalyzerViewModel.FREQUENCY_1)

        etCursor1.setOnTextChangedListener { frequencyCharSequence ->
            val frequencyString = frequencyCharSequence.toString()
            if (frequencyString.isFilled()) {
                val frequency = frequencyString.toDouble()
                viewModel.handleCursor1Changed(frequency)
            }
        }
    }

    private fun initFab() {
        fabRefresh.setOnClickListener {
            restartSampling(analyzerParams)
        }
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
            is AnalyzerViewModel.ViewEvents.Cursor1Changed -> cursor1Changed(viewEvent.frequency)
        }.exhaustive
    }

    private fun cursor1Changed(frequency: Double) {
        agv.setMarkerFrequency(frequency)
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
        agv.setAxisModeLinear("log")

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
            agv = agv
        )

        // travel Views, and attach ClickListener to the views that contain android:tag="select"
        visit(
            agv.rootView as ViewGroup,
            object : Visit {
                override fun exec(view: View) {
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

        rootView.setOnTouchListener { v, motionEvent ->
            requireContext().hideKeyboard(v)
            if (motionEvent.actionMasked == MotionEvent.ACTION_UP) {
                v.performClick()
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        loadPreferences()
        agv.setReady(this) // TODO: move this earlier?

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

    private fun loadPreferenceForView() {
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

        KLog.i("sampleRate = ${analyzerParams.sampleRate}, fFTLength = ${analyzerParams.fftLength}, nFFTAverage = ${analyzerParams.nFftAverage}")
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
//                    processClick(v)
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
        return x >= windowLocation[0] && y >= windowLocation[1] && x < windowLocation[0] + agv.width && y < windowLocation[1] + agv.height
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

    private val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1 // just a number
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
            params = params,
            analyzerViews = analyzerViews,
            paused = false,
            saveWav = false,
            listener = object : SamplingLoopThread.Listener {
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
        return true
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
