package com.andruav.andruavUnit;

import com.andruav.AndruavEngine;
import com.andruav.AndruavFacade;
import com.andruav.TelemetryProtocol;
import com.andruav.controlBoard.shared.missions.MissionBase;
import com.andruav.controlBoard.shared.missions.MissionCameraTrigger;
import com.andruav.controlBoard.shared.missions.MissionCameraControl;
import com.andruav.event.fpv7adath.Event_FPV_CMD;
import com.andruav.event.fpv7adath._7adath_InitAndroidCamera;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_Ctrl_Camera;

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
}
