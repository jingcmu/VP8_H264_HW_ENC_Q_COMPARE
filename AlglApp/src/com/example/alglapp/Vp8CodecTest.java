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
//package com.example.android.vp8activity;
//package com.google.android.xts.media;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
//import com.google.android.xts.media.R;

/**
 * Verification test for vp8 encoder and decoder.
 *
 * A raw yv12 stream is encoded at various settings and written to an IVF
 * file. Encoded stream bitrate and key frame interval are checked against target values.
 * The stream is later decoded by vp8 decoder to verify frames are decodable and to
 * calculate PSNR values for various bitrates.
 *
 * Test list for KK devices:
 *    testSimulcastBitrateSync(),
 *    testCodecDelay(),
 *    testSimulcastEncoderQuality().
 *
 * Test list for L devices (in addition to all KK devices tests):
 *    testSwTemporalLayers(),
 *    testHwTemporalLayers() - disabled until HW codec will support temporal layers,
 *    testRequestSyncFrame(),
 *    testConfiguredSyncFrame() - disabled until HW codec will support key frame interval setting,
 *    testDynamicBitrateChange(),
 *    testDynamicFramerateChange(),
 *    testParallelDecoderQuality(),
 *    testHangouts().
 */
public class Vp8CodecTest extends Vp8CodecTestBase {

    private static final String ENCODED_IVF_BASE = "football";
    private static final String INPUT_YUV = "football_qvga.yuv";
    //private static final String INPUT_YUV = null;

    //private static final String ENCODED_IVF_BASE = "ducks";
    //private static final String INPUT_YUV = "ducks_vga.yuv";

    //private static final String ENCODED_IVF_BASE = "marco";
    //private static final String INPUT_YUV = "marco_vga.yuv";

    //private static final String ENCODED_IVF_BASE = "nicklas";
    //private static final String INPUT_YUV = "nicklas_720p.yuv";

    //private static final String ENCODED_IVF_BASE = "record";
    //private static final String INPUT_YUV = "record_720p.yuv";

    //private static final String ENCODED_IVF_BASE = "ducks";
    //private static final String INPUT_YUV = "ducks_720p.yuv";

    //private static final String ENCODED_IVF_BASE = "/sdcard/foreman";
    //private static final String INPUT_YUV = "/sdcard/foreman_cif.yuv";
    //private static final String OUTPUT_YUV = "/sdcard/foreman_cif_out.yuv";

    // YUV stream properties.
    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;
    private static final int WIDTH_HANGOUTS = 320;
    private static final int HEIGHT_HANGOUTS = 180;
    //private static final int WIDTH = 640;
    //private static final int HEIGHT = 480;
    //private static final int WIDTH = 1280;
    //private static final int HEIGHT = 720;
    private static final int FPS = 30;
    // Default encoding bitrates.
    private static final int BITRATE_180P = 200000;
    private static final int BITRATE_QVGA = 400000;
    private static final int BITRATE_360P = 500000;
    private static final int BITRATE_VGA = 700000;
    private static final int BITRATE_HD = 1200000;
    private static final int[] BITRATE_LIST = { BITRATE_QVGA, BITRATE_VGA, BITRATE_HD };
    // Default sync frame interval
    private static final int SYNC_FRAME_INTERVAL = 3000;
    // Maximum number of temporal layers
    private static final int MAX_TS_LAYERS = 3;
    // Default encoding bitrate mode
    private static final int BITRATE_MODE = VIDEO_ControlRateConstant;
    //private static final int BITRATE_MODE = VIDEO_ControlRateVariable;
    // List of bitrates used in quality and basic bitrate tests.
//    private static final double[] TEST_BITRATES_SCALES = { 1, 2 };
    private static final double[] TEST_BITRATES_SCALES = { 1 };
    // Maximum allowed bitrate variation from the target value.
    private static final double MAX_BITRATE_VARIATION = 0.15;
    // Maximum allowed dynamic bitrate variation from the target value.
    private static final double MAX_DYNAMIC_BITRATE_VARIATION = 0.25;
    // Maximum allowed total encoder and decoder frame delay in ms.
    private static final int MAX_DELAY_VALUE_MS = 2 * 1000 / FPS;
    // Maximum allowed average PSNR difference of HW encoder comparing to reference SW encoder.
    private static final double MAX_AVERAGE_PSNR_DIFFERENCE = 1.5;
    // Maximum allowed minimum PSNR difference of HW encoder comparing to reference SW encoder.
    private static final double MAX_MINIMUM_PSNR_DIFFERENCE = 3.0;
    // Maximum allowed average PSNR difference of multiple layer configuration comparing
    // to single layer.
    private static final double MAX_AVERAGE_PSNR_TSLAYER_DIFFERENCE = 2;
    // Maximum allowed average key frame interval variation from the target value.
    private static final int MAX_AVERAGE_KEYFRAME_INTERVAL_VARIATION = 1;
    // Maximum allowed key frame interval variation from the target value.
    private static final int MAX_KEYFRAME_INTERVAL_VARIATION = 3;
    // QCOM OMX component prefix
    private static final String OMX_QCOM_CODEC_PREFIX = "OMX.qcom";
    // Minimum SDK version with VP8 to test - we only test on KK or later
    private static final int MIN_VP8_SDK_VERSION = android.os.Build.VERSION_CODES.KITKAT;
    // Current SDK version
    private static final int CURRENT_VP8_SDK_VERSION = android.os.Build.VERSION.SDK_INT;

    /**
     * Helper function to return encoder scale values used for all simulcast tests.
     *
     * Returns { 1, 2 } for KitKat to test QVGA and VGA resolutions and
     * { 1, 2, 4 } for later versions to test QVGA, VGA and 720p resolutions.
     */
    private int[] getVideoScaleValues() {
        if (android.os.Build.VERSION.SDK_INT > MIN_VP8_SDK_VERSION) {
            return new int[] { 1, 2, 4 };
        } else {
            return new int[] { 1, 2 };
        }
    }

    /**
     * A basic test for VP8 asynchronous encoding and decoding.
     *
     * Encodes 9 seconds of raw stream with default configuration options,
     * and then decodes it to verify the bitstream.
     */
    public void testBasic() throws Exception {
        Log.d(TAG, "Current SDK version:" + CURRENT_VP8_SDK_VERSION +
                ". Build:" + android.os.Build.VERSION.SDK_INT);
        if (CURRENT_VP8_SDK_VERSION < MIN_VP8_SDK_VERSION) {
            return;
        }
        int encodeSeconds = 9;
        //int encodeSeconds = 10;
        int[] scaleValues = { 1 };
        Log.d(TAG, "---------- testBasic------------");
        //isResolutionSupported(8192, 4096 , true);

        // First test the encoder running in a looper thread with buffer callbacks enabled.
        CodecStreamParameters params = getDefaultCodecStreamParameters(
                INPUT_YUV,
                ENCODED_IVF_BASE,
                encodeSeconds,
                WIDTH,
                HEIGHT,
                FPS,
                BITRATE_MODE,
                BITRATE_QVGA,
                //BITRATE_VGA,
                //BITRATE_HD,
                //2000000,
                true);
        //params.forceSwCodec = true;
        //params.temporalLayers = 1;
        //params.cacheInputData = true;
        //params.encodedIvfFilename = null;
        //params.frameRateSet[0] = 10;
        params.syncFrameInterval = 3000;
        //params.bitrateSet[0] = 1425000;
/*        params.frameRateSet = new int[encodeSeconds * FPS];
        Random rand = new Random(System.currentTimeMillis());
        params.frameRateSet[0] = FPS;
        for (int i = 1; i < params.frameRateSet.length ; i++) {
            int fpsDiff = (int) Math.round( (0.6 * (2 * rand.nextFloat() - 0.9) * FPS));
            params.frameRateSet[i] = FPS + fpsDiff;
        }
*/
        ArrayList<BufferInfo> bufInfoEnc = encode(params);
        computeEncodingStatistics(bufInfoEnc);
        averageCodecTimeUs(0, bufInfoEnc);

        // Check decoding
        //params.outputYuvFilename = null;
        //params.useSurface = true;
        //params.forceSwCodec = true;
        ArrayList<BufferInfo> bufInfoDec = decode(params);
        //averageCodecTimeUs(0, bufInfoDec);
        computeDecodingStatistics(params.inputYuvFilename, R.raw.football_qvga,
                 params.outputYuvFilename, WIDTH, HEIGHT);
        //deleteTemporaryFiles(params);
        Log.d(TAG, "testBasic PASSED");
    }

    public void testBasicThread() throws Exception {
        Log.d(TAG, "Current SDK version:" + CURRENT_VP8_SDK_VERSION +
                ". Build:" + android.os.Build.VERSION.SDK_INT);
        if (CURRENT_VP8_SDK_VERSION < MIN_VP8_SDK_VERSION) {
            return;
        }
        //int encodeSeconds = 1;
        int encodeSeconds = 9;
        //int encodeSeconds = 10;
        int[] scaleValues = { 1 };
        Log.d(TAG, "---------- testBasicThread------------");

        // First test the encoder running in a looper thread with buffer callbacks enabled.
        final CodecStreamParameters encParams = getDefaultCodecStreamParameters(
                INPUT_YUV,
                ENCODED_IVF_BASE,
                encodeSeconds,
                WIDTH,
                HEIGHT,
                FPS,
                BITRATE_MODE,
                BITRATE_QVGA,
                //BITRATE_VGA,
                //BITRATE_HD,
                //2000000,
                true);


        final CodecStreamParameters decParams = getDefaultCodecStreamParameters(
                INPUT_YUV,
                ENCODED_IVF_BASE,
                encodeSeconds,
                WIDTH,
                HEIGHT,
                FPS,
                BITRATE_MODE,
                BITRATE_QVGA,
                //BITRATE_VGA,
                //BITRATE_HD,
                //2000000,
                true);

        Runnable rEnc = new Runnable() {
            public void run() {
                ArrayList<BufferInfo> bufInfoEnc = null;
                try {
                    bufInfoEnc =  encode(encParams);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                computeEncodingStatistics(bufInfoEnc);
            }
        };

        Runnable rDec = new Runnable() {
            public void run() {
                ArrayList<BufferInfo> bufInfoDec = null;
                try {
                    bufInfoDec =  decode(decParams);
                    averageCodecTimeUs(0, bufInfoDec);
                    computeDecodingStatistics(decParams.inputYuvFilename, R.raw.football_qvga,
                            decParams.outputYuvFilename, WIDTH, HEIGHT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        encParams.encodedIvfFilename = SDCARD_DIR + File.separator + ENCODED_IVF_BASE +
                "_" + encParams.frameWidth + "x" + encParams.frameHeight + "_0.ivf";
        Thread tEnc = new Thread(rEnc);
        Thread tDec = new Thread(rDec);
        tEnc.start();
        tDec.start();
        tEnc.join();
        tDec.join();


        Log.d(TAG, "testBasicThread PASSED");
    }


    private void testSimulcastBitrate(boolean syncMode) throws Exception {
        if (CURRENT_VP8_SDK_VERSION < MIN_VP8_SDK_VERSION) {
            return;
        }
        int encodeSeconds = 9;  // Encoding sequence duration in seconds for each bitrate.
        int[] scaleValues = getVideoScaleValues();
        scaleValues = supportedScaleValues(WIDTH, HEIGHT, scaleValues, true);
        int numCodecs = scaleValues.length;
        int[] bitrateList = new int[numCodecs];

        Log.d(TAG, "---------- testSimulcastBitrate. Sync: " + syncMode);
        for (double bitrateScale : TEST_BITRATES_SCALES) {
            for (int i = 0; i < numCodecs; i++) {
                bitrateList[i] = (int)(bitrateScale * BITRATE_LIST[i]);
            }
            ArrayList<CodecStreamParameters> params = getDefaultCodecStreamParameterList(
                    INPUT_YUV,
                    ENCODED_IVF_BASE,
                    encodeSeconds,
                    scaleValues,
                    WIDTH,
                    HEIGHT,
                    FPS,
                    BITRATE_MODE,
                    bitrateList,
                    syncMode);

            // Encodes 3 streams simultaneously
            ArrayList<ArrayList<BufferInfo>> bufInfos = encodeSimulcast(
                    WIDTH, HEIGHT, params);

            // Check average bitrate value - should be within 15% of the target value.
            ArrayList<Vp8EncodingStatistics> statistics =
                    computeSimulcastEncodingStatistics(bufInfos);

            for (int i = 0; i < numCodecs; i++) {
                int encoderDelayMs = maxPresentationTimeDifference(bufInfos.get(i));
                Log.d(TAG, "Encoder delay #" + i + " : " + encoderDelayMs);
                int actualBitrate = statistics.get(i).mAverageBitrate;
                int targetBitrate = params.get(i).bitrateSet[0];
                // QCOM 720p encoder will generate too high bitrate - b/12910631.
                // TODO(glaznev): Remove this condition once QCOM will submit a fix
                if (scaleValues[i] == 4) {
                    CodecProperties properties = getVp8CodecProperties(true, false);
                    if (properties.codecName.contains(OMX_QCOM_CODEC_PREFIX)) {
                        continue;
                    }
                }
                assertEquals("Stream bitrate " + actualBitrate +
                        " is different from the target " + targetBitrate,
                        targetBitrate, actualBitrate, MAX_BITRATE_VARIATION * targetBitrate);
            }

            // Check if streams are decodable
            decodeParallel(params);
            deleteTemporaryFiles(params);
        }
        Log.d(TAG, "testSimulcastBitrate PASSED");
    }

    /**
     * Simulcast encoding tests for VP8 encoder.
     *
     * Encodes 9 seconds of raw stream in sync or async mode at various bitrates and
     * resolutions, and then decodes it to verify the bitstream.
     * Checks the average bitrate is within 15% of the target value.
     */
    public  void testSimulcastBitrateSync() throws Exception {
        testSimulcastBitrate(true);
    }

    public  void FIXME_testSimulcastBitrateAsync() throws Exception {
        testSimulcastBitrate(false);
    }

    /**
     * Temporal layer support test for VP8 encoder and decoder.
     *
     * Encodes 9 seconds of raw stream using 2 or 3 layers.
     * If encoder or decoder do not support temporal layers decoding
     * of lower layers will result in distorted output.
     * Test will compare PSNR for base layer decoding with PSNR when all
     * enhancement layers are decoded.
     */
    private void testTemporalLayers(boolean useSwCodec) throws Exception {
        if (CURRENT_VP8_SDK_VERSION <= MIN_VP8_SDK_VERSION) {
            return;
        }
        Log.d(TAG, "---------- testTemporalLayers------------");

        int encodeSeconds = 9;
        double[][] psnr = new double[MAX_TS_LAYERS][MAX_TS_LAYERS];

        for (int tsLayers = 1; tsLayers <= MAX_TS_LAYERS; tsLayers ++) {
            CodecStreamParameters params = getDefaultCodecStreamParameters(
                    INPUT_YUV,
                    ENCODED_IVF_BASE,
                    encodeSeconds,
                    WIDTH,
                    HEIGHT,
                    FPS,
                    BITRATE_MODE,
                    BITRATE_QVGA,
                    true);
            params.forceSwCodec = useSwCodec;
            params.temporalLayers = tsLayers;

            ArrayList<BufferInfo> bufInfo = encode(params);
            Vp8EncodingStatistics statistics = computeEncodingStatistics(bufInfo);

            // Check average bitrate value - should be within 15% of the target value.
            int actualBitrate = statistics.mAverageBitrate;
            assertEquals("Stream bitrate " + actualBitrate +
                    " is different from the target " + BITRATE_QVGA,
                    BITRATE_QVGA, actualBitrate, MAX_BITRATE_VARIATION * BITRATE_QVGA);

            for (int maxDecodedLayer = 1; maxDecodedLayer <= tsLayers; maxDecodedLayer++) {
                // Check decoding of video stream with "maxDecodedLayers" lower layers
                params.decodingRateDecimator = 1 << (tsLayers - maxDecodedLayer);
                bufInfo = decode(params);
                Vp8DecodingStatistics decodingStatistics = computeDecodingStatisticsEx(
                        params.inputYuvFilename, R.raw.football_qvga,
                        params.outputYuvFilename, WIDTH, HEIGHT,
                        params.decodingRateDecimator);
                psnr[tsLayers - 1][maxDecodedLayer - 1] = decodingStatistics.mAveragePSNR;
            }
            deleteTemporaryFiles(params);
        }

        for (int tsLayers = 1; tsLayers <= MAX_TS_LAYERS; tsLayers ++) {
            for (int maxDecodedLayer = 1; maxDecodedLayer <= tsLayers; maxDecodedLayer++) {
                Log.d(TAG, "PSNR for maximum layer #" + maxDecodedLayer + " out of " +
                        tsLayers + ": " + psnr[tsLayers - 1][maxDecodedLayer - 1]);
            }
        }

        // Check PSNRs of multiple layers
        for (int tsLayers = 2; tsLayers <= MAX_TS_LAYERS; tsLayers ++) {
            // First check PSNR for multiple layer configuration is no less than 2 dB
            // comparing to single layer case.
            if (psnr[tsLayers - 1][tsLayers - 1] <
                    psnr[0][0] - MAX_AVERAGE_PSNR_TSLAYER_DIFFERENCE) {
                throw new RuntimeException("Low average PSNR " +
                    psnr[tsLayers - 1][tsLayers - 1] + " comparing to reference PSNR " +
                    psnr[0][0] + " for " + tsLayers + "ts layers.");

            }
            for (int maxDecodedLayer = 1; maxDecodedLayer <= tsLayers - 1; maxDecodedLayer++) {
                // Then check PSNR of lower layers is no less than 2 dB comparing to
                // the case when all enhancement layers are decoded.
                if (psnr[tsLayers - 1][maxDecodedLayer - 1] <
                        psnr[tsLayers - 1][tsLayers - 1] - MAX_AVERAGE_PSNR_TSLAYER_DIFFERENCE) {
                    throw new RuntimeException("Low average PSNR " +
                        psnr[tsLayers - 1][maxDecodedLayer - 1]);
                }
            }
        }
        Log.d(TAG, "testTemporalLayers PASSED");
    }


    public void testSwTemporalLayers() throws Exception {
        testTemporalLayers(true);
    }

    public void testHwTemporalLayers() throws Exception {
        // TODO(glaznev): Uncomment this once HW vendor will support temporal layers
        //testTemporalLayers(false);
    }

    /**
     * Encoder and decoder delay test for VP8 encoder.
     *
     * Encodes 9 seconds of raw 30 fps stream with default configuration options,
     * and then decodes it. Checks the encoder and decoder can  process data
     * in real-time, without buffering more than 1 frame internally.
     */
    public void testCodecDelay() throws Exception {
        if (CURRENT_VP8_SDK_VERSION < MIN_VP8_SDK_VERSION) {
            return;
        }
        int encodeSeconds = 9;

        Log.d(TAG, "---------- testCodecDelay------------");
        CodecStreamParameters params = getDefaultCodecStreamParameters(
                INPUT_YUV,
                ENCODED_IVF_BASE,
                encodeSeconds,
                WIDTH,
                HEIGHT,
                FPS,
                BITRATE_MODE,
                BITRATE_QVGA,
                true);

        ArrayList<BufferInfo> bufInfoEncoder = encode(params);
        int encoderDelayMs = maxPresentationTimeDifference(bufInfoEncoder);

        ArrayList<BufferInfo> bufInfoDecoder = decode(params);
        int decoderDelayMs = maxPresentationTimeDifference(bufInfoDecoder);
        deleteTemporaryFiles(params);

        Log.d(TAG, "Encoder delay: " + encoderDelayMs + " ms. Decoder delay: "
                + decoderDelayMs + " ms. Limit: " + MAX_DELAY_VALUE_MS + " ms.");
        assertTrue("High codec delay", encoderDelayMs + decoderDelayMs <= MAX_DELAY_VALUE_MS);
        Log.d(TAG, "testCodecDelay PASSED");
    }

    /**
     * Check if MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME is honored.
     *
     * Encodes 9 seconds of raw stream, requests a sync frame every second (30 frames)
     * and compares actual number of key frames with the expected value.
     * The test does not verify the output stream.
     */
    public void testRequestSyncFrame() throws Exception {
        // Due to b/12035976 Qualcomm VP8 encoder will insert key frames every 15
        // frames - so check key frame interval only for L and later.
        if (CURRENT_VP8_SDK_VERSION <= MIN_VP8_SDK_VERSION) {
            return;
        }
        int encodeSeconds = 9;
        Log.d(TAG, "---------- testRequestSyncFrame ------------");
        testSyncFrame(SYNC_FRAME_INTERVAL, FPS, encodeSeconds);
        Log.d(TAG, "testRequestSyncFrame PASSED");
    }

    /**
     * Check if key frame interval set during encoder configuration is honored.
     *
     * Set the encoder to generate key frame every second. Encodes 9 seconds of raw video
     * stream and compares actual number of key frames with the expected value.
     * The test does not verify the output stream.
     */
    public void testConfiguredSyncFrame() throws Exception {
        // Due to b/11969958 syncFrameIntervalConfigured is not honored - so check
        // key frame interval only for L and later SDK.
        if (CURRENT_VP8_SDK_VERSION <= MIN_VP8_SDK_VERSION) {
            return;
        }
        int encodeSeconds = 9;
        Log.d(TAG, "---------- testConfiguredSyncFrame -----------");
        testSyncFrame(FPS, 0, encodeSeconds);
        Log.d(TAG, "testConfiguredSyncFrame PASSED");
    }

    private void testSyncFrame(int syncFrameIntervalConfigured, int syncFrameIntervalRequest,
            int encodeSeconds) throws Exception {
        CodecStreamParameters params = getDefaultCodecStreamParameters(
                INPUT_YUV,
                ENCODED_IVF_BASE,
                encodeSeconds,
                WIDTH,
                HEIGHT,
                FPS,
                BITRATE_MODE,
                BITRATE_QVGA,
                true);
        params.syncFrameInterval = syncFrameIntervalConfigured;
        params.syncForceFrameInterval = syncFrameIntervalRequest;

        // Check configured key frame interval only for SW codec  - b/11969958.
        // TODO(glaznev): Remove this once HW vendor will support key frame interval configuration
        if (syncFrameIntervalConfigured > 0) {
            CodecProperties properties = getVp8CodecProperties(true, params.forceSwCodec);
            if (!properties.isGoogleSwCodec()) {
                return;
            }
        }

        ArrayList<BufferInfo> bufInfos = encode(params);
        Vp8EncodingStatistics statistics = computeEncodingStatistics(0, bufInfos);
        deleteTemporaryFiles(params);

        // Get the number of expected key frames from configuration
        int expectedKeyFrames;
        if (syncFrameIntervalRequest > 0) {  // sync frames are forced
            expectedKeyFrames = encodeSeconds * FPS / syncFrameIntervalRequest;
        } else {
            expectedKeyFrames = encodeSeconds * FPS / syncFrameIntervalConfigured;
        }

        // First check if we got expected number of key frames.
        int actualKeyFrames = statistics.mKeyFrames.size();
        if (actualKeyFrames != expectedKeyFrames) {
            throw new RuntimeException("Number of key frames " + actualKeyFrames +
                    " is different from the expected " + expectedKeyFrames);
        }

        // Check key frame intervals:
        // Average value should be within +/- 1 frame of the target value,
        // maximum value should not be greater than target value + 3,
        // and minimum value should not be less that target value - 3.
        if (Math.abs(statistics.mAverageKeyFrameInterval - FPS) >
            MAX_AVERAGE_KEYFRAME_INTERVAL_VARIATION
            || (statistics.mMaximumKeyFrameInterval - FPS > MAX_KEYFRAME_INTERVAL_VARIATION)
            || (FPS - statistics.mMinimumKeyFrameInterval > MAX_KEYFRAME_INTERVAL_VARIATION)) {
            throw new RuntimeException(
                    "Key frame intervals are different from the expected " + FPS);
        }

        // Finally check average bitrate value - should be within 15% of the target value.
        int actualBitrate = statistics.mAverageBitrate;
        int targetBitrate = params.bitrateSet[0];
        assertEquals("Stream bitrate " + actualBitrate +
                " is different from the target " + targetBitrate,
                targetBitrate, actualBitrate, MAX_BITRATE_VARIATION * targetBitrate);
    }

    /**
     * Check if MediaCodec.PARAMETER_KEY_VIDEO_BITRATE is honored.
     *
     * Run the the encoder for 12 seconds. Request changes to the
     * bitrate every 4 seconds and ensure the encoder responds.
     */
    public void testDynamicBitrateChange() throws Exception {
        if (CURRENT_VP8_SDK_VERSION <= MIN_VP8_SDK_VERSION) {
            return;
        }
        Log.d(TAG, "---------- testDynamicBitrateChange ------------");
        int encodeSeconds = 12;    // Encoding sequence duration in seconds.
        int[] bitrateValues = { 1200000, 300000, 600000 };  // List of bitrates to test.
        CodecStreamParameters params = getDefaultCodecStreamParameters(
                INPUT_YUV,
                ENCODED_IVF_BASE,
                encodeSeconds,
                WIDTH,
                HEIGHT,
                FPS,
                BITRATE_MODE,
                bitrateValues[0],
                true);

        // Number of seconds for each bitrate
        int stepSeconds = encodeSeconds / bitrateValues.length;

        // Fill the bitrates values in encoding parameter structure.
        params.bitrateSet = new int[encodeSeconds * FPS];
        for (int i = 0; i < bitrateValues.length ; i++) {
            Arrays.fill(params.bitrateSet,
                    i * encodeSeconds * FPS / bitrateValues.length,
                    (i + 1) * encodeSeconds * FPS / bitrateValues.length,
                    bitrateValues[i]);
        }

        ArrayList<BufferInfo> bufInfos = encode(params);
        Vp8EncodingStatistics statistics = computeEncodingStatistics(0, bufInfos);
        deleteTemporaryFiles(params);

        // Calculate actual average bitrates for every [stepSeconds] second.
        int[] bitrateActualValues = new int[bitrateValues.length];
        for (int i = 0; i < bitrateValues.length ; i++) {
            bitrateActualValues[i] = 0;
            for (int j = i * stepSeconds; j < (i + 1) * stepSeconds; j++) {
                bitrateActualValues[i] += statistics.mBitrates.get(j);
            }
            bitrateActualValues[i] /= stepSeconds;
            Log.d(TAG, "Actual bitrate for interval #" + i + " : " + bitrateActualValues[i] +
                    ". Target: " + bitrateValues[i]);

            assertEquals("Stream bitrate " + bitrateActualValues[i] +
                    " is different from the target " + bitrateValues[i],
                    bitrateValues[i],  bitrateActualValues[i],
                    MAX_DYNAMIC_BITRATE_VARIATION * bitrateValues[i]);
        }
        Log.d(TAG, "testDynamicBitrateChange PASSED");
    }

    /**
     * Check if framerate can by dynamically changed using video buffer timestamps.
     *
     * Run the the encoder for 9 seconds. Request changes to the framerate every
     * 3 seconds and ensure the encoder still maintains target bitrate and framerate.
     */
    public void testDynamicFramerateChange() throws Exception {
        if (CURRENT_VP8_SDK_VERSION <= MIN_VP8_SDK_VERSION) {
            return;
        }
        Log.d(TAG, "---------- testDynamicFramerateChange ------------");
        int encodeSeconds = 15;    // Encoding sequence duration in seconds.
        int[] framerateValues = { 10, 30, 15 };  // List of framerates to test.
        CodecStreamParameters params = getDefaultCodecStreamParameters(
                INPUT_YUV,
                ENCODED_IVF_BASE,
                encodeSeconds,
                WIDTH,
                HEIGHT,
                FPS,
                BITRATE_MODE,
                BITRATE_QVGA,
                true);

        // Fill the bitrates values in encoding parameter structure.
        params.frameRateSet = new int[encodeSeconds * FPS];
        for (int i = 0; i < framerateValues.length ; i++) {
            Arrays.fill(params.frameRateSet,
                    i * encodeSeconds * FPS / framerateValues.length,
                    (i + 1) * encodeSeconds * FPS / framerateValues.length,
                    framerateValues[i]);
        }

        ArrayList<BufferInfo> bufInfos = encode(params);
        Vp8EncodingStatistics statistics = computeEncodingStatistics(0, bufInfos);
        deleteTemporaryFiles(params);

        // Calculate actual average bitrates and framerates for every target value of framerate.
        int[] bitrateActualValues = new int[framerateValues.length];
        int[] framerateActualValues = new int[framerateValues.length];
        int startSecond = 0;
        for (int i = 0; i < framerateValues.length ; i++) {
            bitrateActualValues[i] = 0;
            framerateActualValues[i] = 0;
            int durationSecond = (encodeSeconds / framerateValues.length) *
                    (FPS / framerateValues[i]);
            for (int j = startSecond; j < startSecond + durationSecond; j++) {
                framerateActualValues[i] += statistics.mFrames.get(j);
                bitrateActualValues[i] += statistics.mBitrates.get(j);
            }
            framerateActualValues[i] = (framerateActualValues[i] + durationSecond / 2)
                    / durationSecond;
            bitrateActualValues[i] /= durationSecond;
            Log.d(TAG, "Frame interval #" + i + ". Interval: " + startSecond + " - " +
                    (startSecond + durationSecond) + " sec.");
            Log.d(TAG, "  Actual framerate: " + framerateActualValues[i] +
                    ". Target: " + framerateValues[i]);
            Log.d(TAG, "  Actual bitrate: " + bitrateActualValues[i] + ". Target: " + BITRATE_QVGA);
            startSecond += durationSecond;

            assertEquals("Stream bitrate " + bitrateActualValues[i] +
                    " is different from the target " + BITRATE_QVGA,
                    BITRATE_QVGA,  bitrateActualValues[i],
                    MAX_DYNAMIC_BITRATE_VARIATION * BITRATE_QVGA);
            // Actual framerate should not differ from the target.
            assertEquals("Stream framerate " + framerateActualValues[i] +
                    " is different from the target " + framerateValues[i],
                    framerateValues[i],  framerateActualValues[i]);
        }
        Log.d(TAG, "testDynamicFramerateChange PASSED");
    }

    /**
     * Check simulcast encoding quality by decoding output streams and calculating PSNR.
     *
     * Run the the encoder for 5 seconds for each bitrate and resolution and calculate PSNR
     * for all encoded output streams.
     * Compare average and minimum PSNR of hw codec with PSNR values of reference sw codec.
     */
    public void testSimulcastEncoderQuality() throws Exception {
        if (CURRENT_VP8_SDK_VERSION < MIN_VP8_SDK_VERSION) {
            return;
        }
        if (!isHwCodecExist()) {
            return; // no need to run this test if only SW codec exist
        }
        int encodeSeconds = 9;
        int[] scaleValues = getVideoScaleValues();
        scaleValues = supportedScaleValues(WIDTH, HEIGHT, scaleValues, true);

        int numCodecs = scaleValues.length;

        // Allocate arrays for storing streams bitrates and PSNRs
        double[] psnrHwCodecAverage = new double[numCodecs];
        double[] psnrHwCodecMin = new double[numCodecs];
        int[] bitrateHwCodec = new int[numCodecs];
        double[] psnrSwCodecAverage = new double[numCodecs];
        double[] psnrSwCodecMin = new double[numCodecs];
        int[] bitrateSwCodec = new int[numCodecs];
        int[] bitrateTarget = new int[numCodecs];
        int[] encodingWidth = new int[numCodecs];
        int[] encodingHeight = new int[numCodecs];

        Log.d(TAG, "---------- testSimulcastEncoderQuality ------------");
        // Run sw and platform specific encoders and compare PSNR
        // of hw codec with PSNR of sw codec.
        ArrayList<CodecStreamParameters> params = getDefaultCodecStreamParameterList(
                INPUT_YUV,
                ENCODED_IVF_BASE,
                encodeSeconds,
                scaleValues,
                WIDTH,
                HEIGHT,
                FPS,
                BITRATE_MODE,
                BITRATE_LIST,
                true);

        // Store some encoding parameters for later use
        for (int i = 0; i < numCodecs; i++) {
            bitrateTarget[i] = params.get(i).bitrateSet[0];
            encodingWidth[i] = params.get(i).frameWidth;
            encodingHeight[i] = params.get(i).frameHeight;
        }

        // Run sw codec first - use its PSNR values as reference values.
        for (int i = 0; i < numCodecs; i++) {
            params.get(i).forceSwCodec = true;
        }
        ArrayList<ArrayList<BufferInfo>> bufInfos = encodeSimulcast( WIDTH, HEIGHT, params);

        ArrayList<Vp8EncodingStatistics>  encodingStatistics =
                computeSimulcastEncodingStatistics(bufInfos);
        for (int i = 0; i < numCodecs; i++) {
            bitrateSwCodec[i] = encodingStatistics.get(i).mAverageBitrate;
        }

        decodeParallel(params);
        for (int i = 0; i < numCodecs; i++) {
            Vp8DecodingStatistics decodingStatistics = computeDecodingStatistics(
                    params.get(i).scaledYuvFilename, R.raw.football_qvga,
                    params.get(i).outputYuvFilename,
                    encodingWidth[i], encodingHeight[i]);
            psnrSwCodecAverage[i] = decodingStatistics.mAveragePSNR;
            psnrSwCodecMin[i] = decodingStatistics.mMinimumPSNR;
        }

        // Run platform specific codec next
        for (int i = 0; i < numCodecs; i++) {
            params.get(i).forceSwCodec = false;
        }
        bufInfos = encodeSimulcast(WIDTH, HEIGHT, params);

        encodingStatistics = computeSimulcastEncodingStatistics(bufInfos);
        for (int i = 0; i < numCodecs; i++) {
            bitrateHwCodec[i] = encodingStatistics.get(i).mAverageBitrate;
        }

        decodeParallel(params);
        for (int i = 0; i < numCodecs; i++) {
            Vp8DecodingStatistics decodingStatistics = computeDecodingStatistics(
                    params.get(i).scaledYuvFilename, R.raw.football_qvga,
                    params.get(i).outputYuvFilename,
                    encodingWidth[i], encodingHeight[i]);
            psnrHwCodecAverage[i] = decodingStatistics.mAveragePSNR;
            psnrHwCodecMin[i] = decodingStatistics.mMinimumPSNR;
        }
        deleteTemporaryFiles(params);

        // Compare average and minimum PSNR of platform codec with reference sw codec -
        // average PSNR for platform codec should be no more than 1.5 dB less than reference PSNR
        // and minumum PSNR - no more than 3 dB less than reference minimum PSNR.
        // These PSNR difference numbers  are arbitrary for now, will need further estimation
        // when more devices with hw VP8 codec will appear.
        for (int i = 0; i < numCodecs; i++) {
            Log.d(TAG, "Resolution: " + encodingWidth[i] + " x " +  encodingHeight[i] +
                    ". Target bitrate:" + bitrateTarget[i]);
            Log.d(TAG, "  Sw: Bitrate: " + bitrateSwCodec[i] + ". PSNR Average: " +
                    psnrSwCodecAverage[i] + ". Minimum: " + psnrSwCodecMin[i]);
            Log.d(TAG, "  Hw: Bitrate: " + bitrateHwCodec[i] + ". PSNR Average: " +
                    psnrHwCodecAverage[i] + ". Minimum: " + psnrHwCodecMin[i]);
            if (psnrHwCodecAverage[i] < psnrSwCodecAverage[i] - MAX_AVERAGE_PSNR_DIFFERENCE) {
                throw new RuntimeException("Low average PSNR " + psnrHwCodecAverage[i] +
                        " comparing to reference PSNR " + psnrSwCodecAverage[i] +
                        " for bitrate " + bitrateTarget[i]);
            }
            if (psnrHwCodecMin[i] < psnrSwCodecMin[i] - MAX_MINIMUM_PSNR_DIFFERENCE) {
                throw new RuntimeException("Low minimum PSNR " + psnrHwCodecMin[i] +
                        " comparing to reference PSNR " + psnrSwCodecMin[i] +
                        " for bitrate " + bitrateTarget[i]);
            }
        }
        Log.d(TAG, "testEncoderQuality PASSED");
    }

    /**
     * Check parallel decoding quality by calculating PSNR of output streams.
     *
     * Run parallel decoding of one 720p and four 180p streams.
     * Compare average and minimum PSNR of hw codec with PSNR values of reference sw codec.
     */
    public void testParallelDecoderQuality() throws Exception {
        if (CURRENT_VP8_SDK_VERSION <= MIN_VP8_SDK_VERSION) {
            return;
        }
        if (!isHwCodecExist()) {
            return; // no need to run this test if only SW codec exist
        }
        int encodeSeconds = 5;
        int[] scaleValues = { 4, 1, 1, 1, 1 };
        scaleValues = supportedScaleValues(WIDTH_HANGOUTS, HEIGHT_HANGOUTS, scaleValues, false);

        int numCodecs = scaleValues.length;
        int[] bitrateList = new int[numCodecs];

        // Allocate arrays for storing streams bitrates and PSNRs
        double[] psnrHwCodecAverage = new double[numCodecs];
        double[] psnrHwCodecMin = new double[numCodecs];
        int[] bitrateHwCodec = new int[numCodecs];
        double[] psnrSwCodecAverage = new double[numCodecs];
        double[] psnrSwCodecMin = new double[numCodecs];
        int[] bitrateSwCodec = new int[numCodecs];
        int[] bitrateTarget = new int[numCodecs];
        int[] encodingWidth = new int[numCodecs];
        int[] encodingHeight = new int[numCodecs];

        Log.d(TAG, "---------- testParallelDecoderQuality ------------");
        // Run sw and platform specific encoders for different bitrates
        // and compare PSNR of hw codec with PSNR of sw codec.
        for (int i = 0; i < numCodecs; i++) {
            if (scaleValues[i] == 4) {
                bitrateList[i] = BITRATE_HD;
            } else {
                bitrateList[i] = BITRATE_180P;
            }
        }
        ArrayList<CodecStreamParameters> params = getDefaultCodecStreamParameterList(
                INPUT_YUV,
                ENCODED_IVF_BASE,
                encodeSeconds,
                scaleValues,
                WIDTH_HANGOUTS,
                HEIGHT_HANGOUTS,
                FPS,
                BITRATE_MODE,
                bitrateList,
                true);

        // Change media stream parameters to encode 4 180p streams -
        // should use different output yuv file names
        for (int i = 0; i < numCodecs; i++) {
            params.get(i).scaledYuvFilename = SDCARD_DIR + File.separator + ENCODED_IVF_BASE +
                    "_" + params.get(i).frameWidth + "x" + params.get(i).frameHeight + ".yuv";
            params.get(i).encodedIvfFilename = SDCARD_DIR + File.separator + ENCODED_IVF_BASE +
                    "_" + params.get(i).frameWidth + "x" + params.get(i).frameHeight +
                    "_" + i + ".ivf";
            params.get(i).outputYuvFilename = SDCARD_DIR + File.separator + ENCODED_IVF_BASE +
                    "_" + params.get(i).frameWidth + "x" + params.get(i).frameHeight +
                    "_" + i + "_out.yuv";
        }

        // Store some encoding parameters for later use
        for (int i = 0; i < numCodecs; i++) {
            bitrateTarget[i] = params.get(i).bitrateSet[0];
            encodingWidth[i] = params.get(i).frameWidth;
            encodingHeight[i] = params.get(i).frameHeight;
        }

        // Run sw encode
        for (int i = 0; i < numCodecs; i++) {
            params.get(i).forceSwCodec = true;
        }
        ArrayList<ArrayList<BufferInfo>> bufInfos = encodeSimulcast( WIDTH, HEIGHT, params);

        ArrayList<Vp8EncodingStatistics>  encodingStatistics =
                computeSimulcastEncodingStatistics(bufInfos);
        for (int i = 0; i < numCodecs; i++) {
            bitrateSwCodec[i] = encodingStatistics.get(i).mAverageBitrate;
        }

        // Run SW decoder -  use its PSNR values as reference values.
        decodeParallel(params);
        for (int i = 0; i < numCodecs; i++) {
            Vp8DecodingStatistics decodingStatistics = computeDecodingStatistics(
                    params.get(i).scaledYuvFilename, R.raw.football_qvga,
                    params.get(i).outputYuvFilename,
                    encodingWidth[i], encodingHeight[i]);
            psnrSwCodecAverage[i] = decodingStatistics.mAveragePSNR;
            psnrSwCodecMin[i] = decodingStatistics.mMinimumPSNR;
        }

        // Run platform specific HW encoder next - one resolution at a time
        // since HW encoder may not support 5 simultaneous instances.
        for (int i = 0; i < numCodecs; i++) {
            params.get(i).forceSwCodec = false;
            ArrayList<CodecStreamParameters> paramsList = new ArrayList<CodecStreamParameters>();
            paramsList.add(params.get(i));
            bufInfos = encodeSimulcast(WIDTH, HEIGHT, paramsList);

            encodingStatistics = computeSimulcastEncodingStatistics(bufInfos);
            bitrateHwCodec[i] = encodingStatistics.get(0).mAverageBitrate;
        }

        // Run HW decoder
        decodeParallel(params);
        for (int i = 0; i < numCodecs; i++) {
            Vp8DecodingStatistics decodingStatistics = computeDecodingStatistics(
                    params.get(i).scaledYuvFilename, R.raw.football_qvga,
                    params.get(i).outputYuvFilename,
                    encodingWidth[i], encodingHeight[i]);
            psnrHwCodecAverage[i] = decodingStatistics.mAveragePSNR;
            psnrHwCodecMin[i] = decodingStatistics.mMinimumPSNR;
        }
        deleteTemporaryFiles(params);

        // Compare average and minimum PSNR of platform codec with reference sw codec -
        // average PSNR for platform codec should be no more than 1.5 dB less than reference PSNR
        // and minumum PSNR - no more than 3 dB less than reference minimum PSNR.
        // These PSNR difference numbers  are arbitrary for now, will need further estimation
        // when more devices with hw VP8 codec will appear.
        for (int i = 0; i < numCodecs; i++) {
            Log.d(TAG, "Resolution: " + encodingWidth[i] + " x " +  encodingHeight[i] +
                    ". Target bitrate:" + bitrateTarget[i]);
            Log.d(TAG, "  Sw: Bitrate: " + bitrateSwCodec[i] + ". PSNR Average: " +
                    psnrSwCodecAverage[i] + ". Minimum: " + psnrSwCodecMin[i]);
            Log.d(TAG, "  Hw: Bitrate: " + bitrateHwCodec[i] + ". PSNR Average: " +
                    psnrHwCodecAverage[i] + ". Minimum: " + psnrHwCodecMin[i]);
            if (psnrHwCodecAverage[i] < psnrSwCodecAverage[i] - MAX_AVERAGE_PSNR_DIFFERENCE) {
                throw new RuntimeException("Low average PSNR " + psnrHwCodecAverage[i] +
                        " comparing to reference PSNR " + psnrSwCodecAverage[i] +
                        " for decoder # " + i);
            }
            if (psnrHwCodecMin[i] < psnrSwCodecMin[i] - MAX_MINIMUM_PSNR_DIFFERENCE) {
                throw new RuntimeException("Low minimum PSNR " + psnrHwCodecMin[i] +
                        " comparing to reference PSNR " + psnrSwCodecMin[i] +
                        " for decoder # " + i);
            }
        }
        Log.d(TAG, "testParallelDecoderQuality PASSED");
    }

    /**
     * Check simulcast encoding and parallel decoding quality by calculating PSNR.
     *
     * Run simulcast encoding of one 720p, one 360p and one 180p stream and
     * parallel decoding of one 720p and four 180p streams.
     * Send 720p encoder output for 720p decoder input and 180p encoder output to
     * the inputs of all four 180p decoders.
     * Compare average and minimum PSNR of hw codec with PSNR values of reference sw codec.
     */
    public void testHangouts() throws Exception {
        if (CURRENT_VP8_SDK_VERSION <= MIN_VP8_SDK_VERSION) {
            return;
        }
        if (!isHwCodecExist()) {
            return; // no need to run this test if only SW codec exist
        }
        Log.d(TAG, "---------- testHangouts ------------");

        int encodeSeconds = 6;
        int[] encodeScaleValues = { 4, 2, 1 };
        encodeScaleValues = supportedScaleValues(WIDTH_HANGOUTS, HEIGHT_HANGOUTS,
                encodeScaleValues, true);
        int[] decodeScaleValues = { 4, 1, 1, 1, 1 };
        decodeScaleValues = supportedScaleValues(WIDTH_HANGOUTS, HEIGHT_HANGOUTS,
                decodeScaleValues, false);
        // Decoder input mapping - encoder index which should be used for
        // decoder input
        int[] streamMapping = new int[decodeScaleValues.length];
        for (int i = 0; i < decodeScaleValues.length; i++) {
            for (int j = 0; j < encodeScaleValues.length; j++) {
                if (decodeScaleValues[i] == encodeScaleValues[j]) {
                    streamMapping[i] = j;
                    break;
                }
            }
        }

        int numEncoders = encodeScaleValues.length;
        int numDecoders = decodeScaleValues.length;

        // Allocate arrays for storing streams bitrates and PSNRs
        double[] psnrHwCodecAverage = new double[numDecoders];
        double[] psnrHwCodecMin = new double[numDecoders];
        int[] bitrateHwCodec = new int[numEncoders];
        double[] psnrSwCodecAverage = new double[numDecoders];
        double[] psnrSwCodecMin = new double[numDecoders];
        int[] bitrateSwCodec = new int[numEncoders];
        int[] bitrateTarget = new int[numEncoders];
        int[] encodingWidth = new int[numEncoders];
        int[] encodingHeight = new int[numEncoders];

        int[] bitrateList = new int[Math.max(numEncoders, numDecoders)];
        for (int i = 0; i < encodeScaleValues.length; i++) {
            if (encodeScaleValues[i] == 4) {
                bitrateList[i] = BITRATE_HD;
            } else if (encodeScaleValues[i] == 2) {
                bitrateList[i] = BITRATE_360P;
            } else {
                bitrateList[i] = BITRATE_180P;
            }
        }

        ArrayList<CodecStreamParameters> encoderParams = getDefaultCodecStreamParameterList(
                INPUT_YUV,
                ENCODED_IVF_BASE,
                encodeSeconds,
                encodeScaleValues,
                WIDTH_HANGOUTS,
                HEIGHT_HANGOUTS,
                FPS,
                BITRATE_MODE,
                bitrateList,
                true);

        ArrayList<CodecStreamParameters> decoderParams = getDefaultCodecStreamParameterList(
                INPUT_YUV,
                ENCODED_IVF_BASE,
                encodeSeconds,
                decodeScaleValues,
                WIDTH_HANGOUTS,
                HEIGHT_HANGOUTS,
                FPS,
                BITRATE_MODE,
                bitrateList,
                true);

        // Change media stream parameters to decode 4 180p streams -
        // should use same input ivf and different output yuv file names
        for (int i = 0; i < numDecoders; i++) {
            decoderParams.get(i).outputYuvFilename = SDCARD_DIR + File.separator +
                    ENCODED_IVF_BASE + "_" + decoderParams.get(i).frameWidth + "x" +
                    decoderParams.get(i).frameHeight + "_" + i + "_out.yuv";
        }

        // Store some encoding parameters for later use
        for (int i = 0; i < numEncoders; i++) {
            bitrateTarget[i] = encoderParams.get(i).bitrateSet[0];
            encodingWidth[i] = encoderParams.get(i).frameWidth;
            encodingHeight[i] = encoderParams.get(i).frameHeight;
        }

        // Run SW codec
        for (int i = 0; i < numEncoders; i++) {
            encoderParams.get(i).forceSwCodec = true;
        }
        for (int i = 0; i < numDecoders; i++) {
            decoderParams.get(i).forceSwCodec = true;
        }

        ArrayList<ArrayList<BufferInfo>> bufInfos = encodeAndDecode(
                WIDTH, HEIGHT, streamMapping, encoderParams, decoderParams);

        ArrayList<Vp8EncodingStatistics>  encodingStatistics =
                computeSimulcastEncodingStatistics(bufInfos);
        for (int i = 0; i < numEncoders; i++) {
            bitrateSwCodec[i] = encodingStatistics.get(i).mAverageBitrate;
        }

        for (int i = 0; i < numDecoders; i++) {
            int j = streamMapping[i]; // encoder index for this decoder
            Vp8DecodingStatistics decodingStatistics = computeDecodingStatistics(
                    encoderParams.get(j).scaledYuvFilename, R.raw.football_qvga,
                    decoderParams.get(i).outputYuvFilename,
                    encodingWidth[j], encodingHeight[j]);
            psnrSwCodecAverage[i] = decodingStatistics.mAveragePSNR;
            psnrSwCodecMin[i] = decodingStatistics.mMinimumPSNR;
        }

        // Run HW codec
        for (int i = 0; i < numEncoders; i++) {
            encoderParams.get(i).forceSwCodec = false;
        }
        for (int i = 0; i < numDecoders; i++) {
            decoderParams.get(i).forceSwCodec = false;
        }

        bufInfos = encodeAndDecode(WIDTH, HEIGHT, streamMapping, encoderParams, decoderParams);
        encodingStatistics = computeSimulcastEncodingStatistics(bufInfos);
        for (int i = 0; i < numEncoders; i++) {
            bitrateHwCodec[i] = encodingStatistics.get(i).mAverageBitrate;
        }

        for (int i = 0; i < numDecoders; i++) {
            int j = streamMapping[i]; // encoder index for this decoder
            Vp8DecodingStatistics decodingStatistics = computeDecodingStatistics(
                    encoderParams.get(j).scaledYuvFilename, R.raw.football_qvga,
                    decoderParams.get(i).outputYuvFilename,
                    encodingWidth[j], encodingHeight[j]);
            psnrHwCodecAverage[i] = decodingStatistics.mAveragePSNR;
            psnrHwCodecMin[i] = decodingStatistics.mMinimumPSNR;
        }
        deleteTemporaryFiles(encoderParams);
        deleteTemporaryFiles(decoderParams);

        // Print statistics and verify PSNR and bitrates -
        // average PSNR for platform codec should be no more than 1.5 dB less than reference PSNR
        // and minumum PSNR - no more than 3 dB less than reference minimum PSNR.
        for (int i = 0; i < numEncoders; i++) {
            Log.d(TAG, "Resolution: " + encodingWidth[i] + " x " +  encodingHeight[i] +
                    ". Target bitrate:" + bitrateTarget[i]);
            Log.d(TAG, "  SW: Bitrate: " + bitrateSwCodec[i] +
                    ". HW: Bitrate: " + bitrateHwCodec[i]);
            // QCOM 720p encoder will generate too high bitrate - b/12910631.
            // TODO(glaznev): Remove this condition once QCOM will submit a fix
            if (encodeScaleValues[i] == 4) {
                CodecProperties properties = getVp8CodecProperties(true, false);
                if (properties.codecName.contains(OMX_QCOM_CODEC_PREFIX)) {
                    continue;
                }
            }
            assertEquals("HW codec bitrate " + bitrateHwCodec[i] +
                    " is different from the target " + bitrateTarget[i],
                    bitrateTarget[i], bitrateHwCodec[i], MAX_BITRATE_VARIATION * bitrateTarget[i]);
        }
        for (int i = 0; i < numDecoders; i++) {
            int j = streamMapping[i];
            Log.d(TAG, "Resolution: " + encodingWidth[j] + " x " +  encodingHeight[j]);
            Log.d(TAG, "  SW: PSNR Average: " + psnrSwCodecAverage[i] +
                    ". Minimum: " + psnrSwCodecMin[i]);
            Log.d(TAG, "  HW: PSNR Average: " + psnrHwCodecAverage[i] +
                    ". Minimum: " + psnrHwCodecMin[i]);
            if (psnrHwCodecAverage[i] < psnrSwCodecAverage[i] - MAX_AVERAGE_PSNR_DIFFERENCE) {
                throw new RuntimeException("Low average PSNR " + psnrHwCodecAverage[i] +
                        " comparing to reference PSNR " + psnrSwCodecAverage[i] +
                        " for decoder # " + i);
            }
            if (psnrHwCodecMin[i] < psnrSwCodecMin[i] - MAX_MINIMUM_PSNR_DIFFERENCE) {
                throw new RuntimeException("Low minimum PSNR " + psnrHwCodecMin[i] +
                        " comparing to reference PSNR " + psnrSwCodecMin[i] +
                        " for decoder # " + i);
            }
        }
        Log.d(TAG, "testHangouts PASSED");
    }
}

