package org.droidplanner.services.android.impl.core.helpers.geoTools;

import com.o3dr.services.android.lib.coordinate.LatLong;


public class GeoTools {
    private static final double RADIUS_OF_EARTH = 6378137.0;// In meters.
    // Source: WGS84
    public GeoTools() {
    }

    /**
     * Returns the distance between two points
     *
     * @return distance between the points in degrees
     */
    public static Double getAproximatedDistance(LatLong p1, LatLong p2) {
        return (Math.hypot((p1.getLatitude() - p2.getLatitude()), (p1.getLongitude() - p2.getLongitude())));
    }

    public static Double latToMeters(double lat) {
        return Math.toRadians(lat) * RADIUS_OF_EARTH;
    }

    /**
     * Extrapolate latitude/longitude given a heading and distance thanks to
     * http://www.movable-type.co.uk/scripts/latlong.html
     *
     * @param origin   Point of origin
     * @param bearing  bearing to navigate
     * @param distance distance to be added
     * @return New point with the added distance
     */
    /**
     * Extrapolate latitude/longitude given a heading and distance thanks to
     * http://www.movable-type.co.uk/scripts/latlong.html
     *
     * @param lat   latitude
     * @param lon   longitude
     * @param bearing  bearing to navigate
     * @param distance distance to be added
     * @return New point with the added distance
     */
    /**
     * Offset a coordinate by a local distance
     *
     * @param origin  location in WGS84
     * @param xMeters Offset distance in the east direction
     * @param yMeters Offset distance in the north direction
     * @return new coordinate with the offset
     */
    /**
     * Calculates the arc between two points
     * http://en.wikipedia.org/wiki/Haversine_formula
     *
     * @return the arc in degrees
     */
    static double getArcInRadians(LatLong from, LatLong to) {

        double latitudeArc = Math.toRadians(from.getLatitude() - to.getLatitude());
        double longitudeArc = Math.toRadians(from.getLongitude() - to.getLongitude());

        double latitudeH = Math.sin(latitudeArc * 0.5);
        latitudeH *= latitudeH;
        double lontitudeH = Math.sin(longitudeArc * 0.5);
        lontitudeH *= lontitudeH;

        double tmp = Math.cos(Math.toRadians(from.getLatitude()))
                * Math.cos(Math.toRadians(to.getLatitude()));
        return Math.toDegrees(2.0 * Math.asin(Math.sqrt(latitudeH + tmp * lontitudeH)));
    }

    /**
     * Computes the distance between two coordinates
     *
     * @return distance in meters
     */
    public static double getDistance(LatLong from, LatLong to) {
        return RADIUS_OF_EARTH * Math.toRadians(getArcInRadians(from, to));
    }

    /**
     * Computes the distance between two coordinates taking in account the
     * height difference
     *
     * @return distance in meters
     */
    /**
     * Computes the heading between two coordinates
     *
     * @return heading in degrees
     */
    public static double warpToPositiveAngle(double degree) {
        if (degree >= 0) {
            return degree;
        } else {
            return 360 + degree;
        }
    }

}
