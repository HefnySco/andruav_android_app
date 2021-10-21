package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by M.Hefny on 28-Oct-14.
 * <br>cmd: <b>1003</b>
 */
public class AndruavMessage_POW extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_POW = 1003;

    /***
     * batteryLevel
     */
    public double BatteryLevel;
    /***
     * voltage
     */
    public double Voltage;
    /***
     * batteryTemperature
     */
    public double BatteryTemperature;
    /***
     * C
     */
    public boolean Charging;
    /***
     * batteryTechnology
     */
    public String BatteryTechnology;
    /***
     * health
     */
    public String Health;
    /***
     * PlugStatus
     */
    public String PlugStatus;


    public boolean hasFCBPowerInfo = false;

    /***
     * FCB Battery Voltage
     */
    public double FCB_BatteryVoltage;

    /***
     * Current Consumed
     */
    public double FCB_BatteryCurrent;

    /***
     * Remaining battery charge in percentage
     */
    public double FCB_BatteryRemaining;


    public AndruavMessage_POW() {
        super();
        messageTypeID = TYPE_AndruavMessage_POW;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        BatteryLevel = json_receive_data.getDouble("BL");
        Voltage = json_receive_data.getDouble("V");
        BatteryTemperature = json_receive_data.getDouble("BT");

        if (json_receive_data.has("H")) {
            Health = json_receive_data.getString("H");
        } else {
            Health = "na";
        }
        if (json_receive_data.has("PS")) {
            PlugStatus = json_receive_data.getString("PS");
        } else {
            PlugStatus = "na";
        }

        Charging = PlugStatus.toLowerCase().equals("charging");

        if (json_receive_data.has("FV")) {
            hasFCBPowerInfo = true;
            FCB_BatteryVoltage = json_receive_data.getDouble("FV");
            FCB_BatteryCurrent = json_receive_data.getDouble("FI");
            FCB_BatteryRemaining = json_receive_data.getDouble("FR");
        }

    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("BL", BatteryLevel);
        json_data.accumulate("V", Voltage);
        json_data.accumulate("BT", BatteryTemperature);
        json_data.accumulate("H", Health);
        json_data.accumulate("PS", PlugStatus);

        // FCB STATUS
        if (hasFCBPowerInfo) {
            json_data.accumulate("FV", FCB_BatteryVoltage);
            json_data.accumulate("FI", FCB_BatteryCurrent);
            json_data.accumulate("FR", FCB_BatteryRemaining);
        }

        return json_data.toString();
    }


}
