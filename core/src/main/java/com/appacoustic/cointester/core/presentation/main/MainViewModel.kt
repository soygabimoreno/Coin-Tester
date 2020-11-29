package com.appacoustic.cointester.core.presentation.main

import androidx.lifecycle.viewModelScope
import com.appacoustic.cointester.core.presentation.main.analytics.MainEvents
import com.appacoustic.cointester.coreAnalytics.AnalyticsTrackerComponent
import com.appacoustic.cointester.libbase.viewmodel.StatelessBaseViewModel
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
