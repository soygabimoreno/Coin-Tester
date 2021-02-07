package com.appacoustic.cointester.core.presentation.main

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import com.appacoustic.cointester.core.R
import com.appacoustic.cointester.core.databinding.ActivityMainBinding
import com.appacoustic.cointester.core.presentation.analyzer.AnalyzerFragment
import com.appacoustic.cointester.libFramework.extension.exhaustive
import com.appacoustic.cointester.libFramework.extension.navigateTo
import com.appacoustic.cointester.libbase.activity.StatelessBaseActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : StatelessBaseActivity<
    ActivityMainBinding,
    MainViewModel.ViewEvents,
    MainViewModel
    >() {

    companion object {
        fun launch(context: Context) {
            val intent = Intent(
                context,
                MainActivity::class.java
            )
            context.startActivity(intent)
        }
    }

    override val viewBinding: (LayoutInflater) -> ActivityMainBinding = {
        ActivityMainBinding.inflate(it)
    }

    override val viewModel: MainViewModel by viewModel()

    override fun initUI() {}

    override fun handleViewEvent(viewEvent: MainViewModel.ViewEvents) {
        when (viewEvent) {
            is MainViewModel.ViewEvents.NavigateToAnalyzer -> navigateToAnalyzer()
        }.exhaustive
    }

    private fun navigateToAnalyzer() {
        navigateTo(
            R.id.flContainer,
            AnalyzerFragment.newInstance()
        )
    }
}
