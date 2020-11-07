package com.appacoustic.cointester.di

import com.appacoustic.cointester.coreAnalytics.analyticsTrackerModule
import com.appacoustic.cointester.coreAnalytics.error.errorTrackerModule
import com.appacoustic.cointester.coreAnalytics.remoteconfig.remoteConfigModule
import com.appacoustic.cointester.presentation.analyzer.analyzerModule
import com.appacoustic.cointester.presentation.main.mainModule

val serviceLocator = listOf(
    remoteConfigModule,

    analyticsTrackerModule,
    errorTrackerModule,

    mainModule,
    analyzerModule
)
