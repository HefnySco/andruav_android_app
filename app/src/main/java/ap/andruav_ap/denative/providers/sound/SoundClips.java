package ap.andruav_ap.denative.providers.sound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import de.databus.CapSchema;

/**
 * Pure model of the sound.<clip>.* event surface — the Java port of
 * droneengage_sound_module/sound_events.py (TASK-EV-SND). Owns the clip
 * key derivation (_slug/clip_key), the surface list (one per configured
 * clip, first key wins), the command-name parse and the config gates.
 * No Android, no bus — SoundProvider and the JUnit tests drive it.
 */
public final class SoundClips {

    public static final String NS = "sound";

    /** Frozen action names: a clip whose key matches one keeps its event
     *  surface but gets no per-clip action - reserved names win. */
    public static final Set<String> RESERVED_ACTIONS =
            new HashSet<>(Arrays.asList("say", "play", "stop"));

    public static final Set<String> COMMAND_VERBS =
            new HashSet<>(Arrays.asList("play", "stop"));
    public static final String VERB_PLAY = "play";
    public static final String VERB_STOP = "stop";

    /** Loop cap in seconds: config "loop_max_s", default 60, clamped 1..600. */
    public static final double LOOP_MAX_S_DEFAULT = 60.0;
    public static final double LOOP_MAX_S_MIN = 1.0;
    public static final double LOOP_MAX_S_MAX = 600.0;

    private SoundClips() {}

    /** One configured clip: explicit key (optional), display name and the
     *  backend file locator (a file name inside the sounds dir, or a
     *  "builtin:<key>" locator). `events` is the per-entry gate. */
    public static final class Entry {
        public String key;
        public String name;
        public String file;
        public boolean events = true;

        public Entry(String key, String name, String file, boolean events) {
            this.key = key;
            this.name = name;
            this.file = file;
            this.events = events;
        }
    }

    /** A surfaced clip: its derived event key plus the advert/action flag. */
    public static final class Surface {
        public String key;
        public String name;
        public String file;
        public int index;        // 1-based position in the library
        public boolean events;
        public boolean action;   // false on the reserved names say/play/stop
    }

    /**
     * Display name -> event-segment slug (sound_events._slug): lowercase,
     * every run of non-[a-z0-9] folds to '_', '_' stripped at both ends,
     * 'clip_' prefixed when it starts with a digit, cut to the name cap.
     * "Beep Tone" -> "beep_tone".
     */
    public static String slug(String name) {
        if (name == null) return "";
        String slug = name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
        while (slug.startsWith("_")) slug = slug.substring(1);
        while (slug.endsWith("_")) slug = slug.substring(0, slug.length() - 1);
        if (!slug.isEmpty() && Character.isDigit(slug.charAt(0))) {
            slug = "clip_" + slug;
        }
        if (slug.length() > CapSchema.CAP_NAME_MAX_LEN) {
            slug = slug.substring(0, CapSchema.CAP_NAME_MAX_LEN);
        }
        return slug;
    }

    /** Event key for one library entry (D-SND-1): the explicit `key` when
     *  it is a valid name, else the slug of `name`, else clip_<index>
     *  (1-based position in the library). */
    public static String clipKey(Entry entry, int index) {
        if (entry != null && entry.key != null && CapSchema.cap_is_valid_name(entry.key)) {
            return entry.key;
        }
        String slug = slug(entry != null ? entry.name : null);
        if (!slug.isEmpty() && CapSchema.cap_is_valid_name(slug)) {
            return slug;
        }
        return "clip_" + index;
    }

    /**
     * (surfaces, warnings) for the clip library. Entries missing name or
     * file get no surface; the first key wins on collision — later same-key
     * entries keep no surface (they stay reachable through play {file}).
     * `action` is false on the reserved names say/play/stop.
     */
    public static List<Surface> buildSurfaces(List<Entry> entries, List<String> warnings) {
        List<Surface> surfaces = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (entries == null) return surfaces;
        for (int index = 1; index <= entries.size(); index++) {
            Entry entry = entries.get(index - 1);
            if (entry == null || entry.name == null || entry.name.isEmpty()
                    || entry.file == null || entry.file.isEmpty()) {
                continue;
            }
            String key = clipKey(entry, index);
            if (seen.contains(key)) {
                if (warnings != null) {
                    warnings.add("sound clip '" + entry.name + "': key '" + key
                            + "' already taken - no event surface or per-clip action");
                }
                continue;
            }
            seen.add(key);
            Surface surface = new Surface();
            surface.key = key;
            surface.name = entry.name;
            surface.file = entry.file;
            surface.index = index;
            surface.events = entry.events;
            surface.action = !RESERVED_ACTIONS.contains(key);
            if (!surface.action && warnings != null) {
                warnings.add("sound clip '" + entry.name + "': key '" + key
                        + "' is a reserved action name - the clip keeps its "
                        + "events but gets no action");
            }
            surfaces.add(surface);
        }
        return surfaces;
    }

    /**
     * Config "sound_files" -> Entry list. `file` stays the configured file
     * name; the backend resolves it inside the sounds directory.
     */
    public static List<Entry> entriesFromConfig(JSONObject config) {
        List<Entry> entries = new ArrayList<>();
        if (config == null) return entries;
        JSONArray files = config.optJSONArray("sound_files");
        if (files == null) return entries;
        for (int i = 0; i < files.length(); i++) {
            JSONObject e = files.optJSONObject(i);
            if (e == null) continue;
            entries.add(new Entry(
                    e.optString("key", null),
                    e.optString("name", null),
                    e.optString("file", null),
                    e.optBoolean("events", true)));
        }
        return entries;
    }

    /** "sound.<key>.<verb>" -> [key, verb] for verbs play|stop; anything
     *  else - facts, two-segment names, other namespaces -> null. */
    public static String[] parseCommand(String d) {
        if (d == null) return null;
        String[] parts = d.split("\\.");
        if (parts.length != 3 || !NS.equals(parts[0])) return null;
        if (!COMMAND_VERBS.contains(parts[2]) || !CapSchema.cap_is_valid_name(parts[1])) {
            return null;
        }
        return new String[]{parts[1], parts[2]};
    }

    /** Module-level gate (default true): false switches the whole event
     *  surface off - commands ignored, no facts. */
    public static boolean eventsEnabled(JSONObject config) {
        return config == null || config.optBoolean("events_enabled", true);
    }

    /** Loop cap in milliseconds: config "loop_max_s" (default 60 s,
     *  clamped to 1..600 s). */
    public static long loopMaxMs(JSONObject config) {
        double value = config != null
                ? config.optDouble("loop_max_s", LOOP_MAX_S_DEFAULT)
                : LOOP_MAX_S_DEFAULT;
        if (Double.isNaN(value)) value = LOOP_MAX_S_DEFAULT;
        value = Math.min(Math.max(value, LOOP_MAX_S_MIN), LOOP_MAX_S_MAX);
        return (long) (value * 1000.0);
    }
}
