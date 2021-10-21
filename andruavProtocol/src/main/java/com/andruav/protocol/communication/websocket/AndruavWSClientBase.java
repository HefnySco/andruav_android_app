package com.andruav.protocol.communication.websocket;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.andruav.AndruavInternalCommands;
import com.andruav.AndruavDroneFacade;
import com.andruav.AndruavFacade;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.AndruavTaskManager;
import com.andruav.event.Event_Remote_ChannelsCMD;
import com.andruav.event.droneReport_7adath._7adath_Battery_Ready;
import com.andruav.event.droneReport_7adath._7adath_GPS_Ready;
import com.andruav.event.droneReport_7adath._7adath_GeoFence_Hit;
import com.andruav.event.droneReport_7adath._7adath_HomeLocation_Ready;
import com.andruav.event.droneReport_7adath._7adath_IMU_Ready;
import com.andruav.event.droneReport_7adath._7adath_RemoteControlSettingsReceived;
import com.andruav.event.droneReport_7adath._7adath_Signalling;
import com.andruav.event.droneReport_7adath._7adath_TRK_Target_Lost;
import com.andruav.event.droneReport_7adath._7adath_TRK_Target_Ready;
import com.andruav.event.droneReport_7adath._7adath_TRK_Target_Stop;
import com.andruav.event.droneReport_7adath._7adath_TargetLocation_Ready;
import com.andruav.event.droneReport_7adath._7adath_WayPointsRecieved;
import com.andruav.event.droneReport_7adath._7adath_WayPointsUpdated;
import com.andruav.event.droneReport_7adath._7adath_CameraZoom;
import com.andruav.event.fcb_7adath._7adath_FCB_RemoteControlSettings;
import com.andruav.event.fpv7adath._7adath_FPV_CMD;
import com.andruav.event.networkEvent.EventSocketState;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.controlBoard.ControlBoardBase;
import com.andruav.controlBoard.shared.geoFence.GeoFenceBase;
import com.andruav.controlBoard.shared.geoFence.GeoFenceManager;
import com.andruav.controlBoard.shared.missions.MohemmaMapBase;
import com.andruav.protocol.commands.Andruav_Parser;
import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.protocol.commands.binaryMessages.AndruavBinary_2MR;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinaryBase;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_ExternalCommand_WayPoints;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMU;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_RemoteControlSettings;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_WayPoints;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_WayPointsUpdates;
import com.andruav.protocol.commands.textMessages.AndruavMessageBase;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraFlash;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraSwitch;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraZoom;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CommSignalsStatus;
import com.andruav.protocol.commands.textMessages.AndruavMessage_DistinationLocation;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ExternalCommand_GeoFence;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ExternalCommand_WayPoints;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GEOFenceHit;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GPS;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GeoFence;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GeoFenceAttachStatus;
import com.andruav.protocol.commands.textMessages.AndruavMessage_HomeLocation;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ID;
import com.andruav.protocol.commands.textMessages.AndruavMessage_NAV_INFO;
import com.andruav.protocol.commands.textMessages.AndruavMessage_POW;
import com.andruav.protocol.commands.textMessages.AndruavMessage_RemoteControl2;
import com.andruav.protocol.commands.textMessages.AndruavMessage_RemoteControlSettings;
import com.andruav.protocol.commands.textMessages.AndruavMessage_SetHomeLocation;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Signaling;
import com.andruav.protocol.commands.textMessages.AndruavMessage_UploadWayPoints;
import com.andruav.protocol.commands.textMessages.AndruavMessage_WayPoints;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_Arm;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_ChangeSpeed;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_CirclePoint;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_DoYAW;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_FlightControl;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_GimbalCtrl;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_GuidedPoint;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_Land;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_ChangeAltitude;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_Ctrl_Camera;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_RemoteExecute;
import com.andruav.protocol.commands.textMessages.systemCommands.AndruavSystem_ConnectedCommServer;
import com.andruav.protocol.commands.textMessages.systemCommands.AndruavSystem_LoadTasks;
import com.andruav.protocol.commands.textMessages.systemCommands.AndruavSystem_LogoutCommServer;
import com.andruav.protocol.commands.textMessages.systemCommands.AndruavSystem_Ping;
import com.andruav.protocol.communication.uavos.AndruavUDPServerBase;
import com.andruav.uavos.modules.UAVOSHelper;
import com.andruav.uavos.modules.UAVOSModuleCamera;
import com.andruav.util.CustomCircularBuffer;

import org.json.JSONException;

import java.net.URI;
import java.util.List;

import static com.andruav.event.fcb_7adath._7adath_FCB_RemoteControlSettings.RC_SUB_ACTION_JOYSTICK_CHANNELS;
import static com.andruav.event.fcb_7adath._7adath_FCB_RemoteControlSettings.RC_SUB_ACTION_JOYSTICK_CHANNELS_GUIDED;
import static com.andruav.protocol.commands.ProtocolHeaders.CMD_COMM_GROUP;

public abstract class AndruavWSClientBase {
    protected final AndruavWSClientBase Me;


    public static final String MESSAGE_TYPE_SYSTEM = ProtocolHeaders.CMD_TYPE_SYS;


    public static final String CMD_SYS_CONNECTED = "connected";
    public static final String CMD_SYS_PING = "ping";
    public static final String CMD_SYS_ADD = "add";
    public static final String CMD_SYS_ADD_ENFORCE = "addd";
    public static final String CMD_SYS_DEL = "del";
    public static final String CMD_SYS_PING_OK = "OK:ping";
    public static final String CMD_SYS_ADD_OK = "OK:add";
    public static final String CMD_SYS_ADD_ENFORCE_OK = "OK:addd";
    public static final String CMD_SYS_DEL_OK = "OK:del";
    public static final String CMD_SYS_DEL_ENFORCE_OK = "OK:dell";
    public static final String CMD_COM_GROUP = CMD_COMM_GROUP;
    public static final String CMD_COM_INDIVIDUAL = ProtocolHeaders.CMD_COMM_INDIVIDUAL;


    //////////////////// Measures & COUNTERS
    public double TotalBytesSent = 0;
    public double TotalBytesRecieved = 0;
    public int TotalPacketsSent = 0;
    public int TotalPacketsRecieved = 0;
    public double TotalWebRTCBytesSent = 0;
    public double TotalWebRTCBytesRecieved = 0;
    public double TotalBinaryBytesSent = 0;
    public double TotalBinaryBytesRecieved = 0;
    public int TotalBinaryPacketsSent = 0;
    public int TotalBinaryPacketsRecieved = 0;

    public static final int PING_HISTORY_SIZE = 10;           // number of points to plot in history
    public final CustomCircularBuffer HistoryPing = new CustomCircularBuffer(PING_HISTORY_SIZE);
    public long LastPing = 0;
    public long AveragePing = 0;


    //////////////////// Timing
    private final static  long monSendIDDuration                =60000;  //should be > monSlowOperationTicks
    private final static  long monSendIDMinDuration             =10000;
    private final static  long monPingDuration                  =90000;  //should be > monSlowOperationTicks
    private final static  long monSlowOperationTicks            =10000;  //calling rate of SlowSehculeTasks

    private static  long monSendIDStepDuration=monSendIDMinDuration;
    private static  long monSendID=0;
    private static  long monPing=0;



    /***
     * @// FIXME: 10/2/15 remove emnuerator and use constants
     * This enumerator is used to define status of the websocket connection.
     */

    public static final int SOCKETSTATE_FREASH          = 1;
    public static final int SOCKETSTATE_DISCONNECTED    = 2;
    public static final int SOCKETSTATE_CONNECTED       = 3;
    public static final int SOCKETSTATE_REGISTERED      = 4;
    public static final int SOCKETSTATE_ERROR           = 5;



    public static final int SOCKETACTION_NONE            = 0;
    public static final int SOCKETACTION_RECONNECTING    = 1;
    public static final int SOCKETACTION_CONNECTING      = 2;
    public static final int SOCKETACTION_DISCONNECTING   = 3;

    protected int mSocketState = SOCKETSTATE_FREASH;  // Experimental dont rely on them
    protected int mSocketAction = SOCKETACTION_NONE;  // Experimental dont rely on them


    protected boolean mIgnoreConnect = false;


    protected boolean getEnforceNameStatus() {
        return true;
    }



    public abstract boolean isConnected();

    public abstract void connect(final String url );
    public abstract void connect (final URI url);


    protected abstract void onBinaryMessage(final AndruavBinary_2MR andruavBinary2MR);


    protected abstract void onTextMessage(final Andruav_2MR andruav2MR);

    protected abstract void onError(final int code, final String reason);

    protected abstract void onDeleted(final boolean isSuccess);

    protected abstract void onAdded(final boolean isSuccess);

    protected abstract void executeInternalBinaryCommand(final AndruavBinary_2MR andruav_2MR);

    protected abstract void executeInternalCommand(final Andruav_2MR andruav_2MR);

    protected abstract void executeRemoteExecuteCMD(final Andruav_2MR andruav2MR) ;




    /***
     * Socket Sends Text Message
     */
    protected abstract void socketSendTextMessage(String payload);
    protected abstract void socketSendBinaryMessage(byte[] payload);
    protected abstract void socketDisconnect ();
    protected HandlerThread mhandlerThread;
    protected Handler mhandler;
    protected Handler mhandlerScheduler;

    protected boolean mkillMe = false;


    /***
     * Sync object used to ensure atomic operation.
     */
    protected final Object mSocketStateSync= new Object();


    protected boolean merrorRecovery=false;


    /***
     * connection port as seen from server.
     */
    protected String mport;
    /***
     * client IP as seend from server...not the LAN IP.
     */
    protected String mIP;



    public int getSocketState ()
    {
        return mSocketState;
    }

    public static String generateWSURL (final String wsURL, final boolean SSL)
    {
        if (SSL)
        {
            return "wss://" + wsURL + "&SID=" + AndruavSettings.Account_SID;
        }


        return "wss://" + wsURL+ "&SID=" + AndruavSettings.Account_SID;

    }

    protected void reconnect ()
    {
        setSocketAction(SOCKETACTION_RECONNECTING);
        Log.d("ac","mSocketAction   = SOCKETACTION_RECONNECTING");
    }





    public void disconnect()
    {
        mhandler.removeCallbacksAndMessages(null);
        synchronized (mSocketStateSync) {
            //socketState = enum_socketState.DISCONNECTING;

            // mSocketAction   = SOCKETACTION_DISCONNECTING;

            setSocketAction (SOCKETACTION_DISCONNECTING);

        }

        synchronized (mSocketStateSync) {
            mIgnoreConnect = false;
            merrorRecovery = false;
        }

        if ( (getSocketState() == SOCKETSTATE_FREASH) && (getSocketAction() == SOCKETACTION_NONE))
        {
            setSocketState  (SOCKETSTATE_FREASH);
            setSocketAction (SOCKETACTION_NONE);
            return ;
        }

        socketDisconnect();
        synchronized (mSocketStateSync) {
            mkillMe = true;
            //socketState = enum_socketState.FREASH;

            setSocketState  (SOCKETSTATE_FREASH);
            setSocketAction (SOCKETACTION_NONE);
        }

    }



    /***
     * completly close socket
     * called when shutting down application
     */
    public void shutDown ()
    {
        mkillMe = true;

        if (isConnected())
        {
            disconnect();
        }

        if (mhandler != null) {
            mhandler.removeCallbacksAndMessages(null);
            mhandler = null;
        }

        if (mhandlerScheduler != null) {
            mhandlerScheduler.removeCallbacksAndMessages(null);
            mhandlerScheduler = null;
        }



        if (mhandlerThread != null)
        {
            mhandlerThread.quit();
        }
    }

    public void setSocketState(int value)
    {
        mSocketState = value;
        switch (value)
        {
            case SOCKETSTATE_REGISTERED:

                mhandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            if (!AndruavInternalCommands.getHasExecuted()) {
                                final List<AndruavMessageBase> local_2awamer = AndruavInternalCommands.getList();
                                for (int i = 0, s = local_2awamer.size(); i < s; ++i) {
                                    final AndruavMessageBase andruavMessageBase = local_2awamer.get(i);
                                    Me.sendMessageToMySelf(andruavMessageBase);
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                },1000);
                break;
            case SOCKETSTATE_ERROR:
                mIgnoreConnect = false; // last connect has failed.
                merrorRecovery = true;
                break;

            case SOCKETSTATE_DISCONNECTED:
                mIgnoreConnect = false;
                // DO NOT change merrorRecoverystatus here
                break;
        }

        //Log.d("ac","mSocketState   = " + value);
    }

    public void resetDataCounters()
    {
        TotalBytesSent=0;
        TotalBytesRecieved=0;
        TotalPacketsSent=0;
        TotalPacketsRecieved=0;

        TotalWebRTCBytesSent=0;
        TotalWebRTCBytesRecieved=0;

        TotalBinaryBytesSent =0;
        TotalBinaryBytesRecieved=0;
        TotalBinaryPacketsSent =0;
        TotalBinaryPacketsRecieved =0;

    }


    public int getSocketAction  ()
    {

        return mSocketAction ;
    }

    public void setSocketAction (int value)
    {
        mSocketAction = value;
        //Log.d("ac","mSocketAction   = " + value);
    }

    public enum enum_socketState {
        FREASH,         // No call for connection yet.
        /**
         * Socket is disconnected. and never has been connected before.
         */
        CONNECTING,
        /**
         * Socket is disconnected
         */
        DISCONNECTING,
        /**
         * Socket is disconnected
         */
        DISCONNECTED,
        /**
         * Socket is connected with server.
         */
        CONNECTED,
        /**
         * Socket has a registered name and group on the server
         */
        REGISTERED,
        /**
         * onError received during work ... we should reconnect
         */
        ERROR
    }


    protected void onScheduledTasks(long now)
    {

    }


    protected void onClose(final int code, final String reason)
    {

        AndruavEngine.getEventBus().post(new EventSocketState(EventSocketState.ENUM_SOCKETSTATE.onDisconnect, reason));


    }

    protected void onOpen ()
    {
        // reset ID timer
        monSendIDStepDuration=monSendIDMinDuration;
        //merrorRecovery = false;

        merrorRecovery = true; /// are should be safe now
        mIgnoreConnect = false;
    }


    private final Runnable ScheduledTasks = new Runnable() {
        @Override
        public void run() {

            /* do what you need to do */
            if (!mkillMe) {
                // very slow update as other parties can explicitly ask for status using command
                mhandler.postDelayed(this, monSlowOperationTicks);
            }

            if  (getSocketState() != SOCKETSTATE_REGISTERED)
            {
                return ;
            }

            long now = System.currentTimeMillis();

            onScheduledTasks(now);

            // ID timer here we try to make it increment by steps
            // and reset on connect
            if ((now - monSendID) > monSendIDStepDuration) {
                // The only reason we sendMessageToModule ID message here
                // is that sometimes a unit specially GCS not sending anything, and need to tell
                // others that it is still alive.
                // note that Others sendMessageToModule requestID anyway when they start or need to know
                // who is online.
                // Regularly sendMessageToModule my ID
                monSendID = now;
                AndruavFacade.broadcastID();
                monSendIDStepDuration = monSendIDStepDuration + 1000;
                if (monSendIDStepDuration > monSendIDDuration )
                {
                    monSendIDStepDuration = monSendIDDuration;
                }
            }

            if ((now - monPing) > monPingDuration) {
                // Regularly sendMessageToModule my ID
                monPing = now;
                sendPing();
            }

            //TODO enh .
            // units that are not active should be requested... if still not active
            // should be mared as Offline or ShutDown ...
            // also be removed from request lists IMU, Telemetry & Video to reduce unnecessary traffic here.
        }
    };




    protected void initHandler()
    {
        mhandlerThread = new HandlerThread("WS_SendCMD");
        mhandlerThread.start(); //mhandlerThread.getLooper() will return nll if not started.


        mhandlerScheduler = new Handler(mhandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }

        };

        mhandler = new Handler(mhandlerThread.getLooper())
        {
            private int exception_MainLoop_counter = 3;

            @Override
            public void handleMessage (Message msg){
                try {
                    super.handleMessage(msg);
                    if (mkillMe) return;
                    if (msg.what==1)
                    {
                        socketSendTextMessage((String) msg.obj);
                    }
                    else
                    {
                        socketSendBinaryMessage((byte[]) msg.obj);
                    }

                }
                catch (Exception e)
                {
                    //Log.e("WS", e.getMessage());
                    if (exception_MainLoop_counter>0) {
                        AndruavEngine.log().logException(AndruavSettings.Account_SID, "exception_socketmainloop", e);
                        exception_MainLoop_counter = exception_MainLoop_counter -1;
                    }
                }
            }
        };

        mhandler.postDelayed(ScheduledTasks, 100);
    }


    protected void doErrorRecovery ()
    {
        if ((mhandler!=null) &&(merrorRecovery)) {
            mhandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mkillMe) return ;
                    merrorRecovery = true;
                    Me.reconnect();
                }
            }, 1000);
        }
    }


    protected void onBinaryMessage(final byte[] message)
    {
        final AndruavBinary_2MR andruavBinary2MR = Andruav_Parser.parseBinary(message);

        if (andruavBinary2MR == null) {
            // TODO: Send Notification Bad Parsing Data.
            return;
        }

        andruavBinary2MR.IsReceived = Boolean.TRUE;
        Execute(andruavBinary2MR);
        onBinaryMessage (andruavBinary2MR);

        if (!andruavBinary2MR.processed)
        {
            AndruavEngine.getEventBus().post(andruavBinary2MR);
        }
    }


    protected void onTextMessage(final String message)
    {

        Andruav_2MR andruav2MR = Andruav_Parser.parseText(message);
        if (andruav2MR == null) {
            // TODO: Send Notification Bad Parsing Data.
            return;
        }
        andruav2MR.IsReceived = Boolean.TRUE;
        Execute(andruav2MR);

        // Should be the last line as previous lines may change socket status.
        onTextMessage(andruav2MR);


        if (!andruav2MR.processed)
        {
            // any unprocessed message should be broadcasted to all system.
            AndruavEngine.getEventBus().post(andruav2MR);
        }
    }




    /***
     * Executes commands embedded in the message section.
     * @param andruav_2MR
     */
    private void executeInternalCommand_default(Andruav_2MR andruav_2MR) {

        final AndruavUnitBase andruavUnitBase = AndruavEngine.getAndruavWe7daMapBase().get(andruav_2MR.partyID);


        switch (andruav_2MR.andruavMessageBase.messageTypeID) {


            // this is data from Another Andruav
            case AndruavMessage_ID.TYPE_AndruavMessage_ID:
                // add a new AndruavUnit
                // check {@link AndruavWe7daMapBase.newUnitAdded} if you want to add actions
                AndruavEngine.getAndruavWe7daMapBase().put(andruav_2MR);

                andruav_2MR.processed = true;
                break;

            // This message is equivelant to IMU Ready but FCB is used
            case AndruavMessage_NAV_INFO.TYPE_AndruavMessage_NAV_INFO: {
                andruav_2MR.processed = true;
                final AndruavUnitBase andruavUnit = AndruavEngine.getAndruavWe7daMapBase().updateNAV(andruav_2MR);
                if (andruavUnit != null) {
                    AndruavEngine.getEventBus().post(new _7adath_IMU_Ready(andruavUnit)); // inform all that a data is ready
                }

                andruav_2MR.processed = true;

            }
            break;

            case AndruavMessage_ExternalCommand_GeoFence.TYPE_AndruavMessage_ExternalGeoFence: {
                /* I am a drone and need to uploade a Fence Info */
                /* andruavWe7daBase could be SYSTEM */
                if (AndruavSettings.andruavWe7daBase.getIsCGS()) break; /// this is a Drone Command.

                final AndruavMessage_ExternalCommand_GeoFence andruavMessage_externalGeoFence = (AndruavMessage_ExternalCommand_GeoFence) andruav_2MR.andruavMessageBase;
                final GeoFenceBase g = andruavMessage_externalGeoFence.getGeoFencePoints();
                if (g != null) {

                    GeoFenceManager.addGeoFence(AndruavSettings.andruavWe7daBase, g);
                }
            }
            break;

            case AndruavMessage_RemoteControl2.TYPE_AndruavMessage_RemoteControl2: {
                andruav_2MR.processed = true;
                final Event_Remote_ChannelsCMD eventRemote_Channels_cmd = new Event_Remote_ChannelsCMD(((AndruavMessage_RemoteControl2) andruav_2MR.andruavMessageBase).getChannelsCopy());
                if ((AndruavSettings.andruavWe7daBase.getManualTXBlockedSubAction() & (RC_SUB_ACTION_JOYSTICK_CHANNELS | RC_SUB_ACTION_JOYSTICK_CHANNELS_GUIDED)) == 0)
                {
                    return ;
                }
                // Channels from [0-1000]
                eventRemote_Channels_cmd.partyID = AndruavSettings.andruavWe7daBase.PartyID; // ME
                eventRemote_Channels_cmd.engaged = true; // ME
                AndruavEngine.getEventBus().post(eventRemote_Channels_cmd);
            }
            break;

            case AndruavMessage_GeoFence.TYPE_AndruavMessage_GeoFence:
                /* Some Drone updated its Geo Fence */
                if (!AndruavSettings.andruavWe7daBase.getIsCGS())
                    break; // I as a third Drone dont save GeoFencePoint of other's drones...Not in this version at least :)

                final AndruavMessage_GeoFence andruavMessage_GeoFence = (AndruavMessage_GeoFence) andruav_2MR.andruavMessageBase;
                final GeoFenceBase g = andruavMessage_GeoFence.getGeoFencePoints();
                if (g != null) {
                    if (andruavUnitBase != null) {
                        GeoFenceManager.addGeoFence(andruavUnitBase, g);
                    }
                }
                break;

            case AndruavMessage_GeoFenceAttachStatus.TYPE_AndruavResala_GeoFenceAttachStatus: {
                // A drone has been attached to a fence.
                // if you have the fence then use it other wise request full fence info.
                if (andruavUnitBase == null) {
                    AndruavFacade.requestID(andruav_2MR.partyID);
                }

                if (AndruavSettings.andruavWe7daBase.getIsCGS()) { // I as a third Drone dont save GeoFencePoint of other's drones...Not in this version at least :)

                    final AndruavMessage_GeoFenceAttachStatus andruavMessage_geoFenceAttachStatus = (AndruavMessage_GeoFenceAttachStatus) andruav_2MR.andruavMessageBase;
                    final GeoFenceBase geoFenceBase = GeoFenceManager.getGeoFence(andruavMessage_geoFenceAttachStatus.fenceName);

                    if (andruavMessage_geoFenceAttachStatus.isAttachedToFence) {
                        // we need to
                        // 1- Make sure we have this fence --- if not then ask for it from this drone.
                        // 2- Add this Drone to the fence

                        if (geoFenceBase != null) {
                            // we have it already
                            GeoFenceManager.attachToGeoFence(geoFenceBase, andruavUnitBase);
                        } else {    // what is this fence ... please sendMessageToModule fence info & hit info
                            AndruavFacade.requestGeoFenceInfo(andruavUnitBase, andruavMessage_geoFenceAttachStatus.fenceName);
                        }
                    } else {
                        // Deattach Action
                        // 1- Deattach Drone from fence... if we dont have this fence then we DONT want IT
                        // If another drone uses it we will know and ask for it from that drone.

                        GeoFenceManager.removeUnitFromGeoFence(geoFenceBase, andruavUnitBase);

                    }
                }
            }
            break;

            case AndruavMessage_GEOFenceHit.TYPE_AndruavResala_GEOFenceHit: {
                if (andruavUnitBase != null) {
                    final AndruavMessage_GEOFenceHit andruavMessage_geoFenceHit = (AndruavMessage_GEOFenceHit) andruav_2MR.andruavMessageBase;
                    //final GeoFenceCompositBase geoFenceMapBase = andruavWe7daBase.getGeoFenceMapBase().get(andruavResala_geoFenceHit.fenceName);
                    final GeoFenceBase geoFenceMapBase = GeoFenceManager.getGeoFence(andruavMessage_geoFenceHit.fenceName);

                    final _7adath_GeoFence_Hit a7adath_geoFence_hit = new _7adath_GeoFence_Hit(andruavUnitBase, andruavMessage_geoFenceHit.fenceName, andruavMessage_geoFenceHit.inZone, andruavMessage_geoFenceHit.distance, andruavMessage_geoFenceHit.shouldKeepOutside);
                    if (geoFenceMapBase != null) {
                        geoFenceMapBase.setisInsideRemote(andruavUnitBase, a7adath_geoFence_hit);
                    } else { // TODO: IMPORTANT sendMessageToModule ask for loading track

                    }
                    AndruavEngine.getEventBus().post(new _7adath_GeoFence_Hit(andruavUnitBase, andruavMessage_geoFenceHit.fenceName, andruavMessage_geoFenceHit.inZone, andruavMessage_geoFenceHit.distance, andruavMessage_geoFenceHit.shouldKeepOutside));
                }
            }
            break;

            case AndruavMessage_GPS.TYPE_AndruavMessage_GPS:
                // Update GPS Data
            {

                andruav_2MR.processed = true;
                final AndruavUnitBase andruavUnit = AndruavEngine.getAndruavWe7daMapBase().updateGPS(andruav_2MR);

                if (andruavUnit != null) {
                    AndruavEngine.getEventBus().post(new _7adath_GPS_Ready(andruavUnit)); // inform all that a data is ready
                }
            }
            break;

            case AndruavMessage_SetHomeLocation.TYPE_AndruavMessage_SetHomeLocation:
            {
                andruav_2MR.processed = true;

                if (andruavUnitBase == null) {
                    AndruavFacade.requestID(andruav_2MR.partyID);
                }

                if (!AndruavSettings.andruavWe7daBase.getIsCGS())
                {
                    final AndruavMessage_SetHomeLocation andruavMessage_setHomeLocation = (AndruavMessage_SetHomeLocation) andruav_2MR.andruavMessageBase;
                    AndruavSettings.andruavWe7daBase.do_UpdateExternalHomeLocation(andruavMessage_setHomeLocation.home_gps_lng, andruavMessage_setHomeLocation.home_gps_lat, andruavMessage_setHomeLocation.home_gps_alt);
                }
            }
            break;

            case AndruavMessage_HomeLocation.TYPE_AndruavMessage_HomeLocation:
            {

                andruav_2MR.processed = true;
                final AndruavUnitBase andruavUnit = AndruavEngine.getAndruavWe7daMapBase().updateHomeLocation(andruav_2MR);

                if (andruavUnit != null) // cannot set my home location remotely
                {
                    AndruavEngine.getEventBus().post(new _7adath_HomeLocation_Ready(andruavUnit)); // inform all that a data is ready
                }
            }
            break;

            case AndruavMessage_DistinationLocation.TYPE_AndruavMessage_DistinationLocation:
            {

                andruav_2MR.processed = true;
                final AndruavUnitBase andruavUnit = AndruavEngine.getAndruavWe7daMapBase().updateTargetLocation(andruav_2MR);

                if (andruavUnit != null) {
                    AndruavEngine.getEventBus().post(new _7adath_TargetLocation_Ready(andruavUnit)); // inform all that a data is ready
                }
            }
            break;

            case AndruavMessage_POW.TYPE_AndruavMessage_POW:
                // Update POW Data
            {
                andruav_2MR.processed = true;
                final AndruavUnitBase andruavUnit = AndruavEngine.getAndruavWe7daMapBase().updatePOW(andruav_2MR);
                if (andruavUnit != null) {
                    AndruavEngine.getEventBus().post(new _7adath_Battery_Ready(andruavUnit)); // inform all that a data is ready
                }
            }
            break;

            case AndruavMessage_CameraFlash.TYPE_AndruavResala_CameraFlash:
            {
                try
                {
                    andruav_2MR.processed = true;
                    final AndruavUnitBase andruavWe7da = (AndruavEngine.getAndruavWe7daMapBase()).get(andruav_2MR.partyID);

                    if ((andruavWe7da != null) && (!andruavWe7da.canImage())) break;
                    if (AndruavSettings.andruavWe7daBase.getIsCGS())
                        break; // not a valid command to GCSevent_fpv_cmd = new _7adath_FPV_CMD(_7adath_FPV_CMD.FPV_CMD_TAKEIMAGE);

                    final AndruavMessage_CameraFlash andruavMessage_cameraFlash = (AndruavMessage_CameraFlash) andruav_2MR.andruavMessageBase;

                    UAVOSModuleCamera cameraModule = UAVOSHelper.getCameraByID(andruavMessage_cameraFlash.CameraUniqueName);
                    if (cameraModule == null) {
                        // camera is not available
                        return;
                    }

                    if (cameraModule.BuiltInModule) {
                        // This is a local camera for this Andruav Device
                        _7adath_FPV_CMD a7adath_fpv_cmd = new _7adath_FPV_CMD(_7adath_FPV_CMD.FPV_CMD_FLASHCAM);

                        a7adath_fpv_cmd.ACT = (andruavMessage_cameraFlash.FlashOn == AndruavMessage_CameraFlash.FLASH_ON);
                        a7adath_fpv_cmd.Requester = andruav_2MR.partyID;
                        AndruavEngine.getEventBus().post(a7adath_fpv_cmd);
                    } else {
                        // This is a camera module connected to Andruav Device
                        ((AndruavUDPServerBase) AndruavEngine.getAndruavUDP()).sendMessageToModule(cameraModule, andruav_2MR);
                    }
                }
                catch (final Exception ex)
                {
                    ex.printStackTrace();
                }
            }
            case AndruavMessage_CameraSwitch.TYPE_AndruavMessage_CameraSwitch:
            {
                try
                {
                    andruav_2MR.processed = true;
                    final AndruavUnitBase andruavWe7da = (AndruavEngine.getAndruavWe7daMapBase()).get(andruav_2MR.partyID);

                    if ((andruavWe7da != null) && (!andruavWe7da.canImage())) break;
                    if (AndruavSettings.andruavWe7daBase.getIsCGS())
                        break; // not a valid command to GCSevent_fpv_cmd = new _7adath_FPV_CMD(_7adath_FPV_CMD.FPV_CMD_TAKEIMAGE);

                    final AndruavMessage_CameraSwitch andruavMessage_cameraSwitch = (AndruavMessage_CameraSwitch) andruav_2MR.andruavMessageBase;


                    UAVOSModuleCamera cameraModule = UAVOSHelper.getCameraByID(andruavMessage_cameraSwitch.CameraUniqueName);
                    if (cameraModule == null) {
                        // camera is not available
                        return;
                    }

                    if (cameraModule.BuiltInModule) {
                        // This is a local camera for this Andruav Device
                        _7adath_FPV_CMD a7adath_fpv_cmd = new _7adath_FPV_CMD(_7adath_FPV_CMD.FPV_CMD_SWITCHCAM);

                        a7adath_fpv_cmd.Variables.put("SendBackTo", andruavMessage_cameraSwitch.CameraUniqueName);
                        a7adath_fpv_cmd.Requester = andruav_2MR.partyID;
                        AndruavEngine.getEventBus().post(a7adath_fpv_cmd);
                    } else {
                        // This is a camera module connected to Andruav Device
                        ((AndruavUDPServerBase) AndruavEngine.getAndruavUDP()).sendMessageToModule(cameraModule, andruav_2MR);
                    }
                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                }
            }
            case AndruavMessage_CameraZoom.TYPE_AndruavMessage_CameraZoom:
            {
                try
                {
                    andruav_2MR.processed = true;
                    final AndruavUnitBase andruavWe7da = (AndruavEngine.getAndruavWe7daMapBase()).get(andruav_2MR.partyID);
                    if (andruavWe7da == null) return;

                    if (AndruavSettings.andruavWe7daBase.getIsCGS())
                    {
                        return ;
                    }

                    final AndruavMessage_CameraZoom andruavMessage_cameraZoom = (AndruavMessage_CameraZoom) andruav_2MR.andruavMessageBase;
                    UAVOSModuleCamera cameraModule = UAVOSHelper.getCameraByID(andruavMessage_cameraZoom.CameraUniqueName);
                    if (cameraModule == null) {
                        // camera is not available
                        return;
                    }

                    if (cameraModule.BuiltInModule) {
                        // This is a local camera for this Andruav Device
                        _7adath_CameraZoom adath_cameraZoom = new _7adath_CameraZoom(andruavMessage_cameraZoom);
                        AndruavEngine.getEventBus().post(adath_cameraZoom);
                    } else {
                        // This is a camera module connected to Andruav Device
                        ((AndruavUDPServerBase) AndruavEngine.getAndruavUDP()).sendMessageToModule(cameraModule, andruav_2MR);
                    }
                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                }
            }
            break;
            case AndruavMessage_Signaling.TYPE_AndruavMessage_Signaling: {
                try {
                    andruav_2MR.processed = true;
                    final AndruavUnitBase andruavWe7da = (AndruavEngine.getAndruavWe7daMapBase()).get(andruav_2MR.partyID);
                    if (andruavWe7da == null) return;

                    final AndruavMessage_Signaling andruavMessage_signaling = (AndruavMessage_Signaling) andruav_2MR.andruavMessageBase;


                    if (AndruavSettings.andruavWe7daBase.getIsCGS())
                    {
                        _7adath_Signalling a7adath_signalling = new _7adath_Signalling(andruavMessage_signaling.getJsonResala(), andruavWe7da);
                        AndruavEngine.getEventBus().post(a7adath_signalling);

                        return ;
                    }
                    else {

                        // get Module for Requested Camera
                        UAVOSModuleCamera cameraModule = UAVOSHelper.getCameraByID(andruavMessage_signaling.getJsonResala().getString("channel"));
                        if (cameraModule == null) {
                            // camera is not available
                            break;
                        }

                        if (cameraModule.BuiltInModule) {
                            // This is a local camera for this Andruav Device
                            _7adath_Signalling a7adath_signalling = new _7adath_Signalling(andruavMessage_signaling.getJsonResala(), andruavWe7da);
                            AndruavEngine.getEventBus().post(a7adath_signalling);
                        } else {
                            // This is a camera module connected to Andruav Device
                            ((AndruavUDPServerBase) AndruavEngine.getAndruavUDP()).sendMessageToModule(cameraModule, andruav_2MR);
                        }
                    }
                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                }

            }
            break;


            case AndruavMessage_UploadWayPoints.TYPE_AndruavMessage_UploadWayPoints: {
                andruav_2MR.processed = true;
                final AndruavUnitBase andruavWe7da = (AndruavEngine.getAndruavWe7daMapBase()).get(andruav_2MR.partyID);
                if (andruavWe7da == null) {
                    // Object is not in the defined markers...
                    // either because :
                    // 1- No Message ID has been sent yet.
                    // 2- Future Reason: permissions and security related issues.
                    // We can add it as update list to increase response time but I prefer to leave it as is
                    // For future security and permissions updates.
                    // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
                    break;
                }
                final AndruavMessage_UploadWayPoints andruavMessage_uploadWayPoints = (AndruavMessage_UploadWayPoints) andruav_2MR.andruavMessageBase;

                if (AndruavSettings.andruavWe7daBase.useFCBIMU()) {
                    // should be POST EVENT
                    AndruavSettings.andruavWe7daBase.doPutMissionintoFCB(andruavMessage_uploadWayPoints.MissionText);
                } else {
                    // AndruavFacade.sendHomeLocation(andruavUnit);
                    //AndruavFacade.sendWayPoints(andruavUnit);
                }
            }
            break;

            case AndruavMessage_WayPoints.TYPE_AndruavMessage_WayPoints: {
                andruav_2MR.processed = true;
                final AndruavUnitBase andruavWe7da = (AndruavEngine.getAndruavWe7daMapBase()).get(andruav_2MR.partyID);
                if (andruavWe7da == null) {
                    // Object is not in the defined markers...
                    // either because :
                    // 1- No Message ID has been sent yet.
                    // 2- future reason: permissions and security related issues.
                    // We can add it as update list to increase response time but I prefer to leave it as is
                    // for future security and permissions updates.
                    // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
                    break;
                }
                (AndruavEngine.getAndruavWe7daMapBase()).refreshWayPoints(andruavWe7da, ((AndruavMessage_WayPoints) andruav_2MR.andruavMessageBase));
                AndruavEngine.getEventBus().post(new _7adath_WayPointsRecieved(andruavWe7da));
            }
            break;

            case AndruavMessage_ExternalCommand_WayPoints.TYPE_AndruavResala_ExternalCommand_WayPoints: {
                andruav_2MR.processed = true;
                AndruavSettings.andruavWe7daBase.getMohemmaMapBase().clear();

                final MohemmaMapBase mohemmaMapBase = ((AndruavMessage_ExternalCommand_WayPoints) andruav_2MR.andruavMessageBase).getWayPoints();
                if (mohemmaMapBase == null) return;

                AndruavSettings.andruavWe7daBase.updateExternalWayPoints(mohemmaMapBase);

                // AndruavMo7arek.getEventBus().post(new _7adath_WayPointsRecieved(andruavWe7da));
            }
            break;

            case AndruavMessage_FlightControl.TYPE_AndruavMessage_FlightControl: {
                andruav_2MR.processed = true;
                if ((andruavUnitBase == null) || (!andruavUnitBase.canControl())) {
                    // Object is not in the defined markers...
                    // either because :
                    // 1- No Message ID has been sent yet.
                    // 2- future reason: permissions and security related issues. :)
                    // We can add it as update list to increase response time but I prefer to leave it as is
                    // for future security and permissions updates.
                    // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
                    break;
                }

                final ControlBoardBase controlBoardBase = AndruavSettings.andruavWe7daBase.FCBoard;
                if (controlBoardBase != null) {
                    controlBoardBase.do_Mode(((AndruavMessage_FlightControl) andruav_2MR.andruavMessageBase).FlightMode, null);
                }
            }
            break;

            case AndruavMessage_Arm.TYPE_AndruavMessage_Arm: {
                andruav_2MR.processed = true;
                if ((andruavUnitBase == null)  || (!andruavUnitBase.canControl())) {
                    // Object is not in the defined markers...
                    // either because :
                    // 1- No Message ID has been sent yet.
                    // 2- future reason: permissions and security related issues.
                    // We can add it as update list to increase response time but I prefer to leave it as is
                    // for future security and permissions updates.
                    // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
                    break;
                }

                final AndruavMessage_Arm andruavResala_arm = (AndruavMessage_Arm) andruav_2MR.andruavMessageBase;

                final ControlBoardBase controlBoardBase = AndruavSettings.andruavWe7daBase.FCBoard;
                if (controlBoardBase != null) {
                    if (andruavResala_arm.arm ) {
                        controlBoardBase.do_ARM(null);
                    } else {
                        controlBoardBase.do_Disarm(andruavResala_arm.emergencyDisarm,null);
                    }
                }
            }
            break;


            case AndruavMessage_Land.TYPE_AndruavMessage_Land:
            {
                andruav_2MR.processed = true;
                if ((andruavUnitBase == null) || (!andruavUnitBase.canControl())) {
                    // Object is not in the defined markers...
                    // either because :
                    // 1- No Message ID has been sent yet.
                    // 2- future reason: permissions and security related issues.
                    // We can add it as update list to increase response time but I prefer to leave it as is
                    // for future security and permissions updates.
                    // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
                    break;
                }

                final AndruavMessage_Land andruavResala_land = (AndruavMessage_Land) andruav_2MR.andruavMessageBase;

                final ControlBoardBase controlBoardBase = AndruavSettings.andruavWe7daBase.FCBoard;
                if (controlBoardBase != null) {
                    controlBoardBase.do_Land(null);
                }
            }
            break;


            case AndruavMessage_RemoteControlSettings.TYPE_RemoteControlSettings:
            {
                andruav_2MR.processed = true;
                if ((andruavUnitBase == null) || (!andruavUnitBase.canControl())) {
                    break;
                }

                final AndruavMessage_RemoteControlSettings andruavMessage_remoteControlSettings = (AndruavMessage_RemoteControlSettings) andruav_2MR.andruavMessageBase;
                _7adath_FCB_RemoteControlSettings a7adathFCB_remoteControlSettings = new _7adath_FCB_RemoteControlSettings(andruavMessage_remoteControlSettings.rcSubAction);
                AndruavEngine.getEventBus().post(a7adathFCB_remoteControlSettings);

            }
            break;

            case AndruavMessage_ChangeSpeed.TYPE_AndruavResala_ChangeSpeed:
            {
                andruav_2MR.processed = true;
                if ((andruavUnitBase == null) || (!andruavUnitBase.canControl())) {
                    // Object is not in the defined markers...
                    // either because :
                    // 1- No Message ID has been sent yet.
                    // 2- future reason: permissions and security related issues.
                    // We can add it as update list to increase response time but I prefer to leave it as is
                    // for future security and permissions updates.
                    // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
                    break;
                }

                final AndruavMessage_ChangeSpeed andruavResala_changeSpeed = (AndruavMessage_ChangeSpeed) andruav_2MR.andruavMessageBase;

                AndruavSettings.andruavWe7daBase.do_SetNavigationSpeed(andruavResala_changeSpeed.speed,andruavResala_changeSpeed.isGroundSpeed,andruavResala_changeSpeed.throttle,andruavResala_changeSpeed.isRelative);

            }
            break;



            case AndruavMessage_ChangeAltitude.TYPE_AndruavMessage_ChangeAltitude: {
                andruav_2MR.processed = true;
                if ((andruavUnitBase == null) || (!andruavUnitBase.canControl())) {
                    // Object is not in the defined markers...
                    // either because :
                    // 1- No Message ID has been sent yet.
                    // 2- future reason: permissions and security related issues.
                    // We can add it as update list to increase response time but I prefer to leave it as is
                    // for future security and permissions updates.
                    // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
                    break;
                }

                final AndruavMessage_ChangeAltitude andruavResala_changeAltitude = (AndruavMessage_ChangeAltitude) andruav_2MR.andruavMessageBase;

                final ControlBoardBase controlBoardBase = AndruavSettings.andruavWe7daBase.FCBoard;
                if (controlBoardBase != null) {
                    controlBoardBase.do_ChangeAltitude(andruavResala_changeAltitude.altitude,null);
                }
            }
            break;

            case AndruavMessage_CirclePoint.TYPE_AndruavMessage_CirclePoint:
            {
                andruav_2MR.processed = true;
                if ((andruavUnitBase == null) || (!andruavUnitBase.canControl())) {
                    // Object is not in the defined markers...
                    // either because :
                    // 1- No Message ID has been sent yet.
                    // 2- future reason: permissions and security related issues.
                    // We can add it as update list to increase response time but I prefer to leave it as is
                    // for future security and permissions updates.
                    // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
                    break;
                }

                final AndruavMessage_CirclePoint andruavResala_circlePoint = (AndruavMessage_CirclePoint) andruav_2MR.andruavMessageBase;

                final ControlBoardBase controlBoardBase = AndruavSettings.andruavWe7daBase.FCBoard;
                if (controlBoardBase != null) {
                    controlBoardBase.do_CircleHere(
                            andruavResala_circlePoint.Longitude, andruavResala_circlePoint.Latitude, andruavResala_circlePoint.Altitude, andruavResala_circlePoint.Radius, andruavResala_circlePoint.Turns,null);
                }
            }
            break;

            case AndruavMessage_DoYAW.TYPE_AndruavMessage_DoYAW:
            {
                andruav_2MR.processed = true;
                if ((andruavUnitBase == null) || (!andruavUnitBase.canControl())) {
                    // Object is not in the defined markers...
                    // either because :
                    // 1- No Message ID has been sent yet.
                    // 2- future reason: permissions and security related issues.
                    // We can add it as update list to increase response time but I prefer to leave it as is
                    // for future security and permissions updates.
                    // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
                    break;
                }

                final AndruavMessage_DoYAW andruavResala_doYAW = (AndruavMessage_DoYAW) andruav_2MR.andruavMessageBase;

                final ControlBoardBase controlBoardBase = AndruavSettings.andruavWe7daBase.FCBoard;
                if (controlBoardBase != null) {
                    controlBoardBase.do_Yaw(andruavResala_doYAW.targetAngle,andruavResala_doYAW.turnRate,andruavResala_doYAW.isClockwise,andruavResala_doYAW.isRelative);
                }
            }
            break;

            case AndruavMessage_GimbalCtrl.TYPE_AndruavMessage_GimbalCtrl:
            {
                andruav_2MR.processed = true;
                if ((andruavUnitBase == null) || (!andruavUnitBase.canControl())) {
                    // Object is not in the defined markers...
                    // either because :
                    // 1- No Message ID has been sent yet.
                    // 2- future reason: permissions and security related issues.
                    // We can add it as update list to increase response time but I prefer to leave it as is
                    // for future security and permissions updates.
                    // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
                    break;
                }

                final AndruavMessage_GimbalCtrl andruavResala_gimbalCtrl = (AndruavMessage_GimbalCtrl) andruav_2MR.andruavMessageBase;

                final ControlBoardBase controlBoardBase = AndruavSettings.andruavWe7daBase.FCBoard;
                if (controlBoardBase != null) {
                    controlBoardBase.do_GimbalCtrl(andruavResala_gimbalCtrl.pitch_degx100,andruavResala_gimbalCtrl.roll_degx100,andruavResala_gimbalCtrl.yaw_degx100, andruavResala_gimbalCtrl.isAbsolute);
                }
            }
            break;

            case AndruavMessage_GuidedPoint.TYPE_AndruavMessage_GuidedPoint:
            {
                andruav_2MR.processed = true;
                if ((andruavUnitBase == null)  || (!andruavUnitBase.canControl())) {
                    // Object is not in the defined markers...
                    // either because :
                    // 1- No Message ID has been sent yet.
                    // 2- future reason: permissions and security related issues.
                    // We can add it as update list to increase response time but I prefer to leave it as is
                    // for future security and permissions updates.
                    // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
                    break;
                }

                final AndruavMessage_GuidedPoint andruavResala_guidedPoint = (AndruavMessage_GuidedPoint) andruav_2MR.andruavMessageBase;

                final ControlBoardBase controlBoardBase = AndruavSettings.andruavWe7daBase.FCBoard;
                if (controlBoardBase != null) {
                    controlBoardBase.do_FlytoHere(
                            andruavResala_guidedPoint.Longitude, andruavResala_guidedPoint.Latitude, andruavResala_guidedPoint.Altitude,
                            andruavResala_guidedPoint.xVelocity,andruavResala_guidedPoint.yVelocity,andruavResala_guidedPoint.zVelocity,
                            andruavResala_guidedPoint.Yaw,andruavResala_guidedPoint.YawRate);
                }
            }
            break;

            case AndruavMessage_CommSignalsStatus.TYPE_AndruavMessage_CommSignalsStatus:
            {
                // Update GPS Data
                andruav_2MR.processed = true;
                final AndruavUnitBase andruavWe7da = (AndruavEngine.getAndruavWe7daMapBase()).get(andruav_2MR.partyID);
                if (andruavWe7da != null) {
                    final AndruavMessage_CommSignalsStatus andruavMessage_commSignalsStatus = (AndruavMessage_CommSignalsStatus) andruav_2MR.andruavMessageBase;
                    andruavWe7da.setSignal(andruavMessage_commSignalsStatus.signalType, andruavMessage_commSignalsStatus.signalLevel);
                }
            }
            break;
        }

        // just update the unit... some commands are not executed here
        // such as TYPE_AndruavMessage_Telemetry
        AndruavEngine.getAndruavWe7daMapBase().updateLastActiveTime (andruav_2MR.partyID);
        executeInternalCommand(andruav_2MR);
        return;
    }




    protected void executeInternalBinaryCommand_default(final AndruavBinary_2MR andruav_2MR) {

        //AndruavMo7arek.getAndruavWe7daMapBase().updateLastActiveTime (andruavCMD.PartyID);

        switch (andruav_2MR.andruavResalaBinaryBase.messageTypeID) {

            case AndruavResalaBinary_IMU.TYPE_AndruavMessage_BinaryIMU: {
                // Update IMU Data

                andruav_2MR.processed = true;
                final AndruavUnitBase andruavUnit = AndruavEngine.getAndruavWe7daMapBase().updateIMU(andruav_2MR);
                if (andruavUnit != null) {
                    AndruavEngine.getEventBus().post(new _7adath_IMU_Ready(andruavUnit)); // inform all that a data is ready
                }

                andruav_2MR.processed = true;

            }
            break;



            // This is not internal command it s external command comes from a third party and to be executed here
            case AndruavResalaBinary_ExternalCommand_WayPoints.TYPE_AndruavResalaBinary_ExternalCommand_WayPoints: {
                andruav_2MR.processed = true;
                AndruavSettings.andruavWe7daBase.getMohemmaMapBase().clear();

                MohemmaMapBase mohemmaMapBase = ((AndruavResalaBinary_ExternalCommand_WayPoints) andruav_2MR.andruavResalaBinaryBase).getWayPoints();
                if (mohemmaMapBase == null) return ;

                AndruavSettings.andruavWe7daBase.updateExternalWayPoints(mohemmaMapBase);

                // AndruavMo7arek.getEventBus().post(new _7adath_WayPointsRecieved(andruavWe7da));
            }
            break;

            // Waypoints recieved.
            case AndruavResalaBinary_WayPoints.TYPE_AndruavMessageBinary_WayPoints: {
                andruav_2MR.processed = true;
                AndruavUnitBase andruavWe7da = (AndruavEngine.getAndruavWe7daMapBase()).get(andruav_2MR.partyID);
                if (andruavWe7da == null) {
                    // Object is not in the defined markers...
                    // either because :
                    // 1- No Message ID has been sent yet.
                    // 2- future reason: permissions and security related issues.
                    // We can add it as update list to increase response time but I prefer to leave it as is
                    // for future security and permissions updates.
                    // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
                    break;
                }
                (AndruavEngine.getAndruavWe7daMapBase()).refreshWayPoints(andruavWe7da, ((AndruavResalaBinary_WayPoints) andruav_2MR.andruavResalaBinaryBase));
                AndruavEngine.getEventBus().post(new _7adath_WayPointsRecieved(andruavWe7da));
            }
            break;

            case AndruavResalaBinary_WayPointsUpdates.TYPE_AndruavMessage_WayPointsUpates: {
                {
                    // Update IMU Data
                    andruav_2MR.processed = true;

                    AndruavUnitBase andruavWe7da = (AndruavEngine.getAndruavWe7daMapBase()).get(andruav_2MR.partyID);
                    if (andruavWe7da == null) {
                        // Object is not in the defined markers...
                        // either because :
                        // 1- No Message ID has been sent yet.
                        // 2- future reason: permissions and security related issues.
                        // We can add it as update list to increase response time but I prefer to leave it as is
                        // for future security and permissions updates.
                        // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
                        break;
                    }
                    //BUG: This is a bad logic
                    AndruavEngine.getAndruavWe7daMapBase().updateWayPoints(andruavWe7da,((AndruavResalaBinary_WayPointsUpdates)andruav_2MR.andruavResalaBinaryBase));
                    AndruavEngine.getEventBus().post(new _7adath_WayPointsUpdated(andruavWe7da, ((AndruavResalaBinary_WayPointsUpdates) andruav_2MR.andruavResalaBinaryBase).getWayPoints()));

                }
            }
            break;

            case AndruavResalaBinary_RemoteControlSettings.TYPE_AndruavMessage_RemoteControlSettings:
            {
                andruav_2MR.processed = true;
                AndruavUnitBase andruavWe7da = (AndruavEngine.getAndruavWe7daMapBase()).get(andruav_2MR.partyID);
                if (andruavWe7da == null) {
                    // Object is not in the defined markers...
                    // either because :
                    // 1- No Message ID has been sent yet.
                    // 2- future reason: permissions and security related issues.
                    // We can add it as update list to increase response time but I prefer to leave it as is
                    // for future security and permissions updates.
                    // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
                    break;
                }

                andruavWe7da.setRTC(((AndruavResalaBinary_RemoteControlSettings)andruav_2MR.andruavResalaBinaryBase).getRTC());
                if (andruavWe7da != null) {
                    AndruavEngine.getEventBus().post(new _7adath_RemoteControlSettingsReceived(andruavWe7da));
                }
            }
            break;
            default:
                executeInternalBinaryCommand(andruav_2MR);
                break;
        }

    }



    private void executeRemoteExecuteCMD_default(final Andruav_2MR andruav2MR)
    {
        final AndruavUnitBase andruavUnit = AndruavEngine.getAndruavWe7daMapBase().get(andruav2MR.partyID);

        switch (andruav2MR.andruavMessageBase.messageTypeID) {

            // this is data from Another Andruav
            case AndruavMessage_RemoteExecute.TYPE_AndruavMessage_RemoteExecute: {

                AndruavMessage_RemoteExecute andruavResala_remoteExecute = ((AndruavMessage_RemoteExecute) (andruav2MR.andruavMessageBase));
                int CMD_ID = andruavResala_remoteExecute.RemoteCommandID;
                switch (CMD_ID) {
                    case AndruavMessage_RemoteExecute.RemoteCommand_REQUEST_ID:
                        AndruavFacade.sendID(andruav2MR.partyID); // you cannot replace this with AndruavWe7da object because you dont have one yet and this could lead to echo
                        break;

                    case AndruavMessage_RemoteExecute.RemoteControl_RequestRemoteControlSettings:
                        AndruavFacade.sendRemoteControlSettingsMessage(AndruavSettings.andruavWe7daBase.getRTC(), andruavUnit);
                        break;

                    case AndruavMessage_RemoteExecute.RemoteCommand_REQUEST_POW:
                        sendMessageToIndividual(AndruavDroneFacade.createPowerInfoMessage(), andruav2MR.partyID, false, false);

                        break;

                    case AndruavMessage_RemoteExecute.RemoteCommand_GET_WAY_POINTS:
                        if ((andruavUnit == null) || AndruavSettings.andruavWe7daBase.getIsCGS())
                            break; // this command is broadcasted from a drone.
                        AndruavFacade.sendHomeLocation(andruavUnit);
                        AndruavFacade.sendWayPoints(andruavUnit);
                        break;

                    case AndruavMessage_RemoteExecute.RemoteCommand_RELOAD_WAY_POINTS_FROM_FCB:
                        if ((andruavUnit == null) || (AndruavSettings.andruavWe7daBase.getIsCGS()))
                            break; // this command is broadcasted from a drone.
                        if (AndruavSettings.andruavWe7daBase.useFCBIMU()) {
                            AndruavSettings.andruavWe7daBase.doReloadMissionfromFCB();
                        } else {
                            AndruavFacade.sendHomeLocation(andruavUnit);
                            AndruavFacade.sendWayPoints(andruavUnit);
                        }

                        break;

                    case AndruavMessage_RemoteExecute.RemoteCommand_CLEAR_WAY_POINTS:
                        if (AndruavSettings.andruavWe7daBase.getIsCGS())
                            break; // this command is broadcasted from a drone.
                        AndruavSettings.andruavWe7daBase.doClearMission();
                        break;
                    case AndruavMessage_RemoteExecute.RemoteCommand_SET_START_MISSION_ITEM: {
                        if (AndruavSettings.andruavWe7daBase.getIsCGS())
                            break; // this command is broadcasted from a drone.
                        int missionItemNumber = 1;
                        if (andruavResala_remoteExecute.Variables.containsKey("n")) {
                            missionItemNumber = Integer.parseInt(andruavResala_remoteExecute.Variables.get("n")); // n not fn
                        }

                        AndruavSettings.andruavWe7daBase.doSetCurrentMission(missionItemNumber);
                    }
                    break;

                    case AndruavMessage_RemoteExecute.RemoteCommand_CLEAR_FENCE_DATA: {
                        //if (AndruavSettings.andruavWe7daBase.IsCGS) break; ALL Vehicles should clear fence as it is invalid
                        // this is different that sending deattach or attached remote command to fence ... which does not exist now

                        String fenceName = null;

                        if (andruavResala_remoteExecute.Variables.containsKey("fn")) {
                            fenceName = andruavResala_remoteExecute.Variables.get("fn"); // n not fn
                        }

                        GeoFenceManager.removeGeoFence(fenceName);

                    }
                    break;


                    // Request attached fences. called when a new Unit is online, it asks others for fences names.
                    // as many of fences names are replicated.
                    case AndruavMessage_GeoFenceAttachStatus.TYPE_AndruavResala_GeoFenceAttachStatus: {
                        if (andruavUnit == null) {
                            AndruavFacade.requestID(andruav2MR.partyID);
                            break;
                        }

                        String fenceName = null;

                        if (andruavResala_remoteExecute.Variables.containsKey("fn")) {
                            fenceName = andruavResala_remoteExecute.Variables.get("fn"); // n not fn
                        }

                        GeoFenceManager.sendAttachedStatusToTarget(fenceName, andruavUnit);


                    }

                    // A remote units is RequestING a GeoFencePoint Status of Me
                    case AndruavMessage_GeoFence.TYPE_AndruavMessage_GeoFence: {// This is a request from outside [Drone or GCS] to all or a specific
                        // fence detail. fencename is specific is sent in a variable "fn" -fence name-.
                        if ((andruavUnit != null) && (!AndruavSettings.andruavWe7daBase.getIsCGS())) {
                            final String fenceName = andruavResala_remoteExecute.Variables.get("fn");
                            if (fenceName != null) {
                                final GeoFenceBase geoFenceMapBase = GeoFenceManager.get(fenceName);
                                if (geoFenceMapBase != null) {
                                    AndruavFacade.sendGeoFence(andruavUnit, geoFenceMapBase);
                                    AndruavFacade.sendGeoFenceHit(andruavUnit, geoFenceMapBase.mAndruavUnits.get(AndruavSettings.andruavWe7daBase.PartyID));
                                }
                            } else {
                                AndruavFacade.sendGeoFence(andruavUnit);
                                AndruavFacade.sendMyGeoFenceHitStatus(andruavUnit);
                            }
                        }
                    }
                    break;


                    case AndruavSystem_LoadTasks.TYPE_AndruavSystem_LoadTasks:
                        if ((andruavUnit != null) && (!AndruavSettings.andruavWe7daBase.getIsCGS())) {
                            int taskscope = 0;
                            if (andruavResala_remoteExecute.Variables.containsKey("ts")) {
                                taskscope = andruavResala_remoteExecute.getIntValue("ts"); // task scope {
                            }

                            String taskType = null;
                            if (andruavResala_remoteExecute.Variables.containsKey("tp")) {
                                taskType = andruavResala_remoteExecute.Variables.get("tp"); // task Type {
                            }


                            AndruavTaskManager.loadTasksByScope(taskscope, taskType);
                        }
                        break;

                }

            }

            case AndruavMessage_Ctrl_Camera.TYPE_AndruavResala_Ctrl_Camera:

                break;

        }


        executeRemoteExecuteCMD(andruav2MR);
        andruav2MR.processed = true;
    }


    public void Execute (Andruav_2MR andruav_2MR) {
        try {
            if (!andruav_2MR.IsReceived) return ; // these are my messages

            if (andruav_2MR.MessageRouting.equals(MESSAGE_TYPE_SYSTEM)) { // SYSTEM MESSAGES

                if (executeSystemCommand(andruav_2MR)) return ; // all system messages are text

                return;
            }
            else
            if ((andruav_2MR.MessageRouting.equals(CMD_COM_INDIVIDUAL)) ||(andruav_2MR.MessageRouting.equals(CMD_COMM_GROUP)))
            {  // process internal commands.

                // TODO: IMPROVEMENT ... You update the record with each message...the value here is that you update the last message time check @link andruav_2MR
                switch (andruav_2MR.andruavMessageBase.getMessageDomain())
                {
                    case AndruavMessageBase.DOMAIN_RESALA_INFO_MESSAGE:
                        executeInternalCommand_default(andruav_2MR);
                        break;
                    case AndruavMessageBase.DOMAIN_RESALA_REMOTE_EXECUTE:
                        executeRemoteExecuteCMD_default(andruav_2MR);
                        break;
                }


            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();

        }
    }
    /***
     * Executes message of type 'MESSAGE_TYPE_SYSTEM'
     * @param andruavBinary2MR
     */
    protected final void Execute(AndruavBinary_2MR andruavBinary2MR) {

        try {
            if (!andruavBinary2MR.IsReceived) return; // these are my messages
            else if ((andruavBinary2MR.MessageRouting.equals(CMD_COM_INDIVIDUAL)) || (andruavBinary2MR.MessageRouting.equals(CMD_COMM_GROUP))) {  // process internal commands.

                // TODO: IMPROVEMENT ... You update the record with each message...the value here is that you update the last message time check @link andruavCMD
                executeInternalBinaryCommand_default(andruavBinary2MR);
                AndruavEngine.getAndruavWe7daMapBase().updateLastActiveTime(andruavBinary2MR.partyID);
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }


    /***
     * Executes commands of type SYS
     * @param andruav2MR
     * @return
     * @throws org.json.JSONException
     */
    protected boolean executeSystemCommand(final Andruav_2MR andruav2MR) throws JSONException {
//        if (!(andruav2MR.andruavResalaBase instanceof AndruavResala_String))
//            return true;

        final AndruavMessageBase andruavMessageBase = andruav2MR.andruavMessageBase;

        final int cmd = andruav2MR.andruavMessageBase.messageTypeID;

        switch (cmd)
        {
            case AndruavSystem_Ping.TYPE_AndruavSystem_Ping: {
                final AndruavSystem_Ping andruavSystem_ping = (AndruavSystem_Ping) andruavMessageBase;
                if ((andruavSystem_ping.Message == null) || (andruavSystem_ping.Message.equals("OK:pong"))) {
                    LastPing = System.currentTimeMillis() - andruavSystem_ping.TimeStamp;
                    HistoryPing.add(LastPing, true);

                    if (AveragePing == 0) {
                        AveragePing = LastPing;
                    } else {
                        AveragePing = (AveragePing + LastPing) / 2;
                    }
                    andruav2MR.timeStamp = String.valueOf(LastPing);
                } else {
                    andruav2MR.IsErr = true;
                }
            }
            break;

            case AndruavSystem_LogoutCommServer.TYPE_AndruavSystem_LogoutCommServer:
            {
                final AndruavSystem_LogoutCommServer andruavSystem_logoutCommServer = (AndruavSystem_LogoutCommServer) andruavMessageBase;
                setSocketState  (SOCKETSTATE_FREASH);
                setSocketAction (SOCKETACTION_NONE);
            }
            break;

            case AndruavSystem_ConnectedCommServer.TYPE_AndruavSystem_ConnectedCommServer:
            {
                setSocketState  (SOCKETSTATE_REGISTERED);
                setSocketAction (SOCKETACTION_NONE);
            }
            break;
        }

//        if ((cmd.equals(CMD_SYS_ADD) || cmd.equals(CMD_SYS_ADD_ENFORCE))) {
//            if (reply.equals("OK:add") || reply.equals("OK:addd")) {
//                synchronized (mSocketStateSync) {
//                    //socketState = enum_socketState.REGISTERED;
//
//
//                    setSocketState  (SOCKETSTATE_REGISTERED);
//                    setSocketAction (SOCKETACTION_NONE);
//
//                }
//                //AndruavMo7arek.log().LogDeviceInfo(AndruavMo7arek.getPreference().getLoginUserName(), "INFO-Registered");
//
//
//            } else {
//                andruav2MR.IsErr = true;
//            }
//            onAdded(!andruav2MR.IsErr);
//        } else
//
//        if (cmd.equals(CMD_SYS_DEL)) {
//            if (andruavMessage_string.getJsonMessage().equals("OK:del")) {
//                // socketState = enum_socketState.CONNECTED; // socket is connected as "TCP" but NOT REGISTERED
//
//                setSocketState  (SOCKETSTATE_CONNECTED);
//                setSocketAction (SOCKETACTION_NONE);
//
//
//                merrorRecovery = false; /// are should be safe now
//            } else {
//                andruav2MR.IsErr = true;
//            }
//            onDeleted(!andruav2MR.IsErr);
//        } else
//
//        if(cmd.equals(CMD_SYS_CONNECTED)) {
//            //TODO: pls update message reply to contain parsable JSON
//            //raw:{"type":"sys","cmd":"connected","message":"41.199.138.33 connected from port 5393"}
//
//            String[] S = StringSplit.fastSplit(andruavMessage_string.getJsonMessage(),':');
//            if (S[0].equals("OK")) {
//                // S[2] contains tcp4 or tcp6   {OK:connected:tcp4:217.55.182.240:34371}
//                mIP = S[3];
//                mport = S[4];
//                Boolean benForce;
//                if (merrorRecovery) {
//                    // enforce addd as maybe the our socket name still exists in the server due to improper disconnection.
//
//                    //setSocketState  (SOCKETSTATE_ERROR);
//                    setSocketState  (SOCKETSTATE_CONNECTED);
//
//                    benForce = true;
//                }
//                else
//                {
//                    benForce = getEnforceNameStatus();
//                }
//                addMe(benForce);
//            }
//            else
//            {
//                andruav2MR.IsErr=true;
//            }
//        }

        return false;
    }



    public AndruavWSClientBase ()
    {
        Me = this;
    }


    public void sendMessageToMySelf (final AndruavMessageBase andruavMessageBase)
    {
        final Andruav_2MR andruav_2MR = new Andruav_2MR();
        andruav_2MR.andruavMessageBase = andruavMessageBase;

        andruav_2MR.groupName = AndruavSettings.andruavWe7daBase.GroupName;
        andruav_2MR.IsReceived = true;
        andruav_2MR.partyID = ProtocolHeaders.SPECIAL_NAME_SYS_NAME;
        andruav_2MR.MessageRouting = ProtocolHeaders.CMD_COMM_INDIVIDUAL;
        Execute(andruav_2MR);
    }


//    /***
//     * Subscribe in Andruav Server
//     * @param EnforceName
//     */
//    public void addMe (final boolean EnforceName)
//    {
//        String CMD;
//        if (EnforceName)
//        {
//            CMD = CMD_SYS_ADD_ENFORCE;
//        }
//        else
//        {
//            CMD = CMD_SYS_ADD;
//        }
//
//
//        setSocketAction(SOCKETACTION_CONNECTING);
//
//        sendSystemCMD(CMD, false,false);
//    }

    /***
     * Send ping command to Andruav Server
     * should expect a reply with Pong
     */
    public void sendPing ()
    {
        sendSystemCMD(new AndruavSystem_Ping(), false,false);
    }

    public void broadcastMessageToGroup(final AndruavResalaBinaryBase andruavMessage, final boolean addTime)
    {
        AndruavBinary_2MR andruavBinary2MR = new AndruavBinary_2MR();
        andruavBinary2MR.isEncrypted = AndruavSettings.encryptionEnabled;
        andruavBinary2MR.MessageRouting = CMD_COMM_GROUP;
        andruavBinary2MR.andruavResalaBinaryBase = andruavMessage;

        broadcastMessageToGroup(andruavBinary2MR, addTime);

    }

    public void broadcastMessageToGroup(final AndruavMessageBase andruavMessage, final boolean addTime)
    {

        broadcastMessageToGroup(andruavMessage, addTime,false);
    }


    public void sendMessageToIndividual(final AndruavResalaBinaryBase andruavMessage, final String target, final boolean addTime)
    {
        sendMessageToIndividual(andruavMessage,target,addTime,false);
    }

    public void sendMessageToIndividual(final AndruavMessageBase andruavMessage, final String target, final Boolean addTime, final boolean instant)
    {
        Andruav_2MR andruav2MR = new Andruav_2MR();
        andruav2MR.isEncrypted = AndruavSettings.encryptionEnabled;
        andruav2MR.MessageRouting =ProtocolHeaders.CMD_COMM_INDIVIDUAL;
        andruav2MR.andruavMessageBase = andruavMessage;

        sendMessageToIndividual(andruav2MR, target, addTime, instant);

    }

    public void sendMessageToIndividual(final AndruavMessageBase andruavMessage, final String target, final Boolean addTime)
    {
        sendMessageToIndividual(andruavMessage,target,addTime,false);
    }

    public void sendMessageToIndividual(final AndruavBinary_2MR andruavBinary2MR, final String target, final boolean addTime, final boolean instant)
    {
        andruavBinary2MR.partyID  = AndruavSettings.andruavWe7daBase.PartyID;
        andruavBinary2MR.MessageRouting =ProtocolHeaders.CMD_COMM_INDIVIDUAL;
        andruavBinary2MR.targetName = target;

        sendCMD(andruavBinary2MR, addTime,instant);

    }


    public void sendMessageToIndividual(final AndruavBinary_2MR andruavBinary2MR, final String target, final boolean addTime)
    {
        sendMessageToIndividual(andruavBinary2MR, target, addTime, false);
    }



    public void sendMessageToIndividual(final Andruav_2MR andruav2MR, final String target, final boolean addTime, final boolean instant)
    {
        andruav2MR.partyID = AndruavSettings.andruavWe7daBase.PartyID;
        andruav2MR.groupName   = AndruavSettings.andruavWe7daBase.GroupName;
        andruav2MR.targetName = target;

        sendCMD(andruav2MR, addTime,instant);

    }

    public void sendMessageToIndividual(final AndruavResalaBinaryBase andruavMessage, final String target, final boolean addTime,  final  boolean instant) {
        AndruavBinary_2MR andruavBinary2MR = new AndruavBinary_2MR();
        andruavBinary2MR.isEncrypted = AndruavSettings.encryptionEnabled;
        andruavBinary2MR.MessageRouting =ProtocolHeaders.CMD_COMM_INDIVIDUAL;
        andruavBinary2MR.andruavResalaBinaryBase = andruavMessage;

        sendMessageToIndividual(andruavBinary2MR, target, addTime,instant);
    }



    public void broadcastMessageToGroup(final AndruavMessageBase andruavMessage, final boolean addTime, final boolean instant)
    {
        Andruav_2MR andruav2MR = new Andruav_2MR();
        andruav2MR.isEncrypted = AndruavSettings.encryptionEnabled;
        andruav2MR.MessageRouting =ProtocolHeaders.CMD_COMM_GROUP;
        andruav2MR.andruavMessageBase = andruavMessage;

        broadcastMessageToGroup(andruav2MR, addTime, instant);

    }

    public void broadcastMessageToGroup(final AndruavBinary_2MR andruavBinary2MR, final boolean addTime)
    {

        andruavBinary2MR.partyID  = AndruavSettings.andruavWe7daBase.PartyID;
        andruavBinary2MR.groupName   = AndruavSettings.andruavWe7daBase.GroupName;
        sendCMD(andruavBinary2MR, addTime);

    }

    /***
     * Send message to my group [AndruavSettings.GroupName]
     * @param andruav2MR
     * @param addTime
     */
    public void broadcastMessageToGroup(final Andruav_2MR andruav2MR, final boolean addTime, final boolean instant)
    {

        andruav2MR.partyID = AndruavSettings.andruavWe7daBase.PartyID;
        andruav2MR.groupName   = AndruavSettings.andruavWe7daBase.GroupName;
        sendCMD(andruav2MR, addTime, instant);

    }

    public void broadcastTextMessageToGroup(final String messageText, final boolean addTime, final boolean instant)
    {
        Andruav_2MR andruav2MR = new Andruav_2MR();
        andruav2MR.MessageRouting = ProtocolHeaders.CMD_COMM_INDIVIDUAL;
        try {
            andruav2MR.setPayloadTextMessage(messageText);
        } catch (Exception e) {
            AndruavEngine.log().logException("exception_parser", e);
            // dont sendMessageToModule bad messages
            return ;
        }
        broadcastMessageToGroup(andruav2MR, addTime, instant);

    }



    public void sendSysCMD(final AndruavMessageBase andruavMessage, final boolean addTime, final boolean instant)
    {
        Andruav_2MR andruav2MR = new Andruav_2MR();
        andruav2MR.isEncrypted = false; // never encrypt a system command
        andruav2MR.MessageRouting = ProtocolHeaders.CMD_TYPE_SYS;
        andruav2MR.partyID = AndruavSettings.andruavWe7daBase.PartyID;
        andruav2MR.groupName   = AndruavSettings.andruavWe7daBase.GroupName;
        andruav2MR.andruavMessageBase = andruavMessage;

        sendCMD(andruav2MR, addTime, instant);

    }

    public void sendSystemCMD(final AndruavMessageBase andruavMessageBase, final boolean addTime, final boolean instant)
    {
        Andruav_2MR andruav2MR = new Andruav_2MR();
        andruav2MR.isEncrypted = false; // never encrypt a system command
        andruav2MR.MessageRouting = ProtocolHeaders.CMD_TYPE_SYS;
        andruav2MR.partyID = AndruavSettings.andruavWe7daBase.PartyID;
        andruav2MR.groupName   = AndruavSettings.andruavWe7daBase.GroupName;
        andruav2MR.andruavMessageBase = andruavMessageBase;
//        try {
//            andruav2MR.setPayloadTextMessage("{'scmd':" + andruavResalaBase + "}");
//        } catch (Exception e) {
//            AndruavMo7arek.log().logException("exception-parser", e);
//            // dont sendMessageToModule bad messages
//            return ;
//        }
        sendCMD(andruav2MR, addTime, instant);

    }



    public void sendCMD(final AndruavBinary_2MR andruavBinary2MR, final boolean addTime)
    {
        sendCMD(andruavBinary2MR,addTime,false);
    }



    public void sendCMD(final AndruavBinary_2MR andruavBinary2MR, final boolean addTime,final  boolean instant)
    {
        try {
            final int state = getSocketState();
            if ((state == SOCKETSTATE_ERROR) || (state == SOCKETSTATE_DISCONNECTED))
            {
                return ;
            }
            //Dont broadcast local messages  - performance-.
            //EventBus.getDefault().post(andruavCMD);
            final byte[] finalMsg = andruavBinary2MR.getJscon(addTime);
            //msg.what=2;
            // msg.obj = finalMsg;

            TotalBinaryBytesSent += finalMsg.length;
            TotalBinaryPacketsSent +=1;
            if (instant)
            {

                socketSendBinaryMessage(finalMsg);

            }
            else
            {
                Message msg =  mhandler.obtainMessage(2,finalMsg);

                mhandler.sendMessageDelayed(msg, 0);

            }

        }
        catch (Exception e)
        {
            // TODO: Send event here to say that you cannot sendMessageToModule.... maybe also you need to disconnect.
            AndruavEngine.log().logException(AndruavSettings.Account_SID, "exception_ws3", e);

            return ;
        }
    }




    public void sendCMD(final Andruav_2MR andruav2MR, final boolean addTime,final  boolean instant)
    {
        try {
            final int state = this.getSocketState();
            if  ((state == SOCKETSTATE_ERROR) || (state == SOCKETSTATE_DISCONNECTED))
            {
                return ;
            }
            //Dont broadcast local messages  - performance-.
            //EventBus.getDefault().post(andruav2MR);
            final String finalMsg = andruav2MR.getJscon(addTime);

            TotalBytesSent += finalMsg.length();
            TotalPacketsSent +=1;

            if (instant)
            {
                socketSendTextMessage(finalMsg);
            }
            else
            {
                if (mhandler == null)
                {
                    // fix an issue
                    return ;
                }
                Message msg = mhandler.obtainMessage(1,finalMsg);

                mhandler.sendMessageDelayed(msg, 0);

            }

        }
        catch (Exception e)
        {
            // TODO: Send event here to say that you cannot sendMessageToModule.... maybe also you need to disconnect.
            AndruavEngine.log().logException(AndruavSettings.Account_SID, "exception_ws3", e);
            return ;
        }
    }

}