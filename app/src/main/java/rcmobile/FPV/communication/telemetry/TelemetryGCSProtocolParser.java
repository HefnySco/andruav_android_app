package rcmobile.FPV.communication.telemetry;

import com.mavlink.MAVLinkPacket;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.TelemetryProtocol;

import rcmobile.FPV.communication.telemetry.BlueTooth.Event_FCBData;
import rcmobile.FPV.communication.telemetry.SerialSocketServer.AndruavGCSSerialSocketServer;
import rcmobile.FPV.communication.telemetry.SerialSocketServer.Event_SocketData;

/**
 * Created by mhefny on 2/15/16.
 */
public class TelemetryGCSProtocolParser extends TelemetryProtocolParser {


    //////////BUS EVENT


    //////////////////////////////

   @Override
    protected void initHandler() {
        super.initHandler();

    }


    public TelemetryGCSProtocolParser() {
        super();

    }



    @Override
    public void shutDown() {
        super.shutDown();
    }

    /***
     * Data is LOCAL comes from here and we should forward it to Drone.
     *
     * @param event_socketData
     */
    @Override
    protected void sendGCSTelemetry (final Event_SocketData event_socketData)
    {
        try
        {
            if (AndruavSettings.remoteTelemetryAndruavWe7da==null)
                return ;

            if (AndruavSettings.remoteTelemetryAndruavWe7da.getTelemetry_protocol() == TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry)  {
                parseDroneKit(event_socketData);

            }
            else
            {
                // BAD PROTOCOL I DONT KNOW WHAT TO DO ... GCS SHOULD KNOW FROM THE BEGINNNING THE DRONE PROTOCOL
            }
        } catch (Exception e) {
            if (exception_init_counter <= 0) return;

            exception_init_counter = exception_init_counter - 1;

            AndruavEngine.log().logException(AndruavSettings.Account_SID, "TeleGCS",e);
        }
    }


    /***
     * Data is remote comming from a Drone. We just forward it to board.
     * Another Listener in {@link AndruavGCSSerialSocketServer} handles actual data delivery
     *
     * @param event_FCBData
     */
    @Override
    protected void sendFCBTelemetry (final Event_FCBData event_FCBData)
    {
        try {

            /*
            A case here is App.remoteTelemetryAndruavWe7da is null because you disabled the telemetry in GCS
            still Drone not aware and you have just received a packet.
             */
            if (AndruavSettings.remoteTelemetryAndruavWe7da == null) return ;

            if (AndruavSettings.remoteTelemetryAndruavWe7da.getTelemetry_protocol() == TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry)
            {
                parseDroneKit(event_FCBData);
            }
            else
            {
                // BAD PROTOCOL I DONT KNOW WHAT TO DO ... GCS SHOULD KNOW FROM THE BEGINNNING THE DRONE PROTOCOL
            }
        } catch (Exception e) {
            if (exception_init_counter <= 0) return;

            exception_init_counter = exception_init_counter - 1;

            AndruavEngine.log().logException(AndruavSettings.Account_SID, "TeleGCS", e);
        }
    }


    /***
     * Data is going from ME the GCS to a Drone
     *
     * @param event_socketData
     */
    protected void parseMavlink(Event_SocketData event_socketData) {

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
             }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            if (tmpMavLinkPacket != null) {
                // We have a ready packet HERE
                if ((event_socketData.targetWe7da!=null) && (!event_socketData.targetWe7da.getIsShutdown())) {
                }
            }
        }
    }

    protected void parseDroneKit (Event_SocketData event_socketData) {

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
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            if (tmpMavLinkPacket != null) {
                // We have a ready packet HERE
                if ((event_socketData.targetWe7da!=null) && (!event_socketData.targetWe7da.getIsShutdown())) {
                    final AndruavUnitBase andruavWe7da = event_socketData.targetWe7da;

                }
            }
        }
    }


    /***
     * Data is comming from a Drone to me GCS
     *
     * @param event_FCBData
     */
    protected void parseMavlink(Event_FCBData event_FCBData) {

        final int len = event_FCBData.DataLength;
        final byte[] data = event_FCBData.Data;

        for (int j = 0; j < len; j++) {


            MAVLinkPacket tmpMavLinkPacket = null;
            try {


                tmpMavLinkPacket = parserMavlinkFCB.mavlink_parse_char(data[j] & 0x00ff);

            }
            catch ( java.lang.IndexOutOfBoundsException e)
            {
                e.printStackTrace();
            }

            if (tmpMavLinkPacket != null) {
                // We have a ready packet HERE

                if ((event_FCBData.senderWe7da!=null) && (event_FCBData.senderWe7da.Equals(AndruavSettings.remoteTelemetryAndruavWe7da))) {

                    final AndruavUnitShadow andruavWe7da = (AndruavUnitShadow) event_FCBData.senderWe7da;
                }
            }
        }
    }



    /***
     * Data is comming from a Drone to me GCS
     *
     * @param event_FCBData
     */
    protected void parseDroneKit (Event_FCBData event_FCBData)
    {
        final int len = event_FCBData.DataLength;
        final byte[] data = event_FCBData.Data;

        for (int j = 0; j < len; j++) {


            MAVLinkPacket tmpMavLinkPacket = null;
            try {


                tmpMavLinkPacket = parserMavlinkFCB.mavlink_parse_char(data[j] & 0x00ff);

            }
            catch ( java.lang.IndexOutOfBoundsException e)
            {
                e.printStackTrace();
            }

            if (tmpMavLinkPacket != null) {
                // We have a ready packet HERE

                if ((event_FCBData.senderWe7da!=null) && (event_FCBData.senderWe7da.Equals(AndruavSettings.remoteTelemetryAndruavWe7da))) {

                    final AndruavUnitShadow andruavWe7da = (AndruavUnitShadow) event_FCBData.senderWe7da;
                }
            }
        }
    }


}
