package ap.andruav_ap.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.andruav.AndruavEngine;

import ap.andruav_ap.App;
import ap.andruav_ap.communication.UnitVoiceRegistry;
import ap.andruav_ap.services.fpv.FPVStreamingService;
import ap.andruavmiddlelibrary.webrtc.classes.PeerConnectionManager;

/**
 * Drives one half-duplex voice channel toward a remote unit (VC-9).
 *
 * The only live WebRTC stack in the app is the FPVStreamingService's PeerConnectionManager,
 * so press-and-hold works in two steps: ensure the service is started and bound, then
 * joinToDrone(partyID, channel) once its manager exists. Release sends disconnectToDrone.
 * Only one channel may be held at a time - half duplex, mirroring the webclient.
 */
public class VoiceChannelController {

    public static final String CHANNEL_LISTEN = com.andruav.uavos.modules.UAVOSConstants.VOICE_CHANNEL_MIC;
    public static final String CHANNEL_TALK = com.andruav.uavos.modules.UAVOSConstants.VOICE_CHANNEL_SPK;

    private static final int JOIN_RETRY_DELAY_MS = 400;
    private static final int JOIN_RETRY_LIMIT = 15;

    public interface StateListener {
        void onVoiceChannelStateChanged();
    }

    private FPVStreamingService mBoundService;
    private boolean mBoundRequested = false;
    private String mHeldChannel = null;
    private String mHeldTarget = null;
    private boolean mJoinPending = false;
    private int mJoinRetries = 0;
    private StateListener mStateListener;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            mBoundService = ((FPVStreamingService.LocalBinder) service).getService();
            notifyState();
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mBoundService = null;
        }
    };

    public void setStateListener(final StateListener listener) {
        mStateListener = listener;
    }

    public boolean isHeld() {
        return mHeldChannel != null;
    }

    public String getHeldChannel() {
        return mHeldChannel;
    }

    /**
     * Binds to an already-running FPVStreamingService without creating it - a free peek at
     * whether a PeerConnectionManager is available.
     */
    public void bindQuiet(final Context context) {
        if (mBoundRequested) return;
        mBoundRequested = true;
        context.bindService(new Intent(context, FPVStreamingService.class), mServiceConnection, 0);
    }

    public void unbind(final Context context) {
        release();
        if (mBoundRequested) {
            mBoundRequested = false;
            try {
                context.unbindService(mServiceConnection);
            }
            catch (Exception ignored) { }
        }
        mBoundService = null;
    }

    /**
     * Starts a push-and-hold on the given channel ("mic" to hear the unit, "spk" to talk
     * to its speaker). Resolves the remote unit from the capability registry, ensures the
     * streaming service is up, then dials once its PeerConnectionManager is live.
     */
    public void press(final Context context, final String channel) {
        if (mHeldChannel != null) release();

        final String namespace = CHANNEL_TALK.equals(channel)
                ? UnitVoiceRegistry.NS_SPEAKER : UnitVoiceRegistry.NS_MICROPHONE;
        final String target = UnitVoiceRegistry.getInstance().findUnitWith(namespace);
        if (target == null) {
            notifyState();
            return;
        }

        mHeldChannel = channel;
        mHeldTarget = target;
        mJoinPending = true;
        mJoinRetries = 0;
        notifyState();

        bindQuiet(context);
        if (mBoundService == null) {
            App.startFPVStreamingService();
            context.bindService(new Intent(context, FPVStreamingService.class),
                    mServiceConnection, Context.BIND_AUTO_CREATE);
        }
        tryJoin(context);
    }

    /** Releases the held channel: hangs up the dialed stream. Safe to call when idle. */
    public void release() {
        if (mHeldChannel == null) return;

        final PeerConnectionManager manager =
                (mBoundService != null) ? mBoundService.getPeerConnectionManager() : null;
        if (manager != null) {
            manager.disconnectToDrone(mHeldTarget, mHeldChannel);
        }

        mHeldChannel = null;
        mHeldTarget = null;
        mJoinPending = false;
        notifyState();
    }

    private void tryJoin(final Context context) {
        if (!mJoinPending || (mHeldChannel == null)) return;

        final PeerConnectionManager manager =
                (mBoundService != null) ? mBoundService.getPeerConnectionManager() : null;
        if (manager != null) {
            mJoinPending = false;
            manager.joinToDrone(mHeldTarget, mHeldChannel);
            notifyState();
            return;
        }

        if (++mJoinRetries > JOIN_RETRY_LIMIT) {
            AndruavEngine.log().log(null, "voice", "PeerConnectionManager not ready - voice dial dropped");
            release();
            return;
        }
        mHandler.postDelayed(() -> tryJoin(context), JOIN_RETRY_DELAY_MS);
    }

    private void notifyState() {
        if (mStateListener != null) mStateListener.onVoiceChannelStateChanged();
    }
}
