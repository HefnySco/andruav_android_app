package com.andruav.event.droneReport_Event;

import com.andruav.andruavUnit.AndruavUnitBase;

/**
 * Called when GCS blocked of a unit
 * Created by mhefny on 12/30/16.
 */

public class Event_GCSBlockedChanged {

    public final AndruavUnitBase andruavUnitBase;

    public Event_GCSBlockedChanged(final AndruavUnitBase unit)
    {
        andruavUnitBase = unit;
    }
}
