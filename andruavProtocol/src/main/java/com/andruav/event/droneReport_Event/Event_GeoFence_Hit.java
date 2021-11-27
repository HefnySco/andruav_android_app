package com.andruav.event.droneReport_Event;


import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.controlBoard.shared.geoFence.GeoFenceManager;

/**
 * Created by mhefny on 6/19/16.
 */
public class Event_GeoFence_Hit {

    public AndruavUnitBase andruavUnitBase;
    public String fenceName;
    public boolean inZone = false;
    public double  distance;
    public boolean shouldKeepOutside;

    /***
     * Has a valid value. because inZone is initialized false. You need to know wither it is false by initialization or because of a valid value.
     *
     * used when liking with {@link GeoFenceManager}
     */
    public boolean hasValue = false;


    public Event_GeoFence_Hit(final AndruavUnitBase andruavUnitBase, final String fenceName)
    {
        this.andruavUnitBase = andruavUnitBase;
        this.fenceName = fenceName;
    }

    public Event_GeoFence_Hit(AndruavUnitBase andruavUnitBase, String fenceName, boolean inZone, double distance, boolean shouldKeepOutside)
    {
        this.andruavUnitBase = andruavUnitBase;
        this.fenceName = fenceName;
        this.inZone = inZone;
        this.distance = distance;
        this.shouldKeepOutside = shouldKeepOutside;
        this.hasValue = true;
    }


    public boolean isViolating ()
    {
        return  (hasValue && inZone && shouldKeepOutside) ||   (hasValue && !inZone && !shouldKeepOutside);
    }


}
