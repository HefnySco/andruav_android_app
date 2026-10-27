package ap.andruav_ap.communication.controlBoard;

import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.controlBoard.ControlBoardBase;
import com.andruav.controlBoard.IControlBoard_Callback;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.controlBoard.shared.missions.MohemmaMapBase;
import com.andruav.notification.PanicFacade;
import com.andruav.protocol.R;
import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.protocol.communication.websocket.AndruavWSClientBase;

import org.json.JSONException;
import org.json.JSONObject;

import ap.andruav_ap.communication.AndruavDeBusClient;

/**
 * F3 (C4c, AN-0070): the native-mode flight-control board. Replaces
 * {@link ControlBoard_DroneKit} when the unit runs on the DroneEngage
 * native stack: the Java {@code :Mavlink} module must never touch the FC
 * (single-owner rule - the FC belongs to de_mavlink), so every command
 * goes over the databus as the same {@code mt}/{@code ms} a GCS sends,
 * addressed to this unit's own party id.
 * <p>
 * Routing: {@link AndruavDeBusClient#sendBusCommand} emits
 * {@code {GU,tg,ty:"i",mt,ms,p}} to de_comm, whose
 * {@code parseIntermoduleMessage} default branch fans the datagram out to
 * every locally subscribed module - de_mavlink executes it - and
 * separately relays it to the comm server when one is connected. The
 * local fan-out needs no server round trip, so commands keep working
 * while offline.
 * <p>
 * Callbacks are answered optimistically on dispatch: the bus is
 * fire-and-forget and de_mavlink confirms the outcome through the
 * telemetry stream (arm/mode changes mirror back into the me-unit via
 * {@code NativeUnitMirror}).
 */
public class ControlBoard_DeBus extends ControlBoardBase {

    // ---- Andruav message types (same ids a GCS sends) ----
    private static final int MT_RemoteExecute   = 1005;
    private static final int MT_FlightControl   = 1010;
    private static final int MT_Arm             = 1030;
    private static final int MT_ChangeAltitude  = 1031;
    private static final int MT_Land            = 1032;
    private static final int MT_GuidedPoint     = 1033;
    private static final int MT_CirclePoint     = 1034;
    private static final int MT_DoYAW           = 1035;
    private static final int MT_ChangeSpeed     = 1040;
    private static final int MT_GimbalCtrl      = 1045;
    private static final int MT_UploadWayPoints = 1046;
    private static final int MT_SetHomeLocation = 1048;

    // ---- RemoteExecute sub-commands de_mavlink answers ----
    private static final int RC_RELOAD_WAY_POINTS_FROM_FCB = 501;
    private static final int RC_CLEAR_WAY_POINTS           = 502;
    private static final int RC_SET_START_MISSION_ITEM     = 504;
    private static final int RC_REQUEST_PARA_LIST          = 505;

    public ControlBoard_DeBus(final AndruavUnitBase andruavUnitBase) {
        super(andruavUnitBase);
    }

    @Override
    public String getFCBDescription() {
        return "DroneEngage databus (de_mavlink)";
    }

    /*** Arm state is known only through the mirrored telemetry stream. */
    @Override
    public boolean isArmed() {
        return mAndruavUnitBase.IsArmed();
    }

    /**
     * Put one command on the bus addressed to this unit.
     * @return true when the datagram was handed to the bus client.
     */
    private boolean sendCommand(final int messageType, final JSONObject ms) {
        if (rcChannelBlock) return false;

        final AndruavWSClientBase ws = AndruavEngine.getAndruavWS();
        if (!(ws instanceof AndruavDeBusClient)) {
            // No bus client (native stack down / Java mode) - the command
            // has no route to de_mavlink; fail it like a missing board.
            return false;
        }
        ((AndruavDeBusClient) ws).sendBusCommand(mAndruavUnitBase.PartyID, ms, messageType);
        return true;
    }

    private void sendCommand(final int messageType, final JSONObject ms,
                             final IControlBoard_Callback callback) {
        if (sendCommand(messageType, ms)) {
            if (callback != null) callback.OnSuccess();
        } else {
            if (callback != null) callback.OnFailue(-1);
        }
    }

    private void sendFlightMode(final int flightMode, final IControlBoard_Callback callback) {
        sendCommand(MT_FlightControl, ms("F", flightMode), callback);
    }

    /*** Builds an {@code ms} payload; key/value pairs, checked JSONException wrapped. */
    private static JSONObject ms(final Object... keyValues) {
        final JSONObject ms = new JSONObject();
        try {
            for (int i = 0; (i + 1) < keyValues.length; i += 2) {
                ms.put((String) keyValues[i], keyValues[i + 1]);
            }
        } catch (JSONException e) {
            // constant literal keys can never fail - keep callers tidy
            throw new IllegalArgumentException(e);
        }
        return ms;
    }

    @Override
    public void do_Mode(final int flightMode, final IControlBoard_Callback callback) {
        // One hop: de_mavlink maps F through CFCBModes - no need to fan out
        // through the per-mode do_* methods like the DroneKit board does.
        sendFlightMode(flightMode, callback);
    }

    @Override
    public void do_ARM(final IControlBoard_Callback callback) {
        sendCommand(MT_Arm, ms("A", true), callback);
    }

    @Override
    public void do_Disarm(final boolean emergencyDisarm, final IControlBoard_Callback callback) {
        sendCommand(MT_Arm, ms("A", false, "D", emergencyDisarm), callback);
    }

    @Override
    public void do_Land(final IControlBoard_Callback callback) {
        sendCommand(MT_Land, new JSONObject(), callback);
    }

    @Override
    public void do_ChangeAltitude(final double altitude, final IControlBoard_Callback callback) {
        sendCommand(MT_ChangeAltitude, ms("a", altitude), callback);
    }

    /*** Ground takeoff: de_mavlink's ChangeAltitude handler takes off when not flying. */
    @Override
    public void do_TakeOff(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_TAKEOFF, callback);
    }

    @Override
    public void do_RTL(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_RTL, callback);
    }

    @Override
    public void do_Smart_RTL(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_SMART_RTL, callback);
    }

    @Override
    public void do_Guided(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_GUIDED, callback);
    }

    @Override
    public void do_Loiter(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_LOITER, callback);
    }

    @Override
    public void do_Auto(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_AUTO, callback);
    }

    @Override
    public void do_Manual(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_MANUAL, callback);
    }

    @Override
    public void do_Acro(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_ACRO, callback);
    }

    @Override
    public void do_POS_Hold(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_POSTION_HOLD, callback);
    }

    @Override
    public void do_ALT_Hold(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_ALT_HOLD, callback);
    }

    @Override
    public void do_Brake(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_BRAKE, callback);
    }

    @Override
    public void do_Hold(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_BRAKE, callback);
    }

    @Override
    public void do_FBWA(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_FBWA, callback);
    }

    @Override
    public void do_FBWB(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_FBWB, callback);
    }

    @Override
    public void do_Cruise(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_CRUISE, callback);
    }

    @Override
    public void do_Surface(final IControlBoard_Callback callback) {
        sendFlightMode(FlightMode.CONST_FLIGHT_CONTROL_SURFACE, callback);
    }

    /*** Guided goto: GCS wire layout is a=latitude, g=longitude, l=altitude. */
    @Override
    public void do_FlytoHere(final double lng, final double lat, final double alt,
                             final double xVel, final double yVel, final double zVel,
                             final double yaw, final double yawRate) {
        if (!sendCommand(MT_GuidedPoint, ms("a", lat, "g", lng, "l", alt))) {
            return;
        }
        target_gps_lat = lat;
        target_gps_lng = lng;
        target_gps_alt = alt;
    }

    @Override
    public void do_CircleHere(final double lng, final double lat, final double alt,
                              final double radius, final int turns,
                              final IControlBoard_Callback callback) {
        sendCommand(MT_CirclePoint, ms("a", lat, "g", lng, "l", alt, "r", radius, "t", turns), callback);
    }

    @Override
    public void do_Yaw(final double targetAngle, final double turnRate,
                       final boolean isClockwise, final boolean isRelative) {
        sendCommand(MT_DoYAW, ms("A", targetAngle, "R", turnRate, "C", isClockwise, "L", isRelative));
    }

    /*** Speed change; -1 means "no change" (same convention as the GCS command). */
    @Override
    public void do_SetNavigationSpeed(final double speed, final boolean isGroundSpeed,
                                      final double throttle, final boolean isRelative) {
        if (speed == -1) return;
        sendCommand(MT_ChangeSpeed, ms("a", speed, "b", isGroundSpeed, "c", throttle, "d", isRelative));
    }

    /**
     * Gimbal axes in deg*100 (same wire fields the GCS sends; for
     * GPS-point mounts they carry lat/lon/alt-cm instead - the mount mode
     * decides at de_mavlink).
     */
    @Override
    public void do_GimbalCtrl(final double pitch, final double roll, final double yaw,
                              final boolean isAbsolute) {
        sendCommand(MT_GimbalCtrl, ms("A", pitch, "B", roll, "C", yaw, "D", isAbsolute));
    }

    @Override
    public void do_GimbalCtrlByGPS(final double lng, final double lat, final double alt) {
        // GPS-point mount: A/B/C carry lat/lon/alt(cm), absolute pointing.
        sendCommand(MT_GimbalCtrl, ms("A", lat, "B", lng, "C", alt, "D", true));
    }

    /*** Home set: GCS wire layout is T=latitude, O=longitude, A=altitude. */
    @Override
    public void do_SetHomeLocation(final double longitude, final double latitude,
                                   final double altitude) {
        sendCommand(MT_SetHomeLocation, ms("T", latitude, "O", longitude, "A", altitude));
    }

    @Override
    public void do_ClearHomeLocation() {
        do_SetHomeLocation(0, 0, 0);
    }

    /*** RemoteExecute {C:504, n:int} - n must be a JSON number for de_mavlink. */
    @Override
    public void do_SetCurrentMission(final int missionItemNumber) {
        sendCommand(MT_RemoteExecute, ms("C", RC_SET_START_MISSION_ITEM, "n", missionItemNumber));
    }

    /*** RemoteExecute {C:501} - de_mavlink reloads waypoints from the FC. */
    @Override
    public void do_ReadMission() {
        sendCommand(MT_RemoteExecute, ms("C", RC_RELOAD_WAY_POINTS_FROM_FCB));
    }

    /*** RemoteExecute {C:502} - de_mavlink clears the FC mission. */
    @Override
    public void do_ClearMission() {
        sendCommand(MT_RemoteExecute, ms("C", RC_CLEAR_WAY_POINTS));
    }

    /*** RemoteExecute {C:505} - de_mavlink answers with the FC parameter list. */
    public void do_ReadParameterList() {
        sendCommand(MT_RemoteExecute, ms("C", RC_REQUEST_PARA_LIST));
    }

    /*** Upload a serialized QGC/MP plan text - the same 1046 payload a GCS sends. */
    @Override
    public void doPutMissionintoFCB(final String missionText) {
        if ((missionText == null) || missionText.isEmpty()) return;
        sendCommand(MT_UploadWayPoints, ms("a", missionText));
    }

    /**
     * MohemmaMap -> FC mission upload needs a plan-text serializer the
     * Java side never had (DroneKit converted items one by one). Left to
     * the base until a serializer exists - callers get the standard
     * "cannot do" notice instead of a silent no-op.
     */
    @Override
    public void do_SendMission(final MohemmaMapBase mohemmaMapBase) {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference()
                .getContext().getString(R.string.andruav_error_autopilot_cannot_do_mission_write));
    }
}
