package com.andruav.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mhefny on 4/3/16.
 */
public class GPSMapHelper {
    private static final double RADIUS_OF_EARTH_IN_METERS = 6378137.0;  // Source: WGS84

    public static final int SIGNAL_MAX_FADE_MARGIN = 50;
    public static final int SIGNAL_MIN_FADE_MARGIN = 6;

    /**
     * Computes the distance between two points taking into consideration maxAltitude.
     * @param from  start lat/long position
     * @param to    end lat/long position
     * @return      distance between positions in meters.
     */
    public static double getDistance3D(AndruavLatLngAlt from, AndruavLatLngAlt to) {
        if (from == null || to == null) {
            return -1;
        }

        final double distance2d = getDistance2D(from, to);
        double distanceSqr = Math.pow(distance2d, 2);
        double altitudeSqr = Math.pow(to.getAltitude() - from.getAltitude(), 2);

        return Math.sqrt(altitudeSqr + distanceSqr);
    }

    /**
     * Computes the distance between two points without considering maxAltitude.
     * @param from  start lat/long position
     * @param to    end lat/long position
     * @return      distance between positions in meters.
     */
    public static double getDistance2D(AndruavLatLng from, AndruavLatLng to) {
        if (from == null || to == null) {
            return -1;
        }

        return RADIUS_OF_EARTH_IN_METERS * Math.toRadians(getArcInRadians(from, to));
    }

    /**
     * Compute a new Lat/Long point (without maxAltitude) given specified changes along latitude and
     * longitude.
     * @param from      start lat/long position
     * @param xMeters   longitude change in meters
     * @param yMeters   latitude change in meters
     * @return          new lat/long position.
     */
    public static AndruavLatLng addDistance(AndruavLatLng from, double xMeters, double yMeters) {
        double lat = from.getLatitude();
        double lon = from.getLongitude();

        // Coordinate offsets in radians
        double dLat = yMeters / RADIUS_OF_EARTH_IN_METERS;
        double dLon = xMeters / (RADIUS_OF_EARTH_IN_METERS * Math.cos(Math.PI * lat / 180));

        // OffsetPosition, decimal degrees
        double latO = lat + dLat * 180 / Math.PI;
        double lonO = lon + dLon * 180 / Math.PI;

        return new AndruavLatLng(latO, lonO);
    }

    /**
     * Calculates the arc between two points (http://en.wikipedia.org/wiki/Haversine_formula).
     * @param from  start lat/long position
     * @param to    stop lat/long position
     * @return      the arc in degrees
     */
    public static double getArcInRadians(AndruavLatLng from, AndruavLatLng to) {
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
     * Signal strength in percentage.
     * @param fadeMargin    TODO
     * @param remFadeMargin TODO
     * @return percentage   TODO
     */
    public static int getSignalStrength(double fadeMargin, double remFadeMargin) {
        return (int) (normalize(Math.min(fadeMargin, remFadeMargin),
                SIGNAL_MIN_FADE_MARGIN, SIGNAL_MAX_FADE_MARGIN) * 100);
    }

    /**
     * TODO
     * @param value TODO
     * @param min   TODO
     * @param max   TODO
     * @return      TODO
     */
    public static double normalize(double value, double min, double max) {
        value = constrain(value, min, max);
        return (value - min) / (max - min);

    }

    private static double constrain(double value, double min, double max) {
        value = Math.max(value, min);
        value = Math.min(value, max);
        return value;
    }

    /**
     * Compute the difference between two angles.
     * @param a     Minuend angle in degrees
     * @param b     Subtrahend angle in degrees.
     * @return      Difference between the angles in degrees
     */
    public static double angleDiff(double a, double b) {
        double diff = Math.IEEEremainder(b - a + 180, 360);
        if (diff < 0)
            diff += 360;
        return diff - 180;
    }

    /**
     * TODO
     * @param x TODO
     * @return  TODO
     */
    public static double constrainAngle(double x) {
        x = Math.IEEEremainder(x, 360);
        if (x < 0)
            x += 360;
        return x;
    }

    /**
     * TODO
     * @param a     TODO
     * @param b     TODO
     * @param alpha TODO
     * @return      TODO
     */
    public static double bisectAngle(double a, double b, double alpha) {
        return constrainAngle(a + angleDiff(a, b) * alpha);
    }

    /**
     * TODO
     * @param altDelta  TODO
     * @param distDelta TODO
     * @return          TODO
     */
    public static double hypot(double altDelta, double distDelta) {
        return Math.hypot(altDelta, distDelta);
    }

    /**
     * Create a rotation matrix given some euler angles this is based on
     * http://gentlenav.googlecode.com/files/EulerAngles.pdf
     * @param roll  vehicle roll in degrees
     * @param pitch vehicle pitch in degrees
     * @param yaw   vehicle yaw in degrees
     * @return      Rotation matrix
     */
    public static double[][] dcmFromEuler(double roll, double pitch, double yaw) {
        double[][] dcm = new double[3][3];

        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        double sr = Math.sin(roll);
        double cr = Math.cos(roll);
        double sy = Math.sin(yaw);
        double cy = Math.cos(yaw);

        dcm[0][0] = cp * cy;
        dcm[1][0] = (sr * sp * cy) - (cr * sy);
        dcm[2][0] = (cr * sp * cy) + (sr * sy);
        dcm[0][1] = cp * sy;
        dcm[1][1] = (sr * sp * sy) + (cr * cy);
        dcm[2][1] = (cr * sp * sy) - (sr * cy);
        dcm[0][2] = -sp;
        dcm[1][2] = sr * cp;
        dcm[2][2] = cr * cp;

        return dcm;
    }

    /**
     * Based on the Ramer–Douglas–Peucker algorithm
     * http://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm
     * @param list      List of lat/long points in the curve.
     * @param epsilon   Tolerance for determining list of points for approximation of curve.
     * @return          List of lat/long points in the approximated curve.
     */
    public static List<AndruavLatLng> simplify(List<AndruavLatLng> list, double epsilon) {
        int index = 0;
        double dmax = 0;
        int lastIndex = list.size() - 1;

        // Find the point with the maximum distance.
        for (int i = 1; i < lastIndex; i++) {
            double d = pointToLineDistance(list.get(0), list.get(lastIndex), list.get(i));
            if (d > dmax) {
                index = i;
                dmax = d;
            }
        }

        // If max distance is greater than epsilon, recursively simplify.
        List<AndruavLatLng> ResultList = new ArrayList<AndruavLatLng>();
        if (dmax > epsilon) {
            // Recursive call.
            List<AndruavLatLng> recResults1 = simplify(list.subList(0, index + 1), epsilon);
            List<AndruavLatLng> recResults2 = simplify(list.subList(index, lastIndex + 1), epsilon);

            // Build the result list.
            recResults1.remove(recResults1.size() - 1);
            ResultList.addAll(recResults1);
            ResultList.addAll(recResults2);
        } else {
            ResultList.add(list.get(0));
            ResultList.add(list.get(lastIndex));
        }

        return ResultList;
    }

    /**
     * Provides the distance from a point P to the line segment that passes
     * through A-B. If the point is not on the side of the line, returns the
     * distance to the closest point
     *
     * @param L1    First point of the line
     * @param L2    Second point of the line
     * @param P     Point to measure the distance
     * @return      distance between point and line in meters.
     */
    public static double pointToLineDistance(AndruavLatLng L1, AndruavLatLng L2, AndruavLatLng P) {
        double A = P.getLatitude() - L1.getLatitude();
        double B = P.getLongitude() - L1.getLongitude();
        double C = L2.getLatitude() - L1.getLatitude();
        double D = L2.getLongitude() - L1.getLongitude();

        double dot = A * C + B * D;
        double len_sq = C * C + D * D;
        double param = dot / len_sq;

        double xx, yy;

        if (param < 0) // point behind the segment
        {
            xx = L1.getLatitude();
            yy = L1.getLongitude();
        } else if (param > 1) // point after the segment
        {
            xx = L2.getLatitude();
            yy = L2.getLongitude();
        } else { // point on the side of the segment
            xx = L1.getLatitude() + param * C;
            yy = L1.getLongitude() + param * D;
        }

        return Math.hypot(xx - P.getLatitude(), yy - P.getLongitude());
    }

    /**
     * This class contains functions used to generate a spline path.
     */
    public static class SplinePath {
        /**
         * Used as tag for logging.
         */
        private static final String TAG = SplinePath.class.getSimpleName();

        private final static int SPLINE_DECIMATION = 20;

        /**
         * Process the given map coordinates, and return a set of coordinates
         * describing the spline path.
         *
         * @param points map coordinates decimation factor
         * @return set of coordinates describing the spline path
         */
        public static List<AndruavLatLng> process(List<AndruavLatLng> points) {
            final int pointsCount = points.size();
            if (pointsCount < 4) {
                System.err.println("Not enough points!");
                return points;
            }

            final List<AndruavLatLng> results = processPath(points);
            results.add(0, points.get(0));
            results.add(points.get(pointsCount - 1));
            return results;
        }

        private static List<AndruavLatLng> processPath(List<AndruavLatLng> points) {
            final List<AndruavLatLng> results = new ArrayList<AndruavLatLng>();
            for (int i = 3; i < points.size(); i++) {
                results.addAll(processPathSegment(points.get(i - 3), points.get(i - 2),
                        points.get(i - 1), points.get(i)));
            }
            return results;
        }

        private static List<AndruavLatLng> processPathSegment(AndruavLatLng l1, AndruavLatLng l2, AndruavLatLng l3, AndruavLatLng l4) {
            Spline spline = new Spline(l1, l2, l3, l4);
            return spline.generateCoordinates(SPLINE_DECIMATION);
        }

    }

    public static class Spline {
        private static final float SPLINE_TENSION = 1.6f;

        private final AndruavLatLng p0;
        private final AndruavLatLng p0_prime;
        private final AndruavLatLng a;
        private final AndruavLatLng b;

        public Spline(AndruavLatLng pMinus1, AndruavLatLng p0, AndruavLatLng p1, AndruavLatLng p2) {
            this.p0 = p0;

            // derivative at a point is based on difference of previous and next
            // points
            p0_prime = p1.subtract(pMinus1).dot(1 / SPLINE_TENSION);
            AndruavLatLng p1_prime = p2.subtract(this.p0).dot(1 / SPLINE_TENSION);

            // compute a and b coords used in spline formula
            a = AndruavLatLng.sum(this.p0.dot(2), p1.dot(-2), p0_prime, p1_prime);
            b = AndruavLatLng.sum(this.p0.dot(-3), p1.dot(3), p0_prime.dot(-2), p1_prime.negate());
        }

        public List<AndruavLatLng> generateCoordinates(int decimation) {
            ArrayList<AndruavLatLng> result = new ArrayList<AndruavLatLng>();
            float step = 1f / decimation;
            for (float i = 0; i < 1; i += step) {
                result.add(evaluate(i));
            }

            return result;
        }

        private AndruavLatLng evaluate(float t) {
            float tSquared = t * t;
            float tCubed = tSquared * t;

            return AndruavLatLng.sum(a.dot(tCubed), b.dot(tSquared), p0_prime.dot(t), p0);
        }
    }





    /**
     * Compute total length of the polyline in meters.
     *
     * @param gridPoints    list of lat/long points for the polyline.
     * @return              length of the polyline in meters.
     */
    public static double getPolylineLength(List<AndruavLatLng> gridPoints) {
        double length = 0;
        for (int i = 1; i < gridPoints.size(); i++) {
            final AndruavLatLng to = gridPoints.get(i - 1);
            if (to == null) {
                continue;
            }

            length += getDistance2D(gridPoints.get(i), to);
        }
        return length;
    }
}

