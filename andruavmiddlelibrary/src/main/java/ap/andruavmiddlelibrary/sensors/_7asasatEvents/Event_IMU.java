package ap.andruavmiddlelibrary.sensors._7asasatEvents;

import android.location.Location;

import com.andruav.sensors.AndruavIMU;

/**
 * Created by M.Hefny on 17-Sep-14.
 */
public class Event_IMU {

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
    public Location CurrentLocation;
   // public double groundAltitude;
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
    /**
     * smoothedValues: are values with complementary filter and calibrated
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

    public Event_IMU ()
    {

    }

    /***
     *
     * @param pitch in degrees
     * @param roll  in degrees
     * @param yaw in degrees
     */
    public Event_IMU (float pitch, float roll, float yaw, float pitchTilt, float rollTilt)
    {
        R = roll;
        P = pitch;
        Y = yaw;
        PT = pitchTilt;
        RT = rollTilt;
    }

    public Event_IMU (AndruavIMU andruavIMU)
    {
        R = andruavIMU.R;
        P = andruavIMU.P;
        Y = andruavIMU.Y;
        GPS3DFix = andruavIMU.GPS3DFix;
        CurrentLocation = andruavIMU.getCurrentLocation();
    //    groundAltitude = andruavIMU.GroundAltitude;
        SATC = andruavIMU.SATC;
        PT = andruavIMU.PT;
        RT = andruavIMU.RT;
        iA = andruavIMU.iA;
        iG = andruavIMU.iG;
        iM = andruavIMU.iM;
        ACCsmoothedValues = andruavIMU.ACCsmoothedValues;
        GSV = andruavIMU.GSV;
        MSV = andruavIMU.MSV;
    }
}
