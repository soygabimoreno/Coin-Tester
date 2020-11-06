package com.appacoustic.cointester.presentation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appacoustic.cointester.libFramework.extension.requestPermissions

class StartActivity : AppCompatActivity() {

    private lateinit var activity: Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this@StartActivity
        requestPermissions()
    }

    private fun requestPermissions() {
        val permissions = mutableSetOf<String>()
        permissions.add(Manifest.permission.RECORD_AUDIO)
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        requestPermissions(permissions) {
            init()
        }
    }

    private fun init() {
        val intent = Intent(
            activity,
            MainActivity::class.java
        )
        startActivity(intent)
    }
}
