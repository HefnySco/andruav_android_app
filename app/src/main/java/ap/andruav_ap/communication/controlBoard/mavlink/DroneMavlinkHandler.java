package ap.andruav_ap.communication.controlBoard.mavlink;

import static com.mavlink.enums.MAV_TYPE.MAV_TYPE_ADSB;
import static com.mavlink.enums.MAV_TYPE.MAV_TYPE_GIMBAL;

import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitAllGCS;
import com.andruav.event.fpv7adath.Event_FPV_CMD;
import com.andruav.event.fpv7adath._7adath_InitAndroidCamera;
import com.andruav.interfaces.INotification;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_Ctrl_Camera;
import com.mavlink.ardupilotmega.msg_mount_status;
import com.mavlink.common.msg_attitude;
import com.mavlink.common.msg_command_long;
import com.mavlink.common.msg_heartbeat;
import com.mavlink.common.msg_nav_controller_output;
import com.mavlink.common.msg_rc_channels;
import com.mavlink.common.msg_servo_output_raw;
import com.mavlink.common.msg_statustext;
import com.andruav.AndruavSettings;
import com.andruav.notification.PanicFacade;
import com.mavlink.enums.MAV_CMD;

import ap.andruav_ap.communication.controlBoard.ControlBoard_DroneKit;

/**
 * Static Class that is called ONLY by Drone.IsMe TRUE
 * <br>Class Handles Mavlink messages that can alter internal states.
 * Created by mhefny on 1/9/17.
 */

public class DroneMavlinkHandler {

    private static int rcChannelBlock_trials = 0; // to avoid glitches

    public static int[] channelsRaw = new int[18];


    public static void execute_StatusMessage(final msg_statustext msg_statustext)
    {
        // https://tools.ietf.org/html/rfc5424#section-6.2.1
        // ignore low level messages
        //if (msg_statustext.severity >=5) return;

        int len = msg_statustext.text.length;
        char[] cbuf = new char[len+1];
        for (int i = 0; i < len; i++) {
            cbuf[i] = (char) msg_statustext.text[i];
        }
        //MHefny: status message is max 50 character and is NOT null terminated string when sending 50 character
        cbuf[len]=0;

        PanicFacade.cannotDoAutopilotAction(msg_statustext.severity,msg_statustext.severity,String.valueOf(cbuf),null);
    }

    public static void execute_ServoOutputMessage(final msg_servo_output_raw msg_servo_output_raw) {
        final ControlBoard_DroneKit controlBoard_droneKit = (ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard;

        if (controlBoard_droneKit == null) return ;


        controlBoard_droneKit.execute_ServoOutputMessage(msg_servo_output_raw);
    }

    public static void execute_attitude (msg_attitude msg_attitude)
    {
        final ControlBoard_DroneKit controlBoard_droneKit = (ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard;

        if (controlBoard_droneKit == null) return ;


        controlBoard_droneKit.execute_msg_attitude(msg_attitude);

    }
    public static void execute_NavController (msg_nav_controller_output msg_nav_controller_output)
    {
        final ControlBoard_DroneKit controlBoard_droneKit = (ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard;

        if (controlBoard_droneKit == null) return ;


        controlBoard_droneKit.execute_NavController(msg_nav_controller_output);

    }



    public static void execute_mount_status (final msg_mount_status msg_mount_status)
    {
        final ControlBoard_DroneKit controlBoard_droneKit = (ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard;

        if (controlBoard_droneKit == null) return ;
        controlBoard_droneKit.onDroneEvent_OnGimbalOrientationUpdate(msg_mount_status.pointing_a/100,msg_mount_status.pointing_b/100,msg_mount_status.pointing_c/100);

    }


    /***
     *
     * @param msg_heartbeat
     */
    public static void execute_heartbeat_raw( msg_heartbeat msg_heartbeat)
    {
        if (msg_heartbeat.type>= MAV_TYPE_GIMBAL) return ; // ignore parsing ths ADSB message
        if (msg_heartbeat.compid==0) return; // fix ADSB sensor.
        if (msg_heartbeat.sysid==255) return;
        final ControlBoard_DroneKit controlBoard_droneKit = (ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard;

        if (controlBoard_droneKit == null) return ;
        controlBoard_droneKit.onDroneEvent_HeartBeat (msg_heartbeat.sysid, msg_heartbeat.type, msg_heartbeat.base_mode, msg_heartbeat.system_status, msg_heartbeat.mavlink_version);
    }

    /***
     *
     * @param msg_rc_channels
     */
    public static void execute_rc_channels( msg_rc_channels msg_rc_channels)
    {
        // COPY LATEST CHANNELS FOR Channel Freezeing & Releasing
        channelsRaw[0] =  msg_rc_channels.chan1_raw;
        channelsRaw[1] =  msg_rc_channels.chan2_raw;
        channelsRaw[2] =  msg_rc_channels.chan3_raw;
        channelsRaw[3] =  msg_rc_channels.chan4_raw;
        channelsRaw[4] =  msg_rc_channels.chan5_raw;
        channelsRaw[5] =  msg_rc_channels.chan6_raw;
        channelsRaw[6] =  msg_rc_channels.chan7_raw;
        channelsRaw[7] =  msg_rc_channels.chan8_raw;
        channelsRaw[8] =  msg_rc_channels.chan9_raw;
        channelsRaw[9] =  msg_rc_channels.chan10_raw;
        channelsRaw[10] =  msg_rc_channels.chan11_raw;
        channelsRaw[11] =  msg_rc_channels.chan12_raw;
        channelsRaw[12] =  msg_rc_channels.chan13_raw;
        channelsRaw[13] =  msg_rc_channels.chan14_raw;
        channelsRaw[14] =  msg_rc_channels.chan15_raw;
        channelsRaw[15] =  msg_rc_channels.chan16_raw;
        channelsRaw[16] =  msg_rc_channels.chan17_raw;
        channelsRaw[17] =  msg_rc_channels.chan18_raw;

        rcChannelBlock_trials = rcChannelBlock_trials + 1;

        if ((rcChannelBlock_trials % 5) ==0) {
            rcChannelBlock_trials = 0 ;
            ((ControlBoard_DroneKit) AndruavSettings.andruavWe7daBase.FCBoard).checkBlockingMode();
            ((ControlBoard_DroneKit) AndruavSettings.andruavWe7daBase.FCBoard).checkRCCamSwitch();
        }
    }

    public static void execute_command_long (msg_command_long mavLinkMessage)
    {
        switch(mavLinkMessage.command)
        {
            case MAV_CMD.MAV_CMD_DO_DIGICAM_CONTROL: {
                AndruavEngine.getEventBus().post(new _7adath_InitAndroidCamera());

                Event_FPV_CMD event_fpv_cmd = new Event_FPV_CMD(Event_FPV_CMD.FPV_CMD_TAKEIMAGE);
                event_fpv_cmd.CameraSource = AndruavMessage_Ctrl_Camera.CAMERA_SOURCE_MOBILE;
                event_fpv_cmd.NumberOfImages = 1;
                event_fpv_cmd.TimeBetweenShotes = 0;
                event_fpv_cmd.DistanceBetweenShotes = 0;
                event_fpv_cmd.SendBackImages = true;
                event_fpv_cmd.SaveImageLocally = true;
                event_fpv_cmd.Requester = new AndruavUnitAllGCS(); //.getPartyID();
                AndruavEngine.getEventBus().post(event_fpv_cmd);
                break;
            }
        }
    }

}
