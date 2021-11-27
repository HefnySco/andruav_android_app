package com.andruav.event.droneReport_Event;

import com.andruav.andruavUnit.AndruavUnitBase;

public class Event_SERVO_Outputs_Ready {

    public AndruavUnitBase mAndruavWe7da;

    public boolean          mValuesChanged;

    public Event_SERVO_Outputs_Ready(final AndruavUnitBase andruavUnit)
    {
        mAndruavWe7da = andruavUnit;
    }
}
