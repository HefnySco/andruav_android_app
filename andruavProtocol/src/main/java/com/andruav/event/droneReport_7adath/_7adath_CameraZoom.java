package com.andruav.event.droneReport_7adath;

import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraZoom;

public class _7adath_CameraZoom {

    /**
     * Zomm In/Out
     */
    public boolean ZoomIn;

    /**
     * Value: 0..1
     */
    public Double ZoomValue = Double.MAX_VALUE;

    /**
     * Step to increase or decrease
     */
    public Double ZoomValueStep = Double.MAX_VALUE;



    public _7adath_CameraZoom (final AndruavMessage_CameraZoom andruavMessage_cameraZoom)
    {
        ZoomIn = andruavMessage_cameraZoom.ZoomIn;
        ZoomValue = andruavMessage_cameraZoom.ZoomValue;
        ZoomValueStep = andruavMessage_cameraZoom.ZoomValueStep;
    }

}
