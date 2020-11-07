package com.appacoustic.cointester.presentation.analyzer

import com.appacoustic.cointester.libbase.viewmodel.BaseViewModel

class AnalyzerViewModel(
) : BaseViewModel<
    AnalyzerViewModel.ViewState,
    AnalyzerViewModel.ViewEvents>() {

    init {
        updateViewState(ViewState.Content("foo"))
    }

    sealed class ViewState {
        data class Content(val foo: String) : ViewState()
    }

    sealed class ViewEvents {
        object Foo : ViewEvents()
    }
}
