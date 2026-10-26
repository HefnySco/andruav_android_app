package ap.andruav_ap.denative.position;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Gating logic for the phone-GPS 9102 sender (B8 / AN-0047) with a fake
 * FC-presence feed: external "position reports" are pushed in directly,
 * no bus needed.
 */
public class PositionSourceGateTest {

    private static final int LAT_E7 = 300000000;   // 30.0 deg
    private static final int LNG_E7 = 310000000;   // 31.0 deg

    private static PositionFix fix(long elapsed_ms) {
        return new PositionFix(30.0, 31.0, 120.0, 4.0, 8.0, 0.5, 90.0, elapsed_ms);
    }

    @Test
    public void autoEmitsWhenNoExternalSource() {
        PositionSourceGate gate = new PositionSourceGate();
        assertTrue(gate.shouldSendPhonePosition(10000,
                PositionSourceGate.GPS_MODE_AUTO, 500));
    }

    @Test
    public void autoSuppressedWhileExternalSourceIsFresh() {
        PositionSourceGate gate = new PositionSourceGate();
        gate.onExternalLocationReport(1000, LAT_E7, LNG_E7);
        assertTrue(gate.isOtherPositionSourceActive(1000));
        assertFalse(gate.shouldSendPhonePosition(1000,
                PositionSourceGate.GPS_MODE_AUTO, 500));
        // still inside the 5 s hysteresis window
        assertFalse(gate.shouldSendPhonePosition(5999,
                PositionSourceGate.GPS_MODE_AUTO, 500));
    }

    @Test
    public void autoTakesOverAfterFiveSecondsSilence() {
        PositionSourceGate gate = new PositionSourceGate();
        gate.onExternalLocationReport(1000, LAT_E7, LNG_E7);
        // exactly at the boundary the other source is no longer active
        assertFalse(gate.isOtherPositionSourceActive(6000));
        assertTrue(gate.shouldSendPhonePosition(6000,
                PositionSourceGate.GPS_MODE_AUTO, 500));
    }

    @Test
    public void autoYieldsImmediatelyWhenExternalReturns() {
        PositionSourceGate gate = new PositionSourceGate();
        // external source silent -> phone owns the position
        gate.onExternalLocationReport(1000, LAT_E7, LNG_E7);
        assertTrue(gate.shouldSendPhonePosition(7000,
                PositionSourceGate.GPS_MODE_AUTO, 500));
        // a fresh external report suppresses the phone right away
        gate.onExternalLocationReport(7100, LAT_E7, LNG_E7);
        assertFalse(gate.shouldSendPhonePosition(7101,
                PositionSourceGate.GPS_MODE_AUTO, 500));
    }

    @Test
    public void zeroCoordinateReportDoesNotSuppress() {
        PositionSourceGate gate = new PositionSourceGate();
        // de_mavlink streams all-zero 9102s while it has no FC fix - that
        // is not a position source and must not block the phone.
        gate.onExternalLocationReport(1000, 0, 0);
        assertFalse(gate.isOtherPositionSourceActive(1500));
        assertTrue(gate.shouldSendPhonePosition(1500,
                PositionSourceGate.GPS_MODE_AUTO, 500));
    }

    @Test
    public void zeroInOneAxisStillSuppresses() {
        PositionSourceGate gate = new PositionSourceGate();
        // lat==0 or lng==0 alone is still a real fix (equator / Greenwich
        // meridian) - only both-zero is the never-fixed sentinel.
        gate.onExternalLocationReport(1000, LAT_E7, 0);
        assertTrue(gate.isOtherPositionSourceActive(1500));
        gate.onExternalLocationReport(1000, 0, LNG_E7);
        assertTrue(gate.isOtherPositionSourceActive(1500));
    }

    @Test
    public void fcbModeNeverEmits() {
        PositionSourceGate gate = new PositionSourceGate();
        assertFalse(gate.shouldSendPhonePosition(10000,
                PositionSourceGate.GPS_MODE_FCB, 500));
        assertFalse(gate.shouldSendPhonePosition(10000,
                PositionSourceGate.GPS_MODE_FCB, 0));
    }

    @Test
    public void mobileModeOverridesExternalSource() {
        PositionSourceGate gate = new PositionSourceGate();
        gate.onExternalLocationReport(1000, LAT_E7, LNG_E7);
        // MOBILE is the explicit "use the phone" override - same as
        // getActiveGPS() returning the phone IMU unconditionally.
        assertTrue(gate.shouldSendPhonePosition(1500,
                PositionSourceGate.GPS_MODE_MOBILE, 500));
    }

    @Test
    public void staleOrMissingPhoneFixNeverEmits() {
        PositionSourceGate gate = new PositionSourceGate();
        // no fix at all
        assertFalse(gate.shouldSendPhonePosition(10000,
                PositionSourceGate.GPS_MODE_AUTO, -1));
        assertFalse(gate.shouldSendPhonePosition(10000,
                PositionSourceGate.GPS_MODE_MOBILE, -1));
        // fix too old
        assertFalse(gate.shouldSendPhonePosition(20000,
                PositionSourceGate.GPS_MODE_AUTO,
                PositionSourceGate.PHONE_FIX_MAX_AGE_MS + 1));
        // boundary: exactly MAX_AGE is still usable
        assertTrue(gate.shouldSendPhonePosition(20000,
                PositionSourceGate.GPS_MODE_AUTO,
                PositionSourceGate.PHONE_FIX_MAX_AGE_MS));
    }

    @Test
    public void buildLocationInfoMatchesDeMavlinkFields() {
        PositionFix f = new PositionFix(30.1234567, 31.7654321, 123.456,
                4.2, 7.8, 0.35, 91.5, 12345);
        Map<String, Object> ms = PositionSourceGate.buildLocationInfoFields(f);
        assertEquals(301234567, ((Number) ms.get("la")).intValue());
        assertEquals(317654321, ((Number) ms.get("ln")).intValue());
        assertEquals(123456, ((Number) ms.get("a")).intValue());
        assertEquals(123456, ((Number) ms.get("r")).intValue());
        assertEquals(4200, ((Number) ms.get("ha")).longValue());
        assertEquals(7800, ((Number) ms.get("v_acc")).longValue());
        assertEquals(350, ((Number) ms.get("vel_acc")).longValue());
        assertEquals(9150, ((Number) ms.get("y")).intValue());
    }

    @Test
    public void buildLocationInfoUnknownsUseUint32Max() {
        PositionFix f = new PositionFix(30.0, 31.0, 120.0,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN, 1);
        Map<String, Object> ms = PositionSourceGate.buildLocationInfoFields(f);
        assertEquals(PositionSourceGate.ACCURACY_UNKNOWN,
                ((Number) ms.get("ha")).longValue());
        assertEquals(PositionSourceGate.ACCURACY_UNKNOWN,
                ((Number) ms.get("v_acc")).longValue());
        assertEquals(PositionSourceGate.ACCURACY_UNKNOWN,
                ((Number) ms.get("vel_acc")).longValue());
        // yaw 0 = unknown on GPS_RAW_INT
        assertEquals(0, ((Number) ms.get("y")).intValue());
    }
}
