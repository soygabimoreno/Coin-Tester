package com.appacoustic.cointester.core.presentation.domain

import androidx.test.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appacoustic.cointester.core.R
import com.appacoustic.cointester.core.presentation.analyzer.domain.AnalyzerParams
import com.appacoustic.cointester.coreAnalytics.error.ErrorTrackerComponent
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalyzerParamsTest {

    private lateinit var audioSourceNames: Array<String>
    private lateinit var audioSourceIdsString: Array<String>
    private lateinit var windowFunctionNames: Array<String>
    private val errorTrackerComponent = mockk<ErrorTrackerComponent>(relaxed = true)

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getTargetContext()
        audioSourceNames = context.resources.getStringArray(R.array.audio_sources_entries)
        audioSourceIdsString = context.resources.getStringArray(R.array.audio_source_ids)
        windowFunctionNames = context.resources.getStringArray(R.array.window_functions)
    }

    @Test
    fun audioSourceName_is_filled() {
        val analyzerParams = buildAnalyzerParams()

        analyzerParams.audioSourceName

        verify(exactly = 0) { errorTrackerComponent.trackError(any()) }
    }

    @Test
    fun audioSourceName_gets_the_proper_value() {
        val analyzerParams = buildAnalyzerParams()

        val audioSourceName = analyzerParams.audioSourceName

        assertTrue("DEFAULT" == audioSourceName)
    }

    private fun buildAnalyzerParams() = AnalyzerParams(
        audioSourceNames = audioSourceNames,
        audioSourceIdsString = audioSourceIdsString,
        windowFunctionNames = windowFunctionNames,
        errorTrackerComponent = errorTrackerComponent
    )
}
