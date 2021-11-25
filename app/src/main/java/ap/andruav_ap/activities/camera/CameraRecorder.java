package ap.andruav_ap.activities.camera;

import android.media.MediaScannerConnection;
import android.view.Surface;

import com.andruav.AndruavEngine;
import com.andruav.AndruavFacade;
import com.andruav.AndruavSettings;
import com.andruav.event.fpv7adath._7adath_RecordVideoStatus;
import com.andruav.andruavUnit.AndruavUnitBase;

import ap.andruavmiddlelibrary.CameraRecorderBase;
import ap.andruavmiddlelibrary.com.serenegiant.encoder.MediaAudioEncoder;
import ap.andruavmiddlelibrary.com.serenegiant.encoder.MediaEncoder;
import ap.andruavmiddlelibrary.com.serenegiant.encoder.MediaMuxerWrapper;
import ap.andruavmiddlelibrary.com.serenegiant.encoder.MediaVideoEncoder;

import java.nio.ByteBuffer;

import ap.andruav_ap.App;
import ap.andruavmiddlelibrary.factory.util.Time_Helper;
import ap.andruavmiddlelibrary.preference.Preference;

import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.VideoFrame;

/**
 * Created by mhefny on 12/5/15.
 */
/**
 * Created by mhefny on 12/5/15.
 */
public class CameraRecorder extends CameraRecorderBase {

    /////// ATTRIBUTES

    /**
     * muxer for audio/video recording
     */
    private MediaMuxerWrapper mMuxer;

    /**
     * for video recording
     */

    private MediaVideoEncoder mVideoEncoder;
    private MediaAudioEncoder mAudioEncoder;

    private int twoENcodersFinished = 0;




    /////////////////////////////////////////////////


    private final MediaEncoder.MediaEncoderListener mMediaEncoderAudioListener = new MediaEncoder.MediaEncoderListener()
    {

        @Override
        public void onPrepared(MediaEncoder encoder) {

        }

        @Override
        public void onStopped(MediaEncoder encoder) {
            twoENcodersFinished +=1;

            encrypt();
        }
    };

    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            // if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
            mIsRecording = true;
            AndruavSettings.andruavWe7daBase.VideoRecording = AndruavUnitBase.VIDEORECORDING_ON;
            AndruavEngine.getEventBus().post(new _7adath_RecordVideoStatus(_7adath_RecordVideoStatus.CONST_IS_RECORDING));
            if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {


                AndruavFacade.sendID("");        AndruavFacade.requestID();  // guys !! who are there ?
            }
        }


        @Override
        public void onStopped(final MediaEncoder encoder) {

            twoENcodersFinished +=1;

            if (encoder instanceof MediaVideoEncoder)
                try {
                    mIsRecording = false;

                    ///*final MainShasha parent = mWeakParent.get();
                    final String path = mVideoFile;
                    if (!mkillMe) {

                        // in kill me mode the handler is NULL
                        mhandler.sendMessageDelayed(mhandler.obtainMessage(MSG_MEDIA_UPDATE, path), 1000);
                    }
                    AndruavSettings.andruavWe7daBase.VideoRecording = AndruavUnitBase.VIDEORECORDING_OFF;

                    if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
                        AndruavFacade.sendID("");        AndruavFacade.requestID();  // guys !! who are there ?
                    }



                } catch (final Exception ex) {
                    AndruavEngine.log().logException("exception_camcorder", ex);
                }
            AndruavEngine.getEventBus().post(new _7adath_RecordVideoStatus(_7adath_RecordVideoStatus.CONST_STOP_RECORDING));

            encrypt();
        }
    };



    private void encrypt()
    {
        mhandler.post(new Runnable() {
            @Override
            public void run() {
                if (((!mRecordAudio) || (twoENcodersFinished ==2)) && (mEncrypted))
                {
                    try {
                        final String outputName = mVideoFile.replace(".mp4","") + ".enc";

                    } catch (Exception ex) {
                        AndruavEngine.log().logException("vid-file", ex);
                    }
                }
            }
        });

    }

    public  CameraRecorder ()
    {
        super();




        if (Preference.useStreamVideoHD(null)) {
            VIDEO_HEIGHT  = 1080;
            VIDEO_WIDTH = 720;
        }
        else
        {
            VIDEO_HEIGHT  = 640;
            VIDEO_WIDTH = 480;
        }
    }


    public void encodeFeed (final ByteBuffer Frame)
    {
        /*if (!mIsRecording || (mVideoEncoder == null)) return ;
        mhandler.post(() -> {
            try {
                mVideoEncoder.frameAvailableSoon();
                mVideoEncoder.encode(Frame);
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }

        });*/
        if (mVideoEncoder == null) return;
        mVideoEncoder.frameAvailableSoon();
        mVideoEncoder.encode(Frame);
    }


    public void encodeFeed (final VideoFrame Frame)
    {
        /*if (!mIsRecording || (mVideoEncoder == null)) return ;
        mhandler.post(() -> {
            try {
                mVideoEncoder.frameAvailableSoon();
                mVideoEncoder.encode(Frame);
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }

        });*/
        if (mVideoEncoder == null) return;
        //mVideoEncoder.frameAvailableSoon();

        //mVideoEncoder.encode(Frame, null);
    }




    public void encodeAudio (final byte[] data)
    {
        if (!mIsRecording || (mAudioEncoder== null)) return ;

        mAudioEncoder.frameAvailableSoon();
        mAudioEncoder.encode(ByteBuffer.wrap(data));
    }

    public void startRecording (final int width,final int height, final int frameRate, final boolean recordAudio, final boolean encrypted, final Surface videoSurface, final DefaultVideoEncoderFactory videoEncoder )
    {
        if ((width == 0) || (height ==0)) return ;

        if (mhandler== null) return ;
        // mhandler.sendEmptyMessage(MSG_CAPTURE_START)  ;
        mRecordAudio = recordAudio;
        mEncrypted = encrypted;
        VIDEO_WIDTH =width;
        VIDEO_HEIGHT =height;
        FRAME_RATE = frameRate;
        mVideoSurface = null; //videoSurface;
        mDefaultVideoEncoderFactory = videoEncoder;
        handle_StartRecording(VIDEO_WIDTH, VIDEO_HEIGHT, FRAME_RATE,mVideoSurface, mDefaultVideoEncoderFactory);
    }

    public void startRecording (final boolean recordAudio)
    {
        startRecording(VIDEO_WIDTH,VIDEO_HEIGHT,FRAME_RATE, recordAudio,true, null, null);
    }


    public void stopRecording ()
    {

        if (mhandler== null) return ;
        mhandler.removeCallbacksAndMessages(null);
        //mhandler.sendEmptyMessage(MSG_CAPTURE_STOP)  ;

        handle_StopRecording();
    }


    public String getVideoFileName ()
    {
        return mVideoFile;
    }


    @Override
    protected void handle_StopRecording()
    {
        if (mMuxer != null) {
            mMuxer.stopRecording();
            mMuxer = null;
            // you should not wait here
        }


        mVideoEncoder = null;
        mAudioEncoder = null;

    }


    @Override
    protected void handle_StartRecording(final int width,final int height,final int frameRate, final Surface videoSurface, final DefaultVideoEncoderFactory defaultVideoEncoderFactory)
    {
        try {
            twoENcodersFinished = 0;
            final String outputPath;
            if (mMuxer != null) return;

            if (App.KMLFile != null) {
                outputPath = App.KMLFile.getVideoPath();

                try {
                    if (outputPath != null) {
                        mVideoFile = App.KMLFile.getVideoPath() + "/v_" + Time_Helper.getDateTimeString()+".mp4";
                    }

                } catch (final Exception e) {
                    throw new RuntimeException("This app has no permission of writing external storage");
                }
                mMuxer = new MediaMuxerWrapper(mVideoFile);    // if you record audio only, ".m4a" is also OK.
                // for video capturing using MediaVideoEncoder

                mVideoEncoder = new MediaVideoEncoder(mMuxer, mMediaEncoderListener);

                //mVideoEncoder = mDefaultVideoEncoderFactory.createEncoder(mDefaultVideoEncoderFactory.getSupportedCodecs()[0]);



                if (mRecordAudio) {
                    mAudioEncoder = new MediaAudioEncoder(mMuxer, mMediaEncoderAudioListener);
                }
                mMuxer.prepare(width, height, frameRate);
                mMuxer.startRecording();
            }
        } catch (final Exception ex) {
            AndruavEngine.log().logException("exception_camcorder", ex);
            AndruavEngine.getEventBus().post(new _7adath_RecordVideoStatus(_7adath_RecordVideoStatus.CONST_ERROR_RECORDING));

        }
    }



    public void handle_UpdateMedia(final String path) {
        //if (DEBUG) Log.v(TAG_THREAD, "handleUpdateMedia:path=" + path);
        //final MainShasha parent = mWeakParent.get();
        MediaScannerConnection.scanFile(App.getAppContext(), new String[]{path}, null, null);
    }

    @Override
    public void shutDown()
    {

        this.stopRecording();

        super.shutDown();


        App.KMLFile = null;

    }





}
