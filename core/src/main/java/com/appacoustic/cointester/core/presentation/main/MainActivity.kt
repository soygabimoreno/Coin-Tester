package com.appacoustic.cointester.core.presentation.main

import android.content.Context
import android.content.Intent
import com.appacoustic.cointester.core.R
import com.appacoustic.cointester.core.presentation.analyzer.AnalyzerFragment
import com.appacoustic.cointester.libFramework.extension.exhaustive
import com.appacoustic.cointester.libFramework.extension.navigateTo
import com.appacoustic.cointester.libbase.activity.StatelessBaseActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : StatelessBaseActivity<
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

    override val layoutResId = R.layout.activity_main
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
