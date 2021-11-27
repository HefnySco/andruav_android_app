package com.andruav;

import android.location.Location;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.sensors.AndruavIMU;
import com.andruav.controlBoard.shared.geoFence.GeoFenceManager;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMU;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMUStatistics;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_ServoOutput;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraFlash;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraList;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraZoom;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CommSignalsStatus;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GPS;
import com.andruav.protocol.commands.textMessages.AndruavMessage_NAV_INFO;
import com.andruav.protocol.commands.textMessages.AndruavMessage_POW;
import com.andruav.uavos.modules.UAVOSHelper;
import com.andruav.util.GPSHelper;

import org.json.JSONArray;

/**
 * Created by mhefny on 2/23/17.
 */

public class AndruavDroneFacade extends AndruavFacadeBase {



    ///////////////////// CONSTANTS IMPORTANT FOR PERFORMANCE
    private final static  long monTRKEventTimeDuration                  = 600;
    private final static  long monNAVEventTimeDuration                  = 1000;
    private final static  long monIMUEventTimeDuration                  = 600;
    private final static  long monBATTEventTimeDuration                 = 40000;
    private final static  long monGPSventTimeDuration                   = 2000;
    private final static  long monIMUStatisticsTimeDuration             = 20000;
    private final static  long monSignalTimeDuration                    = 10000;

    private static final long monTRKEventTime =0;
    private static  long monNAVEventTime =0;
    private static  long monIMUEventTime =0;
    private static  long monBATTEventTime=0;
    private static  long monIMUStatisticsEventTime=0;
    private static  long monGPSventTime=0;
    private static  long monSignalTime=0;


    private static Location mOldLocation;
    private static Location mLastLocation;


    public static Location getLastKnownLocation()
    {
        return mLastLocation;
    }

    //////////////////////////////////////////////////////////

    /***
     * Checks if GPS information is available and based on criteria it does the following:
     * 1- Sends GPS Information to Server. {@link #sendGPSInfo(AndruavIMU)}
     * 2- Updates GeoFence status. {@link GeoFenceManager#updateGeoFenceHit(AndruavUnitBase)}
     * 3- Sends Swarm direction if any.
     */
    public static void handleGPSInfo()
    {
        final AndruavIMU andruavIMU = AndruavSettings.andruavWe7daBase.getActiveGPS();
        if (andruavIMU == null) return ;

        if (andruavIMU.hasCurrentLocation()) {
            mOldLocation = mLastLocation;
            mLastLocation = andruavIMU.getCurrentLocation(); // save if for emergency recovery
        }

        final long now = System.currentTimeMillis();

        boolean GPSDistanceAlert;
        boolean GPSTrackingDistanceAlert = false;
        if (mOldLocation!= null) {
            final double distance = GPSHelper.calculateDistance(mLastLocation, mOldLocation);
            GPSDistanceAlert = (distance > 25);
            GPSTrackingDistanceAlert = (distance > 5);
        }
        else
        {
            // first time location.
            GPSDistanceAlert = true;
            GPSTrackingDistanceAlert = true;
        }
        // always sendMessageToModule GPS data
        if (GPSDistanceAlert || ((now - monGPSventTime) > monGPSventTimeDuration)) {

            monGPSventTime = now;

            sendGPSInfo (andruavIMU);

            GeoFenceManager.updateGeoFenceHit(AndruavSettings.andruavWe7daBase);

            // This is handled only when changes happens so it need to be handled in the event {@link _7adath_GeoFence_Hit}
            ///////GeoFenceManager.determineFenceValidationAction(AndruavSettings.andruavWe7daBase);

        }

    }

    /***
     * Sends GPS infomration to server.
     * @param andruavIMU
     */
    protected static void sendGPSInfo (final AndruavIMU andruavIMU)
    {
        final AndruavMessage_GPS andruavMessage_gps = new AndruavMessage_GPS();
        andruavMessage_gps.CurrentLocation = AndruavSettings.andruavWe7daBase.getAvailableLocation();
        //andruavMessage_gps.groundAltitude = andruavIMU.GroundAltitude;
        andruavMessage_gps.GPSFCB = andruavIMU.getUseFCBIMU();
        andruavMessage_gps.GPS3DFix = andruavIMU.GPS3DFix;
        andruavMessage_gps.SATC = andruavIMU.SATC;

        sendMessage(andruavMessage_gps, (AndruavUnitBase)null, Boolean.FALSE);
    }

    /***
     * Sends Servi output data. [READ FROM FCB BOARD]
     */
    public static void sendServoOutputInfo () {

        final AndruavResalaBinary_ServoOutput andruavResalaBinary_servoOutput = new AndruavResalaBinary_ServoOutput();

        andruavResalaBinary_servoOutput.ServosOutput = AndruavSettings.andruavWe7daBase.getServoOutputs();

        sendMessage(andruavResalaBinary_servoOutput, null, Boolean.TRUE);

    }


    /***
     * Drone uses this command to sendMessageToModule NAV information. [READ FROM FCB BOARD]
     * @param now {@link #monNAVEventTimeDuration } will be considered.
     */
    public static void sendNAVInfo (final long now)
    {
        if ((now - monNAVEventTime) > monNAVEventTimeDuration) {

            monNAVEventTime = now;

            final AndruavIMU andruavIMU = AndruavSettings.andruavWe7daBase.getActiveIMU();

            final AndruavMessage_NAV_INFO andruavMessage_nav_info = new AndruavMessage_NAV_INFO();
            andruavMessage_nav_info.alt_error=andruavIMU.nav_AltitudeError;
            andruavMessage_nav_info.nav_pitch=andruavIMU.nav_Pitch;
            andruavMessage_nav_info.nav_roll=andruavIMU.nav_Roll;
            andruavMessage_nav_info.nav_yaw = andruavIMU.Y; // YAW is Orientation Variavle
            andruavMessage_nav_info.target_bearing = andruavIMU.nav_TargetBearing;
            andruavMessage_nav_info.wp_dist= andruavIMU.nav_WayPointDistance;

            sendMessage(andruavMessage_nav_info, (AndruavUnitBase)null, Boolean.TRUE);
            //broadcastMessageToGroup(andruavResala_nav_info, Boolean.TRUE);
            /*
            if (size > 1) { //boradcast IMU if two or more listeners
                broadcastMessageToGroup(andruavResala_nav_info, Boolean.TRUE);
            } else {
                AndruavWe7da andruavUnit = (AndruavWe7da) AndruavSettings.mIMURequests.get(0);
                sendMessageToIndividual(event_nav_info_ready, andruavUnit.PartyID, Boolean.FALSE); // time here can be used on the other side ofr calculation.
            }
            */
        }
    }


    /***
     * Drone uses this function to sendMessageToModule IMU data.
     * IMU DATA is not important if you want to stop using mobile sensors. NAV_Data should be enough
     * @param now {@link #monIMUEventTimeDuration } will be considered.
     */
    public static void sendIMUInfo (final long now )
    {
        if ((now - monIMUEventTime) > monIMUEventTimeDuration) {
            int size = AndruavSettings.mIMURequests.size();

            if (size > 0) {
                // TODO: improvment...u can check for dead units or sendMessageToModule individual message if size is one

                monIMUEventTime = now;
                // Read Active IMU
                //AndruavResala_IMU andruavMessage_imu = new AndruavResala_IMU();
                final AndruavIMU andruavIMU = AndruavSettings.andruavWe7daBase.getActiveIMU();
                final AndruavResalaBinary_IMU andruavMessage_imu = new AndruavResalaBinary_IMU();
                andruavMessage_imu.useFCBIMU = AndruavSettings.andruavWe7daBase.useFCBIMU();
                andruavMessage_imu.Pitch =  andruavIMU.P;
                andruavMessage_imu.Roll =  andruavIMU.R;
                andruavMessage_imu.Yaw =  andruavIMU.Y;
                andruavMessage_imu.RollTilt =  andruavIMU.RT;
                andruavMessage_imu.PitchTilt =  andruavIMU.PT;
                if (size > 1) { //boradcast IMU if two or more listeners
                    sendMessage(andruavMessage_imu, null, Boolean.TRUE);
                    //broadcastMessageToGroup(andruavMessage_imu, Boolean.TRUE);
                } else {
                    AndruavUnitBase andruavUnit =  AndruavSettings.mIMURequests.get(0);
                    sendMessage(andruavMessage_imu, andruavUnit, Boolean.FALSE); // time here can be used on the other side ofr calculation.
                }
            }
        }



        if ((now - monIMUStatisticsEventTime) > monIMUStatisticsTimeDuration) {
            monIMUStatisticsEventTime = now;

            final AndruavResalaBinary_IMUStatistics andruavMessageBinary_imuStatistics = new AndruavResalaBinary_IMUStatistics();
            final AndruavIMU andruavIMU = AndruavSettings.andruavWe7daBase.getActiveIMU();
            andruavMessageBinary_imuStatistics.GroundAltitude_max = andruavIMU.GroundAltitude_max;
            andruavMessageBinary_imuStatistics.GroundSpeed_max = andruavIMU.GroundSpeed_max;
            andruavMessageBinary_imuStatistics.GroundSpeed_avg = andruavIMU.GroundSpeed_avg;
            andruavMessageBinary_imuStatistics.IdleDuration = andruavIMU.IdleDuration;
            andruavMessageBinary_imuStatistics.IdleTotalDuration = andruavIMU.IdleTotalDuration;

            sendMessage(andruavMessageBinary_imuStatistics, null, Boolean.FALSE);
            //broadcastMessageToGroup(andruavMessageBinary_imuStatistics, Boolean.FALSE); // time is alrdy included in location info

        }
    }

    /***
     * Drone uses this function to sendMessageToModule Battery Info
     * @param now {@link #monBATTEventTimeDuration } will be considered.
     */
    public static void sendBatteryInfo (final long now ) {

        if ((now - monBATTEventTime) < monBATTEventTimeDuration) return;

        monBATTEventTime = now;

        sendMessage(createPowerInfoMessage(), (AndruavUnitBase)null, Boolean.FALSE);
        //broadcastMessageToGroup(createPowerInfoMessage(), Boolean.FALSE);


    }

    /**
     * Read all available cameras in all modules and sendMessageToModule them.
     * @param isReply: true if reply to a request from unit to sendMessageToModule list, false if just this is an info
     * @param target
     */
    public static void sendCameraList (final boolean isReply, final AndruavUnitBase target)
    {
        try {

            final JSONArray cameraList = UAVOSHelper.getCameraList();

            AndruavMessage_CameraList andruavMessage_cameraList = new AndruavMessage_CameraList();
            andruavMessage_cameraList.isReply = isReply;
            andruavMessage_cameraList.CameraList = cameraList;

            sendMessage(andruavMessage_cameraList, target, Boolean.FALSE);
        }
        catch (Exception e) {
            AndruavEngine.log().logException("uav", e);
        }
    }


    public static void sendCameraFlashStatus (final String cameraUniqueName, final int flashOn, final AndruavUnitBase target)
    {
        final AndruavMessage_CameraFlash andruavMessage_cameraFlash = new AndruavMessage_CameraFlash();
        andruavMessage_cameraFlash.CameraUniqueName = cameraUniqueName;
        andruavMessage_cameraFlash.FlashOn = flashOn;

        sendMessage(andruavMessage_cameraFlash, target, Boolean.FALSE);
    }

    public static void sendCameraZoomStatus (final String cameraUniqueName, final float zoom, final AndruavUnitBase target)
    {
        final AndruavMessage_CameraZoom andruavMessage_cameraZoom = new AndruavMessage_CameraZoom();
        andruavMessage_cameraZoom.CameraUniqueName = cameraUniqueName;
        andruavMessage_cameraZoom.ZoomValue = (double) zoom;

        sendMessage(andruavMessage_cameraZoom, target, Boolean.FALSE);
    }


    /***
     * Send communication signal information message
     * @param target
     * @param respectRate if true then comply with {@link #monSignalTimeDuration}
     */
    public  static void sendCommSignalStatus (final AndruavUnitBase target, final boolean respectRate) {

        final long now = System.currentTimeMillis();

        if ((!respectRate) || ((now - monSignalTime) > monSignalTimeDuration)) {

            monSignalTime = now;

            final AndruavMessage_CommSignalsStatus commSignalsStatus = new AndruavMessage_CommSignalsStatus();

            commSignalsStatus.signalLevel = AndruavSettings.andruavWe7daBase.getSignalLevel();
            commSignalsStatus.signalType = AndruavSettings.andruavWe7daBase.getSignalType();

            sendMessage(commSignalsStatus, target, Boolean.FALSE);
        }
    }


    public static AndruavMessage_POW createPowerInfoMessage ()
    {
        final AndruavMessage_POW andruavMessage_pow= new AndruavMessage_POW();
        andruavMessage_pow.BatteryLevel= AndruavSettings.andruavWe7daBase.LastEvent_Battery.BatteryLevel;
        andruavMessage_pow.BatteryTechnology= AndruavSettings.andruavWe7daBase.LastEvent_Battery.BatteryTechnology;
        andruavMessage_pow.BatteryTemperature= AndruavSettings.andruavWe7daBase.LastEvent_Battery.BatteryTemperature;
        andruavMessage_pow.Charging= AndruavSettings.andruavWe7daBase.LastEvent_Battery.Charging;
        andruavMessage_pow.Health= AndruavSettings.andruavWe7daBase.LastEvent_Battery.Health;
        andruavMessage_pow.PlugStatus= AndruavSettings.andruavWe7daBase.LastEvent_Battery.PlugStatus;
        andruavMessage_pow.Voltage= AndruavSettings.andruavWe7daBase.LastEvent_Battery.Voltage;
        if (AndruavSettings.andruavWe7daBase.useFCBIMU())
        {
            andruavMessage_pow.hasFCBPowerInfo = true;
            andruavMessage_pow.FCB_BatteryRemaining = AndruavSettings.andruavWe7daBase.FCBoard.getBatteryRemaining();
            andruavMessage_pow.FCB_BatteryVoltage   = AndruavSettings.andruavWe7daBase.FCBoard.getPowerBatteryVoltage();
            andruavMessage_pow.FCB_BatteryCurrent = AndruavSettings.andruavWe7daBase.FCBoard.getBatteryCurrent();
        }

        return andruavMessage_pow;
    }


}
