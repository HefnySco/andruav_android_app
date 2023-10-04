package ap.andruav_ap.activities.fpv;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.andruav.AndruavSettings;

import ap.andruav_ap.activities.fpv.drone.FPVDroneRTCWebCamActivity;

import com.andruav.event.fpv7adath._7adath_InitAndroidCamera;

import ap.andruav_ap.activities.fpv.drone.FPVModuleRTCWebCamActivity;
import ap.andruav_ap.helpers.CheckAppPermissions;

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
            return ;
        } else {  // you cannot use a drone if mobile does not have a cam
            if (!CheckAppPermissions.checkPermissionAndRequest((Activity) context,
                    Manifest.permission.CAMERA,"Please grant Camera Permission"))
            {
                //Log.d(TAG, "checkPermissionAndRequest failed");
                return;
            }
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
