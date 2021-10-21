package com.andruav.protocol.commands.textMessages.Configuration;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

public class AndruavMessage_Config_Preference extends AndruavMessageBase {

    public final static int TYPE_AndruavResala_Config_Preference = 8000;


    public int          TValue;
    public boolean      BValue;
    public int          IValue;
    public String       SValue;
    public String       KEYID;


    public AndruavMessage_Config_Preference()
    {
        messageTypeID = TYPE_AndruavResala_Config_Preference;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);
        KEYID   = json_receive_data.getString("k");
        TValue  = json_receive_data.getInt("t");
        switch (TValue)
        {
            case 1:
                IValue  = json_receive_data.getInt("v");
                break;
            case 2:
                BValue  = json_receive_data.getBoolean("v");
                break;
            case 3:
                SValue  = json_receive_data.getString("v");
                break;
        }

    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();

        json_data.accumulate("k",KEYID);
        json_data.accumulate("t",TValue);
        switch (TValue)
        {
            case 1:
                json_data.accumulate("v",IValue);
                break;
            case 2:
                json_data.accumulate("v",BValue);
                break;
            case 3:
                json_data.accumulate("v",SValue);
                break;
        }

        return json_data.toString();
    }
}
