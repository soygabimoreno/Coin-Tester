package com.appacoustic.libprocessingandroid.calibration

import android.content.Context
import android.net.Uri
import com.appacoustic.cointester.libFramework.KLog
import java.io.*
import java.util.*

/**
 * Load calibration file.
 */
class CalibrationLoad {

    var freq = DoubleArray(0)
    var gain = DoubleArray(0)
    var centralFreq = 1000.0
    var centralGain = -37.4

    fun loadFile(
        calibrationUri: Uri,
        context: Context
    ) {
        val calibPath = calibrationUri.path
        val br: BufferedReader
        val inputStream: InputStream?
        inputStream = try {
            context.contentResolver.openInputStream(calibrationUri)
        } catch (e: FileNotFoundException) {
            KLog.e("no calib file found: $calibPath")
            return
        }
        if (inputStream == null) {
            KLog.e("open calib file fail: $calibPath")
            return
        }
        br = BufferedReader(InputStreamReader(inputStream))
        var line: String? = null
        try {
            var lineCount = 0
            val freqList = ArrayList<Double>()
            val amplitudeDBList = ArrayList<Double>()
            while (true) {
                line = br.readLine()
                if (line == null) break
                lineCount++
                line = line.trim { it <= ' ' }
                if (line.length == 0) {  // skip empty line
                    continue
                }
                val c = line[0]
                if ('0' <= c && c <= '9' || c == '.' || c == '-') {  // Should be a number
                    // 20.00	-4.2
                    val strs = line.split("[ \t]+").toTypedArray()
                    if (strs.size != 2) {
                        KLog.w("Fail to parse line $lineCount :$line")
                        continue
                    }
                    freqList.add(java.lang.Double.valueOf(strs[0]))
                    amplitudeDBList.add(java.lang.Double.valueOf(strs[1]))
                } else if (line[0] == '*') {  // Dayton Audio txt/cal or miniDSP cal file
                    // parse Only the Dayton Audio header:
                    //*1000Hz	-37.4
                    val strs = line.substring(1).split("Hz[ \t]+").toTypedArray()
                    if (strs.size == 2) {
                        KLog.i("Dayton Audio header")
                        centralFreq = java.lang.Double.valueOf(strs[0])
                        centralGain = java.lang.Double.valueOf(strs[1])
                    }
                    // miniDSP cal header:
                    //* miniDSP PMIK-1 calibration file, serial: 8000234, format: cal
                    // Skip
                } else if (line[0] == '"') {
                    // miniDSP txt header:
                    //"miniDSP PMIK-1 calibration file, serial: 8000234, format: frd"
                    // Skip
                    // miniDSP frd header:
                    //"miniDSP PMIK-1 calibration file, serial: 8000234, format: frd"
                    // Skip
                } else if (line[0] == '#') {
                    // Shell style comment line
                    // Skip
                } else {
                    KLog.e("Bad calibration file.")
                    freqList.clear()
                    amplitudeDBList.clear()
                    break
                }
            }
            br.close()
            freq = DoubleArray(freqList.size)
            gain = DoubleArray(freqList.size)
            val itr: Iterator<*> = freqList.iterator()
            val itr2: Iterator<*> = amplitudeDBList.iterator()
            var j = 0
            while (itr.hasNext()) {
                freq[j] = itr.next() as Double
                gain[j] = itr2.next() as Double
                j++
            }
        } catch (e: IOException) {
            KLog.e("Fail to read file: $calibPath")
        }
    }
}
