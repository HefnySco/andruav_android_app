package ap.andruavmiddlelibrary.com.serenegiant.encoder;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaMuxerWrapper.java
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
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.andruav.AndruavEngine;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaMuxerWrapper {
	private static final boolean DEBUG = true;	// TODO set false on release
	private static final String TAG = "MediaMuxerWrapper";


	private MediaMuxer mMediaMuxer;	// API >= 18
	private int mEncoderCount, mStatredCount;
	private boolean mIsStarted;
	private MediaEncoder mVideoEncoder, mAudioEncoder;



	/**
	 * Constructor
	 * @throws IOException
	 */
	public MediaMuxerWrapper(final String outputPath) throws IOException {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			mMediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
		}
		mEncoderCount = mStatredCount = 0;
		mIsStarted = false;
	}

	public void prepare(final int width,final int height, final int frameRate) throws IOException {
		if (mVideoEncoder != null)
			mVideoEncoder.prepare(width,height, frameRate);
		if (mAudioEncoder != null)
			mAudioEncoder.prepare(0,0, 0);
	}

	public void startRecording() {
		if (mVideoEncoder != null)
			mVideoEncoder.startRecording();
		if (mAudioEncoder != null)
			mAudioEncoder.startRecording();
	}

	public void stopRecording() {
		if (mVideoEncoder != null)
		{
			mVideoEncoder.stopRecording();
			//mVideoEncoder.release();
			mVideoEncoder = null;
		}

		if (mAudioEncoder != null)
		{
			mAudioEncoder.stopRecording();
			//mAudioEncoder.release();
			mAudioEncoder = null;
		}

		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
				mMediaMuxer.release();
			}
		}
		catch ( final Exception e)
		{
			e.printStackTrace();
		}

	}

	public synchronized boolean isStarted() {
		return mIsStarted;
	}

//**********************************************************************
//**********************************************************************
	/**
	 * assign encoder to this calss. this is called from encoder.
	 * @param encoder instance of MediaVideoEncoder or MediaAudioEncoder
	 */
	/*package*/ void addEncoder(final MediaEncoder encoder) {
		if (encoder instanceof MediaVideoEncoder) {
			if (mVideoEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");
			mVideoEncoder = encoder;
		} else if (encoder instanceof MediaAudioEncoder) {
			if (mAudioEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");
			mAudioEncoder = encoder;
		} else
			throw new IllegalArgumentException("unsupported encoder");
		mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
	}

	/**
	 * request startEncryptFile recording from encoder
	 * @return true when muxer is ready to write
	 */
	/*package*/ /*package*/ @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	synchronized boolean start() {
		if (DEBUG) Log.v(TAG,  "startEncryptFile:");
		mStatredCount++;
		if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
			mMediaMuxer.start();
			mIsStarted = true;
			notifyAll();
			if (DEBUG) Log.v(TAG,  "MediaMuxer started:");
		}
		return mIsStarted;
	}

	/**
	 * request stop recording from encoder when encoder received EOS
	 */
	/*package*/ /*package*/ @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	synchronized void stop() {
		try {
			if (DEBUG) Log.v(TAG, "stop:mStatredCount=" + mStatredCount);
			mStatredCount--;
			if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
				mMediaMuxer.stop();
				mIsStarted = false;
				if (DEBUG) Log.v(TAG, "MediaMuxer stopped:");
			}
		}
		catch (final Exception ex)
		{
			AndruavEngine.log().logException("media", ex);
		}
	}

	/**
	 * assign encoder to muxer
	 * @param format
	 * @return minus value indicate error
	 */
	/*package*/ @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	synchronized int addTrack(final MediaFormat format) {
		if (mIsStarted)
			throw new IllegalStateException("muxer already started");
		final int trackIx;
		trackIx = mMediaMuxer.addTrack(format);
		if (DEBUG) Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx + ",format=" + format);
		return trackIx;

	}

	/**
	 * write encoded data to muxer
	 * @param trackIndex
	 * @param byteBuf
	 * @param bufferInfo
	 */
	/*package*/ synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
		if (mStatredCount > 0) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
				mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
			}
		}
	}

//**********************************************************************



}
