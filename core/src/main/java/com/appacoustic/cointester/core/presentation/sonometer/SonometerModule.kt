package com.appacoustic.cointester.core.presentation.sonometer

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val sonometerModule = module {
    scope(named<SonometerFragment>()) {
        viewModel {
            SonometerViewModel(analyzerParams = get())
        }
    }
}
