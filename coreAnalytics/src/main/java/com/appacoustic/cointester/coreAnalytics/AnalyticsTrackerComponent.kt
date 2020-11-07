package com.appacoustic.cointester.coreAnalytics

interface AnalyticsTrackerComponent {
    fun <E : AnalyticsEvent> trackEvent(event: E)
}
