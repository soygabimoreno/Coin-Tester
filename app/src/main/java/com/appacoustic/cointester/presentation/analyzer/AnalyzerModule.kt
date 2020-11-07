package com.appacoustic.cointester.presentation.analyzer

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val analyzerModule = module {
    scope(named<AnalyzerFragment>()) {
        viewModel {
            AnalyzerViewModel()
        }
    }
}
