package com.andruav.util;

import android.location.Location;

import com.andruav.AndruavEngine;

import static com.andruav.util.Maths.d2r;
import static com.andruav.util.Maths.r2d;


/**
 * Created by M.Hefny on 12-Jul-15.
 */
public class GPSHelper {


    private static final double RADIUS_OF_EARTH_IN_METERS = 6371e3;  // Source: WGS84



    /**
     * Returns distance in meters between two points.
     * @param NewLocation
     * @param OldLocation
     * @return in meters
     */
    public static double calculateDistance (Location NewLocation, Location OldLocation) {

        return calculateDistance(NewLocation.getLongitude(), NewLocation.getLatitude(), OldLocation.getLongitude(), OldLocation.getLatitude());

    }

    /**
     * Returns distance in meters between two points.
     * @param NewLocation
     * @param OldLocation
     * @return in meters
     */
    public static double calculateDistance (AndruavLatLng NewLocation, AndruavLatLng OldLocation) {

        return calculateDistance(NewLocation.getLongitude(), NewLocation.getLatitude(), OldLocation.getLongitude(), OldLocation.getLatitude());

    }

    /**
     * Extrapolate latitude/longitude given a heading and distance thanks to
     * http://www.movable-type.co.uk/scripts/AndruavLatLng.html
     *
     * @param origin    Point of origin
     * @param bearing   bearing to navigate
     * @param distance  distance in meters to be added
     * @return          new point with the added distance
     */
    public static AndruavLatLng getCoordFromBearingAndDistance(final AndruavLatLng origin, final double bearing,
                                                               final double distance) {
        double lat = origin.getLatitude();
        double lon = origin.getLongitude();
        double lat1 = Math.toRadians(lat);
        double lon1 = Math.toRadians(lon);
        double brng = Math.toRadians(bearing);
        double dr = distance / RADIUS_OF_EARTH_IN_METERS;

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dr) + Math.cos(lat1) * Math.sin(dr)
                * Math.cos(brng));
        double lng2 = lon1
                + Math.atan2(Math.sin(brng) * Math.sin(dr) * Math.cos(lat1),
                Math.cos(dr) - Math.sin(lat1) * Math.sin(lat2));

        return (new AndruavLatLng(Math.toDegrees(lat2), Math.toDegrees(lng2)));
    }


    /***
     * Get teh vector that points at a location from another location.
     * @see "AP_Mount::calc_GPS_target_angle(const struct Location *target) in Ardupilot code"
     * @param origin
     * @param target
     * @return
     */
    public static AndruavTiltPanRoll getVectorPointingAtCoordinate (final AndruavLatLngAlt origin, final AndruavLatLngAlt target)
    {


        final double GPS_vector_x = (target.getLongitude()-origin.getLongitude())
                *Math.cos((origin.getLatitude()+ target.getLatitude()) * d2r * 0.00000005f)
                *0.01113195f;
        final double GPS_vector_y = (target.getLatitude() - origin.getLatitude())*0.01113195f;
        //final double GPS_vector_z = (target.getAltitude() - origin.getAltitude());

        final double distance_in_2D = calculateDistance(target,origin);
        final double altitude_diff = target.getAltitude() - origin.getAltitude();

        //final double target_distance = calculateDistance3D(target,origin);
        final AndruavTiltPanRoll andruavTiltPanRoll = new AndruavTiltPanRoll();


        andruavTiltPanRoll.Roll =0;
        andruavTiltPanRoll.Tilt = Math.atan2(altitude_diff,distance_in_2D) * r2d;
        andruavTiltPanRoll.Pan = Math.atan2(GPS_vector_x,GPS_vector_y) * r2d;

        return andruavTiltPanRoll;
    }

    public static double calculateDistance3D (AndruavLatLngAlt NewLocation, AndruavLatLngAlt OldLocation)
    {
        final double distance_in_2D = calculateDistance(NewLocation,OldLocation);
        final double altitude_diff = NewLocation.getAltitude() - OldLocation.getAltitude();
        final double target_distance = Math.sqrt(distance_in_2D * distance_in_2D  + (altitude_diff * altitude_diff));

        return target_distance;
    }


    /***
     * * FOR SIMPLICITY ROLL IS ZERO
     * Get Location of an object given observer location and looking direction.
     * @param andruavLatLngAlt
     * @param bearing in degrees
     * @param tilt    in degrees  (-ve is down)
     * @param targetAlt target object altitude in meters
     * @return
     */
    public static AndruavLatLngAlt getCoordFromVectorAndDistance ( final AndruavLatLngAlt andruavLatLngAlt, final double bearing, final double tilt, final double targetAlt)
    {
        final double diff = Math.abs(andruavLatLngAlt.getAltitude()- targetAlt);
        final double distance =  diff / Math.sin(d2r * (180 - tilt)) ; //sine law of trianle .... the 180-tilt here is 180 - (90 + tilt.vertial) and since title is the complementry of tilt.vertical I removed the 90
        final double distance3d = Math.sqrt((distance * distance) - (diff * diff));

        final AndruavLatLng andruavLatLng = getCoordFromBearingAndDistance(andruavLatLngAlt,bearing,distance3d);
        final AndruavLatLngAlt andruavLatLngAltNew = new AndruavLatLngAlt(andruavLatLng,targetAlt);

        return  andruavLatLngAltNew;
    }



    /***
     *
     * @param NewLocation_lng
     * @param NewLocation_lat
     * @param OldLocation_lng
     * @param OldLocation_lat
     * @return distance in meters
     */
    public static double calculateDistance  (final double  NewLocation_lng,
                                             final double  NewLocation_lat,
                                             final double  OldLocation_lng,
                                             final double  OldLocation_lat) {
        try
        {

            final double  R = 6371e3; // metres
            final double  ??1 = OldLocation_lat * d2r;
            final double  ??2 = NewLocation_lat * d2r;
            final double  ???? = (NewLocation_lat-OldLocation_lat) * d2r;
            final double  ???? = (NewLocation_lng-OldLocation_lng) * d2r;

            final double  a = Math.sin(????/2) * Math.sin(????/2) +
                    Math.cos(??1) * Math.cos(??2) *
                            Math.sin(????/2) * Math.sin(????/2);
            final double  c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

            final double  distance = R * c;
            return distance;

        }
        catch (Exception e)
        {
            return -1; // bad data
        }

    }


    /**
     * Returns speed in meters between two points.
     * @param NewLocation
     * @param CurrentLocation
     * @return
     */
    public static double calculateSpeed (Location NewLocation, Location CurrentLocation) {
        try
        {
            //double curLng = CurrentLocation.getLongitude();
            //double newLng = NewLocation.getLongitude();
            //double curLat = CurrentLocation.getLatitude();
            //double newLat = NewLocation.getLatitude();

            final double distance = calculateDistance (NewLocation,CurrentLocation);
            final double TimeDelta = NewLocation.getTime() - CurrentLocation.getTime();	// in milliseconds.

            // Log.d("GPS-speed", String.format("%3.1f", TimeDelta));
            return (distance * 1000 /TimeDelta);	// m/s
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException("calculateSpeed", ex);
            return 0;
        }

    }

    /***
     *
     * //http://stackoverflow.com/questions/9457988/bearing-from-one-coordinate-to-another
     * @param lon1
     * @param lat1
     * @param lon2
     * @param lat2
     * @return heading in degrees
     */
    public static double calculateBearing(double lon1, double lat1, double lon2, double lat2)
    {
        double longitude1 = lon1;
        double longitude2 = lon2;
        double latitude1 = Math.toRadians(lat1);
        double latitude2 = Math.toRadians(lat2);
        double longDiff= Math.toRadians(longitude2-longitude1);
        double y= Math.sin(longDiff)*Math.cos(latitude2);
        double x=Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);
        //return Math.atan2(y, x);
        return (Math.toDegrees(Math.atan2(y, x))+360)%360;
    }


    public static AndruavLatLng calculareIntersection (double lon1, double lat1, double bearing1,
                                                double lon2, double lat2, double bearing2)
    {
        double ??1 = Math.toRadians(lat1), ??1 = Math.toRadians(lon1);
        double ??2 = Math.toRadians(lat2), ??2 = Math.toRadians(lon2);
        double ??13 = Math.toRadians(bearing1), ??23 = Math.toRadians(bearing2);
        double ???? = ??2-??1, ???? = ??2-??1;

        double ??12 = 2*Math.asin( Math.sqrt( Math.sin(????/2)*Math.sin(????/2)
                + Math.cos(??1)*Math.cos(??2)*Math.sin(????/2)*Math.sin(????/2) ) );
        if (??12 == 0) return null;

        // initial/final bearings between points
        double ??a = Math.acos( ( Math.sin(??2) - Math.sin(??1)*Math.cos(??12) ) / ( Math.sin(??12)*Math.cos(??1) ) );
        if (Double.isNaN(??a)) ??a = 0; // protect against rounding
        double ??b = Math.acos( ( Math.sin(??1) - Math.sin(??2)*Math.cos(??12) ) / ( Math.sin(??12)*Math.cos(??2) ) );

        double ??12 = Math.sin(??2-??1)>0 ? ??a : 2*Math.PI-??a;
        double ??21 = Math.sin(??2-??1)>0 ? 2*Math.PI-??b : ??b;

        double ??1 = (??13 - ??12 + Math.PI) % (2*Math.PI) - Math.PI; // angle 2-1-3
        double ??2 = (??21 - ??23 + Math.PI) % (2*Math.PI) - Math.PI; // angle 1-2-3

        if (Math.sin(??1)==0 && Math.sin(??2)==0) return null; // infinite intersections
        if (Math.sin(??1)*Math.sin(??2) < 0) return null;      // ambiguous intersection

        //??1 = Math.abs(??1);
        //??2 = Math.abs(??2);
        // ... Ed Williams takes abs of ??1/??2, but seems to break calculation?

        double ??3 = Math.acos( -Math.cos(??1)*Math.cos(??2) + Math.sin(??1)*Math.sin(??2)*Math.cos(??12) );
        double ??13 = Math.atan2( Math.sin(??12)*Math.sin(??1)*Math.sin(??2), Math.cos(??2)+Math.cos(??1)*Math.cos(??3) );
        double ??3 = Math.asin( Math.sin(??1)*Math.cos(??13) + Math.cos(??1)*Math.sin(??13)*Math.cos(??13) );
        double ????13 = Math.atan2( Math.sin(??13)*Math.sin(??13)*Math.cos(??1), Math.cos(??13)-Math.sin(??1)*Math.sin(??3) );
        double ??3 = ??1 + ????13;

        ??3 = ??3 * r2d;
        ??3 = (((??3 * r2d) +540)%360-180); // normalise to ???180..+180??
        return new AndruavLatLng(??3,??3);
    }



}

