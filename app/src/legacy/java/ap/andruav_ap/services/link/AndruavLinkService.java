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
import com.andruav.AndruavFacade;
import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
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
 * The service never owns the socket itself - it guards whatever link
 * {@link App#startAndruavWS()} brought up, and recreates it after a process death: a
 * START_STICKY restart (or a BOOT start while the link was left desired) is detected in
 * {@link #onStartCommand}, {@link App#resumeLink()} re-logins and reconnects, and the first
 * re-registration triggers a one-shot headless recovery (IDs + permanent tasks + signal
 * monitor - link + IDs only, no FCB auto-connect and no SensorService start). The service
 * still never tears the link down: a transient {@link EventSocketState} disconnect during
 * reconnect back-off updates the notification text but keeps the guardian alive. Stopping
 * happens only through the explicit user-intent paths ({@link App#stopAndruavWS()} /
 * shutdown signalling order 4).
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
    /** Written on the WS handler thread (EventSocketState), read on the main thread. */
    private volatile String mStatusText;
    private volatile boolean mBatteryWarningShown = false;
    private volatile boolean mReleased = false;

    /**
     * True between detecting a process-death restart and the first successful re-registration:
     * gates the one-shot headless post-registration recovery (see {@link #doHeadlessRecovery}).
     * <br>volatile: set on the main thread in {@link #onStartCommand}, read and cleared on the
     * WS handler thread by the {@link EventSocketState} subscriber (ThreadMode.POSTING). Without
     * the barrier the recovery could silently never run after a kill.
     */
    private volatile boolean mRecoveredProcess = false;

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
                updateNotificationText(getString(R.string.link_status_active));
                break;
            case onDisconnect:
            case onError:
                updateNotificationText(getString(R.string.link_status_reconnecting));
                break;
            default: // onMessage: no status change
                break;
        }

        if ((event.SocketState == EventSocketState.ENUM_SOCKETSTATE.onRegistered) && mRecoveredProcess) {
            // First re-registration after a process-death recovery: the Activities that
            // normally do this on registration (MainScreen) do not exist in this headless
            // process. Run once, then clear the flag.
            mRecoveredProcess = false;
            doHeadlessRecovery();
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
        // App.startAndruavLinkService() sets App.iLinkService before starting us - but a
        // START_STICKY recreate (or a BOOT start) comes from the system, which never goes
        // through it. Without this, App.iLinkService stays null while the guardian is alive and
        // stopAndruavLinkService() would clear the desired-flag and then SKIP stopService(),
        // stranding a foreground service holding a wake lock and an un-swipeable notification.
        if (App.iLinkService == null) {
            App.iLinkService = new Intent(this, AndruavLinkService.class);
        }
        EventBus.getDefault().register(this);

        mLinkStartTime = System.currentTimeMillis();
        mStatusText = getString(AndruavEngine.isAndruavWSStatus(AndruavWSClientBase.SOCKETSTATE_REGISTERED)
                ? R.string.link_status_active : R.string.link_status_connecting);

        mHandler.postDelayed(mWakeLockRearm, WAKE_LOCK_REARM_INTERVAL_MS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // A non-null-intent start while the link is not desired is a stray start - a normal
        // start always sets the flag first (App.startAndruavLinkService()). Quit instead of
        // holding a wake lock for a link nobody wants.
        if ((intent != null) && (!Preference.isLinkServiceDesired(null))) {
            // We were started via startForegroundService(): satisfy its contract with a
            // startForeground() call before quitting, otherwise the system crashes with
            // ForegroundServiceDidNotStartInTimeException.
            promoteToForeground();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }

        promoteToForeground();
        acquireWakeLock();
        registerNetworkWatchdog();
        checkBatteryExemption();

        // Process-death recovery: a null intent means the system recreated us via START_STICKY
        // after the process was killed; a start (e.g. from BOOT_Receiver) with no WS client in
        // the process means the same thing from a fresh process. In both cases the link is
        // wanted but gone - recreate it headless. Not checked on the normal connect path:
        // startAndruavWS() runs on the main thread and has already installed the WS client by
        // the time this onStartCommand is dispatched, so it never trips there.
        if ((intent == null) || (AndruavEngine.getAndruavWS() == null)) {
            mRecoveredProcess = true;
            App.resumeLink();
        }

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
                .setContentTitle(getString(R.string.link_notification_title))
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

    /***
     * One-shot headless post-registration recovery after a process restart: link + IDs only,
     * mirroring what MainScreen does on registration when the Activities are alive.
     * <br>Deliberately NOT done headless (per review):
     * <ul>
     * <li>{@code TelemetryModeer.connectToPreferredConnection()} - the USB/Bluetooth paths can
     * need an Activity for permission dialogs, and a silent FCB reconnect can fire while the
     * vehicle is on the ground being serviced. FCB reconnect stays a foreground, user-visible
     * action.</li>
     * <li>{@code App.startSensorService()} - a location-type FGS started from the background
     * is rejected on API 34 in exactly the situation this code runs in; it would be a logged
     * failure, not a recovery.</li>
     * </ul>
     */
    private void doHeadlessRecovery ()
    {
        try {
            AndruavFacade.broadcastID();                     // tell them I am online
            AndruavFacade.requestID();                      // guys !! who are there ?
            AndruavFacade.sendID((AndruavUnitBase) null);    // guys I am here

            AndruavSettings.loadGenericPermanentTasks();
            AndruavSettings.loadMyPermanentTasksByPartyID();

            // Safe and needed: its only other caller is MainScreen, so after a headless restart
            // nothing else would register the signal listener.
            ((App) getApplication()).initSignalMonitor();
        } catch (Exception e) {
            AndruavEngine.log().logException("link_recovery", e);
        }
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
                .setContentTitle(getString(R.string.link_battery_warning_title))
                .setContentText(getString(R.string.link_battery_warning))
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

        // Unregister the event bus FIRST. A socket event arriving after stopForeground() would
        // run updateNotificationText() -> notify(FOREGROUND_ID), re-posting a FLAG_NO_CLEAR
        // notification that no service is left to cancel - the user could not even swipe it away.
        try {
            EventBus.getDefault().unregister(this);
        } catch (Exception e) {
            // never registered - nothing to do.
        }

        // Clear the "is running" flag early: if anything below throws, startAndruavLinkService()
        // must not be left seeing a stale non-null Intent.
        App.iLinkService = null;

        unregisterNetworkWatchdog();
        mHandler.removeCallbacksAndMessages(null);
        releaseWakeLock();
        stopForeground(STOP_FOREGROUND_REMOVE);

        // Belt-and-braces against a notify() that raced the unregister above, and clears the
        // battery warning too - it is about keeping THIS link alive, so it must not outlive it.
        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(FOREGROUND_ID);
            nm.cancel(BATTERY_WARNING_ID);
        }
    }
}
