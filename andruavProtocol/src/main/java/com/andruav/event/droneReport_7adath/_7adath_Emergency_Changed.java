package com.andruav.event.droneReport_7adath;

/**
 * Created by mhefny on 2/7/17.
 */

import com.andruav.andruavUnit.AndruavUnitBase;

/***
 * Triggered in Remote GCS ONLY Units to tell that Emergency Changed
 */
public class _7adath_Emergency_Changed {

    public AndruavUnitBase mAndruavUnitBase;

    public _7adath_Emergency_Changed(final AndruavUnitBase andruavUnitBase)
    {
        mAndruavUnitBase = andruavUnitBase;
    }
}
