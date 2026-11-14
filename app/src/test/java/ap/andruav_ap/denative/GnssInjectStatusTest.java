package ap.andruav_ap.denative;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Field mapping for the 6549 status (AN-0169). The wire contract is
 * de_mavlink's CGnssInjectManager::sendStatus: short field names
 * (src/fix/ns/fc_gps/gid/inj/fin/fout/age/err/ntrip/ntrip_err/ntrip_bps),
 * optional fields absent rather than empty.
 */
public class GnssInjectStatusTest {

    private static GnssInjectStatus freshStatus() {
        final GnssInjectStatus status = new GnssInjectStatus();
        final long[] now = {0};
        status.clock = () -> now[0];
        return status;
    }

    private static GnssInjectStatus freshStatus(final long[] now) {
        final GnssInjectStatus status = new GnssInjectStatus();
        status.clock = () -> now[0];
        return status;
    }

    @Test
    public void update_mapsWireFields() throws Exception {
        final GnssInjectStatus status = freshStatus();
        status.update(new JSONObject()
                .put("src", "bus")
                .put("fix", 3)
                .put("ns", 9)
                .put("fc_gps", 14)
                .put("gid", 0)
                .put("inj", "active")
                .put("fin", 42)
                .put("fout", 400)
                .put("age", 120)
                .put("ntrip", "connected")
                .put("ntrip_bps", 341));

        assertEquals("bus", status.source);
        assertEquals(3, status.fixType);
        assertEquals(9, status.satellites);
        assertEquals(14, status.fcGpsType);
        assertEquals(0, status.gpsId);
        assertEquals("active", status.injectState);
        assertEquals(42, status.fixesIn);
        assertEquals(400, status.framesOut);
        assertEquals(120, status.lastFixAgeMs);
        assertNull(status.error);
        assertEquals("connected", status.ntripState);
        assertEquals(341, status.ntripBps);
        assertTrue(status.reported);
    }

    @Test
    public void update_missingOptionals_defaults() throws Exception {
        final GnssInjectStatus status = freshStatus();
        status.update(new JSONObject()
                .put("src", "none")
                .put("inj", "idle")
                .put("fin", 0)
                .put("fout", 0)
                .put("age", 0)
                .put("ntrip", "off"));

        assertEquals(-1, status.fixType);
        assertEquals(-1, status.satellites);
        assertEquals(-1, status.fcGpsType);
        assertEquals(-1, status.gpsId);
        assertEquals(0, status.lastFixAgeMs);
        assertNull(status.error);
        assertNull(status.ntripError);
        assertEquals(-1, status.ntripBps);
    }

    @Test
    public void update_errorFields_clearedWhenAbsent() throws Exception {
        final long[] now = {0};
        final GnssInjectStatus status = freshStatus(now);
        status.update(new JSONObject()
                .put("src", "bus").put("inj", "error")
                .put("fin", 1).put("fout", 1).put("age", 0)
                .put("err", "port_busy")
                .put("ntrip", "error").put("ntrip_err", "http_404"));
        assertEquals("port_busy", status.error);
        assertEquals("http_404", status.ntripError);

        status.update(new JSONObject()
                .put("src", "bus").put("inj", "active")
                .put("fin", 2).put("fout", 11).put("age", 0)
                .put("ntrip", "connected").put("ntrip_bps", 10));
        assertNull(status.error);
        assertNull(status.ntripError);
    }

    @Test
    public void framesPerSecond_fromTwoSamples() throws Exception {
        final long[] now = {0};
        final GnssInjectStatus status = freshStatus(now);
        final JSONObject base = new JSONObject()
                .put("src", "bus").put("inj", "active")
                .put("fin", 0).put("age", 0).put("ntrip", "off");

        status.update(new JSONObject(base.toString()).put("fout", 100));
        assertEquals(-1, status.framesPerSecond, 0.001);

        now[0] = 1000;
        status.update(new JSONObject(base.toString()).put("fout", 110));
        assertEquals(10.0, status.framesPerSecond, 0.001);

        now[0] = 2000;
        status.update(new JSONObject(base.toString()).put("fout", 115));
        assertEquals(5.0, status.framesPerSecond, 0.001);
    }

    @Test
    public void isFresh_agesOut() throws Exception {
        final long[] now = {0};
        final GnssInjectStatus status = freshStatus(now);
        assertFalse(status.isFresh());

        status.update(new JSONObject()
                .put("src", "bus").put("inj", "active")
                .put("fin", 0).put("fout", 0).put("age", 0).put("ntrip", "off"));
        assertTrue(status.isFresh());

        now[0] = 59_999;
        assertTrue(status.isFresh());
        now[0] = 60_000;
        assertFalse(status.isFresh());
    }
}
