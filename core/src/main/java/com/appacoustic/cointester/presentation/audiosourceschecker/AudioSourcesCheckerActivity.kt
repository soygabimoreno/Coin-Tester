package com.appacoustic.cointester.presentation.audiosourceschecker

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.appacoustic.cointester.R
import com.appacoustic.cointester.aaa.analyzer.AnalyzerUtil
import kotlinx.android.synthetic.main.activity_audio_sources_checker.*
import java.util.*

/**
 * Check all (including unknown) recorder sources by open it and read data.
 */
class AudioSourcesCheckerActivity : AppCompatActivity() {

    companion object {
        private const val TEST_LEVEL_STANDARD_SOURCES = 1
        private const val TEST_LEVEL_SUPPORTED_SOURCES = 7
        private const val TEST_LEVEL_ALL_SOURCES = 0

        fun launch(
            context: Context
        ) {
            val intent = Intent(
                context,
                AudioSourcesCheckerActivity::class.java
            )
            context.startActivity(intent)
        }
    }

    private var analyzerUtil: AnalyzerUtil? = null
    private var testResultSt: CharSequence? = null

    @Volatile
    private var bShouldStop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_sources_checker)
        setupActionBar()
        analyzerUtil = AnalyzerUtil(this)
        testResultSt = null
        tvAudioSourcesChecker.movementMethod = ScrollingMovementMethod()
    }

    private fun setupActionBar() {
        if (actionBar != null) actionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(
            R.menu.menu_audio_sources_checker,
            menu
        )
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
            R.id.menuAudioSourcesCheckerStandardSources -> {
                bShouldStop = true
                runTest(
                    tvAudioSourcesChecker,
                    TEST_LEVEL_STANDARD_SOURCES
                )
            }
            R.id.menuAudioSourcesCheckerSupportedSources -> {
                bShouldStop = true
                runTest(
                    tvAudioSourcesChecker,
                    TEST_LEVEL_SUPPORTED_SOURCES
                )
            }
            R.id.menuAudioSourcesCheckerAllSources -> {
                bShouldStop = true
                runTest(
                    tvAudioSourcesChecker,
                    TEST_LEVEL_ALL_SOURCES
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if (testResultSt != null) {
            tvAudioSourcesChecker.text = testResultSt
            return
        }
        runTest(
            tvAudioSourcesChecker,
            TEST_LEVEL_SUPPORTED_SOURCES
        )
    }

    private var testerThread: Thread? = null
    private fun runTest(
        tv: TextView,
        testLevel: Int
    ) {
        Thread {
            if (testerThread != null) {
                try {
                    testerThread!!.join()
                } catch (e: InterruptedException) {
                    // ???
                }
            }
            testResultSt = null
            setTextData(
                tv,
                getString(R.string.checker_latency_indication)
            )
            bShouldStop = false
            testerThread = Thread {
                TestAudioRecorder(
                    tv,
                    testLevel
                )
            }
            testerThread!!.start()
        }.start()
    }

    private fun setTextData(
        tv: TextView,
        st: String
    ) {
        runOnUiThread { tv.text = st }
    }

    private fun appendTextData(
        tv: TextView,
        s: String
    ) {
        runOnUiThread { tv.append(s) }
    }

    // Show supported sample rate and corresponding minimum buffer size.
    private fun TestAudioRecorder(
        tv: TextView,
        testLevel: Int
    ) {
        // All possible sample rate
        var sampleRates = resources.getStringArray(R.array.std_sampling_rates)
        var st = getString(R.string.checker_latency_header)
        val resultMinBuffer = ArrayList<String>()
        for (rawSampleRate in sampleRates) {
            val sampleRate = rawSampleRate.trim { it <= ' ' }.toInt()
            val minBufSize = AudioRecord
                .getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
            st += if (minBufSize != AudioRecord.ERROR_BAD_VALUE) {
                resultMinBuffer.add(rawSampleRate)
                // /2.0 due to ENCODING_PCM_16BIT, CHANNEL_IN_MONO
                getString(
                    R.string.checker_latency_row,
                    sampleRate,
                    minBufSize,
                    1000.0 * minBufSize / 2.0 / sampleRate
                )
            } else {
                rawSampleRate + getString(R.string.checker_latency_error)
            }
        }
        sampleRates = resultMinBuffer.toTypedArray()
        appendTextData(
            tv,
            st
        )

        // Get audio source list
        val audioSourceId = analyzerUtil!!.GetAllAudioSource(testLevel)
        val audioSourceStringList = ArrayList<String>()
        for (id in audioSourceId) {
            audioSourceStringList.add(analyzerUtil!!.getAudioSourceName(id))
        }
        val audioSourceString = audioSourceStringList.toTypedArray()
        appendTextData(
            tv,
            getString(R.string.checker_sources_indication)
        )
        for (ias in audioSourceId.indices) {
            if (bShouldStop) return
            st = getString(
                R.string.checker_sources_header,
                audioSourceString[ias]
            )
            appendTextData(
                tv,
                st
            )
            for (sr in sampleRates) {
                if (bShouldStop) return
                val sampleRate = sr.trim { it <= ' ' }.toInt()
                val recBufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                st = getString(
                    R.string.checker_sources_freq,
                    sampleRate
                )

                // wait for AudioRecord fully released...
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                var record: AudioRecord?
                try {
                    record = AudioRecord(
                        audioSourceId[ias],
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        recBufferSize
                    )
                } catch (e: IllegalArgumentException) {
                    st += getString(R.string.checker_sources_error_illegal_argument)
                    record = null
                }
                if (record != null) {
                    if (record.state == AudioRecord.STATE_INITIALIZED) {
                        var numOfReadShort: Int
                        numOfReadShort = try {  // try read some samples.
                            record.startRecording()
                            val audioSamples = ShortArray(recBufferSize)
                            record.read(
                                audioSamples,
                                0,
                                recBufferSize
                            )
                        } catch (e: IllegalStateException) {
                            -1
                        }
                        st += if (numOfReadShort > 0) {
                            getString(R.string.checker_sources_succeed)
                        } else if (numOfReadShort == 0) {
                            getString(R.string.checker_sources_error_read_0_byte)
                        } else {
                            getString(R.string.checker_sources_error_read_sample)
                        }
                        val `as` = record.audioSource
                        if (`as` != audioSourceId[ias]) {  // audio source altered
                            st += getString(
                                R.string.checker_sources_alter_header,
                                analyzerUtil!!.getAudioSourceName(`as`)
                            )
                        }
                        record.stop()
                    } else {
                        st += getString(R.string.checker_sources_error_initialization)
                    }
                    record.release()
                }
                st += getString(R.string.checker_sources_end)
                appendTextData(
                    tv,
                    st
                )
            }
        }
        testResultSt = tv.text
    }
}
