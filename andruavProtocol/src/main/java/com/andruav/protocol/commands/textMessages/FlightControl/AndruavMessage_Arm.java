package com.andruav.protocol.commands.textMessages.FlightControl;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mhefny on 8/25/16.
 */
public class AndruavMessage_Arm extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_Arm = 1030;


    public boolean arm;

    public boolean emergencyDisarm;

    public AndruavMessage_Arm() {
        super();
        messageTypeID = TYPE_AndruavMessage_Arm;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);
        arm = json_receive_data.getBoolean("A");

        if (json_receive_data.has("D")) {
            // normaly D is not sent for Arming = true .. may be other cases also.
            emergencyDisarm = json_receive_data.getBoolean("D");
        }

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
        json_data.accumulate("A", arm);
        if (!arm) {
            json_data.accumulate("D", emergencyDisarm);
        }

        return json_data.toString();
    }

}
