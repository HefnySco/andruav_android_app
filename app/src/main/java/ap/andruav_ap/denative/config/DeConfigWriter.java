package ap.andruav_ap.denative.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the A6 asset templates plus a {@link DeConfigSettings} snapshot into
 * the four module config files (AN-0050). Pure Java, no Android types.
 *
 * The templates are JSON with C-style comments, so values are replaced line
 * by line (never parsed and re-serialised) and every comment survives.
 * A key missing from a template throws, so template drift is caught by the
 * golden test instead of silently producing a config that ignores a setting.
 *
 * The access code goes ONLY into the .local file; the module json carries an
 * empty accessCode.
 */
public final class DeConfigWriter {

    private DeConfigWriter() {
    }

    public static String commConfig(String template, DeConfigSettings s) {
        String out = template;
        out = setTopLevel(out, "auth_ip", quote(s.authHost));
        out = setTopLevel(out, "auth_port", Integer.toString(s.authPort));
        out = setTopLevel(out, "auth_verify_ssl", Boolean.toString(s.requireSsl));
        out = setTopLevel(out, "userName", quote(s.email));
        out = setTopLevel(out, "accessCode", quote(""));
        out = setTopLevel(out, "unitID", quote(s.unitName));
        out = setTopLevel(out, "groupID", quote(s.group));
        out = setTopLevel(out, "unitDescription", quote(s.description));
        return out;
    }

    public static String commLocal(DeConfigSettings s) {
        return "{\n    \"accessCode\": " + quote(s.accessCode) + "\n}\n";
    }

    public static String mavlinkConfig(String template, DeConfigSettings s) {
        final String type;
        final String ip;
        final int port;
        final String note;
        if (s.fcType == DeConfigSettings.FC_UDP) {
            type = "udp";
            ip = "0.0.0.0";
            port = s.fcUdpPort;
            note = "udp, de_mavlink listens itself";
        } else {
            // usb, bluetooth, tcp and 'none' all go through the A8 bridge.
            type = "tcp";
            ip = DeConfigSettings.BRIDGE_IP;
            port = DeConfigSettings.BRIDGE_PORT;
            note = fcName(s.fcType) + (s.fcType == DeConfigSettings.FC_USB ? " @" + s.fcBaud : "")
                    + " via the FC link bridge";
        }
        return setUriBlock(template, type, ip, port, "// FC source (generated from app settings): " + note);
    }

    public static String mavlinkLocal(DeConfigSettings s) {
        return "{\n}\n";
    }

    static String fcName(int fcType) {
        switch (fcType) {
            case DeConfigSettings.FC_USB: return "usb";
            case DeConfigSettings.FC_BLUETOOTH: return "bluetooth";
            case DeConfigSettings.FC_TCP: return "tcp";
            case DeConfigSettings.FC_UDP: return "udp";
            default: return "none";
        }
    }

    static String quote(String v) {
        if (v == null) {
            v = "";
        }
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
            }
        }
        return b.append('"').toString();
    }

    private static final String VALUE = "(\"(?:[^\"\\\\]|\\\\.)*\"|[^,/\\s}]+)";

    private static String setTopLevel(String text, String key, String jsonValue) {
        Pattern p = Pattern.compile("^(\\s*\"" + Pattern.quote(key) + "\"\\s*:\\s*)" + VALUE + "(.*)$");
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().startsWith("//")) {
                continue;
            }
            Matcher m = p.matcher(lines[i]);
            if (m.matches()) {
                lines[i] = m.group(1) + jsonValue + m.group(3);
                return String.join("\n", lines);
            }
        }
        throw new IllegalStateException("template has no key " + key);
    }

    /** Rewrites ip/port/type inside the live (uncommented) fcb_connection_uri block. */
    private static String setUriBlock(String text, String type, String ip, int port, String comment) {
        String[] lines = text.split("\n", -1);
        StringBuilder out = new StringBuilder();
        boolean inBlock = false;
        boolean found = false;
        int seen = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean commented = line.trim().startsWith("//");
            if (!commented && !inBlock && line.contains("\"fcb_connection_uri\"")) {
                inBlock = true;
                found = true;
                out.append(leadingSpace(line)).append(comment).append('\n');
            } else if (inBlock && !commented) {
                String t = line.trim();
                if (t.startsWith("\"ip\"")) {
                    line = replaceValue(line, "ip", quote(ip));
                    seen++;
                } else if (t.startsWith("\"port\"")) {
                    line = replaceValue(line, "port", Integer.toString(port));
                    seen++;
                } else if (t.startsWith("\"type\"")) {
                    line = replaceValue(line, "type", quote(type));
                    seen++;
                } else if (t.startsWith("}")) {
                    inBlock = false;
                }
            }
            out.append(line);
            if (i < lines.length - 1) {
                out.append('\n');
            }
        }
        if (!found || seen != 3) {
            throw new IllegalStateException("template has no complete fcb_connection_uri block");
        }
        return out.toString();
    }

    private static String replaceValue(String line, String key, String jsonValue) {
        Matcher m = Pattern.compile("^(\\s*\"" + key + "\"\\s*:\\s*)" + VALUE + "(.*)$").matcher(line);
        if (!m.matches()) {
            throw new IllegalStateException("cannot rewrite line: " + line);
        }
        return m.group(1) + jsonValue + m.group(3);
    }

    private static String leadingSpace(String line) {
        int i = 0;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        return line.substring(0, i);
    }
}
