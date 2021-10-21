package rcmobile.FPV.activities.fcb.drone;

/**
 * Created by mhefny on 1/22/16.
 */
public class ListItem_BluetoothUnit {

    String deviceName;
    String deviceMAC;

    public void setDeviceName (String name)
    {
        deviceName = name;
    }

    public String getDeviceName ()
    {
        return deviceName;
    }

    public void setDeviceMAC (String name)
    {
        deviceMAC = name;
    }

    public String getDeviceMAC ()
    {
        return deviceMAC;
    }

}
