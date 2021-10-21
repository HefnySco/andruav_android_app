package com.andruav;

/**
 * Created by M.Hefny on 21-Aug-15.
 */
public final class Constants {

    // Units
    public static final int  Preferred_UNIT_AUTO =1;
    public static final int  Preferred_UNIT_METRIC_SYSTEM =2;
    public static final int  Preferred_UNIT_IMPERIAL_SYSTEM =3;

    // TRI STATE BUTTON
    public static final int YES = 1;
    public static final int NO  = 0;
    public static final int UNKNOWN  = 999;


    // RC Settings
    public static final int Default_RC_MAX_VALUE = 2000;
    public static final int Default_RC_MIN_VALUE = 1000;
    public static final int Default_RC_MID_VALUE = 1500;
    public static final int Default_RC_MID_LVALUE = 1450;
    public static final int Default_RC_MID_HVALUE = 1550;
    public static final int Default_RC_DR_VALUE = 100;
    public static final int Default_RC_RANGE = Default_RC_MAX_VALUE - Default_RC_MIN_VALUE;
    public static final int Default_RC_RANGE_2 = (Default_RC_MAX_VALUE - Default_RC_MIN_VALUE) / 2;


    public static final int CONST_REMOTECONTROL_RATE = 400;

    public static final int CONST_TRACKING_INDEX_X_P = 0;
    public static final int CONST_TRACKING_INDEX_X_I = 1;
    public static final int CONST_TRACKING_INDEX_X_D = 2;
    public static final int CONST_TRACKING_INDEX_Y_P = 3;
    public static final int CONST_TRACKING_INDEX_Y_I = 4;
    public static final int CONST_TRACKING_INDEX_Y_D = 5;

    public final static int SMART_TELEMETRY_LEVEL_NEGLECT = -1; // use Drone stored value
    public final static int SMART_TELEMETRY_LEVEL_0 = 0; // no smart telemetry
    public final static int SMART_TELEMETRY_LEVEL_1 = 1; // no smart telemetry
    public final static int SMART_TELEMETRY_LEVEL_2 = 2; // no smart telemetry
    public final static int SMART_TELEMETRY_LEVEL_3 = 3; // no smart telemetry



    public static final CharSequence[] baudRateItems = {"921.6Kb", "576Kb/s", "500Kb/s", "460.8Kb/s", "230.4Kb/s", "115200", "57600", "38400", "19200", "14400" , "9600", "4800"};
    public static final int[] baudRateItemsValue = {921600, 576000, 500000, 460800, 230400, 115200, 57600, 38400, 19200, 14400 , 9600, 4800};

    // target system
    public final static String _nezam_ = "_SYS_";
    // target all GCS
    public final static String _gcs_ = "_GCS_";
    // target all vehicles.
    public final static String _vehicle = "_AGN_";
    // target all gcs and drones.
    public final static String _all_ = "_GD_";

    public static double INVALID_GPS_LOCATION = 0.0;
}
