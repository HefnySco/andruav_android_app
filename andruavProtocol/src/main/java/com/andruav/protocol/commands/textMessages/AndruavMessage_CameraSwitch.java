package com.andruav.protocol.commands.textMessages;


import org.json.JSONException;
import org.json.JSONObject;

/**
 * This message used to switch between cameras that share same Camera UID
 * such as front and rear cameras in Android Devices.
 */
public class AndruavMessage_CameraSwitch extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_CameraSwitch = 1050;

    public String CameraUniqueName;

    public AndruavMessage_CameraSwitch() {
        super();

        messageTypeID = TYPE_AndruavMessage_CameraSwitch;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        CameraUniqueName = json_receive_data.getString("u");
    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("u", CameraUniqueName);

        return json_data.toString();
    }
}
