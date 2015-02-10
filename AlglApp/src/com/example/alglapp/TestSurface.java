package com.example.alglapp;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class TestSurface {
    //private static final String ENCODED_IVF_BASE = "football";
    //private static final String INPUT_YUV = "football_qvga.yuv";
    //private static final int WIDTH = 320;
    //private static final int HEIGHT = 240;
    //private static final int BITRATE = 400000;

    private static final String ENCODED_IVF_BASE = "nicklas";
    private static final String INPUT_YUV = "nicklas_720p.yuv";
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int BITRATE = 1200000;

    private static final int FPS = 30;

    protected static final String SDCARD_DIR =
            Environment.getExternalStorageDirectory().getAbsolutePath();

    protected static final String TAG = "VP8CodecTestBase";
    private static final String VP8_MIME = "video/x-vnd.on2.vp8";
    private static final String VPX_SW_DECODER_NAME = "OMX.google.vp8.decoder";
    private static final String VPX_SW_ENCODER_NAME = "OMX.google.vp8.encoder";
    private static final String OMX_SW_CODEC_PREFIX = "OMX.google";
    private static final long DEFAULT_TIMEOUT_US = 200000; //-1;

    // Video bitrate type - should be set to OMX_Video_ControlRateConstant from OMX_Video.h
    protected static final int VIDEO_ControlRateVariable = 1;
    protected static final int VIDEO_ControlRateConstant = 2;

    // NV12 color format supported by QCOM codec, but not declared in MediaCodec -
    // see /hardware/qcom/media/mm-core/inc/OMX_QCOMExtns.h
    private static final int COLOR_QCOM_FORMATYUV420PackedSemiPlanar32m = 0x7FA30C04;
    // Allowable color formats supported by codec - in order of preference.
    private static final int[] mSupportedColorList = {
            CodecCapabilities.COLOR_FormatYUV420Planar,
            CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
            COLOR_QCOM_FORMATYUV420PackedSemiPlanar32m
    };

    private static final int TEST_R0 = 0;                   // RGB equivalent of {0,0,0} (BT.601)
    private static final int TEST_G0 = 136;
    private static final int TEST_B0 = 0;
    private static final int TEST_R1 = 236;                 // RGB equivalent of {120,160,200} (BT.601)
    private static final int TEST_G1 = 50;
    private static final int TEST_B1 = 186;

    private int mInputFrameIndex;
    private int mOutputFrameIndex;
    private long[] mFrameInputTimeMs = new long[30 * 50];
    private long[] mFrameOutputTimeMs = new long[30 * 50];
    private long[] mFrameSize = new long[30 * 50];


    protected class CodecProperties {
        CodecProperties(String codecName, int colorFormat) {
            this.codecName = codecName;
            this.colorFormat = colorFormat;
        }
        public boolean  isGoogleSwCodec() {
            return codecName.startsWith(OMX_SW_CODEC_PREFIX);
        }

        public final String codecName; // OpenMax component name for VP8 codec.
        public final int colorFormat;  // Color format supported by codec.
    }

    /**
     * Function to find VP8 codec.
     *
     * Iterates through the list of available codecs and tries to find
     * VP8 codec, which can support either YUV420 planar or NV12 color formats.
     * If forceSwGoogleCodec parameter set to true the function always returns
     * Google sw VP8 codec.
     * If forceSwGoogleCodec parameter set to false the functions looks for platform
     * specific VP8 codec first. If no platform specific codec exist, falls back to
     * Google sw VP8 codec.
     *
     * @param isEncoder     Flag if encoder is requested.
     * @param forceSwGoogleCodec  Forces to use Google sw codec.
     */
    protected CodecProperties getVp8CodecProperties(boolean isEncoder,
            boolean forceSwGoogleCodec) throws Exception {
        CodecProperties codecProperties = null;

        if (!forceSwGoogleCodec) {
            // Loop through the list of omx components in case platform specific codec
            // is requested.
            for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
                if (isEncoder != codecInfo.isEncoder()) {
                    continue;
                }
                Log.v(TAG, codecInfo.getName());
                // Check if this is sw Google codec - we should ignore it.
                boolean isGoogleSwCodec = codecInfo.getName().startsWith(OMX_SW_CODEC_PREFIX);
                if (isGoogleSwCodec) {
                    continue;
                }

                for (String type : codecInfo.getSupportedTypes()) {
                    if (!type.equalsIgnoreCase(VP8_MIME)) {
                        continue;
                    }
                    CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(VP8_MIME);

                    // Get candidate codec properties.
                    Log.v(TAG, "Found candidate codec " + codecInfo.getName());
                    for (int colorFormat : capabilities.colorFormats) {
                        Log.v(TAG, "   Color: 0x" + Integer.toHexString(colorFormat));
                    }

                    // Check supported color formats.
                    for (int supportedColorFormat : mSupportedColorList) {
                        for (int codecColorFormat : capabilities.colorFormats) {
                            if (codecColorFormat == supportedColorFormat) {
                                codecProperties = new CodecProperties(codecInfo.getName(),
                                        codecColorFormat);
                                Log.v(TAG, "Found target codec " + codecProperties.codecName +
                                        ". Color: 0x" + Integer.toHexString(codecColorFormat));
                                return codecProperties;
                            }
                        }
                    }
                    // HW codec we found does not support one of necessary color formats.
                    throw new RuntimeException("No hw codec with YUV420 or NV12 color formats");
                }
            }
        }
        // If no hw vp8 codec exist or sw codec is requested use default Google sw codec.
        if (codecProperties == null) {
            Log.v(TAG, "Use SW VP8 codec");
            if (isEncoder) {
                codecProperties = new CodecProperties(VPX_SW_ENCODER_NAME,
                        CodecCapabilities.COLOR_FormatYUV420Planar);
            } else {
                codecProperties = new CodecProperties(VPX_SW_DECODER_NAME,
                        CodecCapabilities.COLOR_FormatYUV420Planar);
            }
        }

        return codecProperties;
    }

    /**
     * Converts (interleaves) YUV420 planar to NV12 (if hw) or NV21 (if sw).
     * Assumes packed, macroblock-aligned frame with no cropping
     * (visible/coded row length == stride).  Swap U/V if |sw|.
     */
    private static byte[] YUV420ToNV(int width, int height, byte[] yuv, boolean sw) {
        byte[] nv = new byte[yuv.length];
        // Y plane we just copy.
        System.arraycopy(yuv, 0, nv, 0, width * height);

        // U & V plane we interleave.
        int u_offset = width * height;
        int v_offset = u_offset + u_offset / 4;
        int nv_offset = width * height;
        if (sw) {
            for (int i = 0; i < width * height / 4; i++) {
                nv[nv_offset++] = yuv[v_offset++];
                nv[nv_offset++] = yuv[u_offset++];
            }
        }
        else {
            for (int i = 0; i < width * height / 4; i++) {
                nv[nv_offset++] = yuv[u_offset++];
                nv[nv_offset++] = yuv[v_offset++];
            }
        }
        return nv;
    }

    /**
     * Converts (de-interleaves) NV12 to YUV420 planar.
     * Stride may be greater than width, slice height may be greater than height.
     */
    private static byte[] NV12ToYUV420(int width, int height,
            int stride, int sliceHeight, byte[] nv12) {
        byte[] yuv = new byte[width * height * 3 / 2];

        // Y plane we just copy.
        for (int i = 0; i < height; i++) {
            System.arraycopy(nv12, i * stride, yuv, i * width, width);
        }

        // U & V plane - de-interleave.
        int u_offset = width * height;
        int v_offset = u_offset + u_offset / 4;
        int nv_offset;
        for (int i = 0; i < height / 2; i++) {
            nv_offset = stride * (sliceHeight + i);
            for (int j = 0; j < width / 2; j++) {
                yuv[u_offset++] = nv12[nv_offset++];
                yuv[v_offset++] = nv12[nv_offset++];
            }
        }
        return yuv;
    }

    /**
     * Generates a frame of data using GL commands.
     */
    private void generateSurfaceFrame(int frameIndex, int width, int height) {
        frameIndex %= 8;

        int startX, startY;
        if (frameIndex < 4) {
            // (0,0) is bottom-left in GL
            startX = frameIndex * (width / 4);
            startY = height / 2;
        } else {
            startX = (7 - frameIndex) * (width / 4);
            startY = 0;
        }

        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        GLES20.glClearColor(TEST_R0 / 255.0f, TEST_G0 / 255.0f, TEST_B0 / 255.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(startX, startY, width / 4, height / 2);
        GLES20.glClearColor(TEST_R1 / 255.0f, TEST_G1 / 255.0f, TEST_B1 / 255.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }


    private void encode(
            String inputYuvFilename,
            String outputIvfFilename,
            boolean forceSwEncoder,
            int frameCount,
            int encFrameWidth, int encFrameHeight,
            int bitrate, int frameRate,
            boolean useSurface) throws Exception {
        CodecProperties properties = getVp8CodecProperties(true, forceSwEncoder);
        InputSurface inputSurface = null;
        Triangle triangle = null;
        int colorFormat = properties.colorFormat;
        if (useSurface) {
            colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
        }

        // Open input/output
        FileInputStream yuvStream = new FileInputStream(inputYuvFilename);
        IvfWriter ivf = null;
        if (outputIvfFilename != null) {
            ivf = new IvfWriter(outputIvfFilename, encFrameWidth, encFrameHeight);
        }

        // Create a media format signifying desired output.
        MediaFormat format = MediaFormat.createVideoFormat(VP8_MIME, encFrameWidth, encFrameHeight);
        format.setInteger("bitrate-mode", VIDEO_ControlRateConstant); // set CBR
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        int syncFrameInterval = 10;
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, syncFrameInterval);

        Log.d(TAG, "Creating encoder " + properties.codecName + ". Color format: 0x" +
             Integer.toHexString(colorFormat)+ " : " +
             encFrameWidth + " x " + encFrameHeight +
             ". Fps:" + frameRate + ". Bitrate: " + bitrate +
             ". Key frame:" + syncFrameInterval * frameRate);
        Log.d(TAG, "Source resolution: " + encFrameWidth + " x " + encFrameHeight);
        Log.d(TAG, "Format: " + format);
        Log.d(TAG, "  Output ivf:" + outputIvfFilename);
        MediaCodec encoder =  MediaCodec.createByCodecName(properties.codecName);
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        if (useSurface) {
            triangle = new Triangle();
            Surface surface = encoder.createInputSurface();
            inputSurface = new InputSurface(surface);
            Log.d(TAG, "Surface: " + inputSurface.getWidth() + " x " + inputSurface.getHeight());
        }
        encoder.start();

        ByteBuffer[] inputBuffers = encoder.getInputBuffers();
        ByteBuffer[] outputBuffers = encoder.getOutputBuffers();
        Log.d(TAG, "Input buffers: " + inputBuffers.length +
                ". Output buffers: " + outputBuffers.length);

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        // encode loop
        mInputFrameIndex = 0;
        mOutputFrameIndex = 0;
        long inPresentationTimeUs = 0;
        long outPresentationTimeUs = 0;
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int srcFrameSize = encFrameWidth * encFrameHeight * 3 / 2;
        byte[] srcFrame = new byte[srcFrameSize];
        double frameDuration = 1000000.0 / frameRate;
        double presentationTimeUsCurrent = 0;

        while (!sawOutputEOS) {
            if (!sawInputEOS) {

                if (useSurface) {
                    if (mInputFrameIndex  >= frameCount) {
                        sawInputEOS = true;
                        encoder.signalEndOfInputStream();
                        Log.d(TAG, "----Sending EOS empty frame for frame # " + mInputFrameIndex);
                    }
                    else {
                        inputSurface.makeCurrent();
                        //generateSurfaceFrame(inputFrameIndex, encFrameWidth, encFrameWidth);
                        if (mInputFrameIndex == 0) {
                            triangle.Init();
                        }
                        triangle.draw();
                        inPresentationTimeUs = (int)(presentationTimeUsCurrent + 0.5);
                        inputSurface.setPresentationTime(inPresentationTimeUs);
                        inputSurface.swapBuffers();
                        mInputFrameIndex++;
                        presentationTimeUsCurrent += frameDuration;
                    }
                }
                else {
                    // Feed input
                    int inputBufIndex = encoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                    if (inputBufIndex >= 0) {
                        int bytesRead = yuvStream.read(srcFrame);
                        // Check EOS
                        if (frameCount > 0 && mInputFrameIndex  >= frameCount) {
                            sawInputEOS = true;
                            Log.d(TAG, "----Sending EOS empty frame for frame # " + mInputFrameIndex);
                        }

                        if (!sawInputEOS && bytesRead == -1) {
                            if (frameCount == 0) {
                                sawInputEOS = true;
                                Log.d(TAG, "----Sending EOS empty frame for frame # " +
                                        mInputFrameIndex);
                            } else {
                                yuvStream.close();
                                yuvStream = new FileInputStream(inputYuvFilename);
                                bytesRead = yuvStream.read(srcFrame);
                            }
                        }

                        // Convert YUV420 to NV12 if necessary
                        if (colorFormat != CodecCapabilities.COLOR_FormatYUV420Planar) {
                            srcFrame = YUV420ToNV(encFrameWidth, encFrameHeight, srcFrame,
                                        properties.isGoogleSwCodec());
                        }

                        inPresentationTimeUs = (int)(presentationTimeUsCurrent + 0.5);
                        Log.v(TAG, "Encoder input frame # " + mInputFrameIndex + ". TS: " +
                                (inPresentationTimeUs / 1000) + " ms.");
                        mFrameInputTimeMs[mInputFrameIndex] = SystemClock.elapsedRealtime();
                        inputBuffers[inputBufIndex].clear();
                        inputBuffers[inputBufIndex].put(srcFrame);
                        inputBuffers[inputBufIndex].rewind();
                        int encFrameLength = sawInputEOS ? 0 : srcFrame.length;

                        encoder.queueInputBuffer(
                                inputBufIndex,
                                0,  // offset
                                encFrameLength,  // size
                                inPresentationTimeUs,
                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                        mInputFrameIndex++;
                        presentationTimeUsCurrent += frameDuration;
                    }
                }
             }

             // Get output
             int result = encoder.dequeueOutputBuffer(bufferInfo, DEFAULT_TIMEOUT_US);
             while (result == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED ||
                     result == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                 if (result == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                     outputBuffers = encoder.getOutputBuffers();
                     Log.d(TAG, "New output buffers: " + outputBuffers.length);
                 } else if (result == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                     format = encoder.getOutputFormat();
                     Log.d(TAG, "Format changed: " + format.toString());
                 }
                 result = encoder.dequeueOutputBuffer(bufferInfo, DEFAULT_TIMEOUT_US);
             }
             if (result >= 0) {
                 int outputBufIndex = result;
                 byte[] buffer = new byte[bufferInfo.size];
                 outputBuffers[outputBufIndex].position(bufferInfo.offset);
                 outputBuffers[outputBufIndex].get(buffer, 0, bufferInfo.size);
                 outPresentationTimeUs = bufferInfo.presentationTimeUs;

                 if ((mOutputFrameIndex == 0)
                     && ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) == 0)) {
                         throw new RuntimeException("First frame is not a sync frame.");
                 }

                 String logStr = "Encoder output frame # " + mOutputFrameIndex;
                 if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                     logStr += " CONFIG. ";
                 }
                 if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
                     logStr += " KEY. ";
                 }
                 if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                     sawOutputEOS = true;
                     logStr += " EOS. ";
                 }
                 logStr += " Size: " + bufferInfo.size;
                 logStr += ". OutTime: " + (outPresentationTimeUs + 500)/1000;
                 Log.v(TAG, logStr);

                 encoder.releaseOutputBuffer(outputBufIndex, false);

                 if (bufferInfo.size > 0) {
                     mFrameOutputTimeMs[mOutputFrameIndex] = SystemClock.elapsedRealtime();
                     mFrameSize[mOutputFrameIndex] = bufferInfo.size;
                     if (ivf != null) {
                         ivf.writeFrame(buffer, bufferInfo.presentationTimeUs);
                     }
                     mOutputFrameIndex++;
                 }
            }
        }

        if (inputSurface != null) {
            inputSurface.release();
        }
        encoder.stop();
        encoder.release();
        if (ivf != null) {
            ivf.close();
        }
        yuvStream.close();
    }


    protected void decode(
            String inputIvfFilename,
            String outputYuvFilename,
            boolean forceSwDecoder,
            boolean useSurface,
            Surface surface) throws Exception {
        CodecProperties properties = getVp8CodecProperties(false, forceSwDecoder);
        Surface decoderSurface = null;
        if (useSurface) {
            decoderSurface = surface;
            outputYuvFilename = null;
        }
        // Open input/output.
        IvfReader ivf = new IvfReader(inputIvfFilename);
        int frameWidth = ivf.getWidth();
        int frameHeight = ivf.getHeight();
        int frameCount = ivf.getFrameCount();
        int frameStride = frameWidth;
        int frameSliceHeight = frameHeight;
        int frameColorFormat = properties.colorFormat;

        FileOutputStream yuv = null;
        if (outputYuvFilename != null) {
            yuv = new FileOutputStream(outputYuvFilename, false);
        }

        // Create decoder.
        MediaFormat format = MediaFormat.createVideoFormat(VP8_MIME,
                                                           ivf.getWidth(),
                                                           ivf.getHeight());
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, properties.colorFormat);
        Log.d(TAG, "Creating decoder " + properties.codecName +
                ". Color format: 0x" + Integer.toHexString(frameColorFormat) +
                ". " + frameWidth + " x " + frameHeight);
        Log.d(TAG, "  Format: " + format);
        Log.d(TAG, "  In: " + inputIvfFilename + ". Out:" + outputYuvFilename);
        MediaCodec decoder = MediaCodec.createByCodecName(properties.codecName);
        decoder.configure(format, decoderSurface, null, 0);
        decoder.start();

        ByteBuffer[] inputBuffers = decoder.getInputBuffers();
        ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
        Log.d(TAG, "Input buffers: " + inputBuffers.length +
                ". Output buffers: " + outputBuffers.length);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        // decode loop
        mInputFrameIndex = 0;
        mOutputFrameIndex = 0;
        long inPresentationTimeUs = 0;
        long outPresentationTimeUs = 0;
        boolean sawOutputEOS = false;
        boolean sawInputEOS = false;

        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                int inputBufIndex = decoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                if (inputBufIndex >= 0) {
                    byte[] frame = ivf.readFrame(mInputFrameIndex);
                    inPresentationTimeUs = (long)(ivf.getFrameTimestamp(mInputFrameIndex) * 1e6);

                    if (mInputFrameIndex == frameCount - 1) {
                        Log.d(TAG, "  Input EOS for frame # " + mInputFrameIndex);
                        sawInputEOS = true;
                    }
                    Log.v(TAG, "Decoder input frame # " + mInputFrameIndex + ". TS: " +
                            (inPresentationTimeUs / 1000) + " ms. Size: " + frame.length);
                    mFrameInputTimeMs[mInputFrameIndex] = SystemClock.elapsedRealtime();
                    inputBuffers[inputBufIndex].clear();
                    inputBuffers[inputBufIndex].put(frame);
                    inputBuffers[inputBufIndex].rewind();

                    decoder.queueInputBuffer(
                            inputBufIndex,
                            0,  // offset
                            frame.length,
                            inPresentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                    mInputFrameIndex++;
                }
            }

            int result = decoder.dequeueOutputBuffer(bufferInfo, DEFAULT_TIMEOUT_US);
            while (result == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED ||
                    result == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (result == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    outputBuffers = decoder.getOutputBuffers();
                } else  if (result == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Process format change
                    format = decoder.getOutputFormat();
                    frameWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                    frameHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
                    frameColorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT);
                    Log.d(TAG, "Decoder output format change. Color: 0x" +
                            Integer.toHexString(frameColorFormat));
                    Log.d(TAG, "Format: " + format.toString());

                    // Parse frame and slice height from undocumented values
                    if (format.containsKey("stride")) {
                        frameStride = format.getInteger("stride");
                    } else {
                        frameStride = frameWidth;
                    }
                    if (format.containsKey("slice-height")) {
                        frameSliceHeight = format.getInteger("slice-height");
                    } else {
                        frameSliceHeight = frameHeight;
                    }
                    Log.d(TAG, "Frame stride and slice height: " + frameStride +
                            " x " + frameSliceHeight);
                }
                result = decoder.dequeueOutputBuffer(bufferInfo, DEFAULT_TIMEOUT_US);
            }
            if (result >= 0) {
                int outputBufIndex = result;
                outPresentationTimeUs = bufferInfo.presentationTimeUs;
                Log.v(TAG, "Decoder output frame # " + mOutputFrameIndex +
                        ". TS: " + (outPresentationTimeUs / 1000) + " ms. Size: " + bufferInfo.size);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;
                    Log.d(TAG, "   Output EOS for frame # " + mOutputFrameIndex);
                }

                if (bufferInfo.size > 0 && yuv != null) {
                    // Save decoder output to yuv file.
                    byte[] frame = new byte[bufferInfo.size];
                    outputBuffers[outputBufIndex].position(bufferInfo.offset);
                    outputBuffers[outputBufIndex].get(frame, 0, bufferInfo.size);
                    // Convert NV12 to YUV420 if necessary
                    if (frameColorFormat != CodecCapabilities.COLOR_FormatYUV420Planar) {
                        frame = NV12ToYUV420(frameWidth, frameHeight,
                                frameStride, frameSliceHeight, frame);
                    }
                    yuv.write(frame);
                }
                decoder.releaseOutputBuffer(outputBufIndex, useSurface);
                mFrameOutputTimeMs[mOutputFrameIndex] = SystemClock.elapsedRealtime();
                if (bufferInfo.size > 0) {
                    mOutputFrameIndex++;
                }
            }
            if (result == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.v(TAG, "INFO_TRY_AGAIN_LATER");
            }
        }
        decoder.stop();
        decoder.release();
        ivf.close();
        if (yuv != null) {
            yuv.close();
        }
    }

    // Get average encoding time
    void getAverageCodecTime() {
        long encodingTime = 0;
        long totalSize = 0;
        for (int i = 0; i < mOutputFrameIndex; i++) {
            encodingTime += (mFrameOutputTimeMs[i] - mFrameInputTimeMs[i]);
            totalSize += mFrameSize[i];
        }
        encodingTime /= mOutputFrameIndex;
        Log.d(TAG, "Frames: " + mOutputFrameIndex + ". Codec time: " + encodingTime + " ms." +
                " Size: " + totalSize);
    }


    public void testBasic(Surface surface) throws Exception {
        int frameCount = 10 * FPS;
        boolean useSurface = true;
        //boolean useSurface = false;
        boolean forceSw = false;

        String inputYuvFilename = SDCARD_DIR + File.separator + INPUT_YUV;
        String encodedIvfFilename = SDCARD_DIR + File.separator + ENCODED_IVF_BASE +
                "_" + WIDTH + "x" + HEIGHT + ".ivf";
        String outputYuvFilename = SDCARD_DIR + File.separator + ENCODED_IVF_BASE +
                "_" + WIDTH + "x" + HEIGHT + "_out.yuv";

        Log.d(TAG, "---------- testSurfaceBasic------------");
        outputYuvFilename = null;
        //encodedIvfFilename = null;
        //encode(inputYuvFilename, encodedIvfFilename, forceSw, frameCount,
        //        WIDTH, HEIGHT, BITRATE, FPS, useSurface);

        decode(encodedIvfFilename, outputYuvFilename, forceSw, useSurface, surface);
        getAverageCodecTime();
    }
}
