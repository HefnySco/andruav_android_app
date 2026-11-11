package ap.andruav_ap.denative.position;

import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import de.databus.CModule;

import static de.databus.Messages.TYPE_AndruavModule_Location_Info;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * AN-0173: the listener must retry after start() when the location
 * permission arrives late. The backend seam is faked - no Android
 * runtime needed. The android.jar stubs (Log, SystemClock) throw
 * "not mocked" on the JVM; the helpers below absorb that because the
 * calls are cosmetic on the tested paths - the assertions read the
 * backend/module fakes, never the stubs.
 */
public class PhonePositionSourceRetryTest {

    private static final long TWO_DAYS_MS = 2L * 24 * 3600 * 1000;

    private static final class FakeBackend
            implements PhonePositionSource.LocationBackend {
        boolean granted = false;
        int register_calls = 0;
        int unregister_calls = 0;
        PositionFix seed = null;
        FixSink sink = null;
        /** Fake elapsedRealtime clock (ms). Starts far from
         *  System.nanoTime on purpose - a phone that deep-slept for days
         *  has boottime >> monotonic, and the source must not care. */
        long now_ms = (System.nanoTime() / 1000000L) + TWO_DAYS_MS;

        @Override
        public long nowElapsedMs() {
            return now_ms;
        }

        @Override
        public boolean hasFineLocationPermission() {
            return granted;
        }

        @Override
        public PositionFix bestLastKnownFix() {
            return seed;
        }

        @Override
        public boolean register(FixSink s) {
            register_calls++;
            sink = s;
            return true;
        }

        @Override
        public void unregister() {
            unregister_calls++;
        }
    }

    private static final class FakeModule extends CModule {
        final List<JSONObject> sent = new ArrayList<>();

        @Override
        public void sendJMSG(String targetPartyID, JSONObject jmsg,
                             int andruav_message_id, boolean internal_message) {
            assertEquals(TYPE_AndruavModule_Location_Info, andruav_message_id);
            sent.add(jmsg);
        }
    }

    /** Calls that can touch an android.jar stub (Log) - the stub throw is
     *  expected on the JVM and does not change the faked state. */
    private static void quiet(Runnable r) {
        try {
            r.run();
        } catch (Throwable ignored) {
        }
    }

    /** A fix stamped on the backend's elapsedRealtime clock, ageMs old. */
    private static PositionFix fixAt(FakeBackend backend, long ageMs) {
        return new PositionFix(30.0, 31.0, 120.0, 4.0, 8.0, 0.5, 90.0,
                backend.now_ms - ageMs);
    }

    private static PhonePositionSource source(FakeBackend backend,
                                              FakeModule module) {
        return new PhonePositionSource(backend, module, "test_key",
                () -> PositionSourceGate.GPS_MODE_MOBILE);
    }

    @Test
    public void listenerRetriesWhenPermissionArrivesLate() {
        final FakeBackend backend = new FakeBackend();
        final FakeModule module = new FakeModule();
        final PhonePositionSource src = source(backend, module);

        quiet(src::start);
        assertEquals(0, backend.register_calls);      // refused at start

        quiet(src::_tick_safe);
        quiet(src::_tick_safe);
        assertEquals(0, backend.register_calls);      // still refused

        backend.granted = true;                        // user grants it now
        quiet(src::_tick_safe);
        assertEquals(1, backend.register_calls);      // retry registers

        backend.sink.onFix(fixAt(backend, 0));
        quiet(src::_tick_safe);
        assertEquals(1, module.sent.size());          // 9102 emitted

        quiet(src::_tick_safe);
        assertEquals(1, backend.register_calls);      // no re-register
        quiet(src::stop);
    }

    @Test
    public void stopThenStartRegistersAgainOnce() {
        final FakeBackend backend = new FakeBackend();
        final FakeModule module = new FakeModule();
        backend.granted = true;
        final PhonePositionSource src = source(backend, module);

        quiet(src::start);
        assertEquals(1, backend.register_calls);
        quiet(src::stop);
        assertEquals(1, backend.unregister_calls);

        quiet(src::start);
        assertEquals(2, backend.register_calls);      // re-registered once
        quiet(src::_tick_safe);
        assertEquals(2, backend.register_calls);      // retry does not double
        quiet(src::stop);
    }

    @Test
    public void seedFixEmitsWithoutWaitingForListener() {
        final FakeBackend backend = new FakeBackend();
        final FakeModule module = new FakeModule();
        backend.granted = true;
        backend.seed = fixAt(backend, 0);
        final PhonePositionSource src = source(backend, module);

        quiet(src::start);
        quiet(src::_tick_safe);
        assertTrue(module.sent.size() > 0);           // seed fix emitted
        quiet(src::stop);
    }

    /** AN-0173 regression: the age is taken on the fix clock, never on
     *  System.nanoTime. Boottime two days ahead of monotonic (deep sleep)
     *  must still emit a fresh fix and still drop a stale one. */
    @Test
    public void fixAgeUsesTheFixClockNotNanoTime() {
        final FakeBackend backend = new FakeBackend();
        final FakeModule module = new FakeModule();
        backend.granted = true;
        final PhonePositionSource src = source(backend, module);
        quiet(src::start);

        backend.sink.onFix(fixAt(backend, 1000));      // 1 s old
        quiet(src::_tick_safe);
        assertEquals(1, module.sent.size());

        backend.now_ms += PositionSourceGate.PHONE_FIX_MAX_AGE_MS + 1;
        quiet(src::_tick_safe);
        assertEquals(1, module.sent.size());          // stale: no emit
        quiet(src::stop);
    }
}
