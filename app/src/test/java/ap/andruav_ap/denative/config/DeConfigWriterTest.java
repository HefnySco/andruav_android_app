package ap.andruav_ap.denative.config;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Golden tests for the generated module configs (AN-0050). */
public class DeConfigWriterTest {

    private static String template(String module) throws IOException {
        Path p = Paths.get("..", "deNative", "src", "main", "assets", module, module + ".config.module.json");
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    private static DeConfigSettings settings() {
        DeConfigSettings s = new DeConfigSettings();
        s.email = "pilot@example.com";
        s.accessCode = "s3cr3t\"code";
        s.unitName = "phone_unit_7";
        s.description = "Test phone";
        s.group = "alpha";
        s.authHost = "auth.example.com";
        s.authPort = 19999;
        s.requireSsl = true;
        s.fcType = DeConfigSettings.FC_USB;
        s.fcBaud = 115200;
        return s;
    }

    private static String arg(String[] args, String name) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(name)) {
                return args[i + 1];
            }
        }
        fail("no " + name + " in argv");
        return null;
    }

    @Test
    public void commArgsCarryTheConnection() {
        DeConfigSettings s = settings();
        String[] a = DeConfigWriter.commArgs(s);
        assertEquals("auth.example.com", arg(a, "--auth_ip"));
        assertEquals("19999", arg(a, "--auth_port"));
        assertEquals("true", arg(a, "--auth_verify_ssl"));
        assertEquals("pilot@example.com", arg(a, "--userName"));
        assertEquals("s3cr3t\"code", arg(a, "--accessCode"));
        assertEquals("phone_unit_7", arg(a, "--unitID"));
        assertEquals("alpha", arg(a, "--groupID"));
        assertEquals("Test phone", arg(a, "--unitDescription"));
        assertEquals(18, a.length);
    }

    @Test
    public void commArgsNeverNull() {
        DeConfigSettings s = new DeConfigSettings();
        s.email = null;
        s.accessCode = null;
        for (String v : DeConfigWriter.commArgs(s)) {
            assertTrue(v != null);
        }
    }

    @Test
    public void commTemplateHasNoConnectionInfo() throws IOException {
        String t = template("de_comm");
        assertFalse(t.contains("cloud.ardupilot.org"));
        assertFalse(t.contains("USERNAME_EMAIL"));
        assertFalse(t.contains("PASSWORD_HERE"));
    }

    @Test
    public void commNeverUsesMeshDirectMode() {
        DeConfigSettings s = settings();
        s.isLocalServer = true;
        s.authHost = "10.52.224.109";
        String[] a = DeConfigWriter.commArgs(s);
        assertEquals("false", arg(a, "--use_local_comm_server"));
        assertEquals("10.52.224.109", arg(a, "--auth_ip"));
    }

    @Test
    public void mavlinkTcpGoesStraightToFcbScreenTarget() {
        DeConfigSettings s = settings();
        s.fcType = DeConfigSettings.FC_TCP;
        s.fcTcpIp = "10.52.224.100";
        s.fcTcpPort = 6000;
        String[] a = DeConfigWriter.mavlinkArgs(s);
        assertEquals("tcp", arg(a, "--fcb_type"));
        assertEquals("10.52.224.100", arg(a, "--fcb_ip"));
        assertEquals("6000", arg(a, "--fcb_port"));
    }

    @Test
    public void mavlinkUdpListensOnFcbScreenPort() {
        DeConfigSettings s = settings();
        s.fcType = DeConfigSettings.FC_UDP;
        s.fcUdpPort = 14551;
        String[] a = DeConfigWriter.mavlinkArgs(s);
        assertEquals("udp", arg(a, "--fcb_type"));
        assertEquals("0.0.0.0", arg(a, "--fcb_ip"));
        assertEquals("14551", arg(a, "--fcb_port"));
    }

    @Test
    public void mavlinkUsbAndBluetoothUseTheBridge() {
        for (int t : new int[]{DeConfigSettings.FC_USB, DeConfigSettings.FC_BLUETOOTH, DeConfigSettings.FC_NONE}) {
            DeConfigSettings s = settings();
            s.fcType = t;
            String[] a = DeConfigWriter.mavlinkArgs(s);
            assertEquals("tcp", arg(a, "--fcb_type"));
            assertEquals(DeConfigSettings.BRIDGE_IP, arg(a, "--fcb_ip"));
            assertEquals(Integer.toString(DeConfigSettings.BRIDGE_PORT), arg(a, "--fcb_port"));
        }
    }
}
