package ap.andruav_ap.denative.config;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Golden tests for the generated de_mavlink config seeds (AN-0168). */
public class DeConfigGeneratorTest {

    private static DeConfigSettings settings() {
        DeConfigSettings s = new DeConfigSettings();
        s.ntripEnable = true;
        s.ntripHost = "caster.example.com";
        s.ntripPort = 2101;
        s.ntripMount = "MOUNT_1";
        s.ntripUser = "rover1";
        s.ntripPassword = "topsecret";
        s.ntripSendGga = false;
        return s;
    }

    @Test
    public void mavlinkConfigSeedsNtripKeysFromSettings() throws Exception {
        final String json = DeConfigGenerator.mavlinkConfigText(
                "{\"failsafe_on_comm_lost\": false, \"other\": 1}", true, "rtl", settings());
        final JSONObject config = new JSONObject(json);
        assertEquals(true, config.getBoolean("ntrip_enable"));
        assertEquals("caster.example.com", config.getString("ntrip_host"));
        assertEquals(2101, config.getInt("ntrip_port"));
        assertEquals("MOUNT_1", config.getString("ntrip_mount"));
        assertEquals("rover1", config.getString("ntrip_user"));
        assertEquals("topsecret", config.getString("ntrip_password"));
        assertEquals(false, config.getBoolean("ntrip_send_gga"));
        // The rest of the file is untouched.
        assertEquals(1, config.getInt("other"));
    }

    @Test
    public void ntripOnlyChangeFlagsOnlyTheNtripDiff() {
        final DeConfigSettings s = settings();
        final String a = DeConfigGenerator.mavlinkConfigText(
                "{\"failsafe_on_comm_lost\": false, \"other\": 1}", true, "rtl", s);
        s.ntripMount = "MOUNT_2";
        final String b = DeConfigGenerator.mavlinkConfigText(
                "{\"failsafe_on_comm_lost\": false, \"other\": 1}", true, "rtl", s);
        assertTrue(DeConfigGenerator.ntripOnlyChange(a, b));

        // A non-ntrip difference is NOT an ntrip-only change: it restarts.
        final String c = b.replace("\"other\": 1", "\"other\": 2");
        assertFalse(DeConfigGenerator.ntripOnlyChange(b, c));

        // Identical files change nothing.
        assertFalse(DeConfigGenerator.ntripOnlyChange(a, a));
    }

    @Test
    public void ntripOnlyChangeWorksOnAModuleNormalizedFile() {
        // de_mavlink rewrites its config after an APPLY_CONFIG in normalized
        // JSON: comments gone, keys reordered, whitespace different. The
        // compare is semantic, so a later ntrip change still applies live.
        final DeConfigSettings s = settings();
        final String generated = DeConfigGenerator.mavlinkConfigText(
                "{\n// a comment\n\"other\": 1, \"failsafe_on_comm_lost\": false}",
                true, "rtl", s);
        final String normalized = "{\"ntrip_mount\":\"MOUNT_1\",\"failsafe_on_comm_lost\":true,"
                + "\"ntrip_port\":2101,\"other\":1,\"ntrip_user\":\"rover1\",\"ntrip_enable\":true,"
                + "\"ntrip_send_gga\":false,\"ntrip_password\":\"topsecret\","
                + "\"ntrip_host\":\"caster.example.com\"}";
        s.ntripMount = "MOUNT_2";
        final String next = DeConfigGenerator.mavlinkConfigText(
                "{\n// a comment\n\"other\": 1, \"failsafe_on_comm_lost\": false}",
                true, "rtl", s);
        assertTrue(DeConfigGenerator.ntripOnlyChange(normalized, next));
        assertTrue(DeConfigGenerator.ntripOnlyChange(generated, next));
        assertFalse(DeConfigGenerator.ntripOnlyChange(generated, generated));
    }

    @Test
    public void firstSeedAgainstAnOldConfigIsNtripOnly() {
        // A file written before the ntrip keys existed differs only by the
        // added ntrip block - the live apply adds them without a restart.
        assertTrue(DeConfigGenerator.ntripOnlyChange(
                "{\"other\": 1}",
                DeConfigGenerator.mavlinkConfigText("{\"other\": 1}", true, "rtl", settings())));
    }

    @Test
    public void applyConfigKeepsThePasswordOffTheBus() throws Exception {
        final JSONObject c = DeConfigGenerator.mavlinkNtripConfig(settings());
        assertFalse(c.has("ntrip_password"));
        assertEquals("caster.example.com", c.getString("ntrip_host"));
        assertEquals(2101, c.getInt("ntrip_port"));
        assertEquals("MOUNT_1", c.getString("ntrip_mount"));
        assertEquals("rover1", c.getString("ntrip_user"));
        assertEquals(true, c.getBoolean("ntrip_enable"));
        assertEquals(false, c.getBoolean("ntrip_send_gga"));
    }
}
