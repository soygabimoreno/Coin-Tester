package com.appacoustic.cointester.coreAnalytics

import com.amplitude.api.AmplitudeClient
import com.appacoustic.cointester.libFramework.BuildConfig
import com.appacoustic.cointester.libFramework.KLog
import com.appacoustic.cointester.libFramework.extension.toJSONObject

class AmplitudeAnalyticsTrackerComponent(
    private val amplitudeClient: AmplitudeClient
) : AnalyticsTrackerComponent {
    override fun <E : AnalyticsEvent> trackEvent(event: E) {
        val commonParameters = mapOf(
            "BUILD_TYPE" to BuildConfig.BUILD_TYPE
        )
        val parameters = event.parameters + commonParameters
        amplitudeClient.logEvent(
            event.name,
            parameters.toJSONObject()
        )
        KLog.i("${event.name}: $parameters")
    }
}
