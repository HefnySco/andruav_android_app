package com.andruav.protocol.commands.textMessages;

/**
 * Created by mhefny on 7/29/16.
 */
public class AndruavMessage_ExternalCommand_WayPoints extends AndruavMessage_WayPoints {


    public final static int TYPE_AndruavResala_ExternalCommand_WayPoints = 1028;

    public AndruavMessage_ExternalCommand_WayPoints() {
        super();
        messageTypeID = TYPE_AndruavResala_ExternalCommand_WayPoints;
    }
}
