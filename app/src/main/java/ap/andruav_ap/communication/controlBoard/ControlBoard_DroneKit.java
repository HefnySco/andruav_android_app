package ap.andruav_ap.communication.controlBoard;

import org.greenrobot.eventbus.Subscribe;

import static com.MAVLink.enums.MAV_STATE.MAV_STATE_ACTIVE;
import static com.MAVLink.enums.MAV_STATE.MAV_STATE_CRITICAL;
import static com.MAVLink.enums.MAV_STATE.MAV_STATE_EMERGENCY;
import static com.MAVLink.minimal.msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT;
import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_REGISTERED;


import android.location.Location;
import android.os.Build;

import ap.andruavmiddlelibrary.sensors.Sensor_GPS;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;

import androidx.collection.SimpleArrayMap;

import com.MAVLink.common.msg_sys_status;
import com.MAVLink.enums.MAV_SYS_STATUS_SENSOR;
import com.andruav.controlBoard.shared.missions.MissionCameraTrigger;
import com.andruav.controlBoard.shared.missions.MissionCameraControl;
import com.andruav.event.Event_Remote_ChannelsCMD;
import com.andruav.event.fpv7adath.Event_FPV_CMD;
import com.andruav.sensors.AndruavIMU;
import com.MAVLink.MAVLinkPacket;
import com.MAVLink.common.msg_attitude;
import com.MAVLink.minimal.msg_heartbeat;
import com.MAVLink.common.msg_nav_controller_output;
import com.MAVLink.common.msg_param_value;
import com.MAVLink.common.msg_rc_channels_override;
import com.MAVLink.common.msg_servo_output_raw;
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
import com.MAVLink.enums.MAV_COMPONENT;
import com.MAVLink.enums.MAV_MODE_FLAG;
import com.MAVLink.enums.MAV_STATE;
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


import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.greenrobot.eventbus.EventBus;
import ap.andruav_ap.App;
import ap.andruav_ap.communication.controlBoard.mavlink.DroneMavlinkHandler;
import ap.andruav_ap.helpers.RemoteControl;
import ap.andruav_ap.communication.controlBoard.mavlink.MavLink_Helpers;
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
    final int[] safeGuidedChannels = new int[8];

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
     * mGPS_MAV_NUM 0:send to first GPS,1:send to 2nd GPS - the GPS_INPUT gps_id must match the
     * receiving instance index exactly, there is no "send to all" value for it.
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

    /**
     * GPS_INPUT send period. AP_GPS::is_healthy() needs the average gap between GPS messages under
     * 215 ms and fails after two consecutive gaps over 245 ms, but phone GNSS delivers about one
     * fix per second - so the latest fix is re-sent on this timer rather than once per
     * onLocationChanged(). 100 ms leaves a wide margin for executor and link jitter.
     */
    private static final long GPS_INJECT_PERIOD_MS = 100;

    /**
     * Injection stops once the newest GNSS fix is older than this, so a phone that lost its fix
     * shows up on the FC as a lost GPS rather than as a position frozen in place.
     */
    private static final long GPS_INJECT_MAX_FIX_AGE_MS = 2000;

    /**
     * ScheduledExecutorService used to periodically run {@link #GPSInjectorRunnable}.
     */
    private ScheduledExecutorService gpsInjector;

    private final Runnable GPSInjectorRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                injectLatestGnssFix();
            }
            catch (final Exception e)
            {
                // must not escape: a scheduled executor silently cancels all later runs after one
                e.printStackTrace();
            }
        }
    };

    private void injectLatestGnssFix ()
    {
        if (!isFCConfiguredForGPSInjection()) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        if (!Preference.isGPSInjecttionEnabled(null)) return;
        if (App.droneKitServer == null) return;

        // GPS_PROVIDER-only fix that Sensor_GPS keeps while injection is enabled - never the
        // network/Wi-Fi-mixed location the rest of the app uses, whose jumps and coarse accuracy
        // fail EKF3's GPS checks.
        final Location fix = Sensor_GPS.getLastGnssFix();
        if (fix == null) return;

        final long fixAgeMillis = (SystemClock.elapsedRealtimeNanos() - fix.getElapsedRealtimeNanos()) / 1000000L;
        if (fixAgeMillis > GPS_INJECT_MAX_FIX_AGE_MS) return;

        final AndruavIMU andruavIMU_Mobile = AndruavSettings.andruavWe7daBase.getMobileGPS();

        // GPS time = UTC + leap seconds, counted from the GPS epoch (1980-01-06T00:00:00Z),
        // split into whole weeks + milliseconds into the week. Each re-send of a fix is stamped
        // with the fix time plus its age, so time_usec/time_week/time_week_ms keep advancing with
        // the packets instead of repeating (ArduPilot runs them through its jitter correction).
        final long GPS_EPOCH_UNIX_MILLIS = 315964800000L;
        final long GPS_LEAPSECONDS_MILLIS = 18000L;
        final long AP_MSEC_PER_WEEK = 7L * 86400L * 1000L;

        final long sampleTimeMillis = fix.getTime() + Math.max(0L, fixAgeMillis);
        final long gpsTimeMillis = sampleTimeMillis - GPS_EPOCH_UNIX_MILLIS + GPS_LEAPSECONDS_MILLIS;
        final int time_week = (int) (gpsTimeMillis / AP_MSEC_PER_WEEK);
        final long time_week_ms = gpsTimeMillis % AP_MSEC_PER_WEEK;

        short fixStatus = (short)andruavIMU_Mobile.GPS3DFix;
        if (andruavIMU_Mobile.GPSFixQuality>3)
        {
            fixStatus = (short)andruavIMU_Mobile.GPSFixQuality;

            // fixStatus = 0-1: no fix, 2: 2D fix, 3: 3D fix. 4: 3D with DGPS. 5: 3D with RTK
            // SO for values less than 4 then use true 3DFix status... otherwise check 4 & 5 values in QUalityFix
        }

        // Float.NaN = the phone did not report this value; DroneKitServer.do_InjectGPS() flags it
        // ignored instead of sending a 0 the EKF would take as a perfect measurement.
        final float alt = fix.hasAltitude() ? (float) getAltitudeAboveSeaLevel(fix) : Float.NaN;
        final float hdop = (andruavIMU_Mobile.Hdop > 0.0f) ? andruavIMU_Mobile.Hdop : Float.NaN;
        final float vdop = (andruavIMU_Mobile.Vdop > 0.0f) ? andruavIMU_Mobile.Vdop : Float.NaN;
        final float horizontalAccuracy = fix.hasAccuracy() ? fix.getAccuracy() : Float.NaN;
        final float verticalAccuracy = fix.hasVerticalAccuracy() ? fix.getVerticalAccuracyMeters() : Float.NaN;
        final float speedAccuracy = fix.hasSpeedAccuracy() ? fix.getSpeedAccuracyMetersPerSecond() : Float.NaN;

        // North/east velocity from the provider's own speed and bearing. AP_GPS_MAV keeps the last
        // velocity it was given whenever a packet ignores it, so a stationary fix with no bearing
        // (common on phones) still sends an explicit zero - otherwise the velocity from the last
        // time the vehicle moved would stay latched on the FC.
        float vn = Float.NaN;
        float ve = Float.NaN;
        if (fix.hasSpeed()) {
            if (fix.hasBearing()) {
                final double bearingRad = Math.toRadians(fix.getBearing());
                vn = (float) (fix.getSpeed() * Math.cos(bearingRad));
                ve = (float) (fix.getSpeed() * Math.sin(bearingRad));
            } else if (fix.getSpeed() == 0.0f) {
                vn = 0.0f;
                ve = 0.0f;
            }
        }

        // 0 = "not available" per GPS_INPUT.yaw's own wire semantics (AP_GPS_MAV only
        // honors it when non-zero) - so leaving this at 0 when the heading preference
        // is off, or the phone has no magnetometer, changes nothing else about the fix.
        int yawCentideg = 0;
        if (Preference.isGPSHeadingInjectionEnabled(null) && Boolean.TRUE.equals(andruavIMU_Mobile.iM)) {
            yawCentideg = getYawCentidegrees(andruavIMU_Mobile.Y, fix);
        }

        this.do_InjectGPS(sampleTimeMillis * 1000,
                time_week_ms, time_week, fixStatus,
                (int) (fix.getLatitude() * 1.0e7), (int) (fix.getLongitude() * 1.0e7), alt,
                vn, ve,
                Sensor_GPS.SatUsedInFixCount, hdop, vdop,
                speedAccuracy, horizontalAccuracy, verticalAccuracy, mGPS_MAV_NUM,
                yawCentideg);
    }

    /***
     * ArduPilot reads GPS_INPUT.alt as height above mean sea level (AP_GPS_MAV stores it straight
     * into Location.alt), but Android's Location.getAltitude() reports height above the WGS84
     * ellipsoid. The two differ by the local geoid separation, which reaches tens of metres in
     * parts of the world. Prefer the platform's own MSL value where it exists (API 34+), otherwise
     * subtract the separation the GGA sentence carries - that term is 0 until a GGA has been
     * parsed, which just leaves the raw ellipsoidal height as before.
     */
    private static double getAltitudeAboveSeaLevel (final Location location)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && location.hasMslAltitude()) {
            return location.getMslAltitudeMeters();
        }

        return location.getAltitude() - Sensor_GPS.GeoidSeparation;
    }

    /***
     * Converts the phone's magnetic compass heading (radians, standard clockwise-from-north -
     * see {@link ap.andruavmiddlelibrary.sensors.CompassCalculation}) into GPS_INPUT.yaw's wire
     * format: centidegrees clockwise from *true* north, 0 reserved to mean "not available" and
     * 36000 used for true north itself (MAVLink common.xml's own documented convention for this
     * field - see AP_GPS_MAV::handle_msg()'s "have_yaw = packet.yaw != 0").
     *
     * CompassCalculation hardcodes its declination to 0, so azimuthCompass is magnetic, not true,
     * heading - left uncorrected that's a constant bias equal to the local magnetic declination
     * (tens of degrees in some regions). GeomagneticField supplies that correction from the same
     * WMM data Android's own compass UI uses, computed from the fix we're injecting anyway.
     */
    private static int getYawCentidegrees (final double magneticHeadingRadians, final Location location)
    {
        double trueHeadingDeg = Math.toDegrees(magneticHeadingRadians);
        try {
            final android.hardware.GeomagneticField field = new android.hardware.GeomagneticField(
                    (float) location.getLatitude(), (float) location.getLongitude(),
                    (float) location.getAltitude(), location.getTime());
            trueHeadingDeg += field.getDeclination();
        } catch (final Exception ex) {
            // Bad lat/lng/alt for the model - fall back to magnetic heading uncorrected rather
            // than sending no heading at all.
        }

        trueHeadingDeg = ((trueHeadingDeg % 360.0) + 360.0) % 360.0; // wrap into [0, 360)
        final int centideg = (int) Math.round(trueHeadingDeg * 100.0);
        return (centideg <= 0) ? 36000 : centideg; // 0 means "not available" on the wire, not north
    }

    @Subscribe(priority = 1)
    public void onEvent (final Event_RemoteServo event_remoteServo)
    {
        sendServoChannel (event_remoteServo.ChannelNumber, event_remoteServo.ChannelValue);
    }



    @Subscribe(priority = 1)
    public  void onEvent (final Event_Remote_ChannelsCMD a7adathRemote_channelsCMD)
    {

        if ((do_RCChannelBlocked()) || (!a7adathRemote_channelsCMD.partyID.equals(this.mAndruavUnitBase.PartyID))) return ;
        channelsshared = RemoteControl.calculateChannels3(a7adathRemote_channelsCMD.channels, true);
        rc_command = true;
        rc_command_last = System.currentTimeMillis();
        /////////////////TODO: Please update logic and remove this hack

        sendRCChannels(mAndruavUnitBase.getManualTXBlockedSubAction(), channelsshared,false);
    }


    @Subscribe(priority = 1)
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
            EventBus.getDefault().register(this);

            if (rcRepeater == null || rcRepeater.isShutdown()) {
                rcRepeater = Executors.newSingleThreadScheduledExecutor();
                rcRepeater.scheduleWithFixedDelay(ReapeterCommunicatorRunnable, 0, 300, TimeUnit.MILLISECONDS);
            }

            if (gpsInjector == null || gpsInjector.isShutdown()) {
                gpsInjector = Executors.newSingleThreadScheduledExecutor();
                // fixed rate, not fixed delay: AP_GPS judges health on the average message gap
                gpsInjector.scheduleAtFixedRate(GPSInjectorRunnable, 0, GPS_INJECT_PERIOD_MS, TimeUnit.MILLISECONDS);
            }

        }
        else
        {
            EventBus.getDefault().unregister(this);

            if (rcRepeater != null ) {
                rcRepeater.shutdownNow();
                rcRepeater = null;
            }

            if (gpsInjector != null) {
                gpsInjector.shutdownNow();
                gpsInjector = null;
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

            byte[] msg = null;

            if (sendPacket )
            {
                /**
                 * This is Telemetry via WS.
                 */
                msg = mavLinkPacket.encodePacket();
                App.sendTelemetryfromDrone(msg);
            }


            /**
               UDP Proxy Telemetry
             */
            final int status = AndruavEngine.getAndruavWSStatus();
            if (status == SOCKETSTATE_REGISTERED)
            {
                if (AndruavSettings.andruavWe7daBase.isUdpProxyAccessedLately())
                {
                    if (msg == null) msg = mavLinkPacket.encodePacket();
                    AndruavEngine.getUDPProxy().sendMessage(msg, msg.length);
                }
                else
                if ((AndruavSettings.andruavWe7daBase.isUdpProxyEnabled()) && (mavLinkPacket.msgid == MAVLINK_MSG_ID_HEARTBEAT))
                {
                    if (msg == null) msg = mavLinkPacket.encodePacket();
                    AndruavEngine.getUDPProxy().sendMessage(msg, msg.length);
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

        isArmed = ((base_mode & MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED) == MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED) || (mAndruavUnitBase.getVehicleType() == VehicleTypes.VEHICLE_ROVER);

        this.mAndruavUnitBase.IsArmed(isArmed);

        final int vehicle_type = AndruavSettings.andruavWe7daBase.getVehicleType();


        isFlying = (vehicle_type != VehicleTypes.VEHICLE_ROVER) && isArmed &&
                ((system_status == MAV_STATE_ACTIVE)
                || (isFlying
                && (system_status == MAV_STATE_CRITICAL || system_status == MAV_STATE_EMERGENCY)));
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

            mAndruavUnitBase.updateFCBNavInfo();
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

    public void onDroneEvent_GPS_Position2 (double lat, double lng, double alt_rel, double alt_abs)
    {
        vehicle_gps_lng = lng / 10E6;
        vehicle_gps_lat = lat / 10E6;
        vehicle_gps_alt = alt_rel;
        vehicle_gps_abs = alt_abs;

        LatLong lnglat = new LatLong(vehicle_gps_lat, vehicle_gps_lng);

        counterBearing = counterBearing + 1;
        double distanceFromOldPoint =0;
        if (gps_lng_old != 0)
        {
            distanceFromOldPoint = MathUtils.getDistance2D(new LatLong(gps_lat_old,gps_lng_old),lnglat);
            if (distanceFromOldPoint > 10) {
                nav_bearing = MathUtils.getHeadingFromCoordinates(new LatLong(gps_lat_old, gps_lng_old),
                        lnglat);
            }
        }

        if ((guided_LngLat != null) && (guided_LngLat.getLatitude() != 0)) {
            // we have a valid guided point

            wp_dist_old = wp_dist;
            wp_dist = GPSHelper.calculateDistance(guided_LngLat.getLongitude(), guided_LngLat.getLatitude(), vehicle_gps_lng, vehicle_gps_lat);
            target_bearing = GPSHelper.calculateBearing(vehicle_gps_lng, vehicle_gps_lat, guided_LngLat.getLongitude(), guided_LngLat.getLatitude());

            mAndruavUnitBase.updateFCBNavInfo();
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

        mAndruavUnitBase.setFlightModeFromBoard (MavLink_Helpers.getAndruavStandardFlightMode(mType, (short) vehicleMode.getMode()));

        mAndruavUnitBase.setManualTXBlockedSubAction(adjustRCActionByMode (mAndruavUnitBase.getManualTXBlockedSubAction(), mAndruavUnitBase.getFlightModeFromBoard()));
    }




    public void onDroneEvent_GuidedUpdated (final GuidedState guidedState)
    {
        if (!guidedState.isActive())
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
    { // Now we handle heart beat message directly.

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

        // ArduPilot 4.6 renamed GPS_TYPE/GPS_TYPE2 to GPS1_TYPE/GPS2_TYPE. Accept either name so
        // this keeps working against both pre-4.6 and 4.6+ firmware.
        if ( parametersByName.get("GPS1_TYPE") != null) {
            mGPS1_Type = (int) parametersByName.get("GPS1_TYPE").param_value;
        } else if ( parametersByName.get("GPS_TYPE") != null) {
            mGPS1_Type = (int) parametersByName.get("GPS_TYPE").param_value;
        }

        if ( parametersByName.get("GPS2_TYPE") != null) {
            mGPS2_Type = (int) parametersByName.get("GPS2_TYPE").param_value;
        } else if ( parametersByName.get("GPS_TYPE2") != null) {
            mGPS2_Type = (int) parametersByName.get("GPS_TYPE2").param_value;
        }

        // Which FC GPS instance to address, decided only by which slots are actually set to
        // GPS_TYPE 14 (MAV). GPS_TYPE2 defaults to 0 (None) on nearly every board, so keying off
        // "the parameter exists" instead of its value used to retarget a GPS1-only setup at
        // instance 127, and addressed a GPS2-only setup as instance 2 (which is a third receiver).
        //
        // gps_id must equal the receiving instance index exactly: AP_GPS_MAV::handle_msg() starts
        // with "if (state.instance != packet.gps_id) return;" and has no broadcast case. The
        // 127 = "send to all" convention belongs to ArduPilot's GPS_INJECT_TO parameter, which
        // routes RTCM through GPS_INJECT_DATA - it does not apply to GPS_INPUT, so a 127 here is
        // simply matched by no instance and dropped.
        final boolean gps1IsMav = (mGPS1_Type == GPS_TYPE_MAV);
        final boolean gps2IsMav = (mGPS2_Type == GPS_TYPE_MAV);
        if (gps1IsMav) {
            mGPS_MAV_NUM = 0;   // first GPS (also when both slots are MAV - one phone, one feed)
        } else if (gps2IsMav) {
            mGPS_MAV_NUM = 1;   // second GPS
        }

        if ( parametersByName.get("MNT_TYPE") != null) {
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
                }
        }
        else {
                mAndruavUnitBase.hasGimbal(false);
         }

        mParameteredRefreshedCompleted = true;
    }

    /***
     * Whether a full parameter refresh has completed for this connection, i.e. whether
     * {@link #getGPS1_Type()}/{@link #getGPS2_Type()} reflect the FC's real configuration rather
     * than the GPS_TYPE_NONE the fields start at. Lets a caller (Settings UI) distinguish "not
     * configured for MAV GPS" from "we don't know yet".
     */
    public boolean hasReceivedGPSTypeParams ()
    {
        return mParameteredRefreshedCompleted;
    }

    public int getGPS1_Type ()
    {
        return mGPS1_Type;
    }

    public int getGPS2_Type ()
    {
        return mGPS2_Type;
    }

    /***
     * Whether the FC is actually configured to accept GPS_INPUT injection - only meaningful once
     * {@link #hasReceivedGPSTypeParams()} is true. Mirrors the same check
     * {@link #injectLatestGnssFix()} uses to decide whether to call do_InjectGPS() at all, so
     * the UI can warn the user before they flip the preference expecting it to do something.
     */
    public boolean isFCConfiguredForGPSInjection ()
    {
        return (mGPS1_Type == GPS_TYPE_MAV) || (mGPS2_Type == GPS_TYPE_MAV);
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




        if (isFlying) {
                App.droneKitServer.ctrl_climbTo(altitude);
                target_gps_alt = altitude;
                return ;
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
//                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();
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
        if (!isClockwise)
        {
            turn = turnRate * -1;
        }

        App.droneKitServer.ctrl_Yaw(targetAngle,turn,isRelative, new AbstractCommandListener() {
            @Override
            public void onSuccess() {

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
            , final short fixType, final int lat, final int lng, final float alt
            , final float vn, final float ve
            , final int satellites_visible, final float hdop, final float vdop
            , final float speedAccuracy, final float horizontalAccuracy, final float verticalAccuracy, final int gpsNum
            , final int yawCentideg)
    {
        App.droneKitServer.do_InjectGPS(timeStampe,timeWeekMS, timeWeek
                , fixType, lat,lng, alt, vn, ve, satellites_visible, hdop, vdop
                , speedAccuracy, horizontalAccuracy, verticalAccuracy, gpsNum, yawCentideg);
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
