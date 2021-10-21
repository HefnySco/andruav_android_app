package rcmobile.FPV.communication.telemetry;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.mavlink.Parser;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.TelemetryProtocol;
import com.andruav.controlBoard.ControlBoardBase;

import de.greenrobot.event.EventBus;
import rcmobile.FPV.App;
import rcmobile.FPV.communication.telemetry.BlueTooth.Event_FCBData;
import com.andruav.event.systemEvent.Event_ShutDown_Signalling;
import rcmobile.FPV.communication.telemetry.SerialSocketServer.Event_SocketAction;
import rcmobile.FPV.communication.telemetry.SerialSocketServer.Event_SocketData;

/**
 * Created by mhefny on 2/15/16.
 */
public class TelemetryProtocolParser {

    //////// Attributes
    protected Parser parserMavlinkGCS;
    protected Parser parserMavlinkFCB;


    protected boolean mkillMe;
    protected Handler mhandler;
    protected HandlerThread mhandlerThread;

    protected int exception_init_counter = 5;

    //////////////////////////////

    //////////BUS EVENT

    public void onEvent(Event_ShutDown_Signalling event) {
        if (event.CloseOrder != 2) return;


        this.shutDown();
        App.telemetryProtocolParser = null;
    }

    public void onEvent(Event_SocketAction eventSocketAction) {
        try {
            if (eventSocketAction.socketAction == Event_SocketAction.SOCKETACTION_CLIENT_DISCONNECTED) {
                if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
                    AndruavSettings.andruavWe7daBase.setTelemetry_protocol(TelemetryProtocol.TelemetryProtocol_No_Telemetry);
                }
            }
        }
        catch ( Exception e)
        {
            AndruavEngine.log().logException("gcs-exception1", e);
        }
    }



    /**
     * Bluetooth or USB data that should be sent to mTelemetryRequests units.
     *
     * @param event event.IsLocal = true : means I am probably a Drone and sending this data to GCS
     */
    public void onEvent(final Event_FCBData event) {

        if (event.IsLocal == Event_SocketData.SOURCE_SIMULATED) return; // this could be  a loopback

        sendFCBTelemetry(event);

    }


    /**
     * Receieve event from SerialSocketListener used for 3rd Party connection
     * Data should be sent to single target only as this is GCS software.
     *
     * @param event event.IsLocal = {@link Event_SocketData}.SOURCE_LOCAL: means I am a GCS and I am sending this data to other drones.
     */
    public void onEvent(Event_SocketData event) {

        if (App.iEvent_socketData != null)
        {
            final ControlBoardBase controlBoardBase = AndruavSettings.andruavWe7daBase.FCBoard;
            if ((controlBoardBase ==null) // no control board defined
            || (       // or blocked
                    (controlBoardBase.do_RCChannelBlocked()))  // << RC BLOCKING
                && (!event.byPassBlockedGCS))
            {
                return;
            }

            // Send Binary packets to FCB HW board directly.
            App.iEvent_socketData.onSocketData(event);
        }

        if (event.IsLocal == Event_SocketData.SOURCE_SIMULATED) return; // this could be  a loopback


        // Parse and sendMessageToModule binary data to FCB Classes
        sendGCSTelemetry(event);

    }


////////////////////////////////////////



    public TelemetryProtocolParser() {

        parserMavlinkFCB = new Parser();
        parserMavlinkGCS = new Parser();
        initHandler();
        EventBus.getDefault().register(this);


    }


    protected void initHandler() {
        mhandlerThread = new HandlerThread("WS_SendCMD");
        mhandlerThread.start(); //mhandlerThread.getLooper() will return nll if not started.



        // Note that sendGCSTelemetry & sendFCBTelemetry have different implementations
        // and meaning based on the iimplementer GCS or Drone
        mhandler = new Handler(mhandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                try {


                    // Data from GCS
                    if (msg.obj instanceof Event_SocketData) {
                        Event_SocketData event_socketData = (Event_SocketData) msg.obj;


                        sendGCSTelemetry(event_socketData);

                        return;
                    }

                    // Data from FCB or Drone
                    if (msg.obj instanceof Event_FCBData) {
                        Event_FCBData event_FCBData = (Event_FCBData) msg.obj;
                        sendFCBTelemetry(event_FCBData);


                        return;
                    }

                } catch (Exception e) {
                    //Log.e("WS", e.getMessage());
                    if (exception_init_counter <= 0) return;
                    exception_init_counter = exception_init_counter - 1;

                    AndruavEngine.log().logException(AndruavSettings.AccessCode, "exception_tele_parser", e);

                }
            }
        };
    }



    protected AndruavUnitBase getAndruavUnit(String targetName) {

        if ((AndruavSettings.andruavWe7daBase.getIsCGS())) {
            return  AndruavEngine.getAndruavWe7daMapBase().get(targetName);
        }

        return  AndruavSettings.andruavWe7daBase;

    }



    public void shutDown() {
        try {
            EventBus.getDefault().unregister(this);

            mkillMe = true;

            if (mhandlerThread != null) {
                mhandlerThread.quit();
            }
        } catch (Exception e) {
            AndruavEngine.log().logException("teleprotoparser", e);
        }
    }


    protected void sendGCSTelemetry (final Event_SocketData event_socketData)
    {

    }


    protected void sendFCBTelemetry (final Event_FCBData event_FCBData)
    {

    }

}