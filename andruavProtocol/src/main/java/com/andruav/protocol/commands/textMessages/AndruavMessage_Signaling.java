package com.andruav.protocol.commands.textMessages;

import com.andruav.AndruavEngine;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mhefny on 3/5/16.
 * Should be used as Signalling Socket for WEBRTC ... but not working properly  :(
 */
public class AndruavMessage_Signaling extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_Signaling = 1021;


   protected JSONObject mJsonResala;


    public JSONObject getJsonResala ()
    {
        return  mJsonResala;
    }

    public AndruavMessage_Signaling()
    {
        super();
        messageTypeID = TYPE_AndruavMessage_Signaling;

    }

    public AndruavMessage_Signaling(final JSONObject jsonObject) {
        this();

        mJsonResala = jsonObject;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        try {
            JSONObject json_receive_data = new JSONObject(messageText);
            String jsonString = "";
            if (json_receive_data.has("w")) {
                mJsonResala = json_receive_data.getJSONObject("w");
            }
        }catch (Exception e)
        {
            AndruavEngine.log().logException("parse", e);
        }

    }

    /***
     * You can fill the data using direct throttle variable of using setData
     * that is why variables are used to fill data so it is valid all time.
     *
     * @return
     * @throws JSONException
     */
    @Override
    public String getJsonMessage() throws JSONException {
       try
       {
           JSONObject json_data = new JSONObject();

        if (mJsonResala!= null) {
            json_data.accumulate("w", mJsonResala);
        }
        return json_data.toString();
       }catch (Exception e)
       {
           e.printStackTrace();
           return  "";
       }
    }

}
