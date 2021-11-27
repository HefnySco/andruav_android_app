package com.andruav.andruavUnit;

import android.location.Location;
import android.text.Html;

import com.andruav.AndruavDroneFacade;
import com.andruav.AndruavFacade;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.Constants;
import com.andruav.TelemetryProtocol;
import com.andruav.event.droneReport_Event.Event_Emergency_Changed;
import com.andruav.event.droneReport_Event.Event_FCB_Changed;
import com.andruav.event.droneReport_Event.Event_GCSBlockedChanged;
import com.andruav.event.droneReport_Event.Event_GPS_Ready;
import com.andruav.event.droneReport_Event.Event_TargetLocation_Ready;
import com.andruav.event.droneReport_Event.Event_UnitShutDown;
import com.andruav.event.droneReport_Event.Event_Vehicle_ARM_Changed;
import com.andruav.event.droneReport_Event.Event_Vehicle_Flying_Changed;
import com.andruav.event.droneReport_Event.Event_Vehicle_Mode_Changed;
import com.andruav.event.fcb_7adath._7adath_FCB_RemoteControlSettings;
import com.andruav.protocol.commands.textMessages.AndruavMessage_SensorsStatus;
import com.andruav.sensors.AndruavGimbal;
import com.andruav.interfaces.INotification;
import com.andruav.controlBoard.IControlBoard_Callback;
import com.andruav.controlBoard.ControlBoard_Shadow;
import com.andruav.controlBoard.ControlBoardBase;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.controlBoard.shared.common.VehicleTypes;
import com.andruav.controlBoard.shared.missions.MohemmaMapBase;
import com.andruav.notification.PanicFacade;
import com.andruav.protocol.R;
import com.andruav.sensors.AndruavBattery;
import com.andruav.sensors.AndruavIMU;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;
import com.andruav.util.AndruavLatLngAlt;

/**
 * Created by M.Hefny on 14-Feb-15.
 */
public class AndruavUnitBase {


    private final boolean mIsMe;

    /* ActivityMosa3ed Related Attributes **/

    /** MAP **/
    public int homeIconIndex =-1;

    /***
     * when true means main screens displays info about this unit.
     */
    public boolean isGUIActivated = false;


    /***
     * Image every n Seconds
     */
    public int imageInterval = 0;
    public int imageTotal =1;


    public int PreferredRotationAngle =0; // this is used by when displaying Image on GCS from this unit


    /**
     * @link VehicleTypes
     */
    protected int mVehicleType;

    /***
     * enabled when {@link AndruavMessage_SensorsStatus} need to be sent
     */
    public boolean sensorStatusAlertActive = false;


    //////// Attributes
    public enum enum_userStatus {
        ALIVE,          // receive heart beat identification.
        SUSPECTED,      // has not got anything since a while -normally 60 sec-.
        DISCONNECTED    // has not got anything for more than 2 minutes.

    }

    /***
     * No Recording from camera n (each 3 bit is a camera)
     * <br> see {@link #VideoRecording}
     */
    public final static int VIDEORECORDING_OFF= 0;
    /***
     * Recording from camera n (each 3 bit is a camera)
     * <br> see {@link #VideoRecording}
     */
    public final static int VIDEORECORDING_ON = 1;
    /***
     * Recording from external camera-app n (each 3 bit is a camera)
     * <br> see {@link #VideoRecording}
     */
    public final static int VIDEORECORDING_EXTERNAL = 2;


    /***
     * This is set for Drones in GCS ... it means a Drone is sending video to this GCS.
     * if true then there is a request sent to this unit to stream video.
     * <br>Maybe video is not actually sent for any reason e.g. no camera.
     */
    public boolean VideoStreamingActivated = false;
    public String VideoStreamingChannel = "";


    public final static int CONNECTIONSTATE_EXCELLENT   =15000; /// less than 5 sec
    public final static int CONNECTIONSTATE_VERYGOOD    =60000;
    public final static int CONNECTIONSTATE_GOOD        =120000;
    public final static int CONNECTIONSTATE_FAIR        =200000;
    public final static int CONNECTIONSTATE_LOST        =210000;

    public final static int GPS_MODE_AUTO              = 0;
    public final static int GPS_MODE_MOBILE            = 1;
    public final static int GPS_MODE_FCB               = 2;



    public final static int TRIBOOLEAN_UNKNOWN = 999;
    public final static int TRIBOOLEAN_TRUE    = 1;
    public final static int TRIBOOLEAN_FALSE    = 1;

    public final static int SERVO_OUTPUT_NUMBER = 8;

    public String UnitID;
    public String PartyID;
    public String GroupName;
    public String Description;
    public String LANIPAddress;
    public long lastActiveTime;

    public enum_userStatus unitStatus;
    protected boolean isFlying  = false;
    protected boolean isArmed   = false;

    private long mFlyingLastStartTime = 0;
    /***
     * This value is updated when Flying is Over, i.e. it is Zero till landing.
     * If there are multiple takeoffs then this is the result of multiple flyings.
     */
    private long mFlyingTotalDuration = 0;


    protected boolean isEmergencyChangeFlightModeFaileSafe;
    protected boolean isFlashing;
    protected boolean isWhisling;

    /**
     * extra servos not ones used in plan or rover, but extra ones.
     */
    protected int[] servoOutputs = new int[SERVO_OUTPUT_NUMBER];



    public int[] getServoOutputs ()
    {
        return servoOutputs;
    }

    public void setServoOutputs (final int index, final int value)
    {
        servoOutputs[index] = value;
    }

    public long getFlyingStartTime ()
    {
        return mFlyingLastStartTime;
    }

    public long getFlyingTotalDuration()
    {
        return mFlyingTotalDuration;
    }

    public void setFlyingStartTime (final long value)
    {
        mFlyingLastStartTime = value;
    }

    public void setFlyingTotalDuration(final long value)
    {
        mFlyingTotalDuration = value;
    }


    public boolean getIsFlashing ()
    {
        return  isFlashing;
    }

    public void setIsFlashing (final boolean value)
    {
        if (!IsMe() && AndruavSettings.andruavWe7daBase.IsCGS && (isFlashing != value))
        {

            isFlashing = value;

            AndruavEngine.getEventBus().post(new Event_Emergency_Changed(this));

            return;
        }

        if (IsMe() && (isFlashing != value))
        {

            isFlashing = value;

            AndruavFacade.broadcastID();

            return;
        }


    }

    public boolean getIsWhisling()
    {
        return  isWhisling;
    }

    public void setIsWhisling(final boolean value)
    {
        if (!IsMe() && AndruavSettings.andruavWe7daBase.IsCGS && (isWhisling != value))
        {
            isWhisling = value;

            AndruavEngine.getEventBus().post(new Event_Emergency_Changed(this));

            return;
        }

        if (IsMe() && (isWhisling != value))
        {

            isWhisling = value;

            AndruavFacade.broadcastID();

            return;
        }
    }


    public boolean getIsEmergencyChangeFlightModeFaileSafe()
    {
        return isEmergencyChangeFlightModeFaileSafe;
    }

    public void setIsEmergencyChangeFlightModeFailSafe(final boolean value)
    {
        if (!IsMe() && AndruavSettings.andruavWe7daBase.IsCGS && (isEmergencyChangeFlightModeFaileSafe != value))
        {
            isEmergencyChangeFlightModeFaileSafe = value;

            AndruavEngine.getEventBus().post(new Event_Emergency_Changed(this));

            return;
        }

        if (IsMe() && (isEmergencyChangeFlightModeFaileSafe != value))
        {

            isEmergencyChangeFlightModeFaileSafe = value;

            AndruavFacade.broadcastID();

            return;
        }
    }


    protected int GPSMode = GPS_MODE_AUTO;

    public int getGPSMode() {
        return GPSMode;
    }

    public void setGPSMode (int gpsMode)
    {
        final boolean changed = (GPSMode != gpsMode);

        GPSMode = gpsMode;

        if (changed) {
            AndruavFacade.broadcastID();

            if (IsMe()) {
                AndruavEngine.getEventBus().post(new Event_GPS_Ready(this)); // inform all that a data is ready
                PanicFacade.gpsModeChanged(this);
            }
        }
    }


    protected String  permissions;
    protected static String  readOnlyGCS    = "uided";
    protected static String  doAllGCS       = "D1G2T3R4V5C6";
    protected static String  rootPermssion  = "D1G0T3R4V5C6";
    protected boolean canControl            = false;
    protected boolean canTelemetry          = false;
    protected boolean canVideo              = false;
    protected boolean canVideoTracking      = false;
    protected boolean canImage              = false;
    protected boolean canBeDrone            = false;
    protected boolean canBeGCS              = false;
    protected boolean isRootAccessCode      = false;

    public boolean canControl ()
    {
        return  canControl;
    }

    public boolean canTelemetry ()
    {
        return  canTelemetry;
    }

    public boolean canVideo ()
    {
        return  canVideo;
    }
    public boolean canVideoTracking ()
    {
        return  canVideoTracking;
    }

    public boolean canImage ()
    {
        return  canImage;
    }

    public boolean canBeDrone ()
    {
        return  canBeDrone;
    }

    public boolean canBeGCS ()
    {
        return  canBeGCS;
    }


    public void setLocalPermissions ()
    {
        this.permissions = rootPermssion;
        parsePermissions(permissions);
    }

    public void setPermissions (final String permissions)
    {
        this.permissions = permissions;
        parsePermissions(permissions);
    }


    public String getPermissions ()
    {
        return this.permissions;
    }

    /***
     *
     * @param permissions
     */
    private void parsePermissions (final String permissions)
    {
        if (permissions == null) return;
        final int len = permissions.length();
        if (len>0) this.canBeDrone              = permissions.charAt(0) == 'D';
        if (len>2) this.canBeGCS                = permissions.charAt(2) == 'G';
        if (len>4) this.canTelemetry            = permissions.charAt(4) == 'T';
        if (len>6) this.canControl              = permissions.charAt(6) == 'R';
        if (len>8) this.canVideo                = permissions.charAt(8) == 'V';
        if (len>10) this.canImage               = permissions.charAt(10) == 'C';

        if (len>3)
        {
            isRootAccessCode = this.canBeGCS && permissions.charAt(3) == '0';
        }

        if (this.canVideo)
        {
            int videoPermissions = permissions.charAt(9);
            if (len>8) this.canVideoTracking        = (videoPermissions & 70) == 70;
        }
    }



    /***
     * Please refer to TelephonyManager.NETWORK_TYPE
     */
    protected int signalType; // GSM of other drone
    /***
     * in dbm
     * -113 is OFF , above 90 is OK
     */
    protected int signalLevel; // GSM of other drone

    public int getSignalType ()
    {
        return signalType;
    }

    public void setSignal (final int type, final int dbm)
    {
        final boolean changed = (Math.abs(signalLevel - dbm) > 5) || (type != signalType);
        signalType = type;
        signalLevel = dbm;
        if (changed && IsMe())
        {
            AndruavDroneFacade.sendCommSignalStatus(null, false);
        }

    }
    public int getSignalLevel ()
    {
        return signalLevel;
    }



    /***
     * Remote Control "Return To Center" for all sticks
     * <br> if null then RTC info is not available and remote control for this unit cannot be safely used.
     */
    protected boolean[] mRTC;

    /***
     * true if this is a Ground Control Station
     */
    protected boolean IsCGS;

    public boolean getIsCGS()
    {
        return  IsCGS;
    }


    /***
     * <b>ONLY MEANINGFULE IF RCEngagged = TRUE</b>
     * RC_SUB_ACTION_JOYSTICK_CHANNELS = 0
     * RC_SUB_ACTION_CENTER_CHANNELS   = 1
     * RC_SUB_ACTION_FREEZE_CHANNELS   = 2
     */
    protected int mManualTXBlockedSubAction = 0;



    /***
     * Video recording status:
     * This is a bit wise operator that each bit represent a camera
     * <br> 0 for no recording
     * <br> 1 for recording
     * @Deprecated Now there is a flag in camera list use it instead.
     */
    @Deprecated
    public int VideoRecording;
    /***
     *  a shutdown signal has been sent from this Unit.
     */
    public boolean getIsShutdown ()
    {
        return IsShutdown;
    }

    public void setShutdown (final boolean shutdown)
    {
        final boolean bchanged = IsShutdown != shutdown;


        if (bchanged)
        {

            IsShutdown = shutdown;


            if (IsShutdown) {
                unitStatus = AndruavUnitBase.enum_userStatus.DISCONNECTED;
                AndruavEngine.notification().displayNotification(INotification.NOTIFICATION_TYPE_WARNING, "Andruav", UnitID + AndruavEngine.AppContext.getString(R.string.andruav_noti_disconnect), true, INotification.INFO_TYPE_PROTOCOL, false);
            } else {
                unitStatus = AndruavUnitBase.enum_userStatus.ALIVE; // it is OK
            }



            if (IsMe()) {
                AndruavFacade.sendShutDown(null);
            }
            else
            {
                if ((AndruavSettings.andruavWe7daBase.IsCGS) && (!shutdown))
                { // I am a GCS [The APP] then I want to announce the user that this instance Unit has reconnected.
                   AndruavEngine.notification().displayNotification(INotification.NOTIFICATION_TYPE_WARNING, Html.fromHtml("Andruav"), Html.fromHtml(UnitID + AndruavEngine.AppContext.getString(R.string.andruav_noti_reconnect)), true, INotification.INFO_TYPE_PROTOCOL, false);
                }
            }

            AndruavEngine.getEventBus().post(new Event_UnitShutDown(this));
        }

    }
    private boolean IsShutdown = false;

    /***
     * IMU Read from FCB
     */
    protected boolean useFCBIMU = false;
    public int telemetry_protocol = TelemetryProtocol.TelemetryProtocol_No_Telemetry;

    /***
     * Only Drones has FCBoard active
     * Also Drone andruavUnits registered in AndruavGCS has attached FCBoard to it.
     * So GCBoard can be connected physically to a board, or represents a remote Drone.
     */
    public ControlBoardBase FCBoard;
    public ControlBoard_Shadow FCBoardShadow;


/////////////////////////// HOME & TARGET LOCATION

    /***
     * This is home location of Drone. The object is instantiated once and is updated.
     */
    final private AndruavLatLngAlt gpsHomeLocation     = new AndruavLatLngAlt(-1.0,-1.0,-1.0);
    /***
     * This location is defined when Drone flights in Guided mode. The object is instantiated once and is updated.
     */
    final private AndruavLatLngAlt gpsTargetLocation   = new AndruavLatLngAlt(-1.0,-1.0,-1.0);



    public AndruavLatLngAlt getGpsHomeLocation() {
        return gpsHomeLocation;
    }

    public AndruavLatLngAlt getGpsTargetLocation() {
        return gpsTargetLocation;
    }

    /***
     * This value is updated from the board not from {@link ControlBoardBase#do_Mode(int, IControlBoard_Callback)}}
     */
    protected int flightMode = FlightMode.CONST_FLIGHT_CONTROL_UNKNOWN;

    protected boolean rcChannelBlock;


    protected MohemmaMapBase mMohemmaMapBase = new MohemmaMapBase();



    protected boolean mHasGimbal;
    public void hasGimbal(final boolean enable)
    {
        final boolean changed = mHasGimbal != enable;
        mHasGimbal = enable;
        if (changed)
        {
            if (mHasGimbal)
            {
               // mAndruavGimbal = new AndruavGimbal();
            }
            else
            {
              //  mAndruavGimbal = null;
            }

        }

    }

    public boolean hasGimbal ()
    {
        return mHasGimbal;
    }

    protected   AndruavGimbal    mAndruavGimbal = new AndruavGimbal();

    public AndruavGimbal getAndruavGimbal()
    {
        return  mAndruavGimbal;
    }

    public  AndruavIMU       LastEvent_IMU;      // IMU from Andruav
    public  AndruavIMU       LastEvent_FCB_IMU;  // IMU from FCB
    public  AndruavBattery   LastEvent_Battery;  // Battery from Andruav




    /////////// EOF Attributes

    /***
     *
     * @param type
     */
    public void setVehicleType (int type)
    {

        mVehicleType = type;
    }

    /***
     * if IsGCS = true then the return is always {@link VehicleTypes#VEHICLE_GCS} however {@link #mVehicleType may has different value.}
     * @return
     */
    public int getVehicleType ()
    {

        if (this.IsCGS)
        {
            return VehicleTypes.VEHICLE_GCS;
        }


        return mVehicleType;
    }




    public AndruavUnitBase()
    {
        this.mIsMe =  false;
        LastEvent_Battery   = new AndruavBattery();
        LastEvent_IMU       = new AndruavIMU(false,true,false); // u cannot use this.IsMe() as it is not AndruavSettings.andruavWe7daBase is NULL in APP when application first started
        LastEvent_FCB_IMU   = new AndruavIMU(false,true,true);
    }


    public AndruavUnitBase(final boolean mIsMe, final boolean isGCS)
    {
        this.mIsMe = mIsMe;
        this.IsCGS = isGCS;
        LastEvent_Battery   = new AndruavBattery();
        LastEvent_IMU       = new AndruavIMU(mIsMe,true,false);
        LastEvent_FCB_IMU   = new AndruavIMU(mIsMe,true,true);
    }

    public AndruavUnitBase(final String groupName, final String partyID, final boolean isGCS)
    {
        this();
        PartyID = partyID;
        GroupName = groupName;
        lastActiveTime = System.currentTimeMillis();
        this.IsCGS = isGCS;
    }


    public boolean Equals (final AndruavUnitBase andruavUnitBase)
    {
        if (andruavUnitBase == null) return false;

        return this.PartyID.equals(andruavUnitBase.PartyID);

    }
    /***
     * is me means that I am the actual unit in Andruav and not representing a remote drone of GCS
     * @return
     */
    public boolean IsMe ()
    {

        return mIsMe;
        //return PartyID.equals(AndruavSettings.andruavWe7daBase.PartyID);
    }


    /***
     * Changing in this property leads to change in {@link AndruavUnitBase#telemetry_protocol}.
     * Event {@link Event_FCB_Changed} is sent from {@link AndruavUnitBase#telemetry_protocol}.
     * @param enable
     */
    public void useFCBIMU (boolean enable)
    {

        final boolean changed = (useFCBIMU != enable);
        useFCBIMU = enable;
        if (changed)
        {

            if (IsMe() ) {
                // sendMessageToModule Toggle Event
                AndruavFacade.broadcastID();
            }

            // IMPORTANT: ***_7adath_FCB_Changed is triggered but elsewhere to maintain data consistency.***
       }
    }

    public boolean useFCBIMU ()
    {
        return (useFCBIMU); //  && ((FCBoard!= null) || (FCBoardShadow!= null)));
    }


    public boolean IsFlying()
    {
        return isFlying;
    }

    /***
     * Is the vehicle flying now or not.
     * @param flying
     */
    public void IsFlying(boolean flying)
    {

        final boolean changed = (isFlying != flying);

        isFlying = flying;

        if (changed)
        {
            if(IsMe())
            {
                if (isFlying==true)
                {
                    mFlyingLastStartTime = System.currentTimeMillis();
                }
                else
                {
                    mFlyingTotalDuration = mFlyingTotalDuration + ( System.currentTimeMillis() - mFlyingLastStartTime);
                    mFlyingLastStartTime =0;
                }

                // sendMessageToModule Toggle Event
                // AndruavMo7arek.log().log(AndruavSettings.AccessCode, "apm", "f ");
                AndruavFacade.broadcastID();
            }

            AndruavEngine.getEventBus().post(new Event_Vehicle_Flying_Changed(this));

        }

    }


    /***
     * Vehicle Modes [Auto, RTL, Hold, ...etc.]
     * @param flightMode
     */
    public void setFlightModeFromBoard (final int flightMode)
    {
        final boolean changed = (this.flightMode != flightMode);

        this.flightMode = flightMode;

        if (changed) {
            if (IsMe())
            {
                AndruavFacade.broadcastID();
            }
            AndruavEngine.getEventBus().post(new Event_Vehicle_Mode_Changed(this));

        }


    }


    public int getFlightModeFromBoard ()
    {
        return this.flightMode;
    }

    /***
     * This function is called in the Drone & can be called in Drone_Shadow.
     *
     * @return
     */
    public boolean getisGCSBlockedFromBoard()
    {
        if (this.IsCGS) return  false;



        return rcChannelBlock;
    }

    /***
     * This CANNOT be called in a Drone ONLY in Drone_Shadow class.
     * @return
     */
    public void setisGCSBlockedFromBoard(final boolean blocked)
    {
        /*
        if (this.IsMe()) try {
            throw new Exception("cannot set GCS blocked externally- it should be by remote");
        } catch (Exception e) {
            AndruavMo7arek.log().logException("AW", e);
        }*/
        if (rcChannelBlock != blocked)
        {
            rcChannelBlock = blocked;

            if (IsMe())
            {
                AndruavFacade.broadcastID();
            }
            AndruavEngine.getEventBus().post(new Event_GCSBlockedChanged(this));
        }


    }


    public boolean IsArmed()
    {
        return isArmed;
    }

    public void IsArmed(boolean armed) {

        final boolean changed = (isArmed != armed);
        isArmed = armed; // register value first  [solve bug sendMessageToModule old status]

        if (changed) {
            isArmed= armed;

            if (IsMe()) {
                // sendMessageToModule Toggle Event

                AndruavFacade.broadcastID();
            }
            AndruavEngine.getEventBus().post(new Event_Vehicle_ARM_Changed(this));
        }


    }

    /***
     * get active IMU either Andruav or FCB
     * @return
     */
    public AndruavIMU  getActiveIMU ()
    {
        // GCS has only its own IMU
        if (IsCGS)
        {
            return LastEvent_IMU;
        }

        // Drone can return either FCBoard or IMU

        // Return FCB if requested and available.
        if (useFCBIMU ())
        {
            return  LastEvent_FCB_IMU;
        }

        // Return IMU otherwise
        return LastEvent_IMU;
    }


    public AndruavIMU  getMobileGPS ()
    {
        return LastEvent_IMU;
    }

    public AndruavIMU  getActiveGPS ()
    {
        // GCS has only its own IMU
        if (IsCGS)
        {
            return LastEvent_IMU;
        }

        // Drone can return either FCBoard or IMU

        // Return FCB if requested and available.
        switch (GPSMode)
        {
            case GPS_MODE_AUTO:
                if (useFCBIMU ())
                {
                    if (LastEvent_FCB_IMU.hasCurrentLocation()) {
                        return LastEvent_FCB_IMU;
                    }
                    else
                    {
                        return LastEvent_IMU;
                    }
                }
                break;
            case GPS_MODE_MOBILE:
                return LastEvent_IMU;
           case GPS_MODE_FCB:
                return  LastEvent_FCB_IMU;
        }

        return LastEvent_IMU;
    }

    /***
     * THIS IS A DO COMMAND TO A DRONE
     * This is a recieved waypoints to be implemented by me the DRONE
     * @param mohemmaMapBase
     */
    public void updateExternalWayPoints(final MohemmaMapBase mohemmaMapBase)
    {
        if (!IsMe()) return ;       // This is a DO Command
        if (IsCGS) return ;         // I should be a drone


        //

        if (FCBoard != null) {
            // if you have FCB then update it in FCB then read it from FCB and update the unit
            // so that you are certain it has been successfully stored in FCB.
            // you may choose to store it in a temp location to keep track of it and retry when failure.
            FCBoard.do_SendMission(mohemmaMapBase);
        }
        else
        {
            // if you dont have FCB then update waypoint in unit directly.
            // however this could be useless... UNLESS you resave them when FCB is available.
            refreshWayPoints (mohemmaMapBase);
        }
    }


    public void refreshWayPoints (final MohemmaMapBase mohemmaMapBase)
    {
        mMohemmaMapBase.clear();

        if (mohemmaMapBase == null) return ;

        final int size = mohemmaMapBase.size();
        //BUG: concurrency issue can happen here betweem MapActivity and this function.
        for (int i=0; i<size;++i)
        {
            //andruavWe7da.getMohemmaMapBase().remove(mohemmaMapBase.keyAt(i));
            mMohemmaMapBase.put(mohemmaMapBase.keyAt(i), mohemmaMapBase.valueAt(i));
        }
    }



    public void doClearMission () {

        if (!IsMe()) return ; // THIS IS AN ERROR CALL

        mMohemmaMapBase.clear();
        if (FCBoard != null) {
            FCBoard.do_ClearMission();
        }
    }



    public void doPutMissionintoFCB(final String missionText)
    {
        if (!IsMe()) return ; // THIS IS AN ERROR CALL

        if (FCBoard != null) {
            FCBoard.doPutMissionintoFCB(missionText);
        }
    }

    public void doSetCurrentMission(final int missionItemNumber)
    {
        if (!IsMe()) return ; // THIS IS AN ERROR CALL

        if (FCBoard != null) {
            FCBoard.do_SetCurrentMission(missionItemNumber);
        }
    }

    /***
     * Reloads mission from FCB -if exist- otherwise it returns available waypoint stored in {@link #mMohemmaMapBase}
     */
    public void doReloadMissionfromFCB () {

        if (!IsMe()) return ; // THIS IS AN ERROR CALL

        if (FCBoard != null) {
            FCBoard.do_ReadMission();
        }
    }

    public void clearHomeLocation ()
    {
        do_UpdateExternalHomeLocation(-1.0f,-1.0f,0.0f);
    }


    /***
     * THIS IS **NOT** A DO COMMAND TO A DRONE
     * This command is called by Drone itself, or as a Drone instance running in GCS.
     * @param longitude
     * @param latitude
     * @param altitude
     */
    public void updateHomeLocation(final double longitude, final double latitude, final double altitude) {

        this.getGpsHomeLocation().update(longitude, latitude, altitude);
    }


    /***
     * THIS IS **NOT** A DO COMMAND TO A DRONE
     * This command is called by Drone itself, or as a Drone instance running in GCS.
     * @param longitude
     * @param latitude
     * @param altitude
     */
    public void internal_updateTargetLocation(final double longitude, final double latitude, final double altitude) {

        this.getGpsTargetLocation().update(longitude, latitude, altitude);
    }

    /***
    * THIS IS A DO COMMAND TO A DRONE
    * This function reads updates from outside.
    * It executes a command from another Drone or GCS.
    * @param longitude
    * @param latitude
    * @param altitude
    */
    public void do_UpdateExternalHomeLocation(final double longitude, final double latitude, final double altitude)
    {
        AndruavIMU andruavIMU;
       this.getGpsHomeLocation().update(longitude, latitude, altitude);
        if (FCBoard != null)
        {
            if (longitude==-1.0)
            {
                FCBoard.do_ClearHomeLocation();
            }
            else {
                FCBoard.do_SetHomeLocation(longitude, latitude, altitude);
            }
        }

    }


    /***
     * THIS IS A DO COMMAND TO A DRONE
     * This function reads updates from outside.
     * It executes a command from another Drone or GCS.
     * @param speed navigation speed
     */
    public void do_SetNavigationSpeed(final double speed, final boolean isGroundSpeed, final double throttle, final boolean isRelative)
    {
        if (!IsMe()) return ; // THIS IS AN ERROR CALL

        if (speed <= -1) return ;
        if (FCBoard != null)
        {
           FCBoard.do_SetNavigationSpeed(speed,isGroundSpeed,throttle,isRelative);
        }
    }


    public  boolean hasHomeLocation ()
    {
        final AndruavLatLngAlt andruavLatLngAlt = this.getGpsHomeLocation();
        return  ((andruavLatLngAlt.getLatitude()!=-1.0f) && (andruavLatLngAlt.getLongitude()!=-1.0f));

    }


    /***
     * Target GPS Location used for Guided Navigation
     * @return
     */
    public  boolean hasTargetLocation ()
    {
        final AndruavLatLngAlt andruavLatLngAlt = this.getGpsTargetLocation();
        return  ((andruavLatLngAlt.getLatitude()!=-1.0f) && (andruavLatLngAlt.getLongitude()!=-1.0f));

    }




    /***
     * if useFCBIMU:
     * if GPS not in FCB board, we use Andruav GPS
     * @return
     */
    public Location getAvailableLocation () {
        if (IsCGS) {
            return LastEvent_IMU.getCurrentLocation();
        }


        final AndruavIMU andruavIMH = getActiveGPS();

        return andruavIMH.getCurrentLocation();

    }



    public MohemmaMapBase getMohemmaMapBase()
    {
        return mMohemmaMapBase;
    }


    protected void Telemetry_protocol_changed (int telemetry_protocol)
    {

    }

    public synchronized void setTelemetry_protocol(final int _telemetry_protocol)
    {
        final boolean changed = (this.telemetry_protocol != _telemetry_protocol);

        if (changed)
        {
            this.telemetry_protocol = _telemetry_protocol;

            if ((_telemetry_protocol != TelemetryProtocol.TelemetryProtocol_No_Telemetry)
                &&(_telemetry_protocol != TelemetryProtocol.TelemetryProtocol_Unknown_Telemetry)) {
                AndruavEngine.log().log(AndruavEngine.getPreference().getLoginUserName(), "prot_det", String.valueOf(_telemetry_protocol));
            }
            else
            {
                // No protocol here so turn off reading IMU from FCB
                useFCBIMU (false);
            }

            Telemetry_protocol_changed (_telemetry_protocol);

            // should be last after every other settings so event read correct status of AndruavWe7da not intermidiate one.
            AndruavEngine.getEventBus().post(new Event_FCB_Changed(this));
        }
    }


    /***
     * Dispose FCBoard either Shadow or real.
     */
    protected void disposeFCBBase()
    {
        if (this.mIsMe==true) {
            if (this.FCBoard != null) {
                FCBoard.ActivateListener(false);
                FCBoard = null;
            }
        }
        else {
            if (this.FCBoardShadow != null) {
                FCBoardShadow.ActivateListener(false);
                FCBoardShadow = null;
            }
        }
    }

    /***
     * A measure of last update time.
     * @return
     */
    public int getTelemetry_protocol()
    {
        return this.telemetry_protocol;
    }

     public int getConnectionState ()
    {
        long diff = System.currentTimeMillis() - lastActiveTime;

        if (IsMe()) return 4;

        if (diff < CONNECTIONSTATE_EXCELLENT)
        {
            return 4;
        }
        else
        if (diff < CONNECTIONSTATE_VERYGOOD)
        {
            return 3;
        }
        else
        if (diff < CONNECTIONSTATE_GOOD)
        {
            return 2;
        }
        else
        if (diff < CONNECTIONSTATE_FAIR)
        {
            return 1;
        }

        return 0;
    }


    /***
     * Get Channels of Remote Control
     * returns {@link #mRTC}
     *
     * @return array of booleans. each unit represents RTC of a channel
     */
    public boolean[] getRTC()
    {
        return  mRTC;
    }

    /***
     *
     * @param rtc : array of booleans. each unit represents RTC of a channel
     *            <br>set to null to make it unavailable info
     */
    public void setRTC (boolean[] rtc)
    {
        mRTC = rtc;
    }

    public boolean isManualTXBlocked()
    {
        return (getManualTXBlockedSubAction() != _7adath_FCB_RemoteControlSettings.RC_SUB_ACTION_RELEASED);
    }

    /*
    public void isManualTXBlocked(final boolean manualTXBlocked)
    {
        final boolean changed = (mManualTXBlocked != manualTXBlocked);
        mManualTXBlocked = manualTXBlocked;

        if (IsMe()) {

            if ((changed) && (manualTXBlocked == false)) {
                PanicFacade.cannotDoAutopilotAction(INotification.NOTIFICATION_TYPE_WARNING, AndruavResala_Error.ERROR_RCControl, AndruavMo7arek.AppContext.getString(R.string.gen_rc_releazed), null);
            }
        }

    }*/

    /**
     * TX Status
     *  {@link _7adath_FCB_RemoteControlSettings#RC_SUB_ACTION_CENTER_CHANNELS} : 1500 channels values are sent. TX is no longer effective.
     *  {@link _7adath_FCB_RemoteControlSettings#RC_SUB_ACTION_FREEZE_CHANNELS} : last TX readings are freezed and sent as fixed values. TX is no longer effective.
     *  {@link _7adath_FCB_RemoteControlSettings#RC_SUB_ACTION_JOYSTICK_CHANNELS_GUIDED} : RCChannels is being sent to Drone. TX  is no longer effective for some channels.
     *  {@link _7adath_FCB_RemoteControlSettings#RC_SUB_ACTION_JOYSTICK_CHANNELS} : RCChannels is being sent to Drone. TX  is no longer effective for some channels.
     *  {@link _7adath_FCB_RemoteControlSettings#RC_SUB_ACTION_RELEASED} : there is no RCChannel info sent to Drone.
     *
     * @return
     */
    public int getManualTXBlockedSubAction()
    {
        return mManualTXBlockedSubAction;
    }

    public void setManualTXBlockedSubAction(final int manualTXBlockedSubAction)
    {
        if (IsMe()) {
            switch (mManualTXBlockedSubAction) {

                /*
                Add rules here to govern switching modes criteria.
                for now switching between all modes are allowed.
                 */
                default:
                    break;
            }
        }
        final boolean changed = (mManualTXBlockedSubAction != manualTXBlockedSubAction);
        if (!changed) return ;

        mManualTXBlockedSubAction = manualTXBlockedSubAction;



        if (changed)
        {
            switch (mManualTXBlockedSubAction)
            {
                case _7adath_FCB_RemoteControlSettings.RC_SUB_ACTION_CENTER_CHANNELS:
                    if (IsMe()) PanicFacade.cannotDoAutopilotAction(INotification.NOTIFICATION_TYPE_WARNING, AndruavMessage_Error.ERROR_RCControl, AndruavEngine.AppContext.getString(R.string.gen_rc_centered),null);

                    break;

                case _7adath_FCB_RemoteControlSettings.RC_SUB_ACTION_FREEZE_CHANNELS:
                    if (IsMe()) PanicFacade.cannotDoAutopilotAction(INotification.NOTIFICATION_TYPE_WARNING, AndruavMessage_Error.ERROR_RCControl, AndruavEngine.AppContext.getString(R.string.gen_rc_freezed),null);

                    break;

                case _7adath_FCB_RemoteControlSettings.RC_SUB_ACTION_RELEASED:
                    if (IsMe()) PanicFacade.cannotDoAutopilotAction(INotification.NOTIFICATION_TYPE_WARNING, AndruavMessage_Error.ERROR_RCControl, AndruavEngine.AppContext.getString(R.string.gen_rc_releazed),null);

                    break;

                case _7adath_FCB_RemoteControlSettings.RC_SUB_ACTION_JOYSTICK_CHANNELS_GUIDED:
                case _7adath_FCB_RemoteControlSettings.RC_SUB_ACTION_JOYSTICK_CHANNELS:
                    if (IsMe()) PanicFacade.cannotDoAutopilotAction(INotification.NOTIFICATION_TYPE_WARNING, AndruavMessage_Error.ERROR_RCControl, AndruavEngine.AppContext.getString(R.string.gen_rc_joystick),null);

                    break;
            }

            // Inform other parties.
            //TODO: can you filter this to GCS only ?
            AndruavFacade.sendID(Constants._gcs_);

        }
    }

    // updated by telemetry data
    // In GCS connected to Drone:
    //  if this is a Remote Drone class thenThis data overrides data comes from AndruavResala_IMU
    //  this data will be called in remote drone class IFF there is a telemetry data using GCS-SW
    public void updateFromFCBAttitude()
    {
        if (FCBoard == null) return ;

        LastEvent_FCB_IMU.P = (float)this.FCBoard.getPitch();
        LastEvent_FCB_IMU.R = (float)this.FCBoard.getRoll();
        LastEvent_FCB_IMU.Y = (float)this.FCBoard.getYAW();
    }


    /***
     * <br>This function is called in Drone instance where there is a FCB
     */
    public void updateFromFCBPower()
    {
        if (FCBoard == null) return ;

        LastEvent_Battery.FCB_BatteryRemaining  = this.FCBoard.getBatteryRemaining();
        LastEvent_Battery.FCB_BatteryVoltage    = this.FCBoard.getPowerBatteryVoltage();
        LastEvent_Battery.FCB_CurrentConsumed   = this.FCBoard.getBatteryCurrent();
    }

    /***
     * <br>This function is called in Drone instance where there is a FCB
     */
    public void updateFCBHomeLocation ()
    {
        if (!IsMe())
        {
            return;
        }
        this.getGpsHomeLocation().update(this.FCBoard.getHome_gps_lng(), this.FCBoard.getHome_gps_lat(), this.FCBoard.getHome_gps_alt());
    }

    /***
     * This function is called from Drone itself. It updates and broadcast target guided point
     */
    public void updateFCBTargetLocation ()
    {
        if (!this.IsMe())
        {
            return;
        }
        this.getGpsTargetLocation().update(this.FCBoard.getTarget_gps_lng(), this.FCBoard.getTarget_gps_lat(), this.FCBoard.getTarget_gps_alt());

        AndruavEngine.getEventBus().post(new Event_TargetLocation_Ready(this)); // ToDo: this should be an internal trigger in Andruav Protocol Lib

    }

    /***
     * Reads NavPitch, NavRoll, WaypointDistance, TargetBearing & Altitude Error from FCB.
     * <br>This function is called in Drone instance where there is a FCB
     */
    public void updateFCBNavInfo ()
    {
        if (!this.IsMe()) return;

        LastEvent_FCB_IMU.nav_Pitch             = this.FCBoard.getNavPitch();
        LastEvent_FCB_IMU.nav_Roll              = this.FCBoard.getNavRoll();
        LastEvent_FCB_IMU.nav_WayPointDistance  = this.FCBoard.getNavWayPointDistance();
        LastEvent_FCB_IMU.nav_AltitudeError     = this.FCBoard.getNavAltError();
        LastEvent_FCB_IMU.nav_TargetBearing     = this.FCBoard.getNavTargetBearing();
    }

    /***
     * Reads GPS Status and location from FCB.
     * <br>This function is called in Drone instance where there is a FCB
     */
    public void updateFromFCBGPS()
    {
        if (!this.IsMe()) return;

        LastEvent_FCB_IMU.GPS3DFix          = this.FCBoard.getGPSfixType();
        LastEvent_FCB_IMU.SATC              = this.FCBoard.getGPSSatCount();
        //LastEvent_FCB_IMU.GroundAltitude  = this.FCBoard.getGPSAlt();
        final Location loc = new Location("GPS");
        loc.setLongitude(this.FCBoard.getGPSLongitude());
        loc.setLatitude(this.FCBoard.getGPSLatitude());
        loc.setAltitude(this.FCBoard.getGPSAlt());
        loc.setSpeed((float)this.FCBoard.getGPSGroundSpeed());
        loc.setBearing((float)this.FCBoard.getNavBearing());
        LastEvent_FCB_IMU.setCurrentLocation(loc);

    }


    /***
     * This drone is controllable if it is not GCS
     * and has FCB connected with a controllable protocol.
     * @return
     */
    public boolean isControllable ()
    {
        boolean res = false;

        if ((IsCGS)
                || getIsShutdown()
                || getisGCSBlockedFromBoard()
                || (!useFCBIMU()))
        {
            return  false;
        }

        switch (getTelemetry_protocol()) {
            case TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry:
                res = true;
                break;
        }


        return res;
    }


    /***
     * means that it is a unit that should be able to take images from ..
     * however it could have no camera BTW. this is a guess used by a GCS.
     * @return
     */
    public boolean isCameraEnabled ()
    {
        return (!this.IsCGS) && (!getIsShutdown());
    }

    public String getFCBDesctipion ()
    {
        if (this.FCBoard==null)
        {
            return AndruavEngine.AppContext.getString(R.string.andruav_no_fcb);
        }
        else
        {
            return FCBoard.getFCBDescription();
        }
    }

    public void setUnitStatus (enum_userStatus status)
    {

        switch (status)
        {
            case ALIVE:
                break;
        }

        this.unitStatus = status;
    }


    protected void onReconnect ()
    {

    }


}
