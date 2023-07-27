package ap.andruav_ap.widgets.camera;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.interfaces.INotification;
import com.andruav.notification.PanicFacade;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;

import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.App;
import ap.andruav_ap.activities.camera.CameraRecorder;
import ap.andruavmiddlelibrary.eventClasses.fpvEvent.Event_FPV_VideoURL;
import ap.andruav_ap.R;
import com.andruav.FeatureSwitch;

import ap.andruavmiddlelibrary.factory.util.Time_Helper;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.webrtc.IRTCListener;
import ap.andruavmiddlelibrary.webrtc.classes.AndruavVideoFileRenderer;
import ap.andruavmiddlelibrary.webrtc.events.Event_WebRTC;
import ap.andruavmiddlelibrary.webrtc.classes.PeerConnectionManager;

import ap.andruavmiddlelibrary.webrtc.classes.PnPeer;
import ap.andruavmiddlelibrary.webrtc.classes.VSink;
import ap.andruavmiddlelibrary.Voting;
//import rcmobile.boof.BoofSurfaceView;


/**
 * Created by mhefny on 2/28/16.
 */
public class AndruavRTCVideoDecorderWidget  extends RelativeLayout implements IRTCListener, VideoSink, VSink{


    //////// Attributes
    AndruavRTCVideoDecorderWidget Me;
    PeerConnectionManager peerConnectionManager;
    private CameraRecorder mcameraRecorder;
    private boolean mIsVideoOn = false;
    private LayoutInflater mInflater;
    private AndruavUnitShadow mAndruavWe7da;

    private SurfaceViewRenderer surfaceViewRenderer;
    private AndruavVideoFileRenderer mVideoFileRenderer;
    private Handler mhandler;
    private TextView mTxtLabel;
    private final boolean enableTracking = false;
    private static final int FRAME_RATE = 15;

    private IRTCVideoDecoder mIrtcVideoDecoder;

    public void setIRTCVideoDecoder( final IRTCVideoDecoder irtcVideoDecoder)
    {
        mIrtcVideoDecoder = irtcVideoDecoder;
    }
    //////////BUS EVENT
    public void onEvent (final Event_FPV_VideoURL event_fpv_videoURL)
    {
        if (mhandler == null) return ;

        final Message msg = mhandler.obtainMessage();
        msg.obj = event_fpv_videoURL;
        mhandler.sendMessageDelayed(msg,0);

    }

    public void onEvent (final Event_WebRTC event_webRTC) {

        if (mhandler == null) return ;

        final Message msg = mhandler.obtainMessage();
        msg.obj = event_webRTC;
        mhandler.sendMessageDelayed(msg,0);
    }

    public AndruavRTCVideoDecorderWidget(Context context) {
        super(context);

        initGUI(context);
        init();
        UIHandler();
    }

    public AndruavRTCVideoDecorderWidget(Context context, AttributeSet attrs) {
        super(context, attrs);

        initGUI(context);
        init();
        UIHandler();
    }

    public AndruavRTCVideoDecorderWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initGUI(context);
        init();
        UIHandler();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AndruavRTCVideoDecorderWidget(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initGUI(context);
        init();
        UIHandler();
    }



    public void doMirror (final boolean mirror)
    {
        peerConnectionManager.doMirror(mirror);
    }

    public void stopRecording ()
    {
        if (mVideoFileRenderer == null) return;

        mVideoFileRenderer.release();
        mVideoFileRenderer = null;
        final String s = App.context.getString(R.string.action_record_video_stop_local);
        AndruavEngine.notification().Speak(s);
        mTxtLabel.setText(s);
        mTxtLabel.setTextColor(Color.WHITE);
        mTxtLabel.setVisibility(View.VISIBLE);

        mhandler.postDelayed(() -> mTxtLabel.setVisibility(View.INVISIBLE), 3000);
    }


    public void startRecording ()
    {

        try {
            final int height, width;

            height  = 1080;
            width = 720;
                /*
                height  = 200;
                width   = 320;
                */

            try {
                //mVideoByteRenderer = new VideoByteRenderer(Me,width,height, mPeerConnectionManager.getEglBaseContextRX());
                mVideoFileRenderer = new AndruavVideoFileRenderer(App.KMLFile.getVideoPath() + "/v_" + Time_Helper.getDateTimeString() + ".mp4",FRAME_RATE, width,height,peerConnectionManager.getEglBaseContext());
            } catch (Exception e) {
                e.printStackTrace();
            }

            final String s = App.context.getString(R.string.action_record_video_start_local);
            AndruavEngine.notification().Speak(s);
            mTxtLabel.setText(s);
            mTxtLabel.setTextColor(Color.RED);
            mTxtLabel.setVisibility(View.VISIBLE);

            mhandler.postDelayed(() -> mTxtLabel.setVisibility(View.INVISIBLE), 3000);
        }
        catch (Exception e)
        {
            PanicFacade.cannotStartCamera(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_CAMERA, "Cannot save video", null);

        }
    }

    public boolean isRecording ()
    {
        return mVideoFileRenderer != null;
    }
    public boolean isVideoOn()
    {
        return mIsVideoOn;

    }
    private void UIHandler () {

        if (this.isInEditMode()) return ;

        mhandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);


                if (msg.obj instanceof Event_WebRTC) {
                    Event_WebRTC event_webRTC = (Event_WebRTC) msg.obj;
                    switch (event_webRTC.getEventType())
                    {
                        case Event_WebRTC.EVENT_CLOSED_CONNECTION:
                            if (isRecording())
                            {
                                stopRecording();
                            }
                            mTxtLabel.setText(App.context.getString(R.string.action_video_off));
                            mTxtLabel.setTextColor(App.context.getResources().getColor(R.color.btn_TXT_ERROR));
                            mTxtLabel.setVisibility(View.VISIBLE);
                            if (mAndruavWe7da!=null)
                            {
                                mAndruavWe7da.VideoStreamingActivated = false;
                                mAndruavWe7da.VideoStreamingChannel = "";
                            }
                            mIsVideoOn = false;

                            if (mIrtcVideoDecoder!=null) mIrtcVideoDecoder.IRTCVideoDecoder_onClosingVideo();

                            break;

                        case Event_WebRTC.EVENT_CONNECT_SUCCEEDED: {
                            final String s = App.context.getString(R.string.action_video_rx);
                            mTxtLabel.setText(s);
                            mTxtLabel.setTextColor(Color.WHITE);
                            mTxtLabel.setVisibility(View.VISIBLE);
                            Voting.onVideoRecieved();
                            AndruavEngine.notification().Speak(s);
                            mhandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mTxtLabel.setVisibility(View.INVISIBLE);
                                }
                            }, 3000);
                            if (mAndruavWe7da!=null)
                            {
                                mAndruavWe7da.VideoStreamingActivated = true;
                            }
                            mIsVideoOn = true;

                            if (mIrtcVideoDecoder!=null) mIrtcVideoDecoder.IRTCVideoDecoder_onReceivingVideo();
                        }
                            break;

                        case Event_WebRTC.EVENT_CONNECTION_ERROR: {
                            final String s = App.context.getString(R.string.action_video_err);
                            mTxtLabel.setText(App.context.getString(R.string.action_video_err));
                            mTxtLabel.setTextColor(App.context.getResources().getColor(R.color.btn_TXT_ERROR));
                            mTxtLabel.setVisibility(View.VISIBLE);
                            AndruavEngine.notification().Speak(s);
                            if (mAndruavWe7da!=null)
                            {
                                mAndruavWe7da.VideoStreamingActivated = false;
                                mAndruavWe7da.VideoStreamingChannel = "";
                            }
                            mIsVideoOn = false;



                            if (mIrtcVideoDecoder!=null) mIrtcVideoDecoder.IRTCVideoDecoder_onError();
                        }
                            break;
                    }
                }

                else if (msg.obj instanceof Event_FPV_VideoURL) {

                    Event_FPV_VideoURL event_fpv_videoURL = (Event_FPV_VideoURL) msg.obj;
                    String channel;
                    if ((event_fpv_videoURL.VideoTracks == null) || (event_fpv_videoURL.VideoTracks.isEmpty()))
                    {
                        channel = "default";
                        return ;
                    }
                    else
                    {
                        channel = event_fpv_videoURL.VideoTracks.get(0).mVideoID;
                    }
                    Me.connectToDroneCamera(event_fpv_videoURL.andruavUnit.PartyID,channel); // this should be a correct Name PeerConnectionManager.getProperName()
                    return;
                }

            }
        };
    }


    private void init()
    {
        mcameraRecorder = new CameraRecorder();
        mcameraRecorder.init();

    }

    private void initGUI (final Context context) {
        if (this.isInEditMode()) return ;

    try {


        Me = this;

        mInflater = LayoutInflater.from(context);

        mInflater.inflate(R.layout.widget_andruav_rtc_video_decoder, this, true);
            mTxtLabel = findViewById(R.id.fpvactivity_rtc_txtlabel);
            mTxtLabel.setVisibility(View.INVISIBLE);
            surfaceViewRenderer = findViewById(R.id.fpvactivity_rtc_glviewsurface);
            peerConnectionManager = new PeerConnectionManager();

            if (Preference.getFPVActivityRotation(null) == Preference.SCREEN_ORIENTATION_PORTRAIT) {
                peerConnectionManager.setRotationGCS(90);
            }
            peerConnectionManager.init(App.getAppContext(), this, this, surfaceViewRenderer, false, AndruavSettings.andruavWe7daBase.PartyID);

        }
        catch (Exception e)
        {
            if (FeatureSwitch.DEBUG_MODE) {
                Log.e("fpv", "initGUI: ", e.fillInStackTrace());
            }
        }

    }

    @Override
    public void setVisibility(int visibility) {

        super.setVisibility(visibility);

    }

    public void setAndruavUnit (AndruavUnitShadow andruavWe7da)
    {
        mAndruavWe7da = andruavWe7da;

        /*if (mBoofSurfaceView!=null)
        {
            mBoofSurfaceView.setAndruavUnit(andruavWe7da);
        }
        */
    }

    public final AndruavUnitShadow getAndruavUnit ()
    {
        return mAndruavWe7da;
    }

    public void connectToDroneCamera (final String partyID, final String channel)
    {
        //peerConnectionManager.connectToDrone (PartyID);
        peerConnectionManager.joinToDrone (partyID, channel);
        mTxtLabel.setVisibility(View.VISIBLE);
        mTxtLabel.setText(App.context.getString(R.string.action_video_con));
        mTxtLabel.setTextColor(Color.WHITE);

    }

    public void disconnectVideo (final String partyID, final String channel)
    {
        peerConnectionManager.disconnectToDrone (partyID, channel);
        if (mAndruavWe7da!=null)
        {
            mAndruavWe7da.VideoStreamingActivated = false;
            mAndruavWe7da.VideoStreamingChannel = "";
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        try {

            if (peerConnectionManager != null) {
                peerConnectionManager.onResume();
            }

            /*if (mBoofSurfaceView!=null)
            {
                mBoofSurfaceView.setEnableTargetSelect(false);
            }
            */

            EventBus.getDefault().register(this);
        }
        catch (Exception e)
        {
            AndruavEngine.log().logException("rtc", e);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    try {

        if (peerConnectionManager!= null)
        {
            peerConnectionManager.onPause();
            peerConnectionManager.onDestroy();
        }
        if (mAndruavWe7da!= null)
        {
            mAndruavWe7da.VideoStreamingActivated = false;
            mAndruavWe7da.VideoStreamingChannel = "";
        }
        EventBus.getDefault().unregister(this);
        mhandler.removeCallbacksAndMessages(null);
        mhandler = null;
    }
    catch (Exception e)
    {
        AndruavEngine.log().logException("rtc", e);
    }
    }




    @Override
    public void onLocalStream(MediaStream localStream) {

    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, PnPeer peer) {
        //  Toast.makeText(Me, "Connected to " + peer.getId(), Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onRemoveRemoteStream(MediaStream remoteStream, PnPeer peer) {

    }

    @Override
    public void onPeerConnectionClosed(PnPeer peer) {
        // called when Video Connection broken or disconnected due to client or server.
        // called if you press stop video.
        // called if you press back in Drone
        return ;

    }

    @Override
    public void onPeerConnected(String userId) {
        return ;
    }


    public void openVideo (final AndruavUnitShadow andruavWe7da)
    {
        andruavWe7da.VideoStreamingActivated = true;
        disconnectVideo(null,null); // disconnect from others if exist
        setAndruavUnit(andruavWe7da);

    }

    public void closeVideo (final AndruavUnitShadow andruavWe7da)
    {
        //imgVideoStreaming.setVisibility(View.INVISIBLE);
        setVisibility(View.INVISIBLE);
        disconnectVideo(null,null);
        setAndruavUnit(null);
        setVisibility(View.INVISIBLE);
        andruavWe7da.VideoStreamingActivated = false;
    }

    @Override
    public void onFrame(byte[] frame, int offset, int size) {

    }



    long lastTimeFrame = 0;
    @Override
    public void onFrame(VideoFrame videoFrame) {
        /*if (mVideoByteRenderer != null) {
            mVideoByteRenderer.onFrame(videoFrame);

        }*/

        if (mVideoFileRenderer != null) {
            long now = System.currentTimeMillis();
            if ((now - lastTimeFrame) > (1000 / FRAME_RATE)) {
                lastTimeFrame = now;
                mVideoFileRenderer.onFrame(videoFrame);
            }

            //mVideoFileRenderer.onFrame(videoFrame);
        }
    }



    public interface IRTCVideoDecoder {
        void IRTCVideoDecoder_onError();

        void IRTCVideoDecoder_onReceivingVideo();

        void IRTCVideoDecoder_onClosingVideo();

    }


}
