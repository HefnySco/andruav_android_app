package ap.andruav_ap.denative.providers.camera;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.databus.CapSchema;
import de.databus.caps.CapabilityHost;
import de.databus.caps.DeCapabilityProvider;

/**
 * de.cap/1 provider for the "camera" namespace - the Android twin of the
 * Linux droneengage_camera surface (TASK-EV-CAM / TO-BE section 5.4).
 *
 * Advertised surface:
 *  - actions  capture {count, interval_ms, channel?, gcs_preview},
 *             record {on, channel?}, zoom {level, channel?} (Android
 *             extra), and one per-channel action "<ch>" {record: bool}
 *             (a channel whose key collides with a reserved action name
 *             keeps its events but gets no action);
 *  - events   captured {file, pos?, t, n, inv?, ch?};
 *  - state    recording;
 *  - commands camera.<ch>.capture {count?, interval_ms?},
 *             camera.<ch>.record_on, camera.<ch>.record_off consumed from
 *             locally delivered Sync_EventFire;
 *  - facts    camera.<ch>.went_recording {src, file?},
 *             camera.<ch>.went_idle {src, reason, file?} emitted edge-only
 *             by the backend's setRecording choke point.
 *
 * Facts go out through {@link CapabilityHost#fireEvent} and state changes
 * through {@link CapabilityHost#publishState}; the bus connection is
 * attached after construction ({@link #attach}), matching the sibling
 * providers. All device work is delegated to a {@link CameraBackend}; this
 * class holds no Android references and is exercised by plain JVM unit
 * tests.
 */
public class CameraProvider implements DeCapabilityProvider, CameraBackend.Listener {

    public static final String NS = "camera";

    private static final int CAPTURE_COUNT_MAX = 100;
    private static final int CAPTURE_INTERVAL_MS_MAX = 60000;

    private final CameraBackend m_backend;
    private final String m_version;
    private final Set<String> m_logged_once = ConcurrentHashMap.newKeySet();

    private volatile CapabilityHost m_host;

    public CameraProvider(final CameraBackend backend, final String version) {
        this.m_backend = backend;
        this.m_version = version != null ? version : "";
        backend.setListener(this);
    }

    /** Wire the bus host; called by the service after CapabilityHost.start(). */
    public void attach(final CapabilityHost host) {
        this.m_host = host;
    }

    // ------------------------------------------------------------------
    // DeCapabilityProvider
    // ------------------------------------------------------------------

    @Override
    public List<Object> adverts() {
        final List<Object> adverts = new ArrayList<>(1);
        adverts.add(buildAdvert().toString());
        return adverts;
    }

    @Override
    public void invoke(final String id, final String ns, final String act,
                       final JSONObject params, final long deadlineMs,
                       final ResultSink sink) {
        if (!NS.equals(ns)) {
            sink.err("unknown namespace " + ns);
            return;
        }

        final String[] err = new String[1];
        switch (act) {
            case "capture": {
                final int count = params.optInt("count", 1);
                final int intervalMs = params.optInt("interval_ms", 0);
                final String channel = params.optString("channel", "");
                final boolean gcsPreview = params.optBoolean("gcs_preview", false);
                final String ch = m_backend.resolveChannelKey(channel);
                if (ch == null) {
                    sink.err("unknown channel '" + channel + "'");
                    return;
                }
                if (!m_backend.startCapture(ch, count, intervalMs, gcsPreview, id, "invoke", err)) {
                    sink.err(err[0] != null ? err[0] : "capture failed");
                    return;
                }
                sink.ok(_kv("started", true, "channel", channelId(ch),
                        "ch", ch, "count", count));
                return;
            }

            case "record": {
                final boolean on = params.optBoolean("on", false);
                final String channel = params.optString("channel", "");
                final String ch = m_backend.resolveChannelKey(channel);
                if (ch == null) {
                    sink.err("unknown channel '" + channel + "'");
                    return;
                }
                if (!m_backend.setRecording(ch, on, "invoke", err)) {
                    sink.err(err[0] != null ? err[0] : "record failed");
                    return;
                }
                sink.ok(_kv("recording", m_backend.isRecording(ch),
                        "channel", channelId(ch), "ch", ch));
                return;
            }

            case "zoom": {
                final double level = params.optDouble("level", 1.0);
                final String channel = params.optString("channel", "");
                final String ch = m_backend.resolveChannelKey(channel);
                if (ch == null) {
                    sink.err("unknown channel '" + channel + "'");
                    return;
                }
                if (!m_backend.setZoom(ch, level, err)) {
                    sink.err(err[0] != null ? err[0] : "zoom failed");
                    return;
                }
                sink.ok(_kv("zoom", level, "channel", channelId(ch), "ch", ch));
                return;
            }

            default:
                break;
        }

        // per-channel action "<ch>" {record: bool}
        for (final CameraBackend.CameraChannel channel : m_backend.channels()) {
            if (channel.key.equals(act)) {
                final boolean on = params.optBoolean("record", false);
                if (!m_backend.setRecording(channel.key, on, "invoke", err)) {
                    sink.err(err[0] != null ? err[0] : "record failed");
                    return;
                }
                sink.ok(_kv("recording", m_backend.isRecording(channel.key),
                        "channel", channel.id, "ch", channel.key));
                return;
            }
        }

        sink.err("unknown action " + ns + "." + act);
    }

    /**
     * A locally delivered Sync_EventFire for this namespace:
     * d = "camera.<ch>.<verb>"; verbs capture | record_on | record_off.
     * Runs on the databus receive thread - never blocks (the backend
     * queues the work internally).
     */
    @Override
    public void onEvent(final String name, final JSONObject ed, final String nonce) {
        if (name == null || !name.startsWith(NS + ".")) return;
        final String rest = name.substring(NS.length() + 1);
        final int dot = rest.indexOf('.');
        if (dot <= 0 || dot == rest.length() - 1) return;
        final String key = rest.substring(0, dot);
        final String verb = rest.substring(dot + 1);

        final String ch = m_backend.resolveChannelKey(key);
        if (ch == null) {
            logOnce("unknown channel in command " + name);
            return;
        }
        final String[] err = new String[1];
        switch (verb) {
            case "capture": {
                int count = 1;
                int intervalMs = 0;
                if (ed.has("count")) {
                    final int c = ed.optInt("count", -1);
                    if (c < 1 || c > CAPTURE_COUNT_MAX) {
                        logOnce("camera." + key + ".capture count out of range");
                        return;
                    }
                    count = c;
                }
                if (ed.has("interval_ms")) {
                    final int i = ed.optInt("interval_ms", -1);
                    if (i < 0 || i > CAPTURE_INTERVAL_MS_MAX) {
                        logOnce("camera." + key + ".capture interval_ms out of range");
                        return;
                    }
                    intervalMs = i;
                }
                if (!m_backend.startCapture(ch, count, intervalMs, false, null, "event", err)) {
                    logOnce("capture command failed: " + (err[0] != null ? err[0] : ""));
                }
                return;
            }
            case "record_on":
                m_backend.setRecording(ch, true, "event", err);
                return;
            case "record_off":
                m_backend.setRecording(ch, false, "event", err);
                return;
            default:
                logOnce("unsupported verb in command " + name);
        }
    }

    @Override
    public JSONObject state(final String ns) {
        return _kv("recording", m_backend.anyRecording());
    }

    // ------------------------------------------------------------------
    // CameraBackend.Listener -> facts on the bus
    // ------------------------------------------------------------------

    @Override
    public void onCaptured(final String channelKey, final String file, final double[] pos,
                           final long tMs, final int index, final String invokeId) {
        try {
            final JSONObject payload = new JSONObject()
                    .put("file", file)
                    .put("t", tMs)
                    .put("n", index)
                    .put("ch", channelKey);
            if (pos != null && pos.length == 3) {
                payload.put("pos", new JSONArray().put(pos[0]).put(pos[1]).put(pos[2]));
            }
            if (invokeId != null) {
                payload.put("inv", invokeId);
            }
            emit(NS + ".captured", payload);
        } catch (JSONException e) {
            logOnce("captured payload failed: " + e);
        }
    }

    @Override
    public void onWentRecording(final String channelKey, final String src, final String file) {
        try {
            final JSONObject payload = new JSONObject().put("src", src);
            if (file != null && !file.isEmpty()) payload.put("file", file);
            emit(NS + "." + channelKey + ".went_recording", payload);
            publishState();
        } catch (JSONException e) {
            logOnce("went_recording payload failed: " + e);
        }
    }

    @Override
    public void onWentIdle(final String channelKey, final String src, final String reason,
                           final String file) {
        try {
            final JSONObject payload = new JSONObject()
                    .put("src", src)
                    .put("reason", reason);
            if (file != null && !file.isEmpty()) payload.put("file", file);
            emit(NS + "." + channelKey + ".went_idle", payload);
            publishState();
        } catch (JSONException e) {
            logOnce("went_idle payload failed: " + e);
        }
    }

    /** Fact egress; overridden by tests, the attach()ed host in production. */
    protected void emit(final String name, final JSONObject payload) {
        final CapabilityHost host = m_host;
        if (host != null) host.fireEvent(name, payload);
    }

    private void publishState() {
        final CapabilityHost host = m_host;
        if (host != null) host.publishState(NS);
    }

    // ------------------------------------------------------------------
    // advert
    // ------------------------------------------------------------------

    private String channelId(final String ch) {
        for (final CameraBackend.CameraChannel channel : m_backend.channels()) {
            if (channel.key.equals(ch)) return channel.id;
        }
        return ch;
    }

    private JSONObject buildAdvert() {
        try {
            return buildAdvertJson();
        } catch (JSONException e) {
            // a literal advert never fails; never publish nothing
            return new JSONObject();
        }
    }

    private JSONObject buildAdvertJson() throws JSONException {
        final List<CameraBackend.CameraChannel> channels = m_backend.channels();
        final StringBuilder keys = new StringBuilder();
        for (final CameraBackend.CameraChannel channel : channels) {
            if (keys.length() > 0) keys.append(", ");
            keys.append(channel.key);
        }

        final JSONObject advert = new JSONObject()
                .put("schema", CapSchema.CAP_SCHEMA_ID)
                .put("ns", NS)
                .put("module", "andruav_phone")
                .put("ver", m_version)
                .put("desc", "events: " + NS + ".<ch>.went_recording|went_idle "
                        + "{src,reason,file?}; commands: " + NS
                        + ".<ch>.capture{count?,interval_ms?}|record_on|record_off"
                        + (keys.length() > 0 ? "; channels: " + keys : ""))
                .put("actions", new JSONObject()
                        .put("capture", new JSONObject()
                                .put("desc", "Take a burst of photos")
                                .put("timeout_s", 10)
                                .put("params", new JSONObject()
                                        .put("count", new JSONObject()
                                                .put("type", "int").put("min", 1)
                                                .put("max", CAPTURE_COUNT_MAX).put("default", 1)
                                                .put("desc", "photos to take"))
                                        .put("interval_ms", new JSONObject()
                                                .put("type", "int").put("min", 0)
                                                .put("max", CAPTURE_INTERVAL_MS_MAX)
                                                .put("default", 0).put("unit", "ms"))
                                        .put("channel", new JSONObject()
                                                .put("type", "string").put("default", "")
                                                .put("desc", "camera channel key or device id; empty = first"))
                                        .put("gcs_preview", new JSONObject()
                                                .put("type", "bool").put("default", false)
                                                .put("desc", "send the image back to the GCS"))))
                        .put("record", new JSONObject()
                                .put("desc", "Start or stop video recording")
                                .put("timeout_s", 10)
                                .put("params", new JSONObject()
                                        .put("on", new JSONObject().put("type", "bool"))
                                        .put("channel", new JSONObject()
                                                .put("type", "string").put("default", "")
                                                .put("desc", "camera channel key or device id; empty = first"))))
                        .put("zoom", new JSONObject()
                                .put("desc", "Set camera zoom level")
                                .put("timeout_s", 10)
                                .put("params", new JSONObject()
                                        .put("level", new JSONObject()
                                                .put("type", "number").put("min", 1).put("max", 10))
                                        .put("channel", new JSONObject()
                                                .put("type", "string").put("default", "")
                                                .put("desc", "camera channel key or device id; empty = first")))))
                .put("events", new JSONObject()
                        .put("captured", new JSONObject()
                                .put("desc", "A photo was saved")
                                .put("payload", new JSONObject()
                                        .put("file", new JSONObject().put("type", "string"))
                                        .put("pos", new JSONObject().put("type", "geo")
                                                .put("optional", true)
                                                .put("desc", "[lat,lng,alt_m] or absent when no fresh fix"))
                                        .put("t", new JSONObject().put("type", "int")
                                                .put("desc", "unix ms"))
                                        .put("n", new JSONObject().put("type", "int")
                                                .put("desc", "index in burst"))
                                        .put("inv", new JSONObject().put("type", "string")
                                                .put("optional", true)
                                                .put("desc", "invoke id that started the burst"))
                                        .put("ch", new JSONObject().put("type", "string")
                                                .put("optional", true)
                                                .put("desc", "channel resource key")))))
                .put("state", new JSONObject()
                        .put("recording", new JSONObject().put("type", "bool")))
                .put("event_patterns", new JSONObject()
                        .put("*.capture", new JSONObject()
                                .put("kind", "command").put("res", new JSONArray().put("channel")))
                        .put("*.record_on", new JSONObject()
                                .put("kind", "command").put("res", new JSONArray().put("channel")))
                        .put("*.record_off", new JSONObject()
                                .put("kind", "command").put("res", new JSONArray().put("channel")))
                        .put("*.went_recording", new JSONObject()
                                .put("kind", "fact").put("res", new JSONArray().put("channel")))
                        .put("*.went_idle", new JSONObject()
                                .put("kind", "fact").put("res", new JSONArray().put("channel"))));

        final JSONObject resources = new JSONObject();
        for (final CameraBackend.CameraChannel channel : channels) {
            // the reserved capture/record names win - a colliding channel
            // keeps its events but gets no action
            if (!"capture".equals(channel.key) && !"record".equals(channel.key)) {
                advert.getJSONObject("actions").put(channel.key, new JSONObject()
                        .put("desc", "start/stop video recording on camera '" + channel.name + "'")
                        .put("timeout_s", 10)
                        .put("params", new JSONObject()
                                .put("record", new JSONObject().put("type", "bool")
                                        .put("desc", "true = record, false = idle"))));
            }
            resources.put(channel.key, "channel");
        }
        advert.put("resources", resources);

        // 16 KB wire limit: drop the per-channel actions when the advert
        // would not fit - commands still arrive as Sync_EventFire.
        if (advert.toString().getBytes(StandardCharsets.UTF_8).length
                > CapSchema.CAP_ADVERT_MAX_BYTES) {
            for (final CameraBackend.CameraChannel channel : channels) {
                advert.getJSONObject("actions").remove(channel.key);
            }
        }
        return advert;
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

    private void logOnce(final String what) {
        if (m_logged_once.add(what)) {
            System.out.println("camera provider: " + what);
        }
    }
}
