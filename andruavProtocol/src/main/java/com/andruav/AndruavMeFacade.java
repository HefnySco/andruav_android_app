package com.andruav;

import com.andruav.event.fpv7adath.Event_FPV_CMD;
import com.andruav.controlBoard.ControlBoardBase;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_Ctrl_Camera;

/**
 * This is commands that control me as a Drone.
 * Created by mhefny on 1/25/17.
 */

public  class AndruavMeFacade {


    public static void Ctrl_Camera (final Event_FPV_CMD a7adath_fpv_cmd)
    {

        switch (a7adath_fpv_cmd.CameraSource )
        {
            case AndruavMessage_Ctrl_Camera.CAMERA_SOURCE_MOBILE:
                AndruavEngine.getEventBus().post(a7adath_fpv_cmd);
                break;

            case AndruavMessage_Ctrl_Camera.CAMERA_SOURCE_FCB:
                final ControlBoardBase controlBoardBase = AndruavSettings.andruavWe7daBase.FCBoard;
                if (controlBoardBase != null)
                {
                    controlBoardBase.do_TriggerCamera();
                }
                break;
        }
    }


}
