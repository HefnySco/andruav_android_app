package ap.andruav_ap.denative.providers.phone;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure state machine behind the "phone" namespace health surface
 * (Mission-Planner-Capabilities-TOBE section 5.6). Holds the published
 * state keys (battery, charging, temp_c, net, rssi_dbm, gps_ok, gps_src)
 * and raises the edge-only facts battery_low {v}, overheat {temp_c},
 * net_lost and net_back with hysteresis from PhoneHealthConfig.
 *
 * Android-free and org.json-free (payloads are plain maps) so the edge
 * logic runs under a plain JUnit test on a fake sensor feed; PhoneProvider
 * adapts it to the databus and PhoneHealthMonitor feeds it on device.
 */
public class PhoneHealthTracker {

    /** Values advertised for state.net. */
    public static final String NET_NONE = "none";
    public static final String NET_WIFI = "wifi";
    public static final String NET_LTE = "lte";
    public static final String NET_5G = "5g";
    public static final String NET_OTHER = "other";

    /** Values advertised for state.gps_src. */
    public static final String GPS_SRC_PHONE = "phone";
    public static final String GPS_SRC_FC = "fc";
    public static final String GPS_SRC_STATIC = "static";
    public static final String GPS_SRC_NONE = "none";

    /** Fact names (fired as "phone.<name>" by the provider). */
    public static final String FACT_BATTERY_LOW = "battery_low";
    public static final String FACT_OVERHEAT = "overheat";
    public static final String FACT_NET_LOST = "net_lost";
    public static final String FACT_NET_BACK = "net_back";

    public interface Listener {
        /** Edge fact on a transition; payload is the fact body (may be empty, not null). */
        void onFact(String event, Map<String, Object> payload);
        /** One or more state keys changed; the provider should republish state. */
        void onStateChanged();
    }

    private final PhoneHealthConfig m_config;
    private final LinkedHashMap<String, Object> m_state = new LinkedHashMap<>();
    private Listener m_listener;

    private boolean m_battery_low_latched;
    private boolean m_overheat_latched;
    /** Last observed net; null until the first sample so the startup
     *  baseline never raises a spurious net_back. */
    private String m_net;

    public PhoneHealthTracker(PhoneHealthConfig config) {
        this.m_config = config != null ? config : PhoneHealthConfig.defaults();
    }

    public synchronized void setListener(Listener listener) {
        this.m_listener = listener;
    }

    /**
     * Battery feed (BatteryManager sticky broadcast): level_percent 0-100,
     * charging per EXTRA_STATUS, temp_c in degrees C.
     * battery_low fires once when crossing down through battery_low_on and
     * the latch re-arms only above battery_low_off; overheat is the same
     * shape on temperature.
     */
    public synchronized void onBattery(int level_percent, boolean charging, double temp_c) {
        boolean changed = _set("battery", level_percent)
                | _set("charging", charging)
                | _set("temp_c", temp_c);

        if (!m_battery_low_latched && level_percent <= m_config.battery_low_on_percent) {
            m_battery_low_latched = true;
            _fire(FACT_BATTERY_LOW, Collections.<String, Object>singletonMap("v", level_percent));
        } else if (m_battery_low_latched && level_percent >= m_config.battery_low_off_percent) {
            m_battery_low_latched = false;
        }

        if (!m_overheat_latched && temp_c >= m_config.overheat_on_c) {
            m_overheat_latched = true;
            _fire(FACT_OVERHEAT, Collections.<String, Object>singletonMap("temp_c", temp_c));
        } else if (m_overheat_latched && temp_c <= m_config.overheat_off_c) {
            m_overheat_latched = false;
        }

        if (changed) _notify_changed();
    }

    /**
     * Network feed: net is one of the NET_* enum values, rssi_dbm the
     * signal strength of that transport (0 when unknown). net_lost fires
     * on connected -> none, net_back on none -> connected; rssi changes
     * inside the configured deadband are folded into the last published
     * value so jitter does not churn the state feed.
     */
    public synchronized void onNetwork(String net, double rssi_dbm) {
        if (net == null) net = NET_NONE;
        boolean net_changed = !net.equals(m_state.get("net"));
        Object last_rssi = m_state.get("rssi_dbm");
        boolean rssi_unknown = !(last_rssi instanceof Number);
        boolean rssi_changed = rssi_unknown
                || Math.abs(((Number) last_rssi).doubleValue() - rssi_dbm) >= m_config.rssi_deadband_dbm;

        boolean changed = _set("net", net);
        if (rssi_changed || net_changed) {
            changed |= _set("rssi_dbm", rssi_dbm);
        }

        if (m_net != null) {
            if (NET_NONE.equals(net) && !NET_NONE.equals(m_net)) {
                _fire(FACT_NET_LOST, Collections.<String, Object>emptyMap());
            } else if (!NET_NONE.equals(net) && NET_NONE.equals(m_net)) {
                _fire(FACT_NET_BACK, Collections.<String, Object>emptyMap());
            }
        }
        m_net = net;

        if (changed) _notify_changed();
    }

    /** GPS feed: gps_ok means the source can currently produce a usable
     *  fix; gps_src is one of the GPS_SRC_* enum values. */
    public synchronized void onGps(boolean gps_ok, String gps_src) {
        boolean changed = _set("gps_ok", gps_ok) | _set("gps_src", gps_src);
        if (changed) _notify_changed();
    }

    /** Current state snapshot (keys appear only once sampled). */
    public synchronized Map<String, Object> snapshot() {
        return new LinkedHashMap<>(m_state);
    }

    private boolean _set(String key, Object value) {
        Object old = m_state.get(key);
        if (old == null ? value == null : old.equals(value)) return false;
        m_state.put(key, value);
        return true;
    }

    private void _fire(String event, Map<String, Object> payload) {
        Listener listener = m_listener;
        if (listener != null) {
            listener.onFact(event, payload != null ? payload : Collections.<String, Object>emptyMap());
        }
    }

    private void _notify_changed() {
        Listener listener = m_listener;
        if (listener != null) listener.onStateChanged();
    }
}
