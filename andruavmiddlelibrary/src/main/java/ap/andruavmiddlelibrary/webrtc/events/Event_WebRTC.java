package ap.andruavmiddlelibrary.webrtc.events;

import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitBase;

/**
 * Created by mhefny on 10/28/16.
 */

public class Event_WebRTC {
    public static final int EVENT_CONNECTION_REQUEST = 1;
    public static final int EVENT_CONNECT_SUCCEEDED = 2;
    public static final int EVENT_CONNECTION_ERROR = 4;
    public static final int EVENT_CLOSED_CONNECTION              = 5;


    protected final int eventType ;

    public int getEventType() {
        return eventType;
    }

    /***
     * Andruav Drone
     */
    protected final AndruavUnitBase andruavUnitBase;

    public AndruavUnitBase getAndruavWe7daBase() {
        return andruavUnitBase;
    }

    public Event_WebRTC(final String partyID, final String channel, final int eventType)
    {
        andruavUnitBase = AndruavEngine.getAndruavWe7daMapBase().get(partyID);
        andruavUnitBase.VideoStreamingChannel = channel;
        this.eventType = eventType; // could add this property to the UNIT
    }

    public Event_WebRTC(final AndruavUnitBase andruavUnitBase, final int eventType)
    {
        this.andruavUnitBase = andruavUnitBase;
        this.eventType = eventType;
    }
}
