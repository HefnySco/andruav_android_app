package rcmobile.FPV.communication.controlBoard.mavlink;

import com.mavlink.ardupilotmega.msg_mount_status;
import com.mavlink.common.msg_heartbeat;
import com.mavlink.common.msg_nav_controller_output;
import com.mavlink.common.msg_rc_channels_raw;
import com.mavlink.common.msg_servo_output_raw;
import com.mavlink.common.msg_statustext;
import com.andruav.AndruavSettings;
import com.andruav.notification.PanicFacade;

import rcmobile.FPV.communication.controlBoard.ControlBoard_DroneKit;

/**
 * Static Class that is called ONLY by Drone.IsMe TRUE
 * <br>Class Handles Mavlink messages that can alter internal states.
 * Created by mhefny on 1/9/17.
 */

public class DroneMavlinkHandler {

    private static int rcChannelBlock_trials = 0; // to avoid glitches

    public static int[] channelsRaw = new int[8];


    public static void execute_StatusMessage(final msg_statustext msg_statustext)
    {
        // https://tools.ietf.org/html/rfc5424#section-6.2.1
        // ignore low level messages
        if (msg_statustext.severity >=5) return;

        int len = msg_statustext.text.length;
        char[] cbuf = new char[len+1];
        for (int i = 0; i < len; i++) {
            cbuf[i] = (char) msg_statustext.text[i];
        }
        //MHefny: status message is max 50 character and is NOT null terminated string when sending 50 character
        cbuf[len]=0;


        PanicFacade.cannotDoAutopilotAction(String.valueOf(cbuf));
    }

    public static void execute_ServoOutputMessage(final msg_servo_output_raw msg_servo_output_raw) {
        final ControlBoard_DroneKit controlBoard_droneKit = (ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard;

        if (controlBoard_droneKit == null) return ;


        controlBoard_droneKit.execute_ServoOutputMessage(msg_servo_output_raw);
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
        final ControlBoard_DroneKit controlBoard_droneKit = (ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard;

        if (controlBoard_droneKit == null) return ;
        controlBoard_droneKit.onDroneEvent_HeartBeat (msg_heartbeat.type, msg_heartbeat.base_mode, msg_heartbeat.system_status, msg_heartbeat.mavlink_version);
    }

    /***
     *
     * @param msg_rc_channels_raw
     */
    public static void execute_rc_channel_raw( msg_rc_channels_raw msg_rc_channels_raw)
    {
        // COPY LATEST CHANNELS FOR Channel Freezeing & Releasing
        channelsRaw[0] =  msg_rc_channels_raw.chan1_raw;
        channelsRaw[1] =  msg_rc_channels_raw.chan2_raw;
        channelsRaw[2] =  msg_rc_channels_raw.chan3_raw;
        channelsRaw[3] =  msg_rc_channels_raw.chan4_raw;
        channelsRaw[4] =  msg_rc_channels_raw.chan5_raw;
        channelsRaw[5] =  msg_rc_channels_raw.chan6_raw;
        channelsRaw[6] =  msg_rc_channels_raw.chan7_raw;
        channelsRaw[7] =  msg_rc_channels_raw.chan8_raw;


//        // BLocking Section
//        if (!Preference.isRCBlockEnabled(null))
//        {
//            AndruavSettings.andruavWe7daBase.FCBoard.isRCChannelBlocked(false);
//            return;
//        }
//
//        final int channelNum= Preference.getChannelRCBlock(null);
//        final int channelValue;
//        switch (channelNum)
//        {
//            case 1:
//                channelValue = msg_rc_channels_raw.chan1_raw;
//                break;
//            case 2:
//                channelValue = msg_rc_channels_raw.chan2_raw;
//                break;
//            case 3:
//                channelValue = msg_rc_channels_raw.chan3_raw;
//                break;
//            case 4:
//                channelValue = msg_rc_channels_raw.chan4_raw;
//                break;
//            case 5:
//                channelValue = msg_rc_channels_raw.chan5_raw;
//                break;
//            case 6:
//                channelValue = msg_rc_channels_raw.chan6_raw;
//                break;
//            case 7:
//                channelValue = msg_rc_channels_raw.chan7_raw;
//                break;
//            case 8:
//                channelValue = msg_rc_channels_raw.chan8_raw;
//                break;
//            default:
//                return;
//        }
//
//        rcChannelBlock_trials = rcChannelBlock_trials + 1;
//
//
//        final boolean block = channelValue >= Preference.getChannelRCBlock_min_value(null);
//
//        // if (rcChannelBlock_trials >=3)
//        // {
//        rcChannelBlock_trials =0;
//        //if ((rcChannelBlock != block) && block
//
//        AndruavSettings.andruavWe7daBase.FCBoard.isRCChannelBlocked(block);
//        //TODO:  Broadcast status & add it to extra ID Messages
        // }

        rcChannelBlock_trials = rcChannelBlock_trials + 1;

        if ((rcChannelBlock_trials % 5) ==0) {
            rcChannelBlock_trials = 0 ;
            ((ControlBoard_DroneKit) AndruavSettings.andruavWe7daBase.FCBoard).checkBlockingMode();
        }
    }
}
