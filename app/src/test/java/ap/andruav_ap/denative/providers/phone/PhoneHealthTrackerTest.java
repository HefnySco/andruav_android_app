package ap.andruav_ap.denative.providers.phone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import de.databus.CapSchema;

/**
 * Phone-health edge logic on a fake sensor feed (TASK AN-0044 / TO-BE
 * section 5.6): battery_low and overheat hysteresis, net_lost/net_back
 * transitions, the live state snapshot, and the de.cap/1 advert.
 */
public class PhoneHealthTrackerTest {

    /** Records every fact the tracker raises. */
    private static final class FactSink implements PhoneHealthTracker.Listener {
        final List<String> events = new ArrayList<>();
        final List<Map<String, Object>> payloads = new ArrayList<>();
        int state_notifications = 0;

        @Override
        public void onFact(String event, Map<String, Object> payload) {
            events.add(event);
            payloads.add(payload);
        }

        @Override
        public void onStateChanged() {
            state_notifications++;
        }

        int count(String event) {
            int n = 0;
            for (String e : events) if (e.equals(event)) n++;
            return n;
        }
    }

    private PhoneHealthTracker mTracker;
    private FactSink mSink;

    @Before
    public void setUp() {
        mTracker = new PhoneHealthTracker(PhoneHealthConfig.defaults());
        mSink = new FactSink();
        mTracker.setListener(mSink);
    }

    /** The "phone" advert passes de.cap/1 schema validation. */
    @Test
    public void advertValidates() throws Exception {
        PhoneProvider provider = new PhoneProvider(PhoneHealthConfig.defaults());
        List<Object> adverts = provider.adverts();
        assertEquals(1, adverts.size());
        JSONObject advert = new JSONObject(String.valueOf(adverts.get(0)));
        assertEquals("", CapSchema.cap_validate_advert(advert));
        assertEquals("phone", advert.optString("ns"));
    }

    /** Spec acceptance: level 15 -> one fact; 22 -> none; 30 then 15 -> one more. */
    @Test
    public void batteryLowEdgeWithHysteresis() {
        mTracker.onBattery(50, false, 25.0);
        assertEquals(0, mSink.count(PhoneHealthTracker.FACT_BATTERY_LOW));

        mTracker.onBattery(15, false, 25.0);
        assertEquals(1, mSink.count(PhoneHealthTracker.FACT_BATTERY_LOW));
        assertEquals(15, ((Number) mSink.payloads.get(0).get("v")).intValue());

        // still inside the hysteresis band: no new fact
        mTracker.onBattery(22, false, 25.0);
        assertEquals(1, mSink.count(PhoneHealthTracker.FACT_BATTERY_LOW));

        // back above the clear threshold re-arms the edge
        mTracker.onBattery(30, true, 25.0);
        assertEquals(1, mSink.count(PhoneHealthTracker.FACT_BATTERY_LOW));

        mTracker.onBattery(15, true, 25.0);
        assertEquals(2, mSink.count(PhoneHealthTracker.FACT_BATTERY_LOW));
    }

    /** overheat fires once at 45 C, re-arms below 42 C. */
    @Test
    public void overheatEdgeWithHysteresis() {
        mTracker.onBattery(80, true, 40.0);
        mTracker.onBattery(80, true, 45.0);
        assertEquals(1, mSink.count(PhoneHealthTracker.FACT_OVERHEAT));
        assertEquals(45.0, ((Number) mSink.payloads.get(0).get("temp_c")).doubleValue(), 0.001);

        // inside the band: latched, no repeat
        mTracker.onBattery(80, true, 44.0);
        assertEquals(1, mSink.count(PhoneHealthTracker.FACT_OVERHEAT));

        // cleared at 42 C, then a fresh crossing fires again
        mTracker.onBattery(80, true, 41.5);
        mTracker.onBattery(80, true, 46.0);
        assertEquals(2, mSink.count(PhoneHealthTracker.FACT_OVERHEAT));
    }

    /** Airplane-mode style transitions: connected -> none -> connected. */
    @Test
    public void netLostThenNetBack() {
        // baseline first observation raises nothing
        mTracker.onNetwork(PhoneHealthTracker.NET_WIFI, -55.0);
        assertTrue(mSink.events.isEmpty());

        mTracker.onNetwork(PhoneHealthTracker.NET_NONE, 0.0);
        assertEquals(1, mSink.count(PhoneHealthTracker.FACT_NET_LOST));

        // staying disconnected is not another edge
        mTracker.onNetwork(PhoneHealthTracker.NET_NONE, 0.0);
        assertEquals(1, mSink.count(PhoneHealthTracker.FACT_NET_LOST));

        mTracker.onNetwork(PhoneHealthTracker.NET_LTE, -80.0);
        assertEquals(1, mSink.count(PhoneHealthTracker.FACT_NET_BACK));
    }

    /** A transport change while still connected is not a net edge. */
    @Test
    public void netSwitchIsNotAnEdge() {
        mTracker.onNetwork(PhoneHealthTracker.NET_WIFI, -55.0);
        mTracker.onNetwork(PhoneHealthTracker.NET_LTE, -80.0);
        assertTrue(mSink.events.isEmpty());
    }

    /** rssi jitter inside the deadband does not churn the state feed. */
    @Test
    public void rssiDeadbandSuppressesJitter() {
        mTracker.onNetwork(PhoneHealthTracker.NET_WIFI, -55.0);
        int after_first = mSink.state_notifications;
        mTracker.onNetwork(PhoneHealthTracker.NET_WIFI, -54.5); // < 2 dBm drift
        assertEquals(after_first, mSink.state_notifications);
        mTracker.onNetwork(PhoneHealthTracker.NET_WIFI, -50.0); // beyond deadband
        assertEquals(after_first + 1, mSink.state_notifications);
    }

    /** The state snapshot carries every advertised key with live values. */
    @Test
    public void snapshotCarriesAllHealthKeys() {
        mTracker.onBattery(63, true, 31.5);
        mTracker.onNetwork(PhoneHealthTracker.NET_5G, -70.0);
        mTracker.onGps(true, PhoneHealthTracker.GPS_SRC_PHONE);

        Map<String, Object> state = mTracker.snapshot();
        assertEquals(63, ((Number) state.get("battery")).intValue());
        assertEquals(Boolean.TRUE, state.get("charging"));
        assertEquals(31.5, ((Number) state.get("temp_c")).doubleValue(), 0.001);
        assertEquals("5g", state.get("net"));
        assertEquals(-70.0, ((Number) state.get("rssi_dbm")).doubleValue(), 0.001);
        assertEquals(Boolean.TRUE, state.get("gps_ok"));
        assertEquals("phone", state.get("gps_src"));
    }

    /** state() on the provider mirrors the tracker snapshot. */
    @Test
    public void providerStateMirrorsTracker() {
        PhoneProvider provider = new PhoneProvider(PhoneHealthConfig.defaults());
        provider.tracker().onBattery(42, false, 30.0);
        provider.tracker().onNetwork(PhoneHealthTracker.NET_WIFI, -60.0);
        provider.tracker().onGps(false, PhoneHealthTracker.GPS_SRC_NONE);

        JSONObject state = provider.state(PhoneProvider.NS);
        assertEquals(42, state.optInt("battery"));
        assertFalse(state.optBoolean("charging"));
        assertEquals("wifi", state.optString("net"));
        assertEquals("none", state.optString("gps_src"));
    }

    /** Unknown actions are rejected (B4 owns the phone output actions). */
    @Test
    public void invokeRejectsUnknownAction() {
        PhoneProvider provider = new PhoneProvider(PhoneHealthConfig.defaults());
        final String[] error = {null};
        provider.invoke("i1", PhoneProvider.NS, "bogus", new JSONObject(),
                System.currentTimeMillis() + 1000,
                new de.databus.caps.DeCapabilityProvider.ResultSink() {
                    @Override
                    public void ok(JSONObject data) {
                    }

                    @Override
                    public void err(String err) {
                        error[0] = err;
                    }
                });
        assertEquals("unknown action phone.bogus", error[0]);
    }
}
