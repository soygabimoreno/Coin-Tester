package com.appacoustic.cointester.presentation;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.appacoustic.cointester.R;
import com.appacoustic.cointester.aaa.utils.DataManager;
import com.appacoustic.cointester.aaa.view.CustomViewPager;
import com.appacoustic.cointester.coredomain.Coin;
import com.gabrielmorenoibarra.g.G;
import com.gabrielmorenoibarra.g.GGraphics;
import com.gabrielmorenoibarra.k.util.KLog;
import com.google.android.material.tabs.TabLayout;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements CoinsFragment.OnListFragmentInteractionListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    public static final int N_TABS = 3;
    public static final int COINS_POS = 0;
    public static final int CHECKER_POS = 1;
    public static final int TUTORIAL_POS = 2;

    @BindView(R.id.tabs)
    TabLayout tabLayout;

    @Override
    public void onListFragmentInteraction(Coin item) {
        ViewPager vp = findViewById(R.id.viewPager);
        vp.setCurrentItem(CHECKER_POS);
        ((AnalyzerFragment) vp.getAdapter().instantiateItem(vp, CHECKER_POS)).loadCoin(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

        ViewPager vp = findViewById(R.id.viewPager);
        vp.setAdapter(adapter);
        vp.setCurrentItem(CHECKER_POS);

        tabLayout.setupWithViewPager(vp);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuMainAbout:
                final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle(getString(R.string.developed_by));
                ImageView iv = new ImageView(this);
                int px = GGraphics.dpToPx(this, 10);
                iv.setPadding(px, GGraphics.dpToPx(this, 20), px, px);
                iv.setImageDrawable(getDrawable(R.drawable.ic_logo_planet_devices));
                iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        visitWeb();
                        alertDialog.hide();
                    }
                });
                alertDialog.setView(iv);
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.visit_web), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        visitWeb();
                    }
                });
                alertDialog.setCancelable(true);
                alertDialog.show();
                return true;
            case R.id.menuMainContact:
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + DataManager.getInstance().getContactEmail()));
                intent.putExtra(Intent.EXTRA_SUBJECT, "EMAIL FROM APP COIN TESTER");
                startActivity(Intent.createChooser(intent, getString(R.string.select_email_sending_app)));
                return true;
            case R.id.menuMainUserManual:
//                analyzerViews.showInstructions();
                return false;
            case R.id.menuMainPreferences:
//                Intent settings = new Intent(getBaseContext(), MyPreferencesActivity.class);
//                settings.putExtra(MY_PREFERENCES_MSG_SOURCE_ID, analyzerParam.audioSourceIDs);
//                settings.putExtra(MY_PREFERENCES_MSG_SOURCE_NAME, analyzerParam.audioSourcesEntries);
//                startActivity(settings);
                return false;
            case R.id.menuMainAudioSourcesChecker:
//                Intent int_info_rec = new Intent(this, AudioSourcesCheckerActivity.class);
//                startActivity(int_info_rec);
                return false;
            case R.id.menuMainRanges:
//                rangeViewDialogC.ShowRangeViewDialog();
                return false;
            case R.id.menuMainCalibration:
//                selectFile(REQUEST_CALIB_LOAD);
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void visitWeb() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://planetdevices.com"));
        startActivity(intent);
    }

    public CustomViewPager getVp() {
        return findViewById(R.id.viewPager);
    }

    static class SectionsPagerAdapter extends FragmentPagerAdapter {

        private Context context;

        public SectionsPagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            this.context = context;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case COINS_POS:
                    return CoinsFragment.newInstance();
                case CHECKER_POS:
                    return AnalyzerFragment.newInstance();
                case TUTORIAL_POS:
                    return TutorialFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return N_TABS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case COINS_POS:
                    return context.getString(R.string.coins);
                case CHECKER_POS:
                    return context.getString(R.string.checker);
                case TUTORIAL_POS:
                    return context.getString(R.string.help);
            }
            return null;
        }
    }

    public void updateActivityOnUiThread() {
        KLog.Companion.d(Thread.currentThread().getStackTrace()[2].getMethodName() + " " + hashCode());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                G.refreshActivity(MainActivity.this);
            }
        });
    }
}
