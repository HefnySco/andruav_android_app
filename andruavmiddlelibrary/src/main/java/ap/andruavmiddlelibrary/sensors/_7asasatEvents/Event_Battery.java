package ap.andruavmiddlelibrary.sensors._7asasatEvents;

import ap.andruavmiddlelibrary.sensors.power.BatterySensor;

/**
 * Created by M.Hefny on 19-Oct-14.
 */
public class Event_Battery {
    /***
     * batteryLevel
     */
    public int BatteryLevel;
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
    public Boolean Charging;
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


    public Event_Battery() {

    }

    public Event_Battery(BatterySensor val3)
    {
        BatteryTechnology = val3.batteryTechnology;
        BatteryTemperature = val3.batteryTemperature;
        Voltage = val3.voltage;
        BatteryLevel = val3.batteryLevel;
        Charging = val3.isCharging;
        Health = val3.getHealthString();
        PlugStatus = val3.getStatusString();
    }
}
