/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.alglapp;
//package android.media.cts;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class LooperTest {
    private static final String TAG = "VP8EncoderTest";
    private Vp8EncoderTest mVp8;

    public LooperTest(Vp8EncoderTest vp8) {
        mVp8 = vp8;
    }

//    private class SimpleThread implements Runnable {
    private class SimpleThread extends Thread {
        private int mCount;

        public void run() {
            Log.d(TAG, "Running ...");
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
                mCount++;
                Log.d(TAG, "Thread # " + mCount);
                if (mCount > 10) {
                    break;
                }
            }
            Log.d(TAG, "Done!");
        }
    }

    private class LooperThread extends Thread {
        private final Object mLock = new Object();
        private Handler mHandler;

        public void requestStart() throws InterruptedException {
            Log.d(TAG, "startSync looper");
            this.start();
            synchronized (mLock) {
                while (mHandler == null) {
                    mLock.wait();
                }
            }
            Log.d(TAG, "startSync looper done");
        }

        public void run() {
            Looper.prepare();

            synchronized (mLock) {
                mHandler = new Handler();
                Log.d(TAG, "Running looper ...");
                mLock.notify();
            }
            Looper.loop();
            Log.d(TAG, "Done looper ...");
        }


        public synchronized void runInLooper(Runnable runIf) throws InterruptedException {
            mHandler.post(runIf);
        }

        public synchronized void runInLooperAndWait(Runnable runIf) throws InterruptedException {
            mHandler.post(runIf);
            mHandler.post( new Runnable() {
                @Override
                public void run() {
                    synchronized (mLock) {
                        mLock.notify();
                    }
                }
            } );
            synchronized (mLock) {
                mLock.wait();
            }
        }

        public synchronized void requestStop() {
            // using the handler, post a Runnable that will quit()
            // the Looper attached to  LooperThread
            // obviously, all previously queued tasks will be executed
            // before the loop gets the quit Runnable
            mHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        // This is guaranteed to run on the LooperThread
                        // so we can use myLooper() to get its looper
                        Log.d(TAG, "LooperThread loop quitting by request");

                        Looper.myLooper().quit();
                    }
                }
           );
        }

    }

    public void testSimpleLooper() throws Exception {
        SimpleThread t = new SimpleThread();
//        Thread t = new Thread(new SimpleThread());
        Log.d(TAG, "Start simple thread");
        t.start();
        t.join();
        Log.d(TAG, "Done simple thread");
    }

    public void testLooper() throws Exception {
        LooperThread t = new LooperThread();
        Log.d(TAG, "Start looper test");
        t.requestStart();
//        t.runCounter();
        t.requestStop();
        t.join();
        Log.d(TAG, "Done looper test");
    }

    public void testRunInLooper() throws Exception {
        Log.d(TAG, "Start looper test");
        LooperThread t = new LooperThread();
        t.requestStart();

//        t.runInLooper(
        t.runInLooperAndWait(
            new Runnable() {
                @Override
                public void run() {
                    int mCount = 0;
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                        mCount++;
                        Log.d(TAG, "InLooper Thread # " + mCount);
                        if (mCount > 5) {
                            break;
                        }
                    }
                }
            }
        );

        Log.d(TAG, "Waiting for join looper test");
        t.requestStop();
        t.join();
        Log.d(TAG, "Done looper test");
    }

    public void testVp8InLooper() throws Exception {
        Log.d(TAG, "Start VP8 looper test");
        LooperThread t = new LooperThread();
        t.runInLooper(
            new Runnable() {
                @Override
                public void run() {
/*                    try {
                        mVp8.testSimulcastBitrate();
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }*/
                }
            }
        );
        Log.d(TAG, "Done VP8 looper test");
    }

}

