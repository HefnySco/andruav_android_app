package com.andruav.controlBoard.shared.missions;

/**
 * Mission Land
 * Created by mhefny on 2/21/16.
 */
public class MissionHoboot extends MissionBase {

    public final static byte TYPE_HOBOOT = 21; // same as mavlink


    public MissionHoboot()
    {
        super();

        MohemmaTypeID = TYPE_HOBOOT;
    }

}
