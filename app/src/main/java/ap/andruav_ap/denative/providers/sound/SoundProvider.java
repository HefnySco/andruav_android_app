package ap.andruav_ap.denative.providers.sound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.json.JSONException;
import org.json.JSONObject;

import ap.andruavmiddlelibrary.factory.tts.SoundDispatch;
import de.databus.CapSchema;
import de.databus.caps.CapabilityHost;
import de.databus.caps.DeCapabilityProvider;

/**
 * de.cap/1 provider owning the "sound" namespace on the andruav_phone
 * module (AN-0042 / Mission-Planner-Capabilities-TOBE section 5.1 /
 * PROTOCOL.md "Sound clip events & commands"). Advertises say/play/stop
 * plus one per-clip action per surfaced clip, consumes the
 * sound.<clip>.play|stop event commands, and lets the SoundWorker emit
 * the sound.<clip>.started|finished facts {src, loop|reason}.
 *
 * Clip library: the built-ins siren/beep/error plus the user files
 * configured as sound_files[] {key?, name, file, events?} (files live in
 * getExternalFilesDir("sounds")); clip keys follow
 * droneengage_sound_module/sound_events.py. The provider also registers
 * itself as the {@link SoundDispatch} worker, so the legacy 6511 TTS
 * path, the 6512 play-file path and the MAKEBEEP siren all end in the
 * same bounded FIFO with src "gcs".
 *
 * Android-free on purpose: playback goes through {@link SoundWorker.Backend}
 * and the home-screen sound toggle through a BooleanSupplier, so invokes,
 * event commands and fact gating run under plain JUnit.
 */
public class SoundProvider implements DeCapabilityProvider, SoundDispatch.Worker {

    public static final String NS = SoundClips.NS;

    /** Locator prefix the backend maps to a bundled res/raw clip. */
    public static final String BUILTIN_PREFIX = "builtin:";

    /** The three bundled clips (res/raw), keyed by their event key. */
    public static List<SoundClips.Entry> builtinEntries() {
        return Arrays.asList(
                new SoundClips.Entry("siren", "siren", BUILTIN_PREFIX + "siren", true),
                new SoundClips.Entry("beep", "beep", BUILTIN_PREFIX + "beep", true),
                new SoundClips.Entry("error", "error", BUILTIN_PREFIX + "error", true));
    }

    private final JSONObject m_config;
    private final String m_version;
    private final List<String> m_languages;
    private final BooleanSupplier m_muted;
    private final SoundWorker m_worker;
    private final List<SoundClips.Surface> m_surfaces;
    private final Map<String, SoundClips.Surface> m_surface_by_key = new HashMap<>();
    private final List<String> m_warnings = new ArrayList<>();
    private final Set<String> m_logged_once = ConcurrentHashMap.newKeySet();

    private volatile CapabilityHost m_host;
    /** Mirrors the siren's started/finished edges into the legacy
     *  isWhisling unit flag; wired by the service, null in tests. */
    private volatile Consumer<Boolean> m_siren_hook;

    /**
     * @param config      module config: sound_files[], loop_max_s, events_enabled
     * @param version     app version for the advert's "ver"
     * @param backend     playback backend (AndroidSoundBackend on device, fake in tests)
     * @param languages   device TTS locale tags for the say enum
     * @param muted       home-screen sound toggle (true = muted); evaluated per call
     */
    public SoundProvider(JSONObject config, String version, SoundWorker.Backend backend,
                         List<String> languages, BooleanSupplier muted) {
        this.m_config = config != null ? config : new JSONObject();
        this.m_version = version != null ? version : "";
        this.m_languages = languages != null && !languages.isEmpty()
                ? new ArrayList<>(languages) : new ArrayList<>(Collections.singletonList("en"));
        this.m_muted = muted != null ? muted : () -> false;

        List<SoundClips.Entry> library = new ArrayList<>(builtinEntries());
        library.addAll(SoundClips.entriesFromConfig(this.m_config));
        this.m_surfaces = SoundClips.buildSurfaces(library, this.m_warnings);
        for (SoundClips.Surface surface : this.m_surfaces) {
            this.m_surface_by_key.put(surface.key, surface);
        }

        this.m_worker = new SoundWorker(backend, SoundClips.loopMaxMs(this.m_config));
        this.m_worker.setListener(new SoundWorker.Listener() {
            @Override
            public void onFact(String clip_key, String fact, Map<String, Object> payload) {
                _on_clip_fact(clip_key, fact, payload);
            }

            @Override
            public void onPlaying(boolean playing) {
                CapabilityHost host = m_host;
                if (host != null) host.publishState(NS);
            }
        });
        this.m_worker.start();
        SoundDispatch.setWorker(this);
    }

    /** Wire the bus host; called by the service after CapabilityHost.start(). */
    public void attach(CapabilityHost host) {
        this.m_host = host;
    }

    /** Optional mirror for the siren state into the legacy unit flags. */
    public void setSirenHook(Consumer<Boolean> hook) {
        this.m_siren_hook = hook;
    }

    /** Worker handle — used by tests and the service's stop path. */
    public SoundWorker worker() {
        return this.m_worker;
    }

    /** Surface warnings collected while building the clip library. */
    public List<String> warnings() {
        return this.m_warnings;
    }

    /** Clip surfaces (key, name, file, events, action) in library order. */
    public List<SoundClips.Surface> surfaces() {
        return this.m_surfaces;
    }

    /** Stop the worker and drop the legacy dispatch; service stop path. */
    public void stop() {
        SoundDispatch.clearWorker(this);
        this.m_worker.shutdown();
        this.m_host = null;
    }

    // ------------------------------------------------------------------
    // DeCapabilityProvider
    // ------------------------------------------------------------------

    @Override
    public List<Object> adverts() {
        JSONObject advert = _build_advert(true);
        if (CapSchema.canonicalJson(advert).getBytes().length > CapSchema.CAP_ADVERT_MAX_BYTES) {
            // 16 KB guard: drop the per-clip actions, keep the base advert.
            advert = _build_advert(false);
        }
        List<Object> adverts = new ArrayList<>(1);
        adverts.add(advert);
        return adverts;
    }

    @Override
    public void invoke(String id, String ns, String act, JSONObject params,
                       long deadlineMs, ResultSink sink) {
        if (!NS.equals(ns)) {
            sink.err("unknown namespace " + ns);
            return;
        }

        if ("say".equals(act)) {
            if (_is_muted()) {
                sink.ok(_kv("queued", false, "muted", true));
                return;
            }
            String lang = params.optString("lang", params.optString("language", null));
            m_worker.enqueue(SoundWorker.Item.say(
                    params.optString("text", ""),
                    lang != null && lang.isEmpty() ? null : lang,
                    params.has("pitch") ? params.optInt("pitch") : -1,
                    params.has("volume") ? params.optInt("volume") : -1,
                    SoundWorker.SRC_INVOKE));
            sink.ok(_kv("queued", true));
            return;
        }

        if ("play".equals(act)) {
            String name = params.optString("file", "");
            SoundClips.Surface surface = _resolve_clip(name);
            if (surface == null) {
                sink.err("unknown sound '" + name + "'");
                return;
            }
            if (_is_muted()) {
                sink.ok(_kv("queued", false, "muted", true));
                return;
            }
            m_worker.enqueue(SoundWorker.Item.play(surface.key, surface.file,
                    SoundWorker.SRC_INVOKE, false));
            sink.ok(_kv("queued", true, "file", surface.file));
            return;
        }

        if ("stop".equals(act)) {
            sink.ok(_kv("stopped", m_worker.stopAll()));
            return;
        }

        // Per-clip action: do: sound.<clip> {loop?} enqueues with src "invoke".
        // Reserved names already matched above, so a colliding clip key never
        // reaches this lookup (its surface has action=false anyway).
        SoundClips.Surface surface = m_surface_by_key.get(act);
        if (surface != null && surface.action) {
            if (_is_muted()) {
                sink.ok(_kv("queued", false, "muted", true));
                return;
            }
            m_worker.enqueue(SoundWorker.Item.play(surface.key, surface.file,
                    SoundWorker.SRC_INVOKE, params.optBoolean("loop", false)));
            sink.ok(_kv("queued", true, "clip", surface.key, "file", surface.file));
            return;
        }

        sink.err("unknown action " + ns + "." + act);
    }

    @Override
    public void onEvent(String name, JSONObject ed, String nonce) {
        // Locally delivered Sync_EventFire command "sound.<clip>.<verb>":
        // enqueue-only, never blocks the receive thread (dedupe on d|n is
        // upstream in CapabilityHost).
        String[] parsed = SoundClips.parseCommand(name);
        if (parsed == null) return;
        String key = parsed[0];
        String verb = parsed[1];

        if (!SoundClips.eventsEnabled(m_config)) {
            _log_once(key + "." + verb, "events disabled");
            return;
        }
        SoundClips.Surface surface = m_surface_by_key.get(key);
        if (surface == null) {
            _log_once(key + "." + verb, "unknown clip");
            return;
        }
        if (!surface.events) {
            _log_once(key + "." + verb, "events disabled");
            return;
        }

        if (SoundClips.VERB_PLAY.equals(verb)) {
            Object loop = ed != null ? ed.opt("loop") : null;
            if (loop != null && !(loop instanceof Boolean)) {
                _log_once(key + "." + verb, "loop must be a bool");
                return;
            }
            if (_is_muted()) {
                _log_once(key + "." + verb, "muted");
                return;
            }
            m_worker.enqueue(SoundWorker.Item.play(key, surface.file,
                    SoundWorker.SRC_EVENT, Boolean.TRUE.equals(loop)));
        } else if (SoundClips.VERB_STOP.equals(verb)) {
            m_worker.stopClip(key);
        }
    }

    @Override
    public JSONObject state(String ns) {
        return _kv("playing", m_worker.isPlaying());
    }

    // ------------------------------------------------------------------
    // SoundDispatch.Worker — legacy surfaces into the same choke point
    // ------------------------------------------------------------------

    /** 6511 / Notification speech -> the worker, src "gcs". */
    @Override
    public void onSay(String text, String language, int pitch, int volume, String src) {
        if (_is_muted() || text == null || text.isEmpty()) return;
        m_worker.enqueue(SoundWorker.Item.say(text, language, pitch, volume, src));
    }

    /** 6512 play-file: a library path keeps its clip key (facts src "gcs");
     *  any other file name still plays but stays silent like the Python
     *  module's legacy SOUND_PLAY_FILE path. */
    @Override
    public void onPlayFile(String fileName, String src) {
        if (_is_muted() || fileName == null || fileName.isEmpty()) return;
        SoundClips.Surface surface = _resolve_clip(fileName);
        if (surface != null) {
            m_worker.enqueue(SoundWorker.Item.play(surface.key, surface.file, src, false));
        } else {
            m_worker.enqueue(SoundWorker.Item.play(null, fileName, src, false));
        }
    }

    /** MAKEBEEP siren / built-in clip keys: force bypasses the mute
     *  (the emergency siren always sounds). */
    @Override
    public void onClip(String clipKey, boolean loop, boolean force, String src) {
        SoundClips.Surface surface = m_surface_by_key.get(clipKey);
        if (surface == null) return;
        if (_is_muted() && !force) return;
        m_worker.enqueue(SoundWorker.Item.play(surface.key, surface.file, src, loop));
    }

    @Override
    public void onStopClip(String clipKey) {
        m_worker.stopClip(clipKey);
    }

    @Override
    public void onStopAll() {
        m_worker.stopAll();
    }

    @Override
    public boolean isClipPlaying(String clipKey) {
        return m_worker.isClipPlaying(clipKey);
    }

    // ------------------------------------------------------------------
    // internal
    // ------------------------------------------------------------------

    private boolean _is_muted() {
        try {
            return m_muted.getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    /** Resolve an advertised play name / clip key / file name to a surface. */
    private SoundClips.Surface _resolve_clip(String name) {
        if (name == null || name.isEmpty()) return null;
        for (SoundClips.Surface surface : m_surfaces) {
            if (name.equals(surface.name) || name.equals(surface.key)
                    || name.equals(surface.file)) {
                return surface;
            }
        }
        return null;
    }

    /** Worker-side fact emitter (worker thread): emit "sound.<clip>.<fact>"
     *  gated by events_enabled and the per-clip events flag — the only
     *  outbound point, so gating here means zero fact traffic. */
    private void _on_clip_fact(String clip_key, String fact, Map<String, Object> payload) {
        if ("siren".equals(clip_key)) {
            _siren_hook("started".equals(fact));
        }
        if (!SoundClips.eventsEnabled(m_config)) return;
        SoundClips.Surface surface = m_surface_by_key.get(clip_key);
        if (surface != null && !surface.events) return;
        // the "say" key has no clip surface but still reports its facts
        if (surface == null && !SoundWorker.SAY_KEY.equals(clip_key)) return;
        CapabilityHost host = m_host;
        if (host == null) return;
        host.fireEvent(NS, clip_key + "." + fact,
                payload != null ? new JSONObject(payload) : new JSONObject());
    }

    private void _siren_hook(boolean on) {
        Consumer<Boolean> hook = m_siren_hook;
        if (hook == null) return;
        try {
            hook.accept(on);
        } catch (Exception ignored) {
        }
    }

    private void _log_once(String tag, String reason) {
        if (m_logged_once.add(tag + ": " + reason)) {
            // System.out like de.databus - keeps the provider JVM-testable.
            System.out.println("SoundProvider: event ignored " + tag + ": " + reason);
        }
    }

    private JSONObject _build_advert(boolean with_clip_actions) {
        try {
            return _build_advert_json(with_clip_actions);
        } catch (JSONException e) {
            // a literal advert never fails; never publish nothing
            return new JSONObject();
        }
    }

    private JSONObject _build_advert_json(boolean with_clip_actions) throws JSONException {
        JSONObject lang_param = new JSONObject()
                .put("type", "enum")
                .put("values", new org.json.JSONArray(m_languages))
                .put("optional", true)
                .put("desc", "language code");

        JSONObject actions = new JSONObject()
                .put("say", new JSONObject()
                        .put("desc", "Speak a text (device TTS)")
                        .put("params", new JSONObject()
                                .put("text", new JSONObject()
                                        .put("type", "string")
                                        .put("desc", "text to speak"))
                                .put("lang", lang_param)
                                .put("language", new JSONObject(lang_param.toString())
                                        .put("desc", "same as lang (legacy name)"))
                                .put("pitch", new JSONObject()
                                        .put("type", "int").put("min", 0).put("max", 99)
                                        .put("optional", true))
                                .put("volume", new JSONObject()
                                        .put("type", "int").put("min", 0).put("max", 100)
                                        .put("optional", true))))
                .put("play", new JSONObject()
                        .put("desc", "Play a sound from the clip library")
                        .put("params", new JSONObject()
                                .put("file", new JSONObject()
                                        .put("type", "enum")
                                        .put("values", _play_values()))))
                .put("stop", new JSONObject()
                        .put("desc", "Stop the sound playing now and drop queued ones"));

        JSONObject resources = new JSONObject();
        for (SoundClips.Surface surface : m_surfaces) {
            resources.put(surface.key, "clip");
            if (with_clip_actions && surface.action) {
                actions.put(surface.key, new JSONObject()
                        .put("desc", "play '" + surface.name + "' (enqueues; completion is sound."
                                + surface.key + ".finished)")
                        .put("timeout_s", 5)
                        .put("params", new JSONObject()
                                .put("loop", new JSONObject()
                                        .put("type", "bool")
                                        .put("optional", true)
                                        .put("desc", "repeat until stopped, preempted or "
                                                + "loop_max_s (default false)"))));
            }
        }

        return new JSONObject()
                .put("schema", CapSchema.CAP_SCHEMA_ID)
                .put("ns", NS)
                .put("module", "andruav_android")
                .put("ver", m_version)
                .put("desc", "events: sound.<clip>.play|stop (commands), "
                        + "sound.<clip>.started|finished (facts)")
                .put("actions", actions)
                .put("state", new JSONObject()
                        .put("playing", new JSONObject()
                                .put("type", "bool")
                                .put("desc", "a sound or speech is playing")))
                .put("resources", resources)
                .put("event_patterns", new JSONObject()
                        .put("*.play", new JSONObject()
                                .put("kind", "command")
                                .put("payload", new JSONObject()
                                        .put("loop", new JSONObject()
                                                .put("type", "bool")
                                                .put("optional", true))))
                        .put("*.stop", new JSONObject().put("kind", "command"))
                        .put("*.started", new JSONObject()
                                .put("kind", "fact")
                                .put("payload", new JSONObject()
                                        .put("src", new JSONObject().put("type", "string"))
                                        .put("loop", new JSONObject().put("type", "bool"))))
                        .put("*.finished", new JSONObject()
                                .put("kind", "fact")
                                .put("payload", new JSONObject()
                                        .put("src", new JSONObject().put("type", "string"))
                                        .put("reason", new JSONObject().put("type", "string")))));
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

    /** play {file} enum: the library's clip names (built-in keys + the
     *  configured sound_files names). */
    private org.json.JSONArray _play_values() {
        org.json.JSONArray values = new org.json.JSONArray();
        for (SoundClips.Surface surface : m_surfaces) {
            values.put(surface.name);
        }
        return values;
    }
}
