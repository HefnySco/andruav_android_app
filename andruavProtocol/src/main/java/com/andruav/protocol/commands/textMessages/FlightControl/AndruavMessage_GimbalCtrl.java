package com.andruav.protocol.commands.textMessages.FlightControl;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mhefny on 4/10/17.
 */

public class AndruavMessage_GimbalCtrl extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_GimbalCtrl = 1045;


    public double pitch_degx100;
    public double roll_degx100;
    public double yaw_degx100;
    public boolean isAbsolute;

    public AndruavMessage_GimbalCtrl() {
        super();
        messageTypeID = TYPE_AndruavMessage_GimbalCtrl;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);
        pitch_degx100 = json_receive_data.getDouble("A");
        roll_degx100 = json_receive_data.getDouble("B");
        yaw_degx100 = json_receive_data.getDouble("C");
        isAbsolute = json_receive_data.getBoolean("D");
    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("A", pitch_degx100);
        json_data.accumulate("B", roll_degx100);
        json_data.accumulate("C", yaw_degx100);
        json_data.accumulate("D", isAbsolute);

        return json_data.toString();
    }
}
