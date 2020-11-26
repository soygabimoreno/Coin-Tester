package com.appacoustic.libprocessing

import kotlin.math.log10
import kotlin.math.pow

fun dBToLinear(dBValue: Double): Double = 10.0.pow(dBValue / 20)

fun linearToDB(linearValue: Double): Double = 20 * log10(linearValue)
