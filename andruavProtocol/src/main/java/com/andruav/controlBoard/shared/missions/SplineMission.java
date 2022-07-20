package com.andruav.controlBoard.shared.missions;

/**
 * Created by mhefny on 10/1/16.
 */
public class SplineMission extends MissionBase {
    public final static int TYPE_SPLINE_WAYPOINT = 6;



    public  double  Latitude;
    public  double  Longitude;
    public  double  Altitude;
    public  double  TimeToStay;


    public SplineMission()
    {
        super();

        MohemmaTypeID = TYPE_SPLINE_WAYPOINT;
    }
}
