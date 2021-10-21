package com.andruav.protocol.commands.textMessages.FlightControl;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Created by mhefny on 8/26/16.
 */
public class AndruavMessage_GuidedPoint extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_GuidedPoint = 1033;

    public double Latitude;
    public double Longitude;
    public double Altitude;
    public double xVelocity =-1.0; // ignore
    public double yVelocity;
    public double zVelocity;
    public double Yaw =-1.0; // ignore
    public double YawRate;


    public AndruavMessage_GuidedPoint() {
        super();
        messageTypeID = TYPE_AndruavMessage_GuidedPoint;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException, ParseException {
        final JSONObject json_receive_data = new JSONObject(messageText);
        final NumberFormat nf = NumberFormat.getInstance(Locale.US);

        Latitude       =  nf.parse(json_receive_data.getString("a")).doubleValue();
        Longitude      =  nf.parse(json_receive_data.getString("g")).doubleValue();
        if (json_receive_data.has("l")) {
            Altitude       =  nf.parse(json_receive_data.getString("l")).doubleValue();
        }
        else
        {
            Altitude = -1.0;
        }
        if (json_receive_data.has("x")) {
            xVelocity = nf.parse(json_receive_data.getString("x")).doubleValue();
            yVelocity = nf.parse(json_receive_data.getString("y")).doubleValue();
            zVelocity = nf.parse(json_receive_data.getString("z")).doubleValue();
        }

        if (json_receive_data.has("w")) {
            Yaw     = nf.parse(json_receive_data.getString("w")).doubleValue();
            YawRate = nf.parse(json_receive_data.getString("r")).doubleValue();
        }



    }

    /***
     * You can fill the data using direct throttle variable of using setData
     * that is why variables are used to fill data so it is valid all time.
     *
     * @return
     * @throws org.json.JSONException
     */
    @Override
    public String getJsonMessage() throws org.json.JSONException {
        final JSONObject json_data = new JSONObject();
        json_data.accumulate("a",String.format(Locale.US, "%4.6f", Latitude));
        json_data.accumulate("g",String.format(Locale.US, "%4.6f", Longitude));
        json_data.accumulate("l",String.format(Locale.US, "%4.6f", Altitude));

        if (xVelocity!= -1) {
            json_data.accumulate("x", String.format(Locale.US, "%4.6f", xVelocity));
            json_data.accumulate("y", String.format(Locale.US, "%4.6f", yVelocity));
            json_data.accumulate("z", String.format(Locale.US, "%4.6f", zVelocity));
        }

        if (Yaw != -1.0) {
            json_data.accumulate("w", String.format(Locale.US, "%4.6f", Yaw));
            json_data.accumulate("r", String.format(Locale.US, "%4.6f", YawRate));
        }

            return json_data.toString();
    }


}

