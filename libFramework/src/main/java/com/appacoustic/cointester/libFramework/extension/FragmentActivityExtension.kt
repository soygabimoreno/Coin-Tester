package com.appacoustic.cointester.libFramework.extension

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

fun FragmentActivity.navigateTo(
    containerResId: Int,
    fragment: Fragment
) {
    supportFragmentManager.beginTransaction()
        .replace(
            containerResId,
            fragment
        )
        .commit()
}
