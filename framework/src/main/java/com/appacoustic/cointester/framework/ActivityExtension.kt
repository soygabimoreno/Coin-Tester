package com.appacoustic.cointester.framework

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.gabrielmorenoibarra.g.G
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import org.jetbrains.anko.longToast

fun Activity.requestPermissions(permissions: Set<String>, onSuccess: () -> Unit) {
    Dexter.withActivity(this)
            .withPermissions(permissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report?.areAllPermissionsGranted() == true) {
                        onSuccess()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
                    token?.continuePermissionRequest()
                }
            }).check()

}

fun Activity.requestPermission(permission: String, requestCode: Int, onSuccess: () -> Unit) {
    if (G.checkPermission(this, permission)) {
        onSuccess()
    } else {
        Dexter.withActivity(this)
                .withPermission(permission)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        onSuccess()
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) {
                        val s = String.format(getString(R.string.permission_n_required), response.permissionName)
                        longToast(s)
                        if (response.isPermanentlyDenied) {
                            showSettings(requestCode)
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                        val s = String.format(getString(R.string.permission_n_required), permission.name)
                        longToast(s)
                        showSettings(requestCode)
                    }
                }).check()
    }
}

fun Activity.showSettings(requestCode: Int) {
    showSettings(packageName, requestCode)
}

fun Activity.showSettings(packageName: String, requestCode: Int) {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    intent.data = Uri.parse("package:$packageName")
    startActivityForResult(intent, requestCode)
}
