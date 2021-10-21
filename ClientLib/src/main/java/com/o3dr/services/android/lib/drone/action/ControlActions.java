package com.o3dr.services.android.lib.drone.action;

import com.o3dr.services.android.lib.util.Utils;

/**
 * Created by Fredia Huya-Kouadio on 9/7/15.
 */
public class ControlActions {

    private static final String PACKAGE_NAME = "com.o3dr.services.android.lib.drone.action.control";

    public static final String ACTION_DO_GUIDED_TAKEOFF = Utils.PACKAGE_NAME + ".action.DO_GUIDED_TAKEOFF";
    public static final String EXTRA_ALTITUDE = "extra_altitude";

    public static final String ACTION_SEND_GUIDED_POINT = Utils.PACKAGE_NAME + ".action.SEND_GUIDED_POINT";
    public static final String ACTION_SEND_GUIDED_VELOCITY_LOCAL  = Utils.PACKAGE_NAME + ".action.SEND_GUIDED_VELOCITY_LOCAL";
    public static final String ACTION_SEND_GUIDED_VELOCITY_GLOBAL = Utils.PACKAGE_NAME + ".action.SEND_GUIDED_VELOCITY_GLOBAL";
    public static final String EXTRA_GUIDED_POINT = "extra_guided_point";

    public static final String EXTRA_FORCE_GUIDED_POINT = "extra_force_guided_point";
    public static final String ACTION_SET_GUIDED_ALTITUDE = Utils.PACKAGE_NAME + ".action.SET_GUIDED_ALTITUDE";

    public static final String ACTION_SET_CONDITION_YAW = PACKAGE_NAME + ".SET_CONDITION_YAW";
    public static final String EXTRA_YAW_TARGET_ANGLE = "extra_yaw_target_angle";
    public static final String EXTRA_YAW_CHANGE_RATE = "extra_yaw_change_rate";
    public static final String EXTRA_YAW_IS_RELATIVE = "extra_yaw_is_relative";

    public static final String ACTION_SET_VELOCITY = PACKAGE_NAME + ".SET_VELOCITY";

    public static final String ACTION_SEND_BRAKE_VEHICLE = PACKAGE_NAME + ".action.SEND_BRAKE_VEHICLE";

    /**
     * X X-axis, normalized to the range [-1000,1000]. A value of INT16_MAX indicates that this axis is invalid. Generally corresponds to forward(1000)-backward(-1000) movement on a joystick and the pitch of a vehicle.
     */
    public static final String EXTRA_AXIS_X                 = "extra_axis_x";

    /**
     * Y Y-axis, normalized to the range [-1000,1000]. A value of INT16_MAX indicates that this axis is invalid. Generally corresponds to left(-1000)-right(1000) movement on a joystick and the roll of a vehicle.
     */
    public static final String EXTRA_AXIS_Y                 = "extra_axis_y";

    /**
     * Z Z-axis, normalized to the range [-1000,1000]. A value of INT16_MAX indicates that this axis is invalid. Generally corresponds to a separate slider movement with maximum being 1000 and minimum being -1000 on a joystick and the thrust of a vehicle. Positive values are positive thrust, negative values are negative thrust.
     */
    public static final String EXTRA_AXIS_Z                 = "extra_axis_z";

    /**
     * R R-axis, normalized to the range [-1000,1000]. A value of INT16_MAX indicates that this axis is invalid. Generally corresponds to a twisting of the joystick, with counter-clockwise being 1000 and clockwise being -1000, and the yaw of a vehicle.
     */
    public static final String EXTRA_AXIS_R                 = "extra_axis_r";

    /**
     * A bitfield corresponding to the joystick buttons' current state, 1 for pressed, 0 for released. The lowest bit corresponds to Button 1.
     */
    public static final String EXTRA_BUTTONS                = "extra_buttons";


    /**
     * X velocity in meters per second.
     */
    public static final String EXTRA_VELOCITY_X             = "extra_velocity_x";

    /**
     * Y velocity in meters per second.
     */
    public static final String EXTRA_VELOCITY_Y             = "extra_velocity_y";

    /**
     * Z velocity in meters per second.
     */
    public static final String EXTRA_VELOCITY_Z             = "extra_velocity_z";
    public static final String EXTRA_RELATIVE_FRAME         = "extra_relative_frame";
    public static final String EXTRA_TYPE_MASK              = "extra_type_mask";

    public static final String ACTION_ENABLE_MANUAL_CONTROL = PACKAGE_NAME + ".ENABLE_MANUAL_CONTROL";
    public static final String ACTION_MANUAL_CONTROL        = PACKAGE_NAME + ".MANUAL_CONTROL";

    public static final String EXTRA_DO_ENABLE              = "extra_do_enable";

    public static final String ACTION_LOOK_AT_TARGET        = PACKAGE_NAME + ".action.LOOK_AT_TARGET";

    /**
     * Geo coordinate to orient the vehicle to
     */
    public static final String EXTRA_LOOK_AT_TARGET         = "extra_look_at_target";


    /*
     *   reset region of interest
     */
    public static final String ACTION_RESET_ROI             = Utils.PACKAGE_NAME + ".action.RESET_ROI";



    //Private to prevent instantiation
    private ControlActions(){}


}
