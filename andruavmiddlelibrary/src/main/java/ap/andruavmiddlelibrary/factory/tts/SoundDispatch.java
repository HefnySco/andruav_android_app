package ap.andruavmiddlelibrary.factory.tts;

/**
 * Static bridge from the legacy sound entry points ({@link TTS} and
 * {@link SoundManager}) to the "sound" capability provider's SoundWorker
 * — the single bounded-FIFO choke point where every surface ends
 * (AN-0042, TO-BE section 5.1). The provider installs itself here when
 * the DE module service runs; with no worker installed every call site
 * falls back to its pre-B3 direct playback.
 *
 * This class lives in andruavmiddlelibrary (not app/) because TTS and
 * SoundManager are here and must call it without depending on the app.
 */
public final class SoundDispatch {

    /** src tag for every legacy (non-bus) surface: GCS 6511/6512, MAKEBEEP,
     *  UI notifications — PROTOCOL.md TASK-EV-SND fact src values. */
    public static final String SRC_GCS = "gcs";

    /** The provider's end of the bridge. All calls must be cheap — they
     *  enqueue onto the worker FIFO and return. */
    public interface Worker {

        /** Speak text; language/pitch/volume are the 6511 fields (-1 = unset). */
        void onSay(String text, String language, int pitch, int volume, String src);

        /** Play a file by name inside the sounds dir (6512 `f`). A name
         *  matching a library clip keeps its clip key for the facts. */
        void onPlayFile(String fileName, String src);

        /** Play a library clip by key; `force` bypasses the mute toggle
         *  (the emergency siren keeps its force semantics). */
        void onClip(String clipKey, boolean loop, boolean force, String src);

        /** End the clip with this key (and drop its queued items). */
        void onStopClip(String clipKey);

        /** End whatever is playing and drop the whole queue. */
        void onStopAll();

        /** True while the clip with this key is the item in progress. */
        boolean isClipPlaying(String clipKey);
    }

    private static volatile Worker s_worker;

    private SoundDispatch() {}

    /** Install the provider's worker; called by SoundProvider's ctor. */
    public static void setWorker(Worker worker) {
        s_worker = worker;
    }

    /** Clear only when the installed worker is this one (stop ordering). */
    public static void clearWorker(Worker worker) {
        if (s_worker == worker) {
            s_worker = null;
        }
    }

    /** The installed worker, or null when the DE module is not running —
     *  callers then fall back to their legacy direct playback. */
    public static Worker get() {
        return s_worker;
    }
}
