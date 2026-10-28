package ap.andruav_ap.communication.controlBoard;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;
import com.andruav.AndruavSettings;
import com.andruav.event.droneReport_Event.Event_SERVO_Outputs_Ready;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.droneReport_Event.Event_HomeLocation_Ready;
import com.andruav.event.droneReport_Event.Event_NAV_INFO_Ready;
import com.andruav.controlBoard.ControlBoardBase;


import com.andruav.event.droneReport_Event.Event_Battery_Ready;
import com.andruav.event.droneReport_Event.Event_GPS_Ready;
import com.andruav.event.droneReport_Event.Event_IMU_Ready;
import com.andruav.util.CustomCircularBuffer;

/**
 * Created by mhefny on 1/20/16.
 */
public class ControlBoard_MavlinkBase extends ControlBoardBase {

    final protected Event_HomeLocation_Ready a7adath_homeLocation_ready = new Event_HomeLocation_Ready(AndruavSettings.andruavWe7daBase);
    final protected Event_GPS_Ready a7adath_gps_ready = new Event_GPS_Ready(AndruavSettings.andruavWe7daBase);
    final protected Event_IMU_Ready a7adath_imu_ready = new Event_IMU_Ready(AndruavSettings.andruavWe7daBase);
    final protected Event_Battery_Ready a7adath_battery_ready = new Event_Battery_Ready(AndruavSettings.andruavWe7daBase);
    final protected Event_NAV_INFO_Ready a7adath_nav_info_ready = new Event_NAV_INFO_Ready(AndruavSettings.andruavWe7daBase);
    final protected Event_SERVO_Outputs_Ready a7adath_servo_output_ready = new Event_SERVO_Outputs_Ready(AndruavSettings.andruavWe7daBase);



    // Taked from MavlinkCommands 3DR Lib
    protected static final int EMERGENCY_DISARM_MAGIC_NUMBER = 21196;
//
//    protected static final int MAVLINK_SET_POS_TYPE_MASK_POS_IGNORE         = ((1 << 0) | (1 << 1) | (1 << 2));
//    protected static final int MAVLINK_SET_POS_TYPE_MASK_VEL_IGNORE         = ((1 << 3) | (1 << 4) | (1 << 5));
//    protected static final int MAVLINK_SET_POS_TYPE_MASK_ACC_IGNORE         = ((1 << 6) | (1 << 7) | (1 << 8));
//    protected static final int  MAVLINK_SET_POS_TYPE_MASK_FORCE             = (1<<9);
//    protected static final int  MAVLINK_SET_POS_TYPE_MASK_YAW_IGNORE        = (1<<10);
//    protected static final int  MAVLINK_SET_POS_TYPE_MASK_YAW_RATE_IGNORE   = (1<<11);

    protected static final int  MAV_FRAME_BODY_NED = 8;
    protected static final int  MAV_FRAME_BODY_OFFSET_NED = 9;


    /***
     * Holds parameter values of APM
     */
   // final protected msg_param_value[] parameters = new msg_param_value[1500];


    protected  boolean canFly = false;

    // https://pixhawk.ethz.ch/mavlink/

    // Flying Modes for Plane
    /*
    enum FlightMode {
    MANUAL        = 0,
    CIRCLE        = 1,
    STABILIZE     = 2,
    TRAINING      = 3,
    ACRO          = 4,
    FLY_BY_WIRE_A = 5,
    FLY_BY_WIRE_B = 6,
    CRUISE        = 7,
    AUTOTUNE      = 8,
    AUTO          = 10,
    RTL           = 11,
    LOITER        = 12,
    GUIDED        = 15,
    INITIALISING  = 16
    };
    //Flying Modes for Quad
    // Auto Pilot modes
    // ----------------
    #define STABILIZE 0                     // hold level position
    #define ACRO 1                          // rate control
    #define ALT_HOLD 2                      // AUTO control
    #define AUTO 3                          // AUTO control
    #define GUIDED 4                        // AUTO control
    #define LOITER 5                        // Hold a single mLocation
    #define RTL 6                           // AUTO control
    #define CIRCLE 7                        // AUTO control
    #define LAND 9                          // AUTO control
    #define OF_LOITER 10                    // Hold a single mLocation using optical flow sensor
    #define DRIFT 11                        // DRIFT mode (Note: 12 is no longer used)
    #define SPORT 13                        // earth frame rate control
    #define FLIP        14                  // flip the vehicle on the roll axis
    #define AUTOTUNE    15                  // autotune the vehicle's roll and pitch gains
    #define POSHOLD     16                  // position hold with manual override
    #define NUM_MODES   17

     */
    protected Parser parserDrone;

    protected final short mSystemId=0;
    protected final short mComponentId=0;

    //protected float pitch;
    protected float pitchspeed;
    //protected float roll;
    protected float rollspeed;

    protected float yawspeed;
    protected long time_boot_ms;


    protected final CustomCircularBuffer <MAVLinkPacket> commandQueue ;

    public ControlBoard_MavlinkBase(AndruavUnitBase andruavUnitBase) {
        super(andruavUnitBase);
        commandQueue = new CustomCircularBuffer(5);
        gps_alt_scale = 1.0f;
    }






}
