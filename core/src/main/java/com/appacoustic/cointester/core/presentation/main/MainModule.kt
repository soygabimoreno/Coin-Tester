package com.appacoustic.cointester.core.presentation.main

import com.appacoustic.cointester.core.R
import com.appacoustic.cointester.core.presentation.analyzer.domain.AnalyzerParams
import com.appacoustic.cointester.coreinfrastructure.InfrastructureKeys
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val mainModule = module {
    single(named(InfrastructureKeys.AUDIO_SOURCE_NAMES)) { androidContext().resources.getStringArray(R.array.audio_sources_entries) }
    single(named(InfrastructureKeys.AUDIO_SOURCE_IDS_STRING)) { androidContext().resources.getStringArray(R.array.audio_source_ids) }
    single(named(InfrastructureKeys.WINDOW_FUNCTIONS)) { androidContext().resources.getStringArray(R.array.window_functions) }
    single {
        AnalyzerParams(
            audioSourceNames = get(named(InfrastructureKeys.AUDIO_SOURCE_NAMES)),
            audioSourceIdsString = get(named(InfrastructureKeys.AUDIO_SOURCE_IDS_STRING)),
            windowFunctionNames = get(named(InfrastructureKeys.WINDOW_FUNCTIONS)),
            errorTrackerComponent = get()
        )
    }
    scope(named<MainActivity>()) {
        viewModel {
            MainViewModel(analyticsTrackerComponent = get())
        }
    }
}
