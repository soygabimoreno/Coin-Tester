package com.appacoustic.cointester.view

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * Custom ViewPager with paging enable / disable perform.
 * Created by Gabriel Moreno on 2017-10-15.
 */
class CustomViewPager(
    context: Context,
    attrs: AttributeSet
) : ViewPager(context, attrs) {

    var stuck = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return !stuck && super.onTouchEvent(event)

    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return !stuck && super.onInterceptTouchEvent(event)
    }
}
