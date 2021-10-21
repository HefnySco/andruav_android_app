package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Created by mhefny on 6/20/16.
 */
public class AndruavMessage_GEOFenceHit extends AndruavMessageBase {

    public final static int TYPE_AndruavResala_GEOFenceHit = 1025;


    public String fenceName;
    public boolean inZone;
    public boolean shouldKeepOutside;
    /***
     * in meters
     */
    public double distance;


    public AndruavMessage_GEOFenceHit() {
        super();
        messageTypeID = TYPE_AndruavResala_GEOFenceHit;

    }

    @Override
    public void setMessageText(String messageText) throws JSONException, ParseException {

        final NumberFormat nf = NumberFormat.getInstance(Locale.US);
        JSONObject json_receive_data = new JSONObject(messageText);

        if (json_receive_data.has("d")) {
            distance = nf.parse(json_receive_data.getString("d")).doubleValue();
        }
        else
        {
            distance = Double.NaN;
        }
        fenceName = json_receive_data.getString("n");
        inZone = json_receive_data.getBoolean("z");
        shouldKeepOutside = (json_receive_data.getInt("o")==1);

    }


    @Override
    public String getJsonMessage() throws JSONException {
        JSONObject json_data = new JSONObject();

        json_data.accumulate("n", fenceName);
        json_data.accumulate("z", inZone);
        if (!Double.isNaN(distance)) {
            json_data.accumulate("d", distance);
        }
        json_data.accumulate("o", shouldKeepOutside?1:0);

        return json_data.toString();
    }


}
