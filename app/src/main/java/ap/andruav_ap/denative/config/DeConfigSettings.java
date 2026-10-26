package ap.andruav_ap.denative.config;

/**
 * Plain snapshot of the app preferences that feed the native module configs
 * (AN-0050). No Android types, so the writer can be unit-tested on the JVM.
 */
public final class DeConfigSettings {

    /** FC link kinds, mirroring Preference.FCB_COM_*. */
    public static final int FC_NONE = 0;
    public static final int FC_BLUETOOTH = 1;
    public static final int FC_USB = 2;
    public static final int FC_TCP = 3;
    public static final int FC_UDP = 4;

    /** The FC link bridge (A8) serves the FC byte stream here. */
    public static final String BRIDGE_IP = "127.0.0.1";
    public static final int BRIDGE_PORT = 5760;

    public String email = "";
    public String accessCode = "";
    public String unitName = "";
    public String description = "";
    public String group = "";
    public String authHost = "";
    public int authPort = 0;
    public boolean requireSsl = true;
    public int fcType = FC_NONE;
    public int fcBaud = 57600;
    public int fcUdpPort = 14550;

    public DeConfigSettings() {
    }
}
