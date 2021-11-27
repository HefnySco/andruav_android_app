package com.andruav.event.droneReport_Event;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.controlBoard.shared.missions.MohemmaMapBase;


/**
 * Created by mhefny on 2/20/16.
 */
public class Event_WayPointsUpdated {

    /***
     * Waypoints belongs to This Unit.
     */
    AndruavUnitBase mAndruavWe7da;


    MohemmaMapBase mUpdatedWayPoints;


    public Event_WayPointsUpdated(AndruavUnitBase andruavWe7da, MohemmaMapBase mohemmaMapBase)
    {
        mAndruavWe7da = andruavWe7da;
        mUpdatedWayPoints = mohemmaMapBase;
    }

}
