package com.andruav.util;

/**
 * Created by mhefny on 2/26/17.
 */

public class Tracking_Algorithm {


    public double p,i,d;
    public final static double IMAX = 500;
    public final static double NIMAX = -500;
    public double pTerm,iTerm,dTerm;
    public double oldError;
    public double dOldValue2;



    public Tracking_Algorithm ()
    {
        init();
    }


    public void init ()
    {
        p = 200;
        i = 0;
        d = 0;
    }

    public void setPID (final double P, final double I, final double D)
    {
        p = P;
        i = I;
        d = D;
    }


    public void reset ()
    {
        pTerm = 0.0;
        iTerm = 0.0;
        dTerm = 0.0;
        oldError = 0.0;
    }

    /***
     * error is screen normalized value from [-1,1]
     * @param error
     * @return remote normalized value from [0,1000]
     */
    public double calculate (final double error)
    {

        pTerm = error * p;
        iTerm = iTerm + i * error;
        dTerm = (error - oldError) * dTerm;

        iTerm = Math.max(NIMAX,Math.min(IMAX,iTerm));

        double total = pTerm + iTerm - dTerm;
        total = Math.max(-500,Math.min(500,total));  // output range should be by max 500 from center.

        //total = total + 500; // now change range to on-screen remote range [1,1000]
        //total = Math.max(1,Math.min(1000,total));
        //total = total + 1500; // now change range to on-screen remote range [1000,2000]
        //total = Math.max(1000,Math.min(2000,total));





        return  total;
    }

}
