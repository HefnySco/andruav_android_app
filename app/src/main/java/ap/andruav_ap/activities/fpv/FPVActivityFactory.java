package ap.andruav_ap.activities.fpv;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.andruav.AndruavSettings;
import com.andruav.interfaces.INotification;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;

import ap.andruav_ap.activities.fpv.drone.FPVDroneRTCWebCamActivity;

import com.andruav.event.fpv7adath._7adath_InitAndroidCamera;

import ap.andruav_ap.activities.fpv.drone.FPVModuleRTCWebCamActivity;
import ap.andruav_ap.helpers.CheckAppPermissions;
import ap.andruavmiddlelibrary.factory.DeviceFeatures;

/**
 * Created by M.Hefny on 18-Jul-15.
 */
public class FPVActivityFactory {

    public static void startFPVActivity (final Context context)
    {
        startFPVActivity (context,null);
    }

    public static void startFPVActivity (final Context context,  final _7adath_InitAndroidCamera a7adath_fpv_cmd) {

        if (context == null)
        {
            return ;
        }
        if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
        } else {  // you cannot use a drone if mobile does not have a cam
            // Camera permission is only required when the device actually has a camera.
            // On camera-less devices there is nothing to grant and nothing to stream from,
            // so there is nothing to start either.
            if (!DeviceFeatures.hasCamera)
            {
                return;
            }
            if (!CheckAppPermissions.ensurePermission((Activity) context,
                    Manifest.permission.CAMERA, AndruavMessage_Error.ERROR_CAMERA,
                    INotification.INFO_TYPE_CAMERA, "Please grant Camera Permission"))
            {
                //Log.d(TAG, "checkPermissionAndRequest failed");
                return;
            }
            // Microphone is optional: video streaming does not use the mic, so a missing
            // RECORD_AUDIO permission must not block FPV from starting. Still prompt/report
            // so the user is aware, but ignore the result.
            CheckAppPermissions.ensurePermission((Activity) context,
                    Manifest.permission.RECORD_AUDIO, AndruavMessage_Error.ERROR_CAMERA,
                    INotification.INFO_TYPE_CAMERA, "Please grant Microphone Permission");

            Intent intent;
            if (AndruavSettings.andruavWe7daBase.mIsModule)
            {
                intent = new Intent(context, FPVModuleRTCWebCamActivity.class);
            }
            else
            {
                intent = new Intent(context, FPVDroneRTCWebCamActivity.class);
            }
            // enforce RTC
            //final Intent intent = new Intent(context, FPVDroneRTCWebChromeCamActivity.class);
            context.startActivity(intent);
        }
    }
}
