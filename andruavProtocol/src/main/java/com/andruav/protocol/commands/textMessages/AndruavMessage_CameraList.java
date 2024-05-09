package com.andruav.protocol.commands.textMessages;

import com.andruav.media.Event_VideoTrack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.andruav.uavos.modules.UAVOSConstants.UAVOS_MODULE_TYPE_CAMERA;

/**
 * Created by M.Hefny on 21-Jul-15.
 * <br>cmd: <b>1012</b>
 * <br>Used to sendMessageToModule URL of a 3rd Party Stream link. for example when using IPWebcam.
 * <br>The usrl is handled by AndruavUnit and displayed within Andruav screen.
 */
public class AndruavMessage_CameraList extends AndruavMessageBase {

    public final static int TYPE_AndruavCMD_CameraList = 1012;

    /***
     * Unknown Source -Generic-.
     */
    public final static int EXTERNAL_CAMERA_TYPE_UNKNOWN = 0;
    /***
     * IP WebCam.
     * <br>In this case destination unit can use HTML page to view video, and control camera using URL callbacks.
     */
    @Deprecated
    public final static int EXTERNAL_CAMERA_TYPE_IPWEBCAM = 1;

    /***
     * WEBRTC
     * <br>In this case destination unit can use HTML page to view video.
     */
    public final static int EXTERNAL_CAMERA_TYPE_RTCWEBCAM = 2;
    /***
     * Camera IP or Unit ID in case of RTC
     */
    public String IP;
    /***
     * Camera port
     */
    public int Port;

    /**
     *
     *  jsonVideoSource[CAMERA_SUPPORT_VIDEO "v"]       = true;
     *  jsonVideoSource[CAMERA_SUPPORT_VIDEO "r"]      = true; // recording
     *  jsonVideoSource[CAMERA_SUPPORT_FLASH "f"]       = true;
     *  jsonVideoSource[CAMERA_SUPPORT_ZOOM "z"]        = true;
     *  jsonVideoSource[CAMERA_LOCAL_NAME "ln"]         = deviceInfo.local_name;
     *  jsonVideoSource[CAMERA_UNIQUE_NAME "id"]        = deviceInfo.unique_name;
     *  jsonVideoSource[CAMERA_ACTIVE "active"]         = deviceInfo.active;
     *  jsonVideoSource[CAMERA_TYPE "p"]                = EXTERNAL_CAMERA_TYPE_RTCWEBCAM;
     *
     */
    public JSONArray CameraList;

    /***
     * Is it a reply on a request or just info of availble cameras
     */
    public boolean isReply = false;

    public final ArrayList<Event_VideoTrack> videoTracks = new ArrayList<Event_VideoTrack>();
    /***
     * {@link #EXTERNAL_CAMERA_TYPE_IPWEBCAM}, {@link #EXTERNAL_CAMERA_TYPE_UNKNOWN}
     */
    public int ExternalType = AndruavMessage_CameraList.EXTERNAL_CAMERA_TYPE_RTCWEBCAM;

    public AndruavMessage_CameraList() {
        messageTypeID = TYPE_AndruavCMD_CameraList;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        if (json_receive_data.has("R")) {
            isReply = json_receive_data.getBoolean("R");
        }

        if (json_receive_data.has("E")) {
            ExternalType = json_receive_data.getInt("E");
        }

        if (json_receive_data.has("T"))
        {
            JSONArray videos =json_receive_data.getJSONArray("T");
            final int len = videos.length();
            for (int i=0; i<len; ++i)
            {
                JSONObject o = (JSONObject) videos.get(i);

                videoTracks.add(new Event_VideoTrack(o.getString("id"),o.getBoolean("v"), o.getString("ln")));
            }
        }

    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_track = new JSONObject();
        json_track.put("v",1);
        json_track.put("id",IP);
        json_track.put("ln",UAVOS_MODULE_TYPE_CAMERA);
        JSONArray json_tracks = new JSONArray();
        json_tracks .put(json_track);

        JSONObject json_data = new JSONObject();

        if (isReply) {
            json_data.put("R", isReply);
        }
        json_data.put("E", ExternalType);
        if (CameraList == null)
        {
            CameraList = new JSONArray();
        }
        json_data.accumulate("T", CameraList);

        return json_data.toString();
    }

}
