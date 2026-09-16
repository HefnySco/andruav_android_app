package ap.andruav_ap.communication.telemetry.rtcm;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import ap.andruavmiddlelibrary.ntrip.NtripClient;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.sensors._7asasatEvents.Event_GPS_NMEA;
import ap.andruav_ap.App;

/***
 * Owns the NTRIP-to-FC correction pipeline: starts/stops {@link NtripClient} with the FC
 * link, forwards every RTCM chunk to {@link ap.andruav_ap.communication.telemetry.DroneKit.
 * DroneKitServer#do_InjectRTCM(byte[], int)} and keeps the latest GGA sentence flowing back
 * to the caster for VRS / network-RTK.
 *
 * The complement of GPS injection: GPS_INPUT replaces a missing GPS with the phone's own,
 * while this improves the FC's real RTK-capable GPS (u-blox F9P/M8P...) with centimetre
 * corrections. The two are independent and deliberately not coupled.
 *
 * Corrections are useless without an FC link and must never queue up unboundedly, so
 * onRtcmData silently drops chunks while the link is down.
 */
public class RtcmInjector implements NtripClient.NtripListener {

    private final NtripClient mNtripClient = new NtripClient(this);

    // Status counters for the status UI / logs.
    private long mBytesForwarded = 0;
    private long mMessagesSent = 0;
    private volatile String mLastError = null;



    /*** Starts the NTRIP client and subscribes to NMEA for the GGA upload. Idempotent. */
    public void start () {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        mNtripClient.start();
    }

    /*** Stops the NTRIP client and unsubscribes. Idempotent. */
    public void stop () {
        mNtripClient.stop();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }



    @Override
    public void onRtcmData (final byte[] buffer, final int length) {
        final ap.andruav_ap.communication.telemetry.DroneKit.DroneKitServer droneKitServer = App.droneKitServer;
        if ((droneKitServer == null) || (!droneKitServer.isConnected())) {
            // Drop, do not queue: corrections have a shelf life of seconds and the FC link
            // is the whole point - without it there is nothing to improve.
            return;
        }

        // NtripClient reads into a <=180-byte buffer so no further chunking is needed here.
        // If that ever changes: chunk in do_InjectRTCM's caller - never send len > 180.
        droneKitServer.do_InjectRTCM(buffer, length);
        mBytesForwarded += length;
        mMessagesSent++;
    }

    @Override
    public void onNtripState (final boolean connected, final String message) {
        mLastError = connected ? null : message;
    }

    /***
     * Keeps the most recent GGA sentence for the caster upload. Matches with endsWith("GGA")
     * rather than equals("$GPGGA") - modern multi-constellation phones emit $GNGGA (a
     * $GPGGA-only match silently broke this once before).
     */
    @Subscribe
    public void onEvent (final Event_GPS_NMEA event) {
        if ((event == null) || (event.nmea == null)) return;
        if (!Preference.isNtripSendGga(null)) return;

        final String nmea = event.nmea.trim();
        if (nmea.endsWith("GGA")) {
            mNtripClient.setGgaSentence(nmea);
        }
    }



    public long getBytesForwarded () {
        return mBytesForwarded;
    }

    public long getMessagesSent () {
        return mMessagesSent;
    }

    /*** Last connection error line from the caster, or null while connected. */
    public String getLastError () {
        return mLastError;
    }

    public boolean isNtripConnected () {
        return mNtripClient.isRunning();
    }
}
