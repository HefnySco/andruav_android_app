package com.andruav.controlBoard.shared.common;

/**
 * Created by M.Hefny on 13-May-15.
 */
public class FlightMode {

    // Flight Control Modes
    public static final int CONST_FLIGHT_CONTROL_RTL                =2;  // check array resource in case you want to change this number
    // Flight Follow Me
    public static final int CONST_FLIGHT_CONTROL_FOLLOW_ME          =3;
            // param1:  Longitude:  Master position
            // param2:  Latitude
            // param3(opt):  Radius:             minimum approach to master.
            // param4(opt):  Offset_Longitude:   used to make a fleet formation
            // param5(opt):  Offset_Latitude:    used to make a fleet formation


    // Flight Follow A Unit... The message is sent to the Master Unit
    // and forwarded to the secondary one as a Follow_ME command.
    public static final int CONST_FLIGHT_CONTROL_FOLLOW_UNITED       =4;
            // UNIT_ID: unit to follow.
            // Below commands are forwarded to Slave
            // param1:  Longitude:  Master position
            // param2:  Latitude
            // param3(opt):  Radius:             minimum approach to master.
            // param4(opt):  Offset_Longitude:   used to make a fleet formation
            // param5(opt):  Offset_Latitude:    used to make a fleet formation

    // Flight Auto [Mission UAV]
    public static final int CONST_FLIGHT_CONTROL_AUTO               =5;


    public static final int CONST_FLIGHT_CONTROL_STABILIZE          =6;

    public static final int CONST_FLIGHT_CONTROL_ALT_HOLD           =7;

    // Manual
    public static final int CONST_FLIGHT_CONTROL_MANUAL             =8;


    public static final int CONST_FLIGHT_CONTROL_GUIDED            = 9;

    public static final int CONST_FLIGHT_CONTROL_LOITER            = 10; // check array resource in case you want to change this number


    public static final int CONST_FLIGHT_CONTROL_POSTION_HOLD      = 11;

    public static final int CONST_FLIGHT_CONTROL_LAND              = 12;

    public static final int CONST_FLIGHT_CONTROL_CIRCLE            = 13;

    /***
     * {@http http://ardupilot.org/plane/docs/fbwa-mode.html}
     */
    public static final int CONST_FLIGHT_CONTROL_FBWA              = 14;
    public static final int CONST_FLIGHT_CONTROL_FBWB              = 16;

    /***
     * http://ardupilot.org/plane/docs/cruise-mode.html
     *
     */
    public static final int CONST_FLIGHT_CONTROL_CRUISE            = 15;


    public static final int CONST_FLIGHT_CONTROL_BRAKE             = 17;

    public static final int CONST_FLIGHT_CONTROL_SMART_RTL         = 21;

    public static final int CONST_FLIGHT_CONTROL_TAKEOFF           = 22;  // check array resource in case you want to change this number

    public static final int CONST_FLIGHT_CONTROL_ACRO              = 28;

    public static final int CONST_FLIGHT_CONTROL_INITIALIZING      = 99;  // check array resource in case you want to change this number

    // remapped for ROVER
    public static final int CONST_FLIGHT_CONTROL_HOLD              = 100;

    // remapped for SUBMARINE
    public static final int CONST_FLIGHT_CONTROL_SURFACE           = 101;

    public static final int CONST_FLIGHT_MOTOR_DETECT               = 102;

    public static final int CONST_FLIGHT_CONTROL_UNKNOWN           = 999;


    /***
     * Return text name for Andruav flight modes.
     * @param flightMode
     * @return flight mode text
     */
    public static String getFlightModeText (final int flightMode)
    {
        switch (flightMode)
        {
            case CONST_FLIGHT_CONTROL_SMART_RTL:
                return "Smart RTL";

                case CONST_FLIGHT_CONTROL_RTL:
                return "RTL";

            case CONST_FLIGHT_CONTROL_FOLLOW_ME:
                return "F-Me";

            case CONST_FLIGHT_CONTROL_AUTO:
                return "Auto";

            case CONST_FLIGHT_CONTROL_STABILIZE:
                return "Stable";

            case CONST_FLIGHT_CONTROL_ALT_HOLD:
                return "ATL-H";

            case CONST_FLIGHT_CONTROL_MANUAL:
                return "Manual";

            case CONST_FLIGHT_CONTROL_ACRO:
                return "Acro";

            case CONST_FLIGHT_CONTROL_GUIDED:
                return "Guided";

            case CONST_FLIGHT_CONTROL_LOITER:
                return "Loiter";

            case CONST_FLIGHT_CONTROL_POSTION_HOLD:
                return "Hold";

            case CONST_FLIGHT_CONTROL_LAND:
                return "Land";

            case CONST_FLIGHT_CONTROL_CIRCLE:
                return "Circle";

            case CONST_FLIGHT_CONTROL_BRAKE:
                return "Brake";

            case CONST_FLIGHT_CONTROL_HOLD:
                return "Hold";

            case CONST_FLIGHT_CONTROL_UNKNOWN:
                return "Unknown";

            case CONST_FLIGHT_CONTROL_FBWA:
                return "FBWA";

            case CONST_FLIGHT_CONTROL_FBWB:
                return "FBWB";

            case CONST_FLIGHT_CONTROL_TAKEOFF:
                return "Take-Off";

            case CONST_FLIGHT_CONTROL_CRUISE:
                return "Cruise";

            case CONST_FLIGHT_CONTROL_SURFACE:
                return "Surface";

            case CONST_FLIGHT_MOTOR_DETECT:
                return "Motor Detect";

            case CONST_FLIGHT_CONTROL_INITIALIZING:
                return "Initializing";


        }

        return "Unknown";
    }
}
