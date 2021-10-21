package com.andruav.util;

import android.location.Location;

/**
 * Created by mhefny on 4/3/16.
 */
public class AndruavLatLngAlt extends  AndruavLatLng{

    /**
     * Stores the maxAltitude in meters.
     */
    private double mAltitude;

    public AndruavLatLngAlt(double latitude, double longitude, double altitude) {
        super(latitude, longitude);
        mAltitude = altitude;
    }

    public AndruavLatLngAlt(Location location){
        super(location.getLatitude(),location.getLongitude());
        mAltitude = location.getAltitude();
    }

    public AndruavLatLngAlt(AndruavLatLng location, double altitude){
        super(location);
        mAltitude = altitude;
    }

    public AndruavLatLngAlt(AndruavLatLngAlt copy) {
        this(copy.getLatitude(), copy.getLongitude(), copy.getAltitude());
    }

    public void set(AndruavLatLngAlt source){
        super.set(source);
        this.mAltitude = source.mAltitude;
    }

    public void update (final double longitude, final double latitude, final double altitude )
    {
        this.setAltitude(altitude);
        this.setLatitude(latitude);
        this.setLongitude(longitude);
    }

    /**
     * @return the maxAltitude in meters
     */
    public double getAltitude() {
        return mAltitude;
    }

    public void setAltitude(double altitude) {
        this.mAltitude = altitude;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AndruavLatLngAlt)) return false;
        if (!super.equals(o)) return false;

        AndruavLatLngAlt that = (AndruavLatLngAlt) o;

        return Double.compare(that.mAltitude, mAltitude) == 0;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(mAltitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        final String superToString = super.toString();
        return "LatLongAlt{" +
                superToString +
                ", mAltitude=" + mAltitude +
                '}';
    }

}
