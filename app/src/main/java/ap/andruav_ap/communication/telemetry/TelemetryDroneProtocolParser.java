package ap.andruav_ap.communication.telemetry;


import com.MAVLink.Parser;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.TelemetryProtocol;
import com.andruav.interfaces.INotification;

import ap.andruav_ap.App;

import ap.andruav_ap.communication.controlBoard.ControlBoard_DroneKit;
import com.andruav.event.fcb_event.Event_FCBData;
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


    public  void PropIfMavLink() {
        if (parserMavlinkFCB != null) {
            parserMavlinkFCB = new Parser();
        }
        if (parserMavlinkGCS != null) {
            parserMavlinkGCS = new Parser();
        }
    }
}
