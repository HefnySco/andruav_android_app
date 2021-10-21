package com.andruav.controlBoard.shared.missions;

/**
 * Created by mhefny on 9/30/16.
 */
public class MissionDayra extends MissionBase {


    public final static byte TYPE_DAYRA = 5;

    public double Altitude;
    public double Latitude;
    public double Longitude;
    public double Radius;
    public double Turns;



    public MissionDayra()
    {
        super();

        MohemmaTypeID = TYPE_DAYRA;
    }

    public MissionDayra(double latitude, double longitude, double altitude , double radius, int turns)
    {
        this();

        Latitude   = latitude;
        Longitude  = longitude;
        Altitude   = altitude;
        Radius     = radius;
        Turns      = turns;
    }

}
