package ap.andruavmiddlelibrary.sensors;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;


import com.andruav.AndruavEngine;
import com.andruav.util.GPSHelper;
import com.andruav.util.StringSplit;

import de.greenrobot.event.EventBus;
import ap.andruavmiddlelibrary.sensors._7asasatEvents.Event_GPS_NMEA;
import ap.andruavmiddlelibrary.sensors._7asasatEvents.Event_IMU_CMD;


/**
 * Created by M.Hefny on 05-Sep-14.
 */

public  class Sensor_GPS   extends GenericLocationSensor implements GpsStatus.Listener, LocationListener {



    @RequiresApi(api = Build.VERSION_CODES.N)
    private class OnNmeaMessageListenerLocal implements android.location.OnNmeaMessageListener {

        @Override
        public void onNmeaMessage(String nmea, long timestamp) {
            try {
                //Log.d("sensors.rcmobile.sensors","nmea rx: " + nmea);
                if (nmea == null) return;
                final NumberFormat nf_us = NumberFormat.getInstance(Locale.US);

                String[] nmeaCmd = StringSplit.fastSplit(nmea, ',');

                if (nmeaCmd[0].equals("$GPGSA")) {
                    if ((nmeaCmd.length > 2) && (nmeaCmd[2].length() != 0)) {
                        mFixLevel = Integer.parseInt(nmeaCmd[2]);
                    }
                    if ((nmeaCmd.length > 15)  && (nmeaCmd[15].length() != 0) ) {
                        Pdop = Float.parseFloat(nmeaCmd[15]);
                    }
//                if ((nmeaCmd.length > 16)  && (nmeaCmd[16].length() != 0)) {
//                    Hdop = Float.parseFloat(nmeaCmd[16]);
//                }
                    if ((nmeaCmd.length > 17)  && (nmeaCmd[17].length() != 0)) {
                        if (!nmeaCmd[17].startsWith("*")) {
                            Vdop = Float.parseFloat(nmeaCmd[17].split("\\*")[0]);
                        }
                    }
                } else if (nmeaCmd[0].equals("$GPGGA")) {
                    if ((nmeaCmd.length >= 6) && (nmeaCmd[6].length() != 0)) {
                        mFixQuality = Integer.parseInt(nmeaCmd[6]);
                    }
                    if ((nmeaCmd.length >= 11) && (nmeaCmd[9].length() != 0) && (nmeaCmd[11].length() != 0)) {
                        //Altitude here is $GPGGA.Altitude + $GPGGA.Height of geoid above WGS84 ellipsoid
                        altitude = Float.parseFloat(nmeaCmd[9]) + Float.parseFloat(nmeaCmd[11]);
                        altitude = updateAltitude(altitude); // reference to ground

                    }
                    if ((nmeaCmd.length > 8)  && (nmeaCmd[8].length() != 0)) {
                        Hdop = Float.parseFloat(nmeaCmd[8]);
                    }

                }


            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                // ignore
            }
            catch (Exception ex)
            {
                AndruavEngine.log().logException("gps", ex);
            }

            Event_GPS_NMEA event_gps_nmea = new Event_GPS_NMEA();
            event_gps_nmea.nmea = nmea;
            event_gps_nmea.timestamp = timestamp;
            EventBus.getDefault().post(event_gps_nmea);
        }
    }

    /////// Attributes
    private Object OnNmeaMessageListenerlocal;
    private static final int EXPIREY_TIME = 1000 * 60;
    protected static final double d2r = Math.PI / 180.0;
    public boolean misFirstFix;
    /**
     * http://manuals.deere.com/omview/OMPFP10334_19/JS56696,0000550_19_20090710.html
     * command: $GPGSA
     * 1 = nofix
     * 2 = 2Dfix
     * 3 = 3Dfix
     */
    public int mFixLevel;
    /*
    https://www.trimble.com/OEM_ReceiverHelp/V4.44/en/NMEA-0183messages_GGA.html
    GPS Quality indicator:
    0: Fix not valid
    1: GPS fix
    2: Differential GPS fix, OmniSTAR VBS
    4: Real-Time Kinematic, fixed integers
    5: Real-Time Kinematic, float integers, OmniSTAR XP/HP or Location RTK
     */
    public int mFixQuality;
    protected Iterable<GpsSatellite> msatellites;

    public String strGpsStats;
    public int intSatCount;
    /***
     * minimum time in ms to trigger
     */
    public static final long minimumTime = 200;
    /***
     * minimum distance in meter to trigger
     */
    public static final float minimumDistance = 0.0f;

   // public static float groundaltitude=0.0f;
    public static float altitude=0.0f;
    public static float altitudeMax=0.0f;
    public static float altitudeMin=0.0f;
    public static float Pdop;
    public static float Vdop;
    public static float Hdop;

    private double lastcalculatedspeed = 0.0f;

    public Sensor_GPS (LocationManager locationManager)
    {
        super(locationManager);
    }
    protected Location mLastGPSLocation;
    ///////////EOF Attributes


    protected float updateAltitude (float nmeaAltitude)
    {
        if (mFixLevel<2) return 0.0f ;// only in 2D or 3D;

        if ((altitudeMin > nmeaAltitude) || (altitudeMin ==0.0f)) altitudeMin = nmeaAltitude;

        if (altitudeMax < nmeaAltitude) altitudeMax = nmeaAltitude;

        return nmeaAltitude - altitudeMin;
    }

    @Override
    public boolean isSupported ()
    {
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

    }

//    /***
//     * Implements GpsStatus.NmeaListener
//     * @http "http://manuals.deere.com/omview/OMPFP10334_19/JS56696,0000550_19_20090710.html"
//     * @param timestamp
//     * @param nmea
//     */
//    @Override
//    public void onNmeaReceived(long timestamp, String nmea) {
//        try {
//            //Log.d("sensors.rcmobile.sensors","nmea rx: " + nmea);
//            if (nmea == null) return;
//            final NumberFormat nf_us = NumberFormat.getInstance(Locale.US);
//
//            String[] nmeaCmd = StringSplit.fastSplit(nmea, ',');
//
//            if (nmeaCmd[0].equals("$GPGSA")) {
//                if ((nmeaCmd.length > 2) && (nmeaCmd[2].length() != 0)) {
//                    mFixLevel = Integer.parseInt(nmeaCmd[2]);
//                }
//                if ((nmeaCmd.length > 15)  && (nmeaCmd[15].length() != 0) ) {
//                    Pdop = Float.parseFloat(nmeaCmd[15]);
//                }
////                if ((nmeaCmd.length > 16)  && (nmeaCmd[16].length() != 0)) {
////                    Hdop = Float.parseFloat(nmeaCmd[16]);
////                }
//                if ((nmeaCmd.length > 17)  && (nmeaCmd[17].length() != 0)) {
//                    if (!nmeaCmd[17].startsWith("*")) {
//                        Vdop = Float.parseFloat(nmeaCmd[17].split("\\*")[0]);
//                    }
//                }
//            } else if (nmeaCmd[0].equals("$GPGGA")) {
//                if ((nmeaCmd.length >= 6) && (nmeaCmd[6].length() != 0)) {
//                    mFixQuality = Integer.parseInt(nmeaCmd[6]);
//                }
//                if ((nmeaCmd.length >= 11) && (nmeaCmd[9].length() != 0) && (nmeaCmd[11].length() != 0)) {
//                    //Altitude here is $GPGGA.Altitude + $GPGGA.Height of geoid above WGS84 ellipsoid
//                    altitude = Float.parseFloat(nmeaCmd[9]) + Float.parseFloat(nmeaCmd[11]);
//                    altitude = updateAltitude(altitude); // reference to ground
//
//                }
//                if ((nmeaCmd.length > 8)  && (nmeaCmd[8].length() != 0)) {
//                    Hdop = Float.parseFloat(nmeaCmd[8]);
//                }
//
//            }
//
//
//        }
//        catch (ArrayIndexOutOfBoundsException e)
//        {
//            // ignore
//        }
//        catch (Exception ex)
//        {
//            AndruavMo7arek.log().logException("gps", ex);
//        }
//
//        Event_GPS_NMEA event_gps_nmea = new Event_GPS_NMEA();
//        event_gps_nmea.nmea = nmea;
//        event_gps_nmea.timestamp = timestamp;
//        EventBus.getDefault().post(event_gps_nmea);
//    }

    /***
     * Implements GpsStatus.Listener
     * @param event
     */
    @Override
    public void onGpsStatusChanged(int event) {

        int satcount=0;
        StringBuilder GpsStats=new StringBuilder();
        GpsStatus gpsStatus = null;
        if (ActivityCompat.checkSelfPermission(AndruavEngine.AppContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(AndruavEngine.AppContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        else
        {
            gpsStatus = mLocationManager.getGpsStatus(null);
        }
        if (gpsStatus != null) {
            msatellites = gpsStatus.getSatellites();
            Iterator<GpsSatellite> sat = msatellites.iterator();
            int i = 0;
            while (sat.hasNext()) {
                GpsSatellite satellite = sat.next();
                if (satellite.usedInFix())
                {
                    satcount+=1;
                }
                GpsStats.append(i++);
                GpsStats.append(": ");
                GpsStats.append(satellite.getPrn());
                GpsStats.append(",");
                GpsStats.append(satellite.usedInFix());
                GpsStats.append(",");
                GpsStats.append(satellite.getSnr());
                GpsStats.append(",");
                GpsStats.append(satellite.getAzimuth());
                GpsStats.append(",");
                GpsStats.append(satellite.getElevation());
                GpsStats.append("\n\n");
            }

            strGpsStats = GpsStats.toString();
            intSatCount = satcount;
         }

        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:

                //Log.d("GPS",   " has been GPS_EVENT_SATELLITE_STATUS");
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                misFirstFix = true;
                //Log.d("GPS",   " has been GPS_EVENT_FIRST_FIX");
                break;
            case GpsStatus.GPS_EVENT_STARTED:
                misFirstFix = false;
                //Log.d("GPS",   " has been GPS_EVENT_STARTED");
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                misFirstFix = false;
                //Log.d("GPS",   " has been GPS_EVENT_STOPPED");
                break;
            default:
                // new android veriosn ... maybe
                break;
        }
    }

    /***
     * Implements LocationListener
     * Called when a new location is found by the network location provider.
     * @param loc
     */
    @Override
    public void onLocationChanged(Location loc) {

        //if (loc.getProvider().equals(LocationManager.GPS_PROVIDER) ==false) return;

        loc = getBestLocation(loc);

        if (loc == null) return ;

        loc.setSpeed(0.0f);
        if (loc.hasAccuracy() && loc.getAccuracy() < 80) {

            // Calculate Speed Preparation

            // if (loc.getProvider().equals(LocationManager.GPS_PROVIDER)) {

            if (mLastGPSLocation != null) {
                double TimeDelta = loc.getTime() - mLastGPSLocation.getTime();    // in milliseconds.
                // if (TimeDelta >=1000.0) // 1 second diff
                // {
                if (TimeDelta > 0.0) {
                    // make sure it is a new record.
                    // App.tts.Speak(String.format("Delta %3.1f", TimeDelta));
                    lastcalculatedspeed = GPSHelper.calculateSpeed(loc, mLastGPSLocation);

                    mLastGPSLocation = loc;
                }

                if (Float.isNaN((float) lastcalculatedspeed)) {
                    loc.setSpeed(0.0f);
                } else if (((float) lastcalculatedspeed) <= 1.0) // errors
                {
                    loc.setSpeed(0.0f);
                } else {
                    loc.setSpeed((float) lastcalculatedspeed);
                }


            } else {
                // fill the first value.
                mLastGPSLocation = loc;
            }

        }


        currentLocation = loc;
        currentLocation.setAltitude(altitude);
        //Altitude here is $GPGGA.Altitude + $GPGGA.Height of geoid above WGS84 ellipsoid
        Event_IMU_CMD event_imu_cmd = new Event_IMU_CMD(Event_IMU_CMD.IMU_CMD_ReadGPS);
        event_imu_cmd.tag = loc;
        EventBus.getDefault().post(event_imu_cmd);
    }

    public int getSatelliteCount()
    {
        Iterator<GpsSatellite> sat = msatellites.iterator();
        int i = 0;
        while (sat.hasNext()) {
            GpsSatellite satellite = sat.next();
            if (satellite.usedInFix()==true) i+=0;
        }
        return i;
    }


    /***
     * Implements LocationListener
     * @param provider
     * @param status
     * @param extras
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {

    }

    /***
     * Implements LocationListener
     * @param provider
     */
    @Override
    public void onProviderEnabled(String provider) {
        //Log.d(App.TAG, provider +  " has been enabled");
        misFirstFix = false;
        //BugFix: when GPS disabled in middle of using APP.
        //registerSensor();
    }

    /***
     * Implements LocationListener
     * @param provider
     */
    @Override
    public void onProviderDisabled(String provider) {
     //Log.d(App.TAG, provider +  " has been disabled");
        misFirstFix = false;
        mFixLevel=0;
    }


    /***
     * @link http://android-developers.blogspot.com/2011/06/deep-dive-into-location.html
     * @return
     */
    public Location getBestLocation (Location loc)
    {
        try {
            long minTime = 9999999, bestTime = 9999999;
            float bestAccuracy = 50.0f;
            Location bestResult = loc;
            List<String> matchingProviders = mLocationManager.getAllProviders();
            for (String provider : matchingProviders) {
                Location location = mLocationManager.getLastKnownLocation(provider);
                if (location != null) {
             /*   float accuracy = location.getAccuracy();
                long time = location.getTime();

                if ((time > minTime && accuracy < bestAccuracy)) {
                    bestResult = location;
                    bestAccuracy = accuracy;
                    bestTime = time;
                }
                else if (time < minTime &&
                        bestAccuracy == Float.MAX_VALUE && time > bestTime){
                    bestResult = location;
                    bestTime = time;
                }
                */
                    if (isBetterLocation(location, bestResult)) {
                        bestResult = location;
                    }
                }
            }
            return bestResult;
        }
        catch (SecurityException e)
        {
            return loc;
        }
    }


    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > EXPIREY_TIME;
        boolean isSignificantlyOlder = timeDelta < -EXPIREY_TIME;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else return isNewer && !isSignificantlyLessAccurate && isFromSameProvider;
    }

    /*
    public void update (){
        Location loc;
        try {
            //loc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            loc = getBestLocation();
            if (loc == null) return ;
            //GeomagneticField geoField = new GeomagneticField(Double.valueOf(loc.getLatitude()).floatValue(), Double.valueOf(loc.getLongitude()).floatValue(), Double.valueOf(loc.getAltitude()).floatValue(), System.currentTimeMillis());
            // ToDo: Link Declination  to MAG
            //Declination = geoField.getDeclination();

            //Log.d(App.TAG, loc.getProvider() + " lat:" + loc.getLatitude() + " lng:" + loc.getLongitude() + " alt:" + loc.getAltitude() + " acc:" + loc.getAccuracy());
        }
        catch(RuntimeException re)
        {
            Log.d ("SENSORS_GPS","Exception");
        }

    }*/



    /// TODO: Enh: please check location Fusion APIs http://developer.android.com/training/location/receive-location-updates.html
    @Override
    public void registerSensor()
    {
        //if ((mregisteredSensor == true) || (isSupported() == false)) return ;
        if (mregisteredSensor == true) return ;

        try {

            mLocationManager.addGpsStatusListener(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                OnNmeaMessageListenerlocal = new OnNmeaMessageListenerLocal();
                mLocationManager.addNmeaListener((OnNmeaMessageListenerLocal)OnNmeaMessageListenerlocal);
            }
            if (mLocationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minimumTime, minimumDistance, this);
                mregisteredSensor = true;
            }

            if (mLocationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minimumTime, minimumDistance, this);
                mregisteredSensor = true;
            }

        /*
        if (mLocationManager.getAllProviders().contains(LocationManager.PASSIVE_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, minimumTime, minimumDistance, this);
            mregisteredSensor = true;
        }*/
        }
        catch (SecurityException se)
        {
            Log.e("SENSORS_GPS", se.toString());
        }

    }

    @Override
    public void unregisterSensor()
    {
        if (mregisteredSensor == false)  return ;

        mLocationManager.removeGpsStatusListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mLocationManager.removeNmeaListener((OnNmeaMessageListenerLocal)OnNmeaMessageListenerlocal);
        }
        mLocationManager.removeUpdates(this);
        mregisteredSensor = false;

    }



    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

}
