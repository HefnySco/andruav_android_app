package ap.andruav_ap.communication.controlBoard.mavlink;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.common.msg_open_drone_id_basic_id;
import com.MAVLink.common.msg_open_drone_id_location;
import com.MAVLink.minimal.msg_heartbeat;
import com.MAVLink.common.msg_rc_channels;
import com.MAVLink.messages.MAVLinkMessage;
import com.MAVLink.ardupilotmega.msg_mount_status;
import com.MAVLink.common.msg_command_ack;
import com.MAVLink.common.msg_nav_controller_output;
import com.MAVLink.common.msg_servo_output_raw;
import com.MAVLink.common.msg_statustext;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.Constants;
import com.o3dr.android.client.MavlinkObserver;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;

import ap.andruav_ap.communication.controlBoard.ControlBoard_DroneKit;
import com.andruav.event.fcb_event.Event_FCBData;
import ap.andruav_ap.communication.telemetry.DroneKit.DroneKitServer;
import com.andruav.event.fcb_event.Event_SocketData;
import ap.andruavmiddlelibrary.preference.Preference;

/**
 * DroneKit Mavlink Receiver class.
 * Created by mhefny on 1/9/17.
 */

public class DroneKitMavlinkObserver extends MavlinkObserver
{

    public int sysid;
    public int compid;


    protected final DroneKitServer mDroneKitServer;

    public DroneKitMavlinkObserver (final DroneKitServer droneKitServer)
    {
        mDroneKitServer = droneKitServer;
    }

    /**
     * Called when a Mavlink is received from FCB.
     * @param mavlinkMessageWrapper
     */
    @Override
    public void onMavlinkMessageReceived(MavlinkMessageWrapper mavlinkMessageWrapper) {

        try {
            final MAVLinkMessage mavLinkMessage = mavlinkMessageWrapper.getMavLinkMessage();


            switch (mavLinkMessage.msgid)
            {
//                case msg_open_drone_id_basic_id.MAVLINK_MSG_ID_OPEN_DRONE_ID_BASIC_ID:
//                    break;
//
//                case msg_open_drone_id_location.MAVLINK_MSG_ID_OPEN_DRONE_ID_LOCATION:
//                    break;

                case msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT:
                    DroneMavlinkHandler.execute_heartbeat_raw((msg_heartbeat) mavLinkMessage);
                    break;

                case msg_rc_channels.MAVLINK_MSG_ID_RC_CHANNELS:
                    DroneMavlinkHandler.execute_rc_channels((msg_rc_channels) mavLinkMessage);
                    break;

                case msg_attitude.MAVLINK_MSG_ID_ATTITUDE:
                    DroneMavlinkHandler.execute_attitude((msg_attitude) mavLinkMessage);
                    break;

                case msg_nav_controller_output.MAVLINK_MSG_ID_NAV_CONTROLLER_OUTPUT:
                    DroneMavlinkHandler.execute_NavController((msg_nav_controller_output) mavLinkMessage);
                    break;

                case msg_mount_status.MAVLINK_MSG_ID_MOUNT_STATUS:

                    DroneMavlinkHandler.execute_mount_status ((msg_mount_status)mavLinkMessage);
                    break;

                case msg_command_ack.MAVLINK_MSG_ID_COMMAND_ACK:
                    mDroneKitServer.HandleAckMessage((msg_command_ack)mavLinkMessage);
                    break;

                case msg_statustext.MAVLINK_MSG_ID_STATUSTEXT:
                    DroneMavlinkHandler.execute_StatusMessage ((msg_statustext)mavLinkMessage);
                    break;

                case msg_servo_output_raw.MAVLINK_MSG_ID_SERVO_OUTPUT_RAW:
                    DroneMavlinkHandler.execute_ServoOutputMessage ((msg_servo_output_raw)mavLinkMessage);
                    break;

                case msg_command_long.MAVLINK_MSG_ID_COMMAND_LONG:
                    DroneMavlinkHandler.execute_command_long ((msg_command_long) mavLinkMessage);
                    break;
            }


            final MAVLinkPacket mavLinkPacket = mavLinkMessage.pack();


            boolean bsend;
            final int isSmartTelemetry = Preference.getSmartMavlinkTelemetry(null);
            if (isSmartTelemetry > Constants.SMART_TELEMETRY_LEVEL_0) {
                bsend = TrafficOptimizer.shouldSend(mavLinkPacket,isSmartTelemetry);
                if (!bsend) return ; /// dont sendMessageToModule for optimization purpose
            }

            mavLinkPacket.sysid = mavLinkMessage.sysid;
            mavLinkPacket.compid = mavLinkMessage.compid;


            if (!(AndruavSettings.andruavWe7daBase.FCBoard instanceof ControlBoard_DroneKit)) return;
            sysid = mavLinkMessage.sysid;
            compid = mavLinkMessage.compid;
            Event_FCBData event_FCBData = new Event_FCBData();
            event_FCBData.mavLinkPacket = mavLinkPacket;
            event_FCBData.IsLocal = Event_SocketData.SOURCE_LOCAL;
            AndruavEngine.getEventBus().post(event_FCBData);


            return;
        }
        catch (Exception e)
        {
           e.printStackTrace();
        }
    }
}
