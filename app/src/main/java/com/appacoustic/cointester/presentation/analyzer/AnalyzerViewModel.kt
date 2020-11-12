package com.appacoustic.cointester.presentation.analyzer

import com.appacoustic.cointester.aaa.analyzer.SamplingLoopThread
import com.appacoustic.cointester.aaa.analyzer.model.AnalyzerParams
import com.appacoustic.cointester.libbase.viewmodel.BaseViewModel

class AnalyzerViewModel(
    val analyzerParams: AnalyzerParams
) : BaseViewModel<
    AnalyzerViewModel.ViewState,
    AnalyzerViewModel.ViewEvents>() {

    init {
        updateViewState(ViewState.Content("foo"))
    }

    var samplingThread: SamplingLoopThread? = null
        private set

    fun startSampling(samplingThread: SamplingLoopThread) {
        this.samplingThread = samplingThread
        samplingThread.start()
    }

    fun finishSampling() {
        samplingThread?.finish()
    }

    fun releaseSampling() {
        if (samplingThread != null) {
            samplingThread?.finish()
            try {
                samplingThread?.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            samplingThread = null
        }
    }

    fun setSamplingPaused(paused: Boolean) {
        if (samplingThread != null && samplingThread?.paused != paused) {
            samplingThread?.paused = paused
        }
    }

    fun setSamplingDbaWeighting(dbaWeighting: Boolean) {
        samplingThread?.setDbaWeighting(dbaWeighting)
    }

    sealed class ViewState {
        data class Content(var foo: String) : ViewState()
    }

    sealed class ViewEvents {
        object Foo : ViewEvents()
    }
}
