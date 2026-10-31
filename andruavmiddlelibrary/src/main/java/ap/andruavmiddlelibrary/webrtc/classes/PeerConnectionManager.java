package ap.andruavmiddlelibrary.webrtc.classes;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.webrtc.RTCStats;
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
import ap.andruavmiddlelibrary.webrtc.camera.AndruavCameraCapturer;
import ap.andruavmiddlelibrary.webrtc.camera.AndruavCameraControl;
import ap.andruavmiddlelibrary.factory.communication.NetInfoAdapter;

import com.andruav.AndruavSettings;


/**
 * Created by mhefny on 2/28/16.
 */
public class PeerConnectionManager implements CameraVideoCapturer.CameraEventsHandler,
        AndruavCameraControl.Listener, CapturerObserver {

    protected PeerConnectionManager Me;
    /**
     * Wire channel of this unit's video stream - the camera entry's "id"
     * the GCS dials (the B7 camera channel key in native mode, the PartyID
     * on the legacy Java list). NOT the peer identity: outbound signaling
     * keeps PartyID in the "number" field.
     */
    public static String CameraID;
    private PeerConnectionFactory pcFactory;
    private PnRTC_3ameel pnRTC3ameel;
    private VideoCapturer capturer;
    private int mCaptureWidth = 1280;
    private int mCaptureHeight = 720;
    private int mCaptureFps = 15;
    private boolean mCapturing;
    private SharedPreferences.OnSharedPreferenceChangeListener mStreamResolutionPrefListener;

    // Weak-link quality stepping (AN-0099): poll connected peers'
    // availableOutgoingBitrate; step the capture format down while it stays
    // low, back up toward the configured resolution mode when the link
    // recovers. Bitrate units are bps; ~500k is the floor below which even
    // the SD rung is unlikely to stay smooth.
    private static final long STATS_POLL_MS = 2_000;
    private static final long STEP_DOWN_AFTER_MS = 6_000;
    private static final long STEP_UP_AFTER_MS = 20_000;
    private static final double LOW_OUTGOING_BITRATE_BPS = 500_000.0;
    private final List<PnPeer> mStatsPeers = new ArrayList<>();
    private final Map<PnPeer, Double> mPeerBitrateBps = new HashMap<>();
    private int mConfiguredResolutionMode = Preference.STREAM_RESOLUTION_HD;
    private int mQualityStep = Preference.STREAM_RESOLUTION_HD;
    private long mLowBitrateSinceMs;
    private long mHealthyBitrateSinceMs;
    private boolean mStatsPolling;
    private Handler mStatsHandler;
    /** Enumerator device name + facing of the camera currently captured -
     *  tracked so the camera channel surface can name the active channel. */
    private String mCaptureDeviceName;
    private boolean mCaptureFrontFacing;

    /** Per-stream-session baseline metrics, one line per session under this
     *  logcat tag (AN-0077). Written/read across the executor, camera and
     *  signaling threads, hence volatile. */
    private static final String TAG_CAM_METRICS = "AndruavCamMetrics";
    private volatile long mCamSessionStartMs;
    private volatile boolean mCamFirstFrameLogged;
    private volatile int mCamFreezeCount;

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

    public void setRotationGCS (final int rotation)
    {
        mRotationGCS = rotation;
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

            AndruavSettings.videoCameraRotationDegree = 0;

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

            {
                // The capturing phone is rigidly mounted (e.g. on a drone) and always forced to
                // landscape - it never physically rotates. Freeze the capture rotation to match,
                // instead of letting WebRTC query the live WindowManager rotation, which flips to
                // portrait while this Activity is minimized into Picture-in-Picture (its window
                // briefly stops being the system's "foreground orientation owner"), visibly
                // rotating both the local preview and the outgoing stream by 90 degrees.

                // Returns the number of cams & front/back face device name
                capturer = createVideoCapturer();

                if (capturer == null) {
                    return false;
                }

                if (capturer instanceof AndruavCameraControl) {
                    ((AndruavCameraControl) capturer).setFixedOrientationDegrees(90);
                } else {
                    org.webrtc.AndruavWebRTCGlobals.fixedDeviceOrientationDegrees = 90;
                }

                // First create a Video Source, then we can make a Video Track
                localVideoSource = pcFactory.createVideoSource(false);

                capturer.initialize(mVideoCapturerSurfaceTextureHelper, context, localVideoSource.getCapturerObserver());
                mCamSessionStartMs = SystemClock.elapsedRealtime();
                mCamFirstFrameLogged = false;
                mCamFreezeCount = 0;
                capturer.startCapture(mCaptureWidth, mCaptureHeight, mCaptureFps);
                mCapturing = true;
            }

            // We start out with an emptly MediaStream object, created with help from our PeerConnectionFactory
            //  Note that LOCAL_MEDIA_STREAM_ID can be any string
            mediaStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID+ System.currentTimeMillis());

            {
                // Now we can add our tracks.
                // The track id must equal the published camera entry's "id" -
                // the channel the GCS dials (webclient drops received tracks
                // whose id differs from the dialed one on Chrome).
                localVideoTrack = pcFactory.createVideoTrack(channelName, localVideoSource);
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

            mStreamResolutionPrefListener = (prefs, key) -> {
                if ("streamResModeBack".equals(key) || "streamResModeFront".equals(key)) {
                    onStreamResolutionModeChanged();
                }
            };
            PreferenceManager.getDefaultSharedPreferences(context)
                    .registerOnSharedPreferenceChangeListener(mStreamResolutionPrefListener);

            mStatsHandler = new Handler(Looper.getMainLooper());

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
        try {
            mSurfaceViewRenderer.init(eglBaseTX.getEglBaseContext(), null, EglBase.CONFIG_PLAIN, new GlRectDrawer());
        } catch (IllegalStateException e) {
            // Defense in depth: the guard above only catches "same renderer, same
            // PeerConnectionManager instance". A *different* PeerConnectionManager instance handed
            // the same still-initialized View (e.g. a capture-source switch that stopped one
            // instance and started a fresh one without the Activity/View being recreated) throws
            // "Already initialized" here instead. Log rather than crash - the renderer already has
            // a live render thread and can keep being used.
            AndruavEngine.log().logException("rtc-renderer", e);
        }
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
        mCaptureDeviceName = deviceName;
        mCaptureFrontFacing = enumerator.isFrontFacing(deviceName);

        if (Preference.useAppCameraCapturer(null)) {
            // The app capturer takes a camera2 id. Camera2Enumerator device
            // names already are ids; Camera1Enumerator display names are not,
            // so on a Camera1-only device let the capturer auto-pick instead.
            final String cameraId = (enumerator instanceof Camera2Enumerator) ? deviceName : null;
            return new AndruavCameraCapturer(mContext, cameraId, this);
        }

        return enumerator.createCapturer(deviceName, this);
    }

    /***
     * Resolves the actual capture width/height/fps to use for the selected camera, based on the
     * per-facing (front/back) resolution preference, and stores it in mCaptureWidth/Height/Fps
     * for {@link #init} to pass to VideoCapturer.startCapture().
     */
    private void resolveCaptureFormat(final CameraEnumerator enumerator, final String deviceName) {
        resolveCaptureFormat(enumerator, deviceName, enumerator.isFrontFacing(deviceName));
    }

    private void resolveCaptureFormat(final CameraEnumerator enumerator, final String deviceName, final boolean isFront) {
        final int mode = isFront ? Preference.getStreamResolutionModeFront(null) : Preference.getStreamResolutionModeBack(null);
        // The configured mode is the ceiling the weak-link stepper (AN-0099)
        // may climb back to; any (re)resolution resets the step to it.
        mConfiguredResolutionMode = mode;
        mQualityStep = mode;
        applyCaptureFormatMode(enumerator, deviceName, mode);
    }

    private void applyCaptureFormatMode(final CameraEnumerator enumerator, final String deviceName, final int mode) {
        if (mode == Preference.STREAM_RESOLUTION_MAX) {
            if (Preference.useAppCameraCapturer(null)) {
                // App path: formats come from the Camera2 stream map, not the
                // fork enumerator's supported-formats list.
                final String cameraId = (enumerator instanceof Camera2Enumerator) ? deviceName : null;
                final int[] maxFormat = AndruavCameraCapturer.maxCaptureFormat(mContext, cameraId);
                if (maxFormat != null) {
                    mCaptureWidth = maxFormat[0];
                    mCaptureHeight = maxFormat[1];
                    mCaptureFps = Math.max(10, Math.min(30, maxFormat[2]));
                    Log.i(TAG_CAM_METRICS,
                            "capture " + mCaptureWidth + "x" + mCaptureHeight + "@" + mCaptureFps);
                    return;
                }
                // No format info available: fall through to HD below as a safe default.
            } else {
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
        Log.i(TAG_CAM_METRICS, "capture " + mCaptureWidth + "x" + mCaptureHeight + "@" + mCaptureFps);
    }

    /**
     * Re-resolves the camera-resolution preference and applies it to the running
     * capturer via {@link VideoCapturer#changeCaptureFormat}. The peer connection
     * is untouched - the same video track keeps sending, the encoder just picks
     * up the new frame size, so the viewer's received size changes without a
     * reconnect.
     */
    private void onStreamResolutionModeChanged() {
        if (capturer == null || !mCapturing) {
            return;
        }
        try {
            final int oldWidth = mCaptureWidth;
            final int oldHeight = mCaptureHeight;
            final int oldFps = mCaptureFps;
            // mCaptureDeviceName may be null after a blind camera switch; the
            // tracked facing still selects the right per-facing preference.
            resolveCaptureFormat(createCameraEnumerator(), mCaptureDeviceName, mCaptureFrontFacing);
            if (mCaptureWidth == oldWidth && mCaptureHeight == oldHeight && mCaptureFps == oldFps) {
                return;
            }
            capturer.changeCaptureFormat(mCaptureWidth, mCaptureHeight, mCaptureFps);
        } catch (Exception e) {
            AndruavEngine.log().logException("rtc", e);
        }
    }

    // ---- weak-link quality stepping (AN-0099) ---------------------------------

    private void trackStatsPeer(final PnPeer peer) {
        synchronized (mStatsPeers) {
            if (!mStatsPeers.contains(peer)) {
                mStatsPeers.add(peer);
            }
        }
        if (mStatsHandler != null && !mStatsPolling) {
            mStatsPolling = true;
            mStatsHandler.postDelayed(mStatsPoller, STATS_POLL_MS);
        }
    }

    private void untrackStatsPeer(final PnPeer peer) {
        synchronized (mStatsPeers) {
            mStatsPeers.remove(peer);
            mPeerBitrateBps.remove(peer);
        }
    }

    private void resetLinkQualityState() {
        synchronized (mStatsPeers) {
            mStatsPeers.clear();
            mPeerBitrateBps.clear();
        }
        mLowBitrateSinceMs = 0;
        mHealthyBitrateSinceMs = 0;
        if (mStatsHandler != null) {
            mStatsHandler.removeCallbacks(mStatsPoller);
        }
        mStatsPolling = false;
    }

    private final Runnable mStatsPoller = new Runnable() {
        @Override
        public void run() {
            final List<PnPeer> peers;
            synchronized (mStatsPeers) {
                peers = new ArrayList<>(mStatsPeers);
            }
            for (final PnPeer peer : peers) {
                try {
                    peer.getPc().getStats(report -> {
                        synchronized (mStatsPeers) {
                            if (!mStatsPeers.contains(peer)) return;
                            mPeerBitrateBps.put(peer, outgoingBitrateBps(report));
                        }
                        evaluateLinkQuality();
                    });
                } catch (Exception e) {
                    // A closing peer can throw on getStats - drop it from tracking.
                    untrackStatsPeer(peer);
                }
            }
            synchronized (mStatsPeers) {
                if (mCapturing && !mStatsPeers.isEmpty()) {
                    mStatsHandler.postDelayed(this, STATS_POLL_MS);
                } else {
                    mStatsPolling = false;
                }
            }
        }
    };

    /** Min availableOutgoingBitrate (bps) across nominated candidate-pairs in one report. */
    private static double outgoingBitrateBps(final org.webrtc.RTCStatsReport report) {
        double best = -1;
        for (final RTCStats stats : report.getStatsMap().values()) {
            if (!"candidate-pair".equals(stats.getType())) continue;
            final Object nominated = stats.getMembers().get("nominated");
            if (nominated instanceof Boolean && !((Boolean) nominated)) continue;
            final Object bitrate = stats.getMembers().get("availableOutgoingBitrate");
            if (!(bitrate instanceof Number)) continue;
            final double bps = ((Number) bitrate).doubleValue();
            best = (best < 0) ? bps : Math.min(best, bps);
        }
        return best;
    }

    private void evaluateLinkQuality() {
        double minBitrate = -1;
        synchronized (mStatsPeers) {
            for (final double bps : mPeerBitrateBps.values()) {
                if (bps < 0) continue;
                minBitrate = (minBitrate < 0) ? bps : Math.min(minBitrate, bps);
            }
        }
        if (minBitrate < 0) return;

        final long now = SystemClock.elapsedRealtime();
        if (minBitrate < LOW_OUTGOING_BITRATE_BPS) {
            mHealthyBitrateSinceMs = 0;
            if (mLowBitrateSinceMs == 0) {
                mLowBitrateSinceMs = now;
            } else if (now - mLowBitrateSinceMs >= STEP_DOWN_AFTER_MS
                    && mQualityStep > Preference.STREAM_RESOLUTION_SD) {
                stepCaptureQuality(mQualityStep - 1);
                mLowBitrateSinceMs = 0;
            }
        } else {
            mLowBitrateSinceMs = 0;
            if (mHealthyBitrateSinceMs == 0) {
                mHealthyBitrateSinceMs = now;
            } else if (now - mHealthyBitrateSinceMs >= STEP_UP_AFTER_MS
                    && mQualityStep < mConfiguredResolutionMode) {
                stepCaptureQuality(mQualityStep + 1);
                mHealthyBitrateSinceMs = 0;
            }
        }
    }

    private void stepCaptureQuality(final int newStep) {
        if (capturer == null || !mCapturing) return;
        final int oldWidth = mCaptureWidth;
        final int oldHeight = mCaptureHeight;
        final int oldFps = mCaptureFps;
        applyCaptureFormatMode(createCameraEnumerator(), mCaptureDeviceName, newStep);
        if (mCaptureWidth == oldWidth && mCaptureHeight == oldHeight && mCaptureFps == oldFps) {
            return;
        }
        final boolean down = newStep < mQualityStep;
        mQualityStep = newStep;
        capturer.changeCaptureFormat(mCaptureWidth, mCaptureHeight, mCaptureFps);
        Log.i(TAG_CAM_METRICS, "step=" + (down ? "down" : "up")
                + " " + mCaptureWidth + "x" + mCaptureHeight + "@" + mCaptureFps);
    }

    /***
     * switch between available cameras
     */
    public void switchCamera () {

        if (capturer instanceof AndruavCameraControl) {
            // App path: the capturer owns the camera order and cycles it.
            ((AndruavCameraControl) capturer).switchCamera(new AndruavCameraControl.CameraSwitchHandler() {
                @Override
                public void onCameraSwitchDone(boolean isFrontCamera) {
                    mCaptureFrontFacing = isFrontCamera;
                    // The capturer owns the id order; facing is the channel
                    // fallback id on this path.
                    mCaptureDeviceName = null;
                }

                @Override
                public void onCameraSwitchError(String errorDescription) {
                }
            });
            return;
        }

        final int deviceCount = createCameraEnumerator().getDeviceNames().length;
        if (deviceCount <= 1) return;
        if (!(capturer instanceof CameraVideoCapturer)) return;

        Preference.setCameraNumber(null,(Preference.getCameraNumber(null) + 1) % deviceCount);
        ((CameraVideoCapturer) capturer).switchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
            @Override
            public void onCameraSwitchDone(boolean isFrontCamera) {
                mCaptureFrontFacing = isFrontCamera;
                // The switch handler only reports facing; re-read the camera
                // index preference (already advanced by the caller above) to
                // recover the new enumerator device name.
                final String[] names = createCameraEnumerator().getDeviceNames();
                final int camNum = Preference.getCameraNumber(null);
                mCaptureDeviceName = (camNum >= 0 && camNum < names.length) ? names[camNum] : null;
            }

            @Override
            public void onCameraSwitchError(String errorDescription) {
            }
        });

    }

    /** Device name of the camera being captured (null once a blind switch
     *  invalidated it), for the camera channel surface. */
    public String getCaptureDeviceName ()
    {
        return mCaptureDeviceName;
    }

    public boolean isCaptureFrontFacing ()
    {
        return mCaptureFrontFacing;
    }


    public void setFlash (final int onOff)
    {
        if (capturer instanceof AndruavCameraControl) {
            ((AndruavCameraControl) capturer).setFlash(onOff);
            return;
        }
        if (!(capturer instanceof CameraVideoCapturer)) return;
        ((CameraVideoCapturer) capturer).setFlash(onOff);
    }

    public int getFlash ()
    {
        if (capturer instanceof AndruavCameraControl) {
            return ((AndruavCameraControl) capturer).getFlash();
        }
        if (!(capturer instanceof CameraVideoCapturer)) return org.webrtc.AndruavWebRTCGlobals.FlashOff;
        return ((CameraVideoCapturer) capturer).getFlash();
    }

    public void setZoom (final float zoom)
    {
        if (capturer instanceof AndruavCameraControl) {
            ((AndruavCameraControl) capturer).setZoom(zoom);
            return;
        }
        if (!(capturer instanceof CameraVideoCapturer)) return;
        ((CameraVideoCapturer) capturer).setZoom(zoom);
    }

    public float getZoom ()
    {
        if (capturer instanceof AndruavCameraControl) {
            return ((AndruavCameraControl) capturer).getZoom();
        }
        if (!(capturer instanceof CameraVideoCapturer)) return 0f;
        return ((CameraVideoCapturer) capturer).getZoom();
    }

    public boolean isZoomSupported ()
    {
        if (capturer instanceof AndruavCameraControl) {
            return ((AndruavCameraControl) capturer).isZoomSupported();
        }
        return (capturer instanceof CameraVideoCapturer)
                && ((CameraVideoCapturer) capturer).isZoomSupported();
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
        if (mStreamResolutionPrefListener != null && mContext != null) {
            PreferenceManager.getDefaultSharedPreferences(mContext)
                    .unregisterOnSharedPreferenceChangeListener(mStreamResolutionPrefListener);
            mStreamResolutionPrefListener = null;
        }
        resetLinkQualityState();
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
        mCapturing = false;
        resetLinkQualityState();
        try {
            if (capturer != null)
            {
                capturer.stopCapture();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.i(TAG_CAM_METRICS, "freezes=" + mCamFreezeCount);

        // stopLocalVideoSource() can run on any thread - e.g. a remote RemoteCommand_STREAMVIDEO
        // Act:false lands on the raw WebSocket read thread (see
        // AndruavWSClientBase.onTextMessage() -> ... -> stopStreaming()), which has no Looper.
        // `new Handler()` (no-arg) requires Looper.prepare() on the calling thread and throws
        // RuntimeException otherwise - an uncaught exception here on a non-main thread crashes the
        // whole process. Post to the main Looper explicitly instead.
        new Handler(Looper.getMainLooper()).post(() -> {
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
        mCamFreezeCount++;
    }

    @Override
    public void onCameraOpening(String s) {

    }


    @Override
    public void onFirstFrameAvailable() {
        if (!mCamFirstFrameLogged) {
            mCamFirstFrameLogged = true;
            Log.i(TAG_CAM_METRICS, "ttff_ms=" + (SystemClock.elapsedRealtime() - mCamSessionStartMs));
        }
    }

    @Override
    public void onCameraClosed() {

    }

    @Override
    public void onCameraFlashChanged(int flashOn) {
        // CameraID is the stream's wire channel = the camera entry's "id"
        // the GCS tracks status against (was PartyID before F4).
        AndruavDroneFacade.sendCameraFlashStatus (CameraID, flashOn, null);
    }

    @Override
    public void onCameraZoomChanged(float zoom) {
        AndruavDroneFacade.sendCameraZoomStatus(CameraID, zoom, null);

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
                trackStatsPeer(peer);
                mIRTCListener.onPeerConnected(peer.id);
            }
            else if (peer.status.equals(PnPeer.STATUS_DISCONNECTED))
            {
                untrackStatsPeer(peer);
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
                untrackStatsPeer(peer);
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