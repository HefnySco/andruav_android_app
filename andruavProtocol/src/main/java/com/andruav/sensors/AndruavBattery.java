package com.andruav.sensors;

/**
 * Created by M.Hefny on 11-May-15.
 */
public class AndruavBattery {

    /***
     * batteryLevel
     */
    public double BatteryLevel;
    /***
     * voltage
     * in mV
     */
    public double Voltage;
    /***
     * batteryTemperature
     * in degrees
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


    public double FCB_BatteryVoltage;

    /***
     * Current Consumed
     */
    public double   FCB_CurrentConsumed;

    /***
     * Remaining battery charge in percentage
     */
    public double FCB_BatteryRemaining;

}
