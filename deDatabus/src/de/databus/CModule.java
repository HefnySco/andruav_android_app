package de.databus;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

import static de.databus.Messages.*;

/**
 * Java port of de_databus/python/de_module.py — module registration,
 * send helpers, the intermodule receive path, and the Phase-3 capability
 * helpers (de.cap/1 adverts, invoke/result idempotency, state, events).
 */
public class CModule {

    public static final String MODULE_FEATURE_RECEIVING_TELEMETRY = "R";
    public static final String MODULE_FEATURE_SENDING_TELEMETRY = "T";
    public static final String MODULE_FEATURE_CAPTURE_IMAGE = "C";
    public static final String MODULE_FEATURE_CAPTURE_VIDEO = "V";
    public static final String MODULE_FEATURE_GPIO = "G";
    public static final String MODULE_FEATURE_AI_RECOGNITION = "A";
    public static final String MODULE_FEATURE_TRACKING = "K";
    public static final String MODULE_FEATURE_P2P = "P";

    public static final String MODULE_CLASS_COMM = "comm";
    public static final String MODULE_CLASS_FCB = "fcb";
    public static final String MODULE_CLASS_VIDEO = "camera";
    public static final String MODULE_CLASS_P2P = "p2p";
    public static final String MODULE_CLASS_GENERIC = "gen";
    public static final String MODULE_CLASS_GPIO = "gpio";
    public static final String MODULE_CLASS_A_RECOGNITION = "ai_rec";
    public static final String MODULE_CLASS_TRACKING = "trk";
    public static final String MODULE_CLASS_VIEWLINK = "vlk";

    public static final int HARDWARE_TYPE_UNDEFINED = 0;
    public static final int HARDWARE_TYPE_CPU = 1;

    public static final int DEFAULT_UDP_DATABUS_PACKET_SIZE = 8192;
    public static final int SAFE_REMOTE_UDP_PAYLOAD = 1472;

    /** Callback receiving every routed message (raw + parsed header). */
    public interface OnReceiveCallback {
        void onReceive(byte[] message, int len, JSONObject jMsg);
    }

    /** Capability action handler: returns {data, err} — err null/empty on success.
     *  deadlineMs is the wall-clock time (epoch millis) the invoke must be
     *  answered by - the same budget _handle_invoke enforces. */
    public interface InvokeHandler {
        InvokeResult onInvoke(String id, String ns, String act,
                              Map<String, Object> params, long deadlineMs);
    }

    public static final class InvokeResult {
        public final Object data;
        public final String err;
        public InvokeResult(Object data, String err) { this.data = data; this.err = err; }
        public static InvokeResult ok(Object data) { return new InvokeResult(data, null); }
        public static InvokeResult fail(String err) { return new InvokeResult(null, err); }
    }

    private String m_module_class = "";
    private String m_module_id = "";
    private String m_module_key = "";
    private String m_module_version = "";
    private Object m_message_filter = new JSONArray();
    public CUDPClient cUDPClient;
    private String m_party_id = "";
    private String m_group_id = "";
    private OnReceiveCallback m_OnReceive;
    private final Map<String, Object> m_stdinValues = new LinkedHashMap<>();
    private boolean m_FirstReceived = false;
    private final List<String> m_module_features = new ArrayList<>();
    private String m_hardware_serial = "";
    private int m_hardware_serial_type = 0;
    private final double m_instance_time_stamp = System.currentTimeMillis() / 1000.0;
    private final Object m_lock = new Object();
    // Phase-3 capability helper state
    private static final int MAX_INVOKE_RESULTS = 256;
    private List<String> m_capability_adverts = new ArrayList<>();
    private List<JSONObject> m_capability_parsed = new ArrayList<>();
    private String m_capability_hash = "";
    private InvokeHandler m_on_invoke = null;
    private boolean m_capabilities_sent = false;
    private final Map<String, JSONObject> m_invoke_results = new LinkedHashMap<>();
    private final java.util.Set<String> m_invoke_inflight = ConcurrentHashMap.newKeySet();
    private final Object m_cap_lock = new Object();
    private int m_event_counter = 0;

    public boolean init(String target_ip, int broadcasts_port, String host,
                        int listening_port, int chunk_size) throws java.io.IOException {
        // chunk_size <= 0: auto-detect like the C++ CModule - loopback uses
        // the full default, other targets are capped at
        // SAFE_REMOTE_UDP_PAYLOAD to avoid IP fragmentation.
        if (chunk_size <= 0) {
            if ("127.0.0.1".equals(target_ip) || "::1".equals(target_ip) || "localhost".equals(target_ip)) {
                chunk_size = DEFAULT_UDP_DATABUS_PACKET_SIZE;
            } else {
                chunk_size = SAFE_REMOTE_UDP_PAYLOAD;
            }
            System.out.println("Auto chunk size: " + chunk_size + " (target " + target_ip + ")");
        }

        this.cUDPClient = new CUDPClient();
        this.cUDPClient.init(target_ip, broadcasts_port, host, listening_port, chunk_size, this::onReceive);
        createJSONID(true);
        this.cUDPClient.start();
        return true;
    }

    public boolean uninit() {
        this.cUDPClient.stop();
        return true;
    }

    public void defineModule(String module_class, String module_id, String module_key,
                             String module_version, Object message_filter) {
        this.m_module_class = module_class;
        this.m_module_id = module_id;
        this.m_module_key = module_key;
        this.m_module_version = module_version;
        this.m_message_filter = message_filter;
    }

    public void add_module_features(String feature) {
        this.m_module_features.add(feature);
    }

    public void set_hardware(String hardware_serial, int hardware_serial_type) {
        this.m_hardware_serial = hardware_serial;
        this.m_hardware_serial_type = hardware_serial_type;
    }

    public void setOnReceive(OnReceiveCallback cb) {
        this.m_OnReceive = cb;
    }

    public void sendMSG(byte[] msg, int length) {
        this.cUDPClient.sendMSG(msg, length);
    }

    public void send_sys_msg(JSONObject jmsg, int andruav_message_id) {
        JSONObject full_message = new JSONObject();
        full_message.put(ANDRUAV_PROTOCOL_TARGET_ID, ANDRUAV_PROTOCOL_SENDER_COMM_SERVER);
        full_message.put(INTERMODULE_ROUTING_TYPE, CMD_COMM_SYSTEM);
        full_message.put(ANDRUAV_PROTOCOL_MESSAGE_TYPE, andruav_message_id);
        full_message.put(ANDRUAV_PROTOCOL_MESSAGE_CMD, jmsg);
        byte[] msg = full_message.toString().getBytes(StandardCharsets.UTF_8);
        sendMSG(msg, msg.length);
    }

    public void sendJMSG(String targetPartyID, JSONObject jmsg, int andruav_message_id,
                         boolean internal_message) {
        synchronized (m_lock) {
            String msg_routing_type = CMD_COMM_GROUP;
            if (internal_message) {
                msg_routing_type = CMD_TYPE_INTERMODULE;
            } else if (targetPartyID != null && !targetPartyID.isEmpty()) {
                msg_routing_type = CMD_COMM_INDIVIDUAL;
            }

            JSONObject fullMessage = new JSONObject();
            fullMessage.put(INTERMODULE_MODULE_KEY, this.m_module_key);
            fullMessage.put(ANDRUAV_PROTOCOL_TARGET_ID, targetPartyID);
            fullMessage.put(INTERMODULE_ROUTING_TYPE, msg_routing_type);
            fullMessage.put(ANDRUAV_PROTOCOL_MESSAGE_TYPE, andruav_message_id);
            fullMessage.put(ANDRUAV_PROTOCOL_MESSAGE_CMD, jmsg);
            mergeEnvelope(fullMessage, m_outbound_envelope);

            byte[] msg = fullMessage.toString().getBytes(StandardCharsets.UTF_8);
            sendMSG(msg, msg.length);
        }
    }

    public void sendBMSG(String targetPartyID, byte[] bmsg, int bmsg_length,
                         int andruav_message_id, boolean internal_message, Object message_cmd) {
        synchronized (m_lock) {
            String msg_routing_type = CMD_COMM_GROUP;
            if (internal_message) {
                msg_routing_type = CMD_TYPE_INTERMODULE;
            } else if (targetPartyID != null && !targetPartyID.isEmpty()) {
                msg_routing_type = CMD_COMM_INDIVIDUAL;
            }

            JSONObject fullMessage = new JSONObject();
            fullMessage.put(INTERMODULE_MODULE_KEY, this.m_module_key);
            fullMessage.put(ANDRUAV_PROTOCOL_TARGET_ID, targetPartyID);
            fullMessage.put(INTERMODULE_ROUTING_TYPE, msg_routing_type);
            fullMessage.put(ANDRUAV_PROTOCOL_MESSAGE_TYPE, andruav_message_id);
            fullMessage.put(ANDRUAV_PROTOCOL_MESSAGE_CMD, message_cmd);
            mergeEnvelope(fullMessage, m_outbound_envelope);

            byte[] json_msg = fullMessage.toString().getBytes(StandardCharsets.UTF_8);
            byte[] msg = new byte[json_msg.length + 1 + bmsg_length];
            System.arraycopy(json_msg, 0, msg, 0, json_msg.length);
            if (bmsg_length > 0) {
                System.arraycopy(bmsg, 0, msg, json_msg.length + 1, bmsg_length);
            }
            sendMSG(msg, msg.length);
        }
    }

    /** Extra top-level fields stamped onto every sendJMSG/sendBMSG datagram. */
    private final JSONObject m_outbound_envelope = new JSONObject();

    /**
     * Stamp a top-level field onto every outgoing comm datagram built by
     * {@link #sendJMSG}/{@link #sendBMSG} - e.g. {@code p}, the sender
     * permission the comm server would inject on a wire message. Use for
     * fields the module asserts for itself; {@code ms} content is untouched.
     */
    public void appendOutboundEnvelopeField(String name, Object value) {
        this.m_outbound_envelope.put(name, value);
    }

    /*** Read-only view of the stamped outbound envelope fields (tests). */
    public JSONObject getOutboundEnvelope() {
        return m_outbound_envelope;
    }

    /** Merge caller-supplied envelope fields into an outgoing datagram (null-safe). */
    private static void mergeEnvelope(JSONObject fullMessage, JSONObject extraEnvelope) {
        if (extraEnvelope == null) return;
        java.util.Iterator<String> keys = extraEnvelope.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            fullMessage.put(key, extraEnvelope.opt(key));
        }
    }

    public void sendMREMSG(int command_type) {
        synchronized (m_lock) {
            JSONObject json_msg = new JSONObject();
            json_msg.put(INTERMODULE_MODULE_KEY, this.m_module_key);
            json_msg.put(INTERMODULE_ROUTING_TYPE, CMD_TYPE_INTERMODULE);
            json_msg.put(ANDRUAV_PROTOCOL_MESSAGE_TYPE, TYPE_AndruavModule_RemoteExecute);
            json_msg.put(ANDRUAV_PROTOCOL_MESSAGE_CMD, new JSONObject().put("C", command_type));
            byte[] msg = json_msg.toString().getBytes(StandardCharsets.UTF_8);
            sendMSG(msg, msg.length);
        }
    }

    public void forwardMSG(byte[] message, int datalength) {
        sendMSG(message, datalength);
    }

    /** Extra fields merged into the module ID message by createJSONID. */
    public void appendExtraField(String name, Object ms) {
        this.m_stdinValues.put(name, ms);
    }

    /** Called from CUDPClient when a complete message has been received. */
    public void onReceive(byte[] message, int len) {
        try {
            // The JSON header is always null-terminated; any binary payload
            // (not currently used) would follow the null terminator.
            int null_index = -1;
            for (int i = 0; i < len; i++) {
                if (message[i] == 0) { null_index = i; break; }
            }
            byte[] json_part = (null_index != -1)
                    ? java.util.Arrays.copyOf(message, null_index) : message;
            JSONObject jMsg = new JSONObject(new String(json_part, StandardCharsets.UTF_8));

            if (!jMsg.has(ANDRUAV_PROTOCOL_MESSAGE_TYPE)) return;
            if (!jMsg.has(INTERMODULE_ROUTING_TYPE)) return;

            if (CMD_TYPE_INTERMODULE.equals(jMsg.optString(INTERMODULE_ROUTING_TYPE))) {
                // These messages are sent by other modules to be consumed only
                // by modules and not to be sent outside DroneEngage unit.
                if (!jMsg.has(ANDRUAV_PROTOCOL_MESSAGE_CMD)) return;

                Object cmdObj = jMsg.get(ANDRUAV_PROTOCOL_MESSAGE_CMD);
                int message_type = jMsg.optInt(ANDRUAV_PROTOCOL_MESSAGE_TYPE);
                if (message_type == TYPE_AndruavModule_ID) {
                    if (!(cmdObj instanceof JSONObject)) return;
                    JSONObject cmd = (JSONObject) cmdObj;
                    if (!cmd.has(JSON_INTERMODULE_PARTY_RECORD)) return;
                    JSONObject moduleID = cmd.getJSONObject(JSON_INTERMODULE_PARTY_RECORD);
                    if (!moduleID.has(ANDRUAV_PROTOCOL_SENDER)) return;
                    if (!moduleID.has(ANDRUAV_PROTOCOL_GROUP_ID)) return;

                    this.m_party_id = moduleID.getString(ANDRUAV_PROTOCOL_SENDER);
                    this.m_group_id = moduleID.getString(ANDRUAV_PROTOCOL_GROUP_ID);

                    if (!this.m_FirstReceived) {
                        System.out.println(" ** Communicator Server Found: m_party_id("
                                + this.m_party_id + ") m_group_id(" + this.m_group_id + ")");
                        createJSONID(false);
                        this.m_FirstReceived = true;
                        // Phase-3: publish the advert once on registration
                        if (!this.m_capabilities_sent && !this.m_capability_adverts.isEmpty()) {
                            this.m_capabilities_sent = true;
                            _send_capabilities();
                        }
                    }

                    if (this.m_OnReceive != null) {
                        this.m_OnReceive.onReceive(message, len, jMsg);
                    }
                    return;
                } else if (message_type == TYPE_AndruavMessage_MODULE_CAPABILITIES) {
                    // de_comm asking for the advert (P3-01). Helper-owned:
                    // not forwarded to the module's m_OnReceive.
                    if (cmdObj instanceof JSONObject && ((JSONObject) cmdObj).has("r")) {
                        _send_capabilities();
                    }
                    return;
                } else if (message_type == TYPE_AndruavMessage_CAPABILITY_INVOKE) {
                    if (cmdObj instanceof JSONObject) _handle_invoke((JSONObject) cmdObj);
                    return;
                } else if (message_type == TYPE_AndruavMessage_DUMMY) {
                    System.out.println(" TYPE_AndruavMessage_DUMMY " + new String(message, 0, len, StandardCharsets.UTF_8));
                }
            }

            if (this.m_OnReceive != null) {
                this.m_OnReceive.onReceive(message, len, jMsg);
            }
        } catch (Exception e) {
            System.out.println("ERROR:" + e);
        }
    }

    /** Create TYPE_AndruavModule_ID - JSON message. Essential to identify
     *  the module to de-Communicator. */
    public void createJSONID(boolean reSend) {
        JSONObject json_msg = new JSONObject();
        json_msg.put(INTERMODULE_ROUTING_TYPE, CMD_TYPE_INTERMODULE);
        json_msg.put(ANDRUAV_PROTOCOL_MESSAGE_TYPE, TYPE_AndruavModule_ID);
        JSONObject ms = new JSONObject();

        ms.put(JSON_INTERMODULE_MODULE_ID, this.m_module_id);
        ms.put(JSON_INTERMODULE_MODULE_CLASS, this.m_module_class);
        ms.put(JSON_INTERMODULE_MODULE_MESSAGES_LIST, this.m_message_filter);
        ms.put(JSON_INTERMODULE_MODULE_FEATURES, new JSONArray(this.m_module_features));
        ms.put(JSON_INTERMODULE_MODULE_KEY, this.m_module_key);
        // Only include hardware fields when a hardware serial has been set
        // (via set_hardware). The C++ communicator enters its license-check
        // branch when JSON_INTERMODULE_HARDWARE_ID is present, and expects
        // JSON_INTERMODULE_HARDWARE_TYPE to be an int.
        if (!this.m_hardware_serial.isEmpty()) {
            ms.put(JSON_INTERMODULE_HARDWARE_ID, this.m_hardware_serial);
            ms.put(JSON_INTERMODULE_HARDWARE_TYPE, this.m_hardware_serial_type);
        }
        ms.put(JSON_INTERMODULE_VERSION, this.m_module_version);
        ms.put(JSON_INTERMODULE_RESEND, reSend);
        ms.put(JSON_INTERMODULE_TIMESTAMP_INSTANCE, this.m_instance_time_stamp);

        for (Map.Entry<String, Object> e : this.m_stdinValues.entrySet()) {
            ms.put(e.getKey(), e.getValue());
        }

        json_msg.put(ANDRUAV_PROTOCOL_MESSAGE_CMD, ms);

        this.cUDPClient.setJsonId(json_msg.toString());
    }

    // ------------------------------------------------------------------
    // Phase-3 capability advert helpers (de.cap/1)
    // ------------------------------------------------------------------

    private void _cap_add_to_filter(int msg_id) {
        if (!(this.m_message_filter instanceof JSONArray)) {
            this.m_message_filter = new JSONArray();
        }
        JSONArray filter = (JSONArray) this.m_message_filter;
        for (int i = 0; i < filter.length(); i++) {
            if (filter.get(i).equals(msg_id)) return;
        }
        filter.put(msg_id);
    }

    /** Register this module's de.cap/1 advert(s) - one per namespace.
     *  Validates each advert, derives the "ch" hash carried in the module
     *  ID message, and subscribes MODULE_CAPABILITIES / CAPABILITY_INVOKE.
     *  Returns false (+ log) when any advert fails validation. */
    public boolean set_capabilities(List<Object> adverts) {
        List<Object> norm = CapSchema.cap_normalize_adverts(adverts);
        @SuppressWarnings("unchecked")
        List<String> strings = (List<String>) norm.get(0);
        @SuppressWarnings("unchecked")
        List<JSONObject> parsed = (List<JSONObject>) norm.get(1);
        for (int i = 0; i < strings.size(); i++) {
            if (strings.get(i).getBytes(StandardCharsets.UTF_8).length > CapSchema.CAP_ADVERT_MAX_BYTES) {
                System.out.println("set_capabilities: advert exceeds " + CapSchema.CAP_ADVERT_MAX_BYTES + " bytes");
                return false;
            }
            String err = CapSchema.cap_validate_advert(parsed.get(i));
            if (!err.isEmpty()) {
                System.out.println("set_capabilities: invalid advert: " + err);
                return false;
            }
        }

        synchronized (m_cap_lock) {
            this.m_capability_adverts = strings;
            this.m_capability_parsed = parsed;
            this.m_capability_hash = CapSchema.cap_advert_hash(strings);
        }

        // the ID message only carries the hash; the full adverts go on 6542
        appendExtraField("ch", this.m_capability_hash);

        _cap_add_to_filter(TYPE_AndruavMessage_MODULE_CAPABILITIES);
        _cap_add_to_filter(TYPE_AndruavMessage_CAPABILITY_INVOKE);

        if (this.cUDPClient != null) {
            createJSONID(false);
        }

        // registered already (party known)? send the advert now - the normal
        // path sends it once when the first ID reply arrives
        if (!this.m_party_id.isEmpty() && !this.m_capabilities_sent) {
            this.m_capabilities_sent = true;
            _send_capabilities();
        }

        return true;
    }

    /** Register the action handler: handler(id, ns, act, params,
     *  deadlineMs) -> InvokeResult. Params arrive validated (defaults
     *  filled). Results are idempotent: a repeated invoke id replays the
     *  cached result. */
    public void on_invoke(InvokeHandler handler) {
        synchronized (m_cap_lock) {
            this.m_on_invoke = handler;
        }
    }

    /** Publish changed state keys (MODULE_STATE 6545, intermodule). */
    public void publish_state(String ns, JSONObject changed, boolean full) {
        JSONObject msg = new JSONObject().put("ns", ns).put("s", changed);
        if (full) msg.put("full", true);
        sendJMSG("", msg, TYPE_AndruavMessage_MODULE_STATE, true);
    }

    /** Fire "<ns>.<ev>" as Sync_EventFire d, ed=payload, fresh nonce n,
     *  no scope - intermodule only. Returns the nonce sent. */
    public String fire_event(String ns, String ev, Object payload) {
        this.m_event_counter += 1;
        String nonce = System.currentTimeMillis() + "-" + this.m_event_counter;
        JSONObject msg = new JSONObject()
                .put("d", ns + "." + ev)
                .put("ed", payload)
                .put("n", nonce);
        sendJMSG("", msg, TYPE_AndruavMessage_Sync_EventFire, true);
        return nonce;
    }

    /** Re-broadcast the registered advert set on MODULE_CAPABILITIES
     *  (6542) - for hosts that refresh their adverts after the initial
     *  registration publish. No-op when no adverts are registered. */
    public void send_capabilities() {
        if (!this.m_capability_adverts.isEmpty()) _send_capabilities();
    }

    private void _send_capabilities() {
        JSONObject msg = new JSONObject()
                .put("caps", new JSONArray(this.m_capability_adverts))
                .put("ch", this.m_capability_hash);
        sendJMSG("", msg, TYPE_AndruavMessage_MODULE_CAPABILITIES, true);
    }

    private void _send_invoke_result(JSONObject result) {
        sendJMSG("", result, TYPE_AndruavMessage_CAPABILITY_RESULT, true);
    }

    private void _cache_invoke_result(String id_, JSONObject result) {
        synchronized (m_cap_lock) {
            this.m_invoke_results.remove(id_);
            this.m_invoke_results.put(id_, result);
            while (this.m_invoke_results.size() > MAX_INVOKE_RESULTS) {
                String oldest = this.m_invoke_results.keySet().iterator().next();
                this.m_invoke_results.remove(oldest);
            }
        }
    }

    private void _handle_invoke(JSONObject cmd) {
        String id_ = cmd.optString("id", "");
        String ns = cmd.optString("ns", "");
        String act = cmd.optString("act", "");
        Object withObj = cmd.opt("p");
        JSONObject with_ = (withObj instanceof JSONObject) ? (JSONObject) withObj : new JSONObject();
        double dl = cmd.optDouble("dl", 10);
        if (!(dl > 0)) dl = 10;

        if (id_.isEmpty()) return;

        synchronized (m_cap_lock) {
            JSONObject cached = this.m_invoke_results.get(id_);
            if (cached != null) {
                // idempotent replay - never run the handler twice
                _send_invoke_result(cached);
                return;
            }
            if (!this.m_invoke_inflight.add(id_)) {
                return;
            }
        }

        final String fid = id_;
        final double fdl = dl;
        final long fdeadline = System.currentTimeMillis() + (long) (fdl * 1000);

        java.util.function.Consumer<String> fail = (err) -> {
            this.m_invoke_inflight.remove(fid);
            JSONObject result = new JSONObject().put("id", fid).put("ok", false).put("err", err);
            _cache_invoke_result(fid, result);
            _send_invoke_result(result);
        };

        // resolve "<ns>.<act>" in the adverts
        JSONObject action = null;
        String canonicalAct = act;
        InvokeHandler handler;
        synchronized (m_cap_lock) {
            for (JSONObject advert : this.m_capability_parsed) {
                if (ns.equals(advert.optString("ns"))) {
                    action = CapSchema.cap_find_action(advert, act);
                    if (action != null) {
                        canonicalAct = CapSchema.cap_canonical_action(advert, act);
                    }
                    break;
                }
            }
            handler = this.m_on_invoke;
        }

        if (action == null) {
            fail.accept("unknown action " + ns + "." + act);
            return;
        }

        Map<String, Object> filled = new LinkedHashMap<>();
        List<String> errors = CapSchema.cap_check_params(action.opt("params"), with_, filled);
        if (!errors.isEmpty()) {
            fail.accept("invalid params: " + String.join("; ", errors));
            return;
        }

        if (handler == null) {
            fail.accept("no invoke handler");
            return;
        }

        final String fns = ns;
        final String fact = canonicalAct;
        Thread manager = new Thread(() -> {
            JSONObject[] holder = new JSONObject[1];
            Thread worker = new Thread(() -> {
                JSONObject res;
                try {
                    InvokeResult ir = handler.onInvoke(fid, fns, fact, filled, fdeadline);
                    res = new JSONObject().put("ok", ir.err == null || ir.err.isEmpty());
                    if (ir.err != null && !ir.err.isEmpty()) {
                        res.put("err", ir.err);
                    } else {
                        res.put("data", ir.data != null ? ir.data : JSONObject.NULL);
                    }
                } catch (Exception e) {
                    res = new JSONObject().put("ok", false).put("err", "handler exception: " + e);
                }
                synchronized (holder) {
                    holder[0] = res;
                    holder.notify();
                }
            }, "de_databus-invoke");
            worker.setDaemon(true);
            worker.start();
            synchronized (holder) {
                try {
                    holder.wait((long) (fdl * 1000));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            if (holder[0] == null) {
                // deadline passed - the handler keeps running; its late
                // result is dropped and logged
                this.m_invoke_inflight.remove(fid);
                JSONObject result = new JSONObject().put("id", fid).put("ok", false).put("err", "timeout");
                _cache_invoke_result(fid, result);
                _send_invoke_result(result);
                try { worker.join(); } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("capability invoke " + fid + " (" + fns + "." + fact
                        + ") finished after its deadline - result dropped");
            } else {
                JSONObject result = holder[0];
                result.put("id", fid);
                this.m_invoke_inflight.remove(fid);
                _cache_invoke_result(fid, result);
                _send_invoke_result(result);
            }
        }, "de_databus-invoke-mgr");
        manager.setDaemon(true);
        manager.start();
    }
}
