package com.andruav.andruavUnit;

import com.andruav.Constants;

public class AndruavUnitAllVehicles extends AndruavUnitBase {

    public AndruavUnitAllVehicles()
    {
        super();
        this.PartyID = Constants._vehicle;
    }

    static String getPartyID ()
    {
        return Constants._vehicle;
    }
}