package com.andruav.protocol.commands.textMessages.Control;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mhefny on 12/11/16.
 */

public class AndruavMessage_Ctrl_Camera extends AndruavMessage_Control_Base {

    public final static int TYPE_AndruavResala_Ctrl_Camera = 1041;


    public final static int CAMERA_SOURCE_MOBILE        = 1;
    public final static int CAMERA_SOURCE_FCB           = 2;
    public final static int CAMERA_SOURCE_ANDRUAVCAM    = 3;



    public int CameraSource             = CAMERA_SOURCE_MOBILE;
    public int NumberOfImages           = 1;
    /***
     * in milli seconds
     */
    public long TimeBetweenShotes        = 0;
    /***
     * in meters
     */
    public double DistanceBetweenShotes    = 0;

    public boolean SendBackImages = true; // sendMessageToModule images back to Requested GCS



    public AndruavMessage_Ctrl_Camera() {
        super();
        messageTypeID = TYPE_AndruavResala_Ctrl_Camera;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);
        CameraSource            = json_receive_data.getInt("a");
        NumberOfImages          = json_receive_data.getInt("b");
        TimeBetweenShotes       = json_receive_data.getLong("c");
        DistanceBetweenShotes   = json_receive_data.getDouble("d");
    }

    /***
     * You can fill the data using direct throttle variable of using setData
     * that is why variables are used to fill data so it is valid all time.
     *
     * @return
     * @throws org.json.JSONException
     */
    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();

        json_data.accumulate("a",CameraSource);
        json_data.accumulate("b",NumberOfImages);
        json_data.accumulate("c",TimeBetweenShotes);
        json_data.accumulate("d",DistanceBetweenShotes);

        return json_data.toString();
    }

}
