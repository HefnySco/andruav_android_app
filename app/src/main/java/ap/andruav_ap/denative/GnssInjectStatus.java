package ap.andruav_ap.denative;

import android.os.SystemClock;

import org.json.JSONObject;

/***
 * AN-0169: last GNSS injection status (6549) de_mavlink reported on the
 * databus. de_mavlink emits it at ~1 Hz while a source is recent and on
 * every state change; the wire fields are the short names from
 * GNSS-Injection-Design.md (src/fix/ns/fc_gps/gid/inj/fin/fout/age/err/
 * ntrip/ntrip_err/ntrip_bps). Singleton - Settings and the PHONE > GPS
 * tab only ever read it, writes come from the bus dispatch thread.
 */
public class GnssInjectStatus {

    public static final int GPS_TYPE_MAV = 14;

    /** Elapsed-millis source - swapped by unit tests (android.jar stubs throw on the JVM). */
    public interface Clock { long nowMs(); }

    private static final GnssInjectStatus sInstance = new GnssInjectStatus();

    public static GnssInjectStatus getInstance() {
        return sInstance;
    }

    Clock clock = SystemClock::elapsedRealtime;

    public String source = "none";   // none | serial | bus | <source tag>
    public int fixType = -1;         // -1 = no fix reported
    public int satellites = -1;
    public int fcGpsType = -1;       // GPSx_TYPE of the slot GPS_INPUT addresses; -1 = unknown
    public int gpsId = -1;
    public String injectState = "idle";  // idle | active | timeout | error
    public long fixesIn;
    public long framesOut;
    public long lastFixAgeMs = -1;
    public String error;
    public String ntripState = "off";    // off | connecting | connected | auth_error | no_data | error
    public String ntripError;
    public int ntripBps = -1;            // -1 = not reported (state off)
    public boolean reported;             // any 6549 seen since process start

    /** GPS_INPUT frames per second between the last two status reports; -1 until two samples. */
    public double framesPerSecond = -1;

    public long updatedMonoMs;
    private long mPrevFramesOut = -1;
    private long mPrevMonoMs;

    public synchronized void update(final JSONObject ms) {
        final long now = clock.nowMs();
        final long out = ms.optLong("fout");
        framesPerSecond = (mPrevFramesOut >= 0 && now > mPrevMonoMs)
                ? (out - mPrevFramesOut) * 1000.0 / (now - mPrevMonoMs)
                : -1;
        mPrevFramesOut = out;
        mPrevMonoMs = now;

        source = ms.optString("src", "none");
        fixType = ms.has("fix") ? ms.optInt("fix") : -1;
        satellites = ms.has("ns") ? ms.optInt("ns") : -1;
        fcGpsType = ms.has("fc_gps") ? ms.optInt("fc_gps") : -1;
        gpsId = ms.has("gid") ? ms.optInt("gid") : -1;
        injectState = ms.optString("inj", "idle");
        fixesIn = ms.optLong("fin");
        framesOut = out;
        lastFixAgeMs = ms.has("age") ? ms.optLong("age") : -1;
        final String err = ms.optString("err", "");
        error = err.isEmpty() ? null : err;
        ntripState = ms.optString("ntrip", "off");
        final String nerr = ms.optString("ntrip_err", "");
        ntripError = nerr.isEmpty() ? null : nerr;
        ntripBps = ms.has("ntrip_bps") ? ms.optInt("ntrip_bps") : -1;
        reported = true;
        updatedMonoMs = now;
    }

    public synchronized void clear() {
        reported = false;
        mPrevFramesOut = -1;
        framesPerSecond = -1;
    }

    /***
     * de_mavlink reports on state change and while a source is recent, so an
     * idle link can stay silent for a while - 60 s covers one NTRIP retry
     * period without calling a live module "not connected".
     */
    private static final long FRESHNESS_MS = 60_000;

    public synchronized boolean isFresh() {
        return reported
            && (clock.nowMs() - updatedMonoMs) < FRESHNESS_MS;
    }
}
