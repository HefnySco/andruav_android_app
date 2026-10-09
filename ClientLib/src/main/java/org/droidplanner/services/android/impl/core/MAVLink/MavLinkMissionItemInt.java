package org.droidplanner.services.android.impl.core.MAVLink;

import com.MAVLink.common.msg_mission_item;
import com.MAVLink.common.msg_mission_item_int;
import com.MAVLink.enums.MAV_CMD;

/**
 * Helpers for MISSION_ITEM_INT, which carries x/y as int32 degE7 instead of the
 * float32 degrees used by the deprecated MISSION_ITEM (float32 loses up to ~1.7 m
 * near extreme longitudes).
 */
public class MavLinkMissionItemInt {

    public static int toDegE7(double degrees) {
        return (int) Math.round(degrees * 1E7);
    }

    public static double fromDegE7(int degE7) {
        return degE7 / 1E7;
    }

    /**
     * True when param5/param6 (x/y) of this command hold latitude/longitude, so they
     * are scaled by 1E7 in MISSION_ITEM_INT. Other commands carry x/y as raw integers.
     * Mirrors ArduPilot's AP_Mission::stored_in_location().
     */
    public static boolean isLocationCommand(int command) {
        switch (command) {
            case MAV_CMD.MAV_CMD_NAV_WAYPOINT:
            case MAV_CMD.MAV_CMD_NAV_LOITER_UNLIM:
            case MAV_CMD.MAV_CMD_NAV_LOITER_TURNS:
            case MAV_CMD.MAV_CMD_NAV_LOITER_TIME:
            case MAV_CMD.MAV_CMD_NAV_LAND:
            case MAV_CMD.MAV_CMD_NAV_TAKEOFF:
            case MAV_CMD.MAV_CMD_NAV_CONTINUE_AND_CHANGE_ALT:
            case MAV_CMD.MAV_CMD_NAV_LOITER_TO_ALT:
            case MAV_CMD.MAV_CMD_NAV_SPLINE_WAYPOINT:
            case MAV_CMD.MAV_CMD_NAV_GUIDED_ENABLE:
            case MAV_CMD.MAV_CMD_NAV_VTOL_TAKEOFF:
            case MAV_CMD.MAV_CMD_NAV_VTOL_LAND:
            case MAV_CMD.MAV_CMD_NAV_PAYLOAD_PLACE:
            case MAV_CMD.MAV_CMD_DO_SET_HOME:
            case MAV_CMD.MAV_CMD_DO_LAND_START:
            case MAV_CMD.MAV_CMD_DO_GO_AROUND:
            case MAV_CMD.MAV_CMD_DO_SET_ROI:
            case MAV_CMD.MAV_CMD_DO_SET_ROI_LOCATION:
                return true;
            default:
                return false;
        }
    }

    /**
     * Encodes a param5/param6 value given in its natural unit (degrees for location
     * commands, raw value otherwise) into the MISSION_ITEM_INT x/y field.
     */
    public static int encodeXY(int command, double value) {
        return isLocationCommand(command) ? toDegE7(value) : (int) Math.round(value);
    }

    /**
     * Converts a deprecated MISSION_ITEM (float32 x/y) into MISSION_ITEM_INT.
     * Used only when an old autopilot answers with MISSION_ITEM.
     */
    public static msg_mission_item_int fromMissionItem(msg_mission_item item) {
        msg_mission_item_int msg = new msg_mission_item_int();
        msg.sysid = item.sysid;
        msg.compid = item.compid;
        msg.param1 = item.param1;
        msg.param2 = item.param2;
        msg.param3 = item.param3;
        msg.param4 = item.param4;
        msg.x = encodeXY(item.command, item.x);
        msg.y = encodeXY(item.command, item.y);
        msg.z = item.z;
        msg.seq = item.seq;
        msg.command = item.command;
        msg.target_system = item.target_system;
        msg.target_component = item.target_component;
        msg.frame = item.frame;
        msg.current = item.current;
        msg.autocontinue = item.autocontinue;
        msg.mission_type = item.mission_type;
        return msg;
    }
}
