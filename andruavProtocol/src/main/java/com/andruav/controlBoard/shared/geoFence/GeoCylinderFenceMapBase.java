package com.andruav.controlBoard.shared.geoFence;

import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.droneReport_Event.Event_GeoFence_Hit;
import com.andruav.util.GPSHelper;

/**
 * Created by mhefny on 7/1/16.
 */
public class GeoCylinderFenceMapBase extends  GeoFenceBase{

    public GeoCylinderFenceMapBase()
    {
        shouldKeepOutside = false;
    }

    protected GeoFencePointNodeCylinder geoFence;

    /***
     * Altitude from ground level
     *
     */
    public double maxAltitude = 0.0f;

    public void setGeoFence (GeoFencePointNodeCylinder geoFence)
    {
        if (geoFence == null) throw new IllegalArgumentException("nul Geo Fence");
        this.geoFence = geoFence;
    }

    public GeoFencePointNodeCylinder getGeoFence ()
    {
        return geoFence;
    }


    @Override
    public int size()
    {
        return 1;
    }


    /***
     * sendMessageToModule Announcement {@link Event_GeoFence_Hit} when:
     * <br>1- a toggle in state happpened.
     * <br>2- if old state was {@link #NOT_TESTED} and the new one is {@link #INZONE} if {@link #shouldKeepOutside} is <b>true</b> because this is a violation.
     * <br>3- if old state was {@link #NOT_TESTED} and the new one is {@link #INZONE} or {@link #OUT_ZONE} if {@link #shouldKeepOutside} is <b>false</b> because you need to know if you obey or disobey the rules.
     * <br> <b>DONT IGNORE THIS CASE:</b> if old state was {@link #NOT_TESTED} and the new one is {@link #OUT_ZONE} if {@link #shouldKeepOutside} is <b>true</b> because this is a normal case that you start without violation, aslo there might be hundreads of restricted area.
     * @param inside
     */
    @Override
    protected void set_isInside(final AndruavUnitBase andruavUnitBase, final boolean inside, final double distance)
    {
        final Event_GeoFence_Hit a7adath_geoFence_hit = this.mAndruavUnits.get(andruavUnitBase.PartyID);
        if (a7adath_geoFence_hit == null) return ; //Should never happen

        a7adath_geoFence_hit.distance         = distance;
        a7adath_geoFence_hit.shouldKeepOutside = this.shouldKeepOutside;

        if ((!a7adath_geoFence_hit.hasValue) && (this.shouldKeepOutside && !inside))
        {
            // not has value, and it out already and should be out then dont mention it.
            if (this.shouldKeepOutside && !inside) {
                return ;
            }
        }


        if ((!a7adath_geoFence_hit.hasValue) || (a7adath_geoFence_hit.inZone != inside)) // as inZoone is by default false
        {
            // Value Toggle
            a7adath_geoFence_hit.hasValue = true;

            a7adath_geoFence_hit.inZone           = inside;

            AndruavEngine.getEventBus().post(a7adath_geoFence_hit);
        }
    }



    /***
     * @see "https://github.com/sromku/polygon-contains-point"
     * @param lat
     * @param lng
     * @param fireEvent if <b>true</b> then fire event {@link Event_GeoFence_Hit} if there is change in state.
     * @return a distance that is either Double.NaN or less than {@link #maxDistance}
     */
    @Override
    public double testPoint(final AndruavUnitBase andruavUnitBase, final double lat, final double lng, boolean fireEvent)  {

        if (!mAndruavUnits.containsKey(andruavUnitBase.PartyID)) return Double.NaN;



        double distance = GPSHelper.calculateDistance(lat,lng,geoFence.Latitude,geoFence.Longitude);
        if (distance > maxDistance) // this is a hit
        {
            distance = Double.NaN;
        }


        if (andruavUnitBase.IsMe() && fireEvent)
        {
            set_isInside(andruavUnitBase,distance <= maxDistance, distance);
        }

        return distance;
    }
}
