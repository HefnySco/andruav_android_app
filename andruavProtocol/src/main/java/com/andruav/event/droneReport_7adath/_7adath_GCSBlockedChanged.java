package com.andruav.event.droneReport_7adath;

import com.andruav.andruavUnit.AndruavUnitBase;

/**
 * Called when GCS blocked of a unit
 * Created by mhefny on 12/30/16.
 */

public class _7adath_GCSBlockedChanged {

    public AndruavUnitBase andruavUnitBase;

    public _7adath_GCSBlockedChanged(final AndruavUnitBase unit)
    {
        andruavUnitBase = unit;
    }
}
