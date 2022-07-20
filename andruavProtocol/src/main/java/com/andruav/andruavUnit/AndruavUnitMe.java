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
        MissionBase missionBase = getMohemmaMapBase().valueAt(missionItemIndex);
        if (missionBase == null)
        {
            // current mission is not updated
            //App.droneKitServer.doReadMission();
            FCBoard.do_ReadMission();
            return ;
        }
        missionBase.Status = MissionBase.Report_NAV_ItemReached;

        // Take Actions based on some Mission Items
        if ((missionBase instanceof MissionCameraTrigger) || (missionBase instanceof MissionCameraControl))
        {
            AndruavEngine.getEventBus().post(new _7adath_InitAndroidCamera());

            Event_FPV_CMD event_fpv_cmd = new Event_FPV_CMD(Event_FPV_CMD.FPV_CMD_TAKEIMAGE);
            event_fpv_cmd.CameraSource          = AndruavMessage_Ctrl_Camera.CAMERA_SOURCE_MOBILE;
            event_fpv_cmd.NumberOfImages        = 1;
            event_fpv_cmd.TimeBetweenShotes     = 0;
            event_fpv_cmd.DistanceBetweenShotes = 0;
            event_fpv_cmd.SendBackImages        =  true;
            event_fpv_cmd.SaveImageLocally = true;
            AndruavEngine.getEventBus().post(event_fpv_cmd);

        }
        AndruavFacade.sendWayPointsReached(null, missionItemIndex, MissionBase.Report_NAV_ItemReached);
    }
}
