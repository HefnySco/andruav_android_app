package ap.andruavmiddlelibrary.webrtc.classes;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.andruav.AndruavDroneFacade;
import com.andruav.AndruavEngine;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.CapturerObserver;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.GlRectDrawer;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;


import com.andruav.FeatureSwitch;
import ap.andruavmiddlelibrary.factory.LooperExecutor;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.webrtc.IRTCListener;
import ap.andruavmiddlelibrary.factory.communication.NetInfoAdapter;

import com.andruav.AndruavSettings;


/**
 * Created by mhefny on 2/28/16.
 */
public class PeerConnectionManager implements CameraVideoCapturer.CameraEventsHandler, CapturerObserver {

    protected PeerConnectionManager Me;
    // Camera ID for Android
    public static String CameraID;
    private PeerConnectionFactory pcFactory;
    private PnRTC_3ameel pnRTC3ameel;
    private VideoCapturer capturer;

    private VideoSink mExternalVideoSink;
    private VideoSource localVideoSource;
    private boolean     videoSourceStopped = true;
    private VideoTrack localVideoTrack;
    private MediaStream mediaStream;

    private SurfaceTextureHelper mVideoCapturerSurfaceTextureHelper;
    private SurfaceViewRenderer mSurfaceViewRenderer;
    private EglBase eglBaseTX;
    private Surface mSurfaceTX;

    private int mRotationGCS=0;

    private DefaultVideoEncoderFactory mDefaultVideoEncoderFactory;

    // private Handler mhandler;
    private LooperExecutor executor;

    private boolean mDisplayLocal;
    private Context mContext;

    private boolean connected ;
    public static final String LOCAL_MEDIA_STREAM_ID = "localStreamPN";

    private MediaStream mRemoteStream;

    private final Object _waitClose = new Object();

    public EglBase.Context getEglBaseContext ()
    {
        return eglBaseTX.getEglBaseContext();
    }

    public void rotateNext()
    {
        if (!AndruavSettings.andruavWe7daBase.getIsCGS()) return ;

        mRotationGCS = (mRotationGCS + 90) % 360;
    }

    public void setRotationGCS (final int rotation)
    {
        mRotationGCS = rotation;
    }
    public void doMirror(final boolean mirror)
    {
        if (!AndruavSettings.andruavWe7daBase.getIsCGS()) return ;
    }

    public boolean hasActivePeers ()
    {
        return pnRTC3ameel.hasActivePeers();
    }

    public EglBase.Context getEglBaseContextTX ()
    {
        return eglBaseTX.getEglBaseContext();
    }

    public Surface getSurfaceTX()
    {
        if (mSurfaceTX == null)
        {
            mSurfaceTX = new Surface(mVideoCapturerSurfaceTextureHelper.getSurfaceTexture());
        }
        return mSurfaceTX;
    }


    public DefaultVideoEncoderFactory getDefaultVideoEncoderFactory()
    {
        return mDefaultVideoEncoderFactory;
    }

    private void initHandler ()
    {
        killHandler();

        executor = new LooperExecutor();

    }


    public boolean init (final Context context, final IRTCListener irtcListener, final VideoSink externalVideoSink , final SurfaceViewRenderer surfaceViewRenderer, final boolean displayLocal, final String channelName)
    {

        try {
            Me = this;
            mDisplayLocal = displayLocal;
            mExternalVideoSink = externalVideoSink;
            mContext = context;

            if (AndruavSettings.andruavWe7daBase.getIsCGS())
            {
                AndruavSettings.videoCameraRotationDegree = Preference.getFPVActivityRotation(null);
            }
            else
            {
                AndruavSettings.videoCameraRotationDegree = 0;
            }

            initHandler();

            eglBaseTX = EglBase.create(null,EglBase.CONFIG_PLAIN);



            if ((irtcListener == null) || (surfaceViewRenderer == null)) return false;
            connected = false;
            mSurfaceViewRenderer = surfaceViewRenderer;
            mSurfaceViewRenderer.init(eglBaseTX.getEglBaseContext(),null,EglBase.CONFIG_PLAIN,new GlRectDrawer());
            mSurfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

            mVideoCapturerSurfaceTextureHelper =
                    SurfaceTextureHelper.create("CVTTX", eglBaseTX.getEglBaseContext());
            createPeerConnectionFactoryInternal (context);

            CameraID = channelName;
            pnRTC3ameel = new PnRTC_3ameel(pcFactory);


            if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
                // Returns the number of cams & front/back face device name
                capturer = createVideoCapturer();


                if (capturer == null) {

                    return false;
                }

                capturer.initialize(mVideoCapturerSurfaceTextureHelper, context, this);

                // First create a Video Source, then we can make a Video Track

                localVideoSource = pcFactory.createVideoSource(false); //capturer);

                capturer.initialize(mVideoCapturerSurfaceTextureHelper, context, localVideoSource.getCapturerObserver());

                if (Preference.useStreamVideoHD(null)) {
                    capturer.startCapture(1280, 720, 15);
                }
                else
                {
                    capturer.startCapture(640, 480, 10);
                }
            }

            // We start out with an emptly MediaStream object, created with help from our PeerConnectionFactory
            //  Note that LOCAL_MEDIA_STREAM_ID can be any string
            mediaStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID+ System.currentTimeMillis());

            if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
                // Now we can add our tracks.
                localVideoTrack = pcFactory.createVideoTrack(AndruavSettings.andruavWe7daBase.PartyID, localVideoSource);
                mediaStream.addTrack(localVideoTrack);
               localVideoTrack.addSink(videoFrame -> {
//                    VideoFrame outVideoFrame = new VideoFrame(
//                            videoFrame.getBuffer(),
//                            90, videoFrame.getTimestampNs());
                    // called in Drone Mode ---- Transmitting

                    mSurfaceViewRenderer.onFrame(videoFrame);
                    mExternalVideoSink.onFrame(videoFrame);
                });
            }
            // First attach the RTC Listener so that callback events will be triggered
            pnRTC3ameel.attachRTCListener(new AndruavRTCListener2(irtcListener));

            // Then attach your local media stream to the PnRTC_3ameel.
            //  This will trigger the onLocalStream callback.
            this.pnRTC3ameel.attachLocalMediaStream(mediaStream);

            return  true;
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException("rtc-init", ex);

            return false;
        }

    }


    private VideoCapturer createVideoCapturer() {


        CameraEnumerator enumerator;
        String deviceName;



        if (Camera2Enumerator.isSupported(mContext)) {
            enumerator = new Camera2Enumerator(mContext);
        }
        else {
            enumerator = new Camera1Enumerator();
        }

        if(enumerator.getDeviceNames().length == 1) {
            deviceName = enumerator.getDeviceNames()[0];
        }
        else {
            final int camNum = Preference.getCameraNumber(null);
            deviceName = enumerator.getDeviceNames()[camNum];
        }



        return enumerator.createCapturer(deviceName, this);
    }

    /***
     * switch between available cameras
     */
    public void switchCamera () {

        Preference.setCameraNumber(null,(Preference.getCameraNumber(null) + 1) % 2);
        ((CameraVideoCapturer) capturer).switchCamera(null);

    }


    public void setFlash (final int onOff)
    {
        ((CameraVideoCapturer) capturer).setFlash(onOff);
    }

    public int getFlash ()
    {
        return ((CameraVideoCapturer) capturer).getFlash();
    }

    public void setZoom (final float zoom)
    {
        ((CameraVideoCapturer) capturer).setZoom(zoom);
    }

    public float getZoom ()
    {
        return ((CameraVideoCapturer) capturer).getZoom();
    }

    public boolean isZoomSupported ()
    {
        return ((CameraVideoCapturer) capturer).isZoomSupported();
    }


    private void killHandler()
    {
       /* if (mhandler== null) return;

        mhandler.removeCallbacksAndMessages(null);
        mhandler= null;
*/
        if (executor!= null)
        {
            try {
                executor.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    public void onDestroy ()
    {
        killHandler();

        if (localVideoSource!=null) {
            localVideoSource.dispose();
            localVideoSource = null;
        }

        if (this.pnRTC3ameel != null) {
            this.pnRTC3ameel.onDestroy();
        }
    }

    public void onPause() {
        if (mSurfaceViewRenderer==null) return ;

        try {
            disconnectToDrone(null,null);

            if (mediaStream!= null)
            {
                if (mediaStream.videoTracks.size() > 0) {

                    mediaStream.removeTrack(localVideoTrack);
                    if (localVideoTrack != null) localVideoTrack.dispose();
                    //mediaStream.dispose();
                    //mediaStream = null;
                    //localVideoTrack.dispose();//.removeRenderer(localVideoSource);
                    mediaStream = null;
                }
            }

            stopLocalVideoSource();

        }
        catch (Exception e)
        {
            AndruavEngine.log().logException("rtc", e);
        }
    }


    public void onResume() {

        if (localVideoSource==null) return;
        if (mSurfaceViewRenderer==null) return;

        startLocalVideoSource();

    }

    public void joinToDrone (final String unitID, final String channel)
    {
        try
        {
            if (FeatureSwitch.IGNORE_NO_INTERNET_CONNECTION==false) {
                NetInfoAdapter.Update();
                if ((connected = false)
                        || (!NetInfoAdapter.isHasValidIPAddress())
                        || (pnRTC3ameel == null)
                        )

                {
                    return;
                }

            }

            pnRTC3ameel.joinStream(unitID,channel);
        }
        catch (Exception e)
        {
            AndruavEngine.log().logException("rtc", e);
        }
    }



    public void disconnectToDrone (final String unitID, final String channel)
    {
        try
        {
            if (FeatureSwitch.IGNORE_NO_INTERNET_CONNECTION==false)
            {

                if ((connected = false)
                        || (!NetInfoAdapter.isHasValidIPAddress())
                        || (pnRTC3ameel == null)
                        ) {
                    return;
                }
            }
            if (unitID != null)
            {
                pnRTC3ameel.closeConnection(unitID,channel);
            }
            else
            {
                pnRTC3ameel.closeAllConnections();
            }
        }
        catch (Exception e)
        {
            AndruavEngine.log().logException("rtc", e);
        }
    }



    private void createPeerConnectionFactoryInternal(Context context) {

        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                //.setEnableVideoHwAcceleration(true)
                .createInitializationOptions();

        PeerConnectionFactory.initialize(initializationOptions);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        mDefaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                eglBaseTX.getEglBaseContext(), true,true);

        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(eglBaseTX.getEglBaseContext());
        //VideoDecoder vd = defaultVideoDecoderFactory.createDecoder(defaultVideoDecoderFactory.getSupportedCodecs()[1]);

        pcFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .setVideoEncoderFactory(mDefaultVideoEncoderFactory)
                .createPeerConnectionFactory();
    }


    private void stopLocalVideoSource() {
        try {
            if (capturer != null)
            {
                capturer.stopCapture();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Handler().post(() -> {
            if (localVideoSource != null && videoSourceStopped) {
                localVideoSource.dispose();
                videoSourceStopped = true;
            }
            if (capturer != null)
            {
                capturer.dispose();
            }

            if (mSurfaceViewRenderer != null) {
                mSurfaceViewRenderer.release();
            }
            // dont uncomment
            /*
            if (localAudioSource != null && !audioSourceStopped) {
                localAudioSource.dispose();
                audioSourceStopped = true;
            }
            */

            synchronized (_waitClose)
            {
                _waitClose.notifyAll();
            }
        });

        try {
            synchronized (_waitClose) {
                _waitClose.wait(2000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.i("w","www");
    }

    private void startLocalVideoSource() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (localVideoSource != null && videoSourceStopped) {
                    videoSourceStopped = false;
                }
            }
        });
    }



    ///////////////////////  Begin CameraEventsHandler
    @Override
    public void onCameraError(String s) {

    }

    @Override
    public void onCameraDisconnected() {

    }

    @Override
    public void onCameraFreezed(String s) {

    }

    @Override
    public void onCameraOpening(String s) {

    }


    @Override
    public void onFirstFrameAvailable() {
    }

    @Override
    public void onCameraClosed() {

    }

    @Override
    public void onCameraFlashChanged(int flashOn) {
        AndruavDroneFacade.sendCameraFlashStatus (AndruavSettings.andruavWe7daBase.PartyID, flashOn, null);
    }

    @Override
    public void onCameraZoomChanged(float zoom) {
        AndruavDroneFacade.sendCameraZoomStatus(AndruavSettings.andruavWe7daBase.PartyID, zoom, null);

    }

    @Override
    public void onCapturerStarted(boolean b) {

    }

    @Override
    public void onCapturerStopped() {

    }

    @Override
    public void onFrameCaptured(VideoFrame videoFrame) {
        // called in Drone Mode - Transmitting
        mSurfaceViewRenderer.onFrame(videoFrame);
        mExternalVideoSink.onFrame(videoFrame);
    }

    //////////////////////////////////? EOF CameraEventsHandler

    /**
     * Created by mhefny on 2/26/16.
     */
    private class AndruavRTCListener2 extends PnRTCListener {


        IRTCListener mIRTCListener;

        AndruavRTCListener2(final IRTCListener irtcListener)
        {
            super();

            mIRTCListener = irtcListener;

        }


        @Override
        public void onConnected(final String userId)
        {
            mIRTCListener.onPeerConnected(userId);
        }

        @Override
        public void onPeerStatusChanged(final PnPeer peer)
        {
            // called when a remote closes connection with this Drone
            if (peer.status.equals(PnPeer.STATUS_CONNECTED))
            {
                mIRTCListener.onPeerConnected(peer.id);
            }

        }

        @Override
        public void  onCallReady(final String callId)
        {
            Log.d("fpvstream", "onCallReady");

        }

        @Override
        public void onLocalStream(final MediaStream localStream) {


            Log.d("fpvstream","onLocalStream");

            if (mDisplayLocal)
            {
                mIRTCListener.onLocalStream(localStream);
            }
            else {
               /* mhandler.post(new Runnable() {
                    @Override
                    public void run() {


                    }
                }); */


                mIRTCListener.onLocalStream(localStream);
            }

        }

        @Override
        public void onAddRemoteStream(final MediaStream remoteStream, final PnPeer peer) {

            try
            {
                Log.d("fpvstream", "onAddRemoteStream");

                connected = true;

                mRemoteStream = remoteStream;

                // TODO: WEBRTC fix This
                if (remoteStream.videoTracks.size() == 0)
                    return;
                remoteStream.videoTracks.get(0).setEnabled(true);
                remoteStream.videoTracks.get(0).addSink(videoFrame -> {
                    // called in GCS - Receiving
                    VideoFrame outVideoFrame = new VideoFrame(
                            videoFrame.getBuffer(),
                            mRotationGCS, videoFrame.getTimestampNs());
                            // called in Drone Mode ---- Transmitting

                            mSurfaceViewRenderer.onFrame(outVideoFrame);
                    mExternalVideoSink.onFrame(outVideoFrame);
                });
                mIRTCListener.onAddRemoteStream(remoteStream, peer);

            }
            catch (Exception e)
            {
                AndruavEngine.log().logException("rtc", e);
            }
        }

        @Override
        public void onRemoveRemoteStream(final MediaStream remoteStream, final PnPeer peer) {
            try
            {
                // Handle remote stream added
                Log.d("fpvstream", "onRemoveRemoteStream");
                mIRTCListener.onRemoveRemoteStream(remoteStream, peer);
            }
            catch (Exception e)
            {
                AndruavEngine.log().logException("rtc", e);
            }
        }

        @Override
        public void onMessage(PnPeer peer, Object message) {
            /// Handle Message

        }

        @Override
        public void onPeerConnectionClosed(PnPeer peer) {
            try
            {
                // Quit back to MainActivity
                mIRTCListener.onPeerConnectionClosed(peer);
            }
            catch (Exception e)
            {
                AndruavEngine.log().logException("rtc", e);
            }
        }

        public void onDebug(PnRTCResala message){

        }
    }


}