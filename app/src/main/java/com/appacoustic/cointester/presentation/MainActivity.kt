package com.appacoustic.cointester.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.appacoustic.cointester.R
import com.appacoustic.cointester.aaa.view.CustomViewPager
import com.appacoustic.cointester.coredomain.Coin
import com.appacoustic.cointester.presentation.CoinsFragment.OnListFragmentInteractionListener
import com.appacoustic.cointester.presentation.MainActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.floor

class MainActivity : AppCompatActivity(), OnListFragmentInteractionListener {

    override fun onListFragmentInteraction(item: Coin) {
        viewPager.currentItem = CHECKER_POS
        (viewPager.adapter!!.instantiateItem(
            viewPager,
            CHECKER_POS
        ) as AnalyzerFragment).loadCoin(item)
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
        return true
    }

    fun dpToPx(
        context: Context,
        dp: Int
    ): Int {
        return floor(dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT).toDouble()).toInt()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuMainUserManual -> //                analyzerViews.showInstructions();
                false
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

    private fun visitWeb() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("http://planetdevices.com")
        startActivity(intent)
    }

    val vp: CustomViewPager
        get() = viewPager

    internal class SectionsPagerAdapter(
        private val context: Context,
        fm: FragmentManager?
    ) : FragmentPagerAdapter(fm!!) {
        override fun getItem(position: Int): Fragment {
            when (position) {
                COINS_POS -> return CoinsFragment.newInstance()
                CHECKER_POS -> return AnalyzerFragment.newInstance()
                TUTORIAL_POS -> return TutorialFragment.newInstance()
            }
            return return CoinsFragment.newInstance()
        }

        override fun getCount(): Int {
            return N_TABS
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                COINS_POS -> return context.getString(R.string.coins)
                CHECKER_POS -> return context.getString(R.string.checker)
                TUTORIAL_POS -> return context.getString(R.string.help)
            }
            return null
        }
    }

    companion object {
        val TAG = MainActivity::class.java.simpleName
        const val N_TABS = 3
        const val COINS_POS = 0
        const val CHECKER_POS = 1
        const val TUTORIAL_POS = 2
    }
}
