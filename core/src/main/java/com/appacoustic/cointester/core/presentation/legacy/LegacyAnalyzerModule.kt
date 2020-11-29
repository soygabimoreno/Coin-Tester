package com.appacoustic.cointester.core.presentation.legacy

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val legacyAnalyzerModule = module {
    scope(named<LegacyAnalyzerFragment>()) {
        viewModel {
            LegacyAnalyzerViewModel(analyzerParams = get())
        }
    }
}
