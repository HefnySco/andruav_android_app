package com.andruav.event.fcb_event;

import com.MAVLink.Messages.MAVLinkMessage;
import com.andruav.andruavUnit.AndruavUnitBase;



/**
 * Created by M.Hefny on 25-Nov-14.
 */
public class Event_SocketData {

    /***
     * means that the source of the data is the FCB board. or the 3rd Party Mission Planner.that I am connected to.
     * <br><b>Andruav GCS</b>
     * <br><i>source:</i> 3rd Party GCS e.g. Mission Planner
     * <br><i>target:</i> this message should be forwarded to Drone Andruav.
     *
     * <br><b>Andruav Drone</b>
     * <br><i>source:</i> FCB e.g. Multiwii or APM board
     * <br><i>target:</i> this message should be forwarded to Drone GCS..
     */
    public static final int SOURCE_LOCAL = 1;
    /***
     * means that the source of the data comes from a remote Andruav. could be a GCS Andruav or Drone Andruav
     * <br><b>Andruav GCS</b>
     * <br><i>source:</i> Websocket Server. this message is comming from remote Andruav.
     * <br><i>target:</i> this message should be forwarded any connected GCS such as Mission Planner. it could be parsed to retrieve way points for example stored waypoints and display it on Andruav map.
     *
     * <br><b>Andruav Drone</b>
     * <br><i>source:</i> Websocket Server. this message is comming from remote Andruav.
     * <br><i>target:</i> this message should be forwarded to any connected FCB. it also could be parsed and modified before forwarding it. e.g. "requested waypoints could be modified based on dynamic geo fencing"
     */

    public static final int SOURCE_REMOTE = 2;
    /***
     * means the source of data is Andruav and it should be forwarded to the current connected FCB or Mission Planner.
     * <br><b>Andruav Drone</b>
     * <br><i>source:</i> Andruav Drone wants to talk or control its own FCB.
     * <br><i>target:</i> local attached FCB only. e.g. retrieve waypoints. start mission.
     */
    public static final int SOURCE_SIMULATED = 3;


    /***
     * This message has to be delivered even if BlockingGCM exist.
     */
   public final boolean byPassBlockedGCS = false;

    public byte[]Data;
    public MAVLinkMessage mavLinkMessage;
    public int DataLength;
   // public String PartyID;
    public AndruavUnitBase senderWe7da;
    public String targetName;
    public AndruavUnitBase targetWe7da;
    public int IsLocal;

    public Event_SocketData()
    {

    }


    public Event_SocketData(final byte[] attrsMap, final int btnIcon, final int abcIcClearSearchResult)
    {
        Data = attrsMap;
        DataLength = btnIcon;
        IsLocal = abcIcClearSearchResult;
    }
}
