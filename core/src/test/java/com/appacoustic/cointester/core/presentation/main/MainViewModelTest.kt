package com.appacoustic.cointester.core.presentation.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.appacoustic.cointester.core.presentation.analyzer.domain.AnalyzerParams
import com.appacoustic.cointester.coreAnalytics.AnalyticsTrackerComponent
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val analyzerParams = mockk<AnalyzerParams>()
    val analyticsTrackerComponent = mockk<AnalyticsTrackerComponent>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `at start, navigate to the analyzer`() {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val event = viewModel.viewEvents.tryReceive().getOrNull()
        assertTrue(event is MainViewModel.ViewEvents.NavigateToAnalyzer)
    }

    private fun buildViewModel() = MainViewModel(analyticsTrackerComponent = analyticsTrackerComponent)
}
