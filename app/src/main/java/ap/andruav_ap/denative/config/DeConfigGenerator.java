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
 * Reads the app preferences, builds the de_mavlink config files from the
 * asset templates (filesDir/de_mavlink) and the de_comm connection argv
 * (AN-0050). de_comm's config files carry no connection info: the stock
 * template is used as-is and the settings arrive on its command line. Run it in the main app process (the preferences live there),
 * before the module service starts.
 */
public final class DeConfigGenerator {

    private static final String TAG = "DeConfigGenerator";

    public static final String COMM = "de_comm";
    public static final String MAVLINK = "de_mavlink";

    /** Preference keys that seed the comm-loss failsafe (DE-0027/AN-0074). */
    public static final String PREF_FAILSAFE_ON_COMM_LOST = "pref_failsafe_on_comm_lost";
    public static final String PREF_FAILSAFE_ON_COMM_LOST_MODE = "pref_failsafe_on_comm_lost_mode";

    private DeConfigGenerator() {
    }

    /** Seeds the failsafe keys in the de_mavlink module json from the
     *  preferences, so the generated config follows what the user sees. */
    private static String seedMavlinkFailsafe(final String json, final boolean enabled,
                                              final String mode) {
        final String safeMode = (mode == null || mode.trim().isEmpty()) ? "rtl"
                : mode.trim().replace("\"", "");
        String out = json.replaceAll("(\"failsafe_on_comm_lost\"\\s*:\\s*)(?:true|false)",
                "$1" + enabled);
        return out.replaceAll("(\"failsafe_on_comm_lost_mode\"\\s*:\\s*)\"[^\"]*\"",
                "$1\"" + safeMode + "\"");
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
        s.isLocalServer = Preference.isLocalServer(null);
        s.localServerHost = Preference.getLocalServerURL(null);
        s.localServerPort = Preference.getLocalServerPort(null);
        s.fcType = Preference.getFCBTargetComm(null);
        int sel = Preference.getFCBUSBBaudRateSelector(null);
        int[] bauds = Constants.baudRateItemsValue;
        s.fcBaud = (sel >= 0 && sel < bauds.length) ? bauds[sel] : 57600;
        try {
            s.fcUdpPort = Integer.parseInt(Preference.getFCBDroneUDPServerPort(null).trim());
        } catch (RuntimeException e) {
            // keep the default
        }
        s.fcTcpIp = Preference.getFCBDroneTCPServerIP(null);
        try {
            s.fcTcpPort = Integer.parseInt(Preference.getFCBDroneTCPServerPort(null).trim());
        } catch (RuntimeException e) {
            // keep the default
        }
        s.fcBtMac = Preference.getFCBBlueToothMAC(null);
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
            // de_comm's .local belongs to de_comm (party_id, module_key);
            // only reset the module json to the stock template, which also
            // drops connection info an older build wrote there.
            boolean comm = writePrivate(new File(moduleDir(context, COMM), COMM + ".config.module.json"),
                    asset(context, COMM));
            comm |= writePrivate(new File(moduleDir(context, COMM), ARGS_DIGEST),
                    digest(DeConfigWriter.commArgs(s)));
            // Same for de_mavlink: stock template, FC link from the FCB
            // screen on the command line. The digest also covers what the
            // FC link bridge reads (link kind, baud, BT MAC) so any FCB
            // screen change restarts the module and the bridge with it.
            final android.content.SharedPreferences prefs =
                    androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
            final boolean failsafeOn = prefs.getBoolean(PREF_FAILSAFE_ON_COMM_LOST, true);
            final String failsafeMode = prefs.getString(PREF_FAILSAFE_ON_COMM_LOST_MODE, "rtl");
            boolean mav = writePrivate(new File(moduleDir(context, MAVLINK), MAVLINK + ".config.module.json"),
                    seedMavlinkFailsafe(asset(context, MAVLINK), failsafeOn, failsafeMode));
            mav |= writePrivate(new File(moduleDir(context, MAVLINK), ARGS_DIGEST),
                    digest(DeConfigWriter.mavlinkArgs(s), s.fcType + "", s.fcBaud + "", s.fcBtMac,
                            failsafeOn + "", failsafeMode));
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

    /** de_comm's connection argv from the current preferences. */
    public static String[] commArgs() {
        return DeConfigWriter.commArgs(readSettings());
    }

    /** de_mavlink's FC link argv from the current FCB screen settings. */
    public static String[] mavlinkArgs() {
        return DeConfigWriter.mavlinkArgs(readSettings());
    }

    // Only a digest of the argv is kept on disk, to notice a settings change
    // that needs a de_comm restart - never the values (access code).
    private static final String ARGS_DIGEST = ".conn_args.sha256";

    private static String digest(String[] args, String... extra) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            for (String a : args) {
                md.update(a.getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
            }
            for (String a : extra) {
                md.update(String.valueOf(a).getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
            }
            StringBuilder b = new StringBuilder();
            for (byte x : md.digest()) {
                b.append(String.format("%02x", x));
            }
            return b.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static File moduleDir(Context context, String module) {
        File dir = new File(context.getFilesDir(), module);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return dir;
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
