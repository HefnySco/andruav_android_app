package com.andruav.andruavUnit;

import com.andruav.Constants;

public class AndruavUnitAllGCS extends AndruavUnitBase {

    public AndruavUnitAllGCS()
    {
        super();
        this.PartyID = Constants._gcs_;
    }

    public static String getPartyID ()
    {
        return Constants._gcs_;
    }
}
