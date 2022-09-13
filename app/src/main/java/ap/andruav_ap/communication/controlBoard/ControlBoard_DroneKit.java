package ap.andruav_ap.communication.controlBoard;

import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_REGISTERED;
import static com.mavlink.common.msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT;

import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.collection.SimpleArrayMap;

import com.andruav.controlBoard.shared.missions.MissionCameraTrigger;
import com.andruav.controlBoard.shared.missions.MissionCameraControl;
import com.andruav.event.Event_Remote_ChannelsCMD;
import com.andruav.event.droneReport_Event.Event_GPS_Ready;
import com.andruav.event.fpv7adath.Event_FPV_CMD;
import com.andruav.sensors.AndruavIMU;
import com.mavlink.MAVLinkPacket;
import com.mavlink.common.msg_attitude;
import com.mavlink.common.msg_heartbeat;
import com.mavlink.common.msg_nav_controller_output;
import com.mavlink.common.msg_param_value;
import com.mavlink.common.msg_rc_channels_override;
import com.mavlink.common.msg_servo_output_raw;
import com.andruav.AndruavFacade;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.event.fcb_event.Event_FCB_RemoteControlSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.droneReport_Event.Event_WayPointsRecieved;
import com.andruav.sensors.AndruavGimbal;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.notification.PanicFacade;
import com.andruav.controlBoard.IControlBoard_Callback;
import com.andruav.controlBoard.shared.common.VehicleTypes;
import com.andruav.controlBoard.shared.missions.MissionBase;
import com.andruav.controlBoard.shared.missions.MissionDayra;
import com.andruav.controlBoard.shared.missions.MissionEkla3;
import com.andruav.controlBoard.shared.missions.MissionHoboot;
import com.andruav.controlBoard.shared.missions.MohemmaMapBase;
import com.andruav.controlBoard.shared.missions.MissionROI;
import com.andruav.controlBoard.shared.missions.MissionRTL;
import com.andruav.controlBoard.shared.missions.SplineMission;
import com.andruav.controlBoard.shared.missions.WayPointStep;
import com.andruav.util.GPSHelper;
import com.mavlink.enums.MAV_COMPONENT;
import com.mavlink.enums.MAV_MODE_FLAG;
import com.mavlink.enums.MAV_STATE;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.MissionItemType;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.mission.item.command.CameraControl;
import com.o3dr.services.android.lib.drone.mission.item.command.CameraTrigger;
import com.o3dr.services.android.lib.drone.mission.item.command.ResetROI;
import com.o3dr.services.android.lib.drone.mission.item.command.ReturnToLaunch;
import com.o3dr.services.android.lib.drone.mission.item.command.Takeoff;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Circle;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Land;
import com.o3dr.services.android.lib.drone.mission.item.spatial.SplineWaypoint;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.util.MathUtils;

import org.json.JSONException;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.App;
import ap.andruav_ap.communication.controlBoard.mavlink.DroneMavlinkHandler;
import ap.andruav_ap.helpers.RemoteControl;
import ap.andruav_ap.communication.controlBoard.mavlink.MavLink_Helpers;
import ap.andruavmiddlelibrary.sensors._7asasatEvents.Event_GPS_NMEA;
import ap.andruavmiddlelibrary.eventClasses.remoteControl.Event_RemoteServo;
import ap.andruavmiddlelibrary.factory.math.Angles;
import ap.andruavmiddlelibrary.preference.Preference;

import static org.droidplanner.services.android.impl.core.MAVLink.MavLinkCommands.MAVLINK_SET_POS_TYPE_MASK_ACC_IGNORE;
import static org.droidplanner.services.android.impl.core.MAVLink.MavLinkCommands.MAVLINK_SET_POS_TYPE_MASK_POS_IGNORE;
import static org.droidplanner.services.android.impl.core.MAVLink.MavLinkCommands.MAVLINK_SET_POS_TYPE_MASK_YAW_IGNORE;

/**
 * Created by M.Hefny on 19/07/2022
 */
public class ControlBoard_DroneKit extends ControlBoard_MavlinkBase {

    /**
     * Location of Guided point.
     */
    private LatLongAlt guided_LngLat;
    private final ControlBoard_DroneKit Me;
    private Handler mhandle;
    private HandlerThread mhandlerThread;
    private State vehicleState;
    private final int INTERNAL_CMD_NON           =0;                // no internal commands required
    private boolean canFly = false;
    /***
     * step 1: get home mLocation
     * step 2: get mission
     */
    private final int INTERNAL_GET_HOME_MISSION  =1;
    private final int INTERNAL_CMD_WAYPOINTS     =2;

    private int mInternalCommand= INTERNAL_CMD_NON;
    private int mInternalCommand_Step= 0;

    private int[] channelsshared;
    int[] safeGuidedChannels = new int[8];

    private boolean rc_command =false;
    private long  rc_command_last = 0;

    public static final int  GPS_TYPE_NONE      = 0;
    public static final int  GPS_TYPE_AUTO      = 1;
    public static final int  GPS_TYPE_UBLOX     = 2;
    public static final int  GPS_TYPE_NMEA      = 5;
    public static final int  GPS_TYPE_MAV       = 14;

    private int mGPS1_Type = GPS_TYPE_NONE;
    private int mGPS2_Type = GPS_TYPE_NONE;

    private int mRCMAP_ROLL = -1;
    private int mRCMAP_PITCH = -1;
    private int mRCMAP_THROTTLE = -1;
    private int mRCMAP_YAW = -1;

    private boolean mParameteredRefreshedCompleted = false;

    private int mSysId;
    private short mType;

    private int rcCamera;

    /***
     * mGPS_MAV_NUM 0:send to first GPS,1:send to 2nd GPS,127:send to all
     * mGPS_MAV_NUM is only valid if mGPS1_Type or mGPS2_Type = GPS_TYPE_MAV
     */
    private int mGPS_MAV_NUM = 999;


    private void sendCameraHeartBeat()
    {
        sendHeartBeat(mSysId, MAV_COMPONENT.MAV_COMP_ID_CAMERA);
    }

    private void sendHeartBeat(final int sysid, final int compid)
    {
        msg_heartbeat msg_heartbeat = new msg_heartbeat();
        msg_heartbeat.sysid =  sysid;
        msg_heartbeat.compid = compid;
        final MavlinkMessageWrapper mavlinkMessageWrapper = new MavlinkMessageWrapper(msg_heartbeat);
        if (App.droneKitServer == null) return ;
        App.droneKitServer.sendMavlink (mavlinkMessageWrapper);

    }

    private static final long RC_COMMAND_TIME_OUT   = 3500;

    private void sendRCChannelsRepeater ()
    {
        if (rc_command && (!rcChannelBlock))
        {

            // Safety if no remote signal has been sent you need to break.
            if ((System.currentTimeMillis() - rc_command_last) > RC_COMMAND_TIME_OUT)
            {
                rc_command = false;

            /*
                Note that in guided mode rc_command is already false as navigation is done through
                velocity control.

             */
                App.droneKitServer.do_Brake(null);
            }
            else
            {
                sendRCChannels (AndruavSettings.andruavWe7daBase.getManualTXBlockedSubAction(),channelsshared,false);
            }
        }
    }


    /**
     * ScheduledExecutorService used to periodically schedule the rcRepeater.
     */
    private ScheduledExecutorService rcRepeater;

    /**
     * Runnable used to sendMessageToModule the rcRunnable & Other repeated messages.
     */
    private final Runnable ReapeterCommunicatorRunnable = new Runnable() {
        private int rate = 0;
        @Override
        public void run() {
            try {
                sendRCChannelsRepeater();
                if (rate%5==0) {
                    sendCameraHeartBeat();
                }
                ++rate;
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        }
    };


    /***
     * This handles are created for Drone Andruav only.
     */
    protected void initHandler () {
        if (!this.mAndruavUnitBase.IsMe()) {
            // if I am not the Drone Owner Thread  I dont need this.
            return;
        }
        mhandlerThread = new HandlerThread("DroneAPI_Thread");
        mhandlerThread.start();
        mhandle = new Handler(mhandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
            }
        };

    }

    public void onEvent (final Event_GPS_Ready a7adath_gps_ready) throws JSONException {

        try {

            if ((mGPS1_Type != GPS_TYPE_MAV) && ((mGPS2_Type != GPS_TYPE_MAV)))
            {
                return ;
            }

            final AndruavIMU andruavIMU_Mobile = AndruavSettings.andruavWe7daBase.getMobileGPS();
            final Location mobileLocation = andruavIMU_Mobile.getCurrentLocation();
            final long GPS_LEAPSECONDS_MILLIS = 18000;
            final long AP_SEC_PER_WEEK   = (7 * 86400);
            final long AP_MSEC_PER_SEC  = 1000;

            final long epoch = 86400*(10*365 + (1980-1969)/4 + 1 + 6 - 2) - (GPS_LEAPSECONDS_MILLIS / 1000);
            final long t_ms = mobileLocation.getTime() / 1000;
            final int epoch_seconds = (int)(t_ms  - epoch);
            final int time_week = 1721; //(int) (epoch_seconds / AP_SEC_PER_WEEK);
            // round time to nearest 200ms AndruavResala_RemoteControl2
            long time_week_ms =  System.currentTimeMillis() + 3*60*60*1000 + 37000; //(int)((epoch_seconds % AP_SEC_PER_WEEK) * AP_MSEC_PER_SEC + ((int)(t_ms/200) * 200));

            short fixStatus = (short)andruavIMU_Mobile.GPS3DFix;
            if (andruavIMU_Mobile.GPSFixQuality>3)
            {
                fixStatus = (short)andruavIMU_Mobile.GPSFixQuality;

                // fixStatus = 0-1: no fix, 2: 2D fix, 3: 3D fix. 4: 3D with DGPS. 5: 3D with RTK
                // SO for values less than 4 then use true 3DFix status... otherwise check 4 & 5 values in QUalityFix
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (Preference.isGPSInjecttionEnabled(null)) {
                    this.do_InjectGPS(System.currentTimeMillis() * 1000,
                            time_week_ms, time_week, fixStatus,
                            (int) (mobileLocation.getLatitude() * 1.0e7), (int) (mobileLocation.getLongitude() * 1.0e7),
                            (int) mobileLocation.getAltitude(), andruavIMU_Mobile.SATC,
                            andruavIMU_Mobile.Hdop, andruavIMU_Mobile.Vdop,
                            mobileLocation.getSpeedAccuracyMetersPerSecond(), mobileLocation.getAccuracy(), mobileLocation.getVerticalAccuracyMeters(), mGPS_MAV_NUM);
                }
            }
        }
        catch (final Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void onEvent (final Event_GPS_NMEA event_gps_nmea) throws JSONException {

        if ((mGPS1_Type != GPS_TYPE_NMEA) && ((mGPS2_Type != GPS_TYPE_NMEA)))
        {
            return ;
        }

        try {
            this.do_InjectGPS_NMEA(event_gps_nmea.nmea);
        }
        catch (final Exception ex)
        {

        }
    }

    public void onEvent (final Event_RemoteServo event_remoteServo)
    {
        sendServoChannel (event_remoteServo.ChannelNumber, event_remoteServo.ChannelValue);
    }



    public  void onEvent (final Event_Remote_ChannelsCMD a7adathRemote_channelsCMD)
    {

        if ((do_RCChannelBlocked()) || (!a7adathRemote_channelsCMD.partyID.equals(this.mAndruavUnitBase.PartyID))) return ;
        channelsshared = RemoteControl.calculateChannels3(a7adathRemote_channelsCMD.channels, true);
        rc_command = true;
        rc_command_last = System.currentTimeMillis();
        /////////////////TODO: Please update logic and remove this hack

        sendRCChannels(mAndruavUnitBase.getManualTXBlockedSubAction(), channelsshared,false);
    }


    public void onEvent (final Event_FCB_RemoteControlSettings event)
    {
        int[] channels = new int[8];

        event.rcSubAction = adjustRCActionByMode (event.rcSubAction, mAndruavUnitBase.getFlightModeFromBoard());

        switch (event.rcSubAction) {

            case Event_FCB_RemoteControlSettings.RC_SUB_ACTION_CENTER_CHANNELS: {
                activate_Rc_sub_action_center_channels();
            }
            break;

            case Event_FCB_RemoteControlSettings.RC_SUB_ACTION_FREEZE_CHANNELS: {
                activate_Rc_sub_action_freeze_channels();
            }
            break;

            case Event_FCB_RemoteControlSettings.RC_SUB_ACTION_JOYSTICK_CHANNELS: {
                activate_Rc_sub_action_joystick_channels();
            }
            break;


            case Event_FCB_RemoteControlSettings.RC_SUB_ACTION_JOYSTICK_CHANNELS_GUIDED: {
                activate_Rc_sub_action_channel_guided();
            }
            break;


            case Event_FCB_RemoteControlSettings.RC_SUB_ACTION_RELEASED: {
                // Release all channels by setting them to Zero.
                // TODO: check if this affects servos or you use higher channels.
                for (int i=0;i<8;++i)
                {
                    channels[i]=0;
                }
                //Just release dont repeat message
                rc_command = false;
                event.rcSubAction = Event_FCB_RemoteControlSettings.RC_SUB_ACTION_RELEASED;

                releaseChannels();
            }
            break;
        }
       // 1- Apply first
        //releaseChannels();
        // 2- Announce after Apply action.
        AndruavSettings.andruavWe7daBase.setManualTXBlockedSubAction(event.rcSubAction);
    }


    public ControlBoard_DroneKit(AndruavUnitBase andruavUnitBase) {

        super(andruavUnitBase);
        Me = this;

        PitchPerUnit    = Angles.DEGREES_TO_RADIANS;
        RollPerUnit     = Angles.DEGREES_TO_RADIANS;
        YawPerUnit      = Angles.DEGREES_TO_RADIANS;
        HeadingPerUnit  = Angles.DEGREES_TO_RADIANS;
        HeadingOffset = 0;
        VarioPerUnit = 1;
        gps_alt_scale = 1000.0; // mhefny I updated clinet LIB relative_alt
        gps_lnglat_scale = 1.0;

        rcCamera = (Preference.getCameraNumber(null) + 1) % 2;

       // EventBus.getDefault().register(this, 1);
        ActivateListener(true);

        initHandler();

    }

    private static final int[] channelsRaw = new int[8];
    /***
     * Check if to activate Block mode or not.
     */
    public void checkBlockingMode ()
    {
        // BLocking Section
        if (!Preference.isRCBlockEnabled(null))
        {
            AndruavSettings.andruavWe7daBase.FCBoard.do_RCChannelBlocked(false);
            return;
        }

        final int channelNum = Preference.getChannelRCBlock(null);
        final int channelValue;

        channelValue = DroneMavlinkHandler.channelsRaw[channelNum-1];

        final boolean block = channelValue >= Preference.getChannelRCBlock_min_value(null);

        do_RCChannelBlocked(block);
    }


    /***
     * changes current camera foreground/background based on RC Channel status.
     * Only Mobile camera is affected by this command.
     */
    public void checkRCCamSwitch ()
    {
        if (!Preference.isRCCamEnabled(null))
        {
            return ;
        }

        final int channelNum = Preference.getChannelRCCam(null);
        final int channelValue;

        channelValue = DroneMavlinkHandler.channelsRaw[channelNum-1];

        final boolean button_on = channelValue >= Preference.getChannelRCCam_min_value(null);

        int rcCamera_temp = 0;
        if (button_on) {
            rcCamera_temp = 1;
        }

        if (rcCamera!=rcCamera_temp)
        {
            // switch camera if switch changed.
            rcCamera = rcCamera_temp;
            final Event_FPV_CMD a7adath_fpv_cmd = new Event_FPV_CMD(Event_FPV_CMD.FPV_CMD_SWITCHCAM);
            a7adath_fpv_cmd.Requester = AndruavSettings.andruavWe7daBase;
            AndruavEngine.getEventBus().post(a7adath_fpv_cmd);
        }

    }


    /***
     * Adjust RC Joystick mode ... mainly if guided then use Guided Joystick if joystick is active.
     * @param rcAction
     * @param vehicleMode vehicle mode.
     * @return
     */
    private int adjustRCActionByMode(final int rcAction, final int vehicleMode)
    {
        if (rcChannelBlock)
        {
            // FORCE RELEASE if blocked mode.
            return Event_FCB_RemoteControlSettings.RC_SUB_ACTION_RELEASED;
        }

        if ((rcAction== Event_FCB_RemoteControlSettings.RC_SUB_ACTION_JOYSTICK_CHANNELS) &&(vehicleMode ==FlightMode.CONST_FLIGHT_CONTROL_GUIDED))
        {
            return Event_FCB_RemoteControlSettings.RC_SUB_ACTION_JOYSTICK_CHANNELS_GUIDED;
        }

        if ((rcAction== Event_FCB_RemoteControlSettings.RC_SUB_ACTION_JOYSTICK_CHANNELS_GUIDED) &&(vehicleMode !=FlightMode.CONST_FLIGHT_CONTROL_GUIDED))
        {
            return Event_FCB_RemoteControlSettings.RC_SUB_ACTION_JOYSTICK_CHANNELS;
        }

        return rcAction;
    }

    @Override
    public void ActivateListener (boolean bActivate) {
        if (bActivate) {
            EventBus.getDefault().register(this,1);

            if (rcRepeater == null || rcRepeater.isShutdown()) {
                rcRepeater = Executors.newSingleThreadScheduledExecutor();
                rcRepeater.scheduleWithFixedDelay(ReapeterCommunicatorRunnable, 0, 300, TimeUnit.MILLISECONDS);
            }

        }
        else
        {
            EventBus.getDefault().unregister(this);

            if (rcRepeater != null ) {
                rcRepeater.shutdownNow();
                rcRepeater = null;
            }

        }
    }



    @Override
    public String getFCBDescription()
    {
        return "MAVLINK ver:"; // + String.valueOf(this.mavlink_version);
    }


    public void sendServoChannel (final int channel, final int value)
    {
        App.droneKitServer.ctrl_Servo(channel, value, new IControlBoard_Callback() {
            @Override
            public void OnSuccess() {

            }

            @Override
            public void OnFailue(int code) {
                PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(com.andruav.protocol.R.string.andruav_error_autopilot_cannot_do_servo));
            }

            @Override
            public void OnTimeout() {

            }
        });
    }

    /***
     * Sends Remote control values to FCB
     * @param subAction
     * @param channels
     * @param allEightChannels
     */
    @Override
    public  void sendRCChannels (final int subAction, final int[] channels, final boolean allEightChannels)
    {
        switch (subAction)
        {
           case Event_FCB_RemoteControlSettings.RC_SUB_ACTION_RELEASED:
            {

                releaseChannels();

            }
                break;
            case Event_FCB_RemoteControlSettings.RC_SUB_ACTION_CENTER_CHANNELS:
            case Event_FCB_RemoteControlSettings.RC_SUB_ACTION_FREEZE_CHANNELS:
            case Event_FCB_RemoteControlSettings.RC_SUB_ACTION_JOYSTICK_CHANNELS:
            {
                //https://mavlink.io/en/messages/common.html#RC_CHANNELS_OVERRIDE
                int[] rc_channels=new int[18];
                for (int i=0;i<8;++i)
                {
                    rc_channels[i] = 0; // A value of 0 means to release this channel back to the RC radio.
                }

                for (int i=8;i<18;++i)
                {
                    rc_channels[i] = Short.MAX_VALUE-1 ; // means to release this channel back to the RC radio.
                }

                final msg_rc_channels_override msg = new msg_rc_channels_override();

                if (mParameteredRefreshedCompleted)
                {

                    rc_channels[mRCMAP_ROLL-1]    = (short) channels[0];                // Aileron
                    rc_channels[mRCMAP_PITCH-1]    = (short) channels[1];               // Elevator
                    rc_channels[mRCMAP_THROTTLE-1] = (short) channels[2];                // Throttle
                    rc_channels[mRCMAP_YAW-1]      = (short) channels[3];                // Rudder

                }

                msg.chan1_raw = (short) rc_channels[0];
                msg.chan2_raw = (short) rc_channels[1];
                msg.chan3_raw = (short) rc_channels[2];
                msg.chan4_raw = (short) rc_channels[3];
                msg.chan5_raw = (short) rc_channels[4];
                msg.chan6_raw = (short) rc_channels[5];
                msg.chan7_raw = (short) rc_channels[6];
                msg.chan8_raw = (short) rc_channels[7];
                msg.chan9_raw = (short) rc_channels[8];
                msg.chan10_raw = (short) rc_channels[9];
                msg.chan11_raw = (short) rc_channels[10];
                msg.chan12_raw = (short) rc_channels[11];
                msg.chan13_raw = (short) rc_channels[12];
                msg.chan14_raw = (short) rc_channels[13];
                msg.chan15_raw = (short) rc_channels[14];
                msg.chan16_raw = (short) rc_channels[15];
                msg.chan17_raw = (short) rc_channels[16];
                msg.chan18_raw = (short) rc_channels[17];

                final MavlinkMessageWrapper mavlinkMessageWrapper = new MavlinkMessageWrapper(msg);
                if (App.droneKitServer == null) return ;
                msg.target_system = mSystemId; // simulate GCS
                msg.target_component = mComponentId;
                msg.sysid = 255; // simulate GCS
                msg.compid = 0;
                App.droneKitServer.sendSimulatedPacket (mavlinkMessageWrapper,false);
            }
            break;

            case Event_FCB_RemoteControlSettings.RC_SUB_ACTION_JOYSTICK_CHANNELS_GUIDED:
            {
                if (App.droneKitServer == null) break ;

                for (int i=0; i<4;++i)
                {   // an unsigned values for this mode is not permitted.
                    if (channels[i]<1000)
                    {
                        safeGuidedChannels[i] = 1500;
                    }
                    else
                    {
                        safeGuidedChannels[i] = channels[i];
                    }
                }
                App.droneKitServer.ctrl_guidedVelocityInLocalFrame(
                        (1500 - safeGuidedChannels[1]) / 100.0f,
                        (safeGuidedChannels[0] - 1500) / 100.0f,
                        (1500 - safeGuidedChannels[2]) / 100.0f,
                        (safeGuidedChannels[3] - 1500) /500.0f,
                        0,
                        (short) MAV_FRAME_BODY_OFFSET_NED,
                        (short) (MAVLINK_SET_POS_TYPE_MASK_POS_IGNORE | MAVLINK_SET_POS_TYPE_MASK_ACC_IGNORE | MAVLINK_SET_POS_TYPE_MASK_YAW_IGNORE),
                        null
                );
            }
            break;
        }
    }

    /***
     * This functions sends Zero to RC Channels.
     * Because not all RX modes uses RCChannels, as some uses guided velocity control, you need to release channels before switching between modes.
     */
    private void releaseChannels ()
    {

        final msg_rc_channels_override msg = new msg_rc_channels_override();

        // Channels 1-8 0 means release.
        // Channels 9-18 UINT16_MAX-1 means release.

        final short release = Short.MAX_VALUE -1;
        msg.chan1_raw=0;
        msg.chan2_raw=0;
        msg.chan3_raw=0;
        msg.chan4_raw=0;
        msg.chan5_raw=0;
        msg.chan6_raw=0;
        msg.chan7_raw=0;
        msg.chan8_raw=0;
        msg.chan9_raw = release;
        msg.chan10_raw = release;
        msg.chan11_raw = release;
        msg.chan12_raw = release;
        msg.chan13_raw = release;
        msg.chan14_raw = release;
        msg.chan15_raw = release;
        msg.chan16_raw = release;
        msg.chan17_raw = release;
        msg.chan18_raw = release;

        final MavlinkMessageWrapper mavlinkMessageWrapper = new MavlinkMessageWrapper(msg);
        if (App.droneKitServer == null) return ;
        App.droneKitServer.sendSimulatedPacket (mavlinkMessageWrapper,true);
    }


    private int log_dkit_mavlink_count = 3;

    /***
     * called to parse and processInterModuleMessages command.
     * <br>Maybe called in Drone from the board or from The incomming GCS Data.
     * <br>Maybe called in GCS
     * @param mavLinkPacket
     * @param sendPacket
     */
    public void Execute (final MAVLinkPacket mavLinkPacket, final boolean sendPacket) {

        try {

            if (sendPacket )
            {
                /**
                 * This is Telemetry via WS.
                 */
                App.sendTelemetryfromDrone(mavLinkPacket.encodePacket());
            }


            /**
               UDP Proxy Telemetry
             */
            final int status = App.getAndruavWSStatus();
            final int action = App.getAndruavWSAction();
            if (status == SOCKETSTATE_REGISTERED)
            {
                if (AndruavSettings.andruavWe7daBase.isUdpProxyAccessedLately())
                {
                    final byte[] msg = mavLinkPacket.encodePacket();
                    final int length = msg.length;
                    AndruavEngine.getUDPProxy().sendMessage(msg, length);
                }
                else
                if ((AndruavSettings.andruavWe7daBase.isUdpProxyEnabled()) && (mavLinkPacket.msgid == MAVLINK_MSG_ID_HEARTBEAT))
                {
                    final byte[] msg = mavLinkPacket.encodePacket();
                    final int length = msg.length;
                    AndruavEngine.getUDPProxy().sendMessage(msg, length);
                }
            }


        }
        catch (Exception ex)
        {
            if (log_dkit_mavlink_count > 0) {
                AndruavEngine.log().logException("dkit_mavlink", ex);
                log_dkit_mavlink_count -=1;
            }
        }
    }




    public void onDroneEvent_HeartBeat (final int sysid, final short type, final int base_mode, final int system_status, final int mavlink_version)
    {

        mSysId = sysid;
        mType = type;
        mAndruavUnitBase.setVehicleType(MavLink_Helpers.setCommonVehicleType (type));
        canFly = MavLink_Helpers.isCanFly (type);

        isArmed = ((base_mode & MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED) == MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED) || (App.droneKitServer.getAPM_VehicleType() == VehicleTypes.VEHICLE_ROVER);
        this.mAndruavUnitBase.IsArmed(isArmed);

        final int vehicle_type = AndruavSettings.andruavWe7daBase.getVehicleType();


        isFlying = (vehicle_type != VehicleTypes.VEHICLE_ROVER) &&
                ((system_status == MAV_STATE.MAV_STATE_ACTIVE)
                || (isFlying
                && (system_status == MAV_STATE.MAV_STATE_CRITICAL || system_status == MAV_STATE.MAV_STATE_EMERGENCY)));
        this.mAndruavUnitBase.IsFlying(isFlying);
    }



    public void onDroneEvent_StateConnected ()
    {


    }

    public void onDroneEvent_SpeedUpdated (Speed droneSpeed)
    {
        gps_groundspeed =  droneSpeed.getGroundSpeed();
        airspeed        =  droneSpeed.getAirSpeed();
        verticalspeed   =  droneSpeed.getVerticalSpeed();
    }

    public void onDroneEvent_AttitudeUpdated (final Attitude droneAttitude)
    {

        pitchspeed= droneAttitude.getPitchSpeed();      // Pitch angle (degree)
        rollspeed= droneAttitude.getRollSpeed();        // Roll angle (degree)
        yaw= droneAttitude.getYaw();                    // Yaw angle (degree)
        yawspeed= droneAttitude.getYawSpeed();

        PitchAngle = droneAttitude.getPitch();
        RollAngle = droneAttitude.getRoll();
        pitchspeed =droneAttitude.getPitchSpeed();
        rollspeed =droneAttitude.getRollSpeed();
        yawspeed = droneAttitude.getYawSpeed();



        mAndruavUnitBase.updateFromFCBAttitude();

        EventBus.getDefault().post(a7adath_imu_ready); // ToDo: this should be an internal trigger in Andruav Protocol Lib

        // used By Web in case of no GPS.
        EventBus.getDefault().post(a7adath_nav_info_ready); // ToDo: this should be an internal trigger in Andruav Protocol Lib

    }

    public void onDroneEvent_AltitudeUpdated (final com.o3dr.services.android.lib.drone.property.Altitude droneAltitude)
    {
        // IMPORTANT: THIS IS ABSOLUTE ALT NOT RELATIVE ALT
        Altitude  = droneAltitude.getAltitude();
        alt_error = droneAltitude.getTargetAltitude() - droneAltitude.getAltitude();
    }


    private double gps_lng_old, gps_lat_old;
    private  int counterBearing =5;

    public void onDroneEvent_GPS_NOGPS (final Gps droneGps)
    {
        gps_fixType         =  0;

        gps_satCount = 0;


        mAndruavUnitBase.updateFromFCBGPS();

        EventBus.getDefault().post(a7adath_gps_ready); // ToDo: this should be an internal trigger in Andruav Protocol Lib

        // PANIC HERE GPS ERROR

    }


    // This message is not called when GPS is not active such as in ROver Manual Mode.
    // so we use Attitude message to send this message.
    // Although Attitude message sends IMU data that should be specially requested by GCS and currently is not used by WEB.
    // As Web displays multiple drones which will consumes alot of traffic.
    public void onDroneEvent_GPS_Position (final Gps droneGps)
    {
        final LatLong lnglat =   droneGps.getPosition();

        if (lnglat==null) return ;

        vehicle_gps_lng = lnglat.getLongitude();
        vehicle_gps_lat = lnglat.getLatitude();
        vehicle_gps_alt = droneGps.getRelative_altitude();
        counterBearing = counterBearing + 1;
        double distanceFromOldPoint =0;
        if (gps_lng_old != 0)
        {
            distanceFromOldPoint = MathUtils.getDistance2D(new LatLong(gps_lat_old,gps_lng_old),lnglat);
            if (distanceFromOldPoint > 10) {
                nav_bearing = MathUtils.getHeadingFromCoordinates(new LatLong(gps_lat_old, gps_lng_old),
                        lnglat);
            }
            //nav_bearing = GPSHelper.calculateBearing(,gps_lat_old,gps_lng,gps_lat);
        }

        if ((guided_LngLat != null) && (guided_LngLat.getLatitude() != 0)) {
            // we have a valid guided point

            wp_dist_old = wp_dist;
            wp_dist = GPSHelper.calculateDistance(guided_LngLat.getLongitude(), guided_LngLat.getLatitude(), vehicle_gps_lng, vehicle_gps_lat);
            target_bearing = GPSHelper.calculateBearing(vehicle_gps_lng, vehicle_gps_lat, guided_LngLat.getLongitude(), guided_LngLat.getLatitude());

            this.mAndruavUnitBase.updateFCBNavInfo();
            EventBus.getDefault().post(a7adath_nav_info_ready); // ToDo: this should be an internal trigger in Andruav Protocol Lib

        }

        if ((distanceFromOldPoint > 10) || (gps_lng_old == 0)) {
            // update older point if distance > 10 meters
            if ((gps_lng_old != vehicle_gps_lng) && (gps_lat_old != vehicle_gps_lat)) {
                gps_lng_old = vehicle_gps_lng;
                gps_lat_old = vehicle_gps_lat;
            }
        }



            followMeOn = false;

        mAndruavUnitBase.updateFromFCBGPS();

        EventBus.getDefault().post(a7adath_gps_ready); // ToDo: this should be an internal trigger in Andruav Protocol Lib
    }

    public void onDroneEvent_GPS (final Gps droneGps)
    {


        gps_fixType         =  (short) droneGps.getFixType(); // 0-1: no fix, 2: 2D fix, 3: 3D fix, 4: DGPS, 5: RTK. Some applications will not use the value of this field unless it is at least two, so always correctly fill in the fix.
        if (gps_fixType <2)
        {
            vehicle_gps_alt =0;
        }
        else {

        }

        gps_satCount = droneGps.getSatellitesCount();

        mAndruavUnitBase.updateFromFCBGPS();

        EventBus.getDefault().post(a7adath_gps_ready); // ToDo: this should be an internal trigger in Andruav Protocol Lib

    }


    public void onDroneEvent_HomeUpdated (final com.o3dr.services.android.lib.drone.property.Home droneHome)
    {
        final LatLongAlt latLongAlt =  droneHome.getCoordinate();

        if ((mInternalCommand==INTERNAL_GET_HOME_MISSION) && (mInternalCommand_Step ==0 )){
            // we got the data
            mInternalCommand_Step = 1;

          //  mhandle.postDelayed(doCommands, 5000); // repeat in 10 second if failed.
        }

        if (latLongAlt==null) return ;


        home_gps_alt = latLongAlt.getAltitude();
        home_gps_lng = latLongAlt.getLongitude();
        home_gps_lat = latLongAlt.getLatitude();

        mAndruavUnitBase.updateFCBHomeLocation();

        EventBus.getDefault().post(a7adath_homeLocation_ready); // ToDo: this should be an internal trigger in Andruav Protocol Lib

    }


    public void onDroneEvent_Battery (final Battery droneBattery)
    {
        pow_battery_voltage  = droneBattery.getBatteryVoltage() * 1000.0 ;  //convert it to mV
        pow_battery_current = droneBattery.getBatteryCurrent() * 1000.0 ;  //convert it to mA
        setBatteryRemaining(Math.abs(droneBattery.getBatteryRemain())); // it is read in negative here

        mAndruavUnitBase.updateFromFCBPower();

        EventBus.getDefault().post(a7adath_battery_ready); // ToDo: this should be an internal trigger in Andruav Protocol Lib
    }


    public  void onDroneEvent_MissionSent (final Mission mission)
    {
        loadMission(mission);
    }

    public  void onDroneEvent_MissionReceived (final Mission mission)
    {
        if (mInternalCommand==INTERNAL_CMD_WAYPOINTS) {
            // we got the data
            mInternalCommand = INTERNAL_CMD_NON;
        }

        if (mInternalCommand==INTERNAL_GET_HOME_MISSION) {
            // we got the data
            mInternalCommand = INTERNAL_CMD_NON;
        }

        loadMission(mission);

    }


    protected void loadMission(final Mission mission)
    {
        this.mAndruavUnitBase.getMohemmaMapBase().clear();

        List<MissionItem> missionItems = mission.getMissionItems();
        LatLongAlt geo = null;
        MissionBase missionBase =null;
        int wp =0;
        for (MissionItem item: missionItems)
        {
            final MissionItemType missionItemType = item.getType();

            switch (missionItemType)
            {
                case WAYPOINT:
                    final Waypoint waypoint = ((Waypoint)item);
                    WayPointStep wayPointStep = new WayPointStep();

                    geo = waypoint.getCoordinate();
                    wayPointStep.Altitude       =   geo.getAltitude();
                    wayPointStep.Latitude       =   geo.getLatitude();
                    wayPointStep.Longitude      =   geo.getLongitude();
                    wayPointStep.Heading        =  (float) waypoint.getYawAngle();
                    wayPointStep.TimeToStay     =  waypoint.getDelay();
                    wayPointStep.Sequence       =  wp;

                    // OR DEVELOP SOMTHING BETWEEN THE BOARD & THE 3rdPARTY
                    missionBase = wayPointStep;
                    break;

                case SPLINE_WAYPOINT:
                    final SplineWaypoint splineWaypoint= ((SplineWaypoint)item);
                    geo = splineWaypoint.getCoordinate();

                    SplineMission splineMohemma = new SplineMission();
                    splineMohemma.Altitude      =   geo.getAltitude();
                    splineMohemma.Latitude      =   geo.getLatitude();
                    splineMohemma.Longitude     =   geo.getLongitude();
                    splineMohemma.TimeToStay    =   splineWaypoint.getDelay();
                    splineMohemma.Sequence      =  wp;

                    missionBase = splineMohemma;
                    break;

                case TAKEOFF:
                    final Takeoff takeoff = ((Takeoff)item);
                    missionBase = new MissionEkla3(takeoff.getTakeoffAltitude(),takeoff.getTakeoffPitch());
                    missionBase.Sequence = wp;
                    break;

                case LAND:
                    final Land land = ((Land)item);
                    missionBase = new MissionHoboot();
                    missionBase.Sequence = wp;
                    break;

                case CAMERA_TRIGGER:
                    final CameraTrigger cameraTrigger = ((CameraTrigger)item);

                    MissionCameraTrigger mohemmaCamera= new MissionCameraTrigger();
                    mohemmaCamera.Sequence       =  wp;

                    // OR DEVELOP SOMTHING BETWEEN THE BOARD & THE 3rdPARTY
                    missionBase = mohemmaCamera;

                    break;

                case CAMERA_CONTROL:
                    final CameraControl cameraControl = ((CameraControl)item);

                    MissionCameraControl mohemmaCameraControl= new MissionCameraControl();
                    mohemmaCameraControl.Sequence       =  wp;

                    // OR DEVELOP SOMTHING BETWEEN THE BOARD & THE 3rdPARTY
                    missionBase = mohemmaCameraControl;

                    break;

                case CIRCLE:
                    final Circle circle = ((Circle)item);

                    MissionDayra mohemmaDayra= new MissionDayra();

                    geo = circle.getCoordinate();
                    mohemmaDayra.Altitude       =   geo.getAltitude();
                    mohemmaDayra.Latitude       =   geo.getLatitude();
                    mohemmaDayra.Longitude      =   geo.getLongitude();
                    mohemmaDayra.Radius         =  (float) circle.getRadius();
                    mohemmaDayra.Turns          =  circle.getTurns();
                    mohemmaDayra.Sequence       =  wp;

                    // OR DEVELOP SOMTHING BETWEEN THE BOARD & THE 3rdPARTY
                    missionBase = mohemmaDayra;

                    break;

                case CHANGE_SPEED:
                case DO_JUMP:
                case DO_LAND_START:
                case EPM_GRIPPER:
                case REGION_OF_INTEREST:
                    break;

                case RESET_ROI:
                    final ResetROI resetROI = ((ResetROI)item);
                    missionBase = new MissionROI();
                    missionBase.Sequence = wp;
                    break;

                case RETURN_TO_LAUNCH:
                    missionBase = new MissionRTL();
                    missionBase.Sequence = wp;
                    break;

                case SET_RELAY:
                case SET_SERVO:
                case SPLINE_SURVEY:
                case STRUCTURE_SCANNER:
                case SURVEY:
                case YAW_CONDITION:


                default:
                    //final Waypoint waypoint = ((Waypoint)item);
                    missionBase = new MissionBase();
                    missionBase.Sequence = wp;
                    this.mAndruavUnitBase.getMohemmaMapBase().put(String.valueOf(wp), missionBase);
                    break;
            }

            if (missionBase != null) {
                this.mAndruavUnitBase.getMohemmaMapBase().put(String.valueOf(wp), missionBase);
                missionBase = null;

            }
            wp +=1;
        }

        AndruavFacade.sendWayPoints(null);
        // to update my own interface
        AndruavEngine.getEventBus().post(new Event_WayPointsRecieved(AndruavSettings.andruavWe7daBase));

    }



    public  void onDroneEvent_MissionUpdated ()
    {
    }


    /***
     * Recieves missionItemIndex
     * @param missionItemIndex
     */
    public  void onDroneEvent_MissionItemUpdated (final int  missionItemIndex)
    {

        if (missionItemIndex==-1)
        {
            return ;
        }

        if (missionItemIndex >= this.mAndruavUnitBase.getMohemmaMapBase().size())
        {
            // you may request mission download.
            App.droneKitServer.doReadMission();
            return ;
        }
        MissionBase missionBase = this.mAndruavUnitBase.getMohemmaMapBase().valueAt(missionItemIndex);
        missionBase.Status = MissionBase.Report_NAV_ItemExecuting;

        AndruavFacade.sendWayPointsReached(null, missionItemIndex, MissionBase.Report_NAV_ItemExecuting);
    }


    public void onDroneEvent_MissionItemReached (final int  missionItemIndex)
    {

        this.mAndruavUnitBase.missionItemReached(missionItemIndex);
    }


    public void onDroneEvent_VehicleMode (final VehicleMode vehicleMode)
    {

        mAndruavUnitBase.setFlightModeFromBoard (MavLink_Helpers.getAndruavStandardFlightMode(App.droneKitServer.getAPM_VehicleType(), (short) vehicleMode.getMode()));

        mAndruavUnitBase.setManualTXBlockedSubAction(adjustRCActionByMode (mAndruavUnitBase.getManualTXBlockedSubAction(), mAndruavUnitBase.getFlightModeFromBoard()));
    }


    public void onDroneEvent_StateArming (final State vehicleState)
    {
        isArmed = vehicleState.isArmed() || (App.droneKitServer.getAPM_VehicleType() == VehicleTypes.VEHICLE_ROVER);
        this.mAndruavUnitBase.IsArmed(isArmed);
    }


    public void onDroneEvent_GuidedUpdated (final GuidedState guidedState)
    {
        if (guidedState.isActive()==false)
        {
            guided_LngLat = null;
            target_gps_lat = -1.0;
            target_gps_lng = -1.0;
            //target_gps_alt = (vehicle_gps_alt / 1000.0);

        }
        else
        {
            guided_LngLat = guidedState.getCoordinate();
            target_gps_lat = guided_LngLat.getLatitude();
            target_gps_lng = guided_LngLat.getLongitude();
            //target_gps_alt = (vehicle_gps_alt / 1000.0); //guided_LngLat.getAltitude();
        }

        mAndruavUnitBase.updateFCBTargetLocation();
    }


    public void onDroneEvent_TypeUpdated (final Type vehicleType)
    {

        switch (vehicleType.getDroneType()) {
            case Type.TYPE_ROVER:
                mAndruavUnitBase.setVehicleType( VehicleTypes.VEHICLE_ROVER);
                canFly = false;
                break;

            case Type.TYPE_COPTER:
                mAndruavUnitBase.setVehicleType( VehicleTypes.VEHICLE_QUAD);
                canFly = true;
                break;

            case Type.TYPE_UNKNOWN:
                mAndruavUnitBase.setVehicleType( VehicleTypes.VEHICLE_UNKNOWN);
                canFly = true;
                break;

            case Type.TYPE_PLANE:
                mAndruavUnitBase.setVehicleType( VehicleTypes.VEHICLE_PLANE);
                canFly = true;
                break;

            case Type.TYPE_SUBMARINE:
                mAndruavUnitBase.setVehicleType( VehicleTypes.VEHICLE_SUBMARINE);
                canFly = true;
                break;

            default:
                mAndruavUnitBase.setVehicleType( VehicleTypes.VEHICLE_UNKNOWN);
                canFly = true;
                break;
        }
    }


    public void onDroneEvent_OnGimbalOrientationUpdate(final double pitch, double roll, double yaw)
    {
        mAndruavUnitBase.hasGimbal(true);
        final AndruavGimbal andruavGimbal = mAndruavUnitBase.getAndruavGimbal();
        andruavGimbal.setPitch(pitch);
        andruavGimbal.setRoll(roll);
        andruavGimbal.setYaw(yaw);
    }

    public void onDroneEvent_OnGimbalOrientationCommandError (final int error)
    {
        mAndruavUnitBase.hasGimbal(false);
    }



    public void execute_ServoOutputMessage(final msg_servo_output_raw msg_servo_output_raw)
    {

        final int sum = msg_servo_output_raw.servo9_raw + msg_servo_output_raw.servo10_raw + msg_servo_output_raw.servo11_raw + msg_servo_output_raw.servo12_raw + msg_servo_output_raw.servo13_raw + msg_servo_output_raw.servo14_raw + msg_servo_output_raw.servo15_raw + msg_servo_output_raw.servo16_raw;
        final int[] servoOutput = mAndruavUnitBase.getServoOutputs();
        int origin = 0;

        for (int i =0 ; i < 8; ++i)
        {
            origin +=servoOutput[i];
        }

        mAndruavUnitBase.setServoOutputs(0, msg_servo_output_raw.servo9_raw);
        mAndruavUnitBase.setServoOutputs(1, msg_servo_output_raw.servo10_raw);
        mAndruavUnitBase.setServoOutputs(2, msg_servo_output_raw.servo11_raw);
        mAndruavUnitBase.setServoOutputs(3, msg_servo_output_raw.servo12_raw);
        mAndruavUnitBase.setServoOutputs(4, msg_servo_output_raw.servo13_raw);
        mAndruavUnitBase.setServoOutputs(5, msg_servo_output_raw.servo14_raw);
        mAndruavUnitBase.setServoOutputs(6, msg_servo_output_raw.servo15_raw);
        mAndruavUnitBase.setServoOutputs(7, msg_servo_output_raw.servo16_raw);

        //BUG: here becasue two servos may change values and total sum = zero.
        a7adath_servo_output_ready.mValuesChanged = (Math.abs(origin - sum) > 500);

        EventBus.getDefault().post(a7adath_servo_output_ready);
    }


    public void  execute_msg_attitude(msg_attitude msg_attitude)
    {
        nav_pitch = msg_attitude.pitch * Angles.RADIANS_TO_DEGREES;
        nav_roll = msg_attitude.roll  * Angles.RADIANS_TO_DEGREES;

        this.mAndruavUnitBase.updateFCBNavInfo();
        EventBus.getDefault().post(a7adath_nav_info_ready); // ToDo: this should be an internal trigger in Andruav Protocol Lib
    }


    public void execute_NavController (final msg_nav_controller_output msg_nav_controller_output)
    {
        if (mAndruavUnitBase.getFlightModeFromBoard() == FlightMode.CONST_FLIGHT_CONTROL_AUTO) {
            target_bearing = msg_nav_controller_output.target_bearing * Angles.DEGREES_TO_RADIANS;
            wp_dist = msg_nav_controller_output.wp_dist;
        }
        alt_error = msg_nav_controller_output.alt_error;

        this.mAndruavUnitBase.updateFCBNavInfo();
        EventBus.getDefault().post(a7adath_nav_info_ready); // ToDo: this should be an internal trigger in Andruav Protocol Lib
    }


    /***
     * called after retrieving parameters from FCB. the array contains all parameters
     * @param parametersByName all parameters with values.
     */
    public void  execute_ParseParameters (final SimpleArrayMap<String,msg_param_value> parametersByName)
    {
        if ( parametersByName.get("RCMAP_ROLL") != null) {
            mRCMAP_ROLL = (int) parametersByName.get("RCMAP_ROLL").param_value;
        }

        if ( parametersByName.get("RCMAP_PITCH") != null) {
            mRCMAP_PITCH = (int) parametersByName.get("RCMAP_PITCH").param_value;
        }

        if ( parametersByName.get("RCMAP_THROTTLE") != null) {
            mRCMAP_THROTTLE = (int) parametersByName.get("RCMAP_THROTTLE").param_value;
        }

        if ( parametersByName.get("RCMAP_YAW") != null) {
            mRCMAP_YAW = (int) parametersByName.get("RCMAP_YAW").param_value;
        }

        if ( parametersByName.get("GPS_TYPE") != null) {
            mGPS1_Type = (int) parametersByName.get("GPS_TYPE").param_value;
            if (mGPS1_Type == GPS_TYPE_MAV)
            {
                mGPS_MAV_NUM = 0; // send to MAV1
            }
        }

        if ( parametersByName.get("GPS_TYPE2") != null) {
            mGPS2_Type = (int) parametersByName.get("GPS_TYPE2").param_value;
            if (mGPS_MAV_NUM == 0)
            {
                mGPS_MAV_NUM = 127;
            }
            else
            {
                mGPS_MAV_NUM = 2;
            }
        }

        if (parametersByName.get("MNT_TYPE").param_value != 0) {
                /*
                    msg_param_value.param_value
                    0	None
                    1	Servo
                    2	3DR Solo
                    3	Alexmos Serial
                    4	SToRM32 MAVLink
                    5	SToRM32 Serial
                */
                try {


                    mAndruavUnitBase.hasGimbal(true);

                    final AndruavGimbal andruavGimbal = mAndruavUnitBase.getAndruavGimbal();

               /*
                    0	Retracted
                    1	Neutral
                    2	MavLink Targeting    <<<<< U need this to sendMessageToModule MAVLINK
                    3	RC Targeting         <<<<< U need this to use Channels
                    4	GPS Point            <<<<< U need this to sendMessageToModule lng,lat,alt
                */
                    andruavGimbal.setMode(Math.round(parametersByName.get("MNT_DEFLT_MODE").param_value));

                    andruavGimbal.setStabilizePitch(parametersByName.get("MNT_STAB_TILT").param_value == 1.0f);
                    andruavGimbal.setStabilizeRoll(parametersByName.get("MNT_STAB_ROLL").param_value == 1.0f);
                    andruavGimbal.setStabilizeYaw(parametersByName.get("MNT_STAB_PAN").param_value == 1.0f);

                    andruavGimbal.setMinRollAngle(Math.round(parametersByName.get("MNT_ANGMIN_ROL").param_value));
                    andruavGimbal.setMaxRollAngle(Math.round(parametersByName.get("MNT_ANGMAX_ROL").param_value));
                    andruavGimbal.setMinPitchAngle(Math.round(parametersByName.get("MNT_ANGMIN_TIL").param_value));
                    andruavGimbal.setMaxPitchAngle(Math.round(parametersByName.get("MNT_ANGMAX_TIL").param_value));
                    andruavGimbal.setMinYawAngle(Math.round(parametersByName.get("MNT_ANGMIN_PAN").param_value));
                    andruavGimbal.setMaxYawAngle(Math.round(parametersByName.get("MNT_ANGMAX_PAN").param_value));

                }
                catch (Exception ex)
                {
                    AndruavEngine.log().logException("dkit_mavlink", ex);

                }
                }else {
                mAndruavUnitBase.hasGimbal(false);
         }

        mParameteredRefreshedCompleted = true;
    }


    @Override
    public  void do_SendMission(final MohemmaMapBase mohemmaMapBase){
       final Mission mission = new Mission();

        for (int m=0,s = mohemmaMapBase.size(); m<s; m = m+1)
        {
            MissionBase missionBase = mohemmaMapBase.valueAt(m);

            if (missionBase instanceof WayPointStep)
            {
                final  WayPointStep wayPointStep = (WayPointStep) missionBase;
                final  Waypoint waypoint = new Waypoint();
                final LatLongAlt geo = new LatLongAlt(wayPointStep.Latitude,wayPointStep.Longitude,wayPointStep.Altitude);

                waypoint.setCoordinate(geo);
                waypoint.setYawAngle(wayPointStep.Heading);
                waypoint.setDelay(wayPointStep.TimeToStay);
                mission.addMissionItem(waypoint);
            }

            else if (missionBase instanceof MissionEkla3)
            {
                final MissionEkla3 mohemmaEkla3 = (MissionEkla3) missionBase;

                final Takeoff takeoff = new Takeoff();
                takeoff.setTakeoffAltitude(mohemmaEkla3.getAltitude());
                takeoff.setTakeoffPitch(mohemmaEkla3.getPitch());
                mission.addMissionItem(takeoff);

            }


            else if (missionBase instanceof MissionRTL)
            {
                final MissionRTL mohemmaRTL = (MissionRTL) missionBase;

                final ReturnToLaunch returnToLaunch = new ReturnToLaunch();
                mission.addMissionItem(returnToLaunch);

            }

            else if (missionBase instanceof MissionHoboot)
            {
                final MissionHoboot mohemmaHoboot = (MissionHoboot) missionBase;

                final Land land = new Land();

                mission.addMissionItem(land);

            }
        }

        App.droneKitServer.doSaveMission(mission);
    }




    public void onDroneConnection ()
    {
        doInternalCommand(INTERNAL_GET_HOME_MISSION,0,1000);
    }

    @Override
    public void do_ClearMission()
    {
        App.droneKitServer.doClearMission();
    }

    @Override
    public void do_ClearHomeLocation()
    {
        do_SetHomeLocation(0,0,0);
    }

    int retries = 5;



    @Override
    public void do_SetNavigationSpeed (final double speed, final boolean isGroundSpeed, final double throttle, final boolean isRelative)
    {
        if (speed ==-1) return ;

        double targetSpeed =speed;
        if (isRelative)
        {
            if (speed > gps_groundspeed)
            {
                targetSpeed = speed - gps_groundspeed;
            }
            else
            {
                targetSpeed = gps_groundspeed - speed;
            }
        }

        targetNavigationSpeed = targetSpeed;
        App.droneKitServer.setSpeed(targetSpeed , null);
    }

    @Override
    public void do_SetHomeLocation(final double longitude, final double latitude, final double altitude)
    {
        LatLongAlt latLongAlt = new LatLongAlt(latitude,longitude,altitude);

        App.droneKitServer.setHome(latLongAlt , new AbstractCommandListener() {

            @Override
            public void onSuccess() {
                // read and broadcast new Home.
                if ( !App.droneKitServer.isConnected()) return;

                App.droneKitServer.doReadHome();
            }

            @Override
            public void onError(int executionError) {
                mhandle.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if ( !App.droneKitServer.isConnected()) return;

                        do_SetHomeLocation(longitude,latitude,altitude);

                        //PANIC PLEASE
                    }
                },2000);
            }

            @Override
            public void onTimeout() {
                mhandle.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if ( !App.droneKitServer.isConnected()) return;

                        do_SetHomeLocation(longitude,latitude,altitude);

                        //PANIC PLEASE
                    }
                },1000);
            }
        });
    }


    @Override
    public void doPutMissionintoFCB (final String missionText)
    {

        App.droneKitServer.doPutMission(missionText);
    }


    @Override
    public void do_SetCurrentMission (final int missionItemNumber)
    {
        /*if ((missionItemNumber < 0) || (missionItemNumber >MissionItemCount))
        {

        }*/

        App.droneKitServer.doSetCurrentMission(missionItemNumber);
    }



    @Override
    public void do_ReadMission()
    {
        doInternalCommand(INTERNAL_CMD_WAYPOINTS,0,0);
    }



    @Override
    public boolean isArmed ()
    {
        return isArmed;
    }

    @Override
    public void do_ARM(final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (isArmed()) return;

        if (rcChannelBlock) return ;

        App.droneKitServer.ctrl_arm(true, false, lo7Ta7akom_callback);
    }

    @Override
    public void do_ChangeAltitude (final double altitude,final IControlBoard_Callback lo7Ta7akom_callback)
    {

        if (rcChannelBlock) return ;


        if (vehicleState != null)
        {

            if (vehicleState.isFlying()) {
                App.droneKitServer.ctrl_climbTo(altitude);
                target_gps_alt = altitude;
                return ;
            }
        }

        App.droneKitServer.ctrl_changeAltitude(altitude, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                target_gps_alt = altitude;

                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();
            }
        });
    }

    @Override
    public void do_Land (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_Land(lo7Ta7akom_callback);
    }

    @Override
    public void do_POS_Hold (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_POS_Hold(lo7Ta7akom_callback);
    }

    @Override
    public void do_ALT_Hold (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_ALT_Hold(lo7Ta7akom_callback);
    }

    @Override
    public void do_Auto (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_Auto(lo7Ta7akom_callback);
    }


    @Override
    public void do_Yaw (final double targetAngle, final double turnRate, final boolean isClockwise, final boolean isRelative)
    {
        if (rcChannelBlock) return ;

        double turn=turnRate;
        if (isClockwise == false)
        {
            turn = turnRate * -1;
        }

        App.droneKitServer.ctrl_Yaw(targetAngle,turn,isRelative, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                return ;

            }

            @Override
            public void onError(int executionError) {
                PanicFacade.cannotDoAutopilotAction(AndruavEngine.getPreference().getContext().getString(com.andruav.protocol.R.string.andruav_error_autopilot_cannot_do_yaw));
            }

            @Override
            public void onTimeout() {

            }
        });
    }

    @Override
    public void do_FlytoHere (final double lng, final double lat, final double alt,final double xVel, final double yVel, final double zVel, final double yaw, final double yaw_rate)
    {
        if (rcChannelBlock) return ;

        if (mAndruavUnitBase.getFlightModeFromBoard() != FlightMode.CONST_FLIGHT_CONTROL_GUIDED)
        {
            PanicFacade.cannotDoAutopilotAction("Vehicle is NOT in GUIDED MODE.");

            return ;
        }

        App.droneKitServer.ctrl_gotoLngLatI(new LatLong(lat, lng), true, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                target_gps_lat = lat;
                target_gps_lng = lng;
                target_gps_alt = alt;

                mAndruavUnitBase.updateFCBTargetLocation();
            }

            @Override
            public void onError(int executionError) {
                PanicFacade.cannotDoAutopilotAction("Fly here command failed.");
            }

            @Override
            public void onTimeout() {

            }
        });

    }


    /**
     * called by leader vehicle to update required position and speed.
     * @param slave_drone_lng
     * @param slave_drone_lat
     * @param slave_drone_alt
     * @param leader_drone_lng
     * @param leader_drone_lat
     * @param leader_drone_alt
     * @param leader_linear_speed
     */
    @Override
    public void do_FollowMe (final double slave_drone_lng,final double slave_drone_lat, final double slave_drone_alt,
                             final double leader_drone_lng, final double leader_drone_lat, final double leader_drone_alt, final double leader_linear_speed)
    {

        return ;
    }

    /***
     * Force emergency disarming
     * @param emergencyDisarm ignored here
     */
    @Override
    public void do_Disarm (boolean emergencyDisarm,final IControlBoard_Callback ILo7Ta7Akom__callback)
    {
        if (rcChannelBlock) return ;

        if (!isArmed()) return;
        App.droneKitServer.ctrl_arm(false, emergencyDisarm, ILo7Ta7Akom__callback);

    }


    /**
     * Takeoff command. Vehicle type should allow this mode.
     * Vehicle should be armed.
     * @param lo7Ta7akom_callback
     */
    @Override
    public void do_TakeOff(final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        final int vehicle_type = AndruavSettings.andruavWe7daBase.getVehicleType();
        if (vehicle_type != VehicleTypes.VEHICLE_PLANE) return ;

        App.droneKitServer.do_TakeOff(lo7Ta7akom_callback);
    }


    @Override
    public void do_Guided (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_Guided(new IControlBoard_Callback() {
            @Override
            public void OnSuccess() {
                 target_gps_alt = (vehicle_gps_alt / 1000.0); // reset target altitude as current.

                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void OnFailue(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
            }

            @Override
            public void OnTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();
            }
        });
    }

    @Override
    public void do_Loiter (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_Loiter(lo7Ta7akom_callback);
    }


    @Override
    public void do_Surface (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_Surface(lo7Ta7akom_callback);
    }


    @Override
    public void do_RTL (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_RTL(false, lo7Ta7akom_callback);
    }

    @Override
    public void do_Smart_RTL (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_RTL(true, lo7Ta7akom_callback);
    }

    @Override
    public void do_Brake (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_Brake(lo7Ta7akom_callback);
    }

    @Override
    public void do_Hold (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_Brake(lo7Ta7akom_callback);
    }


    @Override
    public void do_Manual (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_Manual(lo7Ta7akom_callback);
    }

    @Override
    public void do_Acro (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_Acro(lo7Ta7akom_callback);
    }

    @Override
    public void do_FBWA (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_FBWA(lo7Ta7akom_callback);
    }

    @Override
    public void do_FBWB (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_FBWB(lo7Ta7akom_callback);
    }

    @Override
    public void do_Cruise (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_Cruise(lo7Ta7akom_callback);
    }


    @Override
    public void do_CircleHere (final double lng, final double lat, final double alt, final double radius, final int turns, final IControlBoard_Callback lo7Ta7akom_callback) {
        if (rcChannelBlock) return ;

        App.droneKitServer.do_CircleHere(lng,lat,alt,radius,turns,lo7Ta7akom_callback);

    }


    @Override
    public void do_TriggerCamera ()
    {
        App.droneKitServer.do_TriggerCamera();
    }






    /***
     *
     * @param stabilizePitch
     * @param stabilizeRoll
     * @param stabilizeYaw
     * @param GimbalMode e.g. {@link AndruavGimbal#MAV_MOUNT_MODE_GPS_POINT}
     */
    @Override
    public void do_GimbalConfig(final boolean stabilizePitch, final boolean stabilizeRoll, final boolean stabilizeYaw, int GimbalMode)
    {
        App.droneKitServer.do_GimbalConfig(stabilizePitch,stabilizeRoll,stabilizeYaw,GimbalMode);
    }

    /***
     *
     * @param pitch in degrees  or lat, depending on mount mode
     * @param roll  in degrees  or lon depending on mount mode
     * @param yaw   in degrees  or alt (in cm) depending on mount mode
     */
    @Override
    public void do_GimbalCtrl (final double pitch, final double roll, final double yaw, final boolean isAbsolute)
    {

        App.droneKitServer.do_GimbalCtrl(pitch,roll,yaw,isAbsolute, mAndruavUnitBase.getAndruavGimbal());
    }


    /***
     *
     * @param lng
     * @param lat
     * @param alt in meters
     */
    @Override
    public void do_GimbalCtrlByGPS (final double lng, final double lat, final double alt)
    {

        App.droneKitServer.do_GimbalCtrlByGPS(lng,lat,alt, mAndruavUnitBase.getAndruavGimbal());
    }



    protected void doInternalCommand (int internalCommand, int internalCommandStep)
    {
        doInternalCommand(internalCommand, internalCommandStep, 100);
    }

    protected void doInternalCommand (int internalCommand, int internalCommandStep, long delayMillis)
    {
        Me.mInternalCommand = internalCommand;
        Me.mInternalCommand_Step =internalCommandStep;
        mhandle.postDelayed(doCommands, delayMillis);
    }


    public void do_InjectGPS (final long timeStampe, final long timeWeekMS, final int timeWeek
            , final short fixType, final int lat, final int lng, final int alt
            , final int satellites_visible, final float hdop, final float vdop
            , final float speedAccuracy, final float horizontalAccuracy, final float verticalAccuracy, final int gpsNum)
    {
        App.droneKitServer.do_InjectGPS(timeStampe,timeWeekMS, timeWeek
                , fixType, lat,lng, alt, satellites_visible, hdop, vdop
                , speedAccuracy, horizontalAccuracy, verticalAccuracy, gpsNum);
    }

    public void do_InjectGPS_NMEA (final String nmea)
    {
        App.droneKitServer.do_InjectGPS_NMEA(nmea);
    }



    private final boolean lastSelectionNavByVelocity = false;
    private static final int NavCalledTimeOut = 100;
    private double targetNavigationSpeed = 2;
    private final double nav_velocity=0;
    private final double nav_velocity_p=0;
    private final double nav_velocity_i=0;
    private final double nav_velocity_d=0;




    private final Runnable doCommands = new Runnable() {
        @Override
        public void run() {
            boolean brepeat = true;

            if (App.droneKitServer == null) return ;

            switch (Me.mInternalCommand) {
                case INTERNAL_CMD_WAYPOINTS:
                    App.droneKitServer.doReadMission();
                    mhandle.postDelayed(doCommands, 15000); // repeat in 10 second if failed.
                    break;

                case INTERNAL_GET_HOME_MISSION:
                    switch (mInternalCommand_Step) {
                        case 0:
                            App.droneKitServer.doReadHome();
                            break;
                        case 1:
                            App.droneKitServer.doReadMission();
                            break;
                    }

                    mhandle.postDelayed(doCommands, 10000); // repeat in 10 second if failed.
                    break;
            }
        }
    };


    private void activate_Rc_sub_action_center_channels()
    {
        int[] channels = new int[8];

        for (int i = 0; i < 8; ++i) {
            channels[i] = 1500;
        }
        rc_command = true;
        rc_command_last = System.currentTimeMillis();
    }

    private void activate_Rc_sub_action_freeze_channels()
    {

        System.arraycopy(DroneMavlinkHandler.channelsRaw, 0, channelsshared, 0, 8);

        //App.droneKitServer.ctrl_enableManualControl(false, null);
        rc_command = true;
        rc_command_last = System.currentTimeMillis();

    }

    private void activate_Rc_sub_action_joystick_channels()
    {
        int[] channels = new int[8];

        System.arraycopy(DroneMavlinkHandler.channelsRaw, 0, channels, 0, 8);

        //App.droneKitServer.ctrl_enableManualControl(false, null);
        rc_command = true;
        rc_command_last = System.currentTimeMillis();
    }

    private void activate_Rc_sub_action_channel_guided()
    {
//        int[] channels = new int[8];
//
//        // Release remote control signals and use Velocity control commands.
//        for (int i = 0; i < 8; ++i) {
//            channels[i] = 1500;  // it is more accurate to use (MIN+MAX/2) + Trim  values for each channel.
//        }
        // you dont want repeater to keep sending rcChannels as you rely on GuidedVelocity now.
        rc_command = false;
        rc_command_last = System.currentTimeMillis();

        App.droneKitServer.ctrl_guidedVelocityInLocalFrame(0.0f,
                0.0f,
                0.0f,
                0.0f,
                0,             // ignored here
                (short) MAV_FRAME_BODY_OFFSET_NED,
                (short) (MAVLINK_SET_POS_TYPE_MASK_POS_IGNORE | MAVLINK_SET_POS_TYPE_MASK_ACC_IGNORE | MAVLINK_SET_POS_TYPE_MASK_YAW_IGNORE),
                null
        );
    }
}
