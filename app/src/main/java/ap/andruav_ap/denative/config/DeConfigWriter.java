package ap.andruav_ap.denative.config;

/**
 * Turns a {@link DeConfigSettings} snapshot into the de_comm connection argv
 * and the de_mavlink FC link argv (AN-0050). The modules' config files are
 * the stock templates, untouched.
 * Pure Java, no Android types.
 *
 * The templates are JSON with C-style comments, so values are replaced line
 * by line (never parsed and re-serialised) and every comment survives.
 * A key missing from a template throws, so template drift is caught by the
 * golden test instead of silently producing a config that ignores a setting.
 */
public final class DeConfigWriter {

    private DeConfigWriter() {
    }

    /**
     * de_comm's connection settings as command-line options. The app owns
     * this info (home page / settings), so it is handed to de_comm at start
     * and never written into de_comm.config.module.json or de_comm.local;
     * de_comm layers these over both files.
     */
    public static String[] commArgs(DeConfigSettings s) {
        return new String[]{
                "--auth_ip", nz(s.authHost),
                "--auth_port", Integer.toString(s.authPort),
                "--auth_verify_ssl", Boolean.toString(s.requireSsl),
                // The app's 'local server' (Andruav Cloud Server off) is a custom
                // AUTH server - its host/port are already authHost/authPort, the
                // same as the Java client. It is NOT de_comm's mesh direct-connect
                // mode, which would open a WebSocket on the auth port and be refused.
                "--use_local_comm_server", "false",
                "--userName", nz(s.email),
                "--accessCode", nz(s.accessCode),
                "--unitID", nz(s.unitName),
                "--groupID", nz(s.group),
                "--unitDescription", nz(s.description),
        };
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }

    /**
     * de_mavlink's FC link (the FCB screen) as command-line options:
     * tcp goes straight to the FCB screen's ip:port, udp listens on its port,
     * usb / bluetooth / none go through the FC link bridge (A8).
     */
    public static String[] mavlinkArgs(DeConfigSettings s) {
        if (s.fcType == DeConfigSettings.FC_TCP) {
            return new String[]{"--fcb_type", "tcp", "--fcb_ip", nz(s.fcTcpIp),
                    "--fcb_port", Integer.toString(s.fcTcpPort)};
        }
        if (s.fcType == DeConfigSettings.FC_UDP) {
            return new String[]{"--fcb_type", "udp", "--fcb_ip", "0.0.0.0",
                    "--fcb_port", Integer.toString(s.fcUdpPort)};
        }
        return new String[]{"--fcb_type", "tcp", "--fcb_ip", DeConfigSettings.BRIDGE_IP,
                "--fcb_port", Integer.toString(DeConfigSettings.BRIDGE_PORT)};
    }
}
