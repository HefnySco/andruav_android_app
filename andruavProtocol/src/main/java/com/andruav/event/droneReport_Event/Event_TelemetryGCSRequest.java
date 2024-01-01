package com.andruav.event.droneReport_Event;

import com.andruav.andruavUnit.AndruavUnitBase;

/**
 * A GCS Wants Telemetry Data
 * Created by mhefny on 12/14/16.
 */

public class Event_TelemetryGCSRequest {


    public static final int REQUEST_START    = 1;
    public static final int REQUEST_END      = 2;
    /**
     * Add unit to telemetry and adjust rate
     */
    public static final int REQUEST_RESUME   = 3;
    /**
     * Adjust Rate only. Useful in UDPProxy, but will affect rate on all connected gcs.
     */
    public static final int ADJUST_RATE      = 4;
    /**
        Stop sending telemetry data via UDP without closing socket.
     */
    public static final int REQUEST_PAUSE   = 5;

    public AndruavUnitBase andruavUnitBase;
    public int Request;

    public Event_TelemetryGCSRequest(final AndruavUnitBase andruavUnitBase, final  int add)
    {
        this.andruavUnitBase = andruavUnitBase;
        this.Request = add;
    }
}
