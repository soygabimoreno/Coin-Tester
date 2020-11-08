package com.appacoustic.cointester.aaa.analyzer.settings

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.media.MediaRecorder
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceActivity
import com.appacoustic.cointester.R
import com.appacoustic.cointester.aaa.analyzer.AnalyzerUtil
import com.appacoustic.cointester.libFramework.KLog.Companion.e
import com.appacoustic.cointester.libFramework.KLog.Companion.i
import com.appacoustic.cointester.presentation.analyzer.AnalyzerFragment

class MyPreferencesActivity : PreferenceActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        // as soon as the user modifies a preference,
        // the system saves the changes to a default SharedPreferences file
    }

    private val prefListener = OnSharedPreferenceChangeListener { prefs, key ->
        i("$key=$prefs")
        if (key == null || key == "windowFunction") {
            val connectionPref = findPreference(key)
            connectionPref.summary = prefs.getString(
                key,
                ""
            )
        }
        if (key == null || key == "audioSource") {
            val asi = prefs.getString(
                "audioSource",
                getString(R.string.audio_sources_default)
            )
            val audioSourceId = asi!!.toInt()
            val connectionPref = findPreference(key)
            connectionPref.summary = getAudioSourceNameFromId(audioSourceId)
        }
        if (key == null || key == "spectrogramColorMap") {
            val connectionPref = findPreference(key)
            connectionPref.summary = prefs.getString(
                key,
                ""
            )
        }
    }

    override fun onResume() {
        super.onResume()

        // Get list of default sources
        val intent = intent
        val asid = intent.getIntArrayExtra(AnalyzerFragment.MY_PREFERENCES_MSG_SOURCE_ID)
        val `as` = intent.getStringArrayExtra(AnalyzerFragment.MY_PREFERENCES_MSG_SOURCE_NAME)
        var nExtraSources = 0
        for (id in asid!!) {
            // See SamplingLoopThread::run() for the magic number 1000
            if (id >= 1000) nExtraSources++
        }

        // Get list of supported sources
        val au = AnalyzerUtil(this)
        val audioSourcesId = au.GetAllAudioSource(4)
        i(" n_as = " + audioSourcesId.size)
        i(" n_ex = $nExtraSources")
        audioSourcesName = arrayOfNulls(audioSourcesId.size + nExtraSources)
        for (i in audioSourcesId.indices) {
            audioSourcesName[i] = au.getAudioSourceName(audioSourcesId[i])
        }

        // Combine these two sources
        audioSources = arrayOfNulls(audioSourcesName.size)
        var j = 0
        while (j < audioSourcesId.size) {
            audioSources[j] = audioSourcesId[j].toString()
            j++
        }
        for (i in asid.indices) {
            // See SamplingLoopThread::run() for the magic number 1000
            if (asid[i] >= 1000) {
                audioSources[j] = asid[i].toString()
                audioSourcesName[j] = `as`!![i]
                j++
            }
        }
        val lp = findPreference("audioSource") as ListPreference
        lp.setDefaultValue(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        lp.entries = audioSourcesName
        lp.entryValues = audioSources
        preferenceScreen.sharedPreferences
            .registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
            .unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    companion object {
        private val TAG = MyPreferencesActivity::class.java.simpleName
        private lateinit var audioSources: Array<String?>
        private lateinit var audioSourcesName: Array<String?>
        private fun getAudioSourceNameFromId(id: Int): String? {
            for (i in audioSources.indices) {
                if (audioSources[i] == id.toString()) {
                    return audioSourcesName[i]
                }
            }
            e("getAudioSourceName(): no this entry.")
            return ""
        }
    }
}
