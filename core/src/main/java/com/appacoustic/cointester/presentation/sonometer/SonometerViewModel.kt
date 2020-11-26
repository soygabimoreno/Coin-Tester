package com.appacoustic.cointester.presentation.sonometer

import androidx.lifecycle.viewModelScope
import com.appacoustic.cointester.libFramework.extension.roundTo1Decimal
import com.appacoustic.cointester.libbase.viewmodel.BaseViewModel
import com.appacoustic.libprocessing.linearToDB
import kotlinx.coroutines.launch

class SonometerViewModel() : BaseViewModel<
    SonometerViewModel.ViewState,
    SonometerViewModel.ViewEvents>() {

    init {
        updateViewState(ViewState.Content("foo"))
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
