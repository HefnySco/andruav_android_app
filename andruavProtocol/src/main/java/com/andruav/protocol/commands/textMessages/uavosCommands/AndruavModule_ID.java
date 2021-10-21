package com.andruav.protocol.commands.textMessages.uavosCommands;


import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by mhefny on 21/10/17.
 */

public class AndruavModule_ID extends AndruavMessageBase {

    /***
     * request from an individual andruav to sendMessageToModule complete ID info.
     */
    public final static int TYPE_AndruavModule_ID = 9100;

    public String ModuleId;
    public String ModuleClass;
    public String ModuleKey;
    public JSONArray ModuleCapturedMessages;
    public JSONArray ModuleFeatures;
    public boolean SendBack = false;
    public String PartyID;
    public String GroupName;
    public Object ModuleMessage;

    public AndruavModule_ID()
    {
        messageTypeID = TYPE_AndruavModule_ID;
    }


    public AndruavModule_ID(final boolean sendBack)
    {
        SendBack = sendBack;
        messageTypeID = TYPE_AndruavModule_ID;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException, ParseException {


        try {
            final JSONObject json_receive_data = new JSONObject(messageText);

            // modules id
            ModuleId = json_receive_data.getString("a");

            // modules class
            ModuleClass = json_receive_data.getString("b");

            // modules class
            ModuleKey = json_receive_data.getString("e");

            // module messages
            if (json_receive_data.has("c"))
                ModuleCapturedMessages = json_receive_data.getJSONArray("c");

            // module featues
            if (json_receive_data.has("d")) ModuleFeatures = json_receive_data.getJSONArray("d");

            //sendMessageToModule back
            if (json_receive_data.has("z")) SendBack = json_receive_data.getBoolean("z");

            if (json_receive_data.has("m")) {
                ModuleMessage = json_receive_data.get("m");
            }


        }
        catch (final Exception e)
        {

        }

    }


    @Override
    public String getJsonMessage () throws org.json.JSONException {
        final JSONObject json_data= new JSONObject();

        json_data.accumulate("a", ModuleId);
        json_data.accumulate("b", ModuleClass);
        json_data.accumulate("d", ModuleFeatures);
        json_data.accumulate("e", ModuleKey);
        json_data.accumulate("z", SendBack);



        if (ModuleMessage != null) json_data.accumulate("m", ModuleMessage);
        if (ModuleCapturedMessages != null) json_data.accumulate("c", ModuleCapturedMessages);


        return json_data.toString();

    }



}
