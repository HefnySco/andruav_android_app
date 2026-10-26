package ap.andruav_ap.denative.providers.sound;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The single playback choke point of the "sound" namespace (AN-0042,
 * port of droneengage_sound_module/sound_manager.py CSoundManager).
 * One worker thread drains a bounded FIFO; every surface — the do:
 * invokes, the sound.<clip>.play|stop event commands, the legacy 6511
 * TTS path, the 6512 play-file path and the MAKEBEEP siren — enqueues
 * here, so this is the only place audio starts or ends and the only
 * place the sound.<clip>.started|finished facts come from.
 *
 * Pure Java (no android.*, no org.json): payloads are plain maps so the
 * dispatch, loop and stop semantics run under a plain JUnit test with a
 * fake {@link Backend}; AndroidSoundBackend adapts it to MediaPlayer/TTS.
 */
public class SoundWorker implements Runnable {

    public static final String KIND_SAY = "say";
    public static final String KIND_PLAY = "play";

    /** fact src values (PROTOCOL.md TASK-EV-SND). */
    public static final String SRC_EVENT = "event";
    public static final String SRC_INVOKE = "invoke";
    public static final String SRC_GCS = "gcs";

    /** finished.reason values. */
    public static final String REASON_DONE = "done";
    public static final String REASON_STOPPED = "stopped";
    public static final String REASON_PREEMPTED = "preempted";
    public static final String REASON_LIMIT = "limit";
    public static final String REASON_ERROR = "error";

    /** Clip key used for the say surface's started/finished facts. "say"
     *  is a reserved action name, so no real clip can collide with it. */
    public static final String SAY_KEY = "say";

    /** Bounded FIFO capacity; a full queue drops the oldest waiting item. */
    public static final int QUEUE_CAPACITY = 32;

    /** One queued unit of work. */
    public static final class Item {
        public String kind = KIND_PLAY;      // KIND_SAY | KIND_PLAY
        public String clip_key;              // event key; null = unsurfaced file play (no facts)
        public String file;                  // backend locator: "builtin:<key>" or a file name
        public String text;                  // say
        public String language;              // say locale tag, may be null
        public int pitch = -1;               // say pitch 0..99, -1 = default
        public int volume = -1;              // say volume 0..100, -1 = default
        public boolean loop;
        public String src = SRC_GCS;         // SRC_EVENT | SRC_INVOKE | SRC_GCS

        public static Item say(String text, String language, int pitch, int volume, String src) {
            Item item = new Item();
            item.kind = KIND_SAY;
            item.clip_key = SAY_KEY;
            item.text = text;
            item.language = language;
            item.pitch = pitch;
            item.volume = volume;
            item.src = src;
            return item;
        }

        public static Item play(String clip_key, String file, String src, boolean loop) {
            Item item = new Item();
            item.kind = KIND_PLAY;
            item.clip_key = clip_key;
            item.file = file;
            item.src = src;
            item.loop = loop;
            return item;
        }
    }

    /**
     * A playback in flight. The backend owns the audio; the worker owns
     * the lifecycle: {@link #awaitEnd()} returns one of the RESULT_*
     * codes, {@link #requestStop()} ends it with RESULT_STOPPED.
     */
    public interface Playback {
        int RESULT_DONE = 0;
        int RESULT_STOPPED = 1;
        int RESULT_ERROR = 2;

        /** Blocks until the sound ends naturally, is stopped, or fails. */
        int awaitEnd() throws InterruptedException;

        /** Stop now; awaitEnd() then returns RESULT_STOPPED. Must be
         *  safe before/without awaitEnd and idempotent. */
        void requestStop();
    }

    /** Device side: starts an item, returns its live playback handle.
     *  Called only on the worker thread; may throw -> finished {error}. */
    public interface Backend {
        Playback start(Item item);
    }

    /** Fact + playing-edge observer (the provider turns facts into
     *  "sound.<clip>.<fact>" Sync_EventFire). Called on the worker thread,
     *  never under a worker lock. */
    public interface Listener {
        /** fact is "started" {src,loop} or "finished" {src,reason}. */
        void onFact(String clip_key, String fact, Map<String, Object> payload);
        /** playing state edge: a first item started / the queue drained empty. */
        void onPlaying(boolean playing);
    }

    private final LinkedBlockingQueue<Item> m_queue;
    private final Backend m_backend;
    private final long m_loop_max_ms;
    private final Object m_lock = new Object();

    private volatile Listener m_listener;
    private volatile boolean m_running;
    private volatile boolean m_playing;
    private Thread m_thread;

    /** Set only under m_lock: the clip key of the item in progress
     *  (SAY_KEY for speech) and its stop flag / live playback. */
    private String m_current_key;
    private boolean m_stop_requested;
    private Playback m_current_playback;

    public SoundWorker(Backend backend, long loop_max_ms) {
        this(backend, loop_max_ms, QUEUE_CAPACITY);
    }

    public SoundWorker(Backend backend, long loop_max_ms, int queue_capacity) {
        this.m_backend = backend;
        this.m_loop_max_ms = loop_max_ms;
        this.m_queue = new LinkedBlockingQueue<>(Math.max(1, queue_capacity));
    }

    public void setListener(Listener listener) {
        this.m_listener = listener;
    }

    public void start() {
        synchronized (m_lock) {
            if (m_thread != null) return;
            m_running = true;
            m_thread = new Thread(this, "sound_worker");
            m_thread.setDaemon(true);
            m_thread.start();
        }
    }

    /** Ends the current item (finished {reason:"stopped"}), drops the
     *  queue and joins the worker thread. */
    public void shutdown() {
        final Thread thread;
        synchronized (m_lock) {
            m_running = false;
            thread = m_thread;
        }
        m_queue.clear();
        stopCurrent();
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            synchronized (m_lock) {
                m_thread = null;
            }
        }
    }

    /**
     * Enqueue an item; when the FIFO is full the oldest waiting item is
     * dropped to make room (same policy as the Python manager). Safe from
     * any thread, never blocks.
     */
    public void enqueue(Item item) {
        if (item == null) return;
        while (true) {
            if (m_queue.offer(item)) return;
            m_queue.poll();   // full: drop the oldest request, retry
        }
    }

    /** True when an item is playing right now (the advert's "playing" state). */
    public boolean isPlaying() {
        return m_playing;
    }

    /** True when the clip with this key is the item in progress. */
    public boolean isClipPlaying(String clip_key) {
        synchronized (m_lock) {
            return clip_key != null && clip_key.equals(m_current_key);
        }
    }

    /**
     * Drop the queued items of one clip and end it when it is playing
     * (finished reason "stopped"). Other clips keep their queue order.
     * Returns true when something was dropped or stopped.
     */
    public boolean stopClip(String clip_key) {
        boolean dropped = false;
        if (clip_key != null) {
            dropped = m_queue.removeIf(item ->
                    KIND_PLAY.equals(item.kind) && clip_key.equals(item.clip_key));
        }
        synchronized (m_lock) {
            if (clip_key != null && clip_key.equals(m_current_key)) {
                m_stop_requested = true;
                if (m_current_playback != null) m_current_playback.requestStop();
                dropped = true;
            }
        }
        return dropped;
    }

    /** Drop everything queued and end whatever is playing now.
     *  Returns true when something was playing or queued. */
    public boolean stopAll() {
        boolean dropped = !m_queue.isEmpty();
        m_queue.clear();
        synchronized (m_lock) {
            if (m_current_key != null) {
                m_stop_requested = true;
                dropped = true;
            }
            if (m_current_playback != null) m_current_playback.requestStop();
        }
        return dropped;
    }

    // ------------------------------------------------------------------
    // worker thread
    // ------------------------------------------------------------------

    @Override
    public void run() {
        while (m_running) {
            final Item item;
            try {
                item = m_queue.take();
            } catch (InterruptedException e) {
                if (!m_running) break;
                continue;
            }
            if (!m_running) break;

            _set_playing(true);
            try {
                synchronized (m_lock) {
                    m_current_key = item.clip_key;
                    m_stop_requested = false;
                    m_current_playback = null;
                }
                _notify_started(item);
                String reason = _play_item(item);
                _notify_finished(item, reason);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                _notify_finished(item, REASON_STOPPED);
                break;
            } catch (Exception e) {
                _notify_finished(item, REASON_ERROR);
            } finally {
                synchronized (m_lock) {
                    m_current_key = null;
                    m_current_playback = null;
                    m_stop_requested = false;
                }
                if (m_queue.isEmpty()) _set_playing(false);
            }
        }
        _set_playing(false);
    }

    /**
     * Plays one item — one started/finished pair per queued item, also
     * for a loop (not one pair per iteration). A loop repeats until a
     * stop, the loop_max_s cap (reason "limit") or a queued item
     * preempting it between iterations ("preempted") — D-SND-3.
     */
    private String _play_item(Item item) throws InterruptedException {
        String reason = REASON_DONE;
        final long deadline = item.loop
                ? System.nanoTime() + m_loop_max_ms * 1000000L : -1;
        while (true) {
            final int result = _play_once(item);
            if (result == Playback.RESULT_STOPPED) {
                reason = REASON_STOPPED;
                break;
            }
            if (result != Playback.RESULT_DONE) {
                reason = REASON_ERROR;
                break;
            }
            if (!item.loop) break;
            if (!m_queue.isEmpty()) {
                reason = REASON_PREEMPTED;
                break;
            }
            if (System.nanoTime() >= deadline) {
                reason = REASON_LIMIT;
                break;
            }
        }
        return reason;
    }

    /** One playthrough; returns the Playback result, mapping a start
     *  failure to RESULT_ERROR and a pre-set stop flag to RESULT_STOPPED. */
    private int _play_once(Item item) throws InterruptedException {
        final Playback playback;
        try {
            playback = m_backend.start(item);
        } catch (Exception e) {
            return Playback.RESULT_ERROR;
        }
        if (playback == null) return Playback.RESULT_ERROR;

        synchronized (m_lock) {
            m_current_playback = playback;
            if (m_stop_requested) playback.requestStop();
        }
        int result;
        try {
            result = playback.awaitEnd();
        } finally {
            synchronized (m_lock) {
                if (m_current_playback == playback) m_current_playback = null;
            }
        }
        synchronized (m_lock) {
            if (m_stop_requested) return Playback.RESULT_STOPPED;
        }
        return result;
    }

    private void stopCurrent() {
        synchronized (m_lock) {
            m_stop_requested = true;
            if (m_current_playback != null) m_current_playback.requestStop();
        }
    }

    private void _set_playing(boolean playing) {
        if (playing == m_playing) return;
        m_playing = playing;
        final Listener listener = m_listener;
        if (listener != null) {
            try {
                listener.onPlaying(playing);
            } catch (Exception ignored) {
            }
        }
    }

    private void _notify_started(Item item) {
        if (item.clip_key == null) return;   // a raw file play is silent
        final Listener listener = m_listener;
        if (listener == null) return;
        Map<String, Object> payload = new LinkedHashMap<>(2);
        payload.put("src", item.src);
        payload.put("loop", item.loop);
        try {
            listener.onFact(item.clip_key, "started", payload);
        } catch (Exception ignored) {
        }
    }

    private void _notify_finished(Item item, String reason) {
        if (item.clip_key == null) return;
        final Listener listener = m_listener;
        if (listener == null) return;
        Map<String, Object> payload = new LinkedHashMap<>(2);
        payload.put("src", item.src);
        payload.put("reason", reason);
        try {
            listener.onFact(item.clip_key, "finished", payload);
        } catch (Exception ignored) {
        }
    }
}
