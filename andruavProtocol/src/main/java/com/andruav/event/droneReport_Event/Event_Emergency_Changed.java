package com.andruav.event.droneReport_Event;

/*
  Created by mhefny on 2/7/17.
 */

import com.andruav.andruavUnit.AndruavUnitBase;

/***
 * Triggered in Remote GCS ONLY Units to tell that Emergency Changed
 */
public class Event_Emergency_Changed {

    public AndruavUnitBase mAndruavUnitBase;

    public Event_Emergency_Changed(final AndruavUnitBase andruavUnitBase)
    {
        mAndruavUnitBase = andruavUnitBase;
    }
}
