package com.andruav.controlBoard.shared.geoFence;

import androidx.collection.SimpleArrayMap;

/**
 * A generic class for Fence array to allow generic handling of all types of fences.
 * It is used for fences that consists of multiple units such as a polygon or waypoints.
 * Created by mhefny on 6/22/16.
 */
public class GeoFenceCompositBase extends GeoFenceBase {

    protected SimpleArrayMap<String,GeoFencePoint> mGeoFenceArray = new SimpleArrayMap<>();





    @Override
    public int size()
    {
        return mGeoFenceArray.size();
    }







    public GeoFencePoint valueAt(final int index) {
        return mGeoFenceArray.valueAt(index);
    }


    public GeoFencePoint Put(String key, GeoFencePoint value) {
        return mGeoFenceArray.put(key, value);

    }
}
