package com.andruav.event.droneReport_7adath;

/**
 * Created by mhefny on 11/28/16.
 */

import com.andruav.andruavUnit.AndruavUnitBase;

/***
 * triggered by drone to sendMessageToModule Nav info
 * recieved mainly by Websocket to broadcast it.
 */
public class _7adath_NAV_INFO_Ready {

    public AndruavUnitBase mAndruavWe7da;


    public _7adath_NAV_INFO_Ready(final AndruavUnitBase andruavUnit)
    {
        mAndruavWe7da = andruavUnit;
    }
}
