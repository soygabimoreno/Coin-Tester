package com.appacoustic.cointester.presentation.main

import androidx.lifecycle.viewModelScope
import com.appacoustic.cointester.libbase.viewmodel.StatelessBaseViewModel
import kotlinx.coroutines.launch

class MainViewModel(
) : StatelessBaseViewModel<
    MainViewModel.ViewEvents>() {

    init {
        viewModelScope.launch {
            sendViewEvent(ViewEvents.NavigateToAnalyzer)
        }
    }

    sealed class ViewEvents {
        object NavigateToAnalyzer : ViewEvents()
    }
}
