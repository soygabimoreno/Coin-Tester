package com.appacoustic.cointester.presentation.analyzer

import com.appacoustic.cointester.aaa.analyzer.SamplingLoopThread
import com.appacoustic.cointester.libbase.viewmodel.BaseViewModel

class AnalyzerViewModel(
) : BaseViewModel<
    AnalyzerViewModel.ViewState,
    AnalyzerViewModel.ViewEvents>() {

    init {
        updateViewState(ViewState.Content(samplingThread = null))
    }

    fun startSampling() {

        // TODO
    }

    fun finishSampling() {
        (getViewState() as ViewState.Content).samplingThread?.finish()
    }

    fun releaseSampling() {
        val samplingThread = (getViewState() as ViewState.Content).samplingThread
        if (samplingThread != null) {
            samplingThread.finish()
            try {
                samplingThread.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            updateViewState(
                (getViewState() as ViewState.Content).copy(
                    samplingThread = null
                )
            )
        }
    }

    fun setSamplingPaused(paused: Boolean) {
        val samplingThread = (getViewState() as ViewState.Content).samplingThread
        if (samplingThread != null && samplingThread.paused != paused) {
            samplingThread.paused = paused
            updateViewState(
                (getViewState() as ViewState.Content).copy(
                    samplingThread = samplingThread
                )
            )
        }
    }

    fun setSamplingDbaWeighting(dbaWeighting: Boolean) {
        val samplingThread = (getViewState() as ViewState.Content).samplingThread
        samplingThread?.setDbaWeighting(dbaWeighting)
        updateViewState(
            (getViewState() as ViewState.Content).copy(
                samplingThread = samplingThread
            )
        )
    }

    sealed class ViewState {
        data class Content(var samplingThread: SamplingLoopThread? = null) : ViewState()
    }

    sealed class ViewEvents {
        object Foo : ViewEvents()
    }
}
