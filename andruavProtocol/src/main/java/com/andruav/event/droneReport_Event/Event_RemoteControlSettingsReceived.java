package com.andruav.event.droneReport_Event;

import com.andruav.andruavUnit.AndruavUnitBase;

/**
 * This class mainly used to instruct GUI control that there is an update in RC Settings
 * of the currently selected drone -this is to ease the settings and give better user experience
 * when setting remote control.
 * <br><i>In real flight I dont expect this will happen as no one will fly something while adjusting its remote.</i>
 * Created by mhefny on 5/5/16.
 */
public class Event_RemoteControlSettingsReceived {

    public final AndruavUnitBase mAndruavWe7da;


    public Event_RemoteControlSettingsReceived(AndruavUnitBase andruavUnit)
    {
        mAndruavWe7da = andruavUnit;
    }
}
