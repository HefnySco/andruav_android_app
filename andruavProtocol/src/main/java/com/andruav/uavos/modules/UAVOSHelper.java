package com.andruav.uavos.modules;

import com.andruav.AndruavEngine;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.andruav.uavos.modules.UAVOSConstants.UAVOS_MODULE_TYPE_CAMERA;

public final  class UAVOSHelper {


    /**
     * This is very important function.
     * It collects all cameras from all uavos camera modules to send it to GCS
     * so that a user can select a camera to stream from.
     * @return
     */
    public static JSONArray getCameraList()  {

        /*
            jsonVideoSource[CAMERA_SUPPORT_VIDEO "v"]       = true;
            jsonVideoSource[CAMERA_SUPPORT_VIDEO "r"]      = true; // recording
            jsonVideoSource[CAMERA_SUPPORT_FLASH "f"]       = true;
            jsonVideoSource[CAMERA_SUPPORT_ZOOM "z"]        = true;
            jsonVideoSource[CAMERA_LOCAL_NAME "ln"]         = deviceInfo.local_name;
            jsonVideoSource[CAMERA_UNIQUE_NAME "id"]        = deviceInfo.unique_name;
            jsonVideoSource[CAMERA_ACTIVE "active"]         = deviceInfo.active;
            jsonVideoSource[CAMERA_TYPE "p"]                = EXTERNAL_CAMERA_TYPE_RTCWEBCAM;
        */

        final JSONArray cameraList = new JSONArray();

        try {
            final int len = AndruavEngine.getUAVOSMapBase().size();

            for (int i = 0; i < len; ++i) {
                final UAVOSModuleUnit uavosModuleUnit = AndruavEngine.getUAVOSMapBase().valueAt(i);

                if (uavosModuleUnit.ModuleClass.equals(UAVOS_MODULE_TYPE_CAMERA)) {
                    final UAVOSModuleCamera cameraModue = (UAVOSModuleCamera) uavosModuleUnit;

                    final JSONArray cameras = (JSONArray) cameraModue.getModuleMessages();
                    if (cameras != null) {
                        final int len2 = cameras.length();
                        for (int j = 0; j < len2; ++j) {
                            cameraList.put(cameras.get(j));
                        }
                    }

                }
            }

        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            return cameraList;
        }
    }



    public static UAVOSModuleCamera getCameraByID (final String id)
    {
        try {
            final int len = AndruavEngine.getUAVOSMapBase().size();

            for (int i = 0; i < len; ++i) {
                final UAVOSModuleUnit uavosModuleUnit = AndruavEngine.getUAVOSMapBase().valueAt(i);

                if (uavosModuleUnit.ModuleClass.equals(UAVOS_MODULE_TYPE_CAMERA)) {
                    final UAVOSModuleCamera cameraModule = (UAVOSModuleCamera) uavosModuleUnit;

                    final JSONArray cameras = (JSONArray) cameraModule.getModuleMessages();
                    final int camlen = cameras.length();
                    for (int j = 0; j < camlen; ++j) {
                        final JSONObject camera = (JSONObject) cameras.get(j);
                        if (id.equals(camera.getString(UAVOSConstants.CAMERA_UNIQUE_NAME)))
                        {
                            return cameraModule;
                        }
                    }
                }
            }
        }
        catch (final  Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }


    public static UAVOSModuleCamera getBuiltInCamera ()
    {

        try {
            final int len = AndruavEngine.getUAVOSMapBase().size();

            for (int i = 0; i < len; ++i) {
                final UAVOSModuleUnit uavosModuleUnit = AndruavEngine.getUAVOSMapBase().valueAt(i);

                if ((uavosModuleUnit.ModuleClass.equals(UAVOS_MODULE_TYPE_CAMERA))
                    && (uavosModuleUnit.BuiltInModule))
                {
                        return (UAVOSModuleCamera) uavosModuleUnit;
                }
            }
        }
        catch (final  Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

}
