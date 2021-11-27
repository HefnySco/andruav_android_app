package com.andruav.event.droneReport_Event;

import com.andruav.andruavUnit.AndruavUnitBase;



/**
 * Created by M.Hefny on 12-May-15.
 */
public class Event_IMU_Ready {

    public AndruavUnitBase mAndruavWe7da;


    public Event_IMU_Ready(final AndruavUnitBase andruavUnit)
    {
        mAndruavWe7da = andruavUnit;
    }
}
