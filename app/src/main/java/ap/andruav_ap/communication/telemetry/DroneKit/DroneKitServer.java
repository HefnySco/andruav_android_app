package ap.andruav_ap.communication.telemetry.DroneKit;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.collection.SimpleArrayMap;

import com.andruav.AndruavFacade;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.Constants;
import com.andruav.EmergencyBase;
import com.andruav.FeatureSwitch;
import com.andruav.TelemetryProtocol;
import com.andruav.event.systemEvent.Event_ShutDown_Signalling;
import com.andruav.sensors.AndruavGimbal;
import com.andruav.interfaces.INotification;
import com.andruav.controlBoard.IControlBoard_Callback;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.controlBoard.shared.common.VehicleTypes;
import com.andruav.notification.PanicFacade;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;
import com.andruav.util.Maths;
import com.mavlink.MAVLinkPacket;
import com.mavlink.Parser;
import com.mavlink.ardupilotmega.msg_mount_configure;
import com.mavlink.ardupilotmega.msg_mount_control;
import com.mavlink.common.msg_command_ack;
import com.mavlink.common.msg_command_long;
import com.mavlink.common.msg_gps_inject_data;
import com.mavlink.common.msg_gps_input;
import com.mavlink.common.msg_mission_set_current;
import com.mavlink.common.msg_param_value;
import com.mavlink.enums.GPS_INPUT_IGNORE_FLAGS;
import com.mavlink.enums.MAV_CMD;
import com.mavlink.enums.MAV_MOUNT_MODE;
import com.mavlink.messages.MAVLinkMessage;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.ExperimentalApi;
import com.o3dr.android.client.apis.GimbalApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeEventExtra;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Circle;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Parameter;
import com.o3dr.services.android.lib.drone.property.Parameters;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;
import com.o3dr.services.android.lib.model.AbstractCommandListener;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.App;
import ap.andruav_ap.DeviceManagerFacade;
import ap.andruav_ap.R;
import ap.andruav_ap.communication.controlBoard.ControlBoard_DroneKit;
import ap.andruav_ap.communication.controlBoard.mavlink.DroneKitMavlinkObserver;
import ap.andruav_ap.communication.controlBoard.mavlink.MavLink_Helpers;
import ap.andruav_ap.communication.controlBoard.mavlink.MissionPlanner_Helper;
import ap.andruav_ap.communication.telemetry.IEvent_SocketData;
import ap.andruav_ap.communication.telemetry.SerialSocketServer.Event_SocketData;
import ap.andruav_ap.communication.telemetry.TelemetryModeer;
import ap.andruavmiddlelibrary.eventClasses.remoteControl.Event_ProtocolChanged;
import ap.andruavmiddlelibrary.eventClasses.remoteControl.Event_RemoteServo;
import ap.andruavmiddlelibrary.factory.communication.NetInfoAdapter;
import ap.andruavmiddlelibrary.preference.Preference;


/**
 * Created by mhefny on 1/18/16.
 */
public class DroneKitServer implements DroneListener, TowerListener , ControlApi.ManualControlStateListener, LinkListener, GimbalApi.GimbalOrientationListener, IEvent_SocketData {

    protected DroneKitServer Me;

    protected Context mContext;

    private final static int  HEARTBEAT_FIRST    = 1;
    private final static int  HEARTBEAT_RESTORED = 2;
    private final static int  HEARTBEAT_TIMEOUT  = 3;
    private final static int  HEARTBEAT_NEVER    = 0;

    /***  INTERNAL COMMANDS  ***/
    private final int INTERNAL_CMD_NON              = 0;                // no internal commands required
    private final int INTERNAL_CMD_CIRCLE           = 1;                // no internal commands required

    private int mInternalCommand= INTERNAL_CMD_NON;
    private int mInternalCommand_Step= 0;
    private Runnable mInternalCommand_Runnable;


    ///////////////////////////////////////////////////////////////



    private int heartBeatStatus = HEARTBEAT_NEVER;
    private  boolean disCOnnectOnPurpose = false;
    private Drone mDrone;
    private ControlTower mControlTower;
    private final Handler handler = new Handler();


    private boolean isInit = false;

    private final Parser parserDrone;

    private int selectedConnectionType=0;

    /***
     * Current Connection type with DroneAPI
     * @return {@link com.o3dr.services.android.lib.drone.connection.ConnectionType}
     */
    public int getSelectedConnectionType()
    {
        return selectedConnectionType;
    }
    //private Lo7Ta7akom_DroneKit lo7Ta7akom_droneKit;
    //private msg_param_value[] parameters = new msg_param_value[500];
    final private SimpleArrayMap<String,msg_param_value> parameters = new SimpleArrayMap<>();
    final private SimpleArrayMap<String,msg_param_value> parametersByName = new SimpleArrayMap<>();

    private final DroneKitMavlinkObserver mavObserver = new DroneKitMavlinkObserver(this);

    public int getSysID ()
    {
        return  mavObserver.sysid;
    }

    public int getCompID ()
    {
        return mavObserver.compid;
    }

    private int APM_VehicleType = VehicleTypes.VEHICLE_UNKNOWN;



    final MAVLinkPacket  msg_command_ack = new msg_command_ack().pack();
    //////////BUS EVENT


    public int getAPM_VehicleType()
    {
        return APM_VehicleType;
    }

    @Override
    public void onSocketData(final Event_SocketData event)
    {
        if (((mDrone != null) && (!mDrone.isConnected())) ||(event.IsLocal == Event_SocketData.SOURCE_LOCAL)) return ;

        final int j = event.DataLength;
        final byte[] data = event.Data;

        for (int i=0; i<j; i++)
        {
            final MAVLinkPacket tmpMavLinkPacket = parserDrone.mavlink_parse_char(data[i] & 0x00ff);
            if (tmpMavLinkPacket != null)
            {
                final MAVLinkMessage mavLinkMessage = tmpMavLinkPacket.unpack();
                final MavlinkMessageWrapper mavlinkMessageWrapper = new MavlinkMessageWrapper(mavLinkMessage);

                switch (tmpMavLinkPacket.msgid)
                {

                    default:
                        ExperimentalApi.getApi(mDrone).sendMavlinkMessage(mavlinkMessageWrapper);
                        break;
                }
            }
        }
    }

    /***
     * Injects GPS data from Mobile GPS. (EXPERIMENTAL)
     * @param timeStampe
     * @param timeWeekMS
     * @param timeWeek
     * @param fixType
     * @param lat
     * @param lng
     * @param alt
     * @param satellites_visible
     * @param hdop
     * @param vdop
     * @param speedAccuracy
     * @param horizontalAccuracy
     * @param verticalAccuracy
     * @param gpsNum: 0:send to first GPS,1:send to 2nd GPS,127:send to all
     */
    public void do_InjectGPS (final long timeStampe, final long timeWeekMS, final int timeWeek
                              , final short fixType, final int lat, final int lng, final int alt
                              , final int satellites_visible, final float hdop, final float vdop
                              , final float speedAccuracy, final float horizontalAccuracy, final float verticalAccuracy, final int gpsNum)
    {
        msg_gps_input msg = new msg_gps_input();

        msg.sysid       = (short) getSysID();
        msg.compid      = (short) getCompID ();

        msg.lat = lat;
        msg.lon = lng;
        msg.alt = alt;
        msg.fix_type = fixType;

        msg.gps_id = (short) gpsNum;
        msg.ignore_flags = 0xFF & ~(GPS_INPUT_IGNORE_FLAGS.GPS_INPUT_IGNORE_FLAG_ALT | GPS_INPUT_IGNORE_FLAGS.GPS_INPUT_IGNORE_FLAG_HDOP | GPS_INPUT_IGNORE_FLAGS.GPS_INPUT_IGNORE_FLAG_VDOP
                    | GPS_INPUT_IGNORE_FLAGS.GPS_INPUT_IGNORE_FLAG_SPEED_ACCURACY | GPS_INPUT_IGNORE_FLAGS.GPS_INPUT_IGNORE_FLAG_HORIZONTAL_ACCURACY | GPS_INPUT_IGNORE_FLAGS.GPS_INPUT_IGNORE_FLAG_HORIZONTAL_ACCURACY);
        msg.satellites_visible = (short) satellites_visible;
        msg.time_usec = timeStampe;
        msg.time_week = timeWeek;
        msg.time_week_ms = 0;
        msg.hdop = hdop;
        msg.vdop = vdop;
        msg.speed_accuracy = speedAccuracy;
        msg.vert_accuracy = verticalAccuracy;
        msg.horiz_accuracy = horizontalAccuracy;
        
        ExperimentalApi.getApi(mDrone).sendMavlinkMessage(new MavlinkMessageWrapper(msg));
    }

    /**
     * Injects NMEA GPS from module GPS (EXPERIMENTAL)
     * @param nmea
     */
    public void do_InjectGPS_NMEA (final String nmea)
    {

        msg_gps_inject_data msg =  new msg_gps_inject_data();

        msg.target_system       = (short) getSysID();
        msg.target_component    = (short) getCompID ();

        byte[] b= nmea.getBytes();
        for ( int i=0; i < b.length; i+=1 )
        {
            msg.data[i] = b[i]; // + (b[i+1] * 256));
        }
        msg.len = (short) (b.length);
        //m_msg_gps_inject_data = msg;
        ExperimentalApi.getApi(mDrone).sendMavlinkMessage(new MavlinkMessageWrapper(msg));
   }

    /***
     *
     * @param stabilizePitch
     * @param stabilizeRoll
     * @param stabilizeYaw
     * @param GimbalMode {@link MAV_MOUNT_MODE}
     */
    public void do_GimbalConfig(final boolean stabilizePitch, final boolean stabilizeRoll, final boolean stabilizeYaw, int GimbalMode)
    {
        msg_mount_configure msg = new msg_mount_configure();
        msg.target_system       = (short) getSysID();
        msg.target_component    = (short) getCompID ();
        msg.stab_pitch  = (stabilizePitch)?(short)1:(short)0;
        msg.stab_roll   = (stabilizeRoll)?(short)1:(short)0;
        msg.stab_yaw    = (stabilizeYaw)?(short)1:(short)0;
        msg.mount_mode  = (short)GimbalMode;
        ExperimentalApi.getApi(mDrone).sendMavlinkMessage(new MavlinkMessageWrapper(msg));
    }

    private boolean bFirst = true;
    /***
     * @param pitch in degrees  or lat, depending on mount mode
     * @param roll  in degrees  or lon depending on mount mode
     * @param yaw   in degrees  or alt (in cm) depending on mount mode
     */
    public void do_GimbalCtrl (double pitch, double roll, double yaw, final boolean isAbsolute, final AndruavGimbal andruavGimbal )
    {

        if (andruavGimbal == null) return ;

        if ((bFirst) || (andruavGimbal.getMode() != AndruavGimbal.MAV_MOUNT_MODE_MAVLINK_TARGETING)) {
            do_GimbalConfig(andruavGimbal.getStabilizePitch(), andruavGimbal.getStabilizeRoll(), andruavGimbal.getStabilizeYaw(), MAV_MOUNT_MODE.MAV_MOUNT_MODE_MAVLINK_TARGETING);
            andruavGimbal.setMode(MAV_MOUNT_MODE.MAV_MOUNT_MODE_MAVLINK_TARGETING);
            bFirst = false;
         }

        if (!isAbsolute)
        {
            pitch   = Maths.Constraint(andruavGimbal.getmMinPitchAngle(), (int) ((andruavGimbal.getPitch() + pitch)),andruavGimbal.getmMaxPitchAngle());
            roll    = Maths.Constraint(andruavGimbal.getmMinRollAngle(), (int) ((andruavGimbal.getRoll() + roll)),andruavGimbal.getmMaxRollAngle());
            yaw     = Maths.Constraint(andruavGimbal.getmMinYawAngle(), (int) ((andruavGimbal.getYaw() + yaw)),andruavGimbal.getmMaxYawAngle());
        }


        GimbalApi.getApi(mDrone).updateGimbalOrientation((float)pitch, (float)roll, (float)yaw, new GimbalApi.GimbalOrientationListener() {
            @Override
            public void onGimbalOrientationUpdate(GimbalApi.GimbalOrientation orientation) {

            }

            @Override
            public void onGimbalOrientationCommandError(int error) {
                PanicFacade.cannotDoAutopilotAction("Gimbal orientation error");
            }
        });
    }


    /***
     * @param lng
     * @param lat
     * @param alt in meters
     */
    public void do_GimbalCtrlByGPS (final double lng, final double lat, final double alt, final AndruavGimbal andruavGimbal )
    {

        if (andruavGimbal == null) return ;
        if (andruavGimbal.getMode() != AndruavGimbal.MAV_MOUNT_MODE_GPS_POINT) {
            do_GimbalConfig(andruavGimbal.getStabilizePitch(), andruavGimbal.getStabilizeRoll(), andruavGimbal.getStabilizeYaw(), MAV_MOUNT_MODE.MAV_MOUNT_MODE_GPS_POINT);
        }

        msg_mount_control msg = new msg_mount_control();
        msg.target_system       = (short) getSysID();
        msg.target_component    = (short) getCompID ();
        msg.input_a = (int) (lat);
        msg.input_b = (int) (lng);
        msg.input_c = (int) (alt * 100);  // in cm

        ExperimentalApi.getApi(mDrone).sendMavlinkMessage(new MavlinkMessageWrapper(msg));

    }

    /***
    * Data is received from Serial Socket This data could be from the server on the same andruav
    * or can be received from AndruavCommand {@link //Commands.BinaryMessages.AndruavResalaBinaryBase}
    *
    * Data from Remote GCS should be delivered directly to board, as in initialization Drone protocol
    * is set from the board reply only to ensure accuracy
    * @param event
    */
    public void onEvent (Event_SocketData event)
    {

    }


    public void onEvent (final Event_ShutDown_Signalling event)
    {

        if (event.CloseOrder != 1) return ;


        this.shutDown();
        App.droneKitServer = null;
    }



    ///////////////////

    public void shutDown()
    {
        disCOnnectOnPurpose = true;
        App.iEvent_socketData = null;
        EventBus.getDefault().unregister(this);
        if (mDrone != null)
        {
            mDrone.disconnect();
            mDrone.removeMavlinkObserver(mavObserver);
            mDrone.unregisterDroneListener(this);
            mDrone = null;
            AndruavSettings.andruavWe7daBase.setTelemetry_protocol(TelemetryProtocol.TelemetryProtocol_No_Telemetry);
            //TelemetryModeer.setConnected(TelemetryModeer.CURRENTCONNECTION_NON);
            //EventBus.getDefault().post(new Event_ProtocolChanged(true));
            //lo7Ta7akom_droneKit = null;
        }

        if (mControlTower!= null)
        {
            mControlTower.disconnect();
            mControlTower.unregisterDrone(mDrone);
            mControlTower = null;
        }

        isInit = false;

    }

    public static  boolean isValidAndroidVersion ()
    {
        return true;
    }

    protected void start ()
    {

    }


    public Drone getDrone()
    {
        return mDrone;

    }


    public void init ()
    {
        if (isInit) return ;

        disCOnnectOnPurpose = false;

        mControlTower = new ControlTower(App.context);
        mDrone = new Drone(App.context);

        mControlTower.connect(this);

        EventBus.getDefault().register(this);

        App.iEvent_socketData = this;

        isInit = true;
    }

    public DroneKitServer (Context context)
    {
        Me = this;
        mContext = context;
        parserDrone = new Parser();
    }

    protected ControlTower getControlTower()
    {
        return mControlTower;
    }

    protected void connectToDrone ()
    {
        try {


            if (this.mDrone == null) {
                return;
            }


            mDrone.disconnect();

            ConnectionParameter connectionParams = null;
            switch (Preference.getFCBTargetComm(null)) {
                case Preference.FCB_COM_USB:
                    if (!DeviceManagerFacade.hasUSBHost()) return; // should not happen
                    selectedConnectionType = ConnectionType.TYPE_USB;
                    connectionParams = ConnectionParameter.newUsbConnection(Constants.baudRateItemsValue[Preference.getFCBUSBBaudRateSelector(null)], null);
                    break;
                case Preference.FCB_COM_BT:
                    final boolean res = App.BT.Bluetooth.GetAdapter();
                    App.BT.Bluetooth.Enable();
                    selectedConnectionType = ConnectionType.TYPE_BLUETOOTH;
                    connectionParams = ConnectionParameter.newBluetoothConnection(Preference.getFCBBlueToothMAC(null), null);
                    break;
                case Preference.FCB_COM_TCP:
                    selectedConnectionType = ConnectionType.TYPE_TCP;
                    connectionParams = ConnectionParameter.newTcpConnection(Preference.getFCBDroneTCPServerIP(null), Integer.parseInt(Preference.getFCBDroneTCPServerPort(null)), null);
                    break;
                case Preference.FCB_COM_UDP:
                    selectedConnectionType = ConnectionType.TYPE_UDP;
                    connectionParams = ConnectionParameter.newUdpConnection(Integer.parseInt(Preference.getFCBDroneUDPServerPort(null)),null);
                    break;

            }

            boolean isDroneConnected = mDrone.isConnected();

            if (!isDroneConnected) {
                mDrone.connect(connectionParams, this);
            }
        }

        catch (Exception ex)
        {
            AndruavEngine.log().logException("dkit", ex);
        }
    }


    public boolean isConnected()
    {
        return !((mDrone == null) || (!mDrone.isConnected()));
    }

     /**
     * in some cases this function is not called. and result in two instance of lo7Ta7akom_droneKit
     * one in AndruavSettings.andruavWe7daBase.FCBoard and the other is the local variable.
     * so I removed the local one.
     * @param extras
     */
    protected  void onDroneEvent_StateConnected (final Bundle extras)
    {
        AndruavSettings.andruavWe7daBase.setTelemetry_protocol(TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry);
        //lo7Ta7akom_droneKit = (Lo7Ta7akom_DroneKit) AndruavSettings.andruavWe7daBase.FCBoard;

        if (AndruavSettings.andruavWe7daBase.FCBoard == null) return ;

        TelemetryModeer.setConnected(TelemetryModeer.CURRENTCONNECTION_3DR);
        EventBus.getDefault().post(new Event_ProtocolChanged(true));



        if (NetInfoAdapter.isOnSoloNetwork()) {
            NetInfoAdapter.Dual3GAccess(true);
            AndruavEngine.notification().Speak("A Solo has been detected");

        }

        ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_StateConnected();

        Type vehicleType = mDrone.getAttribute(AttributeType.TYPE);
        APM_VehicleType = vehicleType.getDroneType();
        ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_TypeUpdated (vehicleType);



        com.o3dr.android.client.apis.VehicleApi.getApi(mDrone).refreshParameters();

        AndruavSettings.andruavWe7daBase.useFCBIMU(true);
        bFirst = true;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (AndruavSettings.andruavWe7daBase.FCBoard==null) return ;
                ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneConnection();
            }
        },1500);

        AndruavFacade.broadcastID();

        GimbalApi.getApi(mDrone).startGimbalControl(this);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mDrone.isConnected())
                {
                    VehicleApi.getApi(mDrone).refreshParameters();
                }
            }
        },2000);

        return ;
    }


    protected  void onDroneEvent_StateDisconnected (final Bundle extras)
    {

        AndruavSettings.andruavWe7daBase.setTelemetry_protocol(TelemetryProtocol.TelemetryProtocol_No_Telemetry);

        PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_3DR, "Drone Disconnected", null);


        TelemetryModeer.setConnected(TelemetryModeer.CURRENTCONNECTION_NON);
        EventBus.getDefault().post(new Event_ProtocolChanged(disCOnnectOnPurpose));
        AndruavFacade.broadcastID();

        return ;
    }


    protected  void onDroneEvent_StateUpdated (final Bundle extras)
    {
        if (AndruavSettings.andruavWe7daBase.FCBoard ==null) return ;

        final State vehicleState = mDrone.getAttribute(AttributeType.STATE);
        if (vehicleState.isFlying()) {
            // Land
            //setMode(VehicleMode.COPTER_LAND);
        } else if (vehicleState.isArmed()) {
            // Take off
            //ctrl_takeOff(10); // Default take off maxAltitude is 10m
        } else if (!vehicleState.isConnected()) {
            // Connect
            // alertUser("Connect to a drone first");
        } else if (vehicleState.isConnected() && !vehicleState.isArmed()){
            // Connected but not Armed
            // this.drone.arm(true);
        }

        ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_StateUpdated(vehicleState);
        return ;
    }


    protected  void onDroneEvent_TypeUpdated (final Bundle extras)
    {
        if (AndruavSettings.andruavWe7daBase.FCBoard ==null) return ;

        Type vehicleType = mDrone.getAttribute(AttributeType.TYPE);
        ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_TypeUpdated(vehicleType);

    }


    protected  void onDroneEvent_StateArming (final Bundle extras)
    {
        final  State vehicleState = mDrone.getAttribute(AttributeType.STATE);
        if (AndruavSettings.andruavWe7daBase.FCBoard !=null)   ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_StateArming(vehicleState);
    }

    protected void onDroneEvent_GuidedUpdated (final Bundle extras)
    {
        final GuidedState guidedState = mDrone.getAttribute(AttributeType.GUIDED_STATE);
        if (AndruavSettings.andruavWe7daBase.FCBoard !=null) ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_GuidedUpdated(guidedState);
    }


    protected  void onDroneEvent_VehicleMode (final Bundle extras)
    {
        final  State vehicleState = mDrone.getAttribute(AttributeType.STATE);
        final VehicleMode vehicleMode = vehicleState.getVehicleMode();

        if (AndruavSettings.andruavWe7daBase.FCBoard !=null)   ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_VehicleMode(vehicleMode);
    }

    protected  void onDroneEvent_SpeedUpdated (final Bundle extras)
    {
        final Speed droneSpeed = mDrone.getAttribute(AttributeType.SPEED);

        if (AndruavSettings.andruavWe7daBase.FCBoard !=null)   ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_SpeedUpdated(droneSpeed);
    }


    protected  void onDroneEvent_AttitudeUpdated (final Bundle extras)
    {
        final Attitude droneAttitude = mDrone.getAttribute(AttributeType.ATTITUDE);

        if (AndruavSettings.andruavWe7daBase.FCBoard !=null)   ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_AttitudeUpdated(droneAttitude);
    }

    protected  void onDroneEvent_AltitudeUpdated (final Bundle extras)
    {
        final Altitude droneAltitude = mDrone.getAttribute(AttributeType.ALTITUDE);

        if (AndruavSettings.andruavWe7daBase.FCBoard !=null)   ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_AltitudeUpdated(droneAltitude);
    }

    protected  void onDroneEvent_HomeUpdated (final Bundle extras)
    {
        final Home droneHome = mDrone.getAttribute(AttributeType.HOME);

        if (AndruavSettings.andruavWe7daBase.FCBoard !=null)   ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_HomeUpdated(droneHome);
    }

    protected  void onDroneEvent_GPS (final Bundle extras, final String event)
    {
        if (AndruavSettings.andruavWe7daBase.FCBoard ==null) return ;

        final Gps droneGps = mDrone.getAttribute(AttributeType.GPS);
        switch (event) {
            case AttributeEvent.GPS_POSITION:
                ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_GPS_Position(droneGps);
                break;
            case AttributeEvent.WARNING_NO_GPS:
                ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_GPS_NOGPS(droneGps);
                break;
            default:
                ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_GPS(droneGps);
        }

    }

    protected  void onDroneEvent_Battery (final Bundle extras)
    {
        if (AndruavSettings.andruavWe7daBase.FCBoard ==null) return ;

        final Battery droneBattery = mDrone.getAttribute(AttributeType.BATTERY);
        ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_Battery(droneBattery);
    }


    protected  void onDroneEvent_MissionUpdated (final Bundle extras, final String event)
    {
        if (AndruavSettings.andruavWe7daBase.FCBoard ==null) return ;
        ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_MissionUpdated();
        ExecuteInternalCommand();
    }

    protected  void onDroneEvent_MissionSent (final Bundle extras, final String event)
    {
        if (AndruavSettings.andruavWe7daBase.FCBoard ==null) return ;



        Mission droneMission = mDrone.getAttribute(AttributeType.MISSION);
        ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_MissionSent(droneMission);
    }

    protected  void onDroneEvent_MissionReceived (final Bundle extras, final String event)
    {
        if (AndruavSettings.andruavWe7daBase.FCBoard ==null) return ;

        Mission droneMission = mDrone.getAttribute(AttributeType.MISSION);
        if (AndruavSettings.andruavWe7daBase.FCBoard !=null) ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_MissionReceived(droneMission);
    }

    protected  void onDroneEvent_MissionItemUpdated ( Bundle extras,  String event) {
        if (AndruavSettings.andruavWe7daBase.FCBoard == null) return;

        int currentWaypoint = extras.getInt(AttributeEventExtra.EXTRA_MISSION_CURRENT_WAYPOINT, 0);
        currentWaypoint = currentWaypoint -1 ; // zero based
        if (AndruavSettings.andruavWe7daBase.FCBoard !=null) ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_MissionItemUpdated(currentWaypoint);

    }


    protected  void onDroneEvent_MissionItemReached (final Bundle extras, final String event)
    {
        if (AndruavSettings.andruavWe7daBase.FCBoard==null) return ;

        int currentWaypoint = extras.getInt(AttributeEventExtra.EXTRA_MISSION_LAST_REACHED_WAYPOINT, 0);
        currentWaypoint = currentWaypoint -1 ; // zero based

        if (AndruavSettings.andruavWe7daBase.FCBoard !=null) ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_MissionItemReached(currentWaypoint);
    }


    /***
     * For example disarm error could be due missing GPS.
     * It can be handled here.
     * @param msg_command_ack
     */
    public void HandleAckMessage (final msg_command_ack msg_command_ack)
    {

    }


    @Override
    public void onDroneEvent(final String event, final Bundle extras) {

        switch (event) {

            case AttributeEvent.PARAMETERS_REFRESH_STARTED:
                //     msg_command_ack msg_command_ack = new msg_command_ack();
                //    App.sendTelemetryfromDrone(msg_command_ack.pack().encodePacket());
                break;
            case AttributeEvent.PARAMETER_RECEIVED:
                msg_param_value msg_param_value = new msg_param_value();
                msg_param_value.param_index = extras.getInt(AttributeEventExtra.EXTRA_PARAMETER_INDEX);
                msg_param_value.param_count = extras.getInt(AttributeEventExtra.EXTRA_PARAMETERS_COUNT);
                msg_param_value.setParam_Id(extras.getString(AttributeEventExtra.EXTRA_PARAMETER_NAME));
                msg_param_value.param_value = (float) extras.getDouble(AttributeEventExtra.EXTRA_PARAMETER_VALUE);
                parameters.put(String.valueOf(msg_param_value.param_index), msg_param_value);
                parametersByName.put(msg_param_value.getParam_Id(), msg_param_value);

                break;
            case AttributeEvent.PARAMETERS_REFRESH_COMPLETED:
                if (AndruavSettings.andruavWe7daBase.FCBoard !=null)   ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).execute_ParseParameters (parametersByName);
                break;

            case AttributeEvent.HEARTBEAT_FIRST:
                heartBeatStatus = HEARTBEAT_FIRST;
                break;
            case AttributeEvent.HEARTBEAT_RESTORED:
            {
                heartBeatStatus = HEARTBEAT_RESTORED;
                onDroneEvent_StateArming(null);

                PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_NORMAL, AndruavMessage_Error.ERROR_Lo7etTa7akom, App.getAppContext().getString(R.string.andruav_error_dronekitconnection_res), null);

                final EmergencyBase em = AndruavEngine.getEmergency();
                if (em != null) {
                    em.resetEmergency();
                    em.resetTimers();
                }

                AndruavSettings.andruavWe7daBase.setTelemetry_protocol(TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry);
                TelemetryModeer.setConnected(TelemetryModeer.CURRENTCONNECTION_3DR);
                EventBus.getDefault().post(new Event_ProtocolChanged(false));
            }
            break;
            case AttributeEvent.HEARTBEAT_TIMEOUT: {
                if (heartBeatStatus != HEARTBEAT_TIMEOUT) {
                    heartBeatStatus = HEARTBEAT_TIMEOUT;

                    PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_Lo7etTa7akom, App.getAppContext().getString(R.string.andruav_error_dronekitconnection), null);
                    AndruavSettings.andruavWe7daBase.setTelemetry_protocol(TelemetryProtocol.TelemetryProtocol_No_Telemetry);
                    TelemetryModeer.setConnected(TelemetryModeer.CURRENTCONNECTION_NON);
                    EventBus.getDefault().post(new Event_ProtocolChanged(false));
                }


//                final EmergencyBase em = AndruavMo7arek.getEmergency();
//                if (em != null) {
//                    em.triggerConnectionEmergency(true);
//                    em.triggerConnectionEmergency(true);
//                }
            }
            break;

            case AttributeEvent.FOLLOW_START:
            case AttributeEvent.FOLLOW_STOP:
            case AttributeEvent.FOLLOW_UPDATE:
                break;

            case AttributeEvent.AUTOPILOT_MESSAGE:
            case AttributeEvent.AUTOPILOT_ERROR:
                break;

            case AttributeEvent.GUIDED_POINT_UPDATED:
                onDroneEvent_GuidedUpdated(extras);
                break;


            case AttributeEvent.STATE_CONNECTED:
                onDroneEvent_StateConnected(extras);
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                onDroneEvent_StateDisconnected (extras);
                break;

            case AttributeEvent.STATE_UPDATED:
                onDroneEvent_StateUpdated(extras);
                break;

            case AttributeEvent.STATE_ARMING:
                onDroneEvent_StateArming (extras);
                break;

            case AttributeEvent.TYPE_UPDATED:
                onDroneEvent_TypeUpdated (extras);
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                onDroneEvent_VehicleMode(extras);
                break;

            case AttributeEvent.SPEED_UPDATED:
                onDroneEvent_SpeedUpdated(extras);
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                onDroneEvent_AltitudeUpdated(extras);
                break;

            case AttributeEvent.ATTITUDE_UPDATED:
                onDroneEvent_AttitudeUpdated(extras);
                break;

            case AttributeEvent.HOME_UPDATED:
                onDroneEvent_HomeUpdated(extras);
                break;

            case AttributeEvent.GPS_COUNT:
            case AttributeEvent.GPS_FIX:
            case AttributeEvent.GPS_POSITION:
            case AttributeEvent.WARNING_NO_GPS:
                onDroneEvent_GPS(extras,event);
                break;

            case AttributeEvent.BATTERY_UPDATED:
                onDroneEvent_Battery(extras);
                break;


            case AttributeEvent.MISSION_ITEM_REACHED:
                onDroneEvent_MissionItemReached(extras,event);
                break;
            case AttributeEvent.MISSION_ITEM_UPDATED:   // called when switched way points
                onDroneEvent_MissionItemUpdated(extras,event);
                break;
            case AttributeEvent.MISSION_RECEIVED:
                onDroneEvent_MissionReceived(extras,event);
                break;
            case AttributeEvent.MISSION_SENT:
                onDroneEvent_MissionSent(extras,event);
                sendSetCurrentWaypoint((short)1);  // skip Home
                break;
            case AttributeEvent.MISSION_UPDATED: //called when updating waypoints [upload mission]
                onDroneEvent_MissionUpdated(extras,event);
                break;

            default:
      //        Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }

    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {


        Toast.makeText(App.getAppContext(),
                errorMsg, Toast.LENGTH_LONG).show();

        mControlTower.unregisterDrone(mDrone);
    }

    @Override
    public void onManualControlToggled(boolean isEnabled) {
//        ControlApi.getApi(mDrone).manualControl(0.2f, 0, 0, new AbstractCommandListener() {
//            @Override
//            public void onSuccess() {
//                return;
//            }
//
//            @Override
//            public void onError(int executionError) {
//                return;
//
//            }
//
//            @Override
//            public void onTimeout() {
//                return;
//
//            }
//        });
    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        final String msg = "Link Connection Status:" + connectionStatus.getStatusCode();

        Toast.makeText(App.getAppContext(),
                msg, Toast.LENGTH_SHORT).show();

        switch(connectionStatus.getStatusCode()) {
            case LinkConnectionStatus.FAILED:
                if (mDrone == null)
                {
                    return ;
                }

                if (mDrone.isConnected())
                {
                    mDrone.disconnect();
                }

                mControlTower.unregisterDrone(mDrone);
                mControlTower.disconnect();

                isInit = false;
                break;
            case LinkConnectionStatus.CONNECTED:
                break;
            case LinkConnectionStatus.CONNECTING:
                break;
            case LinkConnectionStatus.DISCONNECTED:
                break;

        }



    }


    @Override
    public void onTowerConnected() {
        mControlTower.registerDrone(mDrone, this.handler);
        mDrone.registerDroneListener(this);
        this.mDrone.addMavlinkObserver(mavObserver);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                connectToDrone();
            }
        }, 100);
    }

    @Override
    public void onTowerDisconnected() {
        if (disCOnnectOnPurpose)
        {
            PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_3DR, "3DR Service Connection closed.", null);
            Toast.makeText(App.getAppContext(),
                    "3DR Service Connection closed.", Toast.LENGTH_LONG).show();
        }
        else
        {
            PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_3DR, "Failed to connect to 3DR Service.", null);
        }


    }


    public void sendMavlink (final MavlinkMessageWrapper mavlinkMessageWrapper)
    {
        ExperimentalApi.getApi(mDrone).sendMavlinkMessage(mavlinkMessageWrapper);
    }

    public void sendSimulatedPacket (final MavlinkMessageWrapper mavlinkMessageWrapper, boolean byPassBlocked)
    {
        if (byPassBlocked || (!AndruavSettings.andruavWe7daBase.FCBoard.do_RCChannelBlocked())) {
            ExperimentalApi.getApi(mDrone).sendMavlinkMessage(mavlinkMessageWrapper);
        }
    }

    public void setMode (VehicleMode newMode)
    {
        VehicleApi.getApi(mDrone).setVehicleMode(newMode);
    }



    public void ctrl_gotoLngLatI (LatLong point, boolean force, AbstractCommandListener listener)
    {
        ControlApi.getApi(mDrone).goTo(point, force, listener);

    }


    public void ctrl_guidedVelocityInLocalFrame(double vx, double vy, double vz, double yawRate, double yaw, short  coordinateFrame, short typeMask, AbstractCommandListener listener)
    {
        ControlApi.getApi(mDrone).guidedVelocityInLocalFrame(vx, vy, vz, yawRate, yaw, coordinateFrame, typeMask, listener);
    }

    public void ctrl_guidedVelocityInGlobalFrame(double vx, double vy, double vz, double yawRate, double yaw, short  coordinateFrame, short typeMask, AbstractCommandListener listener)
    {
        ControlApi.getApi(mDrone).guidedVelocityInGlobalFrame(vx, vy, vz, yawRate, yaw, coordinateFrame, typeMask, listener);
    }


    public void ctrl_enableManualControl (final boolean enable, final ControlApi.ManualControlStateListener listener)
    {
        ControlApi.getApi(mDrone).enableManualControl(enable, listener);
    }


    public void ctrl_climbTo (double altitude)
    {
        ControlApi.getApi(mDrone).climbTo(altitude);
    }

    public void ctrl_changeAltitude(double altitude, AbstractCommandListener listener)
    {
        ControlApi.getApi(mDrone).takeoff(altitude, listener);
    }

    /***
     * @param targetAngle targetAngle Target angle in degrees [0-360], with 0 == north.
     * @param turnRate Turning rate normalized to the range [-1.0f, 1.0f]. Positive values for clockwise turns, and negative values for counter-clockwise turns.
     * @param isRelative True is the target angle is relative to the current vehicle attitude, false otherwise if it's absolute.
     * @param abstractCommandListener
     */
    public void ctrl_Yaw(final double targetAngle, final double turnRate, final boolean isRelative, AbstractCommandListener abstractCommandListener)
    {

        if (targetAngle!=0)
        {
            ControlApi.getApi(mDrone).turnTo((float) targetAngle, (float) turnRate, isRelative, abstractCommandListener);
        }
        else
        {
            reset_Yaw_reset(abstractCommandListener);
        }
    }

    public void reset_Yaw_reset (AbstractCommandListener abstractCommandListener)
    {
        ControlApi.getApi(mDrone).reset_roi(abstractCommandListener);
    }

    /**
     *
     * @param servoNumber servo number
     * @param pwm servo value
     * @param iControlBoard_callback
     */
    public void ctrl_Servo(final int servoNumber, int pwm, final IControlBoard_Callback iControlBoard_callback)
    {

        String parameterName = null;

        if (pwm == Event_RemoteServo.CONST_MINIMUM)
        { // if pwm == 0 which means save the min value of servo ... no exactly ZERO

            parameterName = "SERVO"+servoNumber+"_MIN";

        }
        else
        if (pwm == Event_RemoteServo.CONST_MAXIMUM)
        { // if pwm == 9999 which means save the max value of servo ... no exactly ZERO

            parameterName = "SERVO"+servoNumber+"_MAX";
        }


        if (parameterName != null)
        { // no read the min or max value you need.
            msg_param_value msg_param_value = parametersByName.get(parameterName);
            if (msg_param_value != null) {
                pwm = (int) msg_param_value.param_value;
            }
        }

        // set servo to pwm given or updated by previous conditions.
        ExperimentalApi.getApi(mDrone).setServo(servoNumber, pwm, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (iControlBoard_callback != null) iControlBoard_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (iControlBoard_callback != null) iControlBoard_callback.OnFailue(executionError);
            }

            @Override
            public void onTimeout() {
                if (iControlBoard_callback != null) iControlBoard_callback.OnTimeout();
            }
        });
    }


    public void ctrl_arm(final boolean arm, final boolean forceDisarm, final IControlBoard_Callback lo7Ta7akom_callback )
    {
        if (mDrone==null)
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);

            return ;
        }

        VehicleApi.getApi(mDrone).arm(arm,forceDisarm,

                new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
                    }

                    @Override
                    public void onError(int executionError) {
                        if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                        final String error = MavLink_Helpers.getACKError(executionError);
                        PanicFacade.cannotDoAutopilotAction(error);
                    }

                    @Override
                    public void onTimeout() {
                        if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();
                        PanicFacade.cannotDoAutopilotAction("Time Out. Please try Arm Mode again");
                    }
                });
    }

    public void doClearMission()
    {
        if (!isConnected())
        {
            return ;
        }

        Mission mission = new Mission();
        mission.clear();
        MissionApi.getApi(mDrone).setMission(mission,true);
    }

    public void doReadMission()
    {
        if (mDrone==null) return ;

        if (!isConnected())
        {
            return ;
        }


        MissionApi.getApi(mDrone).loadWaypoints();
    }


    public void doPutMission (final String missionText)
    {
        if (mDrone==null) return ;

        if (!isConnected())
        {
            return ;
        }

        final Mission mission = MissionPlanner_Helper.parseMissionWayPointsText (missionText);

        if (mission == null) return ;

        doSaveMission(mission);


    }


    /***
     * Define mission start mission item.
     * <b>Please note that mission item could be either a waypoint or an action.</b>
     * @param i
     */
    public void sendSetCurrentWaypoint(short i) {

        msg_mission_set_current msg = new msg_mission_set_current();
        msg.target_system = (short) getSysID();
        msg.target_component = (short) getCompID();
        msg.seq = i;
        ExperimentalApi.getApi(mDrone).sendMavlinkMessage(new MavlinkMessageWrapper(msg));
    }

    public void doSaveMission(Mission mission)
    {
        if (mDrone==null) return ;

        if (!isConnected())
        {
            return ;
        }

        MissionApi.getApi(mDrone).setMission(mission,true);
    }

    public void doSetCurrentMission (final int missionItemNumber)
    {
        if (mDrone==null) return ;

        if (!isConnected())
        {
            return ;
        }

        sendSetCurrentWaypoint((short)missionItemNumber);  // skip Home
    }

    public void doReadHome()
    {
        if (mDrone==null) return ;

        if (!isConnected())
        {
            return ;
        }

        final Home droneHome =   mDrone.getAttribute(AttributeType.HOME);
        if (AndruavSettings.andruavWe7daBase.FCBoard !=null)   ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_HomeUpdated(droneHome);

        return ;

    }



    public void changeMissionSpeed(final float speed) {

        final Type vehicleType = mDrone.getAttribute(AttributeType.TYPE);
        final int apm_vehicleType= vehicleType.getDroneType();

        switch (apm_vehicleType)
        {
            case Type.TYPE_ROVER: {
                final  msg_param_value msg_param_value = parametersByName.get("CRUISE_SPEED");
                msg_param_value.param_value = speed;
                final Parameters parameters = new Parameters();
                Parameter parameter = new Parameter("CRUISE_SPEED", speed, 1);

                parameters.addParameter(parameter);
                VehicleApi.getApi(mDrone).writeParameters(parameters);
            }
                break;

            case Type.TYPE_COPTER: {
                final Parameters parameters = new Parameters();
                Parameter parameter = new Parameter("WPNAV_SPEED", speed * 100 , 1);

                parameters.addParameter(parameter);
                VehicleApi.getApi(mDrone).writeParameters(parameters);
            }
                break;

        }


        final msg_command_long msg = new msg_command_long();
        msg.target_system = (short) getSysID();
        msg.target_component = (short) getCompID();
        msg.command = MAV_CMD.MAV_CMD_DO_CHANGE_SPEED;
        msg.param1 = 1;     // Speed type (0=Airspeed, 1=Ground Speed)
        msg.param2 = speed; // Speed (m/s, -1 indicates no change)
        msg.param3 = -1;    // Throttle ( Percent, -1 indicates no change)
        msg.param4 = 0;     // absolute or relative [0,1]

        ExperimentalApi.getApi(mDrone).sendMavlinkMessage(new MavlinkMessageWrapper(msg));

    }

    public void setSpeed (double speed,final AbstractCommandListener abstractCommandListener)
    {

        if ((mDrone==null) || (!isConnected()))
        {
            if (abstractCommandListener!= null) abstractCommandListener.onError(-1);
            return ;
        }


        changeMissionSpeed ((float)speed);

        /*// this sets auto mode speed.
        MissionApi.getApi(mDrone).setMissionSpeed((float)speed, abstractCommandListener);

        // this sets guided mode speed.
        speed = speed * 100; // cm/sec
        final Parameters parameters = new Parameters();
        parameters.addParameter(new Parameter("WPNAV_SPEED", speed,9));

        VehicleApi.getApi(mDrone).writeParameters(parameters);*/
    }

    public void setHome(final LatLongAlt latLongAlt,final AbstractCommandListener abstractCommandListener)
    {
        if ((mDrone==null) || (!isConnected()))
        {
            if (abstractCommandListener!= null) abstractCommandListener.onError(-1);
            return ;
        }

        VehicleApi.getApi(mDrone).setVehicleHome(latLongAlt, abstractCommandListener);
    }



    public void do_Land (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }
        VehicleApi.getApi(mDrone).setVehicleMode(VehicleMode.COPTER_LAND, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();
            }
        });
    }



    private void startMission(final IControlBoard_Callback lo7Ta7akom_callback)
    {
        MissionApi.getApi(mDrone).startMission(true, true, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();
            }
        });
    }


    public void do_Auto (final IControlBoard_Callback lo7Ta7akom_callback)
    {

        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }

        //startMission (lo7Ta7akom_callback);



            VehicleApi.getApi(mDrone).setVehicleMode(MavLink_Helpers.get3DRFlightControl(APM_VehicleType, FlightMode.CONST_FLIGHT_CONTROL_AUTO), new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                //startMission(lo7Ta7akom_callback);
                //Internally StartMission is called by dronekit
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();
                PanicFacade.cannotDoAutopilotAction("Time Out. Please try Auto Mode again");

            }
        });

    }

    public void do_POS_Hold (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }

        VehicleApi.getApi(mDrone).setVehicleMode(MavLink_Helpers.get3DRFlightControl(APM_VehicleType, FlightMode.CONST_FLIGHT_CONTROL_POSTION_HOLD), new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();
                PanicFacade.cannotDoAutopilotAction("Time Out. Please try Position Hold Mode again");
            }
        });
    }

    public void do_ALT_Hold (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }

        VehicleApi.getApi(mDrone).setVehicleMode(MavLink_Helpers.get3DRFlightControl(APM_VehicleType, FlightMode.CONST_FLIGHT_CONTROL_ALT_HOLD), new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();
                PanicFacade.cannotDoAutopilotAction("Time Out. Please try Altitude Hold Mode again");
            }
        });
    }


    public void do_Loiter (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }


        VehicleApi.getApi(mDrone).setVehicleMode(MavLink_Helpers.get3DRFlightControl(APM_VehicleType, FlightMode.CONST_FLIGHT_CONTROL_LOITER), new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();
                PanicFacade.cannotDoAutopilotAction("Time Out. Please try Loiter Mode again");
            }
        });
    }


    public void do_Surface (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }


        VehicleApi.getApi(mDrone).setVehicleMode(MavLink_Helpers.get3DRFlightControl(APM_VehicleType, FlightMode.CONST_FLIGHT_CONTROL_SURFACE), new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();
                PanicFacade.cannotDoAutopilotAction("Time Out. Please try Loiter Mode again");
            }
        });
    }


    public void do_TakeOff (final IControlBoard_Callback lo7Ta7akom_callback)
    {

        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }

        VehicleApi.getApi(mDrone).setVehicleMode(MavLink_Helpers.get3DRFlightControl(APM_VehicleType, FlightMode.CONST_FLIGHT_CONTROL_TAKEOFF), new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();

                PanicFacade.cannotDoAutopilotAction("Time Out. Please try fly take off mode again");
            }
        });
    }


    public void do_Guided (final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }

        VehicleApi.getApi(mDrone).setVehicleMode(MavLink_Helpers.get3DRFlightControl(APM_VehicleType, FlightMode.CONST_FLIGHT_CONTROL_GUIDED), new AbstractCommandListener() {
            @Override
            public void onSuccess() {


                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();
                PanicFacade.cannotDoAutopilotAction("Time Out. Please try Guided Mode again");
            }
        });
    }



    public void do_FBWA (final IControlBoard_Callback lo7Ta7akom_callback)
    {

        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }

        VehicleApi.getApi(mDrone).setVehicleMode(MavLink_Helpers.get3DRFlightControl(APM_VehicleType, FlightMode.CONST_FLIGHT_CONTROL_FBWA), new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();

                PanicFacade.cannotDoAutopilotAction("Time Out. Please try fly by wire Mode again");
            }
        });
    }


    public void do_FBWB (final IControlBoard_Callback lo7Ta7akom_callback)
    {

        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }

        VehicleApi.getApi(mDrone).setVehicleMode(MavLink_Helpers.get3DRFlightControl(APM_VehicleType, FlightMode.CONST_FLIGHT_CONTROL_FBWB), new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();

                PanicFacade.cannotDoAutopilotAction("Time Out. Please try fly by wire Mode again");
            }
        });
    }

    public void do_Cruise (final IControlBoard_Callback lo7Ta7akom_callback)
    {

        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }

        VehicleApi.getApi(mDrone).setVehicleMode(MavLink_Helpers.get3DRFlightControl(APM_VehicleType, FlightMode.CONST_FLIGHT_CONTROL_CRUISE), new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();

                PanicFacade.cannotDoAutopilotAction("Time Out. Please try cruise Mode again");
            }
        });
    }

    public void do_Manual (final IControlBoard_Callback lo7Ta7akom_callback)
    {

        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }

        VehicleApi.getApi(mDrone).setVehicleMode(MavLink_Helpers.get3DRFlightControl(APM_VehicleType, FlightMode.CONST_FLIGHT_CONTROL_MANUAL), new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();

                PanicFacade.cannotDoAutopilotAction("Time Out. Please try RTL Mode again");
            }
        });
    }


    public void do_Acro (final IControlBoard_Callback lo7Ta7akom_callback)
    {

        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }

        VehicleApi.getApi(mDrone).setVehicleMode(MavLink_Helpers.get3DRFlightControl(APM_VehicleType, FlightMode.CONST_FLIGHT_CONTROL_ACRO), new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();

                PanicFacade.cannotDoAutopilotAction("Time Out. Please try RTL Mode again");
            }
        });
    }

    /**
     *
     * @param isSmartRTL smart RTL for Hover & Drones.
     * @param lo7Ta7akom_callback
     */
    public void do_RTL (final boolean isSmartRTL, final IControlBoard_Callback lo7Ta7akom_callback)
    {

        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }
        int mode = FlightMode.CONST_FLIGHT_CONTROL_RTL;
        if (isSmartRTL==true)
        {
            mode = FlightMode.CONST_FLIGHT_CONTROL_SMART_RTL;
        }

        VehicleApi.getApi(mDrone).setVehicleMode(MavLink_Helpers.get3DRFlightControl(APM_VehicleType, mode), new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();

                PanicFacade.cannotDoAutopilotAction("Time Out. Please try RTL Mode again");
            }
        });

    }


    /**
     * HOLD, Break is the same ... review Mapping Logic in APMModes
     * @param lo7Ta7akom_callback
     */
    public void do_Brake(final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if ((mDrone==null) || (!isConnected()))
        {
            if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(-1);
            return ;
        }

        VehicleApi.getApi(mDrone).setVehicleMode(MavLink_Helpers.get3DRFlightControl(APM_VehicleType, FlightMode.CONST_FLIGHT_CONTROL_BRAKE), new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnSuccess();
            }

            @Override
            public void onError(int executionError) {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnFailue(executionError);
                final String error = MavLink_Helpers.getACKError(executionError);
                PanicFacade.cannotDoAutopilotAction(error);
            }

            @Override
            public void onTimeout() {
                if (lo7Ta7akom_callback!= null) lo7Ta7akom_callback.OnTimeout();

                PanicFacade.cannotDoAutopilotAction("Time Out. Please try RTL Mode again");
            }
        });
    }

    public void do_CircleHere (final double lng, final double lat, final double altitude, final double radius, final int turns, final IControlBoard_Callback lo7Ta7akom_callback)
    {
        if (mInternalCommand== INTERNAL_CMD_CIRCLE)
        {
            return;
        }
       do_Guided(new IControlBoard_Callback() {
            @Override
            public void OnSuccess() {
                final Mission mission = new Mission();
                // delete old to Reset
                clearInternalCommand();
                mission.clear();
                MissionApi.getApi(mDrone).setMission(mission, true);
                mInternalCommand = INTERNAL_CMD_CIRCLE;
                mInternalCommand_Step = 0;
                mInternalCommand_Runnable = new Runnable() {
                    @Override
                    public void run() {
                        mInternalCommand = INTERNAL_CMD_CIRCLE;
                        mInternalCommand_Step = 1;
                        final Circle circle = new Circle();
                        circle.setRadius(radius);
                        circle.setCoordinate(new LatLongAlt(lat, lng, altitude));
                        circle.setTurns(turns);
                        mission.addMissionItem(circle);
                        MissionApi.getApi(mDrone).setMission(mission, true);
                        mInternalCommand = INTERNAL_CMD_CIRCLE;
                        mInternalCommand_Step = 2;


                    }
                };
            }

                @Override
                public void OnFailue ( int code){
                    clearInternalCommand();
                    if (lo7Ta7akom_callback != null) lo7Ta7akom_callback.OnFailue(code);
                    final String error = MavLink_Helpers.getACKError(code);
                    PanicFacade.cannotDoAutopilotAction(error);
                }

                @Override
                public void OnTimeout () {
                    clearInternalCommand();
                    if (lo7Ta7akom_callback != null) lo7Ta7akom_callback.OnTimeout();
                    PanicFacade.cannotDoAutopilotAction("Time Out. Please try Circle Here again");
                }
            });
    }


    public void do_TriggerCamera ()
    {
        if ((mDrone==null) || (!isConnected()))
        {
            return ;
        }

        ExperimentalApi.getApi(mDrone).triggerCamera();
    }

    public void do_ChangeSysID (final int sysID)
    {
        final Parameters parameters = new Parameters();
        Parameter parameter = new Parameter("SYSID_THISMAV", sysID , 1);

        parameters.addParameter(parameter);
        VehicleApi.getApi(mDrone).writeParameters(parameters);
    }


    @Override
    public void onGimbalOrientationUpdate(GimbalApi.GimbalOrientation orientation) {
        if (AndruavSettings.andruavWe7daBase.FCBoard !=null)   ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_OnGimbalOrientationUpdate(orientation.getPitch(),orientation.getRoll(),orientation.getYaw());
    }

    @Override
    public void onGimbalOrientationCommandError(int error) {
        if (AndruavSettings.andruavWe7daBase.FCBoard !=null)   ((ControlBoard_DroneKit)AndruavSettings.andruavWe7daBase.FCBoard).onDroneEvent_OnGimbalOrientationCommandError(error);
    }



    private void clearInternalCommand ()
    {
        mInternalCommand = INTERNAL_CMD_NON;
        mInternalCommand_Step =0;
    }
    private void ExecuteInternalCommand ()
    {
        try {
            switch (mInternalCommand) {
                case INTERNAL_CMD_NON:
                    mInternalCommand_Step = 0;
                    break;
                case INTERNAL_CMD_CIRCLE: {
                    switch (mInternalCommand_Step) {
                        case 0:
                            handler.postDelayed(mInternalCommand_Runnable,2500);
                            break;
                        case 1:
                            // skip Mission Updated Event
                            //mInternalCommand_Step = 2;
                            break;
                        case 2:
                            clearInternalCommand();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    do_Auto(new IControlBoard_Callback() {
                                        @Override
                                        public void OnSuccess() {
                                            clearInternalCommand();
                                        }

                                        @Override
                                        public void OnFailue(int code) {
                                            final String error = MavLink_Helpers.getACKError(code);
                                            PanicFacade.cannotDoAutopilotAction(error);

                                        }
                                        @Override
                                        public void OnTimeout() {
                                            PanicFacade.cannotDoAutopilotAction("Time Out. Please try Circle Here again");

                                        }
                                    });
                                }
                            },2500);
                            break;

                    }
                }
                break;

            }
        }
        catch (Exception e)
        {

        }
    }
}
