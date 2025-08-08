package ap.andruav_ap.communication;

import android.content.ContentValues;

import com.andruav.AndruavDroneFacade;
import com.andruav.AndruavFacade;
import com.andruav.AndruavMeFacade;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.Constants;
import com.andruav.TelemetryProtocol;
import com.andruav.event.droneReport_Event.Event_Battery_Ready;
import com.andruav.event.droneReport_Event.Event_GPS_Ready;
import com.andruav.event.droneReport_Event.Event_GeoFence_Hit;
import com.andruav.event.droneReport_Event.Event_GeoFence_Ready;
import com.andruav.event.droneReport_Event.Event_HomeLocation_Ready;
import com.andruav.event.droneReport_Event.Event_IMU_Ready;
import com.andruav.event.droneReport_Event.Event_NAV_INFO_Ready;
import com.andruav.event.droneReport_Event.Event_SERVO_Outputs_Ready;
import com.andruav.event.droneReport_Event.Event_TargetLocation_Ready;
import com.andruav.event.droneReport_Event.Event_TelemetryGCSRequest;
import com.andruav.event.droneReport_Event.Event_WayPointReached;
import com.andruav.event.fcb_event._7adath_FCB_2AMR;
import com.andruav.event.fpv7adath.Event_FPV_CMD;
import com.andruav.event.fpv7adath._7adath_InitAndroidCamera;
import com.andruav.event.networkEvent.EventLoginClient;
import com.andruav.event.networkEvent.EventSocketState;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.interfaces.INotification;
import com.andruav.controlBoard.shared.geoFence.GeoFenceBase;
import com.andruav.controlBoard.shared.geoFence.GeoFenceManager;
import com.andruav.controlBoard.shared.missions.MissionBase;
import com.andruav.notification.PanicFacade;
import com.andruav.protocol.commands.binaryMessages.AndruavBinary_2MR;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMG;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMUStatistics;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_LightTelemetry;
import com.andruav.protocol.commands.textMessages.AndruavMessage_DroneReport;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraList;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ServoChannel;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Sound_TextToSpeech;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;
import com.andruav.protocol.commands.textMessages.Configuration.AndruavMessage_Config_COM;
import com.andruav.protocol.commands.textMessages.Configuration.AndruavMessage_Config_Preference;
import com.andruav.protocol.commands.textMessages.Configuration.AndruavMessage_Config_UnitID;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_Ctrl_Camera;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_RemoteExecute;
import com.andruav.protocol.commands.textMessages.AndruavMessage_UDPProxy_Info;
import com.andruav.protocol.communication.uavos.AndruavUDPServerBase;
import com.andruav.protocol.communication.websocket.AndruavWSClientBase_TooTallNate;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.App;
import ap.andruav_ap.Emergency;
import com.andruav.event.fcb_event.Event_FCBData;
import com.andruav.event.fcb_event.Event_SocketData;
import ap.andruavmiddlelibrary.ILoginClientCallback;
import ap.andruavmiddlelibrary.LoginClient;
import ap.andruavmiddlelibrary.sensors._7asasatEvents.Event_IMU_CMD;
import ap.andruavmiddlelibrary.eventClasses.fpvEvent.Event_FPV_Image;
import ap.andruavmiddlelibrary.eventClasses.fpvEvent.Event_FPV_VideoURL;
import ap.andruavmiddlelibrary.eventClasses.remoteControl.Event_RemoteServo;
import com.andruav.event.systemEvent.Event_ShutDown_Signalling;
import ap.andruavmiddlelibrary.preference.Preference;

public class AndruavWSClient_TooTallNate extends AndruavWSClientBase_TooTallNate {




    private static final int telemetryBufferTimeOut=200;  // ms
    //private static final int telemetryBufferLength=50;
    //private static byte[] telemetryBytes = new byte[telemetryBufferLength + 20];
    private static final int telemetryBytesIndex=0;
    // private static byte[] telemetryCommand = new byte[telemetryBufferLength + 20];
    private static final int telemetryCommandIndex=0;
    private final static  long monPingDroneTelemetryDuration    =30000;  //should be > monSlowOperationTicks
    private static  long monPingDroneTelemetry=0;

    /////////// EOF Attributes


    //////////BUS EVENT

    public void onEvent (final Event_ShutDown_Signalling event)
    {
        if (event.CloseOrder != 3) return ;

        AndruavSettings.andruavWe7daBase.setShutdown(true);

        this.shutDown();

    }

    public void onEvent (Event_GeoFence_Hit a7adath_geoFence_hit)
    {
        if (a7adath_geoFence_hit.andruavUnitBase.IsMe()) {
            //NotificationFacade.cannotStartCamera();
            AndruavFacade.sendGeoFenceHit(null, a7adath_geoFence_hit);

            final GeoFenceBase geoFenceBase = GeoFenceManager.getGeoFence(a7adath_geoFence_hit.fenceName);
            GeoFenceManager.determineFenceValidationAction(a7adath_geoFence_hit.inZone, a7adath_geoFence_hit.shouldKeepOutside, geoFenceBase);
        }
        else
        {
            // Handle fences of other drone by making sure that you have fence info loaded into your drone.
            // check if the mentioned fence is saved here

            if (!GeoFenceManager.containsKey(a7adath_geoFence_hit.fenceName)) {
                AndruavFacade.requestGeoFenceInfo(a7adath_geoFence_hit.andruavUnitBase, a7adath_geoFence_hit.fenceName);
            }
        }
    }


    public void onEvent (Event_GeoFence_Ready a7adath_geoFence_ready)
    {
        final AndruavUnitBase andruavUnitBase = a7adath_geoFence_ready.andruavWe7da;
        if(andruavUnitBase.IsMe())
        {
            // seems I got my GEO Fence Updated
            // TODO: you can sendMessageToModule a REMORT ONLY and THEN GCS later can use RemoteExecuteCommand to ask for sending GeoFencePoint.


            // OLD TECHNIQUE
            // Here we Broadcast a large amount of DATA !!!
            // We could Send the name and then interested units will sendMessageToModule RemoteExecute - TYPE_AndruavMessage_GeoFence
            ///////AndruavFacade.sendGeoFence(null,GeoFenceManager.getGeoFence(a7adath_geoFence_ready.fenceName));

            // Here we broad cast thet this drone is only attached to a fence by fenceName that is it.
            AndruavFacade.sendGeoFenceAttach(a7adath_geoFence_ready.fenceName,true,null);
        }

    }



    public void onEvent (final Event_GPS_Ready a7adath_gps_ready)  {
        if (!a7adath_gps_ready.mAndruavWe7da.IsMe()) return ;

        if (getSocketState() != SOCKETSTATE_REGISTERED) {
            return;
        }

        AndruavDroneFacade.handleGPSInfo();
    }


    public void onEvent (final Event_NAV_INFO_Ready a7adath_nav_info_ready)
    {
        if (getSocketState() != SOCKETSTATE_REGISTERED) {
            return;
        }

        if (!a7adath_nav_info_ready.mAndruavWe7da.IsMe()) return ;


        final long now = System.currentTimeMillis();

        if (!AndruavSettings.andruavWe7daBase.useFCBIMU()) return ; // it should be true

        AndruavDroneFacade.sendNAVInfo(now);
    }


    final long sendServoOutputInfo_sent_duration = 5000;
    long sendServoOutputInfo_sent_time = 0;
    public void onEvent (final Event_SERVO_Outputs_Ready a7adath_servo_output_ready)
    {
        if (getSocketState() != SOCKETSTATE_REGISTERED) {
            return;
        }

        if (!a7adath_servo_output_ready.mAndruavWe7da.IsMe()) return ;


        final long now = System.currentTimeMillis();

        if (!AndruavSettings.andruavWe7daBase.useFCBIMU()) return ; // it should be true

        if ((a7adath_servo_output_ready.mValuesChanged) || (now-sendServoOutputInfo_sent_time > sendServoOutputInfo_sent_duration)) {
            AndruavDroneFacade.sendServoOutputInfo();
            sendServoOutputInfo_sent_time = now;
        }
    }



    public void onEvent (final Event_IMU_Ready a7adath_imu_ready) throws JSONException {

        if (getSocketState() != SOCKETSTATE_REGISTERED) {
            return;
        }

        if (!a7adath_imu_ready.mAndruavWe7da.IsMe()) return ;


        /**
         BUG

         if (AndruavSettings.andruavWe7daBase.LastEvent_IMU.hasCurrentLocation()) {
         mLastLocation = AndruavSettings.andruavWe7daBase.LastEvent_IMU.getCurrentLocation(); // save if for emergency recovery
         }

         */

        /*
        AndruavResala_NAV_INFO is sent instead when useFCBIMU is true
         */
        if (AndruavSettings.andruavWe7daBase.useFCBIMU()) return ; // it should be true

        final long now = System.currentTimeMillis();
        AndruavDroneFacade.sendIMUInfo(now);

    }

    public void onEvent (final Event_Battery_Ready a7adath_battery_ready) {

        if ((AndruavSettings.andruavWe7daBase.getIsCGS()) || (getSocketState()!= SOCKETSTATE_REGISTERED)) {
            return;
        }

        if (!a7adath_battery_ready.mAndruavWe7da.IsMe()) return ;

        long now = System.currentTimeMillis();

        AndruavDroneFacade.sendBatteryInfo(now);

    }



    public void onEvent (final Event_HomeLocation_Ready a7adath_homeLocation_ready) throws JSONException {
        if (getSocketState() != SOCKETSTATE_REGISTERED) {
            return;
        }

        if (!a7adath_homeLocation_ready.mAndruavWe7da.IsMe()) return ;

        AndruavFacade.sendHomeLocation(null);
    }



    public void onEvent (final Event_TargetLocation_Ready a7adath_targetLocation_ready) throws JSONException {
        if (getSocketState() != SOCKETSTATE_REGISTERED) {
            return;
        }

        if (!a7adath_targetLocation_ready.mAndruavWe7da.IsMe()) return ;


        AndruavFacade.sendTargetLocation(null);

    }


    @Override
    public void shutDown() {
        mkillMe = true;
        EventBus.getDefault().unregister(this);
        super.shutDown();
        AndruavEngine.setAndruavWS(null);
    }

    private void initHandlerClient () {

        mhandler.postDelayed(ScheduledSocket, 100);
    }

    /**
     * Send Telemetry message to target Drone
     * @param Data
     */
    public void sendTelemetryfromGCS(final byte[] Data,String telemetryTarget)
    {
        // TODO: needs to be moved to AndruavFacade

        if (getSocketState() != SOCKETSTATE_REGISTERED) {
            // TODO: ENH: Add notification here based o TIME
            return;
        }

        if (AndruavSettings.remoteTelemetryAndruavWe7da==null) return ; // no broadcast
        AndruavResalaBinary_LightTelemetry andruavMessage_telemetry = new AndruavResalaBinary_LightTelemetry();
        andruavMessage_telemetry.setData(Data);
        sendMessageToIndividual(andruavMessage_telemetry, telemetryTarget, false);

    }

    /***
     * Send Telemetry Message to one or more sources
     * @param Data
     */
    public void sendTelemetryfromDrone(final byte[] Data) {

        if (getSocketState() != SOCKETSTATE_REGISTERED) {
            // TODO: ENH: Add notification here based o TIME
            return;
        }

        int size = AndruavSettings.mTelemetryRequests.size();
        if (size ==0 ) return ;

        AndruavResalaBinary_LightTelemetry andruavMessageBinary_telemetry = new AndruavResalaBinary_LightTelemetry();
        andruavMessageBinary_telemetry.setData(Data);


        // TODO: improvment...u can check for dead units or sendMessageToModule individual message if size is one
        if (size >1)
        {
            broadcastMessageToGroup(andruavMessageBinary_telemetry, Boolean.FALSE);
        }
        else
        {
            AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavSettings.mTelemetryRequests.get(0);
            if (andruavUnit.getTelemetry_protocol() == TelemetryProtocol.TelemetryProtocol_No_Telemetry)
            {
                // server is nol longer selected
                // this is a safety check ... as telemetry server sends stopTelemetry message anyway.
                AndruavSettings.mTelemetryRequests.remove(andruavUnit);
                return ;
            }
            sendMessageToIndividual(andruavMessageBinary_telemetry, andruavUnit.PartyID, Boolean.FALSE); // time here can be used on the other side ofr calculation.
        }
    }


    private final Runnable ScheduledSocket = new Runnable() {
        @Override
        public void run() {
            /* do what you need to do */

            /*synchronized (telemetryBytesObject)
            {
                if (telemetryBytesIndex <= 0) return ;

                flashBufferedTelemetryData(telemetrySource);


            }*/

            if (!mkillMe) {
                mhandler.postDelayed(this, telemetryBufferTimeOut);
            }
        }
    };

    /***
     * Called by ScheduledTasks Runnable
     */
    @Override
    protected void onScheduledTasks(final long now)
    {
        // Tell Drone that I am listening for you, so if you restarted for any reason please restart data for me
        if ((now - monPingDroneTelemetry) > monPingDroneTelemetryDuration) {
            // Ping Drone Telemetry
            monPingDroneTelemetry = now;
            AndruavFacade.ResumeTelemetry(Constants.SMART_TELEMETRY_LEVEL_NEGLECT);
        }
    }




    @Override
    protected void onOpen ()
    {
        super.onOpen();

        EventBus.getDefault().post(new EventSocketState(EventSocketState.ENUM_SOCKETSTATE.onConnect, "Connected"));

        final Emergency emergency = (Emergency) AndruavEngine.getEmergency();

        if (emergency != null) {
            emergency.triggerConnectionEmergency(false);
        }

        if (Preference.isAutoUDPProxyConnect(null)) {
            // Start it if it is not started on server.
            AndruavFacade.StartUdpProxyTelemetry();
        }
        else
        {
            // stop any previous running UDP if you do not want them.
            // do not stop when exit to avoid changing ports.
            // stop when you do not need it.
            AndruavFacade.StopUdpProxyTelemetry();
        }

    }

    @Override
    protected void onBinaryMessage(final AndruavBinary_2MR andruavBinary2MR)
    {

        final AndruavUnitShadow andruavUnit =(AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(andruavBinary2MR.partyID);
        if (andruavUnit == null) {
            //no need tp make anything here, as logic of adding new units
            // should be in App.andruavWSClient.andruavUnitMap.get function.
            return;
        }

        if (andruavBinary2MR.andruavResalaBinaryBase.messageTypeID == AndruavResalaBinary_IMG.TYPE_AndruavMessage_IMG) {
            final AndruavResalaBinary_IMG andruavMessage_img = (AndruavResalaBinary_IMG) andruavBinary2MR.andruavResalaBinaryBase;
            final Event_FPV_Image event_fpv_image = new Event_FPV_Image();
            event_fpv_image.Sender = andruavUnit.PartyID;
            event_fpv_image.andruavUnit = andruavUnit;
            event_fpv_image.isLocalImage = false;
            event_fpv_image.isVideo = false;
            event_fpv_image.ImageBytes = andruavMessage_img.getImage();
            event_fpv_image.Description = andruavMessage_img.Description;
            event_fpv_image.ImageLocation = andruavMessage_img.ImageLocation;

            EventBus.getDefault().post(event_fpv_image);

            andruavBinary2MR.processed = true;

        }
    }



    @Override
    protected void onTextMessage(final Andruav_2MR andruav2MR) {



    }



    @Override
    protected void onClose(final int code, final String reason)
    {
        super.onClose(code, reason);

        EventBus.getDefault().post(new EventSocketState(EventSocketState.ENUM_SOCKETSTATE.onDisconnect, reason));


    }


    /**
     * @param code   public static final int CLOSE_NORMAL = 1;
     *               public static final int CLOSE_CANNOT_CONNECT = 2;
     *               public static final int CLOSE_CONNECTION_LOST = 3;
     *               public static final int CLOSE_PROTOCOL_ERROR = 4;
     *               public static final int CLOSE_INTERNAL_ERROR = 5;
     * @param reason
     */

    @Override
    protected void onError(final int code, final String reason)
    {
        final Emergency emergency = (Emergency) AndruavEngine.getEmergency();
        if (emergency != null) {
            emergency.triggerEmergencyFlightModeFaileSafe(false);
            emergency.sendSMS(false);
            emergency.triggerConnectionEmergency(true);
        }

        // TODO: Need to handle this as the server is down
        EventBus.getDefault().post(new EventSocketState(EventSocketState.ENUM_SOCKETSTATE.onError, reason));
    }


    @Override
    protected void reconnect ()
    {
        super.reconnect();
        try {
            LoginClient.ValidateAccount(Preference.getLoginUserName(null), Preference.getLoginAccessCode(null), Preference.getWebServerGroupName(null), new ILoginClientCallback() {
                @Override
                public void onError() {
                    if (merrorRecovery == Boolean.TRUE) {
                        if (mhandler == null) return ; // should fix fatal issue.
                        mhandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (getSocketState() == SOCKETSTATE_REGISTERED)
                                {
                                    return ; // just an old retry
                                }
                                if (mkillMe) return;

                                final Emergency emergency = (Emergency) AndruavEngine.getEmergency();
                                if (emergency != null) {
                                    emergency.triggerEmergencyFlightModeFaileSafe(false);
                                    emergency.sendSMS(false); // still cannot connect
                                    emergency.triggerConnectionEmergency(false);
                                }
                                //BUG: if multiservers and server is down retries will focus on one server.... you should query the auth server.

                                reconnect();
                            }
                        }, 4000);
                    }
                }

                @Override
                public void onSuccess(EventLoginClient eventLoginClient) {
                    if (eventLoginClient.LastError == LoginClient.ERR_SUCCESS) {

                        AndruavSettings.Account_SID = eventLoginClient.Parameters.get(LoginClient.CONST_SENDER_ID);
                        AndruavSettings.WebServerURL = eventLoginClient.Parameters.get(LoginClient.CONST_COMM_SERVER_PUBLIC_HOST);
                        AndruavSettings.WebServerPort = eventLoginClient.Parameters.get(LoginClient.CONST_COMM_SERVER_PORT);
                        AndruavSettings.WEBMOFTA7 = eventLoginClient.Parameters.get(LoginClient.CONST_COMM_SERVER_LOGIN_TEMP_KEY);

                        String websocketURL = "wss://" + LoginClient.getWSURL();
                        Me.connect(websocketURL);
                    } else {
                        if (merrorRecovery == Boolean.TRUE) {
                            mhandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (mkillMe) return;


                                    //BUG: if multiservers and server is down retries will focus on one server.... you should query the auth server.
                                    final Emergency emergency = (Emergency) AndruavEngine.getEmergency();
                                    if (emergency != null) {
                                        emergency.triggerEmergencyFlightModeFaileSafe(false);
                                        emergency.sendSMS(false); // still cannot connect
                                        emergency.triggerConnectionEmergency(true);
                                    }

                                    reconnect();
                                }
                            }, 4000);
                        }
                    }
                }
            });
        }catch(UnsupportedEncodingException e){
            e.printStackTrace();
        }
    }



    public AndruavWSClient_TooTallNate(final String uri, final ContentValues extraHeaders) {
        super(uri,extraHeaders);


        EventBus.getDefault().register(this,1);

        initHandlerClient();
    }


    /***
     * Executes commands embedded in the message section.
     * @param andruav_2MR
     */
    @Override
    protected void executeInternalBinaryCommand(final AndruavBinary_2MR andruav_2MR) {
        switch (andruav_2MR.andruavResalaBinaryBase.messageTypeID)
        {

            case AndruavResalaBinary_IMUStatistics.TYPE_AndruavMessage_IMUStatistics:
                // Update Statistics

                andruav_2MR.processed = true;
                AndruavEngine.getAndruavWe7daMapBase().updateIMUStatistics(andruav_2MR);

                andruav_2MR.processed = true;

                break;

            case AndruavResalaBinary_LightTelemetry.TYPE_AndruavMessage_LightTelemetry: {
                andruav_2MR.processed = true;
                final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(andruav_2MR.partyID);
                if (andruavUnit == null) {
                    // telemetry from Unknown unit ignore it
                    andruav_2MR.processed = true;
                    break;
                }
                AndruavResalaBinary_LightTelemetry andruavResalaBinary_lightTelemetry = (AndruavResalaBinary_LightTelemetry) andruav_2MR.andruavResalaBinaryBase;

                if (AndruavSettings.andruavWe7daBase.getIsCGS())
                {
                    Event_FCBData event_FCBData = new Event_FCBData();
                    event_FCBData.senderWe7da = andruavUnit;
                    event_FCBData.IsLocal = Event_SocketData.SOURCE_REMOTE;
                    event_FCBData.Data = andruavResalaBinary_lightTelemetry.getData();
                    event_FCBData.DataLength = event_FCBData.Data.length;
                    EventBus.getDefault().post(event_FCBData);
                }
                else {
                    Event_SocketData event_socketData = new Event_SocketData();
                    event_socketData.senderWe7da = andruavUnit;
                    event_socketData.IsLocal = Event_SocketData.SOURCE_REMOTE;
                    event_socketData.Data = andruavResalaBinary_lightTelemetry.getData();
                    event_socketData.DataLength = event_socketData.Data.length;
                    EventBus.getDefault().post(event_socketData);

                }
                andruav_2MR.processed = true;
                break;
            }

            /*case AndruavResalaBinary_Telemetry.TYPE_AndruavMessage_Telemetry: {

                andruav_2MR.processed = true;
                final AndruavWe7daShadow andruavUnit = (AndruavWe7daShadow) AndruavMo7arek.getAndruavWe7daMapBase().get(andruav_2MR.senderName);
                if (andruavUnit==null)
                {
                    // telemetry from Unknown unit ignore it
                    andruav_2MR.processed = true;
                    break;
                }
                AndruavResalaBinary_Telemetry andruavMessage_telemetry = (AndruavResalaBinary_Telemetry) andruav_2MR.andruavResalaBinaryBase;

                int source = andruavMessage_telemetry.getSource();
                if (source == AndruavResalaBinary_Telemetry.SOURCE_FCB) {   // Data from Drone .... This part of Code executed only in GCS Mode.
                    // SOME TIMES Drone broadcasts Telemetry ... that means it will reach other Drone ...
                    if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
                        Event_FCBData event_FCBData = new Event_FCBData();
                        event_FCBData.senderWe7da = andruavUnit;
                        event_FCBData.IsLocal = Event_SocketData.SOURCE_REMOTE;
                        event_FCBData.Data = andruavMessage_telemetry.getData();
                        event_FCBData.DataLength = event_FCBData.Data.length;
                        EventBus.getDefault().post(event_FCBData);
                    }

                } else if (source == AndruavResalaBinary_Telemetry.SOURCE_GCS) {   // Data from GCS .... This part of Code executed only in Drone Mode.
                    Event_SocketData event_socketData = new Event_SocketData();
                    event_socketData.senderWe7da = andruavUnit;
                    event_socketData.IsLocal = Event_SocketData.SOURCE_REMOTE;
                    event_socketData.Data = andruavMessage_telemetry.getData();
                    event_socketData.DataLength = event_socketData.Data.length;
                    EventBus.getDefault().post(event_socketData);

                }

                andruav_2MR.processed = true;

            }
            break;*/



//            case AndruavResalaBinary_RemoteControl.TYPE_AndruavMessage_RemoteControl:
//                final AndruavWe7daShadow andruavUnit = (AndruavWe7daShadow) AndruavMo7arek.getAndruavWe7daMapBase().get(andruav_2MR.partyID);
//                if ((andruavUnit!=null) && (!AndruavSettings.andruavWe7daBase.canControl()))
//                {
//                    andruav_2MR.processed = true;
//                }
//
//                EventRemote_ChannelsCMD eventRemote_Channels_cmd = new EventRemote_ChannelsCMD(
//                        RemoteControl.calculateChannels2(((AndruavResalaBinary_RemoteControl) andruav_2MR.andruavResalaBinaryBase).getChannelsCopy(),false));
//                // DONT CHANGE VALUE then UNCOMMENT      ((AndruavResalaBinary_RemoteControl) andruav_2MR.andruavResalaBinaryBase).getChannelsCopy());
//
//                eventRemote_Channels_cmd.PartyID = AndruavSettings.andruavWe7daBase.PartyID; // ME
//                eventRemote_Channels_cmd.Engaged = ((AndruavResalaBinary_RemoteControl)andruav_2MR.andruavResalaBinaryBase).isEngaged;
//                EventBus.getDefault().post(eventRemote_Channels_cmd);
//
//                andruav_2MR.processed = true;
//
//                break;


            default:
                // Other message are either reply, IMU or other info data.
        }

    }








    /***
     * Executes commands embedded in the message section.
     * @param andruav2MR
     */
    @Override
    protected void executeInternalCommand(final Andruav_2MR andruav2MR) {


        switch (andruav2MR.andruavMessageBase.messageTypeID)
        {

            case AndruavMessage_Config_Preference.TYPE_AndruavResala_Config_Preference:

                andruav2MR.processed = true;
                final AndruavMessage_Config_Preference andruavResala_config_preference = (AndruavMessage_Config_Preference) andruav2MR.andruavMessageBase;

                switch (andruavResala_config_preference.TValue)
                {
                    case 1:
                        Preference.setPreference(null,andruavResala_config_preference.KEYID,andruavResala_config_preference.IValue);
                        break;
                    case 2:
                        Preference.setPreference(null,andruavResala_config_preference.KEYID,andruavResala_config_preference.BValue);
                        break;
                    case 3:
                        Preference.setPreference(null,andruavResala_config_preference.KEYID,andruavResala_config_preference.SValue);
                        break;
                }

                break;

            case AndruavMessage_Config_UnitID.TYPE_AndruavMessage_Config_UnitID:
                andruav2MR.processed = true;

                // This is a request from outside [Drone or GCS] to all or a specific
                // fence detail. fencename is specific is sent in a variable "fn" -fence name-.
                if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
                    final AndruavMessage_Config_UnitID andruavResala_Config_unitID = (AndruavMessage_Config_UnitID) andruav2MR.andruavMessageBase;
                    AndruavSettings.andruavWe7daBase.UnitID = andruavResala_Config_unitID.UnitID.toLowerCase();
                    AndruavSettings.andruavWe7daBase.Description = andruavResala_Config_unitID.Description;
                    AndruavSettings.andruavWe7daBase.GroupName = andruavResala_Config_unitID.GroupName.toLowerCase();

                    // Save permanent
                    Preference.setWebServerUserName(null,AndruavSettings.andruavWe7daBase.UnitID);
                    Preference.setWebServerUserDescription(null, andruavResala_Config_unitID.Description);
                    Preference.setWebServerGroupName(null, andruavResala_Config_unitID.GroupName);

                    // BUG: CALL RECONNECT HERE

                }
                break;

            case AndruavMessage_Config_COM.TYPE_AndruavResala_Config_COM:
            {
                andruav2MR.processed = true;
                if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
                    final AndruavMessage_Config_COM andruavResala_config_com = (AndruavMessage_Config_COM) andruav2MR.andruavMessageBase;

                    // YOU DONT NEED TO RESET [AndruavSettings.WebServerURL .... ]
                    // You NEED TO RECONNECT.

                    Preference.setWebServerPort(null,andruavResala_config_com.Port);
                    Preference.setWebServerURL(null,andruavResala_config_com.ServerIP);
                    Preference.isLocalServer(null,andruavResala_config_com.IsLocalServer);
                    Preference.setWebServerPort(null,andruavResala_config_com.Port);

                    // BUG: CALL RECONNECT HERE
                }
            }
            break;

            case AndruavMessage_DroneReport.TYPE_AndruavMessage_DroneReport:
            {
                andruav2MR.processed = true;
                final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(andruav2MR.partyID);
                if (andruavUnit != null) {
                    AndruavMessage_DroneReport andruavMessage_droneReport = (AndruavMessage_DroneReport) andruav2MR.andruavMessageBase;

                    try {
                        final MissionBase missionBase = andruavUnit.getMohemmaMapBase().valueAt(andruavMessage_droneReport.mParameter1);
                        if (missionBase !=null)
                        {
                            missionBase.Status = andruavMessage_droneReport.mReportType;

                        }
                        else
                        {
                            AndruavFacade.requestWayPoints(andruavUnit);
                        }

                    }
                    catch (final  java.lang.ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException)
                    {
                        // ignore
                        // TODO: request new Mission Map
                    }
                    catch (final Exception ex)
                    {
                        AndruavEngine.log().logException("apm_mission", ex);
                    }
                    EventBus.getDefault().post(new Event_WayPointReached(andruavUnit, andruavMessage_droneReport.mParameter1)); // inform all that a data is ready


                }

            }
            break;



            case AndruavMessage_Error.TYPE_AndruavMessage_Error: {
                andruav2MR.processed = true;
                final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(andruav2MR.partyID);
                if ((andruavUnit != null) && (AndruavSettings.andruavWe7daBase.getIsCGS())) {

                    // dont display errors of other units in a  mobile working in drone mode. very confusing.
                    AndruavMessage_Error andruavMessage_error = ((AndruavMessage_Error) (andruav2MR.andruavMessageBase));
                    String err = andruavMessage_error.Description;
                    App.notification.displayNotification(andruavMessage_error.notification_Type, andruavUnit.UnitID, err, true, andruavMessage_error.infoType, false);
                    AndruavEngine.notification().Speak(err);
                }
            }
            break;


            case AndruavMessage_ServoChannel.TYPE_AndruavMessage_ServoChannel:
                andruav2MR.processed = true;
                Event_RemoteServo event_remoteServo = new Event_RemoteServo(((AndruavMessage_ServoChannel) andruav2MR.andruavMessageBase).channelNumber, ((AndruavMessage_ServoChannel) andruav2MR.andruavMessageBase).channelValue);
                event_remoteServo.PartyID = AndruavSettings.andruavWe7daBase.PartyID; // ME
                EventBus.getDefault().post(event_remoteServo);

                break;

            case AndruavMessage_CameraList.TYPE_AndruavCMD_CameraList: {
                andruav2MR.processed = true;
                final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(andruav2MR.partyID);

                if (andruavUnit == null) break;

                AndruavMessage_CameraList andruavMessage_cameraList = (AndruavMessage_CameraList) andruav2MR.andruavMessageBase;

                if (andruavMessage_cameraList.isReply) {
                    // UAVIS can sendMessageToModule isReply = false to inform GCS about available cameras.



                    Event_FPV_VideoURL event_fpv_videoURL = new Event_FPV_VideoURL();
                    event_fpv_videoURL.andruavUnit = andruavUnit;
                    event_fpv_videoURL.ExternalType = andruavMessage_cameraList.ExternalType;
                    event_fpv_videoURL.VideoTracks = andruavMessage_cameraList.videoTracks;
                    event_fpv_videoURL.isReply = andruavMessage_cameraList.isReply;
                    EventBus.getDefault().post(event_fpv_videoURL);
                }
            }
            break;

            case AndruavMessage_Sound_TextToSpeech.TYPE_AndruavMessage_Sound_TextToSpeech:
                andruav2MR.processed = true;
                AndruavMessage_Sound_TextToSpeech andruavMessage_textToSpeech = (AndruavMessage_Sound_TextToSpeech) andruav2MR.andruavMessageBase;
                AndruavEngine.notification().SpeakNow(andruavMessage_textToSpeech.text);
                break;

            default:
                // Other message are either reply, IMU or other info data.
                // andruav2MR.processed = false; << THIS IS A BUG
                // just update the unit... some commands are not executed here
                // such as TYPE_AndruavMessage_Telemetry
                //andruavUnitMap.updateLastActiveTime (andruav2MR.PartyID);
        }
    }



    /***
     * Executes commands sent from remote users via [TYPE_AndruavMessage_RemoteExecute]
     * @param andruav_2MR
     */
    @Override
    protected void executeRemoteExecuteCMD(final Andruav_2MR andruav_2MR) {
        try {

            Event_FPV_CMD event_fpv_cmd;

            switch (andruav_2MR.andruavMessageBase.messageTypeID) {

                // this is data from Another Andruav
                case AndruavMessage_RemoteExecute.TYPE_AndruavMessage_RemoteExecute: {


                    AndruavMessage_RemoteExecute andruavResala_remoteExecute = ((AndruavMessage_RemoteExecute) (andruav_2MR.andruavMessageBase));
                    int CMD_ID = andruavResala_remoteExecute.RemoteCommandID;
                    final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(andruav_2MR.partyID);

                    switch (CMD_ID) {


                        case AndruavMessage_UDPProxy_Info.TYPE_AndruavMessage_UdpProxy_Info:
                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break;
                            AndruavFacade.sendUdpProxyStatus(andruavUnit);
                            break;

                        case AndruavMessage_CameraList.TYPE_AndruavCMD_CameraList:
                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break; // not a valid command to GCSevent_fpv_cmd = new _7adath_FPV_CMD(_7adath_FPV_CMD.FPV_CMD_TAKEIMAGE);

                            AndruavDroneFacade.sendCameraList(true,andruavUnit);
                            EventBus.getDefault().post(new _7adath_InitAndroidCamera());
                            break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_MAKETILT:

                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break;
                            EventBus.getDefault().post(new Event_IMU_CMD(Event_IMU_CMD.IMU_CMD_UpdateZeroTilt));
                            break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_MAKEBEEP: {
                            if ((andruavUnit != null) && (!andruavUnit.canControl())) break;
                            final Emergency emergency = (Emergency) AndruavEngine.getEmergency();
                            if (emergency != null) {
                                emergency.triggerSirenByGCS(!emergency.getIsSirenActive());
                            }
                        }
                        break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_MAKEFLASH: {
                            if ((andruavUnit != null) && (!andruavUnit.canControl())) break;
                            final Emergency emergency = (Emergency) AndruavEngine.getEmergency();
                            if (emergency != null) {
                                emergency.triggerFlashByGCS(!emergency.getIsFlashing());
                            }
                        }
                        break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_TAKEIMAGE:
                            // DEPRECATED As command is too complex to me included in another command
                            break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_SET_GPS_SOURCE:
                            if ((andruavUnit != null) && (!andruavUnit.canControl())) break;
                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break;
                            AndruavSettings.andruavWe7daBase.setGPSMode(Integer.parseInt(((AndruavMessage_RemoteExecute) (andruav_2MR.andruavMessageBase)).Variables.get("s")));
                            break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_CONNECT_FCB:
                            if ((andruavUnit != null) && (!(andruavUnit.canControl() || andruavUnit.canTelemetry()))) break;
                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break;
                            final _7adath_FCB_2AMR adath_fcb_2AMR = new _7adath_FCB_2AMR();
                            adath_fcb_2AMR.enForceConnection = true;
                            EventBus.getDefault().post(adath_fcb_2AMR);
                            break;


                        // StartStop recording video
                        case AndruavMessage_RemoteExecute.RemoteCommand_RECORDVIDEO:
                            if ((andruavUnit != null) && (!andruavUnit.canVideo())) break;
                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break;

                            event_fpv_cmd = new Event_FPV_CMD(Event_FPV_CMD.FPV_CMD_RECORDVIDEO);
                            event_fpv_cmd.Requester = andruavUnit;
                            event_fpv_cmd.ACT = andruavResala_remoteExecute.getBooleanValue("Act");
                            if (Preference.useExternalCam(null)) {
                                // new IPWebCamImage(event_fpv_cmd);
                                PanicFacade.cannotStartCamera(INotification.NOTIFICATION_TYPE_ERROR, INotification.NOTIFICATION_TYPE_ERROR, "Record Video is not supported in this video mode yet", null);

                            } else {
                                EventBus.getDefault().post(event_fpv_cmd);
                            }
                            break;


                        case AndruavMessage_RemoteExecute.RemoteCommand_STREAMVIDEORESUME:
                            if ((andruavUnit != null) && (!andruavUnit.canVideo())) break;
                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break;
                            break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_ChangeUnitID:

                            break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_STREAMVIDEO:
                            if ((andruavUnit != null) && (!andruavUnit.canVideo())) break; // not permitted
                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break;

                            if (andruavUnit != null) {

                                if (andruavResala_remoteExecute.getBooleanValue("Act")) {
                                    EventBus.getDefault().post(new _7adath_InitAndroidCamera());

                                    if (AndruavSettings.mVideoRequests.get(andruavUnit.PartyID) == null) {

                                        // add requester to the List
                                        AndruavSettings.mVideoRequests.put(andruavUnit.PartyID, andruavUnit);
                                    }
                                } else {
                                    // this is WRONG not after UAVOS update
                                    AndruavSettings.mVideoRequests.remove(andruavUnit.PartyID);
                                }


                                if (andruavResala_remoteExecute.Variables.containsKey("CH")
                                        && andruavResala_remoteExecute.Variables.get("CH").equals(AndruavSettings.andruavWe7daBase.PartyID))
                                {

                                    return ;
                                }
                                ((AndruavUDPServerBase) AndruavEngine.getAndruavUDP()).sendMessageToModule(andruav_2MR);

                            } else {
                                //TODO: enh upi may create a temp record just to be able to sendMessageToModule data to this unit before it replies with full data
                                // sendMessageToModule Asking for Info
                                AndruavFacade.requestID(andruav_2MR.partyID);
                            }
                            // should be called last as FPV form checks on mVideoRequests to turn on video or not.
                            // So this function should eb called anyway unless u use external cam.

                            andruav_2MR.processed = true;

                            break;


                        case AndruavMessage_RemoteExecute.RemoteCommand_ROTATECAM:
                            if ((andruavUnit != null) && (!andruavUnit.canVideo())) break;
                            //sendMessageToModule internal command to FPV activity
                            event_fpv_cmd = new Event_FPV_CMD(Event_FPV_CMD.FPV_CMD_ROTATECAM);
                            event_fpv_cmd.Requester = andruavUnit;
                            EventBus.getDefault().post(event_fpv_cmd);

                            andruav_2MR.processed = true;
                            break;


                        case AndruavMessage_RemoteExecute.RemoteCommand_IMUCTRL:
                            if (andruavUnit != null) {
                                if (andruavResala_remoteExecute.getBooleanValue("Act")) {
                                    if (!AndruavSettings.mIMURequests.contains(andruavUnit)) {
                                        AndruavSettings.mIMURequests.add(andruavUnit);
                                    }
                                } else {
                                    AndruavSettings.mIMURequests.remove(andruavUnit);
                                }
                            } else {
                                //TODO: enh upi may create a temp record just to be able to sendMessageToModule data to this unit before it replies with full data
                                // sendMessageToModule Asking for Info
                                AndruavFacade.requestID(andruav_2MR.partyID);
                            }

                            andruav_2MR.processed = true;

                            break;


                        case AndruavMessage_RemoteExecute.RemoteCommand_TELEMETRYCTRL:
                            if (andruavUnit != null)  {
                                final int request = andruavResala_remoteExecute.getIntValue("Act");
                                switch (request)
                                {
                                    case Event_TelemetryGCSRequest.REQUEST_END: {
                                        if (AndruavSettings.mTelemetryRequests.contains(andruavUnit)) {
                                            AndruavSettings.mTelemetryRequests.remove(andruavUnit);
                                            AndruavEngine.getEventBus().post(new Event_TelemetryGCSRequest(andruavUnit, Event_TelemetryGCSRequest.REQUEST_END));
                                        }
                                    }
                                    break;

                                    case Event_TelemetryGCSRequest.ADJUST_RATE: {
                                        final int LVL = andruavResala_remoteExecute.getIntValue("LVL", Constants.SMART_TELEMETRY_LEVEL_NEGLECT);
                                        if (LVL != Constants.SMART_TELEMETRY_LEVEL_NEGLECT) {
                                            Preference.setSmartMavlinkTelemetry(null, LVL);
                                        }
                                    }
                                        break;
                                    case Event_TelemetryGCSRequest.REQUEST_RESUME: {
                                        AndruavEngine.getUDPProxy().setPause(false);
                                    }
                                    break;
                                    case Event_TelemetryGCSRequest.REQUEST_PAUSE: {
                                        AndruavEngine.getUDPProxy().setPause(true);
                                    }
                                    break;
                                    default:
                                    {
                                        if (andruavUnit.canTelemetry()) {
                                            // add or resume both make sure they are added in our request list
                                            if (!AndruavSettings.mTelemetryRequests.contains(andruavUnit)) {
                                                AndruavSettings.mTelemetryRequests.add(andruavUnit);
                                                AndruavEngine.getEventBus().post(new Event_TelemetryGCSRequest(andruavUnit, Event_TelemetryGCSRequest.REQUEST_START));
                                            } else {
                                                AndruavEngine.getEventBus().post(new Event_TelemetryGCSRequest(andruavUnit, request));
                                            }

                                            // Here Dont SEND ... That is why I replicated [AndruavMo7arek.getEventBus().post(new _7adath_TelemetryGCSRequest(andruavUnit,add ));]  instead of making a bool value
                                            // AndruavMo7arek.getEventBus().post(new _7adath_TelemetryGCSRequest(andruavUnit,add ));
                                            //

                                            // Update Smart Telemetry Level if Requested
                                            // Adjust Rate also if required
                                            final int LVL = andruavResala_remoteExecute.getIntValue("LVL", Constants.SMART_TELEMETRY_LEVEL_NEGLECT);
                                            if (LVL != Constants.SMART_TELEMETRY_LEVEL_NEGLECT) {
                                                Preference.setSmartMavlinkTelemetry(null, LVL);
                                            }
                                        }
                                    }
                                        break;
                                }

                            } else {
                                //TODO: enh upi may create a temp record just to be able to sendMessageToModule data to this unit before it replies with full data
                                // sendMessageToModule Asking for Info
                                AndruavFacade.requestID(andruav_2MR.partyID);
                            }

                            andruav_2MR.processed = true;

                            break;


                        case AndruavMessage_RemoteExecute.RemoteCommand_SENDSMS:
                            if ((andruavUnit != null) && (!andruavUnit.canControl())) break;

                            // sendMessageToModule SMS immediate long as there is a current mLocation defined.
                            andruav_2MR.processed = true;
                            final Emergency emergency = (Emergency) AndruavEngine.getEmergency();
                            if (emergency != null) {
                                emergency.sendSMS(true);
                            }


                            break;


                        default:
                            // unknown command ... maybe a new protocol version
                            // just ignore
                            //TODO: sendMessageToModule message UNKNOWN command to sender to understand that he is talking new language
                            break;
                    }
                }
                break;

                case AndruavMessage_Ctrl_Camera.TYPE_AndruavResala_Ctrl_Camera:
                    final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(andruav_2MR.partyID);
                    if ((andruavUnit != null) && (!andruavUnit.canImage())) break;

                    andruav_2MR.processed = true;

                    final AndruavMessage_Ctrl_Camera andruavResala_ctrl_camera = (AndruavMessage_Ctrl_Camera) (andruav_2MR.andruavMessageBase);
                    if (AndruavSettings.andruavWe7daBase.getIsCGS())
                        break;


                    EventBus.getDefault().post(new _7adath_InitAndroidCamera());

                    event_fpv_cmd = new Event_FPV_CMD(Event_FPV_CMD.FPV_CMD_TAKEIMAGE);

                    event_fpv_cmd.CameraSource    = andruavResala_ctrl_camera.CameraSource;
                    event_fpv_cmd.NumberOfImages    = andruavResala_ctrl_camera.NumberOfImages;
                    event_fpv_cmd.TimeBetweenShotes = andruavResala_ctrl_camera.TimeBetweenShotes;
                    event_fpv_cmd.DistanceBetweenShotes = andruavResala_ctrl_camera.DistanceBetweenShotes;
                    event_fpv_cmd.SendBackImages =  andruavResala_ctrl_camera.SendBackImages;

                    event_fpv_cmd.Requester = andruavUnit;

                    AndruavMeFacade.Ctrl_Camera(event_fpv_cmd);



                    break;
            }
        } catch (Exception ex) {
            AndruavEngine.log().logException("RemoteExecuteCMD", ex);
        }
    }

}
