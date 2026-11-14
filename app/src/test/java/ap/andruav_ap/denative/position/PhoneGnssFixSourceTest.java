package ap.andruav_ap.denative.position;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Map;

import de.databus.CModule;
import de.databus.CUDPClient;
import de.databus.Messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ap.andruav_ap.denative.position.PhoneGnssFixSource.GnssFixSample;
import ap.andruav_ap.denative.position.PhoneGnssFixSource.Switches;

/**
 * Field mapping and send gating for the phone-GNSS 9103 sender
 * (AN-0167). The wire contract is de_mavlink's parseGnssFixJson:
 * required la/ln/a/fix/ns, every optional field absent rather than 0.
 */
public class PhoneGnssFixSourceTest {

    private static final Switches ALL_ON =
            new Switches(true, false, false);
    private static final Switches HEADING_ON =
            new Switches(true, false, true);

    /** A full 3D fix with every optional value present. */
    private static GnssFixSample fullFix() {
        return new GnssFixSample(
                PhoneGnssFixSource.GPS_PROVIDER_NAME,
                30.0456789, -97.123456,
                true, 584.0,
                3, 4,                  // GSA 3D, GGA RTK fixed
                12,                    // sats used in fix
                0.9f, 1.3f,            // hdop, vdop
                1.5, 2.5, 0.3,         // h/v/speed accuracies (m, m, m/s)
                true, 1.3,             // has speed 1.3 m/s
                true, 90.0,            // bearing east
                true, 18000,           // yaw 180 deg
                1700000000000L,        // fix UTC ms
                5000L);                // elapsed ms
    }

    /** A minimal fix: required fields only, every optional absent. */
    private static GnssFixSample bareFix() {
        return new GnssFixSample(
                PhoneGnssFixSource.GPS_PROVIDER_NAME,
                30.0, -97.0,
                true, 100.0,
                3, 1, 7,
                0.0f, 0.0f,
                Double.NaN, Double.NaN, Double.NaN,
                false, 0.0,
                false, 0.0,
                false, 0,
                1700000000000L, 5000L);
    }

    @Test
    public void fullFixMapsEveryWireField() {
        Map<String, Object> ms =
                PhoneGnssFixSource.buildGnssFixFields(fullFix(), HEADING_ON, 0);
        assertEquals(300456789, ms.get("la"));
        assertEquals(-971234560, ms.get("ln"));
        assertEquals(584000, ms.get("a"));
        // GGA quality 4 (RTK fixed) beats the GSA 3D mode
        assertEquals(4, ms.get("fix"));
        assertEquals(12, ms.get("ns"));
        assertEquals(90, ms.get("hd"));     // 0.9 x 100
        assertEquals(130, ms.get("vd"));    // 1.3 x 100
        assertEquals(1500L, ms.get("ha"));  // 1.5 m in mm
        assertEquals(2500L, ms.get("va"));
        assertEquals(300L, ms.get("sa"));
        // 1.3 m/s due east: vn ~ 0, ve ~ 1300 mm/s
        assertEquals(0L, ms.get("vn"));
        assertEquals(1300L, ms.get("ve"));
        assertFalse(ms.containsKey("vdn")); // never sent - no vertical speed
        assertEquals(18000, ms.get("yw"));
        assertEquals("phone", ms.get("src"));
        assertTrue(ms.containsKey("tw"));
        assertTrue(ms.containsKey("tms"));
    }

    @Test
    public void absentOptionalValuesAreNotSent() {
        Map<String, Object> ms =
                PhoneGnssFixSource.buildGnssFixFields(bareFix(), HEADING_ON, 0);
        // required fields still present
        assertEquals(300000000, ms.get("la"));
        assertEquals(-970000000, ms.get("ln"));
        assertEquals(100000, ms.get("a"));
        assertEquals(3, ms.get("fix"));   // GGA 1 < 4, GSA mode wins
        assertEquals(7, ms.get("ns"));
        // every optional field absent - never a 0 the EKF reads as truth
        for (String key : new String[]{"hd", "vd", "ha", "va", "sa",
                "vn", "ve", "vdn", "yw"}) {
            assertFalse("key " + key + " must be absent", ms.containsKey(key));
        }
    }

    @Test
    public void networkProviderFixIsNeverSent() {
        for (String provider : new String[]{"network", "fused", "passive"}) {
            GnssFixSample fix = new GnssFixSample(
                    provider, 30.0, -97.0, true, 100.0,
                    3, 1, 7, 0.9f, 1.3f,
                    1.5, 2.5, 0.3, true, 1.0, true, 90.0,
                    true, 9000, 1700000000000L, 5000L);
            assertNull(provider + " fix must not send",
                    PhoneGnssFixSource.buildGnssFixFields(fix, ALL_ON, 0));
        }
    }

    @Test
    public void sendGates() {
        // gps_inject off -> nothing
        assertNull(PhoneGnssFixSource.buildGnssFixFields(fullFix(),
                new Switches(false, false, true), 0));
        // "Ignore Mobile Sensors" on -> nothing
        assertNull(PhoneGnssFixSource.buildGnssFixFields(fullFix(),
                new Switches(true, true, true), 0));
        // a fix without altitude cannot fill the required "a"
        GnssFixSample noAlt = new GnssFixSample(
                PhoneGnssFixSource.GPS_PROVIDER_NAME,
                30.0, -97.0, false, 0.0,
                3, 1, 7, 0.9f, 1.3f, 1.5, 2.5, 0.3,
                true, 1.0, true, 90.0, true, 9000,
                1700000000000L, 5000L);
        assertNull(PhoneGnssFixSource.buildGnssFixFields(noAlt, ALL_ON, 0));
        // a stale fix does not go out - the FC must see the GPS drop
        assertNull(PhoneGnssFixSource.buildGnssFixFields(fullFix(),
                ALL_ON, 3000));
        assertNull(PhoneGnssFixSource.buildGnssFixFields(fullFix(),
                ALL_ON, -1));
    }

    @Test
    public void headingPreferenceGatesYaw() {
        assertFalse(PhoneGnssFixSource
                .buildGnssFixFields(fullFix(), ALL_ON, 0)
                .containsKey("yw"));
        assertEquals(18000, PhoneGnssFixSource
                .buildGnssFixFields(fullFix(), HEADING_ON, 0).get("yw"));
    }

    @Test
    public void stationaryFixSendsExplicitZeroVelocity() {
        GnssFixSample stopped = new GnssFixSample(
                PhoneGnssFixSource.GPS_PROVIDER_NAME,
                30.0, -97.0, true, 100.0, 3, 1, 7,
                0.9f, 1.3f, 1.5, 2.5, 0.3,
                true, 0.0,            // speed 0
                false, 0.0,           // no bearing - common on phones
                false, 0, 1700000000000L, 5000L);
        Map<String, Object> ms =
                PhoneGnssFixSource.buildGnssFixFields(stopped, ALL_ON, 0);
        assertEquals(0, ms.get("vn"));
        assertEquals(0, ms.get("ve"));
    }

    @Test
    public void movingFixWithoutBearingIgnoresVelocity() {
        GnssFixSample moving = new GnssFixSample(
                PhoneGnssFixSource.GPS_PROVIDER_NAME,
                30.0, -97.0, true, 100.0, 3, 1, 7,
                0.9f, 1.3f, 1.5, 2.5, 0.3,
                true, 2.0,            // moving
                false, 0.0,           // no bearing - vn/ve unknown
                false, 0, 1700000000000L, 5000L);
        Map<String, Object> ms =
                PhoneGnssFixSource.buildGnssFixFields(moving, ALL_ON, 0);
        assertFalse(ms.containsKey("vn"));
        assertFalse(ms.containsKey("ve"));
    }

    @Test
    public void gpsTimeIsWeekAndTowMs() {
        // 2023-11-14T22:13:20Z = Unix 1700000000000 ms; GPS is +18 s.
        Map<String, Object> ms =
                PhoneGnssFixSource.buildGnssFixFields(fullFix(), ALL_ON, 0);
        long gpsMs = 1700000000000L - 315964800000L + 18000L;
        long week = gpsMs / (7L * 86400L * 1000L);
        long tow = gpsMs % (7L * 86400L * 1000L);
        assertEquals((int) week, ms.get("tw"));
        assertEquals(tow, ms.get("tms"));
        // fix age advances the stamp so a late tick still moves time on
        Map<String, Object> aged =
                PhoneGnssFixSource.buildGnssFixFields(fullFix(), ALL_ON, 1500);
        assertEquals(tow + 1500L, aged.get("tms"));
    }

    // ---------------------------------------------------------- emit

    /** CModule fake recording what sendJMSG would put on the bus. */
    private static class FakeModule extends CModule {
        JSONObject lastJmsg;
        int lastType = -1;
        boolean lastInternal;
        int sendCount;

        FakeModule() {
            this.cUDPClient = new CUDPClient();
        }

        @Override
        public void sendJMSG(String targetPartyID, JSONObject jmsg,
                             int andruav_message_id, boolean internal_message) {
            lastJmsg = jmsg;
            lastType = andruav_message_id;
            lastInternal = internal_message;
            sendCount++;
        }
    }

    private static class FakeBackend
            implements PhoneGnssFixSource.GnssBackend {
        GnssFixSample fix;
        long nowMs = 6000;

        @Override
        public GnssFixSample pollLatestFix() {
            return fix;
        }

        @Override
        public long nowElapsedMs() {
            return nowMs;
        }
    }

    private static PhoneGnssFixSource source(FakeBackend backend,
                                             FakeModule module) {
        return new PhoneGnssFixSource(backend, module, () -> ALL_ON);
    }

    @Test
    public void tickSendsEachNewFixOnceAsIntermodule9103() {
        FakeBackend backend = new FakeBackend();
        FakeModule module = new FakeModule();
        PhoneGnssFixSource src = source(backend, module);

        backend.fix = fullFix();
        src._tick_safe();
        assertEquals(1, module.sendCount);
        assertEquals(Messages.TYPE_AndruavModule_GNSS_Fix, module.lastType);
        assertTrue("9103 must ride intermodule routing", module.lastInternal);
        assertEquals(300456789, module.lastJmsg.optInt("la"));

        // same fix still on the sensor copy -> no second send
        src._tick_safe();
        assertEquals(1, module.sendCount);

        // a newer fix goes out on the next tick
        backend.fix = new GnssFixSample(
                PhoneGnssFixSource.GPS_PROVIDER_NAME,
                30.0000001, -97.0, true, 100.0, 3, 1, 7,
                0.9f, 1.3f, 1.5, 2.5, 0.3,
                true, 1.0, true, 90.0, true, 9000,
                1700000000000L, 6000L);
        src._tick_safe();
        assertEquals(2, module.sendCount);
        assertEquals(300000001, module.lastJmsg.optInt("la"));
    }

    @Test
    public void tickSendsNothingWithoutUsableFix() {
        FakeBackend backend = new FakeBackend();
        FakeModule module = new FakeModule();
        PhoneGnssFixSource src = source(backend, module);

        backend.fix = null;            // sensor holds no fix
        src._tick_safe();
        assertEquals(0, module.sendCount);

        // network-provider fix is rejected before the bus
        backend.fix = new GnssFixSample(
                "network", 30.0, -97.0, true, 100.0, 3, 1, 7,
                0.9f, 1.3f, 1.5, 2.5, 0.3,
                true, 1.0, true, 90.0, true, 9000,
                1700000000000L, 6000L);
        src._tick_safe();
        assertEquals(0, module.sendCount);

        // a stale fix: rejected too
        backend.fix = fullFix();
        backend.nowMs = 5000 + 3000;   // fix is 3 s old
        src._tick_safe();
        assertEquals(0, module.sendCount);
    }
}
