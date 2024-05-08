package com.andruav.event.droneReport_Event;

import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraZoom;

public class Event_CameraZoom {

    /**
     * Zomm In/Out
     */
    public final boolean ZoomIn;

    /**
     * Value: 0..1
     */
    public final Double ZoomValue;

    /**
     * Step to increase or decrease
     */
    public final Double ZoomValueStep;



    public Event_CameraZoom(final AndruavMessage_CameraZoom andruavMessage_cameraZoom)
    {
        ZoomIn = andruavMessage_cameraZoom.ZoomIn;
        ZoomValue = andruavMessage_cameraZoom.ZoomValue;
        ZoomValueStep = andruavMessage_cameraZoom.ZoomValueStep;
    }

}
