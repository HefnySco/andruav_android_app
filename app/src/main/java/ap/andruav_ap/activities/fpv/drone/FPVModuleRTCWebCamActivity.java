

package ap.andruav_ap.activities.fpv.drone;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.andruav.AndruavFacade;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.FeatureSwitch;
import com.andruav.event.droneReport_Event.Event_CameraZoom;
import com.andruav.event.droneReport_Event.Event_Vehicle_Flying_Changed;
import com.andruav.event.droneReport_Event.Event_Vehicle_Mode_Changed;
import com.andruav.event.fpv7adath.Event_FPV_CMD;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitSystem;
import com.andruav.interfaces.INotification;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.notification.PanicFacade;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;

import org.webrtc.AndruavWebRTCGlobals;
import org.webrtc.ContextUtils;
import org.webrtc.EglRenderer;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.App;
import ap.andruav_ap.DeviceManagerFacade;
import ap.andruav_ap.R;
import ap.andruav_ap.activities.camera.CameraRecorder;
import ap.andruav_ap.activities.camera.Event_RecordVideoStatus;
import ap.andruav_ap.guiEvent.GUIEvent_EnableFlashing;
import ap.andruav_ap.widgets.AlarmWidget;
import ap.andruavmiddlelibrary.Voting;
import ap.andruavmiddlelibrary.sensors._7asasatEvents.Event_IMU_CMD;
import ap.andruavmiddlelibrary.com.serenegiant.encoder.MediaVideoEncoder;
import ap.andruavmiddlelibrary.eventClasses.fpvEvent.Event_FPV_Image;
import ap.andruavmiddlelibrary.factory.io.FileHelper;
import ap.andruavmiddlelibrary.factory.util.ActivityMosa3ed;
import ap.andruavmiddlelibrary.factory.util.Image_Helper;
import ap.andruavmiddlelibrary.factory.util.Time_Helper;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.webrtc.IRTCListener;
import ap.andruavmiddlelibrary.webrtc.classes.AndruavVideoFileRenderer;
import ap.andruavmiddlelibrary.webrtc.classes.PeerConnectionManager;
import ap.andruavmiddlelibrary.webrtc.classes.PnPeer;
import ap.andruavmiddlelibrary.webrtc.classes.VSink;
import ap.andruavmiddlelibrary.webrtc.classes.VideoByteRenderer;
import ap.andruavmiddlelibrary.webrtc.events.Event_WebRTC;

/*
    This is FPV Camera Display for Camera Module.
    Author: Mohammad S. Hefny
    Date: Feb 2020
*/

public class FPVModuleRTCWebCamActivity extends Activity implements IRTCListener, VideoSink, VSink {


    private final Object stateLock = new Object();

    private FPVModuleRTCWebCamActivity Me;
    private static Handler mHandle;
    private SurfaceViewRenderer mSurfaceViewRenderer;
    PeerConnectionManager mPeerConnectionManager;
    private AndruavUnitBase andruavUnit_selected;
    private final Object synObj = new Object();

    private boolean mTakeImage = false;
    private int mTakeImageCount = 0;
    private boolean mSaveImageLocally = true;
    private long mTimeBetweenShots = 0;
    private double mDistanceBetweenShotes = 0; // ignored
    private boolean mSendBackImages = false;
    private AndruavUnitBase mSendBackTo = null; // name of Image requester
    int frameHeight, frameWidth;
    private boolean mRecordVideo = false;
    /**
     * muxer for audio/video recording
     */
    //private CameraRecorder mcameraRecorder;


    ////// ActivityMosa3ed Variables
    private TextView txtVideoStatus;
    private Button btnZeroTilt;
    private Button btnCameraSwitch;


    private boolean skip = false;  // handle conncurrency in taking images


    private AlarmWidget alarmWidget;

    /**
     * ScheduledExecutorService used to periodically schedule the rcRepeater.
     */
    private ScheduledExecutorService rcRepeater;
    private VideoByteRenderer mVideoByteRenderer;

    private AndruavVideoFileRenderer mVideoFileRenderer;
    private CameraRecorder mcameraRecorder;


    public void onEvent (final Event_CameraZoom adath_cameraZoom)
    {
        final Message msg = mHandle.obtainMessage();
        msg.obj = adath_cameraZoom;
        mHandle.sendMessageDelayed(msg,0);
    }


    public void onEvent (GUIEvent_EnableFlashing guiEvent_enableFlashing)
    {
        if (AndruavSettings.andruavWe7daBase.getIsCGS())
        {
            // you cannot switch screen for a user.
            return;
        }


        if (guiEvent_enableFlashing.enableFlashing) {
            mHandle.post(new Runnable() {
                @Override
                public void run() {

                    if ((alarmWidget == null)) {

                        alarmWidget = new AlarmWidget(App.activeActivity);
                    }
                    alarmWidget.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    App.activeActivity.addContentView(alarmWidget, alarmWidget.getLayoutParams());


                }
            });
        }
        else
        {
            mHandle.post(new Runnable() {
                @Override
                public void run() {
                    if (alarmWidget != null) {
                        ActivityMosa3ed.removeMeFromParentView(alarmWidget);
                        alarmWidget = null;
                    }
                }
            });
        }


    }


    public void onEvent (final Event_Vehicle_Mode_Changed adath_vehicle_mode_changed)
    {

        if (!adath_vehicle_mode_changed.mAndruavWe7da.IsMe()) return ;

        final Message msg = mHandle.obtainMessage();
        msg.obj = adath_vehicle_mode_changed;
        mHandle.sendMessageDelayed(msg,0);
    }


    public void onEvent (final Event_Vehicle_Flying_Changed adath_vehicle_flying_changed)
    {

        if (!adath_vehicle_flying_changed.mAndruavWe7da.IsMe()) return ;

        final Message msg = mHandle.obtainMessage();
        msg.obj = adath_vehicle_flying_changed;
        mHandle.sendMessageDelayed(msg,0);
    }

    public void onEvent(final Event_FPV_CMD a7adath_fpv_CMD) {

        if(!DeviceManagerFacade.hasCamera()) return ;
        final Message msg = mHandle.obtainMessage();
        msg.obj = a7adath_fpv_CMD;
        mHandle.sendMessageDelayed(msg,0);
    }

    public void onEvent (final Event_WebRTC event_webRTC) {
        final Message msg = mHandle.obtainMessage();
        msg.obj = event_webRTC;
        mHandle.sendMessageDelayed(msg,0);
    }


    private final Runnable defaultSchedular = new Runnable() {
        @Override
        public void run() {
            // issue server and connected to online

        }
    };


    /***
     * Event to UI gate to enable access UI safely.
     */
    private void UIHandler () {


        mHandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);


                if (msg.obj instanceof Event_CameraZoom)
                {
                    Event_CameraZoom adath_cameraZoom = (Event_CameraZoom) msg.obj;
                    if (adath_cameraZoom.ZoomValue == Double.MAX_VALUE)
                    {
                        float zoom = mPeerConnectionManager.getZoom();
                        if (adath_cameraZoom.ZoomIn)
                        {
                            zoom +=adath_cameraZoom.ZoomValueStep;
                        }
                        else
                        {
                            zoom -=adath_cameraZoom.ZoomValueStep;
                        }
                        mPeerConnectionManager.setZoom(zoom);
                    }
                }

                else if (msg.obj instanceof Event_WebRTC) {
                    Event_WebRTC event_webRTC = (Event_WebRTC) msg.obj;
                    switch (event_webRTC.getEventType())
                    {
                        case Event_WebRTC.EVENT_CLOSED_CONNECTION:
                            txtVideoStatus.setVisibility(View.INVISIBLE);
                        break;
                        case Event_WebRTC.EVENT_CONNECT_SUCCEEDED:
                            txtVideoStatus.setText(App.context.getString(ap.andruavmiddlelibrary.R.string.action_video_on));
                            txtVideoStatus.setVisibility(View.VISIBLE);
                        break;
                        case Event_WebRTC.EVENT_CONNECTION_ERROR:
                            //txtVideoStatus.setText(App.context.getString(R.string.action_video_err));
                            //txtVideoStatus.setVisibility(View.VISIBLE);
                        break;
                    }
                }

                else if (msg.obj instanceof Event_Vehicle_Mode_Changed)
                {
                    final Event_Vehicle_Mode_Changed mode = (Event_Vehicle_Mode_Changed)msg.obj;
                    switch (mode.mAndruavWe7da.getFlightModeFromBoard())
                    {
                        case FlightMode.CONST_FLIGHT_CONTROL_RTL:
                        case FlightMode.CONST_FLIGHT_CONTROL_AUTO:
                        case FlightMode.CONST_FLIGHT_CONTROL_CRUISE:
                        case FlightMode.CONST_FLIGHT_CONTROL_GUIDED:
                            takeSSingleImage(new AndruavUnitSystem());
                            break;
                    }


                }else if (msg.obj instanceof Event_Vehicle_Flying_Changed)
                {
                    takeSSingleImage(new AndruavUnitSystem());

                }

                else if (msg.obj instanceof Event_FPV_CMD) {
                    Event_FPV_CMD a7adath_FPV_CMD = (Event_FPV_CMD) msg.obj;
                    switch (a7adath_FPV_CMD.CMD_ID) {
                        case Event_FPV_CMD.FPV_CMD_FLASHCAM:
                            mPeerConnectionManager.setFlash(a7adath_FPV_CMD.ACT? AndruavWebRTCGlobals.FlashOn:AndruavWebRTCGlobals.FlashOff);
                            break;

                        case Event_FPV_CMD.FPV_CMD_SWITCHCAM:
                            mPeerConnectionManager.switchCamera();
                            break;

                        case Event_FPV_CMD.FPV_CMD_TAKEIMAGE:
                            mTakeImageCount = a7adath_FPV_CMD.NumberOfImages;
                            mSaveImageLocally = a7adath_FPV_CMD.SaveImageLocally;
                            if (a7adath_FPV_CMD.TimeBetweenShotes ==0)
                            {  // in case the interval is Zero then make it one second by default.
                                a7adath_FPV_CMD.TimeBetweenShotes = 1;
                            }
                            mTimeBetweenShots = a7adath_FPV_CMD.TimeBetweenShotes * 1000 ; // convert to milliseconds
                            mDistanceBetweenShotes = a7adath_FPV_CMD.DistanceBetweenShotes;
                            mSendBackImages = a7adath_FPV_CMD.SendBackImages;

                            mSendBackTo = a7adath_FPV_CMD.Requester;

                            mTakeImage = true;
                            takeImage();
                           // PanicFacade.cannotStartCamera(INotification.NOTIFICATION_TYPE_ERROR,INotification.NOTIFICATION_TYPE_ERROR,"Take Image is not supported in this video mode yet",null);
                            break;


                        case Event_FPV_CMD.FPV_CMD_ROTATECAM:

                            break;

                        case Event_FPV_CMD.FPV_CMD_RECORDVIDEO:
                            mRecordVideo = a7adath_FPV_CMD.ACT;
                            if (!mRecordVideo)
                            {
                                stopRecording();
                            }
                            else
                            {
                                startRecording();

                            }break;
                        case Event_FPV_CMD.FPV_CMD_STREAMVIDEO:
                            /*final String unitName2 = a7adath_FPV_CMD.Requester;
                            mSendBackTo = AndruavMo7arek.getAndruavWe7daMapBase().get(unitName2);

                            if (a7adath_FPV_CMD.ACT) {
                                if (unitName2 != null) {
                                    AndruavFacade.sendVideoWebRTCConnectToMeInfo(AndruavSettings.andruavWe7daBase.PartyID, mSendBackTo);
                                }
                            }
                            else
                            {
                                // disconnect Video
                                if (unitName2 != null) {
                                    AndruavPeerConnectionClientClient.sendHangUpTo(unitName2);
                                }
                            }*/
                            break;

                    }

                }

            }
        };


        if (rcRepeater == null || rcRepeater.isShutdown()) {
            rcRepeater = Executors.newSingleThreadScheduledExecutor();
            rcRepeater.scheduleWithFixedDelay(defaultSchedular, 10, 30, TimeUnit.SECONDS);
        }
    }



    private void initRTC() {

        ContextUtils.initialize(AndruavEngine.AppContext);

        mSurfaceViewRenderer = findViewById(R.id.fpvactivity_rtc_glviewsurface);

        if (mPeerConnectionManager == null)
        {

            mPeerConnectionManager = new PeerConnectionManager();
            mSurfaceViewRenderer.setEnableHardwareScaler(true);

            final boolean res = mPeerConnectionManager.init(this, this, this, mSurfaceViewRenderer, true, AndruavSettings.andruavWe7daBase.PartyID);
            if (!res) {
                PanicFacade.cannotStartCamera();
                Voting.onCameraIssue();
            }


        }


    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);


    }

    private void initGUI ()
    {

        txtVideoStatus = findViewById(R.id.fpvactivity_txtVideoStatus);
        txtVideoStatus.setVisibility(View.INVISIBLE);

        btnZeroTilt = findViewById(R.id.fpvactivity_btn_ZeroTilt);
        btnZeroTilt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (FeatureSwitch.DEBUG_MODE)
                {
                    if (!mRecordVideo)
                    {
                        startRecording();
                    }
                    else
                    {
                        stopRecording();

                    }
                }
                else {
                    EventBus.getDefault().post(new Event_IMU_CMD(Event_IMU_CMD.IMU_CMD_UpdateZeroTilt)); // tel IMU to reread Tilt
                }
            }
        });



        btnCameraSwitch = findViewById(R.id.fpvactivity_btn_CameraSwitch);
        btnCameraSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (andruavUnit_selected.UnitID.equals(AndruavSettings.andruavWe7daBase.UnitID)) {
                    mPeerConnectionManager.switchCamera();
                }
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Me = this;
        App.activeActivity = this;
        App.ForceLanguage();
        //setScreenOrientation(false);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window win = getWindow();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_fpvmodule_rtcweb_cam);

        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        andruavUnit_selected =  AndruavSettings.andruavWe7daBase;

      //initRTC();
        initGUI();



    }

    private void setScreenOrientation (boolean speak)
    {
        int orientation = Preference.getFPVActivityRotation(null);
        int requestedOrientation= ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        switch (orientation)
        {
            case Surface.ROTATION_0:
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                if (speak) AndruavEngine.notification().Speak("zero degree");
                break;
            case Surface.ROTATION_90:
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                if (speak) AndruavEngine.notification().Speak("90 degrees");
                break;
            case Surface.ROTATION_180:
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                if (speak) AndruavEngine.notification().Speak("180 degrees");
                break;
            case Surface.ROTATION_270:
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                if (speak) AndruavEngine.notification().Speak("270 degrees");
                break;
        }
        setRequestedOrientation(requestedOrientation);
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        // get out of this screen so that you can re-initailize video.
        this.finish();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

        @Override
    protected void onResume() {
        super.onResume();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (mPeerConnectionManager != null)
        {
            mPeerConnectionManager.onResume();
        }
        else
        {
            initRTC();
        }

        EventBus.getDefault().register(this);

        //App.startSensorService();

    }

    @Override
    protected void onPause() {

        try
        {
            mHandle.removeCallbacksAndMessages(null);

            if (mPeerConnectionManager != null)
            {
                mPeerConnectionManager.onPause();
                mPeerConnectionManager.onDestroy();
            }
            mSurfaceViewRenderer.release();

            EventBus.getDefault().unregister(this);

            if (rcRepeater != null ) {
                rcRepeater.shutdownNow();
                rcRepeater = null;
            }

            //mcameraRecorder.shutDown();

            super.onPause();
        }
        catch (Exception e)
        {
            AndruavEngine.log().logException("rtc", e);
        }

    }

    @Override
    protected void onDestroy() {
        //mPeerConnectionManager.onDestroy();
        super.onDestroy();

    }

    @Override
    public void onStart() {
        super.onStart();

        App.activeActivity = this;

        UIHandler();


    }

    @Override
    public void onLocalStream(final MediaStream localStream) {

        if (!App.isAndruavWSConnected()) return ;

        Log.d("fpvstream", "onLocalStream");
        mHandle.postDelayed(new Runnable() {
            @Override
            public void run() {
                final int t = Preference.getFirstServer(null) ;

                Preference.setFirstServer(null,t+1);
            }
        },(long) (Math.random() + 1) * 2000);
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, PnPeer peer) {
    }

    @Override
    public void onRemoveRemoteStream(MediaStream remoteStream, PnPeer peer) {
    }

    @Override
    public void onPeerConnectionClosed(final PnPeer peer) {
        synchronized (synObj) {
            if (mHandle == null) return;

            mHandle.post(() ->
            {
                AndruavSettings.mVideoRequests.remove(peer.getConnectedPeer().PartyID);
            });
        }
    }

    @Override
    public void onPeerConnected(final String  userId) {
        //txtVideoStatus.setVisibility(View.VISIBLE);
    }


    private void startRecording()
    {
        mRecordVideo = false;
        final int height, width;
        if (Preference.useStreamVideoHD(null)) {
            height  = 1080;
            width = 720;
        }
        else
        {
            height  = 640;
            width = 480;
        }
        try {
            try {

                if (MediaVideoEncoder.VIDEO_FORMAT== MediaVideoEncoder.MOBILE_WORK_FOR_ALL)
                {
                    mVideoFileRenderer = new AndruavVideoFileRenderer(App.KMLFile.getVideoPath() + "/v_" + Time_Helper.getDateTimeString()+".mp4",15,width,height, mPeerConnectionManager.getEglBaseContextTX());
                }
                else
                {
                    mcameraRecorder = new CameraRecorder();
                    mcameraRecorder.init();
                    mcameraRecorder.startRecording(width,height,15,false,false, mPeerConnectionManager.getSurfaceTX(), mPeerConnectionManager.getDefaultVideoEncoderFactory());

                    mVideoByteRenderer = new VideoByteRenderer(Me,width,height,mPeerConnectionManager.getEglBaseContextTX());
                }


            } catch (Exception e) {
                e.printStackTrace();
                mRecordVideo = false;
            }

            // must be last
            mRecordVideo = true;

            AndruavSettings.andruavWe7daBase.VideoRecording = AndruavUnitBase.VIDEORECORDING_ON;
            AndruavEngine.getEventBus().post(new Event_RecordVideoStatus(Event_RecordVideoStatus.CONST_IS_RECORDING));
            if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
                AndruavFacade.broadcastID();
            }

        } catch (Exception e) {
            e.printStackTrace();
            mRecordVideo = false;
        }

    }

    private void stopRecording()
    {
        mRecordVideo = false;
        if (mVideoFileRenderer!= null) {
            mVideoFileRenderer.release();
            mVideoFileRenderer = null;
        }
        if (mVideoByteRenderer!= null) {
            mVideoByteRenderer.release();
            mVideoByteRenderer = null;
            mcameraRecorder.stopRecording();
            mcameraRecorder = null;
        }

        AndruavSettings.andruavWe7daBase.VideoRecording = AndruavUnitBase.VIDEORECORDING_OFF;
        if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
            AndruavFacade.broadcastID();
        }
    }


    long lastSSImage =0;
    /***
     *
     * Take Silent Single Image
     * @param andruavUnitBase
     */
    private void takeSSingleImage(final AndruavUnitBase andruavUnitBase)
    {

        final long now = System.currentTimeMillis();
        if ((now - lastSSImage) < 20000) return; // dont take too close images.
        lastSSImage = now;
        mTakeImageCount = 1;
        mSaveImageLocally = false;
        mTimeBetweenShots = 0; // convert to milliseconds
        mDistanceBetweenShotes = 0;
        mSendBackImages = true;
        mSendBackTo = andruavUnitBase;
        mTakeImage = true;
        takeImage();
    }

    private void takeImage()
    {
        if (mSurfaceViewRenderer ==null) return;
        mSurfaceViewRenderer.addFrameListener(fl ,1);
    }


    private final EglRenderer.FrameListener fl = new EglRenderer.FrameListener() {

        private final int exception_cam_mux_counter = 3;
        private long lastTime = 0;
        private final long mTimeBetweenBoof = 300;

        @Override
        public void onFrame(Bitmap bitmap) {
            synchronized (stateLock) {
                if (mTakeImageCount > 0) {
                    final long now = System.currentTimeMillis();
                    if (mTimeBetweenShots > 0) {  // dont take multiple images if time is equal to zero ... no burst images

                        if ((now - lastTime) >= mTimeBetweenShots) {
                            mTakeImageCount = mTakeImageCount - 1;
                            mTakeImage = true;
                            lastTime = now;
                        } else {
                            mTakeImage = false;
                        }

                        if (mTakeImageCount > 0) {
                            mHandle.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (mTakeImageCount == 0) return;
                                    takeImage();
                                }
                            }, mTimeBetweenShots);
                        }

                    } else {
                        // only a single image
                        mTakeImageCount = 0; // reset counter
                        mTakeImage = true;
                    }

                } else {
                    mTakeImage = false;
                }

                if (mTakeImage) {
                    try {
                        final String imageDescription = "Image No#" + mTakeImageCount;

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        byte[] pout = stream.toByteArray();
                        stream.flush();


                        AndruavFacade.sendImage(pout, AndruavSettings.andruavWe7daBase.getAvailableLocation(), mSendBackTo);
                        if (mSaveImageLocally) {
                            final Bitmap bitmap2 = Image_Helper.createBMPfromJPG(pout);
                            Bitmap rotatedBmp = Image_Helper.rotateImage(bitmap2, bitmap2.getWidth(), bitmap2.getHeight(), Preference.getFPVActivityRotation(null));
                            File savedImageFile = FileHelper.savePic(rotatedBmp, null, App.KMLFile.getImageFolder());
                            if (savedImageFile == null) {
                                // sendMessageToModule error messages to GCS Please
                                bitmap.recycle();
                                rotatedBmp.recycle();
                                return;
                            }

                            Image_Helper.AddGPStoJpg(savedImageFile.getAbsolutePath(), AndruavSettings.andruavWe7daBase.getAvailableLocation());


                            Event_FPV_Image event_fpv_image = new Event_FPV_Image();
                            event_fpv_image.isLocalImage = true;
                            event_fpv_image.isVideo = false;
                            event_fpv_image.ImageFile = savedImageFile;
                            event_fpv_image.ImageLocation = AndruavSettings.andruavWe7daBase.getAvailableLocation();
                            event_fpv_image.Description = "Image No#" + mTakeImageCount;
                            EventBus.getDefault().post(event_fpv_image);

                            bitmap.recycle();
                            rotatedBmp.recycle();

                        }
                    } catch (Exception ex) {
                        AndruavEngine.log().logException(AndruavSettings.AccessCode, "exception_fpv2", ex);
                        PanicFacade.cannotStartCamera(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_CAMERA, App.getAppContext().getString(com.andruav.protocol.R.string.andruav_error_camertakeimage), null);
                        skip = false;
                    }

                    skip = false;
                }

                mSurfaceViewRenderer.clearImage();
            }
        }

    };


    @Override
    public void onFrame(final byte[] frame, final int offset, final int size) {
        if ((mcameraRecorder!= null) && (mcameraRecorder.isRecording())) {
            // condition is replicated to avoid post runnable without need, and then void calling null object when stop recording.
            mHandle.post(() -> {
                if (mRecordVideo) {


                    mcameraRecorder.encodeFeed(ByteBuffer.wrap(frame, offset, size));
                }
            });
        }
    }


    long lastTimeFrame = 0;
    @Override
    public void onFrame(VideoFrame videoFrame) {
        frameHeight = videoFrame.getRotatedHeight();
        frameWidth = videoFrame.getRotatedWidth();
        if (mRecordVideo) {
            if (MediaVideoEncoder.VIDEO_FORMAT== MediaVideoEncoder.MOBILE_WORK_FOR_ALL)
            {
                if (mVideoFileRenderer != null) {
                    long now = System.currentTimeMillis();
                    if ((now - lastTimeFrame) > 50) {
                        lastTimeFrame = now;
                        mVideoFileRenderer.onFrame(videoFrame);
                    }
                }
                mVideoFileRenderer.onFrame(videoFrame);
            }
            else {
                mVideoByteRenderer.onFrame(videoFrame);
            }
        }


    }

}
