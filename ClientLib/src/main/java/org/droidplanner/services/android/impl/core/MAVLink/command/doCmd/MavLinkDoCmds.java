package org.droidplanner.services.android.impl.core.MAVLink.command.doCmd;

import com.MAVLink.ardupilotmega.msg_digicam_control;
import com.MAVLink.ardupilotmega.msg_mount_control;
import com.MAVLink.common.msg_command_int;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.common.msg_mission_set_current;
import com.MAVLink.enums.GRIPPER_ACTIONS;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_FRAME;

import org.droidplanner.services.android.impl.core.MAVLink.MavLinkMissionItemInt;

import org.droidplanner.services.android.impl.core.drone.autopilot.MavLinkDrone;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.model.ICommandListener;

public class MavLinkDoCmds {

    public static void setVehicleHome(MavLinkDrone drone, LatLongAlt location, ICommandListener listener){
        if(drone == null || location == null)
            return;

        // COMMAND_INT carries lat/lon as degE7 int32 in x/y; COMMAND_LONG would
        // truncate them to float32 param5/6 (~1 m error).
        msg_command_int msg = new msg_command_int();
        msg.target_system = drone.getSysid();
        msg.target_component = drone.getCompid();
        msg.command = MAV_CMD.MAV_CMD_DO_SET_HOME;
        msg.frame = MAV_FRAME.MAV_FRAME_GLOBAL;

        msg.param1 = 0; // use specified location. if 1 then use current location.
        msg.x = MavLinkMissionItemInt.toDegE7(location.getLatitude());
        msg.y = MavLinkMissionItemInt.toDegE7(location.getLongitude());
        msg.z = (float) location.getAltitude();

        drone.getMavClient().sendMessage(msg, listener);
    }

    private static void setROI(MavLinkDrone drone, LatLongAlt coord, ICommandListener listener) {
        if (drone == null || coord == null)
            return;

        // COMMAND_INT carries lat/lon as degE7 int32 in x/y; COMMAND_LONG would
        // truncate them to float32 param5/6 (~1 m error). MAV_CMD_DO_SET_ROI_LOCATION
        // is the command_int variant of the deprecated MAV_CMD_DO_SET_ROI.
        msg_command_int msg = new msg_command_int();
        msg.target_system = drone.getSysid();
        msg.target_component = drone.getCompid();
        msg.command = MAV_CMD.MAV_CMD_DO_SET_ROI_LOCATION;
        msg.frame = MAV_FRAME.MAV_FRAME_GLOBAL;

        msg.x = MavLinkMissionItemInt.toDegE7(coord.getLatitude());
        msg.y = MavLinkMissionItemInt.toDegE7(coord.getLongitude());
        msg.z = (float) coord.getAltitude();

        drone.getMavClient().sendMessage(msg, listener);
    }

    public static void resetROI(MavLinkDrone drone, ICommandListener listener) {
        if (drone == null)
            return;

        msg_command_long msg = new msg_command_long();
        msg.target_system = drone.getSysid();
        msg.target_component = drone.getCompid();
        msg.command = MAV_CMD.MAV_CMD_DO_SET_ROI_NONE;

        drone.getMavClient().sendMessage(msg, listener);
    }

    public static void triggerCamera(MavLinkDrone drone) {
        if (drone == null)
            return;

        msg_digicam_control msg = new msg_digicam_control();
        msg.target_system = drone.getSysid();
        msg.target_component = drone.getCompid();
        msg.shot = 1;
        drone.getMavClient().sendMessage(msg, null);
    }

    /**
     * Move a servo to a particular pwm value
     *
     * @param drone   target vehicle
     * @param channel he output channel the servo is attached to
     * @param pwm     PWM value to output to the servo. Servo’s generally accept pwm values between 1000 and 2000
     */
    public static void setServo(MavLinkDrone drone, int channel, int pwm, ICommandListener listener) {
        if (drone == null)
            return;

        msg_command_long msg = new msg_command_long();
        msg.target_system = drone.getSysid();
        msg.target_component = drone.getCompid();
        msg.command = MAV_CMD.MAV_CMD_DO_SET_SERVO;
        msg.param1 = channel;
        msg.param2 = pwm;

        drone.getMavClient().sendMessage(msg, listener);
    }

    /**
     * Set the orientation of a gimbal
     *
     * @param drone    target vehicle
     * @param pitch    the desired gimbal pitch in degrees
     * @param roll     the desired gimbal roll in degrees
     * @param yaw      the desired gimbal yaw in degrees
     * @param listener Register a callback to receive update of the command execution state.
     *
     * * Note internally in ArduCopter code: if the mount doesn't do pan control then yaw the entire vehicle instead.
     */
    public static void setGimbalOrientation(MavLinkDrone drone, float pitch, float roll, float yaw, ICommandListener
            listener) {
        if (drone == null)
            return;

        msg_mount_control msg = new msg_mount_control();
        msg.target_system = drone.getSysid();
        msg.target_component = drone.getCompid();
        msg.input_a = (int) (pitch * 100);
        msg.input_b = (int) (roll * 100);
        msg.input_c = (int) (yaw * 100);

        drone.getMavClient().sendMessage(msg, listener);
    }

    /**
     * Jump to the desired command in the mission list. Repeat this action only the specified number of times
     *
     * @param drone    target vehicle
     * @param waypoint command
     * @param listener Register a callback to receive update of the command execution state.
     */
}
