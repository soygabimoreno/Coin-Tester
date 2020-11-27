package com.appacoustic.cointester.presentation.analyzer

import androidx.lifecycle.viewModelScope
import com.appacoustic.cointester.framework.sampling.SamplingLoopThread
import com.appacoustic.cointester.libFramework.extension.roundTo1Decimal
import com.appacoustic.cointester.libbase.viewmodel.BaseViewModel
import com.appacoustic.cointester.presentation.analyzer.domain.AnalyzerParams
import com.appacoustic.libprocessing.linearToDB
import kotlinx.coroutines.launch

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

    fun onUpdateRMS(rms: Double) {
        val diff = 95
        val rmsString = ((linearToDB(rms) + diff).roundTo1Decimal()).toString()
        viewModelScope.launch {
            sendViewEvent(ViewEvents.UpdateRMS(rmsString))
        }
    }

    sealed class ViewState {
        data class Content(var foo: String) : ViewState()
    }

    sealed class ViewEvents {
        data class UpdateRMS(val rmsString: String) : ViewEvents()
    }
}
