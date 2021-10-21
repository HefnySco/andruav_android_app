package com.andruav.controlBoard.shared.missions;


/**
 * Created by M.Hefny on 16-Apr-15.
 */
public class WayPointStep extends MissionBase {


    public final static byte TYPE_WAYPOINTSTEP = 16; // same as mavlink


    public  double  Latitude;
    public  double  Longitude;
    public  double  Altitude;
    public  float   Heading;
    public  double  TimeToStay;



    public WayPointStep ()
    {
        super();

        MohemmaTypeID = TYPE_WAYPOINTSTEP;
    }


    @Override
    public double getHash ()
    {
        return Latitude + Longitude + Altitude + Heading + TimeToStay; // dont add Sequence
    }



}
