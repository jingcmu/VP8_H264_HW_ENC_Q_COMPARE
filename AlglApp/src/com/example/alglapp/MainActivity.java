package com.example.alglapp;

import android.os.Bundle;
import android.os.Environment;

import android.app.Activity;
import android.view.Menu;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;

import com.example.alglapp.Vp8CodecTestBase.BufferInfo;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
	private static final String TAG = "VP8CodecTestBase";
    private static final String PATH = "/sdcard/";
    private Button buttonStart;
    private SurfaceView sv;
    private Thread tTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        RelativeLayout layout = new RelativeLayout(this);
        //LinearLayout layout = new LinearLayout(this);
        //layout.setOrientation(LinearLayout.VERTICAL);
        //layout.setOrientation(LinearLayout.HORIZONTAL);

        sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        layout.addView(sv);
        //setContentView(sv);

        buttonStart = new Button(this);
        buttonStart.setBackgroundColor(0xFFE0E0E0);
        buttonStart.setText("Start Test");
        buttonStart.setOnClickListener(mStartTestListener);
        layout.addView(buttonStart);
/*
        try {
            //testVP8Xts();
            //testVP8Cts();
            testVP8Surface();
        }
        catch (Exception e) {
        }*/
        //setContentView(R.layout.activity_main);
        setContentView(layout);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged: " + width + " x " + height);
/*        Runnable rTest = new Runnable() {
            public void run() {
                testVP8Xts();
            }
        };
        tTest = new Thread(rTest);
        tTest.start();
        Log.d(TAG, "surfaceChanged done");*/
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
/*        try {
            tTest.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "surfaceDestroyed done");*/
    }

    public void testTone() throws Exception {

        Log.d(TAG, "Creating tone generator ADT 10");
        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_RING,
                                                  ToneGenerator.MAX_VOLUME);

        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2);
        final long DELAYED = 1000;
        Thread.sleep(DELAYED);
        toneGen.stopTone();

        toneGen.release();
        Log.d(TAG, "Released tone generator");
    }

    public void testLooper() {
        Log.d(TAG, "Start Test Looper");
        Vp8EncoderTest vp8 = new Vp8EncoderTest();
        vp8.setContext(getApplicationContext());

        LooperTest lt = new LooperTest(vp8);
        try {
//            lt.testSimpleLooper();
            //lt.testLooper();
            lt.testRunInLooper();
            //lt.testVp8InLooper();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

	public void testVP8Cts() {
        Log.d(TAG, "Start VP8 encoder");
        Vp8EncoderTest vp8 = new Vp8EncoderTest();
        vp8.setContext(getApplicationContext());
        try {
            vp8.testBasic();
            //vp8.testAsyncEncoding();
            //vp8.testSyncFrame();
            //vp8.testDynamicBitrateChange();
            //vp8.testParallelEncodingAndDecoding();
            //vp8.testEncoderQuality();
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public void testVP8Xts() {
        Log.d(TAG, "Start VP8 encoder");
        Vp8CodecTest vp8 = new Vp8CodecTest();
        //vp8.setContext(getApplicationContext());
        vp8.setContext(this);
        //vp8.setDecodingSurface(sv.getHolder().getSurface());
        try {
            vp8.testBasic();
            //vp8.testBasicThread();
            //vp8.testSimulcastBitrateSync();
            //vp8.testSwTemporalLayers();
            //vp8.testHwTemporalLayers();
            //vp8.testCodecDelay();
            //vp8.testRequestSyncFrame();
            //vp8.testConfiguredSyncFrame();
            //vp8.testDynamicBitrateChange();
            //vp8.testDynamicFramerateChange();
            //vp8.testSimulcastEncoderQuality();
            //vp8.testParallelDecoderQuality();
            //vp8.testHangouts();
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public void testVP8Surface() {
        Log.d(TAG, "Start VP8 surface test");
        TestSurface vp8 = new TestSurface();
        try {
            vp8.testBasic(sv.getHolder().getSurface());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private OnClickListener mStartTestListener = new OnClickListener() {
        public void onClick(View v) {
          Log.d(TAG, "Start test");
          Runnable rTest = new Runnable() {
              public void run() {
                //testVP8Cts();
                testVP8Xts();
                  //testVP8Surface();
              }
          };
          tTest = new Thread(rTest);
          tTest.start();
//          testVP8Xts();
        }
      };


}
