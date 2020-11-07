package com.appacoustic.cointester.coreAnalytics.error

class ErrorTracker(
    private val components: List<ErrorTrackerComponent>
) : ErrorTrackerComponent {
    override fun <E : ErrorEvent> trackError(event: E) {
        components.forEach { it.trackError(event) }
    }
}
