package rcmobile.andruavmiddlelibrary.sensors.power;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import com.andruav.AndruavEngine;

/**
 * Created by M.Hefny on 18-Oct-14.
 */
public class BatterySensor extends BroadcastReceiver{


    //////// Attributes
    public int batteryLevel;
    public double voltage;
    public double batteryTemperature;
    public boolean isCharging;
    public String batteryTechnology;
    public int health;
    public int plugStatus;
    ////// EOF Sensor Variables


    @Override
    public void onReceive(Context arg0, Intent intent) {

        try {
            batteryLevel = intent.getIntExtra("level", 0);
            voltage = intent.getIntExtra("voltage", 0); // divide over 100 to get actual Voltage
            batteryTemperature = intent.getIntExtra("temperature", 0); // divide over 10 to get actual temp in celsius
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            // update battery level.
            batteryTechnology = intent.getExtras().getString(BatteryManager.EXTRA_TECHNOLOGY);
            health = intent.getIntExtra("health", 0);
            plugStatus = intent.getIntExtra("plugged", 0);

            if (batteryLevel < 31) { // connect charger
            } else {  // no need
            }
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException("bat", ex);

        }
    }



    public String getHealthString() {
        String healthString = "Unknown";
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_DEAD:
                healthString = "Dead";
                break;
            case BatteryManager.BATTERY_HEALTH_GOOD:
                healthString = "Good Condition";
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                healthString = "Over Voltage";
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                healthString = "Over Heat";
                break;
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                healthString = "Failure";
                break;
            default:
                // unknown .... new android version maybe
                break;
        }
        return healthString;
    }

    public String getStatusString() {
        String statusString = "Unknown";

        switch (plugStatus) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                statusString = "Charging";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                statusString = "Discharging";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                statusString = "Full";
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                statusString = "Not Charging";
                break;
            default:
                // unknown .... Solar Cell :)))
                break;
        }
        return statusString;
    }


}
