package com.appacoustic.cointester.analyzer.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.appacoustic.cointester.AnalyzerFragment;
import com.appacoustic.cointester.R;
import com.appacoustic.cointester.analyzer.AnalyzerUtil;
import com.appacoustic.cointester.framework.KLog;

@SuppressWarnings("deprecation")
public class MyPreferencesActivity extends PreferenceActivity {

    private static final String TAG = MyPreferencesActivity.class.getSimpleName();

    private static String[] audioSources;
    private static String[] audioSourcesName;

    private static String getAudioSourceNameFromId(int id) {
        for (int i = 0; i < audioSources.length; i++) {
            if (audioSources[i].equals(String.valueOf(id))) {
                return audioSourcesName[i];
            }
        }
        KLog.Companion.e("getAudioSourceName(): no this entry.");
        return "";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        // as soon as the user modifies a preference,
        // the system saves the changes to a default SharedPreferences file
    }

    private SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    KLog.Companion.i(key + "=" + prefs);
                    if (key == null || key.equals("windowFunction")) {
                        Preference connectionPref = findPreference(key);
                        connectionPref.setSummary(prefs.getString(key, ""));
                    }
                    if (key == null || key.equals("audioSource")) {
                        String asi = prefs.getString("audioSource", getString(R.string.audio_sources_default));
                        int audioSourceId = Integer.parseInt(asi);
                        Preference connectionPref = findPreference(key);
                        connectionPref.setSummary(getAudioSourceNameFromId(audioSourceId));
                    }
                    if (key == null || key.equals("spectrogramColorMap")) {
                        Preference connectionPref = findPreference(key);
                        connectionPref.setSummary(prefs.getString(key, ""));
                    }
                }
            };

    @Override
    protected void onResume() {
        super.onResume();

        // Get list of default sources
        Intent intent = getIntent();
        final int[] asid = intent.getIntArrayExtra(AnalyzerFragment.MY_PREFERENCES_MSG_SOURCE_ID);
        final String[] as = intent.getStringArrayExtra(AnalyzerFragment.MY_PREFERENCES_MSG_SOURCE_NAME);

        int nExtraSources = 0;
        for (int id : asid) {
            // See SamplingLoop::run() for the magic number 1000
            if (id >= 1000) nExtraSources++;
        }

        // Get list of supported sources
        AnalyzerUtil au = new AnalyzerUtil(this);
        final int[] audioSourcesId = au.GetAllAudioSource(4);
        KLog.Companion.i(" n_as = " + audioSourcesId.length);
        KLog.Companion.i(" n_ex = " + nExtraSources);
        audioSourcesName = new String[audioSourcesId.length + nExtraSources];
        for (int i = 0; i < audioSourcesId.length; i++) {
            audioSourcesName[i] = au.getAudioSourceName(audioSourcesId[i]);
        }

        // Combine these two sources
        audioSources = new String[audioSourcesName.length];
        int j = 0;
        for (; j < audioSourcesId.length; j++) {
            audioSources[j] = String.valueOf(audioSourcesId[j]);
        }
        for (int i = 0; i < asid.length; i++) {
            // See SamplingLoop::run() for the magic number 1000
            if (asid[i] >= 1000) {
                audioSources[j] = String.valueOf(asid[i]);
                audioSourcesName[j] = as[i];
                j++;
            }
        }

        final ListPreference lp = (ListPreference) findPreference("audioSource");
        lp.setDefaultValue(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        lp.setEntries(audioSourcesName);
        lp.setEntryValues(audioSources);

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(prefListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(prefListener);
    }
}
