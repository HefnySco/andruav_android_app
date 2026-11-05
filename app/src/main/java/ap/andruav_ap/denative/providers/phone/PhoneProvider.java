package ap.andruav_ap.denative.providers.phone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.databus.caps.CapabilityHost;
import de.databus.caps.DeCapabilityProvider;

/**
 * de.cap/1 provider owning the "phone" namespace on the andruav_phone
 * module (Mission-Planner-Capabilities-TOBE sections 5.2/5.6 / PROTOCOL.md
 * "Android unit namespaces").
 *
 * B4 surface - the named {@code out}-class resources screen (strobe
 * activity), torch and vibrate:
 *  - commands {@code phone.<key>.on|off|toggle|pulse {ms?}} (PF-02
 *    {@code event_patterns}, kind "command", res ["out"]);
 *  - facts {@code phone.<key>.went_on|went_off} {@code {v, src}};
 *  - actions {@code phone.<key> {on, pulse_ms?}} flagged
 *    {@code claim:"action"}, plus the reserved generic actions
 *    {@code write {pin, value}} and {@code pulse {pin, length_ms}}.
 * Every output request lands on the {@link PhoneOutputBank} choke points,
 * which the legacy Emergency surface shares - GCS buttons, missions and
 * emergencies drive one state and emit one set of facts.
 *
 * B5 surface - the device-health state keys battery/charging/temp_c/
 * net/rssi_dbm/gps_ok/gps_src and the edge facts battery_low/overheat/
 * net_lost/net_back raised by {@link PhoneHealthTracker}.
 *
 * AN-0128 SMS surface - the configured trigger keywords as class-"sms"
 * resources {@code sms_<kw>} under the {@code *.received} fact pattern
 * ({@code {contact, age_s}} - the payload never carries a number or the
 * message text), plus the {@code phone.sms_send {contact, text}} action,
 * rate-limited per {@link PhoneSmsConfig} so a mission loop cannot send
 * without limit. Facts arrive from the SMS parser through
 * {@link #onSmsKeyword}; with no attached host the event is dropped.
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
    private final PhoneOutputBank m_bank;
    private final PhoneSmsConfig m_sms;
    private final Object m_publish_lock = new Object();
    private final Set<String> m_ignored_once = new HashSet<>();
    /** sms_send send times inside the rate window (config send_limit). */
    private final List<Long> m_sms_sends = new ArrayList<>();

    private CapabilityHost m_host;
    private ScheduledExecutorService m_scheduler;
    private long m_last_publish_ms;
    private boolean m_publish_pending;
    /** First bus publish after attach() goes out flagged full. */
    private boolean m_full_published;

    /** Health-only provider (no output resources wired). */
    public PhoneProvider(PhoneHealthConfig config) {
        this(config, null, null);
    }

    public PhoneProvider(PhoneHealthConfig config, final PhoneOutputBank bank) {
        this(config, bank, null);
    }

    public PhoneProvider(PhoneHealthConfig config, final PhoneOutputBank bank,
                         final PhoneSmsConfig sms) {
        this.m_sms = sms;
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
        this.m_bank = bank;
        if (this.m_bank != null) {
            // output edges emit their fact and republish state; the host
            // may not exist yet, in which case the edge just drives the
            // hardware (Emergency's flash still works pre-attach)
            this.m_bank.setFactEmitter((key, verb, v, src) -> {
                _fire_fact(key + "." + verb, _kv("v", v, "src", src));
                _request_publish();
            });
        }
    }

    /** Sensor feed handle - the monitor pushes samples into this. */
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
        adverts.add(_build_advert());
        return adverts;
    }

    /**
     * The health advert extended with the output surface of TO-BE 5.2:
     * per-resource actions (claim "action"), the reserved write/pulse
     * actions, the PF-02 resources/event_patterns and one int state key
     * per resource.
     */
    private JSONObject _build_advert() {
        try {
            final JSONObject advert = new JSONObject(ADVERT);
            final JSONObject actions = new JSONObject();
            final JSONObject resources = new JSONObject();
            final JSONObject patterns = new JSONObject();
            if (m_bank != null) {
            final JSONObject state = advert.getJSONObject("state");
            for (final PhoneOutputSpec spec : m_bank.specs()) {
                resources.put(spec.name, PhoneOutputSpec.CLASS_OUT);
                state.put(spec.name, _kv("type", "int"));
                // "write"/"pulse" are reserved names: such a key keeps its
                // events but gets no per-resource action (same as gpio pins)
                if (!"write".equals(spec.name) && !"pulse".equals(spec.name)) {
                    actions.put(spec.name, _kv(
                            "desc", spec.kind + " output",
                            "claim", "action",
                            "timeout_s", 5,
                            "params", _kv(
                                    "on", _kv("type", "bool"),
                                    "pulse_ms", _kv(
                                            "type", "int",
                                            "min", 1,
                                            "max", PhoneOutput.PULSE_MS_MAX,
                                            "unit", "ms",
                                            "optional", true))));
                }
            }
            actions.put("write", _kv("params", _kv(
                    "pin", _kv("type", new JSONArray().put("string").put("int"),
                            "desc", "configured resource name"),
                    "value", _kv("type", "bool"))));
            actions.put("pulse", _kv(
                    "timeout_s", 5,
                    "params", _kv(
                            "pin", _kv("type",
                                    new JSONArray().put("string").put("int"),
                                    "desc", "configured resource name"),
                            "length_ms", _kv("type", "int", "min", 1,
                                    "max", PhoneOutput.PULSE_MS_MAX,
                                    "unit", "ms"))));
            patterns.put("*.on", _command_pattern());
            patterns.put("*.off", _command_pattern());
            patterns.put("*.toggle", _command_pattern());
            patterns.put("*.pulse", _command_pattern()
                    .put("payload", _kv("ms", _kv(
                            "type", "int", "min", 1,
                            "max", PhoneOutput.PULSE_MS_MAX,
                            "unit", "ms", "optional", true))));
            patterns.put("*.went_on", _fact_pattern());
            patterns.put("*.went_off", _fact_pattern());
            }

            // AN-0128: sms_<kw> resources (class "sms") + the *.received
            // fact pattern + the rate-limited sms_send action.
            if (m_sms != null) {
                for (final String keyword : m_sms.keywords) {
                    resources.put("sms_" + keyword, "sms");
                }
                patterns.put("*.received", _kv(
                        "kind", "fact",
                        "res", new JSONArray().put("sms"),
                        "payload", _kv(
                                "contact", _kv("type", "string"),
                                "age_s", _kv("type", "int"))));
                actions.put("sms_send", _kv(
                        "desc", "send an SMS to a configured contact",
                        "params", _kv(
                                "contact", _kv("type", "string",
                                        "desc", "configured contact name"),
                                "text", _kv("type", "string"))));
            }

            if (actions.length() > 0) advert.put("actions", actions);
            if (resources.length() > 0) advert.put("resources", resources);
            if (patterns.length() > 0) advert.put("event_patterns", patterns);
            advert.put("desc", "commands: phone.<key>.on|off|toggle|pulse {ms? 1.."
                    + PhoneOutput.PULSE_MS_MAX
                    + "}; facts: phone.<key>.went_on|went_off {v,src}; "
                    + "facts: battery_low {v}, overheat {temp_c}, net_lost, net_back"
                    + "; facts: phone.sms_<kw>.received {contact,age_s}; "
                    + "action: phone.sms_send {contact,text}");
            return advert;
        } catch (JSONException e) {
            // a literal advert never fails; a bad merge keeps the health surface
            try {
                return new JSONObject(ADVERT);
            } catch (JSONException e2) {
                return new JSONObject();
            }
        }
    }

    /**
     * 6543 invoke. Runs on the module's invoke worker; a pulse blocks it
     * until the restore ran ({@code timeout_s: 5} covers the 4 s max).
     */
    @Override
    public void invoke(String id, String ns, String act, JSONObject params,
                       long deadlineMs, ResultSink sink) {
        // AN-0128: sms_send needs no output bank - it can run on a
        // health-only provider.
        if ("sms_send".equals(act)) {
            _invoke_sms_send(params, sink);
            return;
        }
        if (m_bank == null) {
            sink.err("unknown action " + ns + "." + act);
            return;
        }
        try {
            switch (act) {
                case "write": {
                    final PhoneOutput output = _resolve_output(params.opt("pin"), sink);
                    if (output == null) return;
                    final String err = output.write(params.optBoolean("value"),
                            PhoneOutput.SRC_INVOKE);
                    _answer_write(output, err, sink);
                    return;
                }
                case "pulse": {
                    final PhoneOutput output = _resolve_output(params.opt("pin"), sink);
                    if (output == null) return;
                    final String err = output.pulseBlocking(
                            params.optInt("length_ms", output.spec().pulseMs));
                    _answer_write(output, err, sink);
                    return;
                }
                default:
                    break;
            }

            final PhoneOutput output = m_bank.output(act);
            if (output == null) {
                sink.err("unknown action " + ns + "." + act);
                return;
            }

            if (!params.has("on")) {
                sink.err(NS + "." + act + " needs 'on' (bool)");
                return;
            }
            final boolean on = params.optBoolean("on");
            if (params.has("pulse_ms")) {
                // a pulse asserts the on-level; 'on' must be true (gpio rule)
                if (!on) {
                    sink.err(NS + "." + act
                            + ": a pulse asserts the on-level, 'on' must be true");
                    return;
                }
                final String err = output.pulseBlocking(params.optInt("pulse_ms"));
                _answer_write(output, err, sink);
                return;
            }

            final String err = output.write(on, PhoneOutput.SRC_INVOKE);
            _answer_write(output, err, sink);
        } catch (Exception e) {
            sink.err(NS + "." + act + ": " + e);
        }
    }

    /**
     * Locally delivered Sync_EventFire {@code d = "phone.<key>.<verb>"}.
     * Runs on the module receive thread - never blocks: a pulse command
     * restores through the bank's scheduler.
     */
    @Override
    public void onEvent(String name, JSONObject ed, String nonce) {
        if (m_bank == null || name == null || !name.startsWith(NS + ".")) return;
        final String key_verb = name.substring(NS.length() + 1);
        final int dot = key_verb.lastIndexOf('.');
        if (dot <= 0 || dot == key_verb.length() - 1) return;
        final String key = key_verb.substring(0, dot);
        final String verb = key_verb.substring(dot + 1);

        final Integer ms = (ed != null && ed.has("ms")) ? ed.optInt("ms") : null;
        final String reason = m_bank.command(key, verb, ms, PhoneOutput.SRC_EVENT);
        if (reason != null) {
            _log_ignored_once(key_verb, reason);
        }
    }

    @Override
    public JSONObject state(String ns) {
        final JSONObject snapshot = new JSONObject(this.m_tracker.snapshot());
        if (m_bank != null) {
            try {
                for (final Map.Entry<String, Integer> entry : m_bank.stateMap().entrySet()) {
                    snapshot.put(entry.getKey(), entry.getValue().intValue());
                }
            } catch (JSONException ignored) {
            }
        }
        return snapshot;
    }

    /**
     * AN-0128: an allowed contact sent ATT:&lt;kw&gt;:&lt;pin&gt; and the
     * parser matched the configured keyword - fire
     * {@code phone.sms_<kw>.received {contact, age_s}}. Returns false
     * when the event cannot be delivered (unknown keyword here, or no
     * bus host attached) so the caller can tell the sender it failed.
     */
    public boolean onSmsKeyword(final String keyword, final String contact,
                                final long age_s) {
        if (m_sms == null || keyword == null
                || !m_sms.keywords.contains(keyword)) {
            return false;
        }
        final CapabilityHost host;
        synchronized (m_publish_lock) {
            host = this.m_host;
        }
        if (host == null) return false;
        host.fireEvent(NS, "sms_" + keyword + ".received",
                _kv("contact", contact == null ? "" : contact,
                        "age_s", age_s));
        return true;
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    /** phone.sms_send {contact, text}: the contact is a name from the
     *  configured contact book - never a number - and the action is
     *  rate-limited so a mission loop cannot send without limit. */
    private void _invoke_sms_send(final JSONObject params, final ResultSink sink) {
        if (m_sms == null || m_sms.sender == null) {
            sink.err("phone.sms_send not configured");
            return;
        }
        final String contact = params.optString("contact", "").trim().toLowerCase();
        final String text = params.optString("text", "");
        if (contact.isEmpty()) {
            sink.err("phone.sms_send needs 'contact'");
            return;
        }
        if (text.isEmpty()) {
            sink.err("phone.sms_send needs 'text'");
            return;
        }
        if (contact.matches("^\\+?[0-9()\\-. ]+$")) {
            sink.err("phone.sms_send needs a contact name, not a number");
            return;
        }
        final String number = m_sms.contacts.get(contact);
        if (number == null) {
            sink.err("phone.sms_send: unknown contact '" + contact + "'");
            return;
        }
        final long now = System.currentTimeMillis();
        synchronized (m_sms_sends) {
            while (!m_sms_sends.isEmpty()
                    && m_sms_sends.get(0) < now - m_sms.send_window_ms) {
                m_sms_sends.remove(0);
            }
            if (m_sms_sends.size() >= m_sms.send_limit) {
                sink.err("phone.sms_send rate limit ("
                        + m_sms.send_limit + " per "
                        + (m_sms.send_window_ms / 1000) + "s)");
                return;
            }
            m_sms_sends.add(now);
        }
        m_sms.sender.send(number, text);
        sink.ok(_kv("contact", contact, "sent", true));
    }

    /**
     * org.json in the app module declares a checked JSONException on
     * put() (unlike the maven jar deDatabus tests compile against);
     * key/value pairs -> JSONObject without the throws boilerplate.
     * A JSONException on literal data is impossible in practice.
     */
    private static JSONObject _kv(final Object... kv) {
        final JSONObject object = new JSONObject();
        try {
            for (int i = 0; i + 1 < kv.length; i += 2) {
                object.put(String.valueOf(kv[i]), kv[i + 1]);
            }
        } catch (JSONException ignored) {
        }
        return object;
    }

    /** "pin" is a configured resource name; phone outputs have no numbered form. */
    private PhoneOutput _resolve_output(final Object pin, final ResultSink sink) {
        if (pin instanceof String) {
            final PhoneOutput output = m_bank.output((String) pin);
            if (output != null) return output;
        }
        sink.err("unknown resource " + (pin == null ? "null" : pin));
        return null;
    }

    private void _answer_write(final PhoneOutput output, final String err,
                               final ResultSink sink) {
        if (err != null) {
            sink.err(err);
            return;
        }
        sink.ok(_kv("pin", output.spec().name,
                "on", output.isOn(),
                "value", output.isOn() ? 1 : 0));
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

    private void _fire_fact(String event, JSONObject payload) {
        final CapabilityHost host;
        synchronized (m_publish_lock) {
            host = this.m_host;
        }
        if (host == null) return;
        host.fireEvent(NS, event, payload);
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

    private static JSONObject _command_pattern() {
        return _kv("kind", "command", "res",
                new JSONArray().put(PhoneOutputSpec.CLASS_OUT));
    }

    private static JSONObject _fact_pattern() {
        return _kv("kind", "fact",
                "payload", _kv("v", _kv("type", "int"),
                        "src", _kv("type", "string")));
    }

    /** log each distinct ignored event command once (gpio's logEventOnce). */
    private void _log_ignored_once(final String key_verb, final String reason) {
        final String tag = key_verb + ": " + reason;
        synchronized (m_ignored_once) {
            if (!m_ignored_once.add(tag)) return;
        }
        System.out.println("phone event ignored " + tag);
    }
}
