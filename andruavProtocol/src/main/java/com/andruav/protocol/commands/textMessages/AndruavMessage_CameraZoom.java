package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

public class AndruavMessage_CameraZoom extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_CameraZoom = 1049;


    /**
     * Zomm In/Out
     */
    public boolean ZoomIn;

    /**
     * Value: 0..1
     * if stated then {@link #ZoomIn} and {@link #ZoomValueStep} are not needed
     */
    public Double ZoomValue = Double.MAX_VALUE;

    /**
     * Step to increase or decrease
     */
    public Double ZoomValueStep = Double.MAX_VALUE;

    public String CameraUniqueName;


    public AndruavMessage_CameraZoom() {
        super();

        messageTypeID = TYPE_AndruavMessage_CameraZoom;
    }



    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        if (json_receive_data.has("a")) ZoomIn = json_receive_data.getBoolean("a");
        if (json_receive_data.has("b")) ZoomValue = json_receive_data.getDouble("b");
        if (json_receive_data.has("c")) ZoomValueStep = json_receive_data.getDouble("c");
        CameraUniqueName = json_receive_data.getString("u");
    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        //json_data.accumulate("a", ZoomIn); NOT NEEDED TILL NOW

        if (ZoomValue != Double.MAX_VALUE)
        {   // this is absolute value we dont need to know zoomin/out
            json_data.accumulate("b", ZoomValue);
        }
        else
        {
            json_data.accumulate("a", ZoomIn);
        }
        if (ZoomValueStep != Double.MAX_VALUE) json_data.accumulate("c", ZoomValueStep);
        json_data.accumulate("u", CameraUniqueName);

        return json_data.toString();
    }
}
