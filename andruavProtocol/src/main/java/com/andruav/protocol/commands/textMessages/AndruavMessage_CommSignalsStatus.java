package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

public class AndruavMessage_CommSignalsStatus extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_CommSignalsStatus = 1059;


    public int signalType; // GSM of other drone
    public int signalLevel; // GSM of other drone


    public AndruavMessage_CommSignalsStatus() {
        super();
        messageTypeID = TYPE_AndruavMessage_CommSignalsStatus;
    }
    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        if (json_receive_data.has("r"))  signalLevel = json_receive_data.getInt("r");
        if (json_receive_data.has("s"))  signalType  = json_receive_data.getInt("s");

    }

    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("r",signalLevel);
        json_data.accumulate("s",signalType);


        return json_data.toString();
    }
}
