package com.andruav.protocol.commands.textMessages.FlightControl;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Created by mhefny on 9/26/16.
 */
public class AndruavMessage_CirclePoint extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_CirclePoint = 1034;

    public double Latitude;
    public double Longitude;
    public double Altitude;
    public double Radius;
    public int Turns;


    public AndruavMessage_CirclePoint() {
        super();
        messageTypeID = TYPE_AndruavMessage_CirclePoint;
    }

    @Override
    public void setMessageText(String messageText) throws JSONException, ParseException {
        final JSONObject json_receive_data = new JSONObject(messageText);
        final NumberFormat nf = NumberFormat.getInstance(Locale.US);

        Latitude     =  nf.parse(json_receive_data.getString("a")).doubleValue();
        Longitude    =  nf.parse(json_receive_data.getString("g")).doubleValue();
        Altitude    =  nf.parse(json_receive_data.getString("l")).doubleValue();
        Radius       =  nf.parse(json_receive_data.getString("r")).doubleValue();
        Turns       =  nf.parse(json_receive_data.getString("t")).intValue();

    }

    @Override
    public String getJsonMessage() throws org.json.JSONException {
        final JSONObject json_data = new JSONObject();

        json_data.accumulate("a",String.format(Locale.US, "%4.6f", Latitude));
        json_data.accumulate("g",String.format(Locale.US, "%4.6f", Longitude));
        json_data.accumulate("l",String.format(Locale.US, "%4.6f", Altitude));
        json_data.accumulate("r",String.format(Locale.US, "%4.6f", Radius));
        json_data.accumulate("t",Turns);


        return json_data.toString();
    }

}
