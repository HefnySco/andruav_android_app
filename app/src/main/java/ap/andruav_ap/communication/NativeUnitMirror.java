package ap.andruav_ap.communication;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.TelemetryProtocol;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitMe;
import com.andruav.controlBoard.shared.geoFence.GeoFenceBase;
import com.andruav.controlBoard.shared.geoFence.GeoFenceManager;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GeoFence;
import com.andruav.protocol.commands.textMessages.AndruavMessage_UDPProxy_Info;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;
import com.andruav.sensors.AndruavIMU;

/**
 * F2 (C4b, AN-0069): mirrors the own-unit "shadow" entry of
 * {@link com.andruav.andruavUnit.AndruavUnitMapBase} into
 * {@link AndruavSettings#andruavWe7daBase} (the me-unit every screen
 * already reads).
 * <p>
 * In native mode the unit-map entry keyed by the unit's own party id is
 * fed by de_mavlink/de_comm telemetry (the "shadow unit" of the C1
 * spike). The map keeps it as a normal remote-style unit; this class
 * copies the live telemetry fields onto the me-unit so the home card,
 * FCB screens, mission and POW views keep working unchanged.
 * <p>
 * Deliberately NOT mirrored: identity (PartyID/GroupName/UnitID/
 * Description), permissions (de_comm reports its own placeholder string,
 * which would zero the app-side can* flags), GPS-mode source and the
 * telemetry protocol (a protocol flip would rebuild the me-unit's
 * FCBoard through {@code Telemetry_protocol_changed} - the FC belongs
 * to de_mavlink in native mode).
 */
public final class NativeUnitMirror {

    /*** Posted when de_mavlink reports a new UDP-proxy status for this unit. */
    public static final class Event_OwnUdpProxy {
    }

    // The me-unit's own udp fields belong to the Java UDPProxy, which never
    // runs in native mode: de_mavlink owns the proxy and reports it as 1071.
    private static volatile AndruavMessage_UDPProxy_Info sOwnUdpProxy = null;

    private NativeUnitMirror() {
    }

    /*** The last UDP-proxy status (1071) de_mavlink reported for this unit, or null. */
    public static AndruavMessage_UDPProxy_Info ownUdpProxy() {
        return sOwnUdpProxy;
    }

    /*** Forgets the reported UDP-proxy status (native stack stopped). */
    public static void clearOwnUdpProxy() {
        sOwnUdpProxy = null;
    }

    /*** True when {@code partyID} is this unit's own party id. */
    public static boolean isOwnUnit(final String partyID) {
        final AndruavUnitMe me = AndruavSettings.andruavWe7daBase;
        return (me != null) && (partyID != null) && partyID.equals(me.PartyID);
    }


    /**
     * True when a flight controller is really on de_mavlink's link: its
     * 6502 stream is live AND the own-unit 1004 reports a telemetry
     * protocol. de_mavlink sends attitude/nav/vfr_hud on a timer from its
     * cached (zeroed) vehicle state even with no FC, so the stream alone
     * is not proof; it resends the 1004 with TP=no-telemetry while the FC
     * link is down.
     */
    public static boolean isOwnFcLinked() {
        if (!MavlinkTelemetryBridge.isOwnTelemetryLive()) return false;
        final AndruavUnitMe me = AndruavSettings.andruavWe7daBase;
        if ((me == null) || (me.PartyID == null)) return false;
        final AndruavUnitBase shadow = AndruavEngine.getAndruavWe7daMapBase().get(me.PartyID);
        if (shadow == null) return false;
        final int protocol = shadow.getTelemetry_protocol();
        return (protocol != TelemetryProtocol.TelemetryProtocol_No_Telemetry)
                && (protocol != TelemetryProtocol.TelemetryProtocol_Unknown_Telemetry);
    }


    /**
     * Called for every inbound comm message whose sender is this unit.
     * Resolves the shadow entry and runs {@link #syncFromShadow}, then
     * applies the message-specific bits that only matter on the me-unit
     * (today: the reported geofence feeds GeoFenceManager).
     */
    public static void onOwnUnitMessage(final Andruav_2MR andruav2MR) {
        if ((andruav2MR == null) || !isOwnUnit(andruav2MR.partyID)) return;

        final AndruavUnitBase shadow =
                AndruavEngine.getAndruavWe7daMapBase().get(andruav2MR.partyID);
        if (shadow != null) {
            syncFromShadow(shadow);
        }

        if (andruav2MR.andruavMessageBase instanceof AndruavMessage_UDPProxy_Info) {
            sOwnUdpProxy = (AndruavMessage_UDPProxy_Info) andruav2MR.andruavMessageBase;
            AndruavEngine.getEventBus().post(new Event_OwnUdpProxy());
        }

        if (andruav2MR.andruavMessageBase != null
                && andruav2MR.andruavMessageBase.messageTypeID
                    == AndruavMessage_GeoFence.TYPE_AndruavMessage_GeoFence) {
            // A drone's own 1023 fence report: the default Execute() branch
            // ignores it ("don't save other drones' fences"), but for the
            // own unit it IS the fence the geofence screen shows.
            final AndruavUnitMe me = AndruavSettings.andruavWe7daBase;
            final GeoFenceBase fence =
                    ((AndruavMessage_GeoFence) andruav2MR.andruavMessageBase).getGeoFencePoints();
            if ((me != null) && (fence != null)) {
                GeoFenceManager.addGeoFence(me, fence);
            }
        }
    }


    /**
     * Copies the live telemetry fields of the own-unit shadow entry onto
     * the me-unit, using the same setters the WS-path updates use so the
     * same change events fire (arm/mode/flying/GCS-block).
     */
    public static void syncFromShadow(final AndruavUnitBase shadow) {
        final AndruavUnitMe me = AndruavSettings.andruavWe7daBase;
        if ((shadow == null) || (me == null)) return;
        if (!isOwnUnit(shadow.PartyID)) return;

        // --- vehicle state (from the 1004 heartbeat / mavlink heartbeat) ---
        me.setVehicleType(shadow.getVehicleType());
        me.IsArmed(shadow.IsArmed());
        me.isReadyToArm(shadow.isReadyToArm());
        me.IsFlying(shadow.IsFlying());
        me.setFlightModeFromBoard(shadow.getFlightModeFromBoard());
        me.setFlyingStartTime(shadow.getFlyingStartTime());
        me.setFlyingTotalDuration(shadow.getFlyingTotalDuration());
        me.setisGCSBlockedFromBoard(shadow.getisGCSBlockedFromBoard());
        me.setManualTXBlockedSubAction(shadow.getManualTXBlockedSubAction());
        me.useFCBIMU(shadow.useFCBIMU());
        me.VideoRecording = shadow.VideoRecording;

        // --- FCB attitude/nav ---
        final AndruavIMU src = shadow.LastEvent_FCB_IMU;
        final AndruavIMU dst = me.LastEvent_FCB_IMU;
        dst.P = src.P;
        dst.R = src.R;
        dst.Y = src.Y;
        dst.PT = src.PT;
        dst.RT = src.RT;
        dst.nav_Pitch = src.nav_Pitch;
        dst.nav_Roll = src.nav_Roll;
        dst.nav_TargetBearing = src.nav_TargetBearing;
        dst.nav_WayPointDistance = src.nav_WayPointDistance;
        dst.nav_AltitudeError = src.nav_AltitudeError;
        dst.SATC = src.SATC;
        dst.GPS3DFix = src.GPS3DFix;
        dst.GPSFixQuality = src.GPSFixQuality;
        dst.Hdop = src.Hdop;
        if (src.hasCurrentLocation()) {
            dst.setCurrentLocation(src.getCurrentLocation());
        }

        // --- FCB battery (the phone's own battery fields stay local) ---
        me.LastEvent_Battery.FCB_BatteryVoltage = shadow.LastEvent_Battery.FCB_BatteryVoltage;
        me.LastEvent_Battery.FCB_BatteryRemaining = shadow.LastEvent_Battery.FCB_BatteryRemaining;
        me.LastEvent_Battery.FCB_CurrentConsumed = shadow.LastEvent_Battery.FCB_CurrentConsumed;

        // --- home / guided target (defaults are -1,-1,-1: only copy a real fix) ---
        if (shadow.getGpsHomeLocation().getLatitude() != -1.0
                || shadow.getGpsHomeLocation().getLongitude() != -1.0) {
            me.updateHomeLocation(
                    shadow.getGpsHomeLocation().getLongitude(),
                    shadow.getGpsHomeLocation().getLatitude(),
                    shadow.getGpsHomeLocation().getAltitude());
        }
        if (shadow.getGpsTargetLocation().getLatitude() != -1.0
                || shadow.getGpsTargetLocation().getLongitude() != -1.0) {
            me.internal_updateTargetLocation(
                    shadow.getGpsTargetLocation().getLongitude(),
                    shadow.getGpsTargetLocation().getLatitude(),
                    shadow.getGpsTargetLocation().getAltitude());
        }

        // --- mission list (what the FC actually holds, via de_mavlink) ---
        me.refreshWayPoints(shadow.getMohemmaMapBase());
    }
}
