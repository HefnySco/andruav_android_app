package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

public class AndruavMessage_ServoChannel extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_ServoChannel = 6001;


    public int channelNumber;

    public int channelValue;


    public AndruavMessage_ServoChannel() {
        super();
        messageTypeID = TYPE_AndruavMessage_ServoChannel;
    }

    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        channelNumber = json_receive_data.getInt("n");
        channelValue = json_receive_data.getInt("v");
    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("n", channelNumber);
        json_data.accumulate("v", channelValue);

        return json_data.toString();
    }

}