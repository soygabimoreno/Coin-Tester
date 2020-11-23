package com.appacoustic.cointester.libFramework.extension

import kotlin.math.round

fun Double.roundTo1Decimal(): Double {
    val nDecimals = 1
    return roundToDecimals(nDecimals)
}

fun Double.roundToDecimals(nDecimals: Int): Double {
    var multiplier = 1.0
    repeat(nDecimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}
