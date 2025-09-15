package com.andruav.protocol.commands.textMessages;

import android.location.Location;

import com.andruav.andruavUnit.AndruavLocation;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Created by M.Hefny on 28-Oct-14.
 * <br>cmd: <b>1002</b>
 */
public class AndruavMessage_GPS extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_GPS = 1002;


    public boolean hasNAVInfo = false;


    public final static int GPS_SOURCE_MOBILE   =1;
    public final static int GPS_SOURCE_FCB      =2;


    /**
     * GPS3DFix
     */
    public int GPS3DFix;

    /***
     * This is either mobile of FCB as it represent an instance GPS read
     * NOT AndruavWe7da.gpsMode
     */
    public boolean GPSFCB;
    public AndruavLocation CurrentLocation;
    /**
     * Sattellite Count
     */
    public int SATC;


    public AndruavMessage_GPS() {
        super();
        messageTypeID = TYPE_AndruavMessage_GPS;
    }


    @Override
    public void setMessageText(final String messageText) throws JSONException, ParseException {

        final NumberFormat nf_us = NumberFormat.getInstance(Locale.US);

        JSONObject json_receive_data = new JSONObject(messageText);

        GPS3DFix = json_receive_data.getInt("3D");
        GPSFCB = json_receive_data.getBoolean("GS");

        //Gson gson = new Gson();
        // ImageLocation = gson.fromJson(json_receive_data.getString("CL"),Location.class);
        SATC = json_receive_data.getInt("SC");
        //GPS3DFix = json_receive_data.getInt("3D");

        if (json_receive_data.has("p")) {
            //GroundAltitude = Float.parseFloat(json_receive_data.getString("galt"));
            CurrentLocation = new AndruavLocation(json_receive_data.getString("p"));
            CurrentLocation.setLatitude(nf_us.parse(json_receive_data.getString("la")).doubleValue());
            CurrentLocation.setLongitude(nf_us.parse(json_receive_data.getString("ln")).doubleValue());
            CurrentLocation.setAltitude(nf_us.parse(json_receive_data.getString("a")).doubleValue());
            CurrentLocation.setAltitudeRelative(nf_us.parse(json_receive_data.getString("r")).doubleValue());
            CurrentLocation.setTime(json_receive_data.getLong("t"));
            if (json_receive_data.has("s")) {
                CurrentLocation.setSpeed((float) json_receive_data.getDouble("s"));
            }

            if (json_receive_data.has("b")) {
                CurrentLocation.setBearing((float) json_receive_data.getDouble("b"));
            }
            if (json_receive_data.has("c")) {
                CurrentLocation.setAccuracy((float) json_receive_data.getDouble("c"));
            }
        }





    }

    /***
     * Attributes:
     * <br> "3D" 3DFix
     * <br> "Y"  YAW
     * <br> "SC" Satellite Counts
     * <br> "la" Latitude
     * <br> "ln" Longitude
     * <br> "p"  Provider
     * <br> "t" Time
     * <br> "a" Altitude
     * <br> "g" GroundAltitude
     * <br> "s" Speed in m/s
     * <br> "b" Bearing
     * <br> "c" Accuracy in meters
     *
     * @return
     * @throws org.json.JSONException
     */
    @Override
    public String getJsonMessage() throws org.json.JSONException {
        //Gson gson = new Gson();
        JSONObject json_data = new JSONObject();
        json_data.accumulate("3D", GPS3DFix);
        json_data.accumulate("SC", SATC);
        json_data.accumulate("GS", GPSFCB);
        if (CurrentLocation != null) {
            // @see <a href=http://localhost:8080/mantis/view.php?id=25>java.lang.NumberFormatException: Invalid double: "11,021156"  </a>
            json_data.accumulate("la", String.format(Locale.US, "%4.6f", CurrentLocation.getLatitude()));
            json_data.accumulate("ln", String.format(Locale.US, "%4.6f", CurrentLocation.getLongitude()));
            json_data.accumulate("p", CurrentLocation.getProvider());
            json_data.accumulate("t", CurrentLocation.getTime());
            json_data.accumulate("a", String.format(Locale.US, "%5.1f", CurrentLocation.getAltitude()).trim());
            json_data.accumulate("r", String.format(Locale.US, "%5.1f", CurrentLocation.getAltitudeRelative()).trim());
            if (CurrentLocation.hasSpeed()) {
                json_data.accumulate("s", String.format(Locale.US, "%.3f", CurrentLocation.getSpeed()).trim());
            }
            if (CurrentLocation.hasBearing()) {
                json_data.accumulate("b", String.format(Locale.US, "%.4f", CurrentLocation.getBearing()).trim());
            }
            if (CurrentLocation.getAccuracy() != 0) {
                json_data.accumulate("c", CurrentLocation.getAccuracy());
            }
        }



        return json_data.toString();
    }


}
