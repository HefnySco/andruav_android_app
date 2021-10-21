package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

public class AndruavMessage_CameraFlash extends AndruavMessageBase {

    public final static int TYPE_AndruavResala_CameraFlash = 1051;


    public final static int FLASH_OFF       = 0;
    public final static int FLASH_ON        = 1;
    public final static int FLASH_DISABLED  = 999;

    public String CameraUniqueName;
    public int FlashOn = FLASH_DISABLED;

    public AndruavMessage_CameraFlash() {
        super();

        messageTypeID = TYPE_AndruavResala_CameraFlash;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        if (json_receive_data.has("f")) FlashOn = json_receive_data.getInt("f");
        CameraUniqueName = json_receive_data.getString("u");
    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("f", FlashOn);
        json_data.accumulate("u", CameraUniqueName);

        return json_data.toString();
    }

}
