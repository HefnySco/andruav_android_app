package com.andruav.event.droneReport_Event;

import com.andruav.andruavUnit.AndruavUnitBase;

/**
 * Created by mhefny on 12/19/16.
 */

public class Event_UnitShutDown {


    public AndruavUnitBase andruavUnitBase;

    public Event_UnitShutDown(final AndruavUnitBase unit)
    {
        andruavUnitBase = unit;
    }
}
