package ap.andruav_ap.denative.providers.sound;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Loads the "sound" provider config for the andruav_phone module
 * (AN-0042). Keys: sound_files[] {key?, name, file, events?},
 * loop_max_s, events_enabled — the same names the Linux
 * droneengage_sound_module config uses.
 *
 * Lookup order mirrors DeModuleService's override convention: an
 * adb-pushable file under getExternalFilesDir("de_override")/andruav_phone/
 * wins, else the module's own files dir. A missing/unreadable file is
 * not an error — the built-in siren/beep/error clips are always there.
 */
public final class SoundProviderConfig {

    private static final String TAG = "SoundProviderConfig";

    /** Config file name inside the andruav_phone module dir. */
    public static final String FILE_NAME = "sound.config.json";
    public static final String MODULE_DIR = "andruav_phone";

    private SoundProviderConfig() {}

    public static JSONObject load(Context context) {
        File override = new File(new File(context.getExternalFilesDir("de_override"),
                MODULE_DIR), FILE_NAME);
        JSONObject config = _read(override);
        if (config != null) return config;

        File local = new File(new File(context.getFilesDir(), MODULE_DIR), FILE_NAME);
        config = _read(local);
        return config != null ? config : new JSONObject();
    }

    private static JSONObject _read(File file) {
        if (file == null || !file.isFile()) return null;
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) Math.min(file.length(), 1 << 20)];
            int total = 0;
            int read;
            while (total < buffer.length && (read = in.read(buffer, total, buffer.length - total)) > 0) {
                total += read;
            }
            return new JSONObject(new String(buffer, 0, total, "UTF-8"));
        } catch (Exception e) {
            Log.e(TAG, "failed to read " + file, e);
            return null;
        }
    }
}
