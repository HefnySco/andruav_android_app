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

import org.json.JSONObject;

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

    /** AN-0168: told that the running de_mavlink must apply new runtime
     *  config. Fires when only the NTRIP keys changed, which never needs
     *  a module restart. */
    public interface RuntimeConfigListener {
        void onMavlinkRuntimeConfig(JSONObject config);
    }

    private static volatile RuntimeConfigListener sRuntimeConfigListener;

    public static void setRuntimeConfigListener(final RuntimeConfigListener listener) {
        sRuntimeConfigListener = listener;
    }

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

    /** The de_mavlink config text for these settings - asset template plus
     *  the failsafe and NTRIP seeds. Pure, so the unit tests can drive it. */
    static String mavlinkConfigText(final String templateJson, final boolean failsafeOn,
                                    final String failsafeMode, final DeConfigSettings s) {
        return seedMavlinkNtrip(seedMavlinkFailsafe(templateJson, failsafeOn, failsafeMode), s);
    }

    /** AN-0168: appends the NTRIP caster keys the native CNtripClient reads
     *  (the flat ntrip_* form). The password rides the owner-only config
     *  file - it is never sent on the bus, so it cannot reach logcat. */
    private static String seedMavlinkNtrip(final String json, final DeConfigSettings s) {
        final StringBuilder ntrip = new StringBuilder()
                .append(",\n")
                .append("    \"ntrip_enable\": ").append(s.ntripEnable).append(",\n")
                .append("    \"ntrip_host\": ").append(JSONObject.quote(s.ntripHost)).append(",\n")
                .append("    \"ntrip_port\": ").append(s.ntripPort).append(",\n")
                .append("    \"ntrip_mount\": ").append(JSONObject.quote(s.ntripMount)).append(",\n")
                .append("    \"ntrip_user\": ").append(JSONObject.quote(s.ntripUser)).append(",\n")
                .append("    \"ntrip_password\": ").append(JSONObject.quote(s.ntripPassword)).append(",\n")
                .append("    \"ntrip_send_gga\": ").append(s.ntripSendGga).append('\n');
        final int lastBrace = json.lastIndexOf('}');
        if (lastBrace < 0) {
            return json;
        }
        return json.substring(0, lastBrace) + ntrip + json.substring(lastBrace);
    }

    /** The parsed config with every top-level ntrip_* member removed.
     *  de_mavlink re-saves its file in normalized JSON (no comments,
     *  reordered keys) after an APPLY_CONFIG, so text comparison cannot
     *  be used - this parses instead. null when the text is not an object. */
    private static JSONObject withoutNtripKeys(final String json) {
        try {
            final JSONObject obj = new JSONObject(stripComments(json == null ? "" : json));
            final List<String> ntripKeys = new ArrayList<>();
            for (final java.util.Iterator<String> it = obj.keys(); it.hasNext(); ) {
                final String key = it.next();
                if (key.startsWith("ntrip_")) {
                    ntripKeys.add(key);
                }
            }
            for (final String key : ntripKeys) {
                obj.remove(key);
            }
            return obj;
        } catch (org.json.JSONException | RuntimeException e) {
            return null;
        }
    }

    /** Just the top-level ntrip_* members, in one object. */
    private static JSONObject ntripKeySection(final String json) {
        final JSONObject section = new JSONObject();
        try {
            final JSONObject obj = new JSONObject(stripComments(json == null ? "" : json));
            for (final java.util.Iterator<String> it = obj.keys(); it.hasNext(); ) {
                final String key = it.next();
                if (key.startsWith("ntrip_")) {
                    section.put(key, obj.get(key));
                }
            }
        } catch (org.json.JSONException | RuntimeException e) {
            // an unreadable file has no known ntrip members
        }
        return section;
    }

    /** Sorted-key serialization: two objects equal after this differ only
     *  in whitespace and key order - the module-normalized file still
     *  compares equal to the generated template text. */
    private static String canonical(final Object value) throws org.json.JSONException {
        if (value == null || value == JSONObject.NULL) {
            return "null";
        }
        if (value instanceof JSONObject) {
            final JSONObject obj = (JSONObject) value;
            final java.util.TreeSet<String> keys = new java.util.TreeSet<>();
            for (final java.util.Iterator<String> it = obj.keys(); it.hasNext(); ) {
                keys.add(it.next());
            }
            final StringBuilder out = new StringBuilder("{");
            for (final String key : keys) {
                if (out.length() > 1) out.append(',');
                out.append(JSONObject.quote(key)).append(':').append(canonical(obj.get(key)));
            }
            return out.append('}').toString();
        }
        if (value instanceof org.json.JSONArray) {
            final org.json.JSONArray arr = (org.json.JSONArray) value;
            final StringBuilder out = new StringBuilder("[");
            for (int i = 0; i < arr.length(); i++) {
                if (i > 0) out.append(',');
                out.append(canonical(arr.get(i)));
            }
            return out.append(']').toString();
        }
        if (value instanceof String) {
            return JSONObject.quote((String) value);
        }
        return String.valueOf(value);
    }

    /** JSON text without '//' and '/* *\/' comments; string literals are
     *  kept intact. */
    private static String stripComments(final String text) {
        final StringBuilder out = new StringBuilder(text.length());
        boolean inString = false, escaped = false;
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (inString) {
                out.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                out.append(c);
                continue;
            }
            if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
                while (i < text.length() && text.charAt(i) != '\n') i++;
                continue;
            }
            if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < text.length()
                        && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) i++;
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    /** True when the two config texts differ ONLY in their ntrip_* members -
     *  the change de_mavlink picks up live, without a restart. Compares
     *  parsed objects because the module rewrites its own file in
     *  normalized JSON after an apply. */
    static boolean ntripOnlyChange(final String oldConfig, final String newConfig) {
        try {
            final JSONObject oldJson = withoutNtripKeys(oldConfig);
            final JSONObject newJson = withoutNtripKeys(newConfig);
            if (oldJson == null || newJson == null) {
                return false;
            }
            return canonical(newJson).equals(canonical(oldJson))
                    && !canonical(ntripKeySection(newConfig))
                            .equals(canonical(ntripKeySection(oldConfig)));
        } catch (org.json.JSONException | RuntimeException e) {
            return false;
        }
    }

    /** The NTRIP keys sent to the running de_mavlink as the CONFIG_ACTION
     *  "c" payload. ntrip_password is NOT here on purpose: bus traffic is
     *  loggable. The config file carries it, and de_mavlink's own file
     *  watch applies it the same way. */
    public static JSONObject mavlinkNtripConfig(final DeConfigSettings s) {
        final JSONObject c = new JSONObject();
        try {
            c.put("ntrip_enable", s.ntripEnable);
            c.put("ntrip_host", s.ntripHost);
            c.put("ntrip_port", s.ntripPort);
            c.put("ntrip_mount", s.ntripMount);
            c.put("ntrip_user", s.ntripUser);
            c.put("ntrip_send_gga", s.ntripSendGga);
        } catch (org.json.JSONException e) {
            throw new IllegalStateException(e);
        }
        return c;
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
        // FCB sheet Disconnect: no FC link - de_mavlink points at the bridge,
        // and the bridge opens nothing
        s.fcType = Preference.isFCBLinkEnabled(null)
                ? Preference.getFCBTargetComm(null) : DeConfigSettings.FC_NONE;
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
        // AN-0168: NTRIP / RTK caster sheet.
        s.ntripEnable = Preference.isNtripEnabled(null);
        s.ntripHost = Preference.getNtripHost(null);
        try {
            s.ntripPort = Integer.parseInt(Preference.getNtripPort(null).trim());
        } catch (RuntimeException e) {
            // keep the default
        }
        s.ntripMount = Preference.getNtripMountPoint(null);
        s.ntripUser = Preference.getNtripUser(null);
        s.ntripPassword = Preference.getNtripPassword(null);
        s.ntripSendGga = Preference.isNtripSendGga(null);
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
            final boolean commArgs = writePrivate(new File(moduleDir(context, COMM), ARGS_DIGEST),
                    digest(DeConfigWriter.commArgs(s)));
            comm |= commArgs;
            // Same for de_mavlink: stock template, FC link from the FCB
            // screen on the command line. The digest also covers what the
            // FC link bridge reads (link kind, baud, BT MAC) so any FCB
            // screen change restarts the module and the bridge with it.
            final android.content.SharedPreferences prefs =
                    androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
            final boolean failsafeOn = prefs.getBoolean(PREF_FAILSAFE_ON_COMM_LOST, true);
            final String failsafeMode = prefs.getString(PREF_FAILSAFE_ON_COMM_LOST_MODE, "rtl");
            // AN-0168: the NTRIP keys are live-applied - a change there must
            // not restart the module, so the restart check compares the
            // file with the ntrip_* members stripped out of both sides.
            final File mavConfigFile =
                    new File(moduleDir(context, MAVLINK), MAVLINK + ".config.module.json");
            final String newMavConfig =
                    mavlinkConfigText(asset(context, MAVLINK), failsafeOn, failsafeMode, s);
            final String oldMavConfig = readPrivate(mavConfigFile);
            final boolean ntripOnly = ntripOnlyChange(oldMavConfig, newMavConfig);
            final boolean mavConfigWritten = writePrivate(mavConfigFile, newMavConfig);
            final boolean mavArgs = writePrivate(new File(moduleDir(context, MAVLINK), ARGS_DIGEST),
                    digest(DeConfigWriter.mavlinkArgs(s), s.fcType + "", s.fcBaud + "", s.fcBtMac,
                            failsafeOn + "", failsafeMode));
            boolean mav = (mavConfigWritten && !ntripOnly) || mavArgs;
            Log.i(TAG, "regenerate: comm=" + comm + "(args=" + commArgs + ")"
                    + " mav=" + mav + "(file=" + mavConfigWritten
                    + " args=" + mavArgs + " ntripOnly=" + ntripOnly + ")");
            if (comm) {
                changed.add(COMM);
            }
            if (mav) {
                changed.add(MAVLINK);
            } else if (ntripOnly) {
                // The file write alone applies it (de_mavlink watches its
                // config mtime); the listener adds the deterministic bus
                // path, CONFIG_ACTION_APPLY_CONFIG.
                final RuntimeConfigListener listener = sRuntimeConfigListener;
                if (listener != null) {
                    listener.onMavlinkRuntimeConfig(mavlinkNtripConfig(s));
                }
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

    /** File content as text; "" when absent. */
    private static String readPrivate(File file) throws IOException {
        if (!file.isFile()) {
            return "";
        }
        try (FileInputStream in = new FileInputStream(file)) {
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
