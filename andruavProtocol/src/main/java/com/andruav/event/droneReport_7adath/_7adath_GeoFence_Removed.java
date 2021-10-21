package com.andruav.event.droneReport_7adath;

import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_RemoteExecute;

/**
 * Used to internally signal that a fence has been deleted, this is mainly as a result of {@link AndruavMessage_RemoteExecute#RemoteCommand_CLEAR_FENCE_DATA}
 * <br>Also could be a result of an internal decision by the unit.
 *
 * Created by mhefny on 8/17/16.
 */
public class _7adath_GeoFence_Removed {

    public String fenceName;


    /***
     *
     *
     * @param fenceName null means all fences
     */
    public _7adath_GeoFence_Removed(String fenceName)
    {
        this.fenceName = fenceName;
    }
}
