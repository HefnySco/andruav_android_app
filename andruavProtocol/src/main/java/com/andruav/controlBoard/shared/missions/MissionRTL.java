package com.andruav.controlBoard.shared.missions;

/**
 * Created by mhefny on 2/21/16.
 */
public class MissionRTL extends MissionBase {

    public final static byte TYPE_RTL = 20; // same as mavlink


    public MissionRTL()
    {
        super();

        MohemmaTypeID = TYPE_RTL;
    }



}
