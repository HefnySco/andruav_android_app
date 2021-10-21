package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by mhefny on 8/7/16.
 */
public class AndruavMessage_GeoFenceAttachStatus extends AndruavMessageBase {

    public final static int TYPE_AndruavResala_GeoFenceAttachStatus = 1029;


    public AndruavMessage_GeoFenceAttachStatus() {
        super();
        messageTypeID = TYPE_AndruavResala_GeoFenceAttachStatus;

    }


    public String fenceName;

    public boolean isAttachedToFence = false;

    @Override
    public void setMessageText(String messageText) throws JSONException, ParseException {

        JSONObject json_receive_data = new JSONObject(messageText);

        fenceName = json_receive_data.getString("n");
        isAttachedToFence = json_receive_data.getBoolean("a");
    }



    @Override
    public String getJsonMessage () throws JSONException {
        JSONObject json_data = new JSONObject();

        json_data.accumulate("n",fenceName);
        json_data.accumulate("a",isAttachedToFence);

        return json_data.toString();
    }

    }