package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class AndruavMessage_UploadWayPoints extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_UploadWayPoints = 1046;

    public String MissionText;

    public AndruavMessage_UploadWayPoints() {
        super();
        messageTypeID = TYPE_AndruavMessage_UploadWayPoints;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException, ParseException {

        JSONObject json_receive_data = new JSONObject(messageText);
        MissionText = json_receive_data.getString("a");
    }

    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("a",MissionText);
        return json_data.toString();

    }

}
