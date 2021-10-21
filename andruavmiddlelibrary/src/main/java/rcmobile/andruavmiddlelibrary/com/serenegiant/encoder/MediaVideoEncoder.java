package rcmobile.andruavmiddlelibrary.com.serenegiant.encoder;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2015 saki t_saki@serenegiant.com
 *
 * File name: MediaVideoEncoder.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
*/

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class MediaVideoEncoder extends MediaEncoder {
	private static final boolean DEBUG = true;	// TODO set false on release
	private static final String TAG = "MediaVideoEncoder";

	private static final String MIME_TYPE = "video/avc";
	// parameters for recording
	// VIDEO_WITH and VIDEO_HEIGHT should be same as the camera preview size.
	private int VIDEO_WIDTH = 640;
	private int VIDEO_HEIGHT = 480;
	private static final int FRAME_RATE = 15;
	private static final float BPP = 0.50f;


	public final static int MOBILE_GENERIC = 0;
	public final static int MOBILE_MATE10_STYLE = 1;
	public final static int MOBILE_S5_STYLE = 2;
	public final static int MOBILE_MATE8_STYLE = 3;
	public final static int MOBILE_OPPO_F11_STYLE = 4;
	public final static int MOBILE_WORK_FOR_ALL = 999;
	public static int VIDEO_FORMAT = MOBILE_WORK_FOR_ALL;
	public final static int MOBILE_MATE10LITE_STYLE = 5;
	protected int mColorFormat;

	public static void SetVideoEncoder()
	{

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			if (Build.MODEL.toUpperCase().equals("BLA-L29")) {
				// Huuwei 10  Pro
				VIDEO_FORMAT = MOBILE_MATE10_STYLE;
			} else if (Build.MODEL.toUpperCase().equals("SM-G900F")) {
				VIDEO_FORMAT = MOBILE_S5_STYLE;
			} else if (Build.MODEL.toUpperCase().startsWith("SM-G900")) //SM-G900H another S5
			{
				VIDEO_FORMAT = MOBILE_S5_STYLE;
			} else if (Build.MODEL.toUpperCase().equals("HUAWEI NXT-L29")) {
				VIDEO_FORMAT = MOBILE_MATE8_STYLE;
			} else if (Build.MODEL.toUpperCase().equals("CPH1911")) {
				VIDEO_FORMAT = MOBILE_OPPO_F11_STYLE;
			} else if (Build.MODEL.toUpperCase().equals("RNE-L21")) {
				// Huuwei 10  light
				VIDEO_FORMAT = MOBILE_MATE8_STYLE;
			}
			else
			{
				VIDEO_FORMAT = MOBILE_WORK_FOR_ALL;
			}
		}
		else {

			VIDEO_FORMAT = MOBILE_WORK_FOR_ALL;
		}
	}

	public MediaVideoEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
		super(muxer, listener);
		if (DEBUG) Log.i(TAG, "MediaVideoEncoder: ");
	}

	public void encode(final ByteBuffer buffer) {

		try {
//    	if (DEBUG) Log.v(TAG, "encode:buffer=" + buffer);
			synchronized (mSync) {
				if (!mIsCapturing || mRequestStop) return;
			}
			encode(buffer, getPTSUs());
		}
		catch (final BufferOverflowException ex)
		{
            /*
            java.nio.BufferOverflowException
	at java.nio.ByteBuffer.put(ByteBuffer.java:544)
	at hamada.andruavmiddlelibrary.com.serenegiant.encoder.MediaEncoder.encode(SourceFile:316)
	at hamada.andruavmiddlelibrary.com.serenegiant.encoder.MediaVideoEncoder.encode(SourceFile:66)
	at hamada.nee3.activities.camera.CameraRecorder.encodeFeed(SourceFile:233)
	at hamada.nee3.widgets.camera.AndruavRTCVideoEncoderWidget$6.run(SourceFile:947)
	at android.os.Handler.handleCallback(Handler.java:761)
	at android.os.Handler.dispatchMessage(Handler.java:98)
	at android.os.Looper.loop(Looper.java:156)
	at android.app.ActivityThread.main(ActivityThread.java:6524)
	at java.lang.reflect.Method.invoke(Native Method)
	at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:941)
	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:831)
             */
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
	protected void prepare(final int width,final int height, final int frameRate) throws IOException {
		if (DEBUG) Log.i(TAG, "prepare: ");
		mTrackIndex = -1;
		mMuxerStarted = mIsEOS = false;

        /*final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);
        if (videoCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }*/
		//if (DEBUG) Log.i(TAG, "selected codec: " + videoCodecInfo.getName());
		VIDEO_WIDTH  = width;
		VIDEO_HEIGHT = height;
		final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
		final MediaCodecInfo videoCodecInfo;
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            videoCodecInfo = null;// selectVideoCodec2(mMediaCodec.getCodecInfo(),format);
        }
        else
        {
            videoCodecInfo = selectVideoCodec(MIME_TYPE);
        }
        if (videoCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }*/
		format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
		format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
		mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
		mColorFormat = selectColorFormat(mMediaCodec.getCodecInfo(),MIME_TYPE);
		if (mColorFormat ==0)
		{
			if (DEBUG) Log.i(TAG, "format: no available color forat for this MIME_TYPE");
			return;
		}
		format.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
		//mMediaCodec.createInputSurface();
		//mMediaCodec = MediaCodec.createByCodecName("OMX.google.vp8.encoder");
		mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

		mMediaCodec.start();
		if (DEBUG) Log.i(TAG, "prepare finishing");
		if (mListener != null) {
			try {
				mListener.onPrepared(this);
			} catch (final Exception e) {
				Log.e(TAG, "prepare:", e);
			}
		}
	}

	private int calcBitRate() {
		final int bitrate = (int)(BPP * FRAME_RATE * VIDEO_WIDTH * VIDEO_HEIGHT);
		Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
		return bitrate;
	}

    /*
    protected final MediaCodecInfo selectVideoCodec(final String mimeType) {
        if (DEBUG) Log.v(TAG, "selectVideoCodec:");

        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {	// skipp decoder
                continue;
            }
            // select first codec that match a specific MIME type and color format
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (DEBUG) Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME=" + types[j]);
                    final int format = selectColorFormat(codecInfo, mimeType);
                    if (format > 0) {
                        mColorFormat = format;
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }*/

    /*@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected final MediaCodecInfo selectVideoCodec2(final MediaCodecInfo[] codecInfo) {
        if (DEBUG) Log.v(TAG, "selectVideoCodec:");

        // get the list of available codecs
            for (int i=0;i<codecInfo.length;++i)
            {
                if (codecInfo[i].isEncoder())
                {
                    final String[] types = codecInfo[i].getSupportedTypes();
                    for (int j = 0; j < types.length; j++) {
                        final int cformat =selectColorFormat(codecInfo[i], types[j]);
                        if (cformat > 0) {
                            mColorFormat = cformat;
                            return codecInfo[i];
                        }
                    }
                }
            }
            // select first codec that match a specific MIME type and color format
            *//*final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (DEBUG) Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME=" + types[j]);
                    //DO: MHEFNY: I HARDCODED THE VIDEO TYPE HERE.
                    //final int format = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible; //selectColorFormat(codecInfo, mimeType);
                    final int format = 19; //selectColorFormat(codecInfo, mimeType);
                    if (format > 0) {
                        mColorFormat = format;
                        return codecInfo;
                    }
                }
            }
        *//*
            return null;
    }
*/
	/**
	 * select color format available on specific codec and we can use.
	 * @return 0 if no colorFormat is matched
	 */
	protected static int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
		try {
			if (DEBUG) Log.i(TAG, "selectColorFormat: ");
			int result = 0;
			final MediaCodecInfo.CodecCapabilities caps;
			try {
				Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
				caps = codecInfo.getCapabilitiesForType(mimeType);
			} finally {
				Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
			}

			if (recognizedFormats.length==0) return 0;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				caps.getVideoCapabilities().getSupportedHeights();
			}

			switch (VIDEO_FORMAT)
			{
				case MOBILE_MATE8_STYLE:
					result = caps.colorFormats[0];
					break;
				case MOBILE_MATE10_STYLE:
					result = caps.colorFormats[0];
					break;
				case MOBILE_OPPO_F11_STYLE:
					result = 19;
					break;
				case MediaVideoEncoder.MOBILE_S5_STYLE:
					result = 21;
					break;
				default:
					result = caps.colorFormats[0];
			}
			//result = caps.colorFormats[0]; // works with mate-8 & mate-10 if NV21 frame is used.
			//result = 21; // works with s5 if NV21 frame is used.
			return result;
			// search for recognized format in order based on priorities.
			//return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
//			final int n = recognizedFormats != null ? recognizedFormats.length : 0;
//			for (int i = 0; i < n; i++) {
//				for (int j = 0; j < caps.colorFormats.length; j++) {
//					if (recognizedFormats[i] == caps.colorFormats[j]) {
//						result = recognizedFormats[i];
//						return result;
//					}
//				}
//			}
//
//
//
//
//			if (result == 0)
//				Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
//			return result;
		}
		catch (Exception e)
		{
			return 0;
		}
	}


	static final int COLOR_QCOM_FORMATYVU420PackedSemiPlanar32m4ka = 0x7FA30C01;
	static final int COLOR_QCOM_FORMATYVU420PackedSemiPlanar16m4ka = 0x7FA30C02;
	static final int COLOR_QCOM_FORMATYVU420PackedSemiPlanar64x32Tile2m8ka = 0x7FA30C03;
	static final int COLOR_QCOM_FORMATYUV420PackedSemiPlanar32m = 0x7FA30C04;


	/**
	 * color formats that we can use in this class
	 * Check: https://cs.chromium.org/chromium/src/third_party/webrtc/sdk/android/src/java/org/webrtc/MediaCodecUtils.java?type=cs&g=0
	 */
	protected static int[] recognizedFormats;
	static {
		recognizedFormats = new int[] {
				COLOR_QCOM_FORMATYUV420PackedSemiPlanar32m,
				MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,      // worked on OPPO F11 correctly with correct color codes.
				MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,  // good for white S5 & Mate10Pro but both with bad color format
				MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
		};

//        	MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
	}

    /*private static boolean isRecognizedViewoFormat(final int colorFormat) {
        if (DEBUG) Log.i(TAG, "isRecognizedViewoFormat:colorFormat=" + colorFormat);
        final int n = recognizedFormats != null ? recognizedFormats.length : 0;
        for (int i = 0; i < n; i++) {
            if (recognizedFormats[i] == colorFormat) {
                return true;
            }
        }
        return false;
    }*/

}
