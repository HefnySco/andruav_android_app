package ap.andruav_ap.denative.providers.sound;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import ap.andruavmiddlelibrary.factory.tts.TTS;

/**
 * Device side of the SoundWorker choke point (AN-0042): turns a queued
 * {@link SoundWorker.Item} into real Android audio — TextToSpeech for
 * say, MediaPlayer for clips (bundled res/raw built-ins and files in
 * getExternalFilesDir("sounds")). Every start() returns a latch-backed
 * Playback handle so the worker thread can block on it and emit the
 * started/finished facts from the same place.
 *
 * The siren plays on STREAM_ALARM so it stays audible when the media
 * stream is muted (TO-BE 5.1); everything else uses STREAM_MUSIC.
 */
public class AndroidSoundBackend implements SoundWorker.Backend {

    private static final String TAG = "AndroidSoundBackend";

    private final Context m_context;
    private final File m_sounds_dir;
    private final AtomicLong m_utterance_seq = new AtomicLong();

    /** One in-flight utterance; speakNowDirect only runs from the single
     *  worker thread, so a single latch holder is enough. */
    private volatile TtsPlayback m_current_utterance;

    public AndroidSoundBackend(Context context, File soundsDir) {
        this.m_context = context.getApplicationContext() != null
                ? context.getApplicationContext() : context;
        this.m_sounds_dir = soundsDir;
        TTS.getInstance().setUtteranceDoneListener(new TTS.UtteranceDoneListener() {
            @Override
            public void onDone(String utteranceId, boolean failed) {
                TtsPlayback playback = m_current_utterance;
                if (playback != null && playback.matches(utteranceId)) {
                    playback.finish(failed ? SoundWorker.Playback.RESULT_ERROR
                            : SoundWorker.Playback.RESULT_DONE);
                }
            }
        });
    }

    /** Device TTS locale tags for the advert's say lang/language enum. */
    public static List<String> deviceTtsLanguages() {
        List<String> tags = new ArrayList<>();
        try {
            tags.addAll(TTS.getInstance().getAvailableLanguageTags());
        } catch (Exception e) {
            Log.w(TAG, "deviceTtsLanguages failed", e);
        }
        if (tags.isEmpty()) tags.add(Locale.ENGLISH.toLanguageTag());
        return tags;
    }

    @Override
    public SoundWorker.Playback start(SoundWorker.Item item) {
        if (SoundWorker.KIND_SAY.equals(item.kind)) {
            return _start_say(item);
        }
        return _start_clip(item);
    }

    // ------------------------------------------------------------------
    // say
    // ------------------------------------------------------------------

    private SoundWorker.Playback _start_say(SoundWorker.Item item) {
        final String utteranceId = "de_sound_" + m_utterance_seq.incrementAndGet();
        TtsPlayback playback = new TtsPlayback(utteranceId);
        m_current_utterance = playback;
        boolean accepted = TTS.getInstance().speakNowDirect(item.text, item.language,
                item.pitch, item.volume, utteranceId);
        if (!accepted) {
            m_current_utterance = null;
            playback.finish(SoundWorker.Playback.RESULT_ERROR);
        }
        return playback;
    }

    private static final class TtsPlayback implements SoundWorker.Playback {
        private final CountDownLatch m_done = new CountDownLatch(1);
        private final AtomicInteger m_result =
                new AtomicInteger(SoundWorker.Playback.RESULT_DONE);
        private final String m_utterance_id;

        TtsPlayback(String utteranceId) {
            this.m_utterance_id = utteranceId;
        }

        boolean matches(String utteranceId) {
            return m_utterance_id != null && m_utterance_id.equals(utteranceId);
        }

        void finish(int result) {
            m_result.set(result);
            m_done.countDown();
        }

        @Override
        public int awaitEnd() throws InterruptedException {
            m_done.await();
            return m_result.get();
        }

        @Override
        public void requestStop() {
            TTS.getInstance().stopSpeaking();
            finish(SoundWorker.Playback.RESULT_STOPPED);
        }
    }

    // ------------------------------------------------------------------
    // clips
    // ------------------------------------------------------------------

    private SoundWorker.Playback _start_clip(SoundWorker.Item item) {
        try {
            MediaPlayer mediaPlayer = _create_player(item);
            if (mediaPlayer == null) return null;
            mediaPlayer.start();
            return new ClipPlayback(mediaPlayer);
        } catch (Exception e) {
            Log.e(TAG, "clip start failed: " + item.file, e);
            return null;
        }
    }

    private MediaPlayer _create_player(SoundWorker.Item item) throws Exception {
        final String locator = item.file != null ? item.file : "";
        if (locator.startsWith(SoundProvider.BUILTIN_PREFIX)) {
            return _create_builtin(locator.substring(SoundProvider.BUILTIN_PREFIX.length()));
        }
        if (m_sounds_dir == null || locator.isEmpty()) return null;

        // Same guard as SoundManager.playFile: the name must resolve to a
        // file inside the sounds directory - no traversal, no absolute paths.
        final File dirCanonical = m_sounds_dir.getCanonicalFile();
        final File file = new File(dirCanonical, locator).getCanonicalFile();
        if (!file.getPath().startsWith(dirCanonical.getPath() + File.separator)
                || !file.isFile()) {
            Log.w(TAG, "clip rejected path: " + locator);
            return null;
        }
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(_attributes(false));
        mediaPlayer.setDataSource(file.getAbsolutePath());
        mediaPlayer.prepare();
        return mediaPlayer;
    }

    private MediaPlayer _create_builtin(String key) {
        final int resource;
        switch (key) {
            case "siren": resource = ap.andruav_ap.R.raw.lostalarm; break;
            case "beep":  resource = ap.andruav_ap.R.raw.sputnik1;  break;
            case "error": resource = ap.andruav_ap.R.raw.taan;      break;
            default: return null;
        }
        return MediaPlayer.create(m_context, resource, _attributes("siren".equals(key)),
                AudioManager.AUDIO_SESSION_ID_GENERATE);
    }

    /** STREAM_ALARM for the siren so it is heard even with media muted. */
    private static AudioAttributes _attributes(boolean alarm) {
        return new AudioAttributes.Builder()
                .setLegacyStreamType(alarm ? AudioManager.STREAM_ALARM
                        : AudioManager.STREAM_MUSIC)
                .build();
    }

    private static final class ClipPlayback implements SoundWorker.Playback {
        private final CountDownLatch m_done = new CountDownLatch(1);
        private final AtomicInteger m_result =
                new AtomicInteger(SoundWorker.Playback.RESULT_DONE);
        private final MediaPlayer m_media_player;

        ClipPlayback(final MediaPlayer mediaPlayer) {
            this.m_media_player = mediaPlayer;
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    finish(SoundWorker.Playback.RESULT_DONE);
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    finish(SoundWorker.Playback.RESULT_ERROR);
                    return true;
                }
            });
        }

        void finish(int result) {
            m_result.set(result);
            try {
                m_media_player.release();
            } catch (Exception ignored) {
            }
            m_done.countDown();
        }

        @Override
        public int awaitEnd() throws InterruptedException {
            m_done.await();
            return m_result.get();
        }

        @Override
        public void requestStop() {
            try {
                m_media_player.stop();
            } catch (Exception ignored) {
            }
            finish(SoundWorker.Playback.RESULT_STOPPED);
        }
    }
}
