package ap.andruav_ap.activities.fcb.drone;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListItem_BluetoothUnit that = (ListItem_BluetoothUnit) o;
        return Objects.equals(deviceMAC, that.deviceMAC);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceMAC);
    }
}
