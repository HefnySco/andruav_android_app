package com.andruav.protocol.commands.textMessages.FlightControl;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by M.Hefny on 13-May-15.
 * <br>cmd: <b>1010</b>
 * Send a change flight-mode request to a Drone
 */
public class AndruavMessage_FlightControl extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_FlightControl = 1010;

    public int FlightMode;
    public double longitude=0.0f;
    public double latitude=0.0f;
    public double radius=0.0f;      // used in circle mode


    public AndruavMessage_FlightControl() {
        super();
        messageTypeID = TYPE_AndruavMessage_FlightControl;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);
        FlightMode = json_receive_data.getInt("F");
        if (json_receive_data.has("g")) {
            longitude = (int) json_receive_data.getDouble("g");
            latitude = (int) json_receive_data.getDouble("a");
        }
        if (json_receive_data.has("r"))
        {
            radius = (int) json_receive_data.getDouble("r");
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
        json_data.accumulate("F", FlightMode);
        if (latitude != 0.0f)
        {
            json_data.accumulate("g", longitude);
            json_data.accumulate("a", latitude);
        }
        if (radius != 0.0f)
        {
            json_data.accumulate("r", radius);
        }

        return json_data.toString();
    }


}
