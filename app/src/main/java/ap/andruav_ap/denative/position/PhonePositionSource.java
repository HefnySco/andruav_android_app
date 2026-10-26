package ap.andruav_ap.denative.position;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.databus.CModule;

import static de.databus.Messages.ANDRUAV_PROTOCOL_MESSAGE_CMD;
import static de.databus.Messages.ANDRUAV_PROTOCOL_MESSAGE_TYPE;
import static de.databus.Messages.INTERMODULE_MODULE_KEY;
import static de.databus.Messages.TYPE_AndruavModule_Location_Info;

/**
 * Phone-GPS unit position feed for the andruav_phone module (B8 /
 * AN-0047). Runs a LocationManager feed (GPS + network providers, the
 * same registration {@link ap.andruavmiddlelibrary.sensors.Sensor_GPS}
 * uses) and a fixed 1 Hz emitter that asks {@link PositionSourceGate}
 * each tick whether the phone currently owns the unit position; when it
 * does, the newest fix goes out as a module 9102 identical to
 * de_mavlink's.
 *
 * The "other source" signal is the bus itself: the module subscribes to
 * 9102 in its message filter, de_comm fans every other module's 9102 out
 * to subscribers, and {@link #onModuleMessage(JSONObject)} feeds those
 * reports into the gate. Wire the service's receive callback so it calls
 * this method for every inbound message.
 *
 * GPS_INPUT note (wiki/GPS-Injection.md): this feed never touches
 * Sensor_GPS.mLastGnssFix - the injected copy exists only while
 * gps_inject is on. The unit-position stream deliberately uses the same
 * mixed-provider "app location" the map and server reports already use,
 * and is unaffected by the gps_inject switch.
 *
 * Lifecycle: construct at module setup, {@link #start()} once the module
 * socket is up (the emitter talks UDP; the location feed could start
 * earlier but emits nothing without the bus anyway), {@link #stop()} on
 * module teardown. Both are idempotent.
 */
public class PhonePositionSource implements LocationListener {

    private static final String TAG = "PhonePositionSource";

    /** Emit period while the phone owns the position. de_comm marks a
     *  module position stale after ~3 s, so 1 Hz keeps the roster fresh. */
    private static final long EMIT_INTERVAL_MS = 1000;

    /** Same update request as Sensor_GPS (a hint the chipset ignores). */
    private static final long MIN_UPDATE_INTERVAL_MS = 200;
    private static final float MIN_UPDATE_DISTANCE_M = 0.0f;

    /** A last-known fix older than this is not worth seeding from. */
    private static final long SEED_FIX_MAX_AGE_MS = 60000;

    /** Supplies the app's GPS mode (GPS_MODE_* on AndruavUnitBase). */
    public interface GpsModeSource {
        int gpsMode();
    }

    private final Context m_context;
    private final CModule m_module;
    private final String m_own_module_key;
    private final GpsModeSource m_gps_mode_source;
    private final PositionSourceGate m_gate = new PositionSourceGate();
    private final LocationManager m_location_manager;

    private volatile PositionFix m_last_fix;
    private ScheduledExecutorService m_emitter;
    private boolean m_listening;

    public PhonePositionSource(Context context, CModule module,
                               String own_module_key,
                               GpsModeSource gps_mode_source) {
        this.m_context = context.getApplicationContext();
        this.m_module = module;
        this.m_own_module_key = own_module_key;
        this.m_gps_mode_source = gps_mode_source;
        this.m_location_manager =
                (LocationManager) this.m_context.getSystemService(Context.LOCATION_SERVICE);
    }

    /** Test hook: the arbitration state machine. */
    public PositionSourceGate gate() {
        return m_gate;
    }

    public synchronized void start() {
        _start_listening();
        if (m_emitter == null) {
            m_emitter = Executors.newSingleThreadScheduledExecutor();
            m_emitter.scheduleWithFixedDelay(this::_tick_safe,
                    EMIT_INTERVAL_MS, EMIT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void stop() {
        _stop_listening();
        if (m_emitter != null) {
            m_emitter.shutdownNow();
            m_emitter = null;
        }
        m_last_fix = null;
    }

    /**
     * Bus feed for the gate - call for every message the module's
     * OnReceiveCallback sees. Only 9102s from other senders matter;
     * de_comm already skips echoing our own reports back, and the key
     * check below is belt-and-suspenders for transports that would not.
     * Runs on the module's receive thread - must not block.
     */
    public void onModuleMessage(JSONObject jMsg) {
        try {
            if (jMsg == null
                    || jMsg.optInt(ANDRUAV_PROTOCOL_MESSAGE_TYPE)
                        != TYPE_AndruavModule_Location_Info) return;
            if (m_own_module_key != null
                    && m_own_module_key.equals(
                            jMsg.optString(INTERMODULE_MODULE_KEY, ""))) return;
            final Object cmd = jMsg.opt(ANDRUAV_PROTOCOL_MESSAGE_CMD);
            if (!(cmd instanceof JSONObject)) return;
            final JSONObject ms = (JSONObject) cmd;
            m_gate.onExternalLocationReport(_now_ms(),
                    ms.optInt("la"), ms.optInt("ln"));
        } catch (Exception e) {
            Log.w(TAG, "onModuleMessage failed", e);
        }
    }

    // -------------------------------------------------------- location

    private void _start_listening() {
        if (m_listening) return;
        if (m_location_manager == null) return;
        if (ActivityCompat.checkSelfPermission(m_context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "no fine-location grant - phone position source stays off");
            return;
        }
        try {
            // Seed from the freshest last-known fix so the first emit tick
            // does not have to wait for the chipset's next report.
            final Location seed = _best_last_known();
            if (seed != null) m_last_fix = _to_fix(seed);

            if (m_location_manager.getAllProviders().contains(
                    LocationManager.GPS_PROVIDER)) {
                m_location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_UPDATE_INTERVAL_MS, MIN_UPDATE_DISTANCE_M, this);
                m_listening = true;
            }
            // The unit position tolerates network/Wi-Fi fixes - unlike the
            // FC GPS_INPUT feed, which is deliberately GNSS-only.
            if (m_location_manager.getAllProviders().contains(
                    LocationManager.NETWORK_PROVIDER)) {
                m_location_manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        MIN_UPDATE_INTERVAL_MS, MIN_UPDATE_DISTANCE_M, this);
                m_listening = true;
            }
        } catch (SecurityException e) {
            Log.w(TAG, "location listener denied", e);
        } catch (Exception e) {
            Log.w(TAG, "location listener failed", e);
        }
    }

    private void _stop_listening() {
        if (!m_listening) return;
        try {
            m_location_manager.removeUpdates(this);
        } catch (Exception ignored) {
        }
        m_listening = false;
    }

    private Location _best_last_known() {
        Location best = null;
        try {
            for (String provider : m_location_manager.getAllProviders()) {
                final Location loc = m_location_manager.getLastKnownLocation(provider);
                if (loc == null) continue;
                if (_age_ms(loc) > SEED_FIX_MAX_AGE_MS) continue;
                if (_is_better_fix(loc, best)) best = loc;
            }
        } catch (SecurityException ignored) {
        } catch (Exception e) {
            Log.w(TAG, "last-known read failed", e);
        }
        return best;
    }

    @Override
    public void onLocationChanged(Location loc) {
        if (loc == null) return;
        final PositionFix fix = _to_fix(loc);
        final PositionFix current = m_last_fix;
        if (current == null || fix.elapsed_realtime_ms >= current.elapsed_realtime_ms) {
            m_last_fix = fix;
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    /** Newer fix wins; same-time ties keep the more accurate one. */
    private static boolean _is_better_fix(Location candidate, Location current) {
        if (current == null) return true;
        if (candidate.getElapsedRealtimeNanos() != current.getElapsedRealtimeNanos()) {
            return candidate.getElapsedRealtimeNanos() > current.getElapsedRealtimeNanos();
        }
        return candidate.hasAccuracy()
                && (!current.hasAccuracy()
                    || candidate.getAccuracy() <= current.getAccuracy());
    }

    private static long _age_ms(Location loc) {
        return (SystemClock.elapsedRealtimeNanos() - loc.getElapsedRealtimeNanos())
                / 1000000L;
    }

    private static PositionFix _to_fix(Location loc) {
        // API 34 reports a real MSL altitude; below that getAltitude() is
        // the WGS84 ellipsoid height. Either way the field is metres AMSL-
        // ish - de_mavlink's "a" is AMSL mm, and the geoid correction is
        // the only difference on older APIs.
        double altitude_m = loc.getAltitude();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && loc.hasMslAltitude()) {
            altitude_m = loc.getMslAltitudeMeters();
        }
        final double vertical_accuracy_m =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        && loc.hasVerticalAccuracy())
                        ? loc.getVerticalAccuracyMeters() : Double.NaN;
        final double speed_accuracy_mps =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        && loc.hasSpeedAccuracy())
                        ? loc.getSpeedAccuracyMetersPerSecond() : Double.NaN;
        return new PositionFix(
                loc.getLatitude(),
                loc.getLongitude(),
                altitude_m,
                loc.hasAccuracy() ? loc.getAccuracy() : Double.NaN,
                vertical_accuracy_m,
                speed_accuracy_mps,
                loc.hasBearing() ? loc.getBearing() : Double.NaN,
                loc.getElapsedRealtimeNanos() / 1000000L);
    }

    // ------------------------------------------------------------ emit

    private static long _now_ms() {
        return SystemClock.elapsedRealtimeNanos() / 1000000L;
    }

    private void _tick_safe() {
        try {
            _tick();
        } catch (Throwable t) {
            Log.w(TAG, "emit tick failed", t);
        }
    }

    private void _tick() {
        final PositionFix fix = m_last_fix;
        final long now = _now_ms();
        final long fix_age_ms = (fix == null) ? -1 : now - fix.elapsed_realtime_ms;
        if (!m_gate.shouldSendPhonePosition(now, m_gps_mode_source.gpsMode(),
                fix_age_ms)) {
            return;
        }
        m_module.sendJMSG("",
                new JSONObject(PositionSourceGate.buildLocationInfoFields(fix)),
                TYPE_AndruavModule_Location_Info, false);
    }
}
