package com.appacoustic.cointester.core.presentation.mypreference

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.media.MediaRecorder
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceActivity
import com.appacoustic.cointester.core.R
import com.appacoustic.cointester.core.framework.AnalyzerUtil
import com.appacoustic.cointester.libFramework.KLog

class MyPreferenceActivity : PreferenceActivity() {

    companion object {
        const val EXTRA_AUDIO_SOURCE_IDS = "EXTRA_SOURCE_ID"
        const val EXTRA_AUDIO_SOURCE_NAMES = "EXTRA_SOURCE_NAME"

        fun launch(
            context: Context,
            audioSourceIds: IntArray,
            audioSourceNames: Array<String>
        ) {
            val intent = Intent(
                context,
                MyPreferenceActivity::class.java
            )
            intent.putExtra(
                EXTRA_AUDIO_SOURCE_IDS,
                audioSourceIds
            )
            intent.putExtra(
                EXTRA_AUDIO_SOURCE_NAMES,
                audioSourceNames
            )
            context.startActivity(intent)
        }

        private lateinit var audioSources: Array<String?>
        private lateinit var audioSourcesName: Array<String?>

        private fun findAudioSourceNameById(id: Int): String? {
            for (i in audioSources.indices) {
                if (audioSources[i] == id.toString()) {
                    return audioSourcesName[i]
                }
            }
            KLog.e("getAudioSourceName(): no this entry.")
            return ""
        }
    }

    private lateinit var audioSourceIds: IntArray
    private lateinit var audioSourceNames: Array<String>

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        // as soon as the user modifies a preference,
        // the system saves the changes to a default SharedPreferences file

        audioSourceIds = intent.getIntArrayExtra(EXTRA_AUDIO_SOURCE_IDS)!!
        audioSourceNames = intent.getStringArrayExtra(EXTRA_AUDIO_SOURCE_NAMES)!!
    }

    private val prefListener = OnSharedPreferenceChangeListener { prefs, key ->
        KLog.i("$key=$prefs")
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
            connectionPref.summary = findAudioSourceNameById(audioSourceId)
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
        var nExtraSources = 0
        for (id in audioSourceIds) {
            // See SamplingLoopThread::run() for the magic number 1000
            if (id >= 1000) nExtraSources++
        }

        // Get list of supported sources
        val au = AnalyzerUtil(this)
        val audioSourcesId = au.getAllAudioSources(4)
        KLog.i(" n_as = " + audioSourcesId.size)
        KLog.i(" n_ex = $nExtraSources")
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
        for (i in audioSourceIds.indices) {
            // See SamplingLoopThread::run() for the magic number 1000
            if (audioSourceIds[i] >= 1000) {
                audioSources[j] = audioSourceIds[i].toString()
                audioSourcesName[j] = audioSourceNames[i]
                j++
            }
        }

        val listPreference = findPreference("audioSource") as ListPreference
        listPreference.setDefaultValue(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        listPreference.entries = audioSourcesName
        listPreference.entryValues = audioSources
        preferenceScreen
            .sharedPreferences
            .registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen
            .sharedPreferences
            .unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}
