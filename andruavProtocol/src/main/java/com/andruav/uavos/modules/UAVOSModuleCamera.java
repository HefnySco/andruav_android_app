package com.andruav.uavos.modules;


/**
 *
 *
 * Author: Mohammad S. Hefny
 * Date Jan 2020
 */

import org.json.JSONArray;

import static com.andruav.uavos.modules.UAVOSConstants.UAVOS_MODULE_TYPE_CAMERA;

public class UAVOSModuleCamera extends UAVOSModuleUnit {

    protected JSONArray CameraArray;

    public UAVOSModuleCamera ()
    {

        ModuleClass = UAVOS_MODULE_TYPE_CAMERA;
    }

    /**
     * For Camera Module it holds camera array
     * @param moduleMessage
     */
    @Override
    public void setModuleMessages(final Object moduleMessage)
    {
        if (moduleMessage == null)
        {
            return;
        }


        CameraArray = ((JSONArray)moduleMessage);
        /*
            jsonVideoSource[CAMERA_SUPPORT_VIDEO "v"]           = true;
            jsonVideoSource[CAMERA_LOCAL_NAME "ln"]             = deviceInfo.local_name;
            jsonVideoSource[CAMERA_UNIQUE_NAME "id"]            = deviceInfo.unique_name;
            jsonVideoSource[CAMERA_ACTIVE "active"]             = deviceInfo.active;
            jsonVideoSource[CAMERA_TYPE "p"]                    = EXTERNAL_CAMERA_TYPE_RTCWEBCAM;
            jsonVideoSource[CAMERA_TYPE "f"]                    = ANDROID_DUAL_CAM; facing/rearing (true,false)
            jsonVideoSource[CAMERA_TYPE "z"]					= Support Zooming
        */
    }


    /**
     * For Camera Module it holds camera array
     * @return
     */
    @Override
    public Object getModuleMessages()
    {
        return CameraArray;
    }


}
