package com.appacoustic.cointester;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gabrielmorenoibarra.g.GLog;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import fftpack.RealDoubleFFT;

/**
 * Main fragment.
 * Created by Gabriel Moreno on 2017-01-22.
 */
public class CheckerFragment_OLD extends Fragment {

    public static final String TAG = CheckerFragment_OLD.class.getSimpleName();

    private static final int FREQUENCY = 8000;
    private static final int CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BLOCK_SIZE = 256;

    private int frequency;
    private int channelConfiguration;
    private int audioEncoding;
    private int blockSize;

    public static final int HEIGHT_MEDIUM = 150;
    private int factor;

    private AxisImageView ivAxes;
    private Bitmap bitmapSpectrum;
    private Canvas canvasSpectrum;
    private Paint paintSpectrum;

    private int screenWidth;

    private AudioRecord recorder;
    private RecordTask recordTask;
    private RealDoubleFFT transformer;
    private boolean started;

    @BindView(R.id.rlAnalyzerSpectrum) RelativeLayout rlSpectrum;
    //    @BindView(R.id.ivCheckerSpectrum) ImageView ivSpectrum;
    @BindView(R.id.tvAnalyzerCheckerCoinName) TextView tvCoinName;
    @BindView(R.id.tvAnalyzerCheckerCoinPlace) TextView tvCoinPlace;
    @BindView(R.id.fabAnalyzerChecker) FloatingActionButton fab;

    @BindColor(R.color.cadet) int cadet;
    @BindColor(R.color.cadet_1) int cadet1;
    private int magnitudeTextSize;

    public CheckerFragment_OLD() {
    }

    public static CheckerFragment_OLD newInstance() {
        return new CheckerFragment_OLD();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_analyzer, container, false);
        ButterKnife.bind(this, rootView);

        frequency = FREQUENCY;
        channelConfiguration = CHANNEL_CONFIGURATION;
        audioEncoding = AUDIO_ENCODING;
        blockSize = BLOCK_SIZE;

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        screenWidth = display.getWidth();

        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                canvasSpectrum.drawColor(Color.WHITE);
                recordPerform();
            }
        });

        transformer = new RealDoubleFFT(blockSize);

        int width;
        int height;
        if (screenWidth < BLOCK_SIZE * 4) {
            factor = 2;
        } else {
            factor = 4;
        }
        width = BLOCK_SIZE * factor;
        height = HEIGHT_MEDIUM * factor;

        bitmapSpectrum = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        canvasSpectrum = new Canvas(bitmapSpectrum);
        paintSpectrum = new Paint();
        paintSpectrum.setColor(cadet);
//        ivSpectrum.setImageBitmap(bitmapSpectrum);

        magnitudeTextSize = 56;
        ivAxes = new AxisImageView(getContext(), blockSize, width, magnitudeTextSize, cadet1);
        ivAxes.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        rlSpectrum.addView(ivAxes);

        return rootView;
    }

    public void onResume() {
        super.onResume();
        recordPerform();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            recorder.stop();
        } catch (IllegalStateException e) {
            e.printStackTrace();

        }
        recordTask.cancel(true);
    }

    public void recordPerform() {
        started = true;
        recordTask = new RecordTask();
        recordTask.execute();
    }

    private class RecordTask extends AsyncTask<Void, double[], Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            GLog.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + " " + hashCode());
            int bufferSize = AudioRecord.getMinBufferSize(
                    frequency,
                    channelConfiguration,
                    audioEncoding);

            recorder = new AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    frequency,
                    channelConfiguration,
                    audioEncoding,
                    bufferSize);

            int read;
            short[] buffer = new short[blockSize];
            double[] toTransform = new double[blockSize];
            try {
                recorder.startRecording();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            while (started) {
                if (isCancelled()) {
                    started = false;
                    GLog.i(TAG, "Record task cancelled.");
                    break;
                } else {
                    read = recorder.read(buffer, 0, blockSize);
                    for (int i = 0; i < blockSize && i < read; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit
                    }
                    transformer.ft(toTransform);
                    publishProgress(toTransform);
                }

            }
            return true;
        }

        @Override
        protected void onProgressUpdate(double[]... progress) {
            for (int i = 0; i < progress[0].length; i++) {
                int x = factor * i;
                int startY = magnitudeTextSize;
                int stopY = (int) (magnitudeTextSize - (progress[0][i] * 10));
                canvasSpectrum.drawLine(x, startY, x, stopY, paintSpectrum);
            }
//            ivSpectrum.invalidate();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            GLog.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + " " + hashCode());
            super.onPostExecute(result);
            try {
                recorder.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();

            }
            canvasSpectrum.drawColor(Color.WHITE);
//            ivSpectrum.invalidate();
        }
    }

    public void loadCoin(Coin item) {
        tvCoinName.setText(item.getName());
        tvCoinPlace.setText(item.getPlace());
        fab.setImageResource(item.getHead());
    }
}