package com.andruav.andruavUnit;

import com.andruav.AndruavEngine;
import com.andruav.AndruavFacade;
import com.andruav.TelemetryProtocol;
import com.andruav.controlBoard.shared.missions.MissionBase;

/**
 * Created by mhefny on 4/7/17.
 */

public class AndruavUnitMe extends AndruavUnitBase {



    public AndruavUnitMe(final boolean isGCS)
    {
        super(true,isGCS);
    }

    /*
    BAD AREAS enta FAHEM
     */
    public boolean mIssue = false;

    /***
     *  mIsModule True: Camera, AI, FCB...etc.
     *  Note: COMM_MODULE is NOT MODULE here.
     */
    public boolean mIsModule = false;

    @Override
    protected void Telemetry_protocol_changed(int telemetry_protocol)
    {

        disposeFCBBase();


        if (telemetry_protocol != TelemetryProtocol.TelemetryProtocol_No_Telemetry)
        {
            if ((!this.IsCGS) )
            {

                AndruavEngine.getLo7etTa7akomMasna3().getFlightControlBoard(this);

            }
        }
    }


    @Override
    public void missionItemReached(final int missionItemIndex)
    {
        super.missionItemReached(missionItemIndex);

        AndruavFacade.sendWayPointsReached(null, missionItemIndex, MissionBase.Report_NAV_ItemReached);
    }

    private Long lastaccess_udp_proxy_receive =0l;
    /**
     * another vehicle is sending data to me via my udp proxy, I may want to activate telemetry
     * @return
     */
    public boolean isUdpProxyAccessedLately()
    {
        return  !((System.currentTimeMillis() - lastaccess_udp_proxy_receive) > 20000);
    }

    /**
     * update last access time.
     */
    public void updateUdpProxyLastReceiveTime()
    {
       lastaccess_udp_proxy_receive =System.currentTimeMillis();
    }

}
