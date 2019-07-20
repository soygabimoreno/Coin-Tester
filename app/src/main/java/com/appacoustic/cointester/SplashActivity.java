package com.appacoustic.cointester;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.crashlytics.android.Crashlytics;
import com.gabrielmorenoibarra.g.GLog;
import com.mukesh.permissions.AppPermissions;

import io.fabric.sdk.android.Fabric;

public class SplashActivity extends AppCompatActivity {

    public static final String TAG = SplashActivity.class.getSimpleName();
    public static final int REQUEST_CODE_PERMISSIONS = 101;

    public static final int SPLASH_TIME = BuildConfig.DEBUG ? 150 : 1000;

    private AppPermissions runtimePermission;
    private String[] permissionList = new String[]{
            Manifest.permission.RECORD_AUDIO};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_FullScreen);
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();
        GLog.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + " " + hashCode());
        setContentView(R.layout.activity_splash);

        runtimePermission = new AppPermissions(this);
        if (runtimePermission.hasPermission(permissionList)) {
            init();
        } else {
            runtimePermission.requestPermission(permissionList, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    runtimePermission.requestPermission(permissionList, REQUEST_CODE_PERMISSIONS);
                }
            }
            init();
        }
    }

    private void init() {
        if (runtimePermission.hasPermission(permissionList)) { // Ensure all permissions were granted
            goToNextActivity();
        }
    }

    private void goToNextActivity() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }, SPLASH_TIME);
    }
}