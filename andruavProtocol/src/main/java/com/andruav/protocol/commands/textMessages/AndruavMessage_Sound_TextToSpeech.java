package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

public class AndruavMessage_Sound_TextToSpeech extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_Sound_TextToSpeech = 6511;

    public String language;
    public int pitch  = -1;
    public int volume  = -1;
    public String text = "";

    public AndruavMessage_Sound_TextToSpeech() {
        super();
        messageTypeID = TYPE_AndruavMessage_Sound_TextToSpeech;
    }


    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        if (json_receive_data.has(("l"))) {
            language = json_receive_data.getString("l");
        }
        if (json_receive_data.has(("p"))) {
            pitch = json_receive_data.getInt("p");
        }
        if (json_receive_data.has(("v"))) {
            volume = json_receive_data.getInt("v");
        }
        text = json_receive_data.getString("t");
    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();

        json_data.accumulate("t", text);

        if (pitch!=-1) {
            json_data.accumulate("p", pitch);
        }

        if (volume!=-1) {
            json_data.accumulate("p", volume);
        }

        if (language!= "") {
            json_data.accumulate("l", language);
        }

        return json_data.toString();
    }
}
