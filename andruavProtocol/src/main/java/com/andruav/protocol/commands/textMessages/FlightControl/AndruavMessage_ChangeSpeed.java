package com.andruav.protocol.commands.textMessages.FlightControl;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mhefny on 12/10/16.
 */

public class AndruavMessage_ChangeSpeed extends AndruavMessageBase {

    public final static int TYPE_AndruavResala_ChangeSpeed = 1040;


    public final static int CONST_SPEED_NO_CHANGE = -1;

    /***
     * unused when calling 3DR for all Types
     * unused in quadcopter
     * unised in rover
     */
    public boolean isGroundSpeed = true;
    /***
     * m/s
     */
    public double speed = CONST_SPEED_NO_CHANGE;

    /***
     * unused when calling 3DR for all Types
     * unused in quadcopter
     */
    public double throttle = CONST_SPEED_NO_CHANGE;
    /***
     * unused when calling 3DR for all Types
     * unused in quadcopter
     * unused in rover
     */
    public boolean isRelative = false;


    public AndruavMessage_ChangeSpeed() {
        super();
        messageTypeID = TYPE_AndruavResala_ChangeSpeed;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);
        speed = json_receive_data.getDouble("a");
        isGroundSpeed = json_receive_data.getBoolean("b");
        throttle = json_receive_data.getDouble("c");
        isRelative = json_receive_data.getBoolean("d");
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

        json_data.accumulate("a",speed);
        json_data.accumulate("b",isGroundSpeed);
        json_data.accumulate("c",throttle);
        json_data.accumulate("d",isRelative);

        return json_data.toString();
    }

}
