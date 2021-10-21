package com.andruav.protocol.commands.textMessages.FlightControl;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mhefny on 8/25/16.
 */
public class AndruavMessage_Land extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_Land = 1032;



    public AndruavMessage_Land() {
        super();
        messageTypeID = TYPE_AndruavMessage_Land;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {

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


        return json_data.toString();
    }


}