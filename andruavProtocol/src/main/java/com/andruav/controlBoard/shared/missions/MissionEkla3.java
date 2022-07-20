package com.andruav.controlBoard.shared.missions;

/**
 * Created by mhefny on 2/21/16.
 */
public class MissionEkla3 extends MissionBase {


    public final static int TYPE_EKLA3 = 22; // same as mavlink


    private double mAltitude;
    private double mPitch;


    public double getAltitude ()
    {
        return mAltitude;
    }

    public double getPitch()
    {
        return mPitch;
    }

    public void setAltitude (double altitude)
    {
        mAltitude = altitude;
    }


    public void setPitch (double pitch)
    {
        mPitch = pitch;
    }

    public MissionEkla3()
    {
        super();
        MohemmaTypeID = TYPE_EKLA3;
    }

    public MissionEkla3(double altitude, double pitch)
    {
        this();
        mAltitude = altitude;
        mPitch = pitch;
    }
}
