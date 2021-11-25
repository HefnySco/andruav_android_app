package ap.andruavmiddlelibrary;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Surface;

import com.andruav.event.systemEvent.Event_ShutDown_Signalling;

import org.webrtc.DefaultVideoEncoderFactory;

import de.greenrobot.event.EventBus;

public abstract class CameraRecorderBase {
    ///////// CONSTANTS
    protected static final int MSG_OPEN = 0;
    protected static final int MSG_CLOSE = 1;
    protected static final int MSG_PREVIEW_START = 2;
    protected static final int MSG_PREVIEW_STOP = 3;
    protected static final int MSG_CAPTURE_STILL = 4;
    protected static final int MSG_CAPTURE_START = 5;
    protected static final int MSG_CAPTURE_STOP = 6;
    protected static final int MSG_MEDIA_UPDATE = 7;
    protected static final int MSG_RELEASE = 9;

    /////// ATTRIBUTES

    protected int VIDEO_WIDTH = 640;
    protected int VIDEO_HEIGHT = 480;
    protected int FRAME_RATE = 15;


    protected Handler mhandler;
    protected HandlerThread mhandlerThread;

    protected boolean mIsRecording = false;
    protected boolean mRecordAudio = false;
    protected boolean mEncrypted = false;
    protected boolean mkillMe = false;


    protected String mVideoFile;
    protected Surface mVideoSurface;
    protected DefaultVideoEncoderFactory mDefaultVideoEncoderFactory;
    protected CameraRecorderBase Me;


    //////////BUS EVENT


    public void onEvent (final Event_ShutDown_Signalling event)
    {
        if (event.CloseOrder != 2) return ;


        this.shutDown();
    }


    /////////////////////////////////////////////////



    public CameraRecorderBase ()
    {
        Me = this;
    }


    public void init ()
    {

        EventBus.getDefault().register(this);
        initHandler();
    }

    private void initHandler () {

        mhandlerThread = new HandlerThread("Camera Recorder");
        mhandlerThread.start(); //NOTE: mhandlerThread.getLooper() will return null if not started.

        mhandler = new Handler(mhandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (mkillMe) return;
                switch (msg.what) {
                    case MSG_CAPTURE_START:
                        handle_StartRecording(VIDEO_WIDTH, VIDEO_HEIGHT, FRAME_RATE,mVideoSurface,mDefaultVideoEncoderFactory );
                        break;
                    case MSG_CAPTURE_STOP:
                        handle_StopRecording();
                        break;
                    case MSG_MEDIA_UPDATE:
                        //handle_UpdateMedia((String)msg.obj);
                        break;
                }
            }

        };
    }

    public void shutDown()
    {

        mkillMe = true;
        EventBus.getDefault().unregister(this);

        if (mhandler != null) {
            mhandler.removeCallbacksAndMessages(null);
            mhandler = null;
        }

        if (mhandlerThread != null)
        {
            mhandlerThread.quit();
        }
    }


    public boolean isRecording()
    {
        return mIsRecording;
    }


    protected abstract void handle_StartRecording(final int width, final int height, final int frameRate, final Surface videoSurface, final DefaultVideoEncoderFactory defaultVideoEncoderFactory);
    protected abstract void handle_StopRecording();

}

