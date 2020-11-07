package com.appacoustic.cointester.presentation.start

import android.Manifest
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.appacoustic.cointester.presentation.main.MainActivity

class StartActivity : AppCompatActivity() {

    private val requestRecordAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                requestWriteExternalStoragePermission()
            } else {
                // TODO
            }
        }

    private val requestWriteExternalStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                navigateToMain()
            } else {
                // TODO
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRecordAudioPermission()
    }

    private fun requestRecordAudioPermission() {
        requestRecordAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun requestWriteExternalStoragePermission() {
        requestWriteExternalStoragePermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun navigateToMain() {
        MainActivity.launch(this)
        finish()
    }
}
