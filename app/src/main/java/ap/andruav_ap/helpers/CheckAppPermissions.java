package ap.andruav_ap.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import ap.andruav_ap.App;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;

public abstract class CheckAppPermissions {



    public static void goToSettings()
    {

        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + App.getAppContext().getPackageName()));
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        App.getAppContext().startActivity(intent);
    }


    public static boolean isPermissionsOK (final  Activity activity)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

        boolean permissionsOK;
        //https://developer.android.com/reference/android/Manifest.permission#WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) //Q == API 29
        {
            permissionsOK = CheckAppPermissions.checkPermission(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            //Log.d("CheckAppPermissions", "isPermissionsOK 1a = " + permissionsOK);
        }
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
        {
            permissionsOK = CheckAppPermissions.checkPermission(activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            //Log.d("CheckAppPermissions", "isPermissionsOK 1b = " + permissionsOK);
        }
        else
        {
            //https://developer.android.com/about/versions/13/behavior-changes-13#granular-media-permissions
            permissionsOK = CheckAppPermissions.checkPermission(activity,
                    Manifest.permission.READ_MEDIA_AUDIO);
            permissionsOK = permissionsOK && CheckAppPermissions.checkPermission(activity,
                    Manifest.permission.READ_MEDIA_IMAGES);
            permissionsOK = permissionsOK && CheckAppPermissions.checkPermission(activity,
                    Manifest.permission.READ_MEDIA_IMAGES);
        }
        permissionsOK = permissionsOK && CheckAppPermissions.checkPermission(activity, Manifest.permission.CAMERA);
        permissionsOK = permissionsOK && CheckAppPermissions.checkPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
        permissionsOK = permissionsOK && CheckAppPermissions.checkPermission(activity, Manifest.permission.READ_PHONE_STATE);
        //permissionsOK = permissionsOK && CheckAppPermissions.checkPermission(activity, Manifest.permission.SEND_SMS);

        return permissionsOK;
    }

    public static boolean checkPermission (final Activity activity,final String permission)
    {
        return ContextCompat.checkSelfPermission(activity.getApplicationContext(),
                permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkPermissionAndRequest(final Activity activity, final String permission, final String permissionDescription)
    {

        // permission =  Manifest.permission.READ_CONTACTS
        if (!checkPermission(activity,permission))
        {

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    DialogHelper.doModalDialog(activity, "Permissions", permissionDescription, "OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    ActivityCompat.requestPermissions(activity,
                                            new String[]{permission},
                                            1);
                                }
                            });
            }
            else
            {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(activity,
                            new String[]{permission},
                            1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

            return false;
        }

        return true;

    }

}
