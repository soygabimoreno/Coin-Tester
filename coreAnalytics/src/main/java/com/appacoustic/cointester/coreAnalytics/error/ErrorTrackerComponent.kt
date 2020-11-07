package com.appacoustic.cointester.coreAnalytics.error

interface ErrorTrackerComponent {
    fun <E : ErrorEvent> trackError(event: E)

    fun trackError(error: Throwable) {
        trackError(ThrowableErrorEvent(error))
    }
}
