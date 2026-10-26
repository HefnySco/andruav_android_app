package ap.andruav_ap.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import ap.andruav_ap.BuildConfig;
import ap.andruav_ap.R;
import ap.andruav_ap.denative.position.PhonePositionSource;
import ap.andruav_ap.denative.providers.phone.PhoneDevices;
import ap.andruav_ap.denative.providers.phone.PhoneHealthConfig;
import ap.andruav_ap.denative.providers.phone.PhoneHealthMonitor;
import ap.andruav_ap.denative.providers.phone.PhoneProvider;
import com.andruav.andruavUnit.AndruavUnitBase;
import de.databus.CModule;
import de.databus.Messages;
import de.databus.caps.CapabilityHost;
import de.databus.caps.DeCapabilityProvider;
import de.databus.caps.TestEchoProvider;

/**
 * Foreground service hosting the deDatabus module "andruav_phone" in the
 * app's MAIN process (unlike the native DE modules, which run one per
 * process under {@code :de_<module>}). The module talks to the on-device
 * de_comm over UDP loopback (its s2s_udp_listening_port) and exposes the
 * app's de.cap/1 capability surface through a {@link CapabilityHost}.
 * In debug builds a {@link TestEchoProvider} ("test_echo" namespace) is
 * registered so the invoke/state/event path can be exercised end to end.
 * <p>
 * Start: intent action {@link #ACTION_START} on this component, optional
 * int extra {@link #EXTRA_S2S_PORT} overriding {@link #s2sPort}. Stop:
 * {@link #ACTION_STOP}.
 */
public class DeAndroidModuleService extends Service {

    public static final String ACTION_START = "ap.andruav_ap.action.DE_MODULE_START";
    public static final String ACTION_STOP = "ap.andruav_ap.action.DE_MODULE_STOP";

    /** Int extra on ACTION_START: de_comm's s2s UDP port (default {@link #s2sPort}). */
    public static final String EXTRA_S2S_PORT = "s2s_port";

    /**
     * Static config hook: de_comm's s2s_udp_listening_port when the start
     * intent carries no {@link #EXTRA_S2S_PORT}. Matches the stock
     * deNative asset de_comm.config.module.json (60000).
     */
    public static int s2sPort = 60000;

    private static final String TAG = "DeAndroidModuleSvc";
    private static final String CHANNEL_ID = "de_module_host";
    private static final int FOREGROUND_ID = 124;

    private static final String MODULE_ID = "andruav_phone";
    private static final String MODULE_KEY = "andruav_phone";

    private CModule mModule;
    private CapabilityHost mCapabilityHost;
    private PhoneProvider mPhoneProvider;
    private PhoneHealthMonitor mHealthMonitor;
    private PhonePositionSource mPositionSource;
    private volatile boolean mModuleRunning;
    private String mStatusText = "starting";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "DroneEngage app module", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        promoteToForeground();
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopModule();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        startModule(intent != null ? intent.getIntExtra(EXTRA_S2S_PORT, s2sPort) : s2sPort);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopModule();
        super.onDestroy();
    }

    /** Idempotent: a repeated START while running is a no-op. */
    private void startModule(final int port) {
        if (mModuleRunning) {
            return;
        }

        final CModule module = new CModule();
        module.defineModule(
                CModule.MODULE_CLASS_GENERIC,
                MODULE_ID,
                MODULE_KEY,
                BuildConfig.VERSION_NAME,
                // Subscribed to 9102 so de_comm fans other modules'
                // position reports out to us - the "FC position present"
                // signal for the phone-GPS gate (B8).
                new JSONArray().put(Messages.TYPE_AndruavModule_Location_Info));

        final List<DeCapabilityProvider> providers = new ArrayList<>();
        if (BuildConfig.DEBUG) {
            providers.add(new TestEchoProvider());
        }
        // "phone" namespace: output resources (screen strobe / torch /
        // vibrate - TO-BE section 5.2) + health state and edge facts
        // (battery_low, overheat, net_lost, net_back - section 5.6). The
        // outputs bank is process-wide so Emergency drives the same
        // choke points the bus sees.
        final PhoneProvider phoneProvider = new PhoneProvider(
                PhoneHealthConfig.defaults(), PhoneDevices.shared(this));
        providers.add(phoneProvider);
        final CapabilityHost host = new CapabilityHost(module, providers);
        // Registers adverts + takes over invoke/event routing before the
        // socket exists; the advert goes out on the first ID reply.
        host.start();
        phoneProvider.attach(host);

        // B8: phone GPS as the unit position (9102) when no FC position is
        // being reported. The source watches other modules' 9102s through
        // the composite receive callback below and emits at 1 Hz when the
        // gate opens (AUTO: after 5 s of silence; MOBILE: always; FCB:
        // never). GPS mode comes from the app-side unit setting.
        final PhonePositionSource positionSource = new PhonePositionSource(
                this, module, MODULE_KEY,
                () -> (com.andruav.AndruavSettings.andruavWe7daBase != null)
                        ? com.andruav.AndruavSettings.andruavWe7daBase.getGPSMode()
                        : AndruavUnitBase.GPS_MODE_AUTO);
        // host.start() claimed the module's single OnReceive callback -
        // replace it with a composite that still serves the capability
        // host and also feeds the position gate.
        module.setOnReceive((message, len, jMsg) -> {
            host.onReceive(message, len, jMsg);
            positionSource.onModuleMessage(jMsg);
        });

        // Broadcast + poll driven sampler feeding the phone health tracker.
        final PhoneHealthMonitor monitor = new PhoneHealthMonitor(this, phoneProvider.tracker());
        monitor.start();

        // Socket bind + receiver threads off the main thread, mirroring
        // DeModuleService's moduleThread pattern.
        new Thread(() -> {
            try {
                module.init("127.0.0.1", port, "0.0.0.0", 0, 0);
                synchronized (DeAndroidModuleService.this) {
                    if (mModule != module) {
                        // STOP raced the init: drop this module's socket.
                        module.uninit();
                        return;
                    }
                    mModuleRunning = true;
                }
                // Socket is up: start the location feed + 1 Hz emitter.
                positionSource.start();
                mStatusText = MODULE_ID + " on 127.0.0.1:" + port;
                Log.i(TAG, mStatusText);
                updateNotificationText(mStatusText);
            } catch (Exception e) {
                Log.e(TAG, "module init failed", e);
                mStatusText = MODULE_ID + " init failed: " + e;
                updateNotificationText(mStatusText);
            }
        }, "de_" + MODULE_ID).start();

        mModule = module;
        mCapabilityHost = host;
        mPhoneProvider = phoneProvider;
        mHealthMonitor = monitor;
        mPositionSource = positionSource;
    }

    private void stopModule() {
        final CModule module;
        final PhoneProvider phoneProvider;
        final PhoneHealthMonitor monitor;
        final PhonePositionSource positionSource;
        synchronized (this) {
            module = mModule;
            phoneProvider = mPhoneProvider;
            monitor = mHealthMonitor;
            positionSource = mPositionSource;
            mModule = null;
            mCapabilityHost = null;
            mPhoneProvider = null;
            mHealthMonitor = null;
            mPositionSource = null;
            mModuleRunning = false;
        }
        if (monitor != null) {
            monitor.stop();
        }
        if (positionSource != null) {
            positionSource.stop();
        }
        if (phoneProvider != null) {
            phoneProvider.stop();
        }
        if (module != null) {
            try {
                module.uninit();
            } catch (Exception e) {
                Log.e(TAG, "module uninit failed", e);
            }
        }
    }

    private void promoteToForeground() {
        final Notification notification = buildNotification(mStatusText);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(FOREGROUND_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(FOREGROUND_ID, notification);
            }
        } catch (Exception e) {
            Log.e(TAG, "startForeground failed", e);
        }
    }

    private Notification buildNotification(final String text) {
        final Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }
        return builder
                .setContentTitle("DroneEngage app module")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_logo2)
                .setOngoing(true)
                .build();
    }

    private void updateNotificationText(final String text) {
        final NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }
        nm.notify(FOREGROUND_ID, buildNotification(text));
    }
}
