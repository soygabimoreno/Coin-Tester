package com.appacoustic.cointester.presentation.main

import androidx.lifecycle.viewModelScope
import com.appacoustic.cointester.coreAnalytics.AnalyticsTrackerComponent
import com.appacoustic.cointester.libbase.viewmodel.StatelessBaseViewModel
import com.appacoustic.cointester.presentation.main.analytics.MainEvents
import kotlinx.coroutines.launch

class MainViewModel(
    private val analyticsTrackerComponent: AnalyticsTrackerComponent
) : StatelessBaseViewModel<
    MainViewModel.ViewEvents>() {

    init {
        analyticsTrackerComponent.trackEvent(MainEvents.ScreenMain)
        viewModelScope.launch {
            sendViewEvent(ViewEvents.NavigateToAnalyzer)
        }
    }

    sealed class ViewEvents {
        object NavigateToAnalyzer : ViewEvents()
    }
}
