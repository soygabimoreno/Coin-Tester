package com.appacoustic.cointester.presentation.start

import android.Manifest
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.appacoustic.cointester.presentation.main.MainActivity

class StartActivity : AppCompatActivity() {

    private val inputRecordAudioPermission = Manifest.permission.RECORD_AUDIO
    private val requestRecordAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            when {
                isGranted -> {
                    requestWriteExternalStoragePermission()
                }
                shouldShowRequestPermissionRationale(inputRecordAudioPermission) -> {
                    // TODO
                }
                else -> {
                    // TODO
                }
            }
        }

    private val inputWriteExternalStoragePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val requestWriteExternalStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            when {
                isGranted -> {
                    navigateToMain()
                }
                shouldShowRequestPermissionRationale(inputWriteExternalStoragePermission) -> {
                    // TODO
                }
                else -> {
                    // TODO
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRecordAudioPermission()
    }

    private fun requestRecordAudioPermission() {
        requestRecordAudioPermission.launch(inputRecordAudioPermission)
    }

    private fun requestWriteExternalStoragePermission() {
        requestWriteExternalStoragePermission.launch(inputWriteExternalStoragePermission)
    }

    private fun navigateToMain() {
        MainActivity.launch(this)
        finish()
    }
}
