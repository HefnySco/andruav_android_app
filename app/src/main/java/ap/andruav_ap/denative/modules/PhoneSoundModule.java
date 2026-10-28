package ap.andruav_ap.denative.modules;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * AN-0107: the phone's sound as a DroneEngage sound module (class "snd"), the
 * wire surface of droneengage_sound_module, so the webclient shows its Audio
 * tab (play file + library dropdown) for a native unit. GCS-commanded
 * speech and play-file sound even while the app's speaker button is muted.
 * <ul>
 * <li>6511 SOUND_TEXT_TO_SPEECH {t, l?, p?, v?}: speak with language, pitch
 * 0..99 and volume 0..100.</li>
 * <li>6512 SOUND_PLAY_FILE {f}: play a library file.</li>
 * <li>1005 RemoteExecute {C:6530}: answered with 6530 SOUND_LIST
 * {T:[{n, f}], R:true}; the list is also broadcast (R:false) on start.</li>
 * </ul>
 * The app's bus client no longer subscribes 6511 - de_comm hands a message
 * to every subscribed module, so it would be spoken twice.
 */
public final class PhoneSoundModule {

    public static final String MODULE_ID = "andruav_sound";
    public static final String MODULE_KEY = "andruav_sound";
    public static final String MODULE_CLASS_SOUND = "snd";

    public static final int TYPE_REMOTE_EXECUTE = 1005;
    public static final int TYPE_SOUND_TEXT_TO_SPEECH = 6511;
    public static final int TYPE_SOUND_PLAY_FILE = 6512;
    public static final int TYPE_SOUND_LIST = 6530;

    static final String SRC_GCS = "gcs";

    /** A library entry as the webclient lists it. */
    public static final class Clip {
        final String name;
        final String file;

        public Clip(final String name, final String file) {
            this.name = name;
            this.file = file;
        }
    }

    /** The sound provider, narrowed to what the module needs. */
    public interface Sound {
        /** pitch / volume -1 = device default. */
        void say(String text, String language, int pitch, int volume, String src);

        void playFile(String file, String src);

        List<Clip> library();
    }

    private final Sound m_sound;
    private final PhoneGpioModule.Sender m_sender;

    public PhoneSoundModule(final Sound sound, final PhoneGpioModule.Sender sender) {
        m_sound = sound;
        m_sender = sender;
    }

    /** Message types the module subscribes to. */
    public static JSONArray messageFilter() {
        return new JSONArray().put(TYPE_REMOTE_EXECUTE).put(TYPE_SOUND_TEXT_TO_SPEECH)
                .put(TYPE_SOUND_PLAY_FILE);
    }

    public void onMessage(final JSONObject jMsg) {
        final int type = jMsg.optInt("mt", -1);
        final JSONObject ms = PhoneGpioModule.payload(jMsg);
        if (ms == null) return;
        if (type == TYPE_SOUND_TEXT_TO_SPEECH) {
            final String text = ms.optString("t", "");
            if (!text.isEmpty()) {
                m_sound.say(text, ms.optString("l", null),
                        ms.optInt("p", -1), ms.optInt("v", -1), SRC_GCS);
            }
        } else if (type == TYPE_SOUND_PLAY_FILE) {
            final String file = ms.optString("f", "");
            if (!file.isEmpty()) m_sound.playFile(file, SRC_GCS);
        } else if (type == TYPE_REMOTE_EXECUTE) {
            if (ms.optInt("C", -1) == TYPE_SOUND_LIST) {
                sendList(jMsg.optString("sd", ""), true);
            }
        }
    }

    /** Library to every GCS (module start). */
    public void announce() {
        sendList("", false);
    }

    private void sendList(final String target, final boolean reply) {
        try {
            final JSONArray files = new JSONArray();
            for (Clip clip : m_sound.library()) {
                files.put(new JSONObject().put("n", clip.name).put("f", clip.file));
            }
            m_sender.send(target, new JSONObject().put("T", files).put("R", reply), TYPE_SOUND_LIST);
        } catch (JSONException ignored) {
            // put() on literal keys/values does not throw
        }
    }
}
