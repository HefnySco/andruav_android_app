package com.andruav.controlBoard.shared.geoFence;

/**
 * Created by mhefny on 6/17/16.
 */
public class GeoFencePoint {



    public  double  Latitude;
    public  double  Longitude;

    public GeoFencePoint(double lng, double lat)
    {
        this.Latitude = lat;
        this.Longitude = lng;
    }

    public double getHash ()
    {
        return Latitude + Longitude ; // dont add Sequence
    }

}
