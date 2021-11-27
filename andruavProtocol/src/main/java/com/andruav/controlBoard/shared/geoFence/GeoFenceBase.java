package com.andruav.controlBoard.shared.geoFence;

import androidx.collection.SimpleArrayMap;

import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.droneReport_Event.Event_GeoFence_Hit;

/**
 * Created by mhefny on 7/2/16.
 * Represents a base Fence.
 */
public class GeoFenceBase {

    protected static int          NOT_TESTED      = -1;
    protected static int          INZONE          = 1;
    protected static int          OUT_ZONE        = 0;


    public static final int ACTION_SOFT_FENCE = 0;
    public static final int ACTION_SHUT_DOWN  = 999;


    /***
     * Flying Mode Action {@link com.andruav.controlBoard.shared.common.FlightMode}
     * <br>This parameter is <b>optional</b>.
     */
    public int hardFenceAction = GeoFenceBase.ACTION_SOFT_FENCE;

    /***
     * Array of Andruav units that has ths fence attached to them.
     */
    public SimpleArrayMap<String, Event_GeoFence_Hit> mAndruavUnits  = new SimpleArrayMap<String, Event_GeoFence_Hit>();

    protected int _isInside = NOT_TESTED;

    /***
     * Distance in meters from the fence.
     * Useful in Linear Fence and equals to Zero for non linear.
     */
    public double distance = 0.0f;

    /***
     * if <b>true</b> then this area default should be out of it. i.e. <t>restricted</t>.
     * LinearFence value is false by default.
     */
    public boolean shouldKeepOutside = false;


    public  String fenceName = "default";



    public static double        MAX_DISTANCE    = 50;



    /***
     * max distance from path
     */
    public  double maxDistance = MAX_DISTANCE;


    public static boolean isSharableFence (final GeoFenceBase geoFenceBase )
    {
        return geoFenceBase.hardFenceAction != GeoFenceBase.ACTION_SHUT_DOWN;
    }

    public int size()
    {
        return 0;
    }


    public boolean inZone (final AndruavUnitBase andruavUnitBase)
    {
        final Event_GeoFence_Hit a7adath_geoFence_hit = mAndruavUnits.get(andruavUnitBase.PartyID);

        if (a7adath_geoFence_hit ==null) return false;

        return (a7adath_geoFence_hit.hasValue  && a7adath_geoFence_hit.inZone);
    }

    protected void set_isInside(final AndruavUnitBase andruavUnitBase, final boolean inside, final double distance)
    {

    }


    /***
     *
     * @param lat
     * @param lng
     * @param fireEvent if <b>true</b> then fire event {@link Event_GeoFence_Hit} if there is change in state.
     * @return a distance that is either Double.NaN or less than {@link #maxDistance}
     */
    public double testPoint(final AndruavUnitBase andruavUnitBase, final double lat, final double lng, boolean fireEvent) {
        // Virtual function.
        return Double.NaN;
    }

    public boolean isViolatingGeoFence (final AndruavUnitBase andruavUnitBase)
    {

        Event_GeoFence_Hit a7adath_geoFence_hit = mAndruavUnits.get(andruavUnitBase.PartyID);

        if (a7adath_geoFence_hit == null) return false;

        return (a7adath_geoFence_hit.hasValue &&
                (
                        (a7adath_geoFence_hit.inZone && a7adath_geoFence_hit.shouldKeepOutside)
                    || (!a7adath_geoFence_hit.inZone && !a7adath_geoFence_hit.shouldKeepOutside)));


    }


    /***
     * Called by a GCS or another Drone to set status of a given Fence that is attached to a Drone that sent its status.
     * @param andruavUnitBase
     * @param a7adath_geoFence_hit
     */
    public void setisInsideRemote(final AndruavUnitBase andruavUnitBase, final Event_GeoFence_Hit a7adath_geoFence_hit)
    {
        if (andruavUnitBase != null)
        {
            if (andruavUnitBase.IsMe())
            {
                throw new IllegalAccessError();
            }
        }
        Event_GeoFence_Hit geoFence_hit = this.mAndruavUnits.get(andruavUnitBase.PartyID);
        if (geoFence_hit == null)
        {

            // in racing condition
            // GCS is open just after a HIT is sent and after a Drone-Fence subscribtion is sent.
            geoFence_hit = new Event_GeoFence_Hit(andruavUnitBase,this.fenceName);
            this.mAndruavUnits.put(andruavUnitBase.PartyID, geoFence_hit);

        }
        geoFence_hit.hasValue          = a7adath_geoFence_hit.hasValue;
        geoFence_hit.inZone            = a7adath_geoFence_hit.inZone;
        geoFence_hit.distance          = a7adath_geoFence_hit.distance;
        geoFence_hit.shouldKeepOutside = this.shouldKeepOutside;

    }

    /***
     * Called by a GCS or another Drone to set status of a given Fence that is attached to a Drone that sent Attach Report.
     * This is an internal function as it is linked with EventBus
     * @param andruavUnitBase
     */
    protected void setisInsideRemote(final AndruavUnitBase andruavUnitBase)
    {
        if (andruavUnitBase != null)
        {
            if (andruavUnitBase.IsMe())
            {
                throw new IllegalAccessError();
            }
        }
        Event_GeoFence_Hit geoFence_hit = this.mAndruavUnits.get(andruavUnitBase.PartyID);
        if (geoFence_hit == null)
        {

            // in racing condition
            // GCS is open just after a HIT is sent and after a Drone-Fence subscribtion is sent.
            geoFence_hit = new Event_GeoFence_Hit(andruavUnitBase,this.fenceName);
            this.mAndruavUnits.put(andruavUnitBase.PartyID, geoFence_hit);
            geoFence_hit.hasValue          = false;
            geoFence_hit.shouldKeepOutside = this.shouldKeepOutside;
        }
        //else if Already exist then DONT TOUCH keep the geoFence_hit intact.
    }


    protected void fireEvent (final Event_GeoFence_Hit a7adath_geoFence_hit)
    {
        AndruavEngine.getEventBus().post(a7adath_geoFence_hit);
    }

}
