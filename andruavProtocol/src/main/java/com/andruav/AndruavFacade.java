package com.andruav;

import android.location.Location;

import com.andruav.controlBoard.shared.missions.MissionBase;
import com.andruav.event.fcb_event.Event_FCB_RemoteControlSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitMapBase;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.event.droneReport_Event.Event_GeoFence_Hit;
import com.andruav.event.droneReport_Event.Event_TelemetryGCSRequest;
import com.andruav.interfaces.INotification;
import com.andruav.controlBoard.shared.missions.MohemmaMapBase;
import com.andruav.controlBoard.shared.geoFence.GeoFenceBase;
import com.andruav.controlBoard.shared.geoFence.GeoFenceManager;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_RemoteControlSettings;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMG;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_WayPointsUpdates;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraList;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraSwitch;
import com.andruav.protocol.commands.textMessages.AndruavMessage_DroneReport;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ExternalCommand_WayPoints;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GEOFenceHit;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GeoFence;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GeoFenceAttachStatus;
import com.andruav.protocol.commands.textMessages.AndruavMessage_HomeLocation;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ID;
import com.andruav.protocol.commands.textMessages.AndruavMessage_RemoteControl2;
import com.andruav.protocol.commands.textMessages.AndruavMessage_RemoteControlSettings;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_RemoteExecute;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Signaling;
import com.andruav.protocol.commands.textMessages.AndruavMessage_DistinationLocation;
import com.andruav.protocol.commands.textMessages.AndruavMessage_WayPoints;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_Ctrl_Camera;
import com.andruav.protocol.commands.textMessages.systemCommands.AndruavSystem_LoadTasks;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_WayPoints;
import com.andruav.protocol.commands.textMessages.systemCommands.AndruavSystem_UdpProxy;
import com.andruav.protocol.commands.textMessages.AndruavMessage_UDPProxy_Info;
import com.andruav.util.AndruavLatLngAlt;

import org.json.JSONObject;

/**
 * Created by M.Hefny on 02-Nov-14.
 */
public class AndruavFacade extends AndruavFacadeBase{




    public static void loadTasks (int largerThan_SID,
                                  String accessCode,
                                  String accountID,
                                  String party_sid,
                                  String groupName,
                                  String sender,
                                  String receiver,
                                  String messageType,
                                  boolean isPermanent)
    {
        try {

            final AndruavSystem_LoadTasks andruavSystem_loadTasks  = new AndruavSystem_LoadTasks(largerThan_SID,
                    accessCode,
                    accountID,
                    party_sid,
                    groupName,
                    sender,
                    receiver,
                    messageType,
                    isPermanent
            );
            sendSystemCommandToCommServer(andruavSystem_loadTasks, false,false);

        } catch (Exception e) {
        }

    }






    public static void setGPSMode (AndruavUnitBase andruavWe7da, int gpsMode)
    {
        if (!AndruavSettings.andruavWe7daBase.canControl())
        {
            return;
        }

        if (andruavWe7da == null) return;

        final AndruavMessage_RemoteExecute andruavMessage_remoteExecute = new AndruavMessage_RemoteExecute();
        andruavMessage_remoteExecute.RemoteCommandID = AndruavMessage_RemoteExecute.RemoteCommand_SET_GPS_SOURCE;
        andruavMessage_remoteExecute.Variables.put("s",String.valueOf(gpsMode));
        sendMessage(andruavMessage_remoteExecute,andruavWe7da, Boolean.FALSE);
    }

    public static void requestPowerInfo(final AndruavUnitBase target)
    {
        final AndruavMessage_RemoteExecute andruavMessage_remoteExecute = new AndruavMessage_RemoteExecute();
        andruavMessage_remoteExecute.RemoteCommandID = AndruavMessage_RemoteExecute.RemoteCommand_REQUEST_POW;
        sendMessage(andruavMessage_remoteExecute,target, Boolean.FALSE);
    }


    /***
     *
     * @param target
     * @param smartTelemetry_Level {@link  Constants#SMART_TELEMETRY_LEVEL_0} to {@link  Constants#SMART_TELEMETRY_LEVEL_3}. {@link Constants#SMART_TELEMETRY_LEVEL_NEGLECT} means will be neglected, and Drone will use its defined value.
     */
    public static void StartTelemetry(final AndruavUnitShadow target, final int smartTelemetry_Level)
    {
        if (!AndruavSettings.andruavWe7daBase.canTelemetry())
        {
            return;
        }

        if (AndruavSettings.remoteTelemetryAndruavWe7da != null)
        {
            if (AndruavSettings.remoteTelemetryAndruavWe7da.Equals(target))
            {
                // resume the current connection
                ResumeTelemetry(smartTelemetry_Level);
                return;
            }
            else
            {
                // disconnect old and connect to new one.
                StopTelemetry();

            }

        }

        SendTelemetry(Event_TelemetryGCSRequest.REQUEST_START,target,smartTelemetry_Level);
    }

    /***
     *
     * Resume connection with a current defined Drone.
     *
     * @param smartTelemetryLevel {@link  Constants#SMART_TELEMETRY_LEVEL_0} to {@link  Constants#SMART_TELEMETRY_LEVEL_3}. {@link Constants#SMART_TELEMETRY_LEVEL_NEGLECT} means will be neglected, and Drone will use its defined value.
     */
    public static void ResumeTelemetry(final int smartTelemetryLevel)
    {
        if (!AndruavSettings.andruavWe7daBase.canTelemetry())
        {
            return;
        }

        if (AndruavSettings.remoteTelemetryAndruavWe7da == null)
        {
            // Nothing to resume
            return;
        }

        SendTelemetry(Event_TelemetryGCSRequest.REQUEST_RESUME,AndruavSettings.remoteTelemetryAndruavWe7da,smartTelemetryLevel);
    }

    public static void StopTelemetry()
    {
        if (!AndruavSettings.andruavWe7daBase.canTelemetry())
        {
            return;
        }

        if (AndruavSettings.remoteTelemetryAndruavWe7da == null)
        {
            // nothing to stop
            return;
        }

        SendTelemetry(Event_TelemetryGCSRequest.REQUEST_END,AndruavSettings.remoteTelemetryAndruavWe7da, Constants.SMART_TELEMETRY_LEVEL_NEGLECT);
    }

    public static void StartUdpProxyTelemetry()
    {
        final AndruavSystem_UdpProxy andruavMessage_UdpProxy = new AndruavSystem_UdpProxy();
        andruavMessage_UdpProxy.enabled = true;
        sendSystemCommandToCommServer (andruavMessage_UdpProxy,false, false);
    }

    public static void StopUdpProxyTelemetry()
    {
        final AndruavSystem_UdpProxy andruavMessage_UdpProxy = new AndruavSystem_UdpProxy();
        andruavMessage_UdpProxy.enabled = false;
        sendSystemCommandToCommServer (andruavMessage_UdpProxy,false, false);
    }

    public static void sendUdpProxyStatus(final AndruavUnitShadow target)
    {
        final AndruavMessage_UDPProxy_Info andruavMessage_udpProxy_info = new AndruavMessage_UDPProxy_Info(AndruavSettings.andruavWe7daBase.getUdp_socket_ip_3rdparty(), AndruavSettings.andruavWe7daBase.getUdp_socket_port_3rdparty(),
                AndruavEngine.getPreference().getSmartMavlinkTelemetry(), AndruavSettings.andruavWe7daBase.isUdpProxyEnabled(), AndruavEngine.getUDPProxy().isPaused());

        sendMessage(andruavMessage_udpProxy_info,target, Boolean.FALSE);
    }

    /***
     *  /***
     * Request to (Start or Stop) telemetry data from a remote terminal
     * @param action _7adath_TelemetryGCSRequest values. Start , Stop , Resume.
     * @param target remote unit name
     * @param smartTelemetryLevel
     */
    private static void SendTelemetry (final int action, final AndruavUnitShadow target, final int smartTelemetryLevel) {

        final AndruavMessage_RemoteExecute andruavMessage_remoteExecute = new AndruavMessage_RemoteExecute();
        andruavMessage_remoteExecute.RemoteCommandID = AndruavMessage_RemoteExecute.RemoteCommand_TELEMETRYCTRL;
        andruavMessage_remoteExecute.Variables.put("Act", String.valueOf(action));

        if (action != Event_TelemetryGCSRequest.REQUEST_END)
        {
            AndruavSettings.remoteTelemetryAndruavWe7da = target;

            if (smartTelemetryLevel != Constants.SMART_TELEMETRY_LEVEL_NEGLECT)
            {
                // enforce Smart Telemetry Level
                andruavMessage_remoteExecute.Variables.put("LVL", String.valueOf(smartTelemetryLevel));
            }

        }
        else
        {
            AndruavSettings.remoteTelemetryAndruavWe7da = null;
        }


        sendMessage(andruavMessage_remoteExecute,target, Boolean.FALSE);
    }










    /***
     *  Possinle Contradiction {@link   }
     *
     * @param image can be null and will be stored only in mobile device and will not be sent over network.
     * @param imageLocation
     * @param target
     */
    public static void sendImage (final byte[] image,final Location imageLocation,final AndruavUnitBase target)
    {
        final AndruavResalaBinary_IMG andruavMessageBinary_img = new AndruavResalaBinary_IMG();
        andruavMessageBinary_img.ImageLocation = imageLocation;
        //andruavMessageBinary_img.IsVideo = isVideo;
        if ((image != null) && (AndruavEngine.getPreference().getSendBackImages()))
        {
            andruavMessageBinary_img.setImage(image);
        }
        sendMessage(andruavMessageBinary_img,target, Boolean.FALSE);
    }




    /***
    * either broadcast a command in group to know WHOisTHERE
    * or to sendMessageToModule individual msg to ask a particular unit WHOrYOU
    * @param target can be null for broadcasting.
    */
    public static void requestID(final AndruavUnitBase target)
    {

        AndruavMessage_RemoteExecute andruavMessage_remoteExecute = new AndruavMessage_RemoteExecute();
        andruavMessage_remoteExecute.RemoteCommandID = AndruavMessage_RemoteExecute.RemoteCommand_REQUEST_ID;

        sendMessage(andruavMessage_remoteExecute,target, Boolean.FALSE);
    }


    public static void requestID(final String target)
    {

        AndruavMessage_RemoteExecute andruavMessage_remoteExecute = new AndruavMessage_RemoteExecute();
        andruavMessage_remoteExecute.RemoteCommandID = AndruavMessage_RemoteExecute.RemoteCommand_REQUEST_ID;
        AndruavEngine.getAndruavWS().sendMessageToIndividual(andruavMessage_remoteExecute, target,false, false);
    }



    public static void requestID()
    {

        requestID((AndruavUnitBase)null);
    }



    /***
     * Send ID message to given unit or broadcast if target is null
     * @param target a given unit name or null to broadcast
     */
    public static void sendID(final AndruavUnitBase target)
    {

        String partyID = null;
        if (target != null)
        {
            partyID = target.PartyID;
        }


        sendID(partyID);
    }

    /***
     * Send ID message to given unit or broadcast if target is null
     * @param target a given unit name or null to broadcast
     */
    public static void sendID(final String target)
    {

        AndruavMessage_ID andruavMessage_id = new AndruavMessage_ID(AndruavSettings.andruavWe7daBase);
        sendMessage(andruavMessage_id,target, false);
    }

    /***
     * broadcast ShutDown status to other AndruavUnits to tell them that I will shutdown.
     * @param target
     */
    public static void sendShutDown (final AndruavUnitBase target)
    {

        if (AndruavEngine.getAndruavWS() != null)
        {
            sendID(target);
        }
    }

    /***
     * sends ID on both UDP & Andruav Websocket protocols
     * <br> calls {@link #sendID(AndruavUnitBase)}
     */
    public static void broadcastID ()
    {
        sendID((String) null);
    }


    /*public static void sendVideoWebRTCConnectToMeInfo(final String MyChannelName, final AndruavWe7daBase target)
    {
        final AndruavResala_CameraList andruavMessage_externalVideoStream = new AndruavResala_CameraList();
        andruavMessage_externalVideoStream.IP = MyChannelName;
        andruavMessage_externalVideoStream.isReply = true; // request sending start video stream.
        andruavMessage_externalVideoStream.ExternalType = AndruavResala_CameraList.EXTERNAL_CAMERA_TYPE_RTCWEBCAM;

        sendMessage(andruavMessage_externalVideoStream, target, Boolean.FALSE);
    }*/


    public  static void sendHomeLocation (final AndruavUnitBase target)
    {
        final AndruavMessage_HomeLocation andruavMessage_homeLocation = new AndruavMessage_HomeLocation();

        if (AndruavSettings.andruavWe7daBase.hasHomeLocation())
        {
            final AndruavLatLngAlt andruavLatLngAlt = AndruavSettings.andruavWe7daBase.getGpsHomeLocation();

            andruavMessage_homeLocation.home_gps_lng = andruavLatLngAlt.getLongitude();
            andruavMessage_homeLocation.home_gps_lat = andruavLatLngAlt.getLatitude();
            andruavMessage_homeLocation.home_gps_alt = andruavLatLngAlt.getAltitude();

            sendMessage(andruavMessage_homeLocation, target, Boolean.FALSE);
        }
    }


    public  static void sendTargetLocation (final AndruavUnitBase target)
    {
        final AndruavMessage_DistinationLocation andruavMessage_distinationLocation = new AndruavMessage_DistinationLocation();

        if (AndruavSettings.andruavWe7daBase.hasTargetLocation())
        {
            final AndruavLatLngAlt andruavLatLngAlt = AndruavSettings.andruavWe7daBase.getGpsTargetLocation();

            andruavMessage_distinationLocation.target_gps_lng = andruavLatLngAlt.getLongitude();
            andruavMessage_distinationLocation.target_gps_lat = andruavLatLngAlt.getLatitude();
            andruavMessage_distinationLocation.target_gps_alt = andruavLatLngAlt.getAltitude();

            sendMessage(andruavMessage_distinationLocation, target, Boolean.FALSE);
        }
    }

    /***
     * This command is sent to a Drone to update its waypoints
     * @param andruavUnitBase cannot be null
     * @param mohemmaMapBase {@link MohemmaMapBase} collection of {@link MissionBase}
     */
    public static void sendExternalWayPoints(final AndruavUnitBase andruavUnitBase, final MohemmaMapBase mohemmaMapBase)
    {
        if (andruavUnitBase ==null)
        {
            // you should select a target
            return;
        }

        AndruavMessage_ExternalCommand_WayPoints andruavMessage_externalCommand_wayPoints = new AndruavMessage_ExternalCommand_WayPoints();

        andruavMessage_externalCommand_wayPoints.setWayPoints(mohemmaMapBase);
        sendMessage(andruavMessage_externalCommand_wayPoints, andruavUnitBase, Boolean.FALSE);

        /*
        AndruavResalaBinary_ExternalCommand_WayPoints andruavResalaBinary_externalCommand_wayPoints = new AndruavResalaBinary_ExternalCommand_WayPoints();

        andruavResalaBinary_externalCommand_wayPoints.setWayPoints(mohemmaMapBase);
        sendMessage(andruavResalaBinary_externalCommand_wayPoints, andruavWe7daBase);
        */
    }

    /***
     * Sends Andruav Drone waypoints stored in {@link AndruavUnitBase} of self .
     * @param andruavUnitBase
     *
     * POTENTIOAL BUG: should be called when recieving a delete waypoint request.
     */
    public static void sendWayPoints(final AndruavUnitBase andruavUnitBase)
    {
        if (AndruavSettings.andruavWe7daBase.getIsCGS()) return ;

        final MohemmaMapBase mohemmaMapBase = AndruavSettings.andruavWe7daBase.getMohemmaMapBase();
        AndruavMessage_WayPoints andruavMessage_wayPoints = new AndruavMessage_WayPoints();

        andruavMessage_wayPoints.setWayPoints(mohemmaMapBase);
        sendMessage(andruavMessage_wayPoints, andruavUnitBase, Boolean.FALSE);
    }

    public static void sendWayPoints_binary(final AndruavUnitBase andruavUnitBase)
    {
        final MohemmaMapBase mohemmaMapBase = AndruavSettings.andruavWe7daBase.getMohemmaMapBase();
        AndruavResalaBinary_WayPoints andruavMessageBinary_wayPoints = new AndruavResalaBinary_WayPoints();

        andruavMessageBinary_wayPoints.setWayPoints(mohemmaMapBase);
        sendMessage(andruavMessageBinary_wayPoints, andruavUnitBase, Boolean.FALSE);
    }

    /***
     * Sends Andruav updated waypoints stored in {@link AndruavUnitBase} of self.
     * <br>updates could be because Item has been reached or changes in specs.
     * @param target
     * @param mohemmaMapBase if size equals zero then it simply return and does nothing.
     */
    public static void sendWayPointsUpdates(final AndruavUnitBase target, MohemmaMapBase mohemmaMapBase)
    {
        if (mohemmaMapBase.size()==0) return;
        AndruavResalaBinary_WayPointsUpdates andruavMessageBinary_wayPointUpdated = new AndruavResalaBinary_WayPointsUpdates();

        andruavMessageBinary_wayPointUpdated.setWayPoints(mohemmaMapBase);
        sendMessage(andruavMessageBinary_wayPointUpdated, target, Boolean.FALSE);
    }

    public static void  sendWayPointsReached(final AndruavUnitBase target, final int missionItemIndex, final int missionItemStatus)
    {
        AndruavMessage_DroneReport andruavMessage_droneReport = new AndruavMessage_DroneReport(missionItemStatus, missionItemIndex);
        sendMessage(andruavMessage_droneReport, target, Boolean.FALSE);
    }

    /***
     * Request Waypoints from Other Units
     * @param target
     */
    public static void  requestWayPoints (final AndruavUnitBase target)
    {
        AndruavMessage_RemoteExecute andruavMessage_remoteExecute = new AndruavMessage_RemoteExecute();
        andruavMessage_remoteExecute.RemoteCommandID = AndruavMessage_RemoteExecute.RemoteCommand_GET_WAY_POINTS;
        sendMessage(andruavMessage_remoteExecute,target, Boolean.FALSE);
    }

    /***
     * Reload waypoints from FCB -if exist- and sendMessageToModule it back when retrieved-.
     * @param target
     */
    public static void  requestReloadWayPoints (final AndruavUnitBase target)
    {
        AndruavMessage_RemoteExecute andruavMessage_remoteExecute = new AndruavMessage_RemoteExecute();
        andruavMessage_remoteExecute.RemoteCommandID = AndruavMessage_RemoteExecute.RemoteCommand_RELOAD_WAY_POINTS_FROM_FCB;
        sendMessage(andruavMessage_remoteExecute,target, Boolean.FALSE);
    }


    public static void requestClearWayPoints(final AndruavUnitBase target)
    {
        if (!AndruavSettings.andruavWe7daBase.canControl())
        {
            return;
        }

        AndruavMessage_RemoteExecute andruavResala_remoteExecute = new AndruavMessage_RemoteExecute();
        andruavResala_remoteExecute.RemoteCommandID = AndruavMessage_RemoteExecute.RemoteCommand_CLEAR_WAY_POINTS;

        sendMessage(andruavResala_remoteExecute,target, Boolean.FALSE);
    }


    public static void connectToFCB (final AndruavUnitBase target)
    {
        if (target == null) return ;

        AndruavMessage_RemoteExecute andruavResala_remoteExecute = new AndruavMessage_RemoteExecute();
        andruavResala_remoteExecute.RemoteCommandID = AndruavMessage_RemoteExecute.RemoteCommand_CONNECT_FCB;

        sendMessage(andruavResala_remoteExecute,target, Boolean.FALSE);

    }


    /***
     * Send Alert message to other Andruav informing of an internal error state.
     * @param infoType {@link INotification}
     * @param notification_Type {@link INotification}
     * @param errorNumber
     * @param description
     * @param target
     */
    public static void sendErrorMessage (final int infoType, final int notification_Type, final int errorNumber, final String description, final AndruavUnitBase target)
    {
        final AndruavMessage_Error andruavMessage_error = new AndruavMessage_Error();
        andruavMessage_error.Description = description;
        andruavMessage_error.errorNo = errorNumber;
        andruavMessage_error.infoType = infoType;
        andruavMessage_error.notification_Type = notification_Type;
        sendMessage(andruavMessage_error,target, Boolean.FALSE);
    }


    public static void sendRemoteControlMessage (final int[] channels, final boolean engaged, final AndruavUnitBase target)
    {
        if (!AndruavSettings.andruavWe7daBase.canControl()) return ;
        final AndruavMessage_RemoteControl2 andruavMessage_remoteControl2 = new AndruavMessage_RemoteControl2(channels,engaged);
        sendMessage(andruavMessage_remoteControl2,target, Boolean.FALSE);
    }


    /***
     * currently it sends onlt RTC status, as other values are scalled locally in Drone
     * @param target notmally it is a global message
     */
    public static void sendRemoteControlSettingsMessage (boolean[] rtc, final AndruavUnitBase target)
    {
        if (rtc==null) return ; // usually null should never happen
        final AndruavResalaBinary_RemoteControlSettings andruavMessage_remoteControlSettings = new AndruavResalaBinary_RemoteControlSettings(rtc);
        sendMessage(andruavMessage_remoteControlSettings,target, Boolean.FALSE);
    }


    public static void requestRemoteControlSettings(final AndruavUnitBase target)
    {
        if (!AndruavSettings.andruavWe7daBase.canControl())
        {
            return;
        }

        final AndruavMessage_RemoteExecute andruavMessage_remoteExecute = new AndruavMessage_RemoteExecute();
        andruavMessage_remoteExecute.RemoteCommandID = AndruavMessage_RemoteExecute.RemoteControl_RequestRemoteControlSettings;
        sendMessage(andruavMessage_remoteExecute,target, Boolean.FALSE);
    }


    /**
     * Tell drone that I will send you control -gamepad- info.
     * @param andruavUnitBase
     */
    public static void engageRX (final AndruavUnitBase andruavUnitBase)
    {

        

    }

    /**
     * Tell drone that I will send you control -gamepad- info.
     * <b>MAKE SURE</b> that no other drone is already engaged.
     * @param andruavUnitBase
     */
    public static void engageGamePad (final AndruavUnitBase andruavUnitBase)
    {

        if (andruavUnitBase == null) return ;

        sendRemoteControlSettings (andruavUnitBase, Event_FCB_RemoteControlSettings.RC_SUB_ACTION_JOYSTICK_CHANNELS);

    }


    public static void disengageGamePad (final AndruavUnitBase andruavUnitBase)
    {

        if (andruavUnitBase == null) return ;

        sendRemoteControlSettings (andruavUnitBase, Event_FCB_RemoteControlSettings.RC_SUB_ACTION_RELEASED);

    }


    private static void sendRemoteControlSettings (final AndruavUnitBase andruavUnitBase, final int rcSubAction)
    {

        if (andruavUnitBase == null) return ;

        final AndruavMessage_RemoteControlSettings andruavMessage_remoteExecute = new AndruavMessage_RemoteControlSettings();
        andruavMessage_remoteExecute.rcSubAction = rcSubAction;
        sendMessage(andruavMessage_remoteExecute , andruavUnitBase, Boolean.FALSE);

    }

    /***
     * This message is used to exchange webrtc communication data between parties.
     * @param jsonObject
     * @param target
     * @param instant
     */
    public static void sendWebRTCSignalingJSONMessage(final JSONObject jsonObject, final String target, boolean instant)
    {
        if (jsonObject == null) return ;
        final AndruavMessage_Signaling andruavMessage_signaling = new AndruavMessage_Signaling(jsonObject);
        sendMessage(andruavMessage_signaling,target,instant);
    }


    public static void sendGeoFenceAttach (final String fenceName, final boolean isAttach, final AndruavUnitBase target)
    {
        AndruavMessage_GeoFenceAttachStatus andruavMessage_geoFenceAttachStatus = new AndruavMessage_GeoFenceAttachStatus();

        andruavMessage_geoFenceAttachStatus.fenceName = fenceName;
        andruavMessage_geoFenceAttachStatus.isAttachedToFence = isAttach;

        sendMessage(andruavMessage_geoFenceAttachStatus,target, Boolean.FALSE);

    }

    /***
     * Sends fence info of geo-fences the drone is currently interacting with.
     * when {@link Event_GeoFence_Hit#hasValue} is true
     * @param target
     */
    public static void sendMyGeoFenceHitStatus (final AndruavUnitBase target)
    {

        final AndruavUnitBase andruavUnitBase = AndruavSettings.andruavWe7daBase;

        final int size = GeoFenceManager.size();

        for (int i = 0; i < size; ++i) {
            final GeoFenceBase geoLinearFenceMapBase = GeoFenceManager.valueAt(i);


            final Event_GeoFence_Hit geoFence_hit = geoLinearFenceMapBase.mAndruavUnits.get(andruavUnitBase.PartyID);

            if (geoFence_hit.hasValue)
            {
                sendGeoFenceHit (target, geoFence_hit);
            }
        }
    }

    /***
     * Geo Fence Hit is sent to AndruavServer when hitting a fence.
     * @param target
     * @param geoFence_hit
     */
    public static void sendGeoFenceHit (final AndruavUnitBase target, final Event_GeoFence_Hit geoFence_hit)
    {
        if (!geoFence_hit.hasValue) return ; // this is true if fence status has been not updated
        AndruavMessage_GEOFenceHit andruavMessage_geoFenceHit = new AndruavMessage_GEOFenceHit();
        andruavMessage_geoFenceHit.fenceName = geoFence_hit.fenceName;
        andruavMessage_geoFenceHit.distance = geoFence_hit.distance;
        andruavMessage_geoFenceHit.inZone = geoFence_hit.inZone;
        andruavMessage_geoFenceHit.shouldKeepOutside = geoFence_hit.shouldKeepOutside;


        sendMessage(andruavMessage_geoFenceHit,target, Boolean.FALSE);
    }


    public static void sendGeoFence(final AndruavUnitBase target)
    {

        final int size = GeoFenceManager.size();

        for (int i = 0; i < size; ++i) {
            final GeoFenceBase geoLinearFenceMapBase = GeoFenceManager.valueAt(i);
            if (!GeoFenceBase.isSharableFence(geoLinearFenceMapBase)) continue;
            sendGeoFence(target,geoLinearFenceMapBase);
        }

    }

    /***
     * sendMessageToModule Linear Geo Fence to a target
     * @param target
     * @param geoFenceMapBase
     */
    public static void sendGeoFence(final AndruavUnitBase target, final GeoFenceBase geoFenceMapBase)
    {
        if (geoFenceMapBase == null) return;


        if (!GeoFenceBase.isSharableFence(geoFenceMapBase)) return;

        AndruavMessage_GeoFence andruavMessage_geoFence = new AndruavMessage_GeoFence();
        andruavMessage_geoFence.setWayPoints(geoFenceMapBase);

        sendMessage(andruavMessage_geoFence,target, Boolean.FALSE);
    }



    /***
     * Request a geo  fence attached to a drone.
     * @param target
     */
    public static void requestGeoFenceInfo(final AndruavUnitBase target, final String fenceName)
    {
        AndruavMessage_RemoteExecute andruavResala_remoteExecute = new AndruavMessage_RemoteExecute();
        andruavResala_remoteExecute.RemoteCommandID = AndruavMessage_GeoFence.TYPE_AndruavMessage_GeoFence;
        if (fenceName != null)  andruavResala_remoteExecute.Variables.put("fn",fenceName);
        sendMessage(andruavResala_remoteExecute,target, Boolean.FALSE);
    }



    public static void sendSMS (final AndruavUnitBase andruavWe7da)
    {
        if (andruavWe7da == null) return;

        final AndruavMessage_RemoteExecute andruavMessage_remoteExecute = new AndruavMessage_RemoteExecute();
        andruavMessage_remoteExecute.RemoteCommandID = AndruavMessage_RemoteExecute.RemoteCommand_SENDSMS;
        sendMessage(andruavMessage_remoteExecute,andruavWe7da, Boolean.FALSE);
    }

    public static void makeWhisle (final AndruavUnitBase andruavWe7da)
    {
        if (andruavWe7da == null) return;

        final AndruavMessage_RemoteExecute andruavMessage_remoteExecute = new AndruavMessage_RemoteExecute();
        andruavMessage_remoteExecute.RemoteCommandID = AndruavMessage_RemoteExecute.RemoteCommand_MAKEBEEP;
        sendMessage(andruavMessage_remoteExecute,andruavWe7da, Boolean.FALSE);
    }

    public static void makeFlash (final AndruavUnitBase andruavWe7da)
    {
        if (andruavWe7da == null) return;

        final AndruavMessage_RemoteExecute andruavMessage_remoteExecute = new AndruavMessage_RemoteExecute();
        andruavMessage_remoteExecute.RemoteCommandID = AndruavMessage_RemoteExecute.RemoteCommand_MAKEFLASH;
        sendMessage(andruavMessage_remoteExecute,andruavWe7da, Boolean.FALSE);
    }




}