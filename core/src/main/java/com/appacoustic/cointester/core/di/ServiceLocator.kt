package com.appacoustic.cointester.core.di

import com.appacoustic.cointester.core.presentation.analyzer.analyzerModule
import com.appacoustic.cointester.core.presentation.legacy.legacyAnalyzerModule
import com.appacoustic.cointester.core.presentation.main.mainModule
import com.appacoustic.cointester.core.presentation.sonometer.sonometerModule
import com.appacoustic.cointester.coreAnalytics.analyticsTrackerModule
import com.appacoustic.cointester.coreAnalytics.error.errorTrackerModule
import com.appacoustic.cointester.coreAnalytics.remoteconfig.remoteConfigModule
import com.appacoustic.cointester.coredata.coreDataModule

val serviceLocator = listOf(
    remoteConfigModule,

    coreDataModule,
    analyticsTrackerModule,
    errorTrackerModule,

    mainModule,
    analyzerModule,
    legacyAnalyzerModule,
    sonometerModule
)
