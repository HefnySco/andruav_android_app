package ap.andruav_ap.communication.controlBoard.mavlink;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.mavlink.MAVLinkPacket;
import com.mavlink.common.msg_message_interval;
import com.mavlink.common.msg_mission_request_list;
import com.mavlink.common.msg_request_data_stream;
import com.mavlink.enums.MAV_STATE;
import com.mavlink.Parser;
import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.droneReport_Event.Event_Dummy;
import com.andruav.controlBoard.IControlBoard_Callback;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.communication.controlBoard.ControlBoard_MavlinkBase;
import ap.andruav_ap.communication.telemetry.SerialSocketServer.Event_SocketData;
import ap.andruavmiddlelibrary.factory.math.Angles;

/**
 * Created by M.Hefny on 04-Apr-15.
 */
public class ControlBoard_APM extends ControlBoard_MavlinkBase {


    IControlBoard_Callback currentCallBack;






    /***
     * @link
     */
    short APM_VehicleType = -1; /// look @MAV_TYPE
    short  base_mode; /// look @MAV_MODE_FLAG
    /***
     * {@Link} MAV.APMModes
     */
    long   custom_mode = -1;

    /***
     * Check {@link MAV_STATE}
     */
    short  mav_state; /// look @MAV_STATE
    short  mavlink_version;


    int waypoints_counts;


    protected ControlBoard_APM Me;
    protected Handler mhandle;
    protected HandlerThread mhandlerThread;






    public void onEvent (Event_Dummy a7adath_dummy)
    {

    }



    /***
     * This handles are created for Drone Andruav only.
     */
    protected void initHandler ()
    {


    }


    protected void unInitHandler ()
    {

    }


    public ControlBoard_APM(AndruavUnitBase andruavUnitBase) {
        super(andruavUnitBase);
        Me = this;
        parserDrone = new Parser();

        PitchPerUnit = 1;
        RollPerUnit = 1;
        YawPerUnit =1;
        HeadingPerUnit = Angles.DEGREES_TO_RADIANS;
        HeadingOffset = 0;
        VarioPerUnit = 1;

        gps_lnglat_scale = 10000000.0;
        gps_groundspeed_scale = 100.0;
        gps_alt_scale = 1000.0f;


    }


    @Override
    public String getFCBDescription()
    {
        return "MAVLINK ver:" + this.mavlink_version;
    }

    @Deprecated
    public void ExecutePacketfromGCS (MAVLinkPacket mavLinkPacket, boolean sendPacket)
    {
        // Heartbeat is recieved as well from GCS

        switch (mavLinkPacket.msgid)
        {
            case msg_request_data_stream.MAVLINK_MSG_ID_REQUEST_DATA_STREAM:
                msg_request_data_stream m = (msg_request_data_stream) mavLinkPacket.unpack();
                if (m.start_stop==0) {
                     return ;
                }
                break;

        }
        return ;
    }






    protected void execute_msg_request_list (msg_mission_request_list msg_mission_request_list)
    {
        // clear and be ready to refil
        mAndruavUnitBase.getMohemmaMapBase().clear();
    }

    @Override
    public void ActivateListener (boolean bActivate) {
        if (bActivate) {
            initHandler();
            EventBus.getDefault().register(this,1);

        }
        else
        {
            unInitHandler(); // should be before un register
            EventBus.getDefault().unregister(this);

        }
    }






























    protected void sendMessageInterval () {
        try
        {
            final msg_message_interval msg = new msg_message_interval ();
            msg.interval_us=100000;

            sendSimulatedPacket(msg.pack().encodePacket());
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException("apm_mavlink",ex);
        }
    }






    Event_SocketData event_socketData;












    synchronized public  void sendSimulatedPacket(final MAVLinkPacket mavLinkPacket, final IControlBoard_Callback ILo7Ta7Akom__callback)
    {
        currentCallBack = ILo7Ta7Akom__callback;
        sendSimulatedPacket(mavLinkPacket.encodePacket());
    }


    /***
     * compid & sysid are for the board that this instance is attached to.
     * @param mavLinkPacket
     */
    synchronized  public static void sendSimulatedPacket(final MAVLinkPacket mavLinkPacket, final int sysID, final int compID)
    {

        mavLinkPacket.compid = compID;
        mavLinkPacket.sysid = sysID;

        sendSimulatedPacket(mavLinkPacket.encodePacket());
        Log.d("simv", mavLinkPacket.sysid + " - " + mavLinkPacket.compid + " - " + mavLinkPacket.msgid);
    }



    /***
     * Send Packet as if it comes from GCS to FCB board
     * @param Data
     */
    synchronized public static void sendSimulatedPacket(final byte[] Data)
    {

            final Event_SocketData event_socketData = new Event_SocketData();
            event_socketData.IsLocal = Event_SocketData.SOURCE_SIMULATED;
            event_socketData.Data = Data;
            event_socketData.DataLength = event_socketData.Data.length;

            EventBus.getDefault().post(event_socketData);

    }


    /***
     * Send Packet as if it comes from GCS to FCB board
     * @param Data
     */
    synchronized public static void sendSimulatedPacket_Emergency(final byte[] Data)
    {

        final Event_SocketData event_socketData = new Event_SocketData();
        event_socketData.byPassBlockedGCS = true;
        event_socketData.IsLocal = Event_SocketData.SOURCE_SIMULATED;
        event_socketData.Data = Data;
        event_socketData.DataLength = event_socketData.Data.length;

        EventBus.getDefault().post(event_socketData);

    }

 }
