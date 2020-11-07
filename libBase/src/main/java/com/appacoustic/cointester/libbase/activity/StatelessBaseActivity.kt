package com.appacoustic.cointester.libbase.activity

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.lifecycle.lifecycleScope
import com.appacoustic.cointester.libbase.viewmodel.StatelessBaseViewModel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.scope.ScopeActivity

abstract class StatelessBaseActivity<
    VIEW_EVENT,
    VIEW_MODEL : StatelessBaseViewModel<VIEW_EVENT>
    > : ScopeActivity() {

    @get:LayoutRes
    protected abstract val layoutResId: Int
    protected abstract val viewModel: VIEW_MODEL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutResId)
        initUI()
        viewModel.viewEvents
            .consumeAsFlow()
            .onEach { handleViewEvent(it) }
            .launchIn(lifecycleScope)
    }

    protected abstract fun initUI()
    protected abstract fun handleViewEvent(viewEvent: VIEW_EVENT)
}
