package com.appacoustic.cointester.presentation.main

import android.app.AlertDialog
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.appacoustic.cointester.BuildConfig
import com.appacoustic.cointester.R
import com.appacoustic.cointester.aaa.analyzer.view.AnalyzerViews
import com.appacoustic.cointester.libFramework.extension.exhaustive
import com.appacoustic.cointester.libFramework.extension.navigateTo
import com.appacoustic.cointester.libbase.activity.StatelessBaseActivity
import com.appacoustic.cointester.presentation.analyzer.AnalyzerFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : StatelessBaseActivity<
    MainViewModel.ViewEvents,
    MainViewModel
    >() {

    override val layoutResId = R.layout.activity_main
    override val viewModel: MainViewModel by viewModel()

    override fun initUI() {}

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(
            R.menu.menu_main,
            menu
        )

        menu.findItem(R.id.menuMainInstructions).isVisible = BuildConfig.DEBUG
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuMainInstructions -> {
                showInstructions()
                true
            }

            R.id.menuMainPreferences -> //                Intent settings = new Intent(getBaseContext(), MyPreferencesActivity.class);
//                settings.putExtra(MY_PREFERENCES_MSG_SOURCE_ID, analyzerParam.audioSourceIDs);
//                settings.putExtra(MY_PREFERENCES_MSG_SOURCE_NAME, analyzerParam.audioSourcesEntries);
//                startActivity(settings);
                false
            R.id.menuMainAudioSourcesChecker -> //                Intent int_info_rec = new Intent(this, AudioSourcesCheckerActivity.class);
//                startActivity(int_info_rec);
                false
            R.id.menuMainRanges -> //                rangeViewDialogC.ShowRangeViewDialog();
                false
            R.id.menuMainCalibration -> //                selectFile(REQUEST_CALIB_LOAD);
                false
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showInstructions() {
        val tv = TextView(this)
        tv.movementMethod = LinkMovementMethod.getInstance()
        tv.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            15f
        )
        tv.text = AnalyzerViews.fromHtml(getString(R.string.instructions_text))
        AlertDialog.Builder(this)
            .setTitle(R.string.instructions_title)
            .setView(tv)
            .setNegativeButton(
                R.string.instructions_dismiss,
                null
            )
            .create()
            .show()
    }

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
