package com.appacoustic.cointester.libFramework.extension

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun String?.isFilled(): Boolean {
    contract {
        returns(true) implies (this@isFilled != null)
    }
    return !this.isNullOrEmpty()
}
