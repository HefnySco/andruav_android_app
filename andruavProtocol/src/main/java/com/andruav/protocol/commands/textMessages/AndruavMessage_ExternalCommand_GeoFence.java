package com.andruav.protocol.commands.textMessages;

/**
 *
 * used to update Unit with required GeoFencePoint
 * Created by mhefny on 6/18/16.
 *
 *
 */
public class AndruavMessage_ExternalCommand_GeoFence extends AndruavMessage_GeoFence {

    public final static int TYPE_AndruavMessage_ExternalGeoFence = 1024;

    public AndruavMessage_ExternalCommand_GeoFence() {
        super();
        messageTypeID = TYPE_AndruavMessage_ExternalGeoFence;
    }

}
