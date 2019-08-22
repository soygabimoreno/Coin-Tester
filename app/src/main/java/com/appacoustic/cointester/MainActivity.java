package com.appacoustic.cointester;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.appacoustic.cointester.domain.Coin;
import com.appacoustic.cointester.framework.KLog;
import com.appacoustic.cointester.utils.DataManager;
import com.appacoustic.cointester.view.CustomViewPager;
import com.crashlytics.android.Crashlytics;
import com.gabrielmorenoibarra.g.G;
import com.gabrielmorenoibarra.g.GGraphics;

import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity implements CoinsFragment.OnListFragmentInteractionListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    public static final int N_TABS = 3;
    public static final int COINS_POS = 0;
    public static final int CHECKER_POS = 1;
    public static final int TUTORIAL_POS = 2;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.viewPager) CustomViewPager vp;
    @BindView(R.id.tabs) TabLayout tabLayout;

    @BindString(R.string.select_email_sending_app) String selectEmailSendingApp;
    @BindString(R.string.developed_by) String developedBy;
    @BindString(R.string.visit_web) String visitWeb;

    @BindDrawable(R.drawable.ic_logo_planet_devices) Drawable logoPlanetDevices;

    @Override
    public void onListFragmentInteraction(Coin item) {
        vp.setCurrentItem(CHECKER_POS);
        ((AnalyzerFragment) vp.getAdapter().instantiateItem(vp, CHECKER_POS)).loadCoin(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

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
                alertDialog.setTitle(developedBy);
                ImageView iv = new ImageView(this);
                int px = GGraphics.dpToPx(this, 10);
                iv.setPadding(px, GGraphics.dpToPx(this, 20), px, px);
                iv.setImageDrawable(logoPlanetDevices);
                iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        visitWeb();
                        alertDialog.hide();
                    }
                });
                alertDialog.setView(iv);
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, visitWeb, new DialogInterface.OnClickListener() {
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
                startActivity(Intent.createChooser(intent, selectEmailSendingApp));
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
        return vp;
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