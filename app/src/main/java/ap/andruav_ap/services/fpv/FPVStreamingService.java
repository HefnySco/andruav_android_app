package ap.andruav_ap.services.fpv;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.AndruavWebRTCGlobals;
import org.webrtc.ContextUtils;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.andruav.AndruavEngine;
import com.andruav.AndruavFacade;
import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.fpv7adath.Event_FPV_CMD;
import com.andruav.event.fpv7adath._7adath_FPVStreamingStatusChanged;
import com.andruav.event.fpv7adath._7adath_StopAndroidCamera;
import com.andruav.notification.PanicFacade;
import com.andruav.uavos.modules.UAVOSConstants;
import com.andruav.uavos.modules.UAVOSModuleCamera;

import ap.andruav_ap.App;
import ap.andruav_ap.DeviceManagerFacade;
import ap.andruav_ap.R;
import ap.andruav_ap.activities.camera.CameraRecorder;
import ap.andruav_ap.activities.camera.Event_RecordVideoStatus;
import ap.andruavmiddlelibrary.Voting;
import ap.andruavmiddlelibrary.com.serenegiant.encoder.MediaVideoEncoder;
import ap.andruavmiddlelibrary.factory.io.FileHelper;
import ap.andruavmiddlelibrary.factory.util.Time_Helper;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.webrtc.IRTCListener;
import ap.andruavmiddlelibrary.webrtc.classes.AndruavVideoFileRenderer;
import ap.andruavmiddlelibrary.webrtc.classes.PeerConnectionManager;
import ap.andruavmiddlelibrary.webrtc.classes.PnPeer;
import ap.andruavmiddlelibrary.webrtc.classes.VSink;
import ap.andruavmiddlelibrary.webrtc.classes.VideoByteRenderer;

/**
 * Owns the WebRTC camera capture/publish pipeline ({@link PeerConnectionManager}) and the local
 * MP4 recorder ({@link CameraRecorder}) independently of any FPV Activity's lifecycle, running as
 * a {@code camera|microphone} foreground service so both survive the screen turning off (e.g. an
 * accidental power-button press against a drone mount) instead of being torn down in Activity
 * onPause() as before. FPVDroneRTCWebCamActivity/FPVModuleRTCWebCamActivity bind to this service
 * only to attach/detach their local-preview SurfaceViewRenderer; they never stop capture directly.
 * Streaming stops only through the {@link _7adath_StopAndroidCamera} event (UI stop button, or the
 * remote RemoteCommand_STREAMVIDEO Act:false command once no consumer remains).
 */
public class FPVStreamingService extends Service implements IRTCListener, VideoSink, VSink {

    /**
     * Distinct from {@code SensorService}'s 120 - both foreground services can run concurrently.
     */
    private static final int FOREGROUND_ID = 121;

    private final Handler mHandle = new Handler(Looper.getMainLooper());

    private PeerConnectionManager mPeerConnectionManager;
    private CameraRecorder mcameraRecorder;
    private AndruavVideoFileRenderer mVideoFileRenderer;
    private Uri mVideoFileRendererUri; // scoped storage (API 29+) MediaStore entry backing mVideoFileRenderer
    private VideoByteRenderer mVideoByteRenderer;
    private boolean mRecordVideo = false;

    private PowerManager.WakeLock mWakeLock;

    /**
     * Direct in-process reference for the Activity to attach/detach its SurfaceViewRenderer -
     * EventBus (used for every other command in this app) has no request/response semantics and
     * cannot marshal a View reference, so a minimal Binder is used for this one need only.
     */
    public class LocalBinder extends Binder {
        public FPVStreamingService getService() {
            return FPVStreamingService.this;
        }
    }

    private final LocalBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        // App.iFPVStreamingService is already set by App.startFPVStreamingService() (the caller
        // that started us) by the time onCreate() runs, so subscribers re-querying
        // App.isFPVStreamingServiceRunning() now will see the correct "running" state.
        EventBus.getDefault().post(new _7adath_FPVStreamingStatusChanged());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Must promote to foreground with the camera/microphone type before touching the camera -
        // required ordering on API 34.
        promoteToForeground();
        acquireWakeLock();
        initRTC();
        return START_STICKY;
    }

    private void promoteToForeground() {
        android.app.Notification notification = new NotificationCompat.Builder(this, ap.andruav_ap.Notification.CHANNEL_ID)
                .setContentTitle("Andruav")
                .setContentText("Streaming camera")
                .setSmallIcon(R.drawable.ic_logo2)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA | ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(FOREGROUND_ID, notification);
        }
    }

    private void acquireWakeLock() {
        if (mWakeLock != null) return;
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "andruav:FPVStreamingWakeLock");
        mWakeLock.setReferenceCounted(false);
        // Bounded as a crash-path safety net; the normal release path is explicit (stop funnel/onDestroy).
        mWakeLock.acquire(6 * 60 * 60 * 1000L);
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mWakeLock = null;
    }

    private void initRTC() {
        if (mPeerConnectionManager != null) return;

        ContextUtils.initialize(AndruavEngine.AppContext);

        mPeerConnectionManager = new PeerConnectionManager();
        final boolean res = mPeerConnectionManager.init(this, this, this, null, true, AndruavSettings.andruavWe7daBase.PartyID);
        if (!res) {
            PanicFacade.cannotStartCamera();
            Voting.onCameraIssue();
        }
    }

    public PeerConnectionManager getPeerConnectionManager() {
        return mPeerConnectionManager;
    }

    public void attachRenderer(final SurfaceViewRenderer renderer) {
        if (mPeerConnectionManager != null) {
            mPeerConnectionManager.attachLocalRenderer(renderer);
        }
    }

    public void detachRenderer() {
        if (mPeerConnectionManager != null) {
            mPeerConnectionManager.detachLocalRenderer();
        }
    }

    public boolean isRecording() {
        return mRecordVideo;
    }

    @Subscribe
    public void onEvent(final Event_FPV_CMD a7adath_FPV_CMD) {
        if (!DeviceManagerFacade.hasCamera()) return;
        if (mPeerConnectionManager == null) return;

        switch (a7adath_FPV_CMD.CMD_ID) {
            case Event_FPV_CMD.FPV_CMD_FLASHCAM:
                mPeerConnectionManager.setFlash(a7adath_FPV_CMD.ACT ? AndruavWebRTCGlobals.FlashOn : AndruavWebRTCGlobals.FlashOff);
                break;

            case Event_FPV_CMD.FPV_CMD_SWITCHCAM:
                mPeerConnectionManager.switchCamera();
                break;

            case Event_FPV_CMD.FPV_CMD_RECORDVIDEO:
                if (a7adath_FPV_CMD.ACT) {
                    startRecording();
                } else {
                    stopRecording();
                }
                break;
        }
    }

    /**
     * Single funnel for real stop - posted by the UI stop button and by the remote
     * RemoteCommand_STREAMVIDEO Act:false handler once no video consumer remains.
     */
    @Subscribe
    public void onEvent(final _7adath_StopAndroidCamera event) {
        stopStreamingAndSelf();
    }

    private void stopStreamingAndSelf() {
        if (mRecordVideo) {
            stopRecording();
        }
        if (mPeerConnectionManager != null) {
            mPeerConnectionManager.stopStreaming();
            mPeerConnectionManager.onDestroy();
            mPeerConnectionManager = null;
        }
        releaseWakeLock();
        stopForeground(true);
        stopSelf();
        // stopSelf() only *requests* destruction - an FPV Activity still bound with BIND_AUTO_CREATE
        // keeps this instance alive, so onDestroy() (previously the only place clearing App's "is
        // streaming" flag and announcing the change) does not run yet. That is exactly the
        // browser-side close case: the viewer hangs up while the FPV screen is in the foreground and
        // bound, so the Activity never learned the stream died and stayed open, and
        // App.startFPVStreamingService() stayed a no-op - no later remote start could revive
        // streaming until the user pressed exit on the phone (which unbinds and finally lets us be
        // destroyed). Announce from this funnel too; onDestroy() repeating it is harmless.
        // The flag is cleared inline (a concurrent startFPVStreamingService() must not be skipped),
        // the announcement is marshalled to the main thread because this funnel runs on whichever
        // thread posted the stop - the websocket thread for a remote command - while subscribers
        // react with UI calls (Activity.finish()).
        App.iFPVStreamingService = null;
        mHandle.post(() -> EventBus.getDefault().post(new _7adath_FPVStreamingStatusChanged()));
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Deliberately NOT stopping here: a remote GCS operator may still be viewing/recording
        // after the app is swiped from Recents on a drone mount. Streaming stops only through an
        // explicit _7adath_StopAndroidCamera event (see onEvent above) - this is a product
        // decision worth re-confirming, not an obviously "correct" default.
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        if (mPeerConnectionManager != null) {
            mPeerConnectionManager.stopStreaming();
            mPeerConnectionManager.onDestroy();
            mPeerConnectionManager = null;
        }
        releaseWakeLock();
        // We can stop via stopSelf() (the _7adath_StopAndroidCamera funnel) as well as by the
        // system, so this is the one place guaranteed to run regardless of how we stop - keep
        // App's "is streaming" flag in sync and let subscribers (e.g. the home screen's FPV
        // button) know.
        App.iFPVStreamingService = null;
        EventBus.getDefault().post(new _7adath_FPVStreamingStatusChanged());
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    ///////////////////////  IRTCListener

    @Override
    public void onLocalStream(final MediaStream localStream) {
        if (!App.isAndruavWSConnected()) return;

        mHandle.postDelayed(() -> {
            final int t = Preference.getFirstServer(null);
            Preference.setFirstServer(null, t + 1);
        }, (long) (Math.random() + 1) * 2000);
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, PnPeer peer) {
    }

    @Override
    public void onRemoveRemoteStream(MediaStream remoteStream, PnPeer peer) {
    }

    @Override
    public void onPeerConnectionClosed(final PnPeer peer) {
        // Posted (not run inline): the peer that just hung up is still in PeerConnectionManager's
        // peers map at this exact callback point (its own removal is a separately-posted Runnable
        // on the same main-thread queue, enqueued just before this listener call - see
        // PeerConnectionClientBase.removePeer()/PnPeer.hangup()). Posting here queues us behind
        // that pending removal so hasActivePeers() below reflects the post-hangup peer count.
        mHandle.post(() -> {
            AndruavSettings.mVideoRequests.remove(peer.getConnectedPeer().PartyID);

            // The browser's own "close" button drives this via the WebRTC hangup signal, not the
            // RemoteCommand_STREAMVIDEO Act:false path (mVideoRequests is only ever populated by
            // that command's Act:true branch, which this webclient never sends - it starts a view
            // with a bare "joinme"). Relying solely on that command's mVideoRequests bookkeeping
            // left the camera running (and this Activity stuck open) after every browser-side close.
            // The peer count is the one signal a plain hangup always produces, regardless of which
            // command flow the viewer used to start - stop for real once nobody is left watching.
            if ((mPeerConnectionManager != null) && (!mPeerConnectionManager.hasActivePeers())) {
                EventBus.getDefault().post(new _7adath_StopAndroidCamera());
            }
        });
    }

    @Override
    public void onPeerConnected(final String userId) {
    }

    ///////////////////////  Recording

    private void startRecording() {
        mRecordVideo = false;
        final int height, width;
        if (Preference.useStreamVideoHD(null)) {
            height = 1080;
            width = 720;
        } else {
            height = 640;
            width = 480;
        }
        try {
            try {
                if (MediaVideoEncoder.VIDEO_FORMAT == MediaVideoEncoder.MOBILE_WORK_FOR_ALL) {
                    final String videoName = "v_" + Time_Helper.getDateTimeString() + ".mp4";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Scoped storage: write to a MediaStore Downloads entry so the video
                        // survives uninstall and is browsable in a File Manager.
                        mVideoFileRendererUri = FileHelper.createDownloadsEntry(App.KMLFile.getVideoPath(), videoName, "video/mp4");
                        if (mVideoFileRendererUri == null) {
                            throw new IOException("Unable to create video file in Downloads");
                        }
                        java.io.OutputStream os = getContentResolver().openOutputStream(mVideoFileRendererUri);
                        if (os == null) {
                            throw new IOException("Unable to open video file for writing");
                        }
                        mVideoFileRenderer = new AndruavVideoFileRenderer(os, videoName, 15, width, height, mPeerConnectionManager.getEglBaseContextTX());
                    } else {
                        mVideoFileRenderer = new AndruavVideoFileRenderer(App.KMLFile.getVideoPath() + "/v_" + Time_Helper.getDateTimeString() + ".mp4", 15, width, height, mPeerConnectionManager.getEglBaseContextTX());
                    }
                } else {
                    mcameraRecorder = new CameraRecorder();
                    mcameraRecorder.init();
                    mcameraRecorder.startRecording(width, height, 15, false, false, mPeerConnectionManager.getSurfaceTX(), mPeerConnectionManager.getDefaultVideoEncoderFactory());

                    mVideoByteRenderer = new VideoByteRenderer(this, width, height, mPeerConnectionManager.getEglBaseContextTX());
                }
            } catch (Exception e) {
                e.printStackTrace();
                mRecordVideo = false;
            }

            // must be last
            mRecordVideo = true;

            AndruavSettings.andruavWe7daBase.VideoRecording = AndruavUnitBase.VIDEORECORDING_ON;
            try {
                UAVOSModuleCamera uavosModuleCamera = (UAVOSModuleCamera) AndruavEngine.getUAVOSMapBase().get(AndruavSettings.andruavLocalCameraModuleID);
                JSONArray json_tracks = (JSONArray) (uavosModuleCamera.getModuleMessages());
                JSONObject camera = json_tracks.getJSONObject(0);
                camera.put(UAVOSConstants.CAMERA_RECORDING_NOW, true);
            } catch (Exception ex) {
                // failed to update camera module.
            }
            AndruavEngine.getEventBus().post(new Event_RecordVideoStatus(Event_RecordVideoStatus.CONST_IS_RECORDING));
            if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
                AndruavFacade.broadcastID();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mRecordVideo = false;
        }
    }

    private void stopRecording() {
        mRecordVideo = false;
        if (mVideoFileRenderer != null) {
            mVideoFileRenderer.release();
            mVideoFileRenderer = null;
        }
        if (mVideoFileRendererUri != null) {
            FileHelper.markMediaStoreEntryComplete(mVideoFileRendererUri);
            mVideoFileRendererUri = null;
        }
        if (mVideoByteRenderer != null) {
            mVideoByteRenderer.release();
            mVideoByteRenderer = null;
            mcameraRecorder.stopRecording();
            mcameraRecorder = null;
        }

        AndruavSettings.andruavWe7daBase.VideoRecording = AndruavUnitBase.VIDEORECORDING_OFF;

        try {
            UAVOSModuleCamera uavosModuleCamera = (UAVOSModuleCamera) AndruavEngine.getUAVOSMapBase().get(AndruavSettings.andruavLocalCameraModuleID);
            JSONArray json_tracks = (JSONArray) (uavosModuleCamera.getModuleMessages());
            JSONObject camera = json_tracks.getJSONObject(0);
            camera.put(UAVOSConstants.CAMERA_RECORDING_NOW, false);
        } catch (Exception ex) {
            // failed to update camera module.
        }
        if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
            AndruavFacade.broadcastID();
        }
    }

    ///////////////////////  VSink / VideoSink (recording feed)

    @Override
    public void onFrame(final byte[] frame, final int offset, final int size) {
        if ((mcameraRecorder != null) && (mcameraRecorder.isRecording())) {
            // condition is replicated to avoid post runnable without need, and then void calling null object when stop recording.
            mHandle.post(() -> {
                if (mRecordVideo) {
                    mcameraRecorder.encodeFeed(ByteBuffer.wrap(frame, offset, size));
                }
            });
        }
    }

    private long lastTimeFrame = 0;

    @Override
    public void onFrame(VideoFrame videoFrame) {
        if (mRecordVideo) {
            if (MediaVideoEncoder.VIDEO_FORMAT == MediaVideoEncoder.MOBILE_WORK_FOR_ALL) {
                if (mVideoFileRenderer != null) {
                    long now = System.currentTimeMillis();
                    if ((now - lastTimeFrame) > 50) {
                        lastTimeFrame = now;
                        mVideoFileRenderer.onFrame(videoFrame);
                    }
                    mVideoFileRenderer.onFrame(videoFrame);
                }
            } else {
                if (mVideoByteRenderer != null) {
                    mVideoByteRenderer.onFrame(videoFrame);
                }
            }
        }
    }
}
