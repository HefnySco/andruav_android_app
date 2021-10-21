package com.andruav.protocol.commands.textMessages.FlightControl;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mhefny on 8/25/16.
 */
public class AndruavMessage_ChangeAltitude extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_ChangeAltitude = 1031;

    /***
     * in meters
     */
    public double altitude;


    public AndruavMessage_ChangeAltitude() {
        super();
        messageTypeID = TYPE_AndruavMessage_ChangeAltitude;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);
        altitude = (int) json_receive_data.getDouble("a");

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
        json_data.accumulate("a", altitude);

        return json_data.toString();
    }


}