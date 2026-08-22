package ap.andruav_ap.communication;

import org.greenrobot.eventbus.Subscribe;

import android.content.ContentValues;

import com.andruav.AndruavDroneFacade;
import com.andruav.AndruavFacade;
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
import com.andruav.event.networkEvent.EventLoginClient;
import com.andruav.event.networkEvent.EventSocketState;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.controlBoard.shared.geoFence.GeoFenceBase;
import com.andruav.controlBoard.shared.geoFence.GeoFenceManager;
import com.andruav.protocol.commands.binaryMessages.AndruavBinary_2MR;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_LightTelemetry;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;
import com.andruav.protocol.communication.websocket.AndruavWSClientBase_TooTallNate;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;

import org.greenrobot.eventbus.EventBus;
import ap.andruav_ap.Emergency;
import ap.andruavmiddlelibrary.ILoginClientCallback;
import ap.andruavmiddlelibrary.LoginClient;
import com.andruav.event.systemEvent.Event_ShutDown_Signalling;
import ap.andruavmiddlelibrary.preference.Preference;

public class AndruavWSClient_TooTallNate extends AndruavWSClientBase_TooTallNate {

    /**
     * Owns all inbound message dispatch logic (the former big switches on
     * message type). The WebSocket client delegates the
     * {@code executeInternal*}/{@code executeRemoteExecuteCMD}/
     * {@code onBinaryMessage(AndruavBinary_2MR)} hooks to this collaborator so
     * that this class stays focused on transport + outbound EventBus handling.
     */
    private final MessageDispatcher messageDispatcher;


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

    @Subscribe(priority = 1)
    public void onEvent (final Event_ShutDown_Signalling event)
    {
        if (event.CloseOrder != 3) return ;

        AndruavSettings.andruavWe7daBase.setShutdown(true);

        this.shutDown();

    }

    @Subscribe(priority = 1)
    public void onEvent (Event_GeoFence_Hit a7adath_geoFence_hit)
    {
        if (a7adath_geoFence_hit.andruavUnitBase.IsMe()) {
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


    @Subscribe(priority = 1)
    public void onEvent (Event_GeoFence_Ready a7adath_geoFence_ready)
    {
        final AndruavUnitBase andruavUnitBase = a7adath_geoFence_ready.andruavWe7da;
        if(andruavUnitBase.IsMe())
        {

            // Here we broad cast thet this drone is only attached to a fence by fenceName that is it.
            AndruavFacade.sendGeoFenceAttach(a7adath_geoFence_ready.fenceName,true,null);
        }

    }



    @Subscribe(priority = 1)
    public void onEvent (final Event_GPS_Ready a7adath_gps_ready)  {
        if (!a7adath_gps_ready.mAndruavWe7da.IsMe()) return ;

        if (getSocketState() != SOCKETSTATE_REGISTERED) {
            return;
        }

        AndruavDroneFacade.handleGPSInfo();
    }


    @Subscribe(priority = 1)
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
    @Subscribe(priority = 1)
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



    @Subscribe(priority = 1)
    public void onEvent (final Event_IMU_Ready a7adath_imu_ready) throws JSONException {

        if (getSocketState() != SOCKETSTATE_REGISTERED) {
            return;
        }

        if (!a7adath_imu_ready.mAndruavWe7da.IsMe()) return ;


        if (AndruavSettings.andruavWe7daBase.useFCBIMU()) return ; // it should be true

        final long now = System.currentTimeMillis();
        AndruavDroneFacade.sendIMUInfo(now);

    }

    @Subscribe(priority = 1)
    public void onEvent (final Event_Battery_Ready a7adath_battery_ready) {

        if ((AndruavSettings.andruavWe7daBase.getIsCGS()) || (getSocketState()!= SOCKETSTATE_REGISTERED)) {
            return;
        }

        if (!a7adath_battery_ready.mAndruavWe7da.IsMe()) return ;

        long now = System.currentTimeMillis();

        AndruavDroneFacade.sendBatteryInfo(now);

    }



    @Subscribe(priority = 1)
    public void onEvent (final Event_HomeLocation_Ready a7adath_homeLocation_ready) throws JSONException {
        if (getSocketState() != SOCKETSTATE_REGISTERED) {
            return;
        }

        if (!a7adath_homeLocation_ready.mAndruavWe7da.IsMe()) return ;

        AndruavFacade.sendHomeLocation(null);
    }



    @Subscribe(priority = 1)
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

        // Auto UDP proxy start/stop happens once the socket is actually REGISTERED
        // (see App.UIHandler's onRegistered handling) - at onOpen() the server hasn't
        // confirmed registration yet, so sendSystemCommandToCommServer() would silently
        // drop the request.
    }

    @Override
    protected void onBinaryMessage(final AndruavBinary_2MR andruavBinary2MR)
    {
        // Delegated to MessageDispatcher — see communication/MessageDispatcher.java.
        messageDispatcher.onBinaryMessage(andruavBinary2MR);
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
                    if (mkillMe) {
                        // user stopped retrying while this validate-account call was in flight.
                        return;
                    }
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

        messageDispatcher = new MessageDispatcher();

        EventBus.getDefault().register(this);

        initHandlerClient();
    }


    /***
     * Executes commands embedded in the message section.
     * @param andruav_2MR
     */
    @Override
    protected void executeInternalBinaryCommand(final AndruavBinary_2MR andruav_2MR) {
        // Delegated to MessageDispatcher — see communication/MessageDispatcher.java.
        messageDispatcher.executeInternalBinaryCommand(andruav_2MR);
    }








    /***
     * Executes commands embedded in the message section.
     * @param andruav2MR
     */
    @Override
    protected void executeInternalCommand(final Andruav_2MR andruav2MR) {
        // Delegated to MessageDispatcher — see communication/MessageDispatcher.java.
        messageDispatcher.executeInternalCommand(andruav2MR);
    }



    /***
     * Executes commands sent from remote users via [TYPE_AndruavMessage_RemoteExecute]
     * @param andruav_2MR
     */
    @Override
    protected void executeRemoteExecuteCMD(final Andruav_2MR andruav_2MR) {
        // Delegated to MessageDispatcher — see communication/MessageDispatcher.java.
        messageDispatcher.executeRemoteExecuteCMD(andruav_2MR);
    }

}
