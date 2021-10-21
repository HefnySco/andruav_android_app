package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class AndruavMessage_SetHomeLocation extends AndruavMessageBase {
    public final static int TYPE_AndruavMessage_SetHomeLocation = 1048;

/////////////////////////// HOME LOCATION
    /**
     * GPS Longitude <b>HOME</b> location
     */
    public double    home_gps_lng;
    /**
     * GPS Latitude <b>HOME</b> location
     */
    public double    home_gps_lat;
    /**
     * GPS Latitude <b>HOME</b> location
     */
    public double    home_gps_alt;

    public AndruavMessage_SetHomeLocation() {
        super();
        messageTypeID = TYPE_AndruavMessage_SetHomeLocation;
    }

    @Override
    public void setMessageText(String messageText) throws JSONException, ParseException {

        final NumberFormat nf_us = NumberFormat.getInstance(Locale.US);

        final JSONObject json_receive_data = new JSONObject(messageText);

        home_gps_lng = nf_us.parse(json_receive_data.getString("O")).doubleValue();
        home_gps_lat = nf_us.parse(json_receive_data.getString("T")).doubleValue();
        home_gps_alt = nf_us.parse(json_receive_data.getString("A")).doubleValue();


    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        //Gson gson = new Gson();
        final JSONObject json_data = new JSONObject();
        json_data.accumulate("O", home_gps_lng);
        json_data.accumulate("T", home_gps_lat);
        json_data.accumulate("A", home_gps_alt);

        return json_data.toString();
    }
}
