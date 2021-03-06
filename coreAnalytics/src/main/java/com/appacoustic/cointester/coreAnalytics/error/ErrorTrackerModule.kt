package com.appacoustic.cointester.coreAnalytics.error

import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.dsl.module

val errorTrackerModule = module {
    single { FirebaseCrashlytics.getInstance() }

    single<ErrorTrackerComponent> {
        ErrorTracker(
            listOf(
                CrashlyticsErrorTrackerComponent(
                    crashlytics = get(),
                    userSession = get()
                )
            )
        )
    }
}
