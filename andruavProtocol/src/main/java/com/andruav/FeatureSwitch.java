package com.andruav;


/**
 * Created by M.Hefny on 19-Jul-15.
 */
public class FeatureSwitch {


    public static final String _PRIVATE_ = "PRIVATE_";


    /***
     * Should be acending valuesAndruavResala_ID
     */
    public static final int[] cameraWidths    = {176,320,640,800,960};


    /***
     * Should be acending values
     */
    public static final int[] cameraHeights   = {144,240,480,480,720};


    /***
     * Dont use IPWebcam or any external CAM
     *
     * Default Value: true  [for PUBLIC] IPWEBCAM
     */
    public static final boolean Disable_Drone_ExternalCam = true;


    /***
     * Enforce conection to be made to Andruav.com
     * Also the authentication logic happens only when connection is not LOCAL
     *
     * Default value: true [for PUBLIC]
     */
    public static final boolean Disable_Local_Server = false;
    public static final boolean Disable_LOG_In_Local_Server = true; // when Local is active i.e. Preference.isLocalServer(null) then no logging


    /***
     * if True then any mobile can be a Drone mode Andruav
     *
     * Default false
     */
    public static final boolean Disable_DroneMode_Check = true;


    /***
     * Logging Path for Andruav
     */




    //////////////////// FlightControlBoards Integration
    public static final boolean Disable_3DRFCBConnections       = false;



    ////////////////////// TESTING ONLY

    public static final boolean IGNORE_NO_INTERNET_CONNECTION = true; // also should be true for local server


    public static final boolean DEBUG_MODE = false;
    public static final boolean DEBUG_LOG_MODE = false; // works only iff [DEBUG_MODE==true]

}
