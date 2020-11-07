package com.appacoustic.cointester.di

import com.appacoustic.cointester.presentation.analyzer.analyzerModule
import com.appacoustic.cointester.presentation.main.mainModule

val serviceLocator = listOf(
    appModule,

    mainModule,
    analyzerModule
)
