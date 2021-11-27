package com.andruav.sensors;

import android.location.Location;

/**
 * Created by M.Hefny on 11-May-15.
 */
public class AndruavIMU {


    public final static int LOCATION_HISTORY_LENGTH = 100;

     /**
     * true if the source of IMU data is FCB "flight control board"
     */
    protected boolean useFCBIMU;
    public boolean getUseFCBIMU()
    {
        return useFCBIMU;
    }
    /**
     * Pitch
     */
    public double P;
    /**
     * Roll
     */
    public double R;
    /**
     * Y
     */
    public double Y;
    /**
     * GPS3DFix
     */
    public int GPS3DFix;
    public int GPSFixQuality;
    public float Hdop;
    public float Vdop;
    protected Location CurrentLocation;
    //public double GroundAltitude;

    private Boolean misMe;
    /***
     * This is a circular index where the pointer points to last valid point;
     */
    protected Location[] locationHistory;
    protected int locationHistoryIndex = -1;
    /**
     * Sattellite Count
     */
    public int SATC;
    /**
     * Pitch Tilt
     */
    public double PT;
    /**
     * Roll Tilt
     */
    public double RT;


    public Boolean iA;   //isAccSupported
    public Boolean iG;   //isGyroSupported
    public Boolean iM;  // isMagSupported


    /***
     * Current desired roll in degrees
     */
    public double nav_Roll;

    /***
     * Current desired pitch in degrees
     */
    public double nav_Pitch;



    /***
     * Bearing to current MISSION/target in degrees
     */
    public double nav_TargetBearing;

    /***
     * Distance to active MISSION in meters
     */
    public double nav_WayPointDistance;

    /***
     * Current altitude error in meters
     */
    public double nav_AltitudeError;






    // Statistics:
    public  double   GroundAltitude_max      = 0.0f;
    public  float    GroundSpeed_max         = 0.0f;
    public  float    GroundSpeed_avg         = 0.0f;
    public  long     IdleDuration            = 0;
    public  long     IdleTotalDuration       = 0;
    private long     LastIdleTime            = 0;
    /*
      smoothedValues: are values with complementary filter and calibrated
     */
    /**
     * ACCsmoothedValues
     */
    public double[] ACCsmoothedValues;
    /**
     * GSV
     */
    public double[] GSV;
    /**
     * MSV
     */
    public double[] MSV;

    /***
     *
     * @param isMe this is the Drone of this mobile not a remote drone
     * @param enableLocationHistory
     */
    public AndruavIMU (boolean isMe, boolean enableLocationHistory, boolean FCBIMU)
    {
        misMe = isMe;
        if (enableLocationHistory)
        {
            locationHistory = new Location[LOCATION_HISTORY_LENGTH];
        }
        useFCBIMU = FCBIMU;
    }

    /***
     *
     * @param pitch in degrees
     * @param roll  in degrees
     * @param yaw in degrees
     */
    public AndruavIMU (float pitch, float roll, float yaw, float pitchTilt, float rollTilt)
    {
        R = roll;
        P = pitch;
        Y = yaw;
        PT = pitchTilt;
        RT = rollTilt;
    }



    public double getAltitude ()
    {
        final Location loc=getCurrentLocation();
        if ( loc ==null) return 0.0;

        return loc.getAltitude();
    }
    protected void calculateStatistics (Location loc)
    {
        if (!misMe) return ;

        if (loc.hasSpeed()) {
            // measure speed statistics
            float speed = loc.getSpeed();
            if (speed > GroundSpeed_max) {
                GroundSpeed_max = speed;
            }
            if (speed < 0.5f) {
                long now = System.currentTimeMillis();
                if (LastIdleTime != 0) {
                    long timediff = (now - LastIdleTime);
                    IdleDuration = IdleDuration + timediff;
                    IdleTotalDuration = IdleTotalDuration + timediff;
                }
                LastIdleTime = now;
            } else {   // Reset timer
                LastIdleTime = 0;
                IdleDuration = 0;
            }
            GroundSpeed_avg = (GroundSpeed_avg + speed) / 2.0f;
        }

        //loc.hasAltitude() : contains absolute maxAltitude. while GroundAltitude is read from it
        if (loc.hasAltitude()) {
            final double groundAltitude = Double.parseDouble(String.valueOf(loc.getAltitude()));
            if ( groundAltitude > GroundAltitude_max)
            {
                GroundAltitude_max = groundAltitude;
            }
        }

    }


    public void setCurrentLocation(Location loc)
    {
        if (locationHistory != null)
        {
            if (loc != null) {
                // add location history
                locationHistoryIndex = (locationHistoryIndex + 1) % LOCATION_HISTORY_LENGTH;
                locationHistory[locationHistoryIndex] = loc;

                calculateStatistics (loc);

            }
        }
        CurrentLocation = loc;
    }

    public Location getCurrentLocation ()
    {
        return CurrentLocation;
    }

    public Boolean  hasCurrentLocation ()
    {
        return (CurrentLocation != null);
    }

    /***
     * valid only when history is enabled.
     * @param index
     * @return
     */
    public Location getLocationHistory(int index)
    {
        if (locationHistory == null) return null;
        if ((index <0) || (index >= locationHistoryIndex)) return null;

        return locationHistory[(locationHistoryIndex + index) % LOCATION_HISTORY_LENGTH];
    }
}
