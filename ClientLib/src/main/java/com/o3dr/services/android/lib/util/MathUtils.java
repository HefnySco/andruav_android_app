package com.o3dr.services.android.lib.util;

import com.o3dr.services.android.lib.coordinate.LatLong;

/**
 * Utility functions for math.
 */
public class MathUtils {

    private static final double RADIUS_OF_EARTH_IN_METERS = 6378137.0;  // Source: WGS84

    public static final int SIGNAL_MAX_FADE_MARGIN = 50;
    public static final int SIGNAL_MIN_FADE_MARGIN = 6;

    private static double constrain(double value, double min, double max) {
        value = Math.max(value, min);
        value = Math.min(value, max);
        return value;
    }

    public static double normalize(double value, double min, double max) {
        value = constrain(value, min, max);
        return (value - min) / (max - min);

    }

    public static int getSignalStrength(double fadeMargin, double remFadeMargin) {
        return (int) (MathUtils.normalize(Math.min(fadeMargin, remFadeMargin),
            SIGNAL_MIN_FADE_MARGIN, SIGNAL_MAX_FADE_MARGIN) * 100);
    }

    public static double getDistance2D(LatLong from, LatLong to) {
        if (from == null || to == null) {
            return -1;
        }

        return RADIUS_OF_EARTH_IN_METERS * Math.toRadians(getArcInRadians(from, to));
    }

    public static double getArcInRadians(LatLong from, LatLong to) {
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

    public static double getHeadingFromCoordinates(LatLong fromLoc, LatLong toLoc) {
        double fLat = Math.toRadians(fromLoc.getLatitude());
        double fLng = Math.toRadians(fromLoc.getLongitude());
        double tLat = Math.toRadians(toLoc.getLatitude());
        double tLng = Math.toRadians(toLoc.getLongitude());

        double degree = Math.toDegrees(Math.atan2(
            Math.sin(tLng - fLng) * Math.cos(tLat),
            Math.cos(fLat) * Math.sin(tLat) - Math.sin(fLat) * Math.cos(tLat)
                * Math.cos(tLng - fLng)));

        if (degree >= 0) {
            return degree;
        } else {
            return 360 + degree;
        }
    }
}
