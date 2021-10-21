package com.andruav.controlBoard.shared.missions;

/**
 * Created by mhefny on 2/1/16.
 */
public class MissionBase {

    public final static byte TYPE_UNKNOWN =0;

    public final static int Report_NAV_Unknown          = 0;
    public final static int Report_NAV_ItemReached      = 1;
    public final static int Report_NAV_ItemExecuting    = 2;



    public int MohemmaTypeID;

    public  int     Sequence;

    public int  Status = Report_NAV_Unknown;
    /***
     * Nest Target
     */
    public boolean  GoTo = false;



    public double getHash ()
    {
        return this.hashCode();
    }


    public MissionBase()
    {
        MohemmaTypeID = TYPE_UNKNOWN;
    }
}
