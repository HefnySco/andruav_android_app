package ap.andruav_ap.communication.telemetry.BlueTooth;



import com.mavlink.MAVLinkPacket;
import com.andruav.andruavUnit.AndruavUnitBase;


import com.andruav.event.fcb_event.Event_SocketData;

/**
 * Created by M.Hefny on 25-Nov-14.
 */
public class Event_FCBData {
    public byte[]Data;
    public MAVLinkPacket mavLinkPacket;
    public int DataLength;
   // public String PartyID;
    public AndruavUnitBase senderWe7da;
    public String targetName;

    /***
     * Local means com from a local BT module not from Andruav_2MR
     * @see {@link Event_SocketData#SOURCE_LOCAL}
     */
    public int IsLocal;


    public Event_FCBData()
    {

    }

    public Event_FCBData(final byte[] data, final int dataLength, final int isLocal)
    {
        Data = data;
        DataLength = dataLength;
        IsLocal = isLocal;
    }
}
