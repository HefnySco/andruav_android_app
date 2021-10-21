package com.andruav.util;

/**
 * Created by mhefny on 2/26/17.
 */

public class Maths {

    public static final double d2r = Math.PI / 180.0;
    public static final double r2d = 180.0 / Math.PI;

    // was taggged synchronized
    public  static double  Constraint (double min, double x, double max)
    {
        return Math.min(max,Math.max(x,min));
    }



}
