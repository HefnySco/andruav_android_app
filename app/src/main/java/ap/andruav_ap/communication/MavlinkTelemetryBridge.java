package ap.andruav_ap.communication;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Parser;
import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_battery_status;
import com.MAVLink.common.msg_gps_raw_int;
import com.MAVLink.common.msg_mission_current;
import com.MAVLink.common.msg_nav_controller_output;
import com.MAVLink.common.msg_statustext;
import com.MAVLink.common.msg_sys_status;
import com.MAVLink.common.msg_vfr_hud;
import com.MAVLink.enums.MAV_MODE_FLAG;
import com.MAVLink.enums.MAV_STATE;
import com.MAVLink.enums.MAV_SYS_STATUS_SENSOR;
import com.MAVLink.minimal.msg_heartbeat;
import com.MAVLink.standard.msg_global_position_int;
import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavLocation;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.controlBoard.shared.common.VehicleTypes;
import com.andruav.event.droneReport_Event.Event_Battery_Ready;
import com.andruav.event.droneReport_Event.Event_GPS_Ready;
import com.andruav.event.droneReport_Event.Event_IMU_Ready;
import com.andruav.sensors.AndruavIMU;

import ap.andruav_ap.communication.controlBoard.mavlink.DroneMavlinkHandler;
import ap.andruav_ap.communication.controlBoard.mavlink.MavLink_Helpers;

/**
 * F2 (C4b, AN-0069): decodes the binary body of
 * {@code TYPE_AndruavMessage_MAVLINK} (6502) datagrams - packed MAVLink
 * frames, the only telemetry channel de_mavlink emits (the legacy
 * Andruav JSON GPS/NAV/POW types are dead on the DroneEngage side) -
 * into the sender's unit-map entry, then mirrors them onto the me-unit
 * when the sender is this unit (the C1-spike "shadow unit").
 * <p>
 * The frames are standard MAVLink wire bytes back to back (what
 * {@code mavlink_msg_to_send_buffer} produces), re-assembled by
 * {@link Parser}; decoded messages update the same
 * {@code LastEvent_FCB_IMU}/{@code LastEvent_Battery}/arm/mode fields
 * the WS-path messages fill, and post the same EventBus events
 * ({@code Event_IMU_Ready}, {@code Event_GPS_Ready},
 * {@code Event_Battery_Ready}) so every screen works unchanged.
 */
public class MavlinkTelemetryBridge {

    /*** Window in which an own-unit 6502 frame counts as "FC telemetry live". */
    public static final long TELEMETRY_LIVE_MS = 4000;

    private static volatile long sLastOwnTelemetryMs = 0;

    private final Parser mParser = new Parser();
    private String mUnitPartyId;

    /*** True while this unit's own MAVLink telemetry has been seen recently. */
    public static boolean isOwnTelemetryLive() {
        return (System.currentTimeMillis() - sLastOwnTelemetryMs) < TELEMETRY_LIVE_MS;
    }

    /*** The unit's own party id - marks own-unit telemetry + mirrors it to me. */
    public void setUnitIdentity(final String partyId) {
        mUnitPartyId = partyId;
    }

    /**
     * Feeds one 6502 binary body (packed MAVLink frames) from {@code partyID}.
     * Runs on the bus client's handler thread.
     */
    public void feed(final String partyID, final byte[] data, final int offset, final int length) {
        if ((data == null) || (length <= 0)) return;

        final AndruavUnitBase unit = AndruavEngine.getAndruavWe7daMapBase().get(partyID);
        if (unit == null) {
            // no 1004 seen yet - same rule the map's update* helpers apply
            return;
        }

        boolean imuUpdated = false;
        boolean gpsUpdated = false;
        boolean batteryUpdated = false;

        final int end = Math.min(offset + length, data.length);
        for (int i = offset; i < end; ++i) {
            final MAVLinkPacket packet = mParser.mavlink_parse_char(data[i]);
            if (packet == null) continue;

            final MAVLinkMessage message;
            try {
                message = packet.unpack();
            } catch (Exception e) {
                continue;
            }
            if (message == null) continue;

            switch (message.msgid) {
                case msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT:
                    onHeartbeat(unit, (msg_heartbeat) message);
                    break;

                case msg_attitude.MAVLINK_MSG_ID_ATTITUDE: {
                    final msg_attitude attitude = (msg_attitude) message;
                    // mavlink radians -> the degrees the app keeps
                    unit.LastEvent_FCB_IMU.P = Math.toDegrees(attitude.pitch);
                    unit.LastEvent_FCB_IMU.R = Math.toDegrees(attitude.roll);
                    unit.LastEvent_FCB_IMU.Y = Math.toDegrees(attitude.yaw);
                    unit.useFCBIMU(true);
                    imuUpdated = true;
                    break;
                }

                case msg_nav_controller_output.MAVLINK_MSG_ID_NAV_CONTROLLER_OUTPUT: {
                    final msg_nav_controller_output nav = (msg_nav_controller_output) message;
                    unit.LastEvent_FCB_IMU.nav_Pitch = nav.nav_pitch;
                    unit.LastEvent_FCB_IMU.nav_Roll = nav.nav_roll;
                    unit.LastEvent_FCB_IMU.nav_TargetBearing = nav.target_bearing;
                    unit.LastEvent_FCB_IMU.nav_AltitudeError = nav.alt_error;
                    unit.LastEvent_FCB_IMU.nav_WayPointDistance = nav.wp_dist;
                    imuUpdated = true;
                    break;
                }

                case msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
                    onGlobalPosition(unit, (msg_global_position_int) message);
                    gpsUpdated = true;
                    break;

                case msg_gps_raw_int.MAVLINK_MSG_ID_GPS_RAW_INT: {
                    final msg_gps_raw_int gps = (msg_gps_raw_int) message;
                    unit.LastEvent_FCB_IMU.SATC = gps.satellites_visible;
                    unit.LastEvent_FCB_IMU.GPS3DFix = gps.fix_type;
                    unit.LastEvent_FCB_IMU.GPSFixQuality = gps.fix_type;
                    if (gps.eph != 65535) { // UINT16_MAX = unknown
                        unit.LastEvent_FCB_IMU.Hdop = gps.eph / 100.0f;
                    }
                    gpsUpdated = true;
                    break;
                }

                case msg_sys_status.MAVLINK_MSG_ID_SYS_STATUS: {
                    final msg_sys_status sys = (msg_sys_status) message;
                    unit.isReadyToArm((sys.onboard_control_sensors_health
                            & MAV_SYS_STATUS_SENSOR.MAV_SYS_STATUS_PREARM_CHECK) != 0);
                    unit.LastEvent_Battery.FCB_BatteryVoltage = sys.voltage_battery / 1000.0;
                    if (sys.current_battery >= 0) { // -1 = not measured
                        unit.LastEvent_Battery.FCB_CurrentConsumed = sys.current_battery / 100.0;
                    }
                    if (sys.battery_remaining >= 0) { // -1 = not measured
                        unit.LastEvent_Battery.FCB_BatteryRemaining = sys.battery_remaining;
                    }
                    batteryUpdated = true;
                    break;
                }

                case msg_battery_status.MAVLINK_MSG_ID_BATTERY_STATUS: {
                    final msg_battery_status battery = (msg_battery_status) message;
                    if (battery.battery_remaining >= 0) {
                        unit.LastEvent_Battery.FCB_BatteryRemaining = battery.battery_remaining;
                    }
                    long millivolts = 0;
                    for (int v = 0; v < battery.voltages.length; ++v) {
                        if (battery.voltages[v] != 65535) { // UINT16_MAX = not measured
                            millivolts += battery.voltages[v];
                        }
                    }
                    if (millivolts > 0) {
                        unit.LastEvent_Battery.FCB_BatteryVoltage = millivolts / 1000.0;
                    }
                    if (battery.current_battery >= 0) {
                        unit.LastEvent_Battery.FCB_CurrentConsumed = battery.current_battery / 100.0;
                    }
                    batteryUpdated = true;
                    break;
                }

                case msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD: {
                    final msg_vfr_hud hud = (msg_vfr_hud) message;
                    final AndruavLocation loc = unit.LastEvent_FCB_IMU.getCurrentLocation();
                    if (loc != null) {
                        loc.setSpeed(hud.groundspeed);
                    }
                    break;
                }

                case msg_mission_current.MAVLINK_MSG_ID_MISSION_CURRENT:
                    unit.missionItemReached(((msg_mission_current) message).seq);
                    break;

                case msg_statustext.MAVLINK_MSG_ID_STATUSTEXT:
                    DroneMavlinkHandler.execute_StatusMessage((msg_statustext) message);
                    break;

                default:
                    break;
            }
        }

        if (!imuUpdated && !gpsUpdated && !batteryUpdated) {
            return;
        }

        unit.lastActiveTime = System.currentTimeMillis();
        unit.unitStatus = AndruavUnitBase.enum_userStatus.ALIVE;

        if (NativeUnitMirror.isOwnUnit(partyID)
                || ((mUnitPartyId != null) && mUnitPartyId.equals(partyID))) {
            sLastOwnTelemetryMs = unit.lastActiveTime;
            NativeUnitMirror.syncFromShadow(unit);
        }

        if (imuUpdated) {
            AndruavEngine.getEventBus().post(new Event_IMU_Ready(unit));
        }
        if (gpsUpdated) {
            AndruavEngine.getEventBus().post(new Event_GPS_Ready(unit));
        }
        if (batteryUpdated) {
            AndruavEngine.getEventBus().post(new Event_Battery_Ready(unit));
        }
    }


    /**
     * HEARTBEAT carries arm + custom mode + vehicle type - mapped to the
     * app's andruav flight-mode ids the same way the DroneKit path does.
     */
    private void onHeartbeat(final AndruavUnitBase unit, final msg_heartbeat heartbeat) {
        if (heartbeat.compid != 1) return; // ignore non-autopilot components
        if (heartbeat.sysid == 255) return; // ignore GCS heartbeats

        unit.setVehicleType(MavLink_Helpers.setCommonVehicleType(heartbeat.type));

        final boolean isArmed =
                ((heartbeat.base_mode & MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED)
                        == MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED)
                        || (unit.getVehicleType() == VehicleTypes.VEHICLE_ROVER);
        unit.IsArmed(isArmed);

        unit.IsFlying(isArmed
                && (heartbeat.system_status == MAV_STATE.MAV_STATE_ACTIVE
                || (unit.IsFlying() && (heartbeat.system_status == MAV_STATE.MAV_STATE_CRITICAL
                || heartbeat.system_status == MAV_STATE.MAV_STATE_EMERGENCY))));

        unit.setFlightModeFromBoard(MavLink_Helpers.getAndruavStandardFlightMode(
                heartbeat.type, (short) heartbeat.custom_mode));
        unit.useFCBIMU(true);
    }


    /**
     * GLOBAL_POSITION_INT -> the unit's FCB location (degE7 / mm ->
     * degrees / meters). The android.location write is isolated so a
     * host-JVM unit test (no android.location) still lands the scalars.
     */
    private void onGlobalPosition(final AndruavUnitBase unit,
                                  final msg_global_position_int position) {
        try {
            final AndruavLocation location = new AndruavLocation("mavlink");
            location.setLatitude(position.lat / 1e7);
            location.setLongitude(position.lon / 1e7);
            location.setAltitude(position.alt / 1000.0);
            location.setAltitudeRelative(position.relative_alt / 1000.0);
            if (position.hdg != 65535) { // UINT16_MAX = unknown
                location.setBearing(position.hdg / 100.0f);
            }
            unit.LastEvent_FCB_IMU.setCurrentLocation(location);
        } catch (RuntimeException e) {
            // android.location stub on host JVM tests - scalars still land.
        }
        unit.useFCBIMU(true);
    }
}
