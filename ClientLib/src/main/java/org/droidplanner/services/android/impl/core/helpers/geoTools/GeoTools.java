package org.droidplanner.services.android.impl.core.helpers.geoTools;

import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.util.MathUtils;

import java.util.List;

public class GeoTools {
    private static final double RADIUS_OF_EARTH = 6378137.0;// In meters.
    // Source: WGS84
    public List<LatLong> waypoints;

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

    private static Double metersTolat(double meters) {
        return Math.toDegrees(meters / RADIUS_OF_EARTH);
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
    public static LatLong newCoordFromBearingAndDistance(LatLong origin, double bearing, double distance) {
        return newCoordFromBearingAndDistance(origin.getLatitude(), origin.getLongitude(), bearing, distance);
    }

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
    private static LatLong newCoordFromBearingAndDistance(double lat, double lon, double bearing, double distance) {

        double lat1 = Math.toRadians(lat);
        double lon1 = Math.toRadians(lon);
        double brng = Math.toRadians(bearing);
        double dr = distance / RADIUS_OF_EARTH;

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dr) + Math.cos(lat1) * Math.sin(dr)
                * Math.cos(brng));
        double lon2 = lon1
                + Math.atan2(Math.sin(brng) * Math.sin(dr) * Math.cos(lat1),
                Math.cos(dr) - Math.sin(lat1) * Math.sin(lat2));

        return (new LatLong(Math.toDegrees(lat2), Math.toDegrees(lon2)));
    }

    /**
     * Offset a coordinate by a local distance
     *
     * @param origin  location in WGS84
     * @param xMeters Offset distance in the east direction
     * @param yMeters Offset distance in the north direction
     * @return new coordinate with the offset
     */
    public static LatLong moveCoordinate(LatLong origin, double xMeters, double yMeters) {
        double lon = origin.getLongitude();
        double lat = origin.getLatitude();
        double lon1 = Math.toRadians(lon);
        double lat1 = Math.toRadians(lat);

        double lon2 = lon1 + Math.toRadians(metersTolat(xMeters));
        double lat2 = lat1 + Math.toRadians(metersTolat(yMeters));
        return (new LatLong(Math.toDegrees(lat2), Math.toDegrees(lon2)));
    }

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
    public static double get3DDistance(LatLongAlt end, LatLongAlt start) {
        double horizontalDistance = getDistance(end, start);
        double altitudeDiff = Math.abs((end.getAltitude() - start.getAltitude()));
        return MathUtils.hypot(horizontalDistance, altitudeDiff);
    }

    /**
     * Computes the heading between two coordinates
     *
     * @return heading in degrees
     */
    public static double getHeadingFromCoordinates(LatLong fromLoc, LatLong toLoc) {
        double fLat = Math.toRadians(fromLoc.getLatitude());
        double fLng = Math.toRadians(fromLoc.getLongitude());
        double tLat = Math.toRadians(toLoc.getLatitude());
        double tLng = Math.toRadians(toLoc.getLongitude());

        double degree = Math.toDegrees(Math.atan2(
                Math.sin(tLng - fLng) * Math.cos(tLat),
                Math.cos(fLat) * Math.sin(tLat) - Math.sin(fLat) * Math.cos(tLat)
                        * Math.cos(tLng - fLng)));

        return warpToPositiveAngle(degree);
    }

    public static double warpToPositiveAngle(double degree) {
        if (degree >= 0) {
            return degree;
        } else {
            return 360 + degree;
        }
    }

    public static LatLong pointAlongTheLine(LatLong start, LatLong end, int distance) {
        return newCoordFromBearingAndDistance(start, getHeadingFromCoordinates(start, end), distance);
    }
}
