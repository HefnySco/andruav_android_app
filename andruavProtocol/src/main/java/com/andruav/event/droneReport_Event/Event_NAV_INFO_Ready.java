package com.andruav.event.droneReport_Event;

/*
  Created by mhefny on 11/28/16.
 */

import com.andruav.andruavUnit.AndruavUnitBase;

/***
 * triggered by drone to sendMessageToModule Nav info
 * recieved mainly by Websocket to broadcast it.
 */
public class Event_NAV_INFO_Ready {

    public AndruavUnitBase mAndruavWe7da;


    public Event_NAV_INFO_Ready(final AndruavUnitBase andruavUnit)
    {
        mAndruavWe7da = andruavUnit;
    }
}
