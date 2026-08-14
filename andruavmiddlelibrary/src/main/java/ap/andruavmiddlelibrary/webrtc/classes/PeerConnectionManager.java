package ap.andruavmiddlelibrary.webrtc.classes;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import java.util.List;

import com.andruav.AndruavDroneFacade;
import com.andruav.AndruavEngine;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerationAndroid;
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
    private int mCaptureWidth = 1280;
    private int mCaptureHeight = 720;
    private int mCaptureFps = 15;

    private VideoSink mExternalVideoSink;
    /**
     * The transient local-preview sink (backed by {@link #mSurfaceViewRenderer}). Unlike
     * {@code mExternalVideoSink}, this may be null (no Activity currently attached) without
     * affecting capture/publish, which keeps running via the single sink added in {@link #init}.
     */
    private VideoSink mLocalPreviewSink;
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
        if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
        }
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



            if (irtcListener == null) return false;
            connected = false;
            // surfaceViewRenderer may be null: capture/publish must be able to start with no
            // local preview attached (e.g. when initialized from a Service). A renderer can be
            // attached later via attachLocalRenderer().
            if (surfaceViewRenderer != null) {
                attachLocalRenderer(surfaceViewRenderer);
            }

            mVideoCapturerSurfaceTextureHelper =
                    SurfaceTextureHelper.create("CVTTX", eglBaseTX.getEglBaseContext());
            createPeerConnectionFactoryInternal (context);

            CameraID = channelName;

            if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
                // The capturing phone is rigidly mounted (e.g. on a drone) and always forced to
                // landscape - it never physically rotates. Freeze the capture rotation to match,
                // instead of letting WebRTC query the live WindowManager rotation, which flips to
                // portrait while this Activity is minimized into Picture-in-Picture (its window
                // briefly stops being the system's "foreground orientation owner"), visibly
                // rotating both the local preview and the outgoing stream by 90 degrees.
                org.webrtc.AndruavWebRTCGlobals.fixedDeviceOrientationDegrees = 90;

                // Returns the number of cams & front/back face device name
                capturer = createVideoCapturer();


                if (capturer == null) {

                    return false;
                }

                capturer.initialize(mVideoCapturerSurfaceTextureHelper, context, this);

                // First create a Video Source, then we can make a Video Track

                localVideoSource = pcFactory.createVideoSource(false); //capturer);

                capturer.initialize(mVideoCapturerSurfaceTextureHelper, context, localVideoSource.getCapturerObserver());

                capturer.startCapture(mCaptureWidth, mCaptureHeight, mCaptureFps);
            }

            // We start out with an emptly MediaStream object, created with help from our PeerConnectionFactory
            //  Note that LOCAL_MEDIA_STREAM_ID can be any string
            mediaStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID+ System.currentTimeMillis());

            if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
                // Now we can add our tracks.
                localVideoTrack = pcFactory.createVideoTrack(AndruavSettings.andruavWe7daBase.PartyID, localVideoSource);
                mediaStream.addTrack(localVideoTrack);
                // Added exactly once for the life of the track: local preview attach/detach only
                // ever changes mLocalPreviewSink, it never touches this sink registration, so
                // capture/publish keeps running whether or not a renderer is currently attached.
               localVideoTrack.addSink(videoFrame -> {
                    if (mLocalPreviewSink != null) mLocalPreviewSink.onFrame(videoFrame);
                    mExternalVideoSink.onFrame(videoFrame);
                });
            }

            // Construct PnRTC_3ameel (and with it, the incoming-signaling/EventBus listener) only
            // now that mediaStream/localVideoTrack are fully built. PnRTC_3ameel's constructor makes
            // the signaling channel live immediately, so building it any earlier left a window where
            // an incoming "joinme" could create a PnPeer (see PnPeer's ctor: pc.addStream(getLocalMediaStream()))
            // before attachLocalMediaStream() below ever ran - producing a trackless offer that
            // required the viewer to disconnect and rejoin to get a working video track.
            pnRTC3ameel = new PnRTC_3ameel(pcFactory);

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


    /**
     * Attaches a local-preview renderer without touching capture/publish state. Safe to call
     * repeatedly (e.g. each time an Activity resumes) and safe to call while capture is running.
     */
    public void attachLocalRenderer(final SurfaceViewRenderer renderer) {
        if (renderer == null) return;
        if (mSurfaceViewRenderer == renderer) return;

        mSurfaceViewRenderer = renderer;
        mSurfaceViewRenderer.init(eglBaseTX.getEglBaseContext(), null, EglBase.CONFIG_PLAIN, new GlRectDrawer());
        mSurfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        mLocalPreviewSink = mSurfaceViewRenderer;
    }

    /**
     * Detaches and releases the local-preview renderer only. Does NOT stop the capturer, the
     * video source/track, or any peer connection - those keep running so a backgrounded/screen-off
     * Activity never interrupts the stream. Call {@link #stopStreaming()} for real teardown.
     */
    public void detachLocalRenderer() {
        mLocalPreviewSink = null;
        if (mSurfaceViewRenderer != null) {
            mSurfaceViewRenderer.release();
            mSurfaceViewRenderer = null;
        }
    }

    private CameraEnumerator createCameraEnumerator() {
        if (Camera2Enumerator.isSupported(mContext)) {
            return new Camera2Enumerator(mContext);
        }

        return new Camera1Enumerator();
    }

    private VideoCapturer createVideoCapturer() {

        final CameraEnumerator enumerator = createCameraEnumerator();
        String deviceName;

        if(enumerator.getDeviceNames().length == 1) {
            deviceName = enumerator.getDeviceNames()[0];
        }
        else {
            final int camNum = Preference.getCameraNumber(null);
            deviceName = enumerator.getDeviceNames()[camNum];
        }

        resolveCaptureFormat(enumerator, deviceName);

        return enumerator.createCapturer(deviceName, this);
    }

    /***
     * Resolves the actual capture width/height/fps to use for the selected camera, based on the
     * per-facing (front/back) resolution preference, and stores it in mCaptureWidth/Height/Fps
     * for {@link #init} to pass to VideoCapturer.startCapture().
     */
    private void resolveCaptureFormat(final CameraEnumerator enumerator, final String deviceName) {
        final boolean isFront = enumerator.isFrontFacing(deviceName);
        final int mode = isFront ? Preference.getStreamResolutionModeFront(null) : Preference.getStreamResolutionModeBack(null);

        if (mode == Preference.STREAM_RESOLUTION_MAX) {
            final List<CameraEnumerationAndroid.CaptureFormat> formats = enumerator.getSupportedFormats(deviceName);
            if (formats != null && !formats.isEmpty()) {
                CameraEnumerationAndroid.CaptureFormat best = formats.get(0);
                for (final CameraEnumerationAndroid.CaptureFormat format : formats) {
                    if ((long) format.width * format.height > (long) best.width * best.height) {
                        best = format;
                    }
                }
                mCaptureWidth = best.width;
                mCaptureHeight = best.height;
                // framerate.max is expressed in fps*1000; cap it to a sane streaming range.
                mCaptureFps = Math.max(10, Math.min(30, best.framerate.max / 1000));
                return;
            }
            // No format info available: fall through to HD below as a safe default.
        }

        if (mode == Preference.STREAM_RESOLUTION_SD) {
            mCaptureWidth = 640;
            mCaptureHeight = 480;
            mCaptureFps = 10;
        }
        else {
            mCaptureWidth = 1280;
            mCaptureHeight = 720;
            mCaptureFps = 15;
        }
    }

    /***
     * switch between available cameras
     */
    public void switchCamera () {

        final int deviceCount = createCameraEnumerator().getDeviceNames().length;
        if (deviceCount <= 1) return;

        Preference.setCameraNumber(null,(Preference.getCameraNumber(null) + 1) % deviceCount);
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

    /**
     * Real, full teardown of capture + peer connections - disconnects all peers, disposes the
     * local video track/source, and stops the capturer. Call only when genuinely told to stop
     * streaming (explicit UI stop, or the remote stop command) - never on a mere Activity pause,
     * since that would kill the stream on every screen-off.
     */
    public void stopStreaming() {
        if (pnRTC3ameel == null) return ;

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

        startLocalVideoSource();

    }

    public void joinToDrone (final String unitID, final String channel)
    {
        try
        {
            if (!FeatureSwitch.IGNORE_NO_INTERNET_CONNECTION) {
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
            if (!FeatureSwitch.IGNORE_NO_INTERNET_CONNECTION)
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

            // Renderer lifecycle is owned exclusively by attachLocalRenderer()/detachLocalRenderer()
            // now - capture teardown must not touch it.
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
        if (mLocalPreviewSink != null) mLocalPreviewSink.onFrame(videoFrame);
        mExternalVideoSink.onFrame(videoFrame);
    }

    //////////////////////////////////? EOF CameraEventsHandler

    /**
     * Created by mhefny on 2/26/16.
     */
    private class AndruavRTCListener2 extends PnRTCListener {


        final IRTCListener mIRTCListener;

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

                            if (mLocalPreviewSink != null) mLocalPreviewSink.onFrame(outVideoFrame);
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