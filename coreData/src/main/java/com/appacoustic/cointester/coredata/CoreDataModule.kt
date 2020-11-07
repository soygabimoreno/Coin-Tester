package com.appacoustic.cointester.coredata

import com.appacoustic.cointester.coredomain.session.UserSession
import org.koin.dsl.module

val coreDataModule = module {
    single<UserSession> { DefaultUserSession() }
}
