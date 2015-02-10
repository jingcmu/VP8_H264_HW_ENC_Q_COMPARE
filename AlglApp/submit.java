Extend CTS VP8 encoder and decoder tests.

- Change basic test to encode video streams with a set of bitrates and
  verify that actual bitrate values are close to the target.

- Add encoding and decoding quality statistics to the tests.

- Add video quality test, which will encode and decode video stream at
  various bitrates, check PSNR, verifies that video streams with higher
  bitrates have higher PSNR and compare PSNR values of platform
  specific and pure sw video codecs.

- Add function to select platform specific VP8 codec.

- Add YUV420p->NV12 and NV12->YUV420p color space conversions.

- Add support for a few variants of NV12 color space to encoder input
  and decoder output.

- Tested on N4 and N5 devices. Code currently supports
  a few Qualcomm specific color space formats. This part may need
  to be extended later for other devices if any non standard
  color formats will be used.

- Some test results for N5 device:

a) PSNR values calculated for 9 seconds of football_qvga
   QVGA 30 fps video:
  300 kbps: Sw codec PSNR: 32.5 dB. Hw codec PSNR: 33.1 dB.
  500 kbps: Sw codec PSNR: 34.9 dB. Hw codec PSNR: 35.2 dB.
  700 kbps: Sw codec PSNR: 36.7 dB. Hw codec PSNR: 36.6 dB.
  900 kbps: Sw codec PSNR: 38.0 dB. Hw codec PSNR: 37.8 dB.
b) Force key frame requests to hw codec are working well giving key frame interval
   value exactly as was requested unlike pure sw codec which may
   generate key frames 2 frames after the request was actually made.
c) Dynamic bitrate change is also working well - the transition to new
   bitrate is fast and generated bitrate after the change request
   follows the target value close enough unlike sw codec.
