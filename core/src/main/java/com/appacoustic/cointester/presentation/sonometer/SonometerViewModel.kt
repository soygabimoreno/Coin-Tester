package com.appacoustic.cointester.presentation.sonometer

import androidx.lifecycle.viewModelScope
import com.appacoustic.cointester.framework.sampling.SimpleSamplingLoopThread
import com.appacoustic.cointester.libFramework.extension.roundTo1Decimal
import com.appacoustic.cointester.libbase.viewmodel.BaseViewModel
import com.appacoustic.cointester.presentation.analyzer.domain.AnalyzerParams
import com.appacoustic.libprocessing.linearToDB
import kotlinx.coroutines.launch

class SonometerViewModel(
    private val analyzerParams: AnalyzerParams
) : BaseViewModel<
    SonometerViewModel.ViewState,
    SonometerViewModel.ViewEvents>() {

    init {
        updateViewState(ViewState.Content("foo"))
        initSimpleSamplingLoopThread()
    }

    private fun initSimpleSamplingLoopThread() {
        SimpleSamplingLoopThread(
            analyzerParams = analyzerParams,
            paused = false,
            saveWav = false,
            listener = object : SimpleSamplingLoopThread.Listener {
                override fun onUpdateRms(
                    rms: Double,
                    rmsFromFT: Double
                ) {
                    onUpdateRMS(rms)
                }
            }
        ).start()
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
