package com.appacoustic.cointester.aaa.analyzer

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import android.os.Build
import com.appacoustic.cointester.R
import com.appacoustic.cointester.aaa.analyzer.AnalyzerUtil
import com.appacoustic.cointester.libFramework.KLog.Companion.e
import com.appacoustic.cointester.libFramework.KLog.Companion.i
import java.util.*

/**
 * Utility functions for audio analyzer.
 */
class AnalyzerUtil(context: Context) {
    // used to detect if the data is unchanged
    private var cmpDB: DoubleArray? = doubleArrayOf()
    fun sameTest(data: DoubleArray) {
        // test
        if (cmpDB == null || cmpDB!!.size != data.size) {
            cmpDB = DoubleArray(data.size)
        } else {
            var same = true
            for (i in data.indices) {
                if (!java.lang.Double.isNaN(cmpDB!![i]) && !java.lang.Double.isInfinite(cmpDB!![i]) && cmpDB!![i] != data[i]) {
                    same = false
                    break
                }
            }
            if (same) {
                i("Same data row!")
            }
            System.arraycopy(
                data,
                0,
                cmpDB,
                0,
                data.size
            )
        }
    }

    val stdSourceId // how to make it final?
        : IntArray
    val stdSourceApi: IntArray
    val stdSourceName: Array<String>
    val stdAudioSourcePermission: Array<String>

    // filterLevel = 0: no filter
    //             & 1: leave only standard sources
    //             & 2: leave only permitted sources (&1)
    //             & 4: leave only sources coincide the API level (&1)
    fun GetAllAudioSource(filterLevel: Int): IntArray {
        // Use reflection to get all possible audio source (in compilation environment)
        val iList = ArrayList<Int>()
        val clazz = AudioSource::class.java
        val arr = clazz.fields
        for (f in arr) {
            if (f.type == Int::class.javaPrimitiveType) {
                try {
                    val id = f[null] as Int
                    iList.add(id)
                    i("Sources id: $id")
                } catch (e: IllegalAccessException) {
                }
            }
        }
        Collections.sort(iList)

        // Filter unnecessary audio source
        var iterator: Iterator<Int>
        var iListRet = iList
        if (filterLevel > 0) {
            iListRet = ArrayList()
            iterator = iList.iterator()
            for (i in iList.indices) {
                val id = iterator.next()
                val k = arrayContainInt(
                    stdSourceId,
                    id
                ) // get the index to standard source if possible
                if (k < 0) continue
                if (filterLevel and 2 > 0 && stdAudioSourcePermission[k] != "") continue
                if (filterLevel and 4 > 0 && stdSourceApi[k] > Build.VERSION.SDK_INT) continue
                iListRet.add(id)
            }
        }

        // Return an int array
        val ids = IntArray(iListRet.size)
        iterator = iListRet.iterator()
        for (i in ids.indices) {
            ids[i] = iterator.next()
        }
        return ids
    }

    fun getAudioSourceName(id: Int): String {
        val k = arrayContainInt(
            stdSourceId,
            id
        )
        return if (k >= 0) {
            stdSourceName[k]
        } else {
            "(unknown id=$id)"
        }
    }

    companion object {
        private val TAG = AnalyzerUtil::class.java.simpleName
        private val LP = arrayOf(
            "C",
            "C#",
            "D",
            "D#",
            "E",
            "F",
            "F#",
            "G",
            "G#",
            "A",
            "A#",
            "B"
        )

        @JvmStatic
        fun freq2pitch(f: Double): Double {
            return 69 + 12 * Math.log(f / 440.0) / Math.log(2.0) // MIDI pitch
        }

        @JvmStatic
        fun pitch2freq(p: Double): Double {
            return Math.pow(
                2.0,
                (p - 69) / 12
            ) * 440.0
        }

        @JvmStatic
        fun pitch2Note(
            a: StringBuilder,
            p: Double,
            prec_frac: Int,
            tightMode: Boolean
        ) {
            val pi = Math.round(p).toInt()
            val po = Math.floor(pi / 12.0).toInt()
            val pm = pi - po * 12
            a.append(LP[pm])
            StringBuilderNumberFormat.fillInInt(
                a,
                po - 1
            )
            if (LP[pm].length == 1 && !tightMode) {
                a.append(' ')
            }
            val cent = Math.round(
                100 * (p - pi) * Math.pow(
                    10.0,
                    prec_frac.toDouble()
                )
            ) * Math.pow(
                10.0,
                -prec_frac.toDouble()
            )
            if (!tightMode) {
                StringBuilderNumberFormat.fillInNumFixedWidthSignedFirst(
                    a,
                    cent,
                    2,
                    prec_frac
                )
            } else {
                if (cent != 0.0) {
                    a.append(if (cent < 0) '-' else '+')
                    StringBuilderNumberFormat.fillInNumFixedWidthPositive(
                        a,
                        Math.abs(cent),
                        2,
                        prec_frac,
                        '\u0000'
                    )
                }
            }
        }

        // Convert frequency to pitch
        // Fill with sFill until length is 6. If sFill=="", do not fill
        fun freq2Cent(
            a: StringBuilder,
            f: Double,
            sFill: String?
        ) {
            if (f <= 0 || java.lang.Double.isNaN(f) || java.lang.Double.isInfinite(f)) {
                a.append("      ")
                return
            }
            val len0 = a.length
            // A4 = 440Hz
            val p = freq2pitch(f)
            pitch2Note(
                a,
                p,
                0,
                false
            )
            while (a.length - len0 < 6 && sFill != null && sFill.length > 0) {
                a.append(sFill)
            }
        }

        @JvmStatic
        fun isAlmostInteger(x: Double): Boolean {
            // return x % 1 == 0;
            val i = Math.round(x).toDouble()
            return if (i == 0.0) {
                Math.abs(x) < 1.2e-7 // 2^-23 = 1.1921e-07
            } else {
                Math.abs(x - i) / i < 1.2e-7
            }
        }

        /**
         * Return a array of verified audio sampling rates.
         *
         * @param requested: the sampling rates to be verified.
         */
        fun validateAudioRates(requested: Array<String>): Array<String> {
            val validated = ArrayList<String>()
            for (s in requested) {
                var rate: Int
                val sv = s.split("::").toTypedArray()
                rate = if (sv.size == 1) {
                    sv[0].toInt()
                } else {
                    sv[1].toInt()
                }
                if (rate != 0) {
                    val recBufferSize = AudioRecord.getMinBufferSize(
                        rate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    if (recBufferSize != AudioRecord.ERROR_BAD_VALUE) {
                        // Extra exam to high sampling rate, by opening it.
                        if (rate > 48000) {
                            var record: AudioRecord
                            record = try {
                                AudioRecord(
                                    AudioSource.DEFAULT,
                                    rate,
                                    AudioFormat.CHANNEL_IN_MONO,
                                    AudioFormat.ENCODING_PCM_16BIT,
                                    recBufferSize
                                )
                            } catch (e: IllegalArgumentException) {
                                continue
                            }
                            if (record.state == AudioRecord.STATE_INITIALIZED) {
                                validated.add(s)
                            }
                        } else {
                            validated.add(s)
                        }
                    }
                } else {
                    validated.add(s)
                }
            }
            return validated.toTypedArray()
        }

        fun parseDouble(st: String): Double {
            return try {
                st.toDouble()
            } catch (e: NumberFormatException) {
                0.0 / 0.0 // nan
            }
        }

        // Thanks http://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences
        fun putDouble(
            edit: SharedPreferences.Editor,
            key: String?,
            value: Double
        ): SharedPreferences.Editor {
            return edit.putLong(
                key,
                java.lang.Double.doubleToRawLongBits(value)
            )
        }

        fun getDouble(
            prefs: SharedPreferences,
            key: String?,
            defaultValue: Double
        ): Double {
            return java.lang.Double.longBitsToDouble(
                prefs.getLong(
                    key,
                    java.lang.Double.doubleToLongBits(defaultValue)
                )
            )
        }

        // Java s**ks
        fun arrayContainInt(
            arr: IntArray?,
            v: Int
        ): Int {
            if (arr == null) return -1
            for (i in arr.indices) {
                if (arr[i] == v) return i
            }
            return -1
        }

        fun arrayContainString(
            arr: Array<String>?,
            v: String
        ): Int {
            if (arr == null) return -1
            for (i in arr.indices) {
                if (arr[i] == v) return i
            }
            return -1
        }

        fun isSorted(a: DoubleArray?): Boolean {
            if (a == null || a.size <= 1) {
                return true
            }
            var d = a[0]
            for (i in 1 until a.size) {
                if (d > a[i]) {
                    return false
                }
                d = a[i]
            }
            return true
        }

        fun interpLinear(
            xFixed: DoubleArray?,
            yFixed: DoubleArray?,
            xInterp: DoubleArray?
        ): DoubleArray? {
            if (xFixed == null || yFixed == null || xInterp == null) {
                return null
            }
            if (xFixed.size == 0 || yFixed.size == 0 || xInterp.size == 0) {
                return DoubleArray(0)
            }
            if (xFixed.size != yFixed.size) {
                e("Input data length mismatch")
            }

//        if (!isSorted(xFixed)) {
//            sort(xFixed);
//            yFixed = yFixed(x_id);
//        }
            if (!isSorted(xInterp)) {
                Arrays.sort(xInterp)
            }
            val yInterp = DoubleArray(xInterp.size)
            var xiId = 0
            while (xiId < xInterp.size && xInterp[xiId] < xFixed[0]) {
                yInterp[xiId] = yFixed[0]
                xiId++
            }
            for (xfId in 1 until xFixed.size) {
                while (xiId < xInterp.size && xInterp[xiId] < xFixed[xfId]) {
                    // interpolation using (xFixed(x_id - 1), yFixed(xfId - 1)) and (xFixed(x_id), yFixed(xfId))
                    yInterp[xiId] = (yFixed[xfId] - yFixed[xfId - 1]) / (xFixed[xfId] - xFixed[xfId - 1]) * (xInterp[xiId] - xFixed[xfId - 1]) + yFixed[xfId - 1]
                    xiId++
                }
            }
            while (xiId < xInterp.size) {
                yInterp[xiId] = yFixed[yFixed.size - 1]
                xiId++
            }
            return yInterp
        }
    }

    init {
        stdSourceId = context.resources.getIntArray(R.array.std_audio_source_id)
        stdSourceApi = context.resources.getIntArray(R.array.std_audio_source_api_level)
        stdSourceName = context.resources.getStringArray(R.array.std_audio_source_name)
        stdAudioSourcePermission = context.resources.getStringArray(R.array.std_audio_source_permission)
    }
}
