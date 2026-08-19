package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

public class AndruavMessage_CommSignalsStatus extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_CommSignalsStatus = 1059;


    public int signalType; // GSM of other drone
    public int signalLevel; // GSM of other drone
    public String operatorName; // carrier name from TelephonyManager.getNetworkOperatorName()
    public String countryIso;   // country code from TelephonyManager.getNetworkCountryIso()
    public int dataState;       // 0=disconnected, 1=connected, 2=roaming


    public AndruavMessage_CommSignalsStatus() {
        super();
        messageTypeID = TYPE_AndruavMessage_CommSignalsStatus;
    }
    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        if (json_receive_data.has("r"))  signalLevel = json_receive_data.getInt("r");
        if (json_receive_data.has("s"))  signalType  = json_receive_data.getInt("s");
        if (json_receive_data.has("op")) operatorName = json_receive_data.getString("op");
        if (json_receive_data.has("c"))  countryIso   = json_receive_data.getString("c");
        if (json_receive_data.has("ds")) dataState    = json_receive_data.getInt("ds");

    }

    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("r",signalLevel);
        json_data.accumulate("s",signalType);
        if (operatorName != null) json_data.accumulate("op", operatorName);
        if (countryIso != null)   json_data.accumulate("c", countryIso);
        json_data.accumulate("ds", dataState);


        return json_data.toString();
    }
}
