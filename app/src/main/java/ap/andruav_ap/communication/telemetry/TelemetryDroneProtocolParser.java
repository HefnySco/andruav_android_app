package ap.andruav_ap.communication.telemetry;


import com.mavlink.MAVLinkPacket;
import com.mavlink.Parser;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.TelemetryProtocol;
import com.andruav.interfaces.INotification;

import ap.andruav_ap.App;

import ap.andruav_ap.communication.controlBoard.ControlBoard_DroneKit;
import ap.andruav_ap.communication.telemetry.BlueTooth.Event_FCBData;
import ap.andruav_ap.communication.telemetry.SerialSocketServer.Event_SocketData;
import ap.andruavmiddlelibrary.eventClasses.remoteControl.Event_ProtocolChanged;
import ap.andruav_ap.R;

/**
 * Created by M.Hefny on 14-Apr-15.
 */
public class TelemetryDroneProtocolParser  extends TelemetryProtocolParser{

    //////// Attributes


    //protected MAVLinkPacket tmpMavLinkPacket = null;
    //protected MWPacket tmpMWPacket = null;



    //////////////////////////////

    //////////BUS EVENT




    public void onEvent(Event_ProtocolChanged event_protocolChanged) {

        switch (AndruavSettings.andruavWe7daBase.telemetry_protocol) {
            case TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry:
                break;
            case TelemetryProtocol.TelemetryProtocol_Unknown_Telemetry:
                mhandler.postDelayed(TestProtocol, 1500);
                break;
            case TelemetryProtocol.TelemetryProtocol_No_Telemetry:
                break;

            default:
                break;
        }
    }

    //////////EOF BUS EVENT

    private int protocolTestSwitch = 0;

    long delayMillis = 2000;

    protected Runnable TestProtocol = new Runnable() {
        @Override
        public void run() {
            boolean brepeat = true;
            switch (AndruavSettings.andruavWe7daBase.getTelemetry_protocol()) {
                case TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry:
                case TelemetryProtocol.TelemetryProtocol_No_Telemetry:
                    brepeat = false;
                    break;
                case TelemetryProtocol.TelemetryProtocol_Unknown_Telemetry:
                    switch (protocolTestSwitch) {
                        case 0:
                            protocolTestSwitch = protocolTestSwitch + 1;
                            PropIfMavLink();
                            break;
                        case 1:
                            protocolTestSwitch = 0;
                            delayMillis = delayMillis + 1500;
                            String serr = App.getAppContext().getString(R.string.andruav_error_telemetryprotocol_undef);
                            App.notification.displayNotification(INotification.NOTIFICATION_TYPE_WARNING, "Warning", serr, true, INotification.INFO_TYPE_TELEMETRY, false);
                            AndruavEngine.notification().Speak(serr);
                            brepeat = true;
                            break;
                    }
                    break;
            }
            if (brepeat) mhandler.postDelayed(this, delayMillis);


        }
    };


    @Override
    protected void initHandler() {
        super.initHandler();


    }

    @Override
    public void shutDown() {
        super.shutDown();
    }



    public TelemetryDroneProtocolParser()
    {
        super();


    }


    /***
     * Data is Remote comes from GCS and we should parse it and forward it to my FCB & attached board..
     *
     * @param event_socketData
     */
    @Override
    protected void sendGCSTelemetry (final Event_SocketData event_socketData)
    {
        try
        {

            if ((AndruavSettings.andruavWe7daBase.getTelemetry_protocol() == TelemetryProtocol.TelemetryProtocol_Unknown_Telemetry) || (AndruavSettings.andruavWe7daBase.getTelemetry_protocol() == TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry)) {
                parseDroneKit(event_socketData);

            }

            return;

        } catch (Exception e) {
            if (exception_init_counter <= 0) return;

            exception_init_counter = exception_init_counter - 1;

            AndruavEngine.log().logException(AndruavSettings.Account_SID, "TeleGCS",e);
        }
    }


    @Override
    protected void sendFCBTelemetry (final Event_FCBData event_FCBData)
    {
        try
        {

            final int telemetry_protocol = AndruavSettings.andruavWe7daBase.getTelemetry_protocol();
            if ((telemetry_protocol == TelemetryProtocol.TelemetryProtocol_Unknown_Telemetry) || (telemetry_protocol == TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry))
            {
                parseDroneKit(event_FCBData);
                return ;
            }

            return;

        } catch (Exception e) {
            if (exception_init_counter <= 0) return;

            exception_init_counter = exception_init_counter - 1;

            AndruavEngine.log().logException(AndruavSettings.Account_SID, "TeleGCS",e);
        }

    }


    protected void parseDroneKit(Event_SocketData event_socketData)
    {
        final int len = event_socketData.DataLength;
        final byte[] data = event_socketData.Data;

        for (int j = 0; j < len; j++) {
            MAVLinkPacket tmpMavLinkPacket = null;
            try {


                tmpMavLinkPacket = parserMavlinkGCS.mavlink_parse_char(data[j] & 0x00ff);

            }
            catch ( java.lang.IndexOutOfBoundsException e)
            {
                tmpMavLinkPacket = null;
                parserMavlinkGCS.stats.resetStats(); //.mavlinkResetStats(); //.resetStats();
                parserMavlinkGCS = new Parser();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                parserMavlinkGCS.stats.resetStats(); //.mavlinkResetStats(); //.resetStats();
                parserMavlinkGCS = new Parser();
            }

            if (tmpMavLinkPacket != null) {
                // We have a ready packet HERE
                if (AndruavSettings.andruavWe7daBase.getTelemetry_protocol() == TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry)
                {
                    final ControlBoard_DroneKit controlBoard_droneKit =(ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard;

                }

            }
        }
    }





    /***
     * Data is going from Me Drone to GCS
     *
     * @param event_FCBData
     */
    protected void parseDroneKit(final Event_FCBData event_FCBData)
    {


        final ControlBoard_DroneKit controlBoard_droneKit =(ControlBoard_DroneKit) AndruavSettings.andruavWe7daBase.FCBoard;
        /**
         We forward FCB back to board-class so that board-class must be synched with the FCB.
         */
        controlBoard_droneKit.Execute(event_FCBData.mavLinkPacket, true);

    }


    protected void OnProtocolDetected(final int telemetryProtocol) {
        try {


            AndruavSettings.andruavWe7daBase.setTelemetry_protocol(telemetryProtocol);

            String serr;

            switch (telemetryProtocol) {
                case TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry:
                    AndruavSettings.andruavWe7daBase.useFCBIMU(true);
                    serr = App.getAppContext().getString(R.string.andruav_error_telemetryprotocol_mavlink);
                    App.notification.displayNotification(INotification.NOTIFICATION_TYPE_NORMAL, "Info", serr, true, INotification.INFO_TYPE_TELEMETRY, false);
                    AndruavEngine.notification().Speak(serr);
                    break;
                case TelemetryProtocol.TelemetryProtocol_No_Telemetry:
                    break;
                case TelemetryProtocol.TelemetryProtocol_Unknown_Telemetry:
                    break;
            }

        } catch (Exception e) {
            if (exception_init_counter > 0) {
                exception_init_counter = exception_init_counter - 1;
                AndruavEngine.log().logException("teleprotoparser", e);
            }
        }
    }

    public  void PropIfMavLink() {
        if (parserMavlinkFCB != null) {
            parserMavlinkFCB = new Parser();
        }
        if (parserMavlinkGCS != null) {
            parserMavlinkGCS = new Parser();
        }
        // BUG APM BUG HERE


        //Lo7Ta7akom_APM_Drone.initAPM();
    }
}
