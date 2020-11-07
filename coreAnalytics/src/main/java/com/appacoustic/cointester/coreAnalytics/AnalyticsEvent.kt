package com.appacoustic.cointester.coreAnalytics

interface AnalyticsEvent {
    val name: String
    val parameters: Map<String, Any>
}
