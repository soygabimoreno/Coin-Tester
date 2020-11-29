package com.appacoustic.cointester.core.presentation.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.appacoustic.cointester.core.presentation.analyzer.domain.AnalyzerParams
import com.appacoustic.cointester.coreAnalytics.AnalyticsTrackerComponent
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
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

    private val testDispatcher = TestCoroutineDispatcher()
    private val testCoroutineScope = TestCoroutineScope(testDispatcher)

    private val analyzerParams = mockk<AnalyzerParams>()
    val analyticsTrackerComponent = mockk<AnalyticsTrackerComponent>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
        testCoroutineScope.cleanupTestCoroutines()
    }

    @Test
    fun `at start, navigate to the analyzer`() {
        val viewModel = buildViewModel()

        val event = viewModel.viewEvents.poll()
        assertTrue(event is MainViewModel.ViewEvents.NavigateToAnalyzer)
    }

    private fun buildViewModel() = MainViewModel(analyticsTrackerComponent = analyticsTrackerComponent)
}
