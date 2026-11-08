package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

public class AndruavMessage_Sound_PlayFile extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_Sound_PlayFile = 6512;

    public String fileName = "";

    public AndruavMessage_Sound_PlayFile() {
        super();
        messageTypeID = TYPE_AndruavMessage_Sound_PlayFile;
    }


    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        fileName = json_receive_data.getString("f");
    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();

        json_data.accumulate("f", fileName);

        return json_data.toString();
    }
}
