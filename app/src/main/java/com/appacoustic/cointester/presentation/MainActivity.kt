package com.appacoustic.cointester.presentation

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.appacoustic.cointester.BuildConfig
import com.appacoustic.cointester.R
import com.appacoustic.cointester.aaa.analyzer.view.AnalyzerViews
import com.appacoustic.cointester.aaa.view.CustomViewPager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val N_TABS = 1
        const val CHECKER_POS = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val adapter = SectionsPagerAdapter(
            this,
            supportFragmentManager
        )
        viewPager.adapter = adapter
        viewPager.currentItem = CHECKER_POS
        tabs.setupWithViewPager(viewPager)
    }

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

    val vp: CustomViewPager
        get() = viewPager

    internal class SectionsPagerAdapter(
        private val context: Context,
        fm: FragmentManager?
    ) : FragmentPagerAdapter(fm!!) {
        override fun getItem(position: Int): Fragment {
            when (position) {
                CHECKER_POS -> return AnalyzerFragment.newInstance()
            }
            return AnalyzerFragment.newInstance()
        }

        override fun getCount(): Int {
            return N_TABS
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                CHECKER_POS -> return context.getString(R.string.checker)
            }
            return null
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
}
