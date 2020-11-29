package com.appacoustic.cointester.presentation.analyzer

import androidx.lifecycle.viewModelScope
import com.appacoustic.cointester.framework.sampling.SamplingLoopThread
import com.appacoustic.cointester.libbase.viewmodel.BaseViewModel
import com.appacoustic.cointester.presentation.analyzer.domain.AnalyzerParams
import kotlinx.coroutines.launch

class AnalyzerViewModel(
    val analyzerParams: AnalyzerParams
) : BaseViewModel<
    AnalyzerViewModel.ViewState,
    AnalyzerViewModel.ViewEvents>() {

    companion object {
        const val FREQUENCY_1 = 1000.0
    }

    init {
        updateViewState(ViewState.Content("foo"))
    }

    var samplingLoopThread: SamplingLoopThread? = null
        private set

    fun startSampling(samplingLoopThread: SamplingLoopThread) {
        this.samplingLoopThread = samplingLoopThread
        samplingLoopThread.start()
    }

    fun finishSampling() {
        samplingLoopThread?.finish()
    }

    fun releaseSampling() {
        if (samplingLoopThread != null) {
            samplingLoopThread?.finish()
            try {
                samplingLoopThread?.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            samplingLoopThread = null
        }
    }

    fun handleCursor1Changed(frequency: Double) {
        viewModelScope.launch {
            sendViewEvent(ViewEvents.Cursor1Changed(frequency))
        }
    }

    sealed class ViewState {
        data class Content(var foo: String) : ViewState()
    }

    sealed class ViewEvents {
        data class Cursor1Changed(val frequency: Double) : ViewEvents()
    }
}
