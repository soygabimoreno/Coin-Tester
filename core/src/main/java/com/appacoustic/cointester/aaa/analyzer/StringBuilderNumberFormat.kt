package com.appacoustic.cointester.aaa.analyzer

import com.appacoustic.cointester.libFramework.KLog.Companion.w

object StringBuilderNumberFormat {
    private val TAG = StringBuilderNumberFormat::class.java.simpleName
    private val charDigits = charArrayOf(
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9'
    )

    // Invent wheel... so we can eliminate GC
    @JvmOverloads
    fun fillInNumFixedWidthPositive(
        sb: StringBuilder,
        d: Double,
        nInt: Int,
        nFrac: Int,
        padChar: Char = ' '
    ) {
        var nInt = nInt
        if (d < 0) {
            for (i in 0 until nInt + nFrac + if (nFrac > 0) 1 else 0) {
                sb.append(padChar)
            }
            w("fillInNumFixedWidthPositive: negative number")
            return
        }
        if (d >= Math.pow(
                10.0,
                nInt.toDouble()
            )
        ) {
            sb.append("OFL")
            for (i in 3 until nInt + nFrac + if (nFrac > 0) 1 else 0) {
                sb.append(' ')
            }
            return
        }
        if (java.lang.Double.isNaN(d)) {
            sb.append("NaN")
            for (i in 3 until nInt + nFrac + if (nFrac > 0) 1 else 0) {
                sb.append(' ')
            }
            return
        }
        while (nInt > 0) {
            nInt--
            if (d < Math.pow(
                    10.0,
                    nInt.toDouble()
                ) && nInt > 0
            ) {
                if (padChar != '\u0000') {
                    sb.append(padChar)
                }
            } else {
                sb.append(
                    charDigits[(d / Math.pow(
                        10.0,
                        nInt.toDouble()
                    ) % 10.0).toInt()]
                )
            }
        }
        if (nFrac > 0) {
            sb.append('.')
            for (i in 1..nFrac) {
                sb.append(
                    charDigits[(d * Math.pow(
                        10.0,
                        i.toDouble()
                    ) % 10.0).toInt()]
                )
            }
        }
    }

    @JvmStatic
    fun fillInNumFixedFrac(
        sb: StringBuilder,
        d: Double,
        nInt: Int,
        nFrac: Int
    ) {
        var d = d
        if (d < 0) {
            sb.append('-')
            d = -d
        }
        fillInNumFixedWidthPositive(
            sb,
            d,
            nInt,
            nFrac,
            '\u0000'
        )
    }

    fun fillInNumFixedWidth(
        sb: StringBuilder,
        d: Double,
        nInt: Int,
        nFrac: Int
    ) {
        val it = sb.length
        sb.append(' ')
        if (d < 0) {
            fillInNumFixedWidthPositive(
                sb,
                -d,
                nInt,
                nFrac
            )
            for (i in it until sb.length) {
                if (sb[i + 1] != ' ') {
                    sb.setCharAt(
                        i,
                        '-'
                    )
                    return
                }
            }
        }
        fillInNumFixedWidthPositive(
            sb,
            d,
            nInt,
            nFrac
        )
    }

    fun fillInNumFixedWidthSigned(
        sb: StringBuilder,
        d: Double,
        nInt: Int,
        nFrac: Int
    ) {
        val it = sb.length
        sb.append(' ')
        fillInNumFixedWidthPositive(
            sb,
            Math.abs(d),
            nInt,
            nFrac
        )
        for (i in it until sb.length) {
            if (sb[i + 1] != ' ') {
                if (d < 0) {
                    sb.setCharAt(
                        i,
                        '-'
                    )
                } else {
                    sb.setCharAt(
                        i,
                        '+'
                    )
                }
                return
            }
        }
    }

    fun fillInNumFixedWidthSignedFirst(
        sb: StringBuilder,
        d: Double,
        nInt: Int,
        nFrac: Int
    ) {
        if (d < 0) {
            sb.append('-')
        } else {
            sb.append('+')
        }
        fillInNumFixedWidthPositive(
            sb,
            Math.abs(d),
            nInt,
            nFrac
        )
    }

    fun fillInInt(
        sb: StringBuilder,
        `in`: Int
    ) {
        var `in` = `in`
        if (`in` == 0) {
            sb.append('0')
            return
        }
        if (`in` < 0) {
            sb.append('-')
            `in` = -`in`
        }
        val it = sb.length
        while (`in` > 0) {
            sb.insert(
                it,
                `in` % 10
            )
            `in` /= 10
        }
    }

    fun fillTime(
        sb: StringBuilder,
        t: Double,
        nFrac: Int
    ) {
        // in format x0:00:00.x
        var t = t
        if (t < 0) {
            t = -t
            sb.append('-')
        }
        var u: Double
        // hours
        u = Math.floor(t / 3600.0)
        fillInInt(
            sb,
            u.toInt()
        )
        sb.append(':')
        // minutes
        t -= u * 3600
        u = Math.floor(t / 60.0)
        fillInNumFixedWidthPositive(
            sb,
            u,
            2,
            0,
            '0'
        )
        sb.append(':')
        // seconds
        t -= u * 60
        fillInNumFixedWidthPositive(
            sb,
            t,
            2,
            nFrac,
            '0'
        )
    }
}
