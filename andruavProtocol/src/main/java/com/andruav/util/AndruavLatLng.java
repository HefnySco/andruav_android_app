package com.andruav.util;

/**
 * Created by mhefny on 4/3/16.
 */
public class AndruavLatLng {

    private double latitude;
    private double longitude;

    public AndruavLatLng(double latitude, double longitude){
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public AndruavLatLng(AndruavLatLng copy){
        this(copy.getLatitude(), copy.getLongitude());
    }

    public void set(AndruavLatLng update){
        this.latitude = update.latitude;
        this.longitude = update.longitude;
    }

    /**
     * @return the latitude in degrees
     */
    public double getLatitude(){
        return latitude;
    }

    /**
     * @return the longitude in degrees
     */
    public double getLongitude(){
        return longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public AndruavLatLng dot(double scalar) {
        return new AndruavLatLng(latitude * scalar, longitude * scalar);
    }

    public AndruavLatLng negate() {
        return new AndruavLatLng(latitude * -1, longitude * -1);
    }

    public AndruavLatLng subtract(AndruavLatLng coord) {
        return new AndruavLatLng(latitude - coord.latitude, longitude - coord.longitude);
    }

    public AndruavLatLng sum(AndruavLatLng coord) {
        return new AndruavLatLng(latitude + coord.latitude, longitude + coord.longitude);
    }

    public static AndruavLatLng sum(AndruavLatLng... toBeAdded) {
        double latitude = 0;
        double longitude = 0;
        for (AndruavLatLng coord : toBeAdded) {
            latitude += coord.latitude;
            longitude += coord.longitude;
        }
        return new AndruavLatLng(latitude, longitude);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AndruavLatLng)) return false;

        AndruavLatLng AndruavLatLng = (AndruavLatLng) o;

        if (Double.compare(AndruavLatLng.latitude, latitude) != 0) return false;
        return Double.compare(AndruavLatLng.longitude, longitude) == 0;

    }

    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(latitude);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
    
    public String toString() {
        return "AndruavLatLng{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    
}
