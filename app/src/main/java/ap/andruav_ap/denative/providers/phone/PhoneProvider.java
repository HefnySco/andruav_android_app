package ap.andruav_ap.denative.providers.phone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import de.databus.caps.CapabilityHost;
import de.databus.caps.DeCapabilityProvider;

/**
 * de.cap/1 provider owning the "phone" namespace on the andruav_phone
 * module (Mission-Planner-Capabilities-TOBE section 5.6 / PROTOCOL.md
 * "Android unit namespaces"). This task (B5) delivers the health surface:
 * the state keys battery/charging/temp_c/net/rssi_dbm/gps_ok/gps_src and
 * the edge facts battery_low/overheat/net_lost/net_back. The screen,
 * torch and vibrate output resources of section 5.2 (B4) extend this same
 * advert and provider.
 *
 * Facts go out through {@link CapabilityHost#fireEvent} and state changes
 * through {@link CapabilityHost#publishState}, rate-limited to at most
 * two publishes per second per the spec. The bus connection is attached
 * after construction ({@link #attach}); the sensor feed calls
 * {@link #tracker()} from any thread.
 */
public class PhoneProvider implements DeCapabilityProvider {

    public static final String NS = "phone";

    /** TO-BE section 6: "push deltas on provider change (state <= 2/s)". */
    private static final long MIN_PUBLISH_INTERVAL_MS = 500;

    private static final String ADVERT = "{"
            + "\"schema\":\"de.cap/1\","
            + "\"ns\":\"phone\","
            + "\"module\":\"andruav_phone\","
            + "\"ver\":\"1.0.0\","
            + "\"state\":{"
            + "  \"battery\":{\"type\":\"int\",\"unit\":\"%\"},"
            + "  \"charging\":{\"type\":\"bool\"},"
            + "  \"temp_c\":{\"type\":\"number\"},"
            + "  \"net\":{\"type\":\"enum\",\"values\":[\"none\",\"wifi\",\"lte\",\"5g\",\"other\"]},"
            + "  \"rssi_dbm\":{\"type\":\"number\",\"unit\":\"dBm\"},"
            + "  \"gps_ok\":{\"type\":\"bool\"},"
            + "  \"gps_src\":{\"type\":\"enum\",\"values\":[\"phone\",\"fc\",\"static\",\"none\"]}"
            + "},"
            + "\"events\":{"
            + "  \"battery_low\":{\"payload\":{\"v\":{\"type\":\"int\"}}},"
            + "  \"overheat\":{\"payload\":{\"temp_c\":{\"type\":\"number\"}}},"
            + "  \"net_lost\":{},"
            + "  \"net_back\":{}"
            + "}"
            + "}";

    private final PhoneHealthTracker m_tracker;
    private final Object m_publish_lock = new Object();

    private CapabilityHost m_host;
    private ScheduledExecutorService m_scheduler;
    private long m_last_publish_ms;
    private boolean m_publish_pending;
    /** First bus publish after attach() goes out flagged full. */
    private boolean m_full_published;

    public PhoneProvider(PhoneHealthConfig config) {
        this.m_tracker = new PhoneHealthTracker(config);
        this.m_tracker.setListener(new PhoneHealthTracker.Listener() {
            @Override
            public void onFact(String event, Map<String, Object> payload) {
                _fire_fact(event, payload);
            }

            @Override
            public void onStateChanged() {
                _request_publish();
            }
        });
    }

    /** Sensor feed handle — the monitor pushes samples into this. */
    public PhoneHealthTracker tracker() {
        return this.m_tracker;
    }

    /** Wire the bus host; called by the service after CapabilityHost.start(). */
    public void attach(CapabilityHost host) {
        synchronized (m_publish_lock) {
            this.m_host = host;
            if (this.m_scheduler == null) {
                this.m_scheduler = Executors.newSingleThreadScheduledExecutor();
            }
        }
    }

    /** Stop the publish scheduler; called when the module service stops. */
    public void stop() {
        synchronized (m_publish_lock) {
            this.m_host = null;
            if (this.m_scheduler != null) {
                this.m_scheduler.shutdownNow();
                this.m_scheduler = null;
            }
            this.m_publish_pending = false;
        }
    }

    @Override
    public List<Object> adverts() {
        List<Object> adverts = new ArrayList<>(1);
        adverts.add(ADVERT);
        return adverts;
    }

    @Override
    public void invoke(String id, String ns, String act, JSONObject params,
                       long deadlineMs, ResultSink sink) {
        // B4 adds the screen/torch/vibrate actions to this namespace.
        sink.err("unknown action " + ns + "." + act);
    }

    @Override
    public void onEvent(String name, JSONObject ed, String nonce) {
        // The phone namespace produces facts; no inbound commands consume them yet.
    }

    @Override
    public JSONObject state(String ns) {
        return new JSONObject(this.m_tracker.snapshot());
    }

    private void _fire_fact(String event, Map<String, Object> payload) {
        final CapabilityHost host;
        synchronized (m_publish_lock) {
            host = this.m_host;
        }
        if (host == null) return;
        JSONObject body = payload != null && !payload.isEmpty()
                ? new JSONObject(payload) : new JSONObject();
        host.fireEvent(NS, event, body);
    }

    /** Publish the snapshot at most once per MIN_PUBLISH_INTERVAL_MS;
     *  changes arriving inside the window are coalesced into the pending
     *  publish at the window edge. */
    private void _request_publish() {
        synchronized (m_publish_lock) {
            if (this.m_host == null || this.m_scheduler == null) return;
            long now = System.currentTimeMillis();
            long earliest = this.m_last_publish_ms + MIN_PUBLISH_INTERVAL_MS;
            if (now >= earliest) {
                this.m_last_publish_ms = now;
                this.m_scheduler.execute(this::_publish_state);
            } else if (!this.m_publish_pending) {
                this.m_publish_pending = true;
                this.m_scheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (m_publish_lock) {
                            m_publish_pending = false;
                            m_last_publish_ms = System.currentTimeMillis();
                        }
                        _publish_state();
                    }
                }, earliest - now, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void _publish_state() {
        final CapabilityHost host;
        final boolean full;
        synchronized (m_publish_lock) {
            host = this.m_host;
            full = !this.m_full_published;
            this.m_full_published = true;
        }
        if (host != null) host.publishState(NS, full);
    }
}
