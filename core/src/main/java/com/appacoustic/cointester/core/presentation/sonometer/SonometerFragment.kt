package com.appacoustic.cointester.core.presentation.sonometer

import android.view.LayoutInflater
import android.view.ViewGroup
import com.appacoustic.cointester.core.databinding.FragmentSonometerBinding
import com.appacoustic.cointester.libFramework.extension.exhaustive
import com.appacoustic.cointester.libbase.fragment.BaseFragment
import kotlinx.android.synthetic.main.fragment_sonometer.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class SonometerFragment : BaseFragment<
    FragmentSonometerBinding,
    SonometerViewModel.ViewState,
    SonometerViewModel.ViewEvents,
    SonometerViewModel
    >() {

    companion object {
        fun newInstance() = SonometerFragment()
    }

    override val viewBinding: (LayoutInflater, ViewGroup?) -> FragmentSonometerBinding = { layoutInflater, viewGroup ->
        FragmentSonometerBinding.inflate(
            layoutInflater,
            viewGroup,
            false
        )
    }

    override val viewModel: SonometerViewModel by viewModel()

    override fun initUI() {
        viewModel.onUpdateRMS(0.001)
    }

    override fun renderViewState(viewState: SonometerViewModel.ViewState) {
        when (viewState) {
            is SonometerViewModel.ViewState.Content -> showContent()
        }.exhaustive
    }

    private fun showContent() {
//        debugToast("showContent")
    }

    override fun handleViewEvent(viewEvent: SonometerViewModel.ViewEvents) {
        when (viewEvent) {
            is SonometerViewModel.ViewEvents.UpdateRMS -> updateRMS(viewEvent.rmsString)
        }.exhaustive
    }

    private fun updateRMS(rmsString: String) {
        tvCustomRMS.text = rmsString
    }
}
