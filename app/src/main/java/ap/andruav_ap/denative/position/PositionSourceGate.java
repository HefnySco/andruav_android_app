package ap.andruav_ap.denative.position;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Position-source arbitration for the andruav_phone module (B8 /
 * AN-0047): decides when the phone GPS is the unit's position source and
 * builds the TYPE_AndruavModule_Location_Info (9102) payload it emits.
 *
 * On Linux de_mavlink owns the unit position: fcb_facade.cpp
 * sendLocationInfo() emits a 9102 every 300 ms, de_comm's
 * updateUnitLocationFromModule() turns it into the unit's location_info
 * (roster "pos" / "pa" age) and fans the message back out to every
 * module subscribed to 9102 - never to the sender. This class is the
 * phone side of that arbitration: the module subscribes to 9102 and
 * reports every other module's report to {@link #onExternalLocationReport};
 * {@link #shouldSendPhonePosition} applies the spec's "switch source only
 * after 5 s without the other" so the two sources never flap.
 *
 * Field notes versus de_mavlink:
 *  - lat/lng ride degE7 ints ("la"/"ln"); altitudes are mm ("a"/"r");
 *    accuracies are the uint32 millimetre fields of GPS_RAW_INT, where
 *    UINT32_MAX means unknown - kept verbatim here.
 *  - A report whose lat AND lng are both zero carries no position
 *    (de_mavlink streams exactly that while it has never seen an FC
 *    fix). It does not count as an external position source - otherwise
 *    a headless de_mavlink would suppress the phone forever.
 *  - "y" is GPS_RAW_INT yaw in centidegrees, 0 = unknown; the phone
 *    contributes its bearing, so a fix without bearing reports 0.
 *
 * GPS modes mirror AndruavUnitBase.GPS_MODE_* (kept as local ints so the
 * class stays Android-free):
 *  - FCB:    the phone never emits - the FC is the declared source.
 *  - MOBILE: emits on any usable phone fix. Explicit override, the same
 *            semantics getActiveGPS() has on the legacy stack (returns
 *            the phone IMU unconditionally in MOBILE). Caveat: while
 *            de_mavlink is also streaming positions the two 9102s
 *            interleave on de_comm's last-write-wins store - AUTO is the
 *            arbitrated path; MOBILE exists for "I know better, use the
 *            phone".
 *  - AUTO:   emits only after the external source has been silent for
 *            {@link #SOURCE_SWITCH_QUIET_MS}. Yielding back is immediate:
 *            the first valid external report suppresses the phone again
 *            (at most one stale phone beat can overlap).
 *
 * All times are caller-supplied monotonic milliseconds so the decision
 * is a pure function - unit tests feed a fake clock / fake FC presence.
 */
public class PositionSourceGate {

    public static final int GPS_MODE_AUTO = 0;
    public static final int GPS_MODE_MOBILE = 1;
    public static final int GPS_MODE_FCB = 2;

    /** Spec hysteresis: take over only after this much silence on the bus. */
    public static final long SOURCE_SWITCH_QUIET_MS = 5000;

    /** A phone fix older than this is not a usable unit position. */
    public static final long PHONE_FIX_MAX_AGE_MS = 10000;

    /** GPS_RAW_INT uint32 "unknown" sentinel, carried verbatim on 9102. */
    public static final long ACCURACY_UNKNOWN = 4294967295L;

    /** Monotonic ms of the last external 9102 that carried a position;
     *  0 = none seen yet. */
    private long m_last_external_position_ms = 0;

    /**
     * Bus feed: a 9102 received from another module (or de_comm's own
     * declared-static fallback - a deliberate operator position that
     * must win over the phone too). Callers drop the module's own key.
     * Reports with no real coordinates (lat == 0 && lng == 0) do not
     * suppress the phone.
     */
    public synchronized void onExternalLocationReport(long nowMs,
                                                      int latDegE7,
                                                      int lngDegE7) {
        if (latDegE7 == 0 && lngDegE7 == 0) return;
        m_last_external_position_ms = nowMs;
    }

    /** True while another module's position reports are fresh. */
    public synchronized boolean isOtherPositionSourceActive(long nowMs) {
        return m_last_external_position_ms != 0
                && (nowMs - m_last_external_position_ms) < SOURCE_SWITCH_QUIET_MS;
    }

    /**
     * The emit decision for one send tick.
     *
     * @param nowMs               monotonic now (same clock as the reports)
     * @param gpsMode             GPS_MODE_AUTO / MOBILE / FCB
     * @param phoneFixAgeMs       age of the newest phone fix on the same
     *                            clock; negative = no fix at all
     */
    public synchronized boolean shouldSendPhonePosition(long nowMs, int gpsMode,
                                                        long phoneFixAgeMs) {
        if (phoneFixAgeMs < 0 || phoneFixAgeMs > PHONE_FIX_MAX_AGE_MS) {
            return false;
        }
        switch (gpsMode) {
            case GPS_MODE_FCB:
                return false;
            case GPS_MODE_MOBILE:
                return true;
            case GPS_MODE_AUTO:
            default:
                return !isOtherPositionSourceActive(nowMs);
        }
    }

    /**
     * The 9102 "ms" fields for one fix - field for field the same JSON
     * de_mavlink emits in CFCBFacade::sendLocationInfo. Returned as a
     * plain map (org.json-free) so it is covered by the same JUnit.
     */
    public static Map<String, Object> buildLocationInfoFields(PositionFix fix) {
        final Map<String, Object> ms = new LinkedHashMap<>();
        ms.put("la", (int) Math.round(fix.latitude_deg * 1e7));
        ms.put("ln", (int) Math.round(fix.longitude_deg * 1e7));
        // No home datum on a bare phone: relative altitude equals the
        // absolute estimate (the configured-static de_comm path does the
        // same when only alt_abs is configured).
        final int altitude_mm = (int) Math.round(fix.altitude_m * 1000.0);
        ms.put("a", altitude_mm);
        ms.put("r", altitude_mm);
        ms.put("ha", fix.hasHorizontalAccuracy()
                ? Math.round(fix.horizontal_accuracy_m * 1000.0)
                : ACCURACY_UNKNOWN);
        ms.put("v_acc", fix.hasVerticalAccuracy()
                ? Math.round(fix.vertical_accuracy_m * 1000.0)
                : ACCURACY_UNKNOWN);
        ms.put("vel_acc", fix.hasSpeedAccuracy()
                ? Math.round(fix.speed_accuracy_mps * 1000.0)
                : ACCURACY_UNKNOWN);
        ms.put("y", fix.hasBearing()
                ? (int) Math.min(36000, Math.max(0, Math.round(fix.bearing_deg * 100.0)))
                : 0);
        return ms;
    }
}
