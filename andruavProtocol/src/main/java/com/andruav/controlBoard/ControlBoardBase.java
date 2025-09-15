package com.andruav.controlBoard;

import com.andruav.AndruavEngine;
import com.andruav.event.fcb_event.Event_FCB_RemoteControlSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.Constants;
import com.andruav.EmergencyBase;
import com.andruav.notification.PanicFacade;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.controlBoard.shared.missions.MohemmaMapBase;
import com.andruav.protocol.R;


/**
 * Represents A Generic <b>Flight Control Board</b>. Status in this board are Andruav Status, and inherited classes have to map their specific status to this Shared Generic Representation.
 * <br>
 * <a href="http://diydrones.com/profiles/blogs/andruav-towards-a-shared-data-model">see this topic [Andruav - Towards a Shared Data Model]</a>
 * <br><br>Created by M.Hefny on 04-Apr-15.
 */
public class ControlBoardBase {

    public static final int CONST_CHANNEL_1_ROLL = 0;
    public static final int CONST_CHANNEL_2_PITCH = 1;
    public static final int CONST_CHANNEL_3_THROTTLE = 2;
    public static final int CONST_CHANNEL_4_YAW = 3;
    public static final int CONST_CHANNEL_5_AUX1 = 4;
    public static final int CONST_CHANNEL_6_AUX2 = 5;
    public static final int CONST_CHANNEL_7_AUX3 = 6;
    public static final int CONST_CHANNEL_8_AUX4 = 7;

    protected final Object  telemetryBytesObject = new Object();

    protected final AndruavUnitBase mAndruavUnitBase;


    protected boolean isRCFailsafe = false;


    protected int hasGyro   = Constants.UNKNOWN;
    protected int hasAcc    = Constants.UNKNOWN;
    protected int hasBaro   = Constants.UNKNOWN;
    protected int hasMag    = Constants.UNKNOWN;
    protected int hasGPS    = Constants.UNKNOWN;
    protected int hasSonar  = Constants.UNKNOWN;

    protected int hasGyroEnabled    = Constants.UNKNOWN;
    protected int hasAccEnabled     = Constants.UNKNOWN;
    protected int hasBaroEnabled    = Constants.UNKNOWN;
    protected int hasMagEnabled     = Constants.UNKNOWN;
    protected int hasGPSEnabled     = Constants.UNKNOWN;
    protected int hasSonarEnabled   = Constants.UNKNOWN;


    protected int hasGyroHealthy    = Constants.UNKNOWN;
    protected int hasAccHealthy     = Constants.UNKNOWN;
    protected int hasBaroHealthy    = Constants.UNKNOWN;
    protected int hasMagHealthy     = Constants.UNKNOWN;
    protected int hasGPSHealthy     = Constants.UNKNOWN;
    protected int hasSonarHealthy   = Constants.UNKNOWN;





   protected boolean rcChannelBlock = false;
   public boolean do_RCChannelBlocked()
    {
        return  rcChannelBlock;
    }



    public void do_RCChannelBlocked(final boolean block)
    {
        if ((rcChannelBlock != block))
        {   // a change happend
            if (block)
            {   // Software Remote from Andruav GCS and GamePads are released.
                sendRCChannels(Event_FCB_RemoteControlSettings.RC_SUB_ACTION_RELEASED, null, false);
                sendRCChannels(Event_FCB_RemoteControlSettings.RC_SUB_ACTION_RELEASED, null, false);
                sendRCChannels(Event_FCB_RemoteControlSettings.RC_SUB_ACTION_RELEASED, null, false);
                AndruavEngine.notification().Speak("Blocked");
            }
            else
            {
                AndruavEngine.notification().Speak("Unblocked");
            }

        }
        rcChannelBlock = block;
        this.mAndruavUnitBase.setisGCSBlockedFromBoard(block);
    }


    // ATTITUDE
    /***
     * (rad, -pi..+pi)
     */
    protected double PitchAngle;
    protected double PitchPerUnit;


    /***
     * (rad, -pi..+pi)
     */
    protected double RollAngle;
    protected double RollPerUnit;
    /***
     * (rad, -pi..+pi)
     */
    protected float Heading;
    protected float HeadingPerUnit;
    protected float HeadingOffset;


    protected double yaw;
    protected double YawPerUnit;

    /***
     *  in cm
     */
    protected double Altitude;
    protected final double AltitudePerUnit=1.0;

    /***
     * in cm / s
     */
    protected int Vario;
    protected int VarioPerUnit;



    protected short     gps_fixType;            // 0-1: no fix, 2: 2D fix, 3: 3D fix, 4: DGPS, 5: RTK. Some applications will not use the value of this field unless it is at least two, so always correctly fill in the fix.
    protected double    vehicle_gps_lng;
    protected double    vehicle_gps_lat;
    protected double    vehicle_gps_alt;          // (RELATIVE TO GROUND) Altitude (AMSL, NOT WGS84), in meters * 1000 (positive for up). Note that virtually all GPS modules provide the AMSL maxAltitude in addition to the WGS84 maxAltitude.
    protected double    vehicle_gps_abs;

    protected double    gps_groundspeed;       // GPS ground speed (m/s). If unknown, set to: UINT16_MAX
    protected int       gps_satCount;
    protected double    airspeed;
    protected double    verticalspeed;



    protected double    gps_lnglat_scale = 1.0;  // to avoid divide by zero
    protected double    gps_alt_scale = 1.0;  // to avoid divide by zero
    protected final double    gps_groundspeed_scale=1.0;       // GPS ground speed (m/s). If unknown, set to: UINT16_MAX
    protected double    airspeed_scale=1.0;       // GPS ground speed (m/s). If unknown, set to: UINT16_MAX
    protected double    verticalspeed_scale=1.0;       // GPS ground speed (m/s). If unknown, set to: UINT16_MAX


    /////////////////////////// HOME LOCATION
    /**
     * GPS Longitude <b>HOME</b> location
     */

    protected double    home_gps_lng;
    public double getHome_gps_lng () {
        return home_gps_lng;
    }
    /**
     * GPS Latitude <b>HOME</b> location
     */
    protected double    home_gps_lat;
    public double getHome_gps_lat () {
        return home_gps_lat;
    }
    /**
     * GPS Latitude <b>HOME</b> location
     */
    protected double    home_gps_alt;
    public double getHome_gps_alt () {
        return home_gps_alt;
    }

    // BEGIN LEADER SWARM VARIABLES

    protected boolean followMeOn = false;
    protected double leader_drone_pos_time      = 0;
    protected double leader_drone_velocity      = 0;
    protected double leader_drone_lng_old       = Constants.INVALID_GPS_LOCATION;
    protected double leader_drone_lat_old       = Constants.INVALID_GPS_LOCATION;
    protected double leader_drone_lng           = Constants.INVALID_GPS_LOCATION;
    protected double leader_drone_lat           = Constants.INVALID_GPS_LOCATION;
    protected double leader_drone_alt           = Constants.INVALID_GPS_LOCATION;
    protected double leader_dist;
    protected double leader_bearing;


    // END

    protected double target_gps_lng;

    public double getTarget_gps_lng() {
        return target_gps_lng;
    }

    protected double target_gps_lat;

    public double getTarget_gps_lat() {
        return target_gps_lat;
    }

    protected double target_gps_alt;

    public double getTarget_gps_alt() {
        return target_gps_alt;
    }
    ////////////////////////////////////////////////////////////////////////////////////////


    // Sensors Status

    public int getHasGyro() {
        return hasGyro;
    }

    public void setHasGyro(int hasGyro) {
        if (hasGyro != this.hasGyro)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasGyro = hasGyro;
    }

    public int getHasAcc() {
        return hasAcc;
    }

    public void setHasAcc(int hasAcc) {
        if (hasGyro != this.hasGyro)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasAcc = hasAcc;
    }

    public int getHasBaro() {
        return hasBaro;
    }

    public void setHasBaro(int hasBaro) {
        if (hasGyro != this.hasGyro)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasBaro = hasBaro;
    }

    public int getHasMag() {
        return hasMag;
    }

    public void setHasMag(int hasMag) {
        if (hasGyro != this.hasGyro)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasMag = hasMag;
    }

    public int getHasGPS() {
        return hasGPS;
    }

    public void setHasGPS(int hasGPS) {
        if (hasGyro != this.hasGyro)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasGPS = hasGPS;
    }

    public int getHasSonar() {
        return hasSonar;
    }

    public void setHasSonar(int hasSonar) {
        if (hasGyro != this.hasGyro)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasSonar = hasSonar;
    }

    public int getHasGyroEnabled() {
        return hasGyroEnabled;
    }

    public void setHasGyroEnabled(int hasGyroEnabled) {
        if (hasGyroEnabled != this.hasGyroEnabled)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasGyroEnabled = hasGyroEnabled;
    }

    public int getHasAccEnabled() {
        return hasAccEnabled;
    }

    public void setHasAccEnabled(int hasAccEnabled) {
        if (hasAccEnabled != this.hasAccEnabled)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasAccEnabled = hasAccEnabled;
    }

    public int getHasMagEnabled() {
        return hasMagEnabled;
    }

    public void setHasMagEnabled(int hasMagEnabled) {
        if (hasMagEnabled != this.hasMagEnabled)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasMagEnabled = hasMagEnabled;
    }

    public int getHasBaroEnabled() {
        return hasBaroEnabled;
    }

    public void setHasBaroEnabled(int hasBaroEnabled) {
        if (hasBaroEnabled != this.hasBaroEnabled)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasBaroEnabled = hasBaroEnabled;
    }

    public int getHasGPSEnabled() {
        return hasGPSEnabled;
    }

    public void setHasGPSEnabled(int hasGPSEnabled) {
        if (hasGPSEnabled != this.hasGPSEnabled)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasGPSEnabled = hasGPSEnabled;
    }

    public int getHasSonarEnabled() {
        return hasSonarEnabled;
    }

    public void setHasSonarEnabled(int hasSonarEnabled) {
        if (hasSonarEnabled != this.hasSonarEnabled)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasSonarEnabled = hasSonarEnabled;
    }

    public int getHasGyroHealthy() {
        return hasGyroHealthy;
    }

    public void setHasGyroHealthy(int hasGyroHealthy) {
        if (hasGyroHealthy != this.hasGyroHealthy)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasGyroHealthy = hasGyroHealthy;
    }

    public int getHasAccHealthy() {
        return hasAccHealthy;
    }

    public void setHasAccHealthy(int hasAccHealthy) {
        if (hasAccHealthy != this.hasAccHealthy)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasAccHealthy = hasAccHealthy;
    }

    public int getHasMagHealthy() {
        return hasMagHealthy;
    }

    public void setHasMagHealthy(int hasMagHealthy) {
        if (hasGyro != this.hasGyro)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasMagHealthy = hasMagHealthy;
    }

    public int getHasBaroHealthy() {
        return hasBaroHealthy;
    }

    public void setHasBaroHealthy(int hasBaroHealthy) {
        if (hasBaroHealthy != this.hasBaroHealthy)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasBaroHealthy = hasBaroHealthy;
    }

    public int getHasGPSHealthy() {
        return hasGPSHealthy;
    }

    public void setHasGPSHealthy(int hasGPSHealthy) {
        if (hasGPSHealthy != this.hasGPSHealthy)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasGPSHealthy = hasGPSHealthy;
    }

    public int getHasSonarHealthy() {
        return hasSonarHealthy;
    }

    public void setHasSonarHealthy(int hasSonarHealthy) {
        if (hasSonarHealthy != this.hasSonarHealthy)
        {
            mAndruavUnitBase.sensorStatusAlertActive = true;
        }
        this.hasSonarHealthy = hasSonarHealthy;
    }





    /***
     * Power Variable
     * <br>10*milliamperes (1 = 10 milliampere), -1: autopilot does not measure the current
     */
    protected double pow_battery_voltage ;

    /***
     * Power Variable
     * <br>Consumed charge, in 1 milliampere hours 1 mAh, -1: autopilot does not provide mAh consumption estimate
     */
    protected double pow_battery_current;

    /***
     * Power Variable
     * <br>Remaining battery energy: (0%: 0, 100%: 100), -1: autopilot does not estimate the remaining battery
     */
    private double  pow_battery_remaining;




    /***
     * Current desired roll in degrees
     */
    protected double nav_roll;
    public double getNavRoll () { return nav_roll * RollPerUnit;}

    /***
     * Current desired pitch in degrees
     */
    protected double nav_pitch;
    public double getNavPitch () { return nav_pitch * PitchPerUnit;}

    /***
     * Current desired heading in degrees
     */
    protected double    nav_bearing;
    public double getNavBearing () { return nav_bearing * YawPerUnit;}

    /***
     * Bearing to current MISSION/target in degrees
     */
    protected double    target_bearing;
    public double getNavTargetBearing () { return target_bearing * YawPerUnit;}

    /***
     * Distance to active MISSION in meters
     */
    protected double    wp_dist_old;
    protected double    wp_dist;

    public double getNavWayPointDistance () { return wp_dist;}

    /***
     * Current altitude error in meters
     */
    protected double    alt_error;
    public double getNavAltError () { return alt_error / gps_alt_scale;}



    public double getPowerBatteryVoltage () {
        return pow_battery_voltage;
    }

    public double getBatteryCurrent() {
        return pow_battery_current;
    }

    public double getBatteryRemaining () {
        return pow_battery_remaining;
    }

    public void setBatteryRemaining (final double remaining) {
        pow_battery_remaining = remaining;

        final int minBattery = AndruavEngine.getPreference().getBattery_min_value();
        if ((minBattery > 0) && (remaining < minBattery))
        {
            final EmergencyBase emergencyBase = AndruavEngine.getEmergency();
            if (emergencyBase != null)
            {
                emergencyBase.triggerBatteryEmergency(true);


            }
        }
        else
        {
            final EmergencyBase emergencyBase = AndruavEngine.getEmergency();
            if (emergencyBase != null) {
                emergencyBase.triggerBatteryEmergency(false);
            }
        }
    }
    public double getPitch ()
    {
        return PitchAngle * PitchPerUnit;
    }

    public double getRoll ()
    {
        return RollAngle * RollPerUnit;
    }

    public float getHeading ()
    {
        return HeadingOffset + (Heading * HeadingPerUnit);
    }

    public double getYAW ()
    {
        return yaw * YawPerUnit;
    }
    /***
     * 0-1: no fix, 2: 2D fix, 3: 3D fix, 4: DGPS, 5: RTK. Some applications will not use the value of this field unless it is at least two, so always correctly fill in the fix.
     * @return
     */
    public short getGPSfixType ()
    {
        return gps_fixType;
    }


    /***
     * Longitude (WGS84), in degrees * 1E7
     * @return
     */
    public double   getGPSLongitude()
    {
        return vehicle_gps_lng / gps_lnglat_scale;
    }

    /***
     * Latitude (WGS84), in degrees * 1E7
     * @return
     */
    public double   getGPSLatitude()
    {
        return vehicle_gps_lat / gps_lnglat_scale;
    }

    /***
     * Altitude (AMSL, NOT WGS84), in meters * 1000 (positive for up). Note that virtually all GPS modules provide the AMSL maxAltitude in addition to the WGS84 maxAltitude.
     * @return
     */
    public double   getGPSAltRelative()
    {
        return vehicle_gps_alt / gps_alt_scale;
    }

    public double   getGPSAltAbs()
    {
        return vehicle_gps_abs / gps_alt_scale;
    }

    public double   getGPSGroundSpeed()
    {
        return gps_groundspeed / gps_groundspeed_scale;
    }

    /***
     *
     * @return
     */
    public int   getGPSSatCount ()
    {
        return gps_satCount;
    }





    // single exception handler
    protected int bexceptionexecute = 5;


    /***
     * This is advanced feature aims to avoid unnecessary traffic between Drone & CGS in case Telemetry is enabled.
     */
    protected boolean mOptimizeTraffic = false;


    protected boolean isArmed = false;
    protected boolean isFlying = false;

    /***
     * Used to enable event listeners for some instances.
     * Mainly in version 1.0.40 -first time to be used- :
     * it is used to activate the owner -myself- instance.
     * @param bActivate
     */
    public void ActivateListener (boolean bActivate)
    {

    }


    /***
     * Returns a text description for the board
     * @return
     */
    public String getFCBDescription()
    {
        return "";
    }

    public ControlBoardBase(AndruavUnitBase andruavUnitBase)
    {

        mAndruavUnitBase = andruavUnitBase;

    }


    /**
     * Deattach this class from AndruavWe7da and disconnect it from FCB.
     */
    public void Release ()
    {

    }



    public void setOptimizeTraffic (boolean enable)
    {
        mOptimizeTraffic = enable;
    }

    public boolean getOptimizeTraffic ()
    {
        return mOptimizeTraffic;
    }



    /***
     * Define a fly control mode
     * @param flightControl {@link FlightMode}
     */
    public void setFlightControl (int flightControl, final IControlBoard_Callback lo7Ta7akom_callback)
    {
    }


    public boolean isArmed ()
    {
      return false;
    }





    /***
     * Switching to different flying modes
     * Please note that if MW or other Drones does not support some of these modes you can either override
     * this function or for example use do_FBWA to call do_Manual internally from the MW class.
     *
     * @param flightMode Generic Flying Mode Parameter {@link FlightMode}
     */
    public void do_Mode(final int flightMode, final IControlBoard_Callback iControlBoard_callback)
    {
        switch (flightMode)
        {
            case FlightMode.CONST_FLIGHT_CONTROL_RTL:
                do_RTL(iControlBoard_callback);
                break;
            case FlightMode.CONST_FLIGHT_CONTROL_SMART_RTL:
                do_Smart_RTL(iControlBoard_callback);
                break;
            case FlightMode.CONST_FLIGHT_CONTROL_ALT_HOLD:
                do_ALT_Hold(iControlBoard_callback);
                break;
            case FlightMode.CONST_FLIGHT_CONTROL_FOLLOW_ME:
            case FlightMode.CONST_FLIGHT_CONTROL_GUIDED:
                do_Guided(iControlBoard_callback);
                break;

            //case FlightMode.CONST_FLIGHT_CONTROL_CIRCLE:
            //    do_Circle(null);
            //    break;
            case FlightMode.CONST_FLIGHT_CONTROL_LOITER:
                do_Loiter(iControlBoard_callback);
                break;
            case FlightMode.CONST_FLIGHT_CONTROL_AUTO:
                do_Auto(iControlBoard_callback);
                break;
            case FlightMode.CONST_FLIGHT_CONTROL_FBWA:
                do_FBWA(iControlBoard_callback);
                break;
            case FlightMode.CONST_FLIGHT_CONTROL_FBWB:
                do_FBWB(iControlBoard_callback);
                break;
            case FlightMode.CONST_FLIGHT_CONTROL_CRUISE:
                do_Cruise(iControlBoard_callback);
                break;
            case FlightMode.CONST_FLIGHT_CONTROL_MANUAL:
                do_Manual(iControlBoard_callback);
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_ACRO:
                do_Acro(iControlBoard_callback);
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_STABILIZE:
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_BRAKE:
                do_Brake(iControlBoard_callback);
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_POSTION_HOLD:
                do_POS_Hold(iControlBoard_callback);
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_HOLD:
                do_Brake(iControlBoard_callback);
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_TAKEOFF:
                do_TakeOff(iControlBoard_callback);
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_SURFACE:
                do_Surface(iControlBoard_callback);
                break;

        }
    }



    public void do_ARM (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_arm));
    }

    public void do_Disarm (boolean emergencyDisarm,final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_disarm));
    }


    public void do_ChangeAltitude (final double altitude,final IControlBoard_Callback ILo7Ta7Akom__callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_takeoff));
    }


    public void do_TakeOff (final IControlBoard_Callback ILo7Ta7Akom__callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_takeoff));
    }



    public void do_FBWA (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_fbwa));
    }

    public void do_FBWB (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_fbwa));
    }


    /**
     * HOLD, Break is the same ... review Mapping Logic
     * @param lo7Ta7akom_callback
     */
    public void do_Brake(final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_brake));
    }


    public void do_Hold(final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_hold));
    }


    public void do_Cruise (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_cruise));
    }



    public void do_Auto (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_auto));
    }


    public void do_Loiter (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_loiter));
    }

    public void do_RTL (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_rtl));
    }

    public void do_FollowMe (final double slave_drone_lng,final double slave_drone_lat, final double slave_drone_alt,
                             final double leader_drone_lng, final double leader_drone_lat, final double leader_drone_alt, final double leader_linear_speed)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_followme));
    }

    public void do_Smart_RTL(final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_smart_rtl));
    }

    public void do_FollowMe ()
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_followme));
    }

    public void do_Guided (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_guided));
    }

    public void do_Manual (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_manual));
    }

    public void do_Acro (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_acro));
    }

    public void do_POS_Hold (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_pos_hold));
    }


    public void do_ALT_Hold (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_alt_hold));
    }



    public void do_CircleHere  (final double lng, final double lat, final double altitude, final double radius, final int turns, final IControlBoard_Callback lo7Ta7akom_callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_circle));
    }

    public void do_FlytoHere (final double lng, final double lat, final double alt,double xVel, double yVel, double zVel, final double yaw, final double yaw_rate)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_guided));
    }


    public void do_Land (final IControlBoard_Callback ILo7Ta7Akom__callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_land));
    }

    public void do_Surface (final IControlBoard_Callback ILo7Ta7Akom__callback)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_surface));
    }

    public void do_Yaw (final double targetAngle, final double turnRate, final boolean isClockwise, final boolean isRelative)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_yaw));
    }




    public boolean is_RTL() throws Exception {
        throw new Exception("Not Implemented Function");
    }

    public boolean is_MANUAL () throws Exception {
        throw new Exception("Not Implemented Function");
    }

    public boolean is_GUIDED() throws Exception {
        throw new Exception("Not Implemented Function");
    }


    public void do_SetCurrentMission (final int missionItemNumber)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_mission));
    }


    public void do_ClearHomeLocation()
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_mission));

    }


    public void do_ClearMission()
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_mission));

    }

    public void do_SetHomeLocation(final double longitude, final double latitude, final double altitude)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_mission));

    }


    public  void do_SendMission(MohemmaMapBase mohemmaMapBase){
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_mission));

    }

    public void doPutMissionintoFCB (final String missionText)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_mission_write));

    }

    public void do_ReadMission()
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_mission));

    }

    /***
     *
     * Only Speed is used when calling 3DR .. relative will be used in Andruav layer.
     * @param speed -1 means no change .... unit is m/s
     * @param isGroundSpeed used only in fixed wings
     * @param throttle used in fixed wings and rover - ignored in quadcopter
     * @param isRelative
     */
    public void do_SetNavigationSpeed (double speed, boolean isGroundSpeed, double throttle, boolean isRelative)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do));
    }

    public void do_TriggerCamera ()
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_triggercamera));
    }


    /***
     *
     * @param pitch in degrees  or lat, depending on mount mode
     * @param roll  in degrees  or lon depending on mount mode
     * @param yaw   in degrees  or alt (in cm) depending on mount mode
     * @param isAbsolute
     */
    public void do_GimbalCtrl (double pitch, double roll, double yaw, boolean isAbsolute)
    {
        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_gimbal));
    }

    public void do_GimbalCtrlByGPS (double lng, double lat, double alt)
    {
       PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_gimbal));
    }

    public void do_GimbalConfig(final boolean stabilizePitch, final boolean stabilizeRoll, final boolean stabilizeYaw, int GimbalMode) {

        PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_autopilot_cannot_do_gimbal));
    }


    public  void sendRCChannels (int subAction, int[] channels, final boolean allEightChannels)
    {

    }

    public void do_InjectGPS_NMEA (final String nmea)
    {

    }

    public void do_InjectGPS (final long timeStampe, final long timeWeekMS, final int timeWeek
            , final short fixType, final int lat, final int lng, final int alt
            , final int satellites_visible, final float hdop, final float vdop
            , final float speedAccuracy, final float horizontalAccuracy, final float verticalAccuracy, final int gpsNum)
    {

    }

}
