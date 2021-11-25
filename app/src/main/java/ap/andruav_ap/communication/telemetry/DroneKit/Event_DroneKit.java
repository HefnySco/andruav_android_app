package ap.andruav_ap.communication.telemetry.DroneKit;

import com.mavlink.messages.MAVLinkMessage;

/**
 * Created by mhefny on 1/20/16.
 */
public class Event_DroneKit {
    public MAVLinkMessage Data;
    public String senderName;
    public String targetName;

    /***
     * Local means com from a local BT module not from Andruav_2MR
     */
    public int IsLocal;


    public Event_DroneKit ()
    {

    }

    public Event_DroneKit (final MAVLinkMessage data,  final int isLocal)
    {
        Data = data;
        IsLocal = isLocal;
    }
}
