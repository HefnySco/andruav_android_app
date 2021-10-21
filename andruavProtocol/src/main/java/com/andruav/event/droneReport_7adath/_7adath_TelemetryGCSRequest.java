package com.andruav.event.droneReport_7adath;

import com.andruav.andruavUnit.AndruavUnitBase;

/**
 * A GCS Wants Telemetry Data
 * Created by mhefny on 12/14/16.
 */

public class _7adath_TelemetryGCSRequest {


    public static final int REQUEST_START    = 1;
    public static final int REQUEST_END      = 2;
    public static final int REQUEST_RESUME   = 3;
    public AndruavUnitBase andruavUnitBase;
    public int Request;

    public _7adath_TelemetryGCSRequest(final AndruavUnitBase andruavUnitBase, final  int add)
    {
        this.andruavUnitBase = andruavUnitBase;
        this.Request = add;
    }
}
