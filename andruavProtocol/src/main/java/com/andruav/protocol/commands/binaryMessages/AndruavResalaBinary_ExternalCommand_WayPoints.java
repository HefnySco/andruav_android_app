package com.andruav.protocol.commands.binaryMessages;

/**
 * Created by mhefny on 3/29/16.
 */
@Deprecated
public class AndruavResalaBinary_ExternalCommand_WayPoints extends AndruavResalaBinary_WayPoints {

        public final static int TYPE_AndruavResalaBinary_ExternalCommand_WayPoints = 2020;

    public AndruavResalaBinary_ExternalCommand_WayPoints() {
        super();
        messageTypeID = TYPE_AndruavResalaBinary_ExternalCommand_WayPoints;
    }

}
