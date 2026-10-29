package ap.andruav_ap.denative.modules;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The phone camera as a DroneEngage camera module (class "camera"), so a
 * native unit looks like a Pi running de_camera:
 * <ul>
 * <li>features "C"/"V" make de_comm set the image/video letters of the unit
 * permission string ({@code p[10]='C'}, {@code p[8]='V'}) that the webclient's
 * fn_canCamera / fn_canVideo read - andruav_phone is a "gen" module, so
 * without this the camera and video buttons stay grey;</li>
 * <li>{@code m} carries the camera list in the de_camera format, which de_comm
 * answers 1012 CameraList requests from (de_modules_manager updateCameraList
 * reads id, ln, r, v, active, p and s from every entry).</li>
 * </ul>
 * Only the registration lives here. The camera commands (1005 remote execute,
 * 1021 signaling, zoom/switch/flash, take image) stay on andruav_phone, which
 * already runs them through the app's FPV and still-capture code; the list
 * here is the same one that code answers with, so both 1012 replies carry
 * the same camera ids.
 * <p>
 * Features are advertised only once the phone has a camera and the app holds
 * the CAMERA permission; PhoneBusModules opens the module only then, since
 * de_comm reads features at first registration. de_comm never clears a
 * permission letter, so a feature is never withdrawn here either.
 */
public final class PhoneCameraModule {

    public static final String MODULE_ID = "andruav_camera";
    public static final String MODULE_KEY = "andruav_camera";

    public static final String FEATURE_CAPTURE_IMAGE = "C";
    public static final String FEATURE_CAPTURE_VIDEO = "V";

    /** Keys de_comm reads unconditionally from every camera entry. */
    static final String[] REQUIRED_KEYS = {"id", "ln", "r", "v", "active", "p", "s"};

    /** The app's camera surface. */
    public interface Cameras {
        /** True when the phone has a camera and the app holds CAMERA. */
        boolean usable();

        /** Camera list in the 1012 "T" entry format. */
        JSONArray list();
    }

    /** Writes the registration fields onto the module ID message. */
    public interface Registrar {
        void addFeature(String feature);

        /** Sets the {@code m} list and re-sends the module ID. */
        void setCameraList(JSONArray cameras);
    }

    private final Cameras m_cameras;
    private final Registrar m_registrar;
    private boolean m_features_sent;
    private String m_last_list;

    public PhoneCameraModule(final Cameras cameras, final Registrar registrar) {
        m_cameras = cameras;
        m_registrar = registrar;
    }

    /** True when the phone camera can be advertised now. */
    public boolean isUsable() {
        return m_cameras.usable();
    }

    /** Nothing is subscribed - see the class comment. */
    public static JSONArray messageFilter() {
        return new JSONArray();
    }

    /**
     * Brings the registration up to date: called before the module socket
     * opens and then on the poll, so a permission granted later or a camera
     * starting to stream reaches de_comm. Returns true when anything changed.
     */
    public boolean refresh() {
        boolean changed = false;
        final boolean usable = m_cameras.usable();
        if (usable && !m_features_sent) {
            m_registrar.addFeature(FEATURE_CAPTURE_IMAGE);
            m_registrar.addFeature(FEATURE_CAPTURE_VIDEO);
            m_features_sent = true;
            changed = true;
        }

        final JSONArray list = usable ? entries(m_cameras.list()) : new JSONArray();
        final String text = list.toString();
        if (changed || !text.equals(m_last_list)) {
            m_last_list = text;
            m_registrar.setCameraList(list);
            changed = true;
        }
        return changed;
    }

    /** Entries de_comm can read; one missing a required key would throw there. */
    static JSONArray entries(final JSONArray source) {
        final JSONArray out = new JSONArray();
        if (source == null) return out;
        for (int i = 0; i < source.length(); i++) {
            final JSONObject entry = source.optJSONObject(i);
            if (entry == null) continue;
            boolean complete = true;
            for (String key : REQUIRED_KEYS) {
                if (!entry.has(key)) {
                    complete = false;
                    break;
                }
            }
            if (complete) out.put(entry);
        }
        return out;
    }
}
