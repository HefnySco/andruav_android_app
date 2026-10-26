package ap.andruav_ap.denative.providers.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.CellSignalStrength;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Device-side feed for {@link PhoneHealthTracker} (TO-BE section 5.6):
 * battery level/charging/temperature from the ACTION_BATTERY_CHANGED
 * broadcast (re-read each poll so `dumpsys battery` values are always
 * reflected), the network kind from ConnectivityManager callbacks plus a
 * poll of the active network, signal strength from TelephonyManager, and
 * gps_ok/gps_src from the LocationManager GPS provider.
 *
 * Runs a 1 Hz poll on a single thread; battery and network changes also
 * arrive immediately through the receiver/callback. Safe to start/stop
 * more than once; every Android call is permission-guarded so a missing
 * runtime grant degrades the keys to unknowns instead of crashing.
 */
public class PhoneHealthMonitor {

    private static final String TAG = "PhoneHealthMonitor";
    private static final long POLL_INTERVAL_MS = 1000;
    /** A GPS fix older than this no longer counts as gps_ok. */
    private static final long GPS_FRESH_MS = 120000;
    /** WifiInfo.getRssi() returns this when the RSSI is invalid. */
    private static final int WIFI_RSSI_INVALID = -127;

    private final Context m_context;
    private final PhoneHealthTracker m_tracker;

    private ConnectivityManager m_connectivity_manager;
    private TelephonyManager m_telephony_manager;
    private WifiManager m_wifi_manager;
    private LocationManager m_location_manager;

    private ScheduledExecutorService m_executor;
    private BroadcastReceiver m_battery_receiver;
    private ConnectivityManager.NetworkCallback m_network_callback;
    private PhoneStateListener m_phone_state_listener;
    /** Last signal strength seen by the pre-P listener. */
    private volatile SignalStrength m_listener_signal;

    public PhoneHealthMonitor(Context context, PhoneHealthTracker tracker) {
        this.m_context = context.getApplicationContext();
        this.m_tracker = tracker;
    }

    public synchronized void start() {
        if (this.m_executor != null) return;

        this.m_connectivity_manager =
                (ConnectivityManager) m_context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.m_telephony_manager =
                (TelephonyManager) m_context.getSystemService(Context.TELEPHONY_SERVICE);
        this.m_wifi_manager =
                (WifiManager) m_context.getSystemService(Context.WIFI_SERVICE);
        this.m_location_manager =
                (LocationManager) m_context.getSystemService(Context.LOCATION_SERVICE);

        _register_battery();
        _register_network();
        _register_signal_listener();

        // First samples immediately so the state snapshot is never empty.
        _sample_battery_sticky();
        _sample_network();
        _sample_gps();

        this.m_executor = Executors.newSingleThreadScheduledExecutor();
        this.m_executor.scheduleWithFixedDelay(this::_poll_safe,
                POLL_INTERVAL_MS, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
        Log.i(TAG, "phone health monitor started");
    }

    public synchronized void stop() {
        if (this.m_executor != null) {
            this.m_executor.shutdownNow();
            this.m_executor = null;
        }
        if (this.m_battery_receiver != null) {
            try {
                m_context.unregisterReceiver(m_battery_receiver);
            } catch (Exception ignored) {
            }
            this.m_battery_receiver = null;
        }
        if (this.m_network_callback != null && this.m_connectivity_manager != null) {
            try {
                m_connectivity_manager.unregisterNetworkCallback(m_network_callback);
            } catch (Exception ignored) {
            }
            this.m_network_callback = null;
        }
        if (this.m_phone_state_listener != null && this.m_telephony_manager != null) {
            try {
                m_telephony_manager.listen(m_phone_state_listener, PhoneStateListener.LISTEN_NONE);
            } catch (Exception ignored) {
            }
            this.m_phone_state_listener = null;
        }
    }

    // --------------------------------------------------------- battery

    private void _register_battery() {
        this.m_battery_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                _sample_battery(intent);
            }
        };
        try {
            // ACTION_BATTERY_CHANGED is a protected system broadcast, so no
            // RECEIVER_EXPORTED flag is needed on API 33+.
            m_context.registerReceiver(m_battery_receiver,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } catch (Exception e) {
            Log.w(TAG, "battery receiver registration failed; poll still applies", e);
        }
    }

    /** Re-read the sticky battery intent - picks up `dumpsys battery`
     *  overrides even when no broadcast was dispatched to us. */
    private void _sample_battery_sticky() {
        try {
            Intent sticky = m_context.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (sticky != null) _sample_battery(sticky);
        } catch (Exception e) {
            Log.w(TAG, "battery sticky read failed", e);
        }
    }

    private void _sample_battery(Intent intent) {
        try {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int temperature_tenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            if (level < 0 || scale <= 0) return;

            int level_percent = Math.round(level * 100f / scale);
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
            double temp_c = temperature_tenths / 10.0;
            m_tracker.onBattery(level_percent, charging, temp_c);
        } catch (Exception e) {
            Log.w(TAG, "battery sample failed", e);
        }
    }

    // --------------------------------------------------------- network

    private void _register_network() {
        if (m_connectivity_manager == null) return;
        this.m_network_callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                _sample_network();
            }

            @Override
            public void onLost(Network network) {
                _sample_network();
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                _apply_capabilities(caps);
            }
        };
        try {
            m_connectivity_manager.registerDefaultNetworkCallback(m_network_callback);
        } catch (Exception e) {
            Log.w(TAG, "network callback registration failed; poll still applies", e);
            this.m_network_callback = null;
        }
    }

    private void _sample_network() {
        NetworkCapabilities caps = null;
        if (m_connectivity_manager != null) {
            try {
                Network active = m_connectivity_manager.getActiveNetwork();
                if (active != null) {
                    caps = m_connectivity_manager.getNetworkCapabilities(active);
                }
            } catch (Exception e) {
                Log.w(TAG, "active network read failed", e);
            }
        }
        _apply_capabilities(caps);
    }

    private void _apply_capabilities(NetworkCapabilities caps) {
        String net = PhoneHealthTracker.NET_NONE;
        if (caps != null) {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                net = PhoneHealthTracker.NET_WIFI;
            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                net = _cellular_kind();
            } else {
                net = PhoneHealthTracker.NET_OTHER;
            }
        }
        m_tracker.onNetwork(net, _rssi_dbm(net));
    }

    /** Cellular transport mapped onto lte/5g/other from the data network type. */
    private String _cellular_kind() {
        if (m_telephony_manager == null) return PhoneHealthTracker.NET_LTE;
        try {
            int type = m_telephony_manager.getDataNetworkType();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && type == TelephonyManager.NETWORK_TYPE_NR) {
                return PhoneHealthTracker.NET_5G;
            }
            if (type == TelephonyManager.NETWORK_TYPE_LTE) {
                return PhoneHealthTracker.NET_LTE;
            }
        } catch (SecurityException e) {
            // no READ_PHONE_STATE grant: cellular is still at least lte-class
            return PhoneHealthTracker.NET_LTE;
        } catch (Exception e) {
            Log.w(TAG, "data network type read failed", e);
        }
        return PhoneHealthTracker.NET_OTHER;
    }

    // ------------------------------------------------------ signal level

    private void _register_signal_listener() {
        // getSignalStrength() only exists from API 28; below that the
        // (deprecated but working) PhoneStateListener delivers it.
        if (m_telephony_manager == null
                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return;
        this.m_phone_state_listener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                m_listener_signal = signalStrength;
            }
        };
        try {
            m_telephony_manager.listen(m_phone_state_listener,
                    PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        } catch (Exception e) {
            Log.w(TAG, "signal listener registration failed", e);
            this.m_phone_state_listener = null;
        }
    }

    private double _rssi_dbm(String net) {
        if (PhoneHealthTracker.NET_WIFI.equals(net)) {
            return _wifi_dbm();
        }
        if (PhoneHealthTracker.NET_LTE.equals(net)
                || PhoneHealthTracker.NET_5G.equals(net)
                || PhoneHealthTracker.NET_OTHER.equals(net)) {
            return _cell_dbm();
        }
        return 0.0;
    }

    private double _wifi_dbm() {
        if (m_wifi_manager == null) return 0.0;
        try {
            WifiInfo info = m_wifi_manager.getConnectionInfo();
            if (info == null) return 0.0;
            int rssi = info.getRssi();
            return rssi == WIFI_RSSI_INVALID ? 0.0 : rssi;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double _cell_dbm() {
        SignalStrength signal = null;
        if (m_telephony_manager != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                signal = m_telephony_manager.getSignalStrength();
            } catch (Exception ignored) {
            }
        }
        if (signal == null) signal = m_listener_signal;
        if (signal == null) return 0.0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                List<CellSignalStrength> strengths = signal.getCellSignalStrengths();
                if (!strengths.isEmpty()) {
                    return strengths.get(0).getDbm();
                }
            } catch (Exception ignored) {
            }
        }
        // Legacy GSM approximation: dBm = -113 + 2 * asu (asu 99 = unknown).
        int asu = signal.getGsmSignalStrength();
        return asu == 99 ? 0.0 : -113 + 2 * asu;
    }

    // ------------------------------------------------------------- gps

    private void _sample_gps() {
        boolean provider_enabled = false;
        if (m_location_manager != null) {
            try {
                provider_enabled =
                        m_location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception ignored) {
            }
        }
        boolean gps_ok = false;
        if (provider_enabled) {
            try {
                Location last = m_location_manager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER);
                gps_ok = last != null
                        && System.currentTimeMillis() - last.getTime() <= GPS_FRESH_MS;
            } catch (SecurityException ignored) {
                // no location grant: provider is on but no fix is readable
            } catch (Exception e) {
                Log.w(TAG, "last known location failed", e);
            }
        }
        m_tracker.onGps(gps_ok,
                provider_enabled ? PhoneHealthTracker.GPS_SRC_PHONE
                                 : PhoneHealthTracker.GPS_SRC_NONE);
    }

    // ------------------------------------------------------------- poll

    private void _poll_safe() {
        try {
            _sample_battery_sticky();
            _sample_network();
            _sample_gps();
        } catch (Throwable t) {
            Log.w(TAG, "health poll failed", t);
        }
    }
}
