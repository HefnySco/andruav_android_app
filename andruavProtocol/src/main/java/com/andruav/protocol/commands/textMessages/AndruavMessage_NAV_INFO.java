package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Created by mhefny on 11/28/16.
 */

public class AndruavMessage_NAV_INFO extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_NAV_INFO = 1036;

    /***
     * Current desired roll in degrees
     */
    public double nav_roll;
    /***
     * Current desired pitch in degrees
     */
    public double nav_pitch;

    public double nav_yaw;
    /***
     * Bearing to current MISSION/target in degrees
     */
    public double    target_bearing;

    /***
     * Distance to active MISSION in meters
     */
    public double    wp_dist;
    /***
     * Current altitude error in meters
     */
    public double  alt_error;



    public AndruavMessage_NAV_INFO() {
        super();
        messageTypeID = TYPE_AndruavMessage_NAV_INFO;
    }

    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        nav_roll        = json_receive_data.getDouble("a");
        nav_pitch       = json_receive_data.getDouble("b");
        nav_yaw         = json_receive_data.getDouble("y");
        target_bearing  = json_receive_data.getDouble("d");
        wp_dist         = json_receive_data.getDouble("e");
        alt_error       = json_receive_data.getDouble("f");

    }

    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("a", String.format(Locale.US, "%.4f", nav_roll).trim());
        json_data.accumulate("b", String.format(Locale.US, "%.4f", nav_pitch).trim());
        json_data.accumulate("y", String.format(Locale.US, "%.4f", nav_yaw).trim());
        json_data.accumulate("d", String.format(Locale.US, "%.4f", target_bearing).trim());
        json_data.accumulate("e", String.format(Locale.US, "%.1f", wp_dist).trim());
        json_data.accumulate("f", String.format(Locale.US, "%.1f", alt_error).trim());


        return json_data.toString();

    }
}
