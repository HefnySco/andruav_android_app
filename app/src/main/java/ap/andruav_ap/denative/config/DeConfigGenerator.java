package ap.andruav_ap.denative.config;

import android.content.Context;
import android.system.Os;
import android.util.Log;

import com.andruav.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import ap.andruavmiddlelibrary.preference.Preference;

/**
 * Reads the app preferences, builds the module config files from the asset
 * templates and writes them to filesDir/de_comm and filesDir/de_mavlink
 * (AN-0050). Run it in the main app process (the preferences live there),
 * before the module service starts.
 */
public final class DeConfigGenerator {

    private static final String TAG = "DeConfigGenerator";

    public static final String COMM = "de_comm";
    public static final String MAVLINK = "de_mavlink";

    private DeConfigGenerator() {
    }

    /** Snapshot of the current preferences. */
    public static DeConfigSettings readSettings() {
        DeConfigSettings s = new DeConfigSettings();
        s.email = Preference.getLoginUserName(null);
        s.accessCode = Preference.getLoginAccessCode(null);
        s.unitName = Preference.getWebServerUserName(null);
        s.description = Preference.getWebServerUserDescription(null);
        s.group = Preference.getWebServerGroupName(null);
        s.authHost = Preference.getAuthServerURL(null);
        s.authPort = Preference.getAuthServerPort(null);
        s.requireSsl = Preference.isRequireSSL(null);
        s.fcType = Preference.getFCBTargetComm(null);
        int sel = Preference.getFCBUSBBaudRateSelector(null);
        int[] bauds = Constants.baudRateItemsValue;
        s.fcBaud = (sel >= 0 && sel < bauds.length) ? bauds[sel] : 57600;
        try {
            s.fcUdpPort = Integer.parseInt(Preference.getFCBDroneUDPServerPort(null).trim());
        } catch (RuntimeException e) {
            // keep the default
        }
        return s;
    }

    /**
     * Regenerates both modules' files. Only files whose content changed are
     * rewritten.
     *
     * @return the modules whose files changed (and so need a restart)
     */
    public static List<String> regenerate(Context context) {
        DeConfigSettings s = readSettings();
        List<String> changed = new ArrayList<>();
        try {
            boolean comm = writeModule(context, COMM,
                    DeConfigWriter.commConfig(asset(context, COMM), s), DeConfigWriter.commLocal(s));
            boolean mav = writeModule(context, MAVLINK,
                    DeConfigWriter.mavlinkConfig(asset(context, MAVLINK), s), DeConfigWriter.mavlinkLocal(s));
            if (comm) {
                changed.add(COMM);
            }
            if (mav) {
                changed.add(MAVLINK);
            }
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "config generation failed", e);
        }
        return changed;
    }

    private static String asset(Context context, String module) throws IOException {
        try (InputStream in = context.getAssets().open(module + "/" + module + ".config.module.json")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static boolean writeModule(Context context, String module, String config, String local)
            throws IOException {
        File dir = new File(context.getFilesDir(), module);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        boolean a = writePrivate(new File(dir, module + ".config.module.json"), config);
        boolean b = writePrivate(new File(dir, module + ".local"), local);
        return a || b;
    }

    /** Writes owner-only (0600); returns false when the content is already there. */
    private static boolean writePrivate(File file, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (file.isFile() && file.length() == bytes.length && sameBytes(file, bytes)) {
            return false;
        }
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(bytes);
            out.getFD().sync();
        }
        try {
            Os.chmod(tmp.getAbsolutePath(), 0600);
        } catch (Exception e) {
            throw new IOException("chmod failed for " + tmp, e);
        }
        if (!tmp.renameTo(file)) {
            throw new IOException("rename failed for " + file);
        }
        return true;
    }

    private static boolean sameBytes(File file, byte[] bytes) throws IOException {
        byte[] cur = new byte[bytes.length];
        try (FileInputStream in = new FileInputStream(file)) {
            int off = 0;
            while (off < cur.length) {
                int n = in.read(cur, off, cur.length - off);
                if (n < 0) {
                    return false;
                }
                off += n;
            }
        }
        return java.util.Arrays.equals(cur, bytes);
    }
}
