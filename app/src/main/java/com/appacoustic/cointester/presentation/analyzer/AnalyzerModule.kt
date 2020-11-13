package com.appacoustic.cointester.presentation.analyzer

import com.appacoustic.cointester.R
import com.appacoustic.cointester.coreinfrastructure.InfrastructureKeys
import com.appacoustic.cointester.presentation.analyzer.domain.AnalyzerParams
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val analyzerModule = module {
    single(named(InfrastructureKeys.AUDIO_SOURCE_NAMES)) { androidContext().resources.getStringArray(R.array.audio_sources_entries) }
    single(named(InfrastructureKeys.AUDIO_SOURCE_IDS_STRING)) { androidContext().resources.getStringArray(R.array.audio_source_ids) }
    single(named(InfrastructureKeys.WINDOW_FUNCTIONS)) { androidContext().resources.getStringArray(R.array.window_functions) }
    scope(named<AnalyzerFragment>()) {
        viewModel {
            AnalyzerViewModel(
                analyzerParams = AnalyzerParams(
                    audioSourceNames = get(named(InfrastructureKeys.AUDIO_SOURCE_NAMES)),
                    audioSourceIdsString = get(named(InfrastructureKeys.AUDIO_SOURCE_IDS_STRING)),
                    windowFunctionNames = get(named(InfrastructureKeys.WINDOW_FUNCTIONS)),
                    errorTrackerComponent = get()
                )
            )
        }
    }
}
