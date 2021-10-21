package com.andruav.controlBoard.shared.common;

/**
 * Created by M.Hefny on 30-Apr-15.
 */
public class VehicleTypes {

    private VehicleTypes ()
    {}


    public final static int VEHICLE_UNKNOWN = 0;
    public final static int VEHICLE_TRI = 1;
    public final static int VEHICLE_QUAD = 2;
    public final static int VEHICLE_PLANE = 3;
    public final static int VEHICLE_ROVER = 4;
    public final static int VEHICLE_HELI = 5;
    public final static int VEHICLE_SUBMARINE = 12;


    public final static int VEHICLE_GIMBAL = 15;

    public final static int VEHICLE_BUS     = 997;
    public final static int VEHICLE_PERSON  = 998;
    public final static int VEHICLE_GCS = 999;




    public final static String[] vechicleTypes= {"Generic","TriCopter","QuadCopter","Fixed Wing","Rover","HELI"};

}
