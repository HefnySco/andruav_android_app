package com.andruav.protocol.commands.textMessages.Control;

import androidx.collection.SimpleArrayMap;

import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_RemoteControlSettings;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_WayPoints;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ExternalCommand_GeoFence;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GeoFenceAttachStatus;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ID;
import com.andruav.protocol.commands.textMessages.AndruavMessage_POW;
import com.andruav.protocol.commands.textMessages.AndruavMessage_WayPoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by M.Hefny on 30-Oct-14.
 * <br>cmd: <b>1005</b>
 * Request Execution of a command on a remote vehicle.
 * The command can be any of Andruav Commands for example {@link AndruavMessage_ID} means sendMessageToModule me your ID in a {@link AndruavMessage_ID} command.
 * Also commands can be orders such as video on, capture images ...etc.
 * @example: 1- Send_ID
 * 2- Take image(s) in remote mobile
 * 3- sendMessageToModule SMS of location
 * 4- RotateCAM in remote mobile.
 * 5- Send IMU data to sender [subscribe sender in target list of IMU listeners]
 */
public class AndruavMessage_RemoteExecute extends AndruavMessage_Control_Base {

    public final static int TYPE_AndruavMessage_RemoteExecute = 1005;


    /***
     * Request target to sendMessageToModule {@link AndruavMessage_ID} back. i.e. Identification message.
     */
    public final static int RemoteCommand_REQUEST_ID = AndruavMessage_ID.TYPE_AndruavMessage_ID;

    public final static int RemoteControl_RequestRemoteControlSettings = AndruavResalaBinary_RemoteControlSettings.TYPE_AndruavMessage_RemoteControlSettings;

    /***
     * Request a one time POWER Information form a drone.
     */
    public final static int RemoteCommand_REQUEST_POW = AndruavMessage_POW.TYPE_AndruavMessage_POW;


    public final static int RemoteCommand_MAKETILT = 100;
    /***
     * Instruct remote vehicle to take photo
     * NumberofImages: number of images.
     * TimeBetweenShots: difference between images in milliseconds.
     * isFCB:
     */
    public final static int RemoteCommand_TAKEIMAGE = 102;
    /***
     * Instruct target unit to make Beep sound
     */
    public final static int RemoteCommand_MAKEBEEP = 103;
    /***
     * Instruct target unit to sendMessageToModule SMS to number saved on Target units.
     * <br>Message contains GPS Info.
     */
    public final static int RemoteCommand_SENDSMS = 104;

    /***
     * Older Andruav version could not rotate image box
     * so instead we rotate source camera on drone.
     * later rotation on drone is not applied -as picture orientation is the same now in Drone.
     */
    public final static int RemoteCommand_ROTATECAM = 105;

    /***
     * Variable: Act -- boolean
     * TRUE: Send IMU data
     * False Dont sendMessageToModule IMU data.
     */
    public final static int RemoteCommand_IMUCTRL = 106;

    public final static int RemoteCommand_SMSwGPS = 107;

    /***
     * Request to sendMessageToModule telemetry data
     * TRUE: sendMessageToModule telemetry data
     * False: Dont sendMessageToModule telemetry data.
     */
    public final static int RemoteCommand_TELEMETRYCTRL = 108;

    /***
     * sends a notification to another unit.
     * a typical use is sendMessageToModule error or warning messages to GCS.
     */
    public final static int RemoteCommand_NOTIFICATION = 109;

    /***
     * sends a notification asking for streaming video
     * <br>Variable: Act -- boolean
     * <br>TRUE: Start Streaming
     * <br>False STOP Streaming
     */
    public final static int RemoteCommand_STREAMVIDEO = 110;

    /***
     * Variable: Act -- boolean
     * <br>TRUE: Start Recording
     * <br>False STOP Recording
     */
    public final static int RemoteCommand_RECORDVIDEO = 111;

    /***
     * This is used to overcome TCP retransmission issue.
     * <br>You need to tell the sender each frames to continue sendMessageToModule you video,
     * <br>This ensures a pause when there is a delay.
     */
    public final static int RemoteCommand_STREAMVIDEORESUME = 112;


    /***
     * This command is used to remotely change unitID name -permenantly-.
     * <br>A variable <b>uid</b> contains unit new name
     */
    public final static int RemoteCommand_ChangeUnitID = 113;


    /***
     * Activate other cams. in case of no extra parameters this is a round-robin switch.
     */
    @Deprecated
    public final static int RemoteCommand_SWITCHCAM = 114;


    /***
     * Enforce a GPS type such [AUTO , MOBILE , FCB]
     * <br> command: s = 0 , 1 , 2
     * <br> This command is sent to a Drone to witch is GPS Source externally
     * <br><b>even if selected GPS is null it will be sent, internally Drone can select the most suitable source for navigation.</b>
     */
    public final static int RemoteCommand_SET_GPS_SOURCE = 115;


    /***
     * Disconnect and reconnect again .. this can be used to enforce new name or new server ips.
     * <br> command: c:
     * <br>         0 - Disconect
     * <br>         1 - Connect
     * <br>         2 - Disconnect & Reconnect
     * <br> command: t: reconnect after t milliseconds.
     */
    public final static int RemoteCommand_SET_CONNECT = 116;


    /***
     * Instruct target unit to make flash using LED and Screen.
     *
     */
    public final static int RemoteCommand_MAKEFLASH = 117;



    /***
     * Instruct target unit to connect to FCB using current configuration.
     *
     */
    public final static int RemoteCommand_CONNECT_FCB = 118;



    /***
     * Asks Drone to sendMessageToModule back stored Path using {@link AndruavMessage_WayPoints}
     * <br> Drone (may) want to refresh data from FCB but not mandatory.
     */
    public final static int RemoteCommand_GET_WAY_POINTS = 500;




    /***
     * Asks Drone to sendMessageToModule back mission after reloading it from FCB if exist.
     * <br>This is a deep reload
     * <br> Reload WayPoints from OfflineTasks is used by Command Type {@link com.andruav.protocol.commands.textMessages.systemCommands.AndruavSystem_LoadTasks}
     * and give the command Type of {@link AndruavMessage_ExternalCommand_GeoFence}     */
    public final static int RemoteCommand_RELOAD_WAY_POINTS_FROM_FCB = 501;

    /***
     * Asks Drone to clear its waypoints
     * <br> Drone will need  to instruct FCB to do so. and then sbroadcast a {@link AndruavResalaBinary_WayPoints}
     * to all parties with new waypoints which should be null.
     */
    public final static int  RemoteCommand_CLEAR_WAY_POINTS = 502;


    /***
     * Asks Drone to clear its GeoFences
     * <br>A Drone should respond with DeAttach command to all. {@link AndruavMessage_GeoFenceAttachStatus}
     *
     * <br><b>Params:</b>
     * <br>n: fence name.  a <b>null</b> value means all fences.
     * <br>a: isAttachedToFence
     * */
    public final static int RemoteCommand_CLEAR_FENCE_DATA = 503;


    /***
     * Asks Drone to Mark Mission Item as the start point
     *
     * <br><b>Params:</b>
     * <br>n: MissionItem  a <b>1</b> number of mission item should be less than mission item count. Mission 0 = Home as per APM iplementation.
     * */
    public final static int RemoteCommand_SET_START_MISSION_ITEM = 504;


    public final SimpleArrayMap<String, String> Variables = new SimpleArrayMap<>();
    //public final static int T
    /***
     * This represents the ID of the command that we need to processInterModuleMessages on remote party
     * it can be RemoteCommandID of OTHER CLASSES such as GPS,IMU..etc.
     * e.g can equal AndruavResala_ID.TYPE_AndruavMessage_ID for REQUESTING ID info.
     * or can be other specific commands.
     */
    public int RemoteCommandID;

    public AndruavMessage_RemoteExecute() {
        super();
        messageTypeID = TYPE_AndruavMessage_RemoteExecute;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);
        Iterator<String> keys = json_receive_data.keys();
        String key;
        while (keys.hasNext()) {
            key = keys.next();
            if (key.equals("C")) {
                RemoteCommandID = json_receive_data.getInt("C");
            } else {
                Variables.put(key, json_receive_data.getString(key));
            }

        }

    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("C", RemoteCommandID);
        int s = Variables.size();
        for (int i = 0; i < s; ++i) {
            json_data.accumulate(Variables.keyAt(i), Variables.valueAt(i));

        }

        return json_data.toString();
    }



    public int getIntValue(String Key, final int nullValue) {
        if (Variables.indexOfKey(Key) < 0) return  nullValue;
        return Integer.parseInt(Variables.get(Key));
    }

    public int getIntValue(String Key) {
        return Integer.parseInt(Variables.get(Key));
    }

    public Boolean getBooleanValue(String Key) {
        return Boolean.parseBoolean(Variables.get(Key));

    }
}
