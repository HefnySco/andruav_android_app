package com.andruav.andruavUnit;

import androidx.collection.SimpleArrayMap;

import com.andruav.AndruavFacade;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.Constants;
import com.andruav.sensors.AndruavIMU;
import com.andruav.controlBoard.shared.missions.MissionBase;
import com.andruav.controlBoard.shared.missions.MohemmaMapBase;
import com.andruav.protocol.R;
import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.protocol.commands.binaryMessages.AndruavBinary_2MR;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMU;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMUStatistics;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_WayPoints;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_WayPointsUpdates;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GPS;
import com.andruav.protocol.commands.textMessages.AndruavMessage_HomeLocation;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ID;
import com.andruav.protocol.commands.textMessages.AndruavMessage_NAV_INFO;
import com.andruav.protocol.commands.textMessages.AndruavMessage_POW;
import com.andruav.protocol.commands.textMessages.AndruavMessage_DistinationLocation;
import com.andruav.protocol.commands.textMessages.AndruavMessage_WayPoints;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;
import com.andruav.interfaces.INotification;


/**
 * Created by M.Hefny on 14-Feb-15.
 */
public class AndruavUnitMapBase extends SimpleArrayMap<String, AndruavUnitBase> {

    @Override
    public AndruavUnitBase put(final String key, final AndruavUnitBase value) {

        if (key.equals(ProtocolHeaders.SPECIAL_NAME_SYS_NAME)) return null;

        synchronized (this) {
            super.put(key, value);
        }

        return value;
    }


    public AndruavUnitBase get (final String key)
    {
        if ((key == null) || (key.isEmpty())) return null;

        if (key.equals(Constants._nezam_))
        {
            return new AndruavUnitSystem();
        }
        else
        {
            return super.get(key);
        }

    }



    /***
     * Called when a new unit added
     * @param andruavUnitBase
     */
    protected void newUnitAdded (final AndruavUnitBase andruavUnitBase)
    {
        if (!andruavUnitBase.getIsCGS()) {
            AndruavFacade.requestRemoteControlSettings(andruavUnitBase);
            AndruavFacade.requestWayPoints(andruavUnitBase);
            AndruavFacade.requestPowerInfo(andruavUnitBase);
            AndruavFacade.requestGeoFenceInfo(andruavUnitBase, null);
        }
    }

    /**
     * Check for units that hasnot sent identification for a long period and ask it for identification.
     * also update its status from alive to lower states.
     */
    public void UpdateExpiredUsers() {
        return;
    }


    public void remove(final Andruav_2MR andruav2MR) {

        super.remove(andruav2MR.partyID);
        AndruavEngine.notification().displayNotification(INotification.NOTIFICATION_TYPE_WARNING, "Andruav", andruav2MR.partyID + AndruavEngine.AppContext.getString(R.string.andruav_noti_disconnect), true, INotification.INFO_TYPE_PROTOCOL, false);

    }

    /***
     * Update AndruavUnit status from Andruav_2MR.
     * updates IsGCS, Description,lastActiveTime and UnitStatus
     *
     * @param andruav2MR
     * @return
     */
    public AndruavUnitBase put(final Andruav_2MR andruav2MR) {

        boolean first = false;

        AndruavUnitBase andruavUnit = get(andruav2MR.partyID);
        if (andruavUnit == null) {
            first = true;

            andruavUnit = AndruavEngine.getAndruavWe7daMasna3().createAndruavUnitClass(andruav2MR.groupName, andruav2MR.partyID,((AndruavMessage_ID) andruav2MR.andruavMessageBase).IsCGS);


            newUnitAdded (andruavUnit);
        }
        // first settings
        //andruavUnit.PartyID = andruav2MR.PartyID;
        andruavUnit.UnitID = ((AndruavMessage_ID) andruav2MR.andruavMessageBase).UnitID;
        andruavUnit.setPermissions(((AndruavMessage_ID) andruav2MR.andruavMessageBase).Permissions);
        andruavUnit.Description = ((AndruavMessage_ID) andruav2MR.andruavMessageBase).Description;
        //andruavUnit.IsCGS = ((AndruavResala_ID) andruav2MR.andruavResalaBase).IsCGS;

        andruavUnit.setShutdown(((AndruavMessage_ID) andruav2MR.andruavMessageBase).IsShutdown);



        if (!andruavUnit.getIsCGS()) {
            andruavUnit.setisGCSBlockedFromBoard(((AndruavMessage_ID) andruav2MR.andruavMessageBase).isGCSBlocked);
            andruavUnit.setManualTXBlockedSubAction(((AndruavMessage_ID) andruav2MR.andruavMessageBase).manualTXBlockedMode);
        }


        andruavUnit.setGPSMode(((AndruavMessage_ID) andruav2MR.andruavMessageBase).GPSMode);
        andruavUnit.setVehicleType(((AndruavMessage_ID) andruav2MR.andruavMessageBase).VehicleType);



        // TELEMETRY DATA
        // 1-
        // as a GCS or another drone, dont assume that you can use FCB_IMU of  another Drone unless the other drone says you can.
        // because the other drone might fail to connect to board due to any protocol mismatch
        // or get a disconnect error ....etc.... let the owner deciede not YOU.
        andruavUnit.useFCBIMU(((AndruavMessage_ID) andruav2MR.andruavMessageBase).useFCBIMU);
        // 2-
        andruavUnit.setTelemetry_protocol(((AndruavMessage_ID) andruav2MR.andruavMessageBase).telemetry_protocol);




        andruavUnit.VideoRecording = ((AndruavMessage_ID) andruav2MR.andruavMessageBase).VideoRecording;

        andruavUnit.IsArmed(((AndruavMessage_ID) andruav2MR.andruavMessageBase).IsArmed);
        andruavUnit.IsFlying(((AndruavMessage_ID) andruav2MR.andruavMessageBase).IsFlying);
        andruavUnit.setFlightModeFromBoard(((AndruavMessage_ID) andruav2MR.andruavMessageBase).FlyingMode);
        andruavUnit.setFlyingStartTime(((AndruavMessage_ID) andruav2MR.andruavMessageBase).FlyingLastStartTime);
        andruavUnit.setFlyingTotalDuration(((AndruavMessage_ID) andruav2MR.andruavMessageBase).FlyingTotalDuration);

        andruavUnit.setIsFlashing(((AndruavMessage_ID) andruav2MR.andruavMessageBase).IsFlashing);
        andruavUnit.setIsWhisling(((AndruavMessage_ID) andruav2MR.andruavMessageBase).IsWhisling);

        andruavUnit.lastActiveTime = System.currentTimeMillis();

        put(andruav2MR.partyID, andruavUnit);



        if (first && (!andruavUnit.getIsCGS()) && (AndruavSettings.andruavWe7daBase.getIsCGS())) {
            AndruavEngine.notification().displayNotification(INotification.NOTIFICATION_TYPE_NORMAL, "Andruav", andruavUnit.UnitID, true, INotification.INFO_TYPE_PROTOCOL, false);
        }


        return andruavUnit;
    }

    public int getConnectionState(final String partyID) {
        AndruavUnitBase andruavUnit = get(partyID);
        if (andruavUnit == null) {
            return 0;
        }

        return andruavUnit.getConnectionState();
    }


    public void updateLastActiveTime(final String partyID) {

        if (partyID.equals(ProtocolHeaders.SPECIAL_NAME_SYS_NAME)) return ;

        AndruavUnitBase andruavUnit = get(partyID);
        if (andruavUnit == null) {
            // Object is not in the defined markers...
            // either because :
            // 1- No Message ID has been sent yet.
            // 2- future reason: permissions and security related issues.
            // We can add it as update list to increase response time but I prefer to leave it as is
            // for future security and permissions updates.
            // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
            return;
        }
        andruavUnit.lastActiveTime = System.currentTimeMillis();
        andruavUnit.unitStatus = AndruavUnitBase.enum_userStatus.ALIVE; // it is OK

    }


    /***
     * Replaces missions with same Sequence Number.
     * @param andruavWe7da
     * @param andruavResalaBinary_wayPointsUpdates
     */
    public void updateWayPoints(AndruavUnitBase andruavWe7da, AndruavResalaBinary_WayPointsUpdates andruavResalaBinary_wayPointsUpdates)
    {
        updateWayPoints_private (andruavWe7da, andruavResalaBinary_wayPointsUpdates);
    }

    //BUG: This is a bad logic
    private void updateWayPoints_private (AndruavUnitBase anruavWe7da, AndruavResalaBinary_WayPointsUpdates andruavResalaBinary_wayPointsUpdates)
    {
       final MohemmaMapBase mohemmaMapBase = andruavResalaBinary_wayPointsUpdates.getWayPoints();

        for (int i=0, limit=andruavResalaBinary_wayPointsUpdates.getWayPoints().size();i<limit;i++) {

            MissionBase missionBase = mohemmaMapBase.valueAt(i);
            anruavWe7da.getMohemmaMapBase().put(String.valueOf(missionBase.Sequence), missionBase);
        }


    }


    /***
     * sendMessageToModule an empty waypoints will clear the current waypoints of the unit.
     * @param andruavWe7da
     * @param andruavMessageBinary_wayPoints
     */
    public void refreshWayPoints(AndruavUnitBase andruavWe7da, AndruavResalaBinary_WayPoints andruavMessageBinary_wayPoints)
    {

        MohemmaMapBase mohemmaMapBase = andruavMessageBinary_wayPoints.getWayPoints();

        andruavWe7da.refreshWayPoints(mohemmaMapBase);


        return ;
    }

    public void refreshWayPoints(AndruavUnitBase andruavWe7da, AndruavMessage_WayPoints andruavMessage_wayPoints)
    {

        MohemmaMapBase mohemmaMapBase = andruavMessage_wayPoints.getWayPoints();

        andruavWe7da.refreshWayPoints(mohemmaMapBase);


        return ;
    }

    public AndruavUnitBase updateNAV(Andruav_2MR andruavCMD)
    {
        final AndruavUnitBase andruavUnit = get(andruavCMD.partyID);
        if (andruavUnit == null) {
            // Object is not in the defined markers...
            // either because :
            // 1- No Message ID has been sent yet.
            // 2- future reason: permissions and security related issues.
            // We can add it as update list to increase response time but I prefer to leave it as is
            // for future security and permissions updates.
            // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
            return null;
        }

        AndruavMessage_NAV_INFO andruavMessage_nav_info = (AndruavMessage_NAV_INFO)   andruavCMD.andruavMessageBase;
        andruavUnit.LastEvent_FCB_IMU.nav_Pitch             = andruavMessage_nav_info.nav_pitch;
        andruavUnit.LastEvent_FCB_IMU.nav_Roll              = andruavMessage_nav_info.nav_roll;
        andruavUnit.LastEvent_FCB_IMU.P                     = andruavMessage_nav_info.nav_pitch ;
        andruavUnit.LastEvent_FCB_IMU.R                     = andruavMessage_nav_info.nav_roll ;
        andruavUnit.LastEvent_FCB_IMU.Y                     = andruavMessage_nav_info.nav_yaw;
        andruavUnit.LastEvent_FCB_IMU.nav_TargetBearing     = andruavMessage_nav_info.target_bearing;
        andruavUnit.LastEvent_FCB_IMU.nav_AltitudeError     = andruavMessage_nav_info.alt_error;
        andruavUnit.LastEvent_FCB_IMU.nav_WayPointDistance     = andruavMessage_nav_info.wp_dist;
        andruavUnit.useFCBIMU(true);

        andruavUnit.lastActiveTime = System.currentTimeMillis();
        andruavUnit.unitStatus = AndruavUnitBase.enum_userStatus.ALIVE; // it is OK

        return andruavUnit;
    }

    public AndruavUnitBase updateIMU(AndruavBinary_2MR andruavCMD)
    {
        final AndruavUnitBase andruavUnit = get(andruavCMD.partyID);
        if (andruavUnit == null) {
            // Object is not in the defined markers...
            // either because :
            // 1- No Message ID has been sent yet.
            // 2- future reason: permissions and security related issues.
            // We can add it as update list to increase response time but I prefer to leave it as is
            // for future security and permissions updates.
            // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
            return null;
        }

        //AndruavResala_IMU andruavMessage_imu = (AndruavResala_IMU) andruavCMD.andruavResalaBase;
        AndruavResalaBinary_IMU andruavMessage_imu = (AndruavResalaBinary_IMU)   andruavCMD.andruavResalaBinaryBase;

        if (andruavMessage_imu.useFCBIMU)
        {
            andruavUnit.LastEvent_FCB_IMU.P  = andruavMessage_imu.Pitch;
            andruavUnit.LastEvent_FCB_IMU.R  = andruavMessage_imu.Roll;
            andruavUnit.LastEvent_FCB_IMU.Y  = andruavMessage_imu.Yaw;
            andruavUnit.LastEvent_FCB_IMU.PT = andruavMessage_imu.PitchTilt;
            andruavUnit.LastEvent_FCB_IMU.RT = andruavMessage_imu.RollTilt;
        }
        else {
            andruavUnit.LastEvent_IMU.P     = andruavMessage_imu.Pitch;
            andruavUnit.LastEvent_IMU.R     = andruavMessage_imu.Roll;
            andruavUnit.LastEvent_IMU.Y     = andruavMessage_imu.Yaw;
            andruavUnit.LastEvent_IMU.PT    = andruavMessage_imu.PitchTilt;
            andruavUnit.LastEvent_IMU.RT    = andruavMessage_imu.RollTilt;
        }

        andruavUnit.useFCBIMU(andruavMessage_imu.useFCBIMU);

        andruavUnit.lastActiveTime = System.currentTimeMillis();
        andruavUnit.unitStatus = AndruavUnitBase.enum_userStatus.ALIVE; // it is OK

        return andruavUnit;
    }

    public AndruavUnitBase updateHomeLocation(Andruav_2MR andruav2MR) {
        final AndruavUnitBase andruavUnit = get(andruav2MR.partyID);
        if (andruavUnit == null) {
            // Object is not in the defined markers...
            // either because :
            // 1- No Message ID has been sent yet.
            // 2- future reason: permissions and security related issues.
            // We can add it as update list to increase response time but I prefer to leave it as is
            // for future security and permissions updates.
            // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
            return null;
        }

        final AndruavMessage_HomeLocation andruavMessage_homeLocation = (AndruavMessage_HomeLocation) andruav2MR.andruavMessageBase;

        andruavUnit.updateHomeLocation(andruavMessage_homeLocation.home_gps_lng, andruavMessage_homeLocation.home_gps_lat, andruavMessage_homeLocation.home_gps_alt);

        andruavUnit.lastActiveTime = System.currentTimeMillis();
        andruavUnit.unitStatus = AndruavUnitBase.enum_userStatus.ALIVE; // it is OK

        return andruavUnit ;
    }



    public AndruavUnitBase updateTargetLocation(Andruav_2MR andruav2MR) {
        final AndruavUnitBase andruavUnit = get(andruav2MR.partyID);
        if (andruavUnit == null) {
            // Object is not in the defined markers...
            // either because :
            // 1- No Message ID has been sent yet.
            // 2- future reason: permissions and security related issues.
            // We can add it as update list to increase response time but I prefer to leave it as is
            // for future security and permissions updates.
            // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
            return null;
        }

        final AndruavMessage_DistinationLocation andruavMessage_distinationLocation = (AndruavMessage_DistinationLocation) andruav2MR.andruavMessageBase;

        andruavUnit.internal_updateTargetLocation(andruavMessage_distinationLocation.target_gps_lng, andruavMessage_distinationLocation.target_gps_lat, andruavMessage_distinationLocation.target_gps_alt);

        andruavUnit.lastActiveTime = System.currentTimeMillis();
        andruavUnit.unitStatus = AndruavUnitBase.enum_userStatus.ALIVE; // it is OK

        return andruavUnit ;
    }

    public AndruavUnitBase updateGPS(Andruav_2MR andruav2MR)
    {
        final AndruavUnitBase andruavUnit = get(andruav2MR.partyID);
        if (andruavUnit == null) {
            // Object is not in the defined markers...
            // either because :
            // 1- No Message ID has been sent yet.
            // 2- future reason: permissions and security related issues.
            // We can add it as update list to increase response time but I prefer to leave it as is
            // for future security and permissions updates.
            // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
            return null;
        }

        final AndruavMessage_GPS andruavMessage_gps  = (AndruavMessage_GPS) andruav2MR.andruavMessageBase;

        AndruavIMU andruavIMU = null;

        if (andruavMessage_gps.GPSFCB)
        {
            andruavIMU = andruavUnit.LastEvent_FCB_IMU;
        }
        else
        {
            andruavIMU = andruavUnit.LastEvent_IMU;
        }



        andruavIMU.SATC = andruavMessage_gps.SATC;
        andruavIMU.GPS3DFix = andruavMessage_gps.GPS3DFix;
        andruavIMU.setCurrentLocation (andruavMessage_gps.CurrentLocation);

        andruavUnit.lastActiveTime = System.currentTimeMillis();
        andruavUnit.unitStatus = AndruavUnitBase.enum_userStatus.ALIVE; // it is OK

        return andruavUnit ;

    }

    public AndruavUnitBase updateIMUStatistics (AndruavBinary_2MR andruavCMD)
    {
        final AndruavUnitBase andruavUnit = get(andruavCMD.partyID);
        if (andruavUnit == null) {
            // Object is not in the defined markers...
            // either because :
            // 1- No Message ID has been sent yet.
            // 2- future reason: permissions and security related issues.
            // We can add it as update list to increase response time but I prefer to leave it as is
            // for future security and permissions updates.
            // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
            return null;
        }

        AndruavResalaBinary_IMUStatistics andruavMessageBinary_imuStatistics = (AndruavResalaBinary_IMUStatistics)   andruavCMD.andruavResalaBinaryBase;

        if (andruavMessageBinary_imuStatistics.useFCBIMU)
        {
            andruavUnit.LastEvent_FCB_IMU.GroundAltitude_max    = andruavMessageBinary_imuStatistics.GroundAltitude_max;
            andruavUnit.LastEvent_FCB_IMU.GroundSpeed_max       = andruavMessageBinary_imuStatistics.GroundSpeed_max;
            andruavUnit.LastEvent_FCB_IMU.GroundSpeed_avg       = andruavMessageBinary_imuStatistics.GroundSpeed_avg;
            andruavUnit.LastEvent_FCB_IMU.IdleDuration          = andruavMessageBinary_imuStatistics.IdleDuration;
            andruavUnit.LastEvent_FCB_IMU.IdleTotalDuration     = andruavMessageBinary_imuStatistics.IdleTotalDuration;
        }
        else {
            andruavUnit.LastEvent_IMU.GroundAltitude_max    = andruavMessageBinary_imuStatistics.GroundAltitude_max;
            andruavUnit.LastEvent_IMU.GroundSpeed_max       = andruavMessageBinary_imuStatistics.GroundSpeed_max;
            andruavUnit.LastEvent_IMU.GroundSpeed_avg       = andruavMessageBinary_imuStatistics.GroundSpeed_avg;
            andruavUnit.LastEvent_IMU.IdleDuration          = andruavMessageBinary_imuStatistics.IdleDuration;
            andruavUnit.LastEvent_IMU.IdleTotalDuration     = andruavMessageBinary_imuStatistics.IdleTotalDuration;
        }


        andruavUnit.lastActiveTime = System.currentTimeMillis();
        andruavUnit.unitStatus = AndruavUnitBase.enum_userStatus.ALIVE; // it is OK

        return andruavUnit;

    }

    public AndruavUnitBase updatePOW(Andruav_2MR andruav2MR)
    {

        final AndruavUnitBase andruavUnit = get(andruav2MR.partyID);
        if (andruavUnit == null) {
            // Object is not in the defined markers...
            // either because :
            // 1- ScheduledTasks has not run yet
            // 2- No Message ID has been sent yet.
            // 3- future reason: permissions and security related issues.
            // We can add it as update list to increase response time but I prefer to leave it as is
            // for future security and permissions updates.
            // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
            return andruavUnit;
        }

        AndruavMessage_POW andruavMessage_pow  = (AndruavMessage_POW) andruav2MR.andruavMessageBase;

        andruavUnit.LastEvent_Battery.BatteryLevel          = andruavMessage_pow.BatteryLevel;
        andruavUnit.LastEvent_Battery.BatteryTechnology     = andruavMessage_pow.BatteryTechnology;
        andruavUnit.LastEvent_Battery.BatteryTemperature    = andruavMessage_pow.BatteryTemperature;
        andruavUnit.LastEvent_Battery.Charging              = andruavMessage_pow.Charging;
        andruavUnit.LastEvent_Battery.Health                = andruavMessage_pow.Health;
        andruavUnit.LastEvent_Battery.PlugStatus            = andruavMessage_pow.PlugStatus;
        andruavUnit.LastEvent_Battery.Voltage               = andruavMessage_pow.Voltage;
        andruavUnit.LastEvent_Battery.FCB_BatteryVoltage    = andruavMessage_pow.FCB_BatteryVoltage;
        andruavUnit.LastEvent_Battery.FCB_BatteryRemaining  = andruavMessage_pow.FCB_BatteryRemaining;
        andruavUnit.LastEvent_Battery.FCB_CurrentConsumed   = andruavMessage_pow.FCB_BatteryCurrent;

        andruavUnit.lastActiveTime = System.currentTimeMillis();
        andruavUnit.unitStatus = AndruavUnitBase.enum_userStatus.ALIVE; // it is OK

        return andruavUnit;
    }

}
