package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Created by mhefny on 11/30/16.
 */

public class AndruavMessage_DistinationLocation extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_DistinationLocation = 1037;


    /////////////////////////// HOME LOCATION
    /**
     * GPS Longitude <b>HOME</b> location
     */
    public double    target_gps_lng;
    /**
     * GPS Latitude <b>HOME</b> location
     */
    public double    target_gps_lat;
    /**
     * GPS Latitude <b>HOME</b> location
     */
    public double    target_gps_alt;

    public AndruavMessage_DistinationLocation() {
        super();
        messageTypeID = TYPE_AndruavMessage_DistinationLocation;
    }

    @Override
    public void setMessageText(String messageText) throws JSONException, ParseException {

        final NumberFormat nf_us = NumberFormat.getInstance(Locale.US);

        final JSONObject json_receive_data = new JSONObject(messageText);

        target_gps_lng = nf_us.parse(json_receive_data.getString("O")).doubleValue();
        target_gps_lat = nf_us.parse(json_receive_data.getString("T")).doubleValue();
        target_gps_alt = nf_us.parse(json_receive_data.getString("A")).doubleValue();


    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        //Gson gson = new Gson();
        final JSONObject json_data = new JSONObject();
        json_data.accumulate("O", target_gps_lng);
        json_data.accumulate("T", target_gps_lat);
        json_data.accumulate("A", target_gps_alt);

        return json_data.toString();
    }


}
