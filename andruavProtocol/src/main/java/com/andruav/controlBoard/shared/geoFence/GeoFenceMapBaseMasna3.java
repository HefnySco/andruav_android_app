package com.andruav.controlBoard.shared.geoFence;

/**
 * Created by mhefny on 6/23/16.
 */
public class GeoFenceMapBaseMasna3 {


    public final static int LinearFence         = 1;
    public final static int PolygonFence        = 2;
    public final static int CylindersFence      = 3;


    public static GeoFenceBase createGeoFenceMapBase (int type)
    {
        switch (type)
        {
            case LinearFence:
                return  new GeoLinearFenceCompositBase();


            case PolygonFence:
                return  new GeoPolygonFenceCompositBase();

            case CylindersFence:
                return  new GeoCylinderFenceMapBase();


            default:
                throw new IllegalArgumentException();

        }
    }
}
