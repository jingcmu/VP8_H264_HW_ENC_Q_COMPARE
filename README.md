# VP8_H264_HW_ENC_Q_COMPARE
Compare VP8 and H.264 HW encoders quality on Android device.

App will encode and decoder yuv file with given bitrate and resolution and will calculate PSNR.
Switching between HW VP8/H.264 is done by re-defining below constants in Vp8CodecTestBase.java:

H.264:
'''
    private static final String VP8_MIME = "video/avc";
    private static final String VPX_SW_DECODER_NAME = "OMX.google.h264.decoder";
    private static final String VPX_SW_ENCODER_NAME = "OMX.google.h264.encoder";
'''

VP8:
'''
    private static final String VP8_MIME = "video/x-vnd.on2.vp8";
    private static final String VPX_SW_DECODER_NAME = "OMX.google.vp8.decoder";
    private static final String VPX_SW_ENCODER_NAME = "OMX.google.vp8.encoder";
'''

This is part of my work for WebRTC project, in which I need to add dynamic frame scaling for VP8/9 HW encoder.
