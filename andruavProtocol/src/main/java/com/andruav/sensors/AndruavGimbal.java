package com.andruav.sensors;

/**
 * Created by mhefny on 4/10/17.
 */

public class AndruavGimbal {

    // values compatible with MAVLINK
    public static final int MAV_MOUNT_MODE_RETRACT = 0;
    public static final int MAV_MOUNT_MODE_NEUTRAL = 1;
    public static final int MAV_MOUNT_MODE_MAVLINK_TARGETING = 2;
    public static final int MAV_MOUNT_MODE_RC_TARGETING = 3;
    public static final int MAV_MOUNT_MODE_GPS_POINT = 4;
    public static final int MAV_MOUNT_MODE_ENUM_END = 5;

    private int GimbalControlMode = MAV_MOUNT_MODE_GPS_POINT;


    public void setMode (int mode)
    {
        GimbalControlMode = mode;
    }

    public int getMode ()
    {
        return GimbalControlMode;
    }


    /***
     * degg  or lat based on Mode
     */
    private double mPitch;
    /***
     * degg or lng based on Mode
     */
    private double mRoll;
    /***
     * degg or alt (in cm)  based on Mode
     */
    private double mYaw;



    public void setPitch (double degree)
    {
        mPitch = degree;
    }

    public double getPitch ()
    {
        return  mPitch;
    }


    public void setRoll (double degree)
    {
        mRoll = degree;
    }

    public double getRoll ()
    {
        return  mRoll;
    }


    public void setYaw(double degree)
    {
        mYaw = degree;
    }

    public double getYaw ()
    {
        return  mYaw;
    }



    private double mLongitude;
    private double mLatitude;
    private double mAltitude;


    public void setLongitude (double lng)
    {
        mLongitude = lng;
    }

    public double getLongitude ()
    {
        return  mLongitude;
    }


    public void setLatitude (double lat)
    {
        mLatitude = lat;
    }

    public double getLatitude ()
    {
        return  mLatitude;
    }


    public void setAltitude(double alt)
    {
        mAltitude = alt;
    }

    public double getAltitude ()
    {
        return  mAltitude;
    }


    private boolean mStabilizePitch;
    private boolean mStabilizeRoll;
    private boolean mStabilizeYaw;


    public void setStabilizePitch (boolean enable)
    {
        mStabilizePitch = enable;
    }

    public boolean getStabilizePitch ()
    {
        return  mStabilizePitch;
    }


    public void setStabilizeRoll (boolean enable)
    {
        mStabilizeRoll = enable;
    }

    public boolean getStabilizeRoll ()
    {
        return  mStabilizeRoll;
    }


    public void setStabilizeYaw(boolean enable)
    {
        mStabilizeYaw = enable;
    }

    public boolean getStabilizeYaw ()
    {
        return  mStabilizeYaw;
    }


    private int mMinRollAngle;
    private int mMaxRollAngle;
    private int mMinPitchAngle;
    private int mMaxPitchAngle;
    private int mMinYawAngle;
    private int mMaxYawAngle;

    public void setMinPitchAngle(int minPitchAngle) {
        this.mMinPitchAngle = minPitchAngle;
    }

    public int getmMinPitchAngle() {
        return mMinPitchAngle;
    }

    public void setMaxPitchAngle(int maxPitchAngle) {
        this.mMaxPitchAngle = maxPitchAngle;
    }

    public int getmMaxPitchAngle() {
        return mMaxPitchAngle;
    }

    public void setMinRollAngle(int minRollAngle) {
        this.mMinRollAngle = minRollAngle;
    }

    public int getmMinRollAngle() {
        return mMinRollAngle;
    }

    public void setMaxRollAngle(int maxRollAngle) {
        this.mMaxRollAngle = maxRollAngle;
    }

    public int getmMaxRollAngle() {
        return mMaxRollAngle;
    }

    public void setMinYawAngle(int minYawAngle) {
        this.mMinYawAngle = minYawAngle;
    }

    public int getmMinYawAngle() {
        return mMinYawAngle;
    }

    public void setMaxYawAngle(int maxYawAngle) {
        this.mMaxYawAngle = maxYawAngle;
    }

    public int getmMaxYawAngle() {
        return mMaxYawAngle;
    }
}
