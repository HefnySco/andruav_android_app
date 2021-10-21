package com.andruav.protocol.commands.textMessages.FlightControl;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mhefny on 10/17/16.
 */

public class AndruavMessage_DoYAW extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_DoYAW = 1035;


    public double targetAngle;
    /***
     * Degree per second.
     */
    public double turnRate;
    /***
     *
     */
    public boolean isClockwise;
    public boolean isRelative;

    public AndruavMessage_DoYAW() {
        super();
        messageTypeID = TYPE_AndruavMessage_DoYAW;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);
        targetAngle =  json_receive_data.getDouble("A");
        turnRate    =  json_receive_data.getDouble("R");
        isClockwise =  json_receive_data.getBoolean("C");
        isRelative  =  json_receive_data.getBoolean("L");


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
        json_data.accumulate("A", targetAngle);
        json_data.accumulate("R", turnRate);
        json_data.accumulate("C", isClockwise);
        json_data.accumulate("L", isRelative);


        return json_data.toString();
    }
}
