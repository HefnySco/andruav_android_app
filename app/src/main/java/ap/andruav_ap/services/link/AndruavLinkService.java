package ap.andruav_ap.services.link;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import com.andruav.AndruavEngine;
import com.andruav.event.networkEvent.EventSocketState;
import com.andruav.event.systemEvent.Event_ShutDown_Signalling;
import com.andruav.protocol.communication.websocket.AndruavWSClientBase;

import ap.andruav_ap.App;
import ap.andruav_ap.R;
import ap.andruav_ap.activities.main.MainScreen;
import ap.andruav_ap.helpers.CheckAppPermissions;
import ap.andruavmiddlelibrary.preference.Preference;

/**
 * Link guardian for the Andruav server WebSocket link.
 * <p>
 * The WebSocket client ({@code AndruavWSClient_TooTallNate}) runs on a HandlerThread inside the
 * app process with no owning service. When the app is backgrounded mid-flight, Android
 * (Doze / App-Standby / OEM killers) can freeze or kill the process and silently drop the link.
 * This {@code specialUse} foreground service's lifetime is bound to "the user wants the link
 * up": it holds an ongoing un-swipeable notification, a partial wake lock, and a network-change
 * watchdog, so the link survives the app being swiped from recents.
 * <p>
 * The service never owns the socket itself - it only guards whatever link
 * {@link App#startAndruavWS()} brought up, and never tears it down: a transient
 * {@link EventSocketState} disconnect during reconnect back-off updates the notification text
 * but keeps the guardian alive. Stopping happens only through the explicit user-intent paths
 * ({@link App#stopAndruavWS()} / shutdown signalling order 4).
 * <p>
 * Battery-optimization exemption is load-bearing, not cosmetic: in deep Doze partial wake
 * locks are ignored and network is suspended - the allowlist is the only real fix. Being on the
 * allowlist is also the exemption that makes a background {@code startForegroundService()} (and
 * a later START_STICKY restart) legal. Without it every start here is best-effort: it throws
 * {@code ForegroundServiceStartNotAllowedException}, which is caught and logged, never crashed
 * on.
 */
public class AndruavLinkService extends Service {

    /**
     * Notification id for this service's foreground notification. Distinct from
     * {@code SensorService}'s 120 and {@code FPVStreamingService}'s 121 - all three can be in
     * the foreground at once. Every id in {@code INotification} is <= 110.
     */
    private static final int FOREGROUND_ID = 122;

    /**
     * User-dismissible battery-optimization warning notification (shown once per service
     * instance when the app is not exempt).
     */
    private static final int BATTERY_WARNING_ID = 123;

    /**
     * Bounded wake-lock acquire, as a crash-path safety net; the normal release path is
     * explicit (shut down funnel / onDestroy). Re-armed by {@link #mWakeLockRearm} so a
     * ground-station session longer than 12h does not silently lose the lock.
     */
    private static final long WAKE_LOCK_TIMEOUT_MS = 12 * 60 * 60 * 1000L;

    private static final long WAKE_LOCK_REARM_INTERVAL_MS = 60 * 60 * 1000L;

    private PowerManager.WakeLock mWakeLock;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private long mLinkStartTime;
    private String mStatusText;
    private boolean mBatteryWarningShown = false;
    private boolean mReleased = false;

    /** Re-arms the bounded wake lock so a session longer than WAKE_LOCK_TIMEOUT_MS keeps it. */
    private final Runnable mWakeLockRearm = new Runnable() {
        @Override
        public void run() {
            rearmWakeLock();
            mHandler.postDelayed(this, WAKE_LOCK_REARM_INTERVAL_MS);
        }
    };

    //////////BUS EVENT

    /**
     * Link status only: updates the ongoing notification's text. Never stops the service -
     * transient disconnects during reconnect back-off must not tear the guardian down.
     * <br>Arrives on the WS handler thread; NotificationManager.notify() is thread-safe.
     */
    @Subscribe
    public void onEvent (final EventSocketState event)
    {
        switch (event.SocketState) {
            case onConnect:
            case onRegistered:
                updateNotificationText("Link active");
                break;
            case onDisconnect:
            case onError:
                updateNotificationText("Link down — reconnecting…");
                break;
            default: // onMessage: no status change
                break;
        }
    }

    @Subscribe
    public void onEvent (final Event_ShutDown_Signalling event)
    {
        if (event.CloseOrder != 4) return ;

        // Order 4 is correct: the WS client already tore itself down at order 3, so the socket
        // is dead before the guardian exits.
        shutDown();
    }

    ///////////////////

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // App.iLinkService is already set by App.startAndruavLinkService() (the caller that
        // started us) by the time onCreate() runs, so subscribers re-querying
        // App.isLinkServiceRunning() now will see the correct "running" state.
        EventBus.getDefault().register(this);

        mLinkStartTime = System.currentTimeMillis();
        mStatusText = AndruavEngine.isAndruavWSStatus(AndruavWSClientBase.SOCKETSTATE_REGISTERED)
                ? "Link active" : "Connecting…";

        mHandler.postDelayed(mWakeLockRearm, WAKE_LOCK_REARM_INTERVAL_MS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        promoteToForeground();
        acquireWakeLock();
        registerNetworkWatchdog();
        checkBatteryExemption();

        // The START_STICKY restart behaviour (re-login after process death) is phase 2; the
        // return value is harmless now.
        return START_STICKY;
    }

    /**
     * Deliberate no-op: stopWithTask="false" in the manifest keeps this service alive when the
     * user swipes the app from recents - the link is supposed to outlive that gesture.
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
    }

    @Override
    public void onDestroy(){
        // Explicit disconnect (stopAndruavLinkService) or system teardown - the persisted
        // desired-flag intentionally survives here; only the explicit paths clear it.
        release();
        super.onDestroy();
    }

    /**
     * Keeps the process hosting the link in the active standby bucket and out of the cached
     * low-memory-killer list. The notification is the single persistent source of link status.
     */
    private void promoteToForeground() {
        final android.app.Notification notification = buildNotification(mStatusText);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // FOREGROUND_SERVICE_TYPE_SPECIAL_USE is an API 34 constant - the typed
                // overload does not exist below it.
                startForeground(FOREGROUND_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(FOREGROUND_ID, notification);
            }
        } catch (Exception e) {
            // Deliberately Exception, not just SecurityException: a background start without
            // the battery-optimization exemption throws ForegroundServiceStartNotAllowedException
            // (an IllegalStateException), which SensorService's SecurityException-only catch
            // would let through. Never crash - log and continue as a plain service; a STICKY
            // restart in a crash loop would be worse than no guardian at all.
            AndruavEngine.log().logException("link_fgs", e);
        }
    }

    private android.app.Notification buildNotification(final String text) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launchIntent == null) {
            launchIntent = new Intent(this, MainScreen.class);
        }
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ap.andruav_ap.Notification.CHANNEL_ID_LINK)
                .setContentTitle("Andruav Link")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_logo2)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setUsesChronometer(true)
                .setWhen(mLinkStartTime)
                .setContentIntent(contentIntent);

        final android.app.Notification notification = builder.build();
        notification.flags |= android.app.Notification.FLAG_NO_CLEAR | android.app.Notification.FLAG_ONGOING_EVENT;
        return notification;
    }

    /** Replaces the ongoing notification in place; safe from any thread. */
    private void updateNotificationText(final String text) {
        mStatusText = text;
        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        nm.notify(FOREGROUND_ID, buildNotification(text));
    }

    /**
     * Partial wake lock + an open socket is what actually keeps Wi-Fi up while backgrounded.
     * No WifiLock: WIFI_MODE_FULL has been a no-op since API 29 and WIFI_MODE_FULL_LOW_LATENCY
     * is only honored while the screen is on - precisely not the backgrounded-flight case.
     */
    private void acquireWakeLock() {
        if (mWakeLock != null) return;
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "andruav:LinkWakeLock");
        mWakeLock.setReferenceCounted(false);
        mWakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        mWakeLock = null;
    }

    private void rearmWakeLock() {
        if (mWakeLock == null) return;
        if (mWakeLock.isHeld()) mWakeLock.release();
        mWakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
    }

    /**
     * Watches the default network: a Wi-Fi<->LTE handoff leaves a half-dead socket that can
     * hang for minutes waiting for a TCP timeout. Both a lost and a (re)gained network force
     * the WS client's error-recovery chain immediately via its single deterministic entry
     * point, which covers both the live-socket and dead-socket cases.
     */
    private void registerNetworkWatchdog() {
        if (mNetworkCallback != null) return;
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(final Network network) {
                forceLinkReconnect();
            }

            @Override
            public void onLost(final Network network) {
                forceLinkReconnect();
            }
        };
        try {
            cm.registerDefaultNetworkCallback(mNetworkCallback);
        } catch (Exception e) {
            AndruavEngine.log().logException("link_netcb", e);
            mNetworkCallback = null;
        }
    }

    private void unregisterNetworkWatchdog() {
        if (mNetworkCallback == null) return;
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            try {
                cm.unregisterNetworkCallback(mNetworkCallback);
            } catch (Exception e) {
                AndruavEngine.log().logException("link_netcb", e);
            }
        }
        mNetworkCallback = null;
    }

    private void forceLinkReconnect() {
        // Null-guarded: the WS client may not exist yet (never connected) or may already be
        // torn down. requestReconnectNow() is a no-op if the socket is already registered and
        // healthy.
        if (AndruavEngine.getAndruavWS() == null) return;
        AndruavEngine.getAndruavWS().requestReconnectNow();
    }

    /**
     * One-shot warning (id 123) when the app is not exempt from battery optimization: without
     * the exemption this guardian is best-effort at best. User-dismissible by design - it is a
     * warning, not the link notification.
     */
    private void checkBatteryExemption() {
        if (mBatteryWarningShown) return;
        if (CheckAppPermissions.isIgnoringBatteryOptimizations(this)) return;
        mBatteryWarningShown = true;

        final Intent intent = CheckAppPermissions.getIgnoreBatteryOptimizationsIntent(this);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ap.andruav_ap.Notification.CHANNEL_ID)
                .setContentTitle("Andruav")
                .setContentText("Battery optimization may drop the server link in the background. Tap to exempt Andruav.")
                .setSmallIcon(R.drawable.ic_logo2)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        nm.notify(BATTERY_WARNING_ID, builder.build());
    }

    private void shutDown() {
        // The user (or shutdown signalling) no longer wants the link: clear the persisted
        // desired-flag so a later process restart does not resurrect the guardian.
        Preference.setLinkServiceDesired(null, false);
        release();
        stopSelf();
    }

    /** Single idempotent teardown funnel for both shutDown() and onDestroy(). */
    private void release() {
        if (mReleased) return;
        mReleased = true;

        // Clear the "is running" flag FIRST: if anything below throws, startAndruavLinkService()
        // must not be left seeing a stale non-null Intent.
        App.iLinkService = null;

        unregisterNetworkWatchdog();
        mHandler.removeCallbacksAndMessages(null);
        releaseWakeLock();
        stopForeground(STOP_FOREGROUND_REMOVE);

        try {
            EventBus.getDefault().unregister(this);
        } catch (Exception e) {
            // never registered - nothing to do.
        }
    }
}
