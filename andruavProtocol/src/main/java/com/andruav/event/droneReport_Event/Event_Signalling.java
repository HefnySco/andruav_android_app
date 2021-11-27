package com.andruav.event.droneReport_Event;

import com.andruav.andruavUnit.AndruavUnitBase;

import org.json.JSONObject;

/**
 * Created by mhefny on 3/6/16.
 */
public class Event_Signalling {

    public JSONObject jsonObject;
    public AndruavUnitBase andruavUnitBase;


    public Event_Signalling(final JSONObject json, final AndruavUnitBase we7da)
    {
        andruavUnitBase = we7da;
        jsonObject = json;
    }

}
