package com.andruav.event.droneReport_Event;

import com.andruav.andruavUnit.AndruavUnitBase;


/**
 * used to announce recieving of Waypoints from a DRONE - could be me or Other Drone-
 * Created by mhefny on 2/20/16.
 */
public class Event_WayPointsRecieved {


    /***
     * Waypoints belongs to This Unit.
     * <br>Waypoints are already updated for this unot so you can get it from {@link AndruavUnitBase#getMohemmaMapBase}
     */
    public final AndruavUnitBase mAndruavWe7da;


    public Event_WayPointsRecieved(AndruavUnitBase andruavWe7da)
    {
        mAndruavWe7da = andruavWe7da;
    }

}
