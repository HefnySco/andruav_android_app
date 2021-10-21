package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mhefny on 10/22/16.
 */

public class AndruavMessage_SensorsStatus extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_SensorsStatus = 1053;


    // FCB
    public int hasGyro   ;
    public int hasAcc    ;
    public int hasBaro   ;
    public int hasMag    ;
    public int hasGPS    ;
    public int hasSonar  ;

    public int hasGyroEnabled    ;
    public int hasAccEnabled     ;
    public int hasBaroEnabled    ;
    public int hasMagEnabled     ;
    public int hasGPSEnabled     ;
    public int hasSonarEnabled   ;


    public int hasGyroHealthy    ;
    public int hasAccHealthy     ;
    public int hasBaroHealthy    ;
    public int hasMagHealthy     ;
    public int hasGPSHealthy     ;
    public int hasSonarHealthy   ;

    

    public AndruavMessage_SensorsStatus() {
        super();
        messageTypeID = TYPE_AndruavMessage_SensorsStatus;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        hasGyro = json_receive_data.getInt("a");
        hasAcc = json_receive_data.getInt("b");
        hasBaro = json_receive_data.getInt("c");
        hasMag = json_receive_data.getInt("d");
        hasGPS = json_receive_data.getInt("e");
        hasSonar = json_receive_data.getInt("f");

        hasGyroEnabled = json_receive_data.getInt("g");
        hasAccEnabled = json_receive_data.getInt("h");
        hasBaroEnabled = json_receive_data.getInt("i");
        hasMagEnabled = json_receive_data.getInt("j");
        hasGPSEnabled = json_receive_data.getInt("k");
        hasSonarEnabled = json_receive_data.getInt("l");

        hasGyroHealthy = json_receive_data.getInt("m");
        hasAccHealthy = json_receive_data.getInt("n");
        hasBaroHealthy = json_receive_data.getInt("o");
        hasMagHealthy = json_receive_data.getInt("p");
        hasGPSHealthy = json_receive_data.getInt("q");
        hasSonarHealthy = json_receive_data.getInt("r");
    }



    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();

        json_data.accumulate("a", hasGyro);
        json_data.accumulate("b", hasAcc);
        json_data.accumulate("c", hasBaro);
        json_data.accumulate("d", hasMag);
        json_data.accumulate("e", hasGPS);
        json_data.accumulate("f", hasSonar);

        json_data.accumulate("g", hasGyroEnabled);
        json_data.accumulate("h", hasAccEnabled);
        json_data.accumulate("i", hasBaroEnabled);
        json_data.accumulate("j", hasMagEnabled);
        json_data.accumulate("k", hasGPSEnabled);
        json_data.accumulate("l", hasSonarEnabled);

        json_data.accumulate("m", hasGyroHealthy);
        json_data.accumulate("n", hasAccHealthy);
        json_data.accumulate("o", hasBaroHealthy);
        json_data.accumulate("p", hasMagHealthy);
        json_data.accumulate("q", hasGPSHealthy);
        json_data.accumulate("r", hasSonarHealthy);

        return json_data.toString();
    }

}
