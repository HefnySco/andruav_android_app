package ap.andruav_ap.denative.position;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.andruav.sensors.AndruavIMU;

import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.sensors.Sensor_GPS;
import de.databus.CModule;

import static de.databus.Messages.TYPE_AndruavModule_GNSS_Fix;

/**
 * Phone-GNSS injection feed for the FC (AN-0167). Each new GPS_PROVIDER
 * fix goes out once, as a module 9103
 * ({@link de.databus.Messages#TYPE_AndruavModule_GNSS_Fix}), and
 * de_mavlink turns it into MAVLink GPS_INPUT at 10 Hz - the same path a
 * Pi receiver uses through the serial GNSS source.
 *
 * The fix itself comes from {@link Sensor_GPS#getLastGnssFix()}: that
 * copy exists only while gps_inject is on and holds only GPS_PROVIDER
 * reports - never the network/Wi-Fi-mixed location the rest of the app
 * uses, whose jumps and coarse accuracy fail EKF3's GPS checks. The
 * sender double-checks the provider anyway: the wire contract must hold
 * even if the sensor's copy policy ever changes.
 *
 * Field mapping follows the retired DroneKit sender
 * (ControlBoard_DroneKit.injectLatestGnssFix, documented in
 * wiki/GPS-Injection.md "Field mapping"), adapted to the 9103 wire
 * format parsed by de_mavlink's parseGnssFixJson: an absent value is
 * simply left out of the JSON so de_mavlink sets the matching
 * GPS_INPUT_IGNORE_FLAG instead of the EKF reading a 0 as a perfect
 * measurement.
 *
 * Sends nothing while gps_inject is off, while "Ignore Mobile Sensors"
 * is on, or when the newest fix is older than {@link #MAX_FIX_AGE_MS} -
 * a phone that lost its fix must show up on the FC as a lost GPS, not a
 * position frozen in place.
 *
 * Lifecycle mirrors {@link PhonePositionSource}: construct at module
 * setup, {@link #start()} once the module socket is up, {@link #stop()}
 * on teardown. Both idempotent.
 */
public class PhoneGnssFixSource {

    private static final String TAG = "PhoneGnssFixSource";

    /** Emit period: one send per new fix; de_mavlink repeats at 10 Hz. */
    private static final long EMIT_INTERVAL_MS = 1000;

    /** A fix older than this is stale - send nothing, let the FC time out. */
    private static final long MAX_FIX_AGE_MS = 2000;

    /** android.location.LocationManager.GPS_PROVIDER, spelled out so the
     *  pure-Java layer never touches an Android class. */
    public static final String GPS_PROVIDER_NAME = "gps";

    // GPS time = UTC + leap seconds, counted from the GPS epoch
    // (1980-01-06T00:00:00Z), split into whole weeks + milliseconds into
    // the week. Same constants the Java sender and de_mavlink both use.
    private static final long GPS_EPOCH_UNIX_MILLIS = 315964800000L;
    private static final long GPS_LEAPSECONDS_MILLIS = 18000L;
    private static final long AP_MSEC_PER_WEEK = 7L * 86400L * 1000L;

    /**
     * Pure-Java snapshot of one GNSS fix plus the app state the 9103
     * payload needs. The Android backend fills it; the field builder is
     * a pure function of it, so JUnit covers the whole wire mapping.
     */
    public static final class GnssFixSample {
        /** Location.getProvider() verbatim - the send gate wants GPS_PROVIDER_NAME. */
        public final String provider;
        public final double latitude_deg;
        public final double longitude_deg;
        /** False = the platform gave no altitude; the fix is then unsendable
         *  (9103 "a" is required - see parseGnssFixJson). */
        public final boolean has_altitude;
        /** Metres above mean sea level (API 34 value, else ellipsoid minus
         *  the GGA geoid separation). */
        public final double altitude_msl_m;
        /** Best NMEA GSA fix mode of the epoch (0-3). */
        public final int gsa_fix_mode;
        /** NMEA GGA fix quality (0-5; 4 = RTK fixed, 5 = RTK float). */
        public final int gga_quality;
        /** Satellites used in fix (GnssStatus.usedInFix), not in view. */
        public final int sats_used_in_fix;
        /** NMEA dilutions; <= 0 means the receiver did not report them. */
        public final float hdop;
        public final float vdop;
        /** NaN = the phone did not report this accuracy. */
        public final double horiz_accuracy_m;
        public final double vert_accuracy_m;
        public final double speed_accuracy_mps;
        public final boolean has_speed;
        public final double speed_mps;
        public final boolean has_bearing;
        public final double bearing_deg;
        /** Compass heading already converted to true-north centidegrees
         *  (GeomagneticField declination applied), 1..36000. The heading
         *  preference decides at build time whether "yw" goes on the wire. */
        public final boolean has_yaw;
        public final int yaw_cdeg;
        /** Wall-clock UTC of the fix (Location.getTime()). */
        public final long fix_utc_ms;
        /** Monotonic ms on the same clock as the backend's nowElapsedMs -
         *  the dedupe and fix-age key. */
        public final long elapsed_realtime_ms;

        public GnssFixSample(String provider,
                             double latitude_deg, double longitude_deg,
                             boolean has_altitude, double altitude_msl_m,
                             int gsa_fix_mode, int gga_quality,
                             int sats_used_in_fix,
                             float hdop, float vdop,
                             double horiz_accuracy_m, double vert_accuracy_m,
                             double speed_accuracy_mps,
                             boolean has_speed, double speed_mps,
                             boolean has_bearing, double bearing_deg,
                             boolean has_yaw, int yaw_cdeg,
                             long fix_utc_ms, long elapsed_realtime_ms) {
            this.provider = provider;
            this.latitude_deg = latitude_deg;
            this.longitude_deg = longitude_deg;
            this.has_altitude = has_altitude;
            this.altitude_msl_m = altitude_msl_m;
            this.gsa_fix_mode = gsa_fix_mode;
            this.gga_quality = gga_quality;
            this.sats_used_in_fix = sats_used_in_fix;
            this.hdop = hdop;
            this.vdop = vdop;
            this.horiz_accuracy_m = horiz_accuracy_m;
            this.vert_accuracy_m = vert_accuracy_m;
            this.speed_accuracy_mps = speed_accuracy_mps;
            this.has_speed = has_speed;
            this.speed_mps = speed_mps;
            this.has_bearing = has_bearing;
            this.bearing_deg = bearing_deg;
            this.has_yaw = has_yaw;
            this.yaw_cdeg = yaw_cdeg;
            this.fix_utc_ms = fix_utc_ms;
            this.elapsed_realtime_ms = elapsed_realtime_ms;
        }
    }

    /**
     * Platform seam (same pattern as {@link PhonePositionSource}'s
     * LocationBackend): every Android call lives behind this interface so
     * the emit/dedupe/send path runs in a plain JVM unit test.
     * {@link SensorGnssBackend} is the production implementation.
     */
    interface GnssBackend {
        /** Newest GPS_PROVIDER fix the sensor kept for injection, or null. */
        GnssFixSample pollLatestFix();

        /** Monotonic now in ms on the SAME clock as
         *  {@link GnssFixSample#elapsed_realtime_ms}. */
        long nowElapsedMs();
    }

    /** The three app switches that gate the sender, sampled per tick so a
     *  mid-flight toggle takes effect on the next fix. */
    public static final class Switches {
        /** gps_inject master switch (PHONE > GPS). */
        public final boolean injection_on;
        /** "Ignore Mobile Sensors" - when on the phone contributes nothing. */
        public final boolean mobile_sensors_disabled;
        /** gps_inject_heading - compass yaw onto GPS_INPUT.yaw. */
        public final boolean heading_injection_on;

        public Switches(boolean injection_on, boolean mobile_sensors_disabled,
                        boolean heading_injection_on) {
            this.injection_on = injection_on;
            this.mobile_sensors_disabled = mobile_sensors_disabled;
            this.heading_injection_on = heading_injection_on;
        }
    }

    /** Supplies the switch snapshot per tick; tests inject constants. */
    interface SwitchesSource {
        Switches current();
    }

    private final GnssBackend m_backend;
    private final CModule m_module;
    private final SwitchesSource m_switches_source;

    private ScheduledExecutorService m_emitter;
    /** Monotonic stamp of the last fix that went on the wire - the
     *  "send each new fix one time" dedupe key. */
    private long m_last_sent_elapsed_ms = -1;

    public PhoneGnssFixSource(Context context, CModule module) {
        this(new SensorGnssBackend(), module, () -> new Switches(
                Preference.isGPSInjecttionEnabled(null),
                Preference.isMobileSensorsDisabled(null),
                Preference.isGPSHeadingInjectionEnabled(null)));
    }

    /** Test seam: production callers use the Context ctor above. */
    PhoneGnssFixSource(GnssBackend backend, CModule module,
                       SwitchesSource switches_source) {
        this.m_backend = backend;
        this.m_module = module;
        this.m_switches_source = switches_source;
    }

    public synchronized void start() {
        if (m_emitter == null) {
            m_emitter = Executors.newSingleThreadScheduledExecutor();
            m_emitter.scheduleWithFixedDelay(this::_tick_safe,
                    EMIT_INTERVAL_MS, EMIT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void stop() {
        if (m_emitter != null) {
            m_emitter.shutdownNow();
            m_emitter = null;
        }
        m_last_sent_elapsed_ms = -1;
    }

    // ------------------------------------------------------------ emit

    /** Package-private so the unit test drives ticks without a scheduler. */
    void _tick_safe() {
        try {
            _tick();
        } catch (Throwable t) {
            Log.w(TAG, "emit tick failed", t);
        }
    }

    private void _tick() {
        final GnssFixSample fix = m_backend.pollLatestFix();
        if (fix == null) return;
        // One send per new fix: the sensor copy is overwritten in place,
        // so identity is its monotonic stamp, not the object.
        if (fix.elapsed_realtime_ms <= m_last_sent_elapsed_ms) return;
        final long fix_age_ms = m_backend.nowElapsedMs() - fix.elapsed_realtime_ms;
        final Map<String, Object> fields =
                buildGnssFixFields(fix, m_switches_source.current(), fix_age_ms);
        if (fields == null) return;
        m_last_sent_elapsed_ms = fix.elapsed_realtime_ms;
        // internal_message=true: 9103 is intermodule-only - de_mavlink
        // drops it unless it arrives on the module bus routing.
        m_module.sendJMSG("", new JSONObject(fields),
                TYPE_AndruavModule_GNSS_Fix, true);
    }

    // ------------------------------------------------- wire mapping

    /**
     * The 9103 "ms" fields for one fix - or null when the fix must not go
     * out at all. Field-for-field the contract de_mavlink's
     * parseGnssFixJson implements:
     *  - required: la/ln (degE7), a (AMSL mm), fix, ns;
     *  - optional: hd/vd (dop x100), ha/va (mm), vn/ve (mm/s, sent or
     *    ignored as a pair), sa (mm/s), yw (cdeg), tw/tms (GPS time),
     *    src. An absent optional key sets the matching
     *    GPS_INPUT_IGNORE_FLAG - never sent as 0.
     * vdn is never sent: Android reports no vertical speed.
     */
    public static Map<String, Object> buildGnssFixFields(GnssFixSample fix,
                                                         Switches switches,
                                                         long fix_age_ms) {
        if (fix == null) return null;
        if (!switches.injection_on || switches.mobile_sensors_disabled) {
            return null;
        }
        if (!GPS_PROVIDER_NAME.equals(fix.provider)) return null;
        // "a" is required on the wire - a fix the platform gave without
        // altitude cannot be represented, so it does not go out.
        if (!fix.has_altitude) return null;
        if (fix_age_ms < 0 || fix_age_ms > MAX_FIX_AGE_MS) return null;

        final Map<String, Object> ms = new LinkedHashMap<>();
        ms.put("la", (int) Math.round(fix.latitude_deg * 1e7));
        ms.put("ln", (int) Math.round(fix.longitude_deg * 1e7));
        ms.put("a", (int) Math.round(fix.altitude_msl_m * 1000.0));
        // fix type: the best GSA mode of the epoch, lifted to the GGA
        // quality when it reports RTK (4 fixed / 5 float). Same rule the
        // DroneKit sender used.
        ms.put("fix", (fix.gga_quality > 3) ? fix.gga_quality : fix.gsa_fix_mode);
        ms.put("ns", fix.sats_used_in_fix);

        if (fix.hdop > 0.0f) ms.put("hd", Math.round(fix.hdop * 100.0f));
        if (fix.vdop > 0.0f) ms.put("vd", Math.round(fix.vdop * 100.0f));
        if (!Double.isNaN(fix.horiz_accuracy_m)) {
            ms.put("ha", Math.round(fix.horiz_accuracy_m * 1000.0));
        }
        if (!Double.isNaN(fix.vert_accuracy_m)) {
            ms.put("va", Math.round(fix.vert_accuracy_m * 1000.0));
        }
        if (!Double.isNaN(fix.speed_accuracy_mps)) {
            ms.put("sa", Math.round(fix.speed_accuracy_mps * 1000.0));
        }

        // North/east velocity from the provider's own speed and bearing,
        // in mm/s. AP_GPS_MAV keeps the last velocity it was given when a
        // packet ignores it, so a stationary fix with no bearing (common
        // on phones) still sends an explicit zero - otherwise the
        // velocity from the last time the vehicle moved stays latched on
        // the FC.
        if (fix.has_speed) {
            if (fix.has_bearing) {
                final double bearing_rad = Math.toRadians(fix.bearing_deg);
                ms.put("vn", Math.round(fix.speed_mps
                        * Math.cos(bearing_rad) * 1000.0));
                ms.put("ve", Math.round(fix.speed_mps
                        * Math.sin(bearing_rad) * 1000.0));
            } else if (fix.speed_mps == 0.0) {
                ms.put("vn", 0);
                ms.put("ve", 0);
            }
        }

        if (switches.heading_injection_on && fix.has_yaw) {
            ms.put("yw", fix.yaw_cdeg);
        }

        // GPS time: the fix's own UTC stamp advanced by its age, so the
        // stamp tracks the sample even when the emit tick ran late.
        // de_mavlink advances it further for its own 10 Hz re-sends.
        final long sample_time_ms = fix.fix_utc_ms + Math.max(0L, fix_age_ms);
        final long gps_ms = sample_time_ms - GPS_EPOCH_UNIX_MILLIS
                + GPS_LEAPSECONDS_MILLIS;
        ms.put("tw", (int) (gps_ms / AP_MSEC_PER_WEEK));
        ms.put("tms", gps_ms % AP_MSEC_PER_WEEK);

        ms.put("src", "phone");
        return ms;
    }

    // ------------------------------------------------- android backend

    /**
     * Production {@link GnssBackend}: reads the GPS_PROVIDER copy
     * {@link Sensor_GPS} keeps while injection is on, plus the NMEA state
     * SensorService already mirrors into the unit's mobile IMU
     * (GSA fix mode, GGA quality, DOPs, magnetic heading).
     */
    private static final class SensorGnssBackend implements GnssBackend {

        @Override
        public GnssFixSample pollLatestFix() {
            final Location fix = Sensor_GPS.getLastGnssFix();
            if (fix == null) return null;

            final AndruavIMU imu =
                    (com.andruav.AndruavSettings.andruavWe7daBase != null)
                            ? com.andruav.AndruavSettings.andruavWe7daBase
                                    .getMobileGPS()
                            : null;

            // Compass yaw, converted to GPS_INPUT.yaw's wire format here
            // where android.hardware.GeomagneticField is available; the
            // heading preference only gates whether it goes on the wire.
            boolean has_yaw = false;
            int yaw_cdeg = 0;
            if ((imu != null) && Boolean.TRUE.equals(imu.iM)) {
                yaw_cdeg = _yawCentidegrees(imu.Y, fix);
                has_yaw = true;
            }

            return new GnssFixSample(
                    fix.getProvider(),
                    fix.getLatitude(),
                    fix.getLongitude(),
                    fix.hasAltitude(),
                    fix.hasAltitude() ? _altitudeAboveSeaLevel(fix) : 0.0,
                    (imu != null) ? imu.GPS3DFix : 0,
                    (imu != null) ? imu.GPSFixQuality : 0,
                    Sensor_GPS.SatUsedInFixCount,
                    (imu != null) ? imu.Hdop : 0.0f,
                    (imu != null) ? imu.Vdop : 0.0f,
                    fix.hasAccuracy() ? fix.getAccuracy() : Double.NaN,
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            && fix.hasVerticalAccuracy())
                            ? fix.getVerticalAccuracyMeters() : Double.NaN,
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            && fix.hasSpeedAccuracy())
                            ? fix.getSpeedAccuracyMetersPerSecond() : Double.NaN,
                    fix.hasSpeed(),
                    fix.hasSpeed() ? fix.getSpeed() : 0.0,
                    fix.hasBearing(),
                    fix.hasBearing() ? fix.getBearing() : 0.0,
                    has_yaw, yaw_cdeg,
                    fix.getTime(),
                    fix.getElapsedRealtimeNanos() / 1000000L);
        }

        @Override
        public long nowElapsedMs() {
            return SystemClock.elapsedRealtimeNanos() / 1000000L;
        }

        /**
         * ArduPilot reads GPS_INPUT.alt as height above mean sea level,
         * but Location.getAltitude() reports height above the WGS84
         * ellipsoid - the two differ by the local geoid separation (tens
         * of metres in parts of the world). Prefer the platform's own
         * MSL value on API 34+, else subtract the separation the GGA
         * sentence carries; before the first GGA that term is 0, which
         * just leaves the raw ellipsoidal height as the old sender did.
         */
        private static double _altitudeAboveSeaLevel(final Location location) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    && location.hasMslAltitude()) {
                return location.getMslAltitudeMeters();
            }
            return location.getAltitude() - Sensor_GPS.GeoidSeparation;
        }

        /**
         * The phone's magnetic compass heading (radians, clockwise from
         * north) as GPS_INPUT.yaw centidegrees from TRUE north: 0 is
         * reserved for "not available" on the wire, so true north is
         * 36000. GeomagneticField adds the local declination -
         * CompassCalculation hardcodes it to 0, and an uncorrected
         * magnetic heading would be a constant bias on the FC's EKF.
         */
        private static int _yawCentidegrees(final double magnetic_heading_rad,
                                            final Location location) {
            double true_heading_deg = Math.toDegrees(magnetic_heading_rad);
            try {
                final android.hardware.GeomagneticField field =
                        new android.hardware.GeomagneticField(
                                (float) location.getLatitude(),
                                (float) location.getLongitude(),
                                (float) location.getAltitude(),
                                location.getTime());
                true_heading_deg += field.getDeclination();
            } catch (final Exception ex) {
                // Bad lat/lng/alt for the model - fall back to the
                // uncorrected magnetic heading rather than no heading.
            }
            true_heading_deg =
                    ((true_heading_deg % 360.0) + 360.0) % 360.0;
            final int centideg = (int) Math.round(true_heading_deg * 100.0);
            return (centideg <= 0) ? 36000 : centideg;
        }
    }
}
