package com.andruav.protocol.commands.textMessages.Configuration;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mhefny on 12/7/16.
 */

public class AndruavMessage_Config_COM extends AndruavMessageBase {

    public final static int TYPE_AndruavResala_Config_COM = 1038;


    public String   ServerIP;
    public int      Port;
    public boolean  IsLocalServer;


    public AndruavMessage_Config_COM()
    {
        messageTypeID = TYPE_AndruavResala_Config_COM;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        ServerIP        = json_receive_data.getString("a");
        Port            = json_receive_data.getInt("b");
        IsLocalServer   = json_receive_data.getBoolean("c");
    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();

        json_data.accumulate("a",ServerIP);
        json_data.accumulate("b",Port);
        json_data.accumulate("c",IsLocalServer);


        return json_data.toString();
    }
}
