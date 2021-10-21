package com.andruav.protocol.commands.textMessages.Configuration;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * This command targets Drones Only.
 * Created by mhefny on 7/3/16.
 */
public class AndruavMessage_Config_UnitID extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_Config_UnitID = 1026;


    public String UnitID;
    public String Description;
    public String GroupName;
    public String PartyID;


    public AndruavMessage_Config_UnitID()
    {
        messageTypeID = TYPE_AndruavMessage_Config_UnitID;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        PartyID     = json_receive_data.getString("p");
        UnitID      = json_receive_data.getString("u");
        Description = json_receive_data.getString("d");
        GroupName   = json_receive_data.getString("g");

    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();

        json_data.accumulate("p",PartyID);
        json_data.accumulate("u",UnitID);
        json_data.accumulate("d",Description);
        json_data.accumulate("g",GroupName);


        return json_data.toString();
    }
}
