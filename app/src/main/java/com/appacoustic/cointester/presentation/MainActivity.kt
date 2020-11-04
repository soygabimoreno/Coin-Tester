package com.appacoustic.cointester.presentation

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.appacoustic.cointester.R
import com.appacoustic.cointester.aaa.utils.DataManager
import com.appacoustic.cointester.aaa.view.CustomViewPager
import com.appacoustic.cointester.coredomain.Coin
import com.appacoustic.cointester.presentation.CoinsFragment.OnListFragmentInteractionListener
import com.appacoustic.cointester.presentation.MainActivity
import com.gabrielmorenoibarra.g.G
import com.gabrielmorenoibarra.g.GGraphics
import com.gabrielmorenoibarra.k.util.KLog.Companion.d
import kotlinx.android.synthetic.main.activity_main.*

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuMainAbout -> {
                val alertDialog = AlertDialog.Builder(this).create()
                alertDialog.setTitle(getString(R.string.developed_by))
                val iv = ImageView(this)
                val px = GGraphics.dpToPx(
                    this,
                    10
                )
                iv.setPadding(
                    px,
                    GGraphics.dpToPx(
                        this,
                        20
                    ),
                    px,
                    px
                )
                iv.setImageDrawable(getDrawable(R.drawable.ic_logo_planet_devices))
                iv.setOnClickListener {
                    visitWeb()
                    alertDialog.hide()
                }
                alertDialog.setView(iv)
                alertDialog.setButton(
                    DialogInterface.BUTTON_POSITIVE,
                    getString(R.string.visit_web)
                ) { dialog, which -> visitWeb() }
                alertDialog.setCancelable(true)
                alertDialog.show()
                true
            }
            R.id.menuMainContact -> {
                val intent = Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse("mailto:" + DataManager.getInstance().contactEmail)
                )
                intent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "EMAIL FROM APP COIN TESTER"
                )
                startActivity(
                    Intent.createChooser(
                        intent,
                        getString(R.string.select_email_sending_app)
                    )
                )
                true
            }
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
