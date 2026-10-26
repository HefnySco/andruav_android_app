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

    /** Applies the expected edits with plain string replacement on the template. */
    private static String expectedComm(String t) {
        return t
                .replace("\"auth_ip\"                       : \"cloud.ardupilot.org\"",
                        "\"auth_ip\"                       : \"auth.example.com\"")
                .replace("\"auth_port\"                     : 19408", "\"auth_port\"                     : 19999")
                .replace("\"auth_verify_ssl\"               : false", "\"auth_verify_ssl\"               : true")
                .replace("\"userName\"                  :\"USERNAME_EMAIL\"", "\"userName\"                  :\"pilot@example.com\"")
                .replace("\"accessCode\"                :\"PASSWORD_HERE\"", "\"accessCode\"                :\"\"")
                .replace("\"unitID\"                    :\"drone_cairo\"", "\"unitID\"                    :\"phone_unit_7\"")
                .replace("\"groupID\"                   :\"1\"", "\"groupID\"                   :\"alpha\"")
                .replace("\"unitDescription\"           :\"de_unit 1\"", "\"unitDescription\"           :\"Test phone\"");
    }

    @Test
    public void commConfigGolden() throws IOException {
        String t = template("de_comm");
        String expected = expectedComm(t);
        assertFalse("template drifted: expected edits did not apply", expected.equals(t));
        assertEquals(expected, DeConfigWriter.commConfig(t, settings()));
    }

    @Test
    public void accessCodeOnlyInLocalFile() throws IOException {
        DeConfigSettings s = settings();
        String comm = DeConfigWriter.commConfig(template("de_comm"), s);
        String mav = DeConfigWriter.mavlinkConfig(template("de_mavlink"), s);
        assertFalse(comm.contains("s3cr3t"));
        assertFalse(mav.contains("s3cr3t"));
        assertFalse(DeConfigWriter.mavlinkLocal(s).contains("s3cr3t"));
        assertEquals("{\n    \"accessCode\": \"s3cr3t\\\"code\"\n}\n", DeConfigWriter.commLocal(s));
        assertEquals("{\n}\n", DeConfigWriter.mavlinkLocal(s));
    }

    @Test
    public void commentsAreKept() throws IOException {
        String t = template("de_comm");
        String out = DeConfigWriter.commConfig(t, settings());
        assertTrue(out.contains("// Drone-Engage Communication Server Connection"));
        assertEquals(t.split("\n", -1).length, out.split("\n", -1).length);
    }

    @Test
    public void mavlinkUsbGoesThroughBridge() throws IOException {
        String t = template("de_mavlink");
        String out = DeConfigWriter.mavlinkConfig(t, settings());
        String expected = t.replace("    \"fcb_connection_uri\": {\n        \"ip\": \"127.0.0.1\"",
                "    // FC source (generated from app settings): usb @115200 via the FC link bridge\n"
                        + "    \"fcb_connection_uri\": {\n        \"ip\": \"127.0.0.1\"");
        assertFalse(expected.equals(t));
        assertEquals(expected, out);
    }

    @Test
    public void mavlinkUdpListensItself() throws IOException {
        DeConfigSettings s = settings();
        s.fcType = DeConfigSettings.FC_UDP;
        s.fcUdpPort = 14551;
        String out = DeConfigWriter.mavlinkConfig(template("de_mavlink"), s);
        assertTrue(out.contains("        \"ip\": \"0.0.0.0\",\n        \"port\": 14551,\n        \"type\": \"udp\""));
        // the commented example block is untouched
        assertTrue(out.contains("    //     \"port\": 5760,"));
    }

    @Test
    public void unitNameChangeRenamesUnit() throws IOException {
        DeConfigSettings s = settings();
        String a = DeConfigWriter.commConfig(template("de_comm"), s);
        s.unitName = "renamed";
        String b = DeConfigWriter.commConfig(template("de_comm"), s);
        assertTrue(a.contains("\"phone_unit_7\""));
        assertTrue(b.contains("\"renamed\""));
        assertFalse(b.contains("phone_unit_7"));
    }

    @Test
    public void escapesSpecialCharacters() throws IOException {
        DeConfigSettings s = settings();
        s.unitName = "a\\b\"c\nd";
        String out = DeConfigWriter.commConfig(template("de_comm"), s);
        assertTrue(out.contains("\"a\\\\b\\\"c\\nd\""));
    }

    @Test
    public void missingKeyThrows() {
        try {
            DeConfigWriter.commConfig("{ \"auth_ip\": \"x\" }", settings());
            fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // ok
        }
    }
}
