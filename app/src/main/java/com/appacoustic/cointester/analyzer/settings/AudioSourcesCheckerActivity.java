package com.appacoustic.cointester.analyzer.settings;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import com.appacoustic.cointester.R;
import com.appacoustic.cointester.analyzer.AnalyzerUtil;

import java.util.ArrayList;

/**
 * Check all (including unknown) recorder sources by open it and read data.
 */
public class AudioSourcesCheckerActivity extends AppCompatActivity {

    private AnalyzerUtil analyzerUtil;
    private CharSequence testResultSt = null;

    private volatile boolean bShouldStop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_sources_checker);
        // Show the Up button in the action bar.
        setupActionBar();

        analyzerUtil = new AnalyzerUtil(this);
        testResultSt = null;

        final TextView tv = (TextView) findViewById(R.id.tvAudioSourcesChecker);
        tv.setMovementMethod(new ScrollingMovementMethod());
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (getActionBar() != null)
                getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_audio_sources_checker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final TextView tv = (TextView) findViewById(R.id.tvAudioSourcesChecker);
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menuAudioSourcesCheckerStandardSources:
                bShouldStop = true;
                runTest(tv, 1);
                break;
            case R.id.menuAudioSourcesCheckerSupportedSources:
                bShouldStop = true;
                runTest(tv, 7);
                break;
            case R.id.menuAudioSourcesCheckerAllSources:
                bShouldStop = true;
                runTest(tv, 0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final TextView tv = (TextView) findViewById(R.id.tvAudioSourcesChecker);

        if (testResultSt != null) {
            tv.setText(testResultSt);
            return;
        }

        runTest(tv, 7);
    }

    private Thread testerThread;

    private void runTest(final TextView tv, final int testLevel) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (testerThread != null) {
                    try {
                        testerThread.join();
                    } catch (InterruptedException e) {
                        // ???
                    }
                }
                testResultSt = null;
                setTextData(tv, getString(R.string.checker_latency_indication));
                bShouldStop = false;
                testerThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TestAudioRecorder(tv, testLevel);
                    }
                });
                testerThread.start();
            }
        }).start();
    }

    private void setTextData(final TextView tv, final String st) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv.setText(st);
            }
        });
    }

    private void appendTextData(final TextView tv, final String st) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv.append(st);
            }
        });
    }

    // Show supported sample rate and corresponding minimum buffer size.
    private void TestAudioRecorder(final TextView tv, int testLevel) {
        // All possible sample rate
        String[] sampleRates = getResources().getStringArray(R.array.std_sampling_rates);
        String st = getString(R.string.checker_latency_header);

        ArrayList<String> resultMinBuffer = new ArrayList<>();
        for (String sr : sampleRates) {
            int rate = Integer.parseInt(sr.trim());
            int minBufSize = AudioRecord
                    .getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);
            if (minBufSize != AudioRecord.ERROR_BAD_VALUE) {
                resultMinBuffer.add(sr);
                // /2.0 due to ENCODING_PCM_16BIT, CHANNEL_IN_MONO
                st += getString(R.string.checker_latency_row, rate, minBufSize, 1000.0 * minBufSize / 2.0 / rate);
            } else {
                st += sr + getString(R.string.checker_latency_error);
            }
        }
        sampleRates = resultMinBuffer.toArray(new String[0]);

        appendTextData(tv, st);

        // Get audio source list
        int[] audioSourceId = analyzerUtil.GetAllAudioSource(testLevel);
        ArrayList<String> audioSourceStringList = new ArrayList<>();
        for (int id : audioSourceId) {
            audioSourceStringList.add(analyzerUtil.getAudioSourceName(id));
        }
        String[] audioSourceString = audioSourceStringList.toArray(new String[0]);

        appendTextData(tv, getString(R.string.checker_sources_indication));
        for (int ias = 0; ias < audioSourceId.length; ias++) {
            if (bShouldStop) return;
            st = getString(R.string.checker_sources_header, audioSourceString[ias]);
            appendTextData(tv, st);
            for (String sr : sampleRates) {
                if (bShouldStop) return;
                int sampleRate = Integer.parseInt(sr.trim());
                int recBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                st = getString(R.string.checker_sources_freq, sampleRate);

                // wait for AudioRecord fully released...
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                AudioRecord record;
                try {
                    record = new AudioRecord(audioSourceId[ias], sampleRate,
                            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, recBufferSize);
                } catch (IllegalArgumentException e) {
                    st += getString(R.string.checker_sources_error_illegal_argument);
                    record = null;
                }
                if (record != null) {
                    if (record.getState() == AudioRecord.STATE_INITIALIZED) {
                        int numOfReadShort;
                        try {  // try read some samples.
                            record.startRecording();
                            short[] audioSamples = new short[recBufferSize];
                            numOfReadShort = record.read(audioSamples, 0, recBufferSize);
                        } catch (IllegalStateException e) {
                            numOfReadShort = -1;
                        }
                        if (numOfReadShort > 0) {
                            st += getString(R.string.checker_sources_succeed);
                        } else if (numOfReadShort == 0) {
                            st += getString(R.string.checker_sources_error_read_0_byte);
                        } else {
                            st += getString(R.string.checker_sources_error_read_sample);
                        }
                        int as = record.getAudioSource();
                        if (as != audioSourceId[ias]) {  // audio source altered
                            st += getString(R.string.checker_sources_alter_header,
                                    analyzerUtil.getAudioSourceName(as));
                        }
                        record.stop();
                    } else {
                        st += getString(R.string.checker_sources_error_initialization);
                    }
                    record.release();
                }
                st += getString(R.string.checker_sources_end);
                appendTextData(tv, st);
            }
        }

        testResultSt = tv.getText();
    }
}
