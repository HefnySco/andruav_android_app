package ap.andruav_ap.helpers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.andruav.AndruavEngine;
import com.andruav.AndruavFacade;
import com.andruav.interfaces.INotification;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;

import java.util.ArrayList;
import java.util.List;

import ap.andruav_ap.App;
import ap.andruavmiddlelibrary.factory.DeviceFeatures;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;

import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_REGISTERED;

public abstract class CheckAppPermissions {



    public static void goToSettings()
    {

        final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + App.getAppContext().getPackageName()));
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        App.getAppContext().startActivity(intent);
    }

    /***
     * Whether the OS's standard Doze/App-Standby battery optimization is already disabled for
     * this app. This is the plain AOSP mechanism every device exposes the same way - it says
     * nothing about any additional OEM-specific background-process killer layered on top.
     */
    public static boolean isIgnoringBatteryOptimizations(final Activity activity)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        final PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(activity.getPackageName());
    }

    /***
     * Launches the standard OS dialog asking the user to exempt this app from battery
     * optimization, so its foreground services (telemetry link, FPV streaming) are less likely
     * to be killed while backgrounded mid-flight.
     */
    public static void requestIgnoreBatteryOptimizations(final Activity activity, final int requestCode)
    {
        final Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + activity.getPackageName()));
        activity.startActivityForResult(intent, requestCode);
    }


    /***
     * Pure boolean check of whether ALL runtime permissions are granted.
     * No side effects (no popups, no requestPermissions calls).
     */
    public static boolean isPermissionsOK (final  Activity activity)
    {
        return getMissingPermissions(activity).isEmpty();
    }

    /***
     * Same set of permissions checked by {@link #isPermissionsOK}, but returns the
     * names of the ones that are NOT granted instead of collapsing them to a boolean --
     * used to tell the user exactly what they denied instead of a generic message.
     */
    public static List<String> getMissingPermissions (final Activity activity)
    {
        final List<String> missing = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return missing;

        //https://developer.android.com/reference/android/Manifest.permission#WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) //Q == API 29
        {
            addIfMissing(activity, missing, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
        {
            addIfMissing(activity, missing, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        else
        {
            //https://developer.android.com/about/versions/13/behavior-changes-13#granular-media-permissions
            addIfMissing(activity, missing, Manifest.permission.READ_MEDIA_AUDIO);
            addIfMissing(activity, missing, Manifest.permission.READ_MEDIA_IMAGES);
            addIfMissing(activity, missing, Manifest.permission.READ_MEDIA_VIDEO);
        }
        // Camera/GPS/SMS are only required on devices that actually have that
        // hardware -- a device without a camera, GPS, or telephony has nothing
        // to grant, so don't hold up the app waiting on those permissions.
        if (DeviceFeatures.hasCamera) {
            addIfMissing(activity, missing, Manifest.permission.CAMERA);
        }
        if (DeviceFeatures.hasGPS || DeviceFeatures.hasLocation) {
            addIfMissing(activity, missing, Manifest.permission.ACCESS_FINE_LOCATION);
        }
        addIfMissing(activity, missing, Manifest.permission.READ_PHONE_STATE);
        if (DeviceFeatures.hasSMSCapabilities) {
            addIfMissing(activity, missing, Manifest.permission.READ_SMS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //https://developer.android.com/develop/ui/views/notifications/notification-permission
            addIfMissing(activity, missing, Manifest.permission.POST_NOTIFICATIONS);
        }

        return missing;
    }

    private static void addIfMissing (final Activity activity, final List<String> missing, final String permission)
    {
        if (!checkPermission(activity, permission)) {
            missing.add(permission.replace("android.permission.", ""));
        }
    }

    public static boolean checkPermission (final Activity activity,final String permission)
    {
        return ContextCompat.checkSelfPermission(activity.getApplicationContext(),
                permission) == PackageManager.PERMISSION_GRANTED;
    }

    /***
     * Permission check that works without an Activity context (e.g. from sensors,
     * SMS, Bluetooth modules). Returns true if granted.
     */
    public static boolean checkPermission (final String permission)
    {
        return ContextCompat.checkSelfPermission(App.getAppContext(),
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


    /***
     * Smart per-permission check for Activity-based code (e.g. FPV).
     * <ul>
     *   <li>If permission is granted -> returns true.</li>
     *   <li>If connected to Andruav server -> sends an Andruav ERROR message
     *       (no popup) and returns false.</li>
     *   <li>If NOT connected -> delegates to {@link #checkPermissionAndRequest}
     *       (shows popup) and returns false.</li>
     * </ul>
     *
     * @param activity             calling Activity
     * @param permission           Manifest.permission constant
     * @param errorNumber          {@link AndruavMessage_Error} error code
     * @param infoType             {@link INotification} info type
     * @param description          human-readable description (permission name is appended)
     * @return true if permission is granted, false otherwise
     */
    public static boolean ensurePermission (final Activity activity, final String permission,
                                            final int errorNumber, final int infoType,
                                            final String description)
    {
        if (checkPermission(activity, permission)) {
            return true;
        }

        if (AndruavEngine.isAndruavWSStatus(SOCKETSTATE_REGISTERED)) {
            // Connected: send error, do NOT show popup.
            AndruavFacade.sendErrorMessage(infoType, INotification.NOTIFICATION_TYPE_ERROR,
                    errorNumber, description + " (permission denied: " + permission + ")", null);
            return false;
        }

        // Not connected: show popup.
        checkPermissionAndRequest(activity, permission, description);
        return false;
    }


    /***
     * Non-Activity variant for module-level code (Sensor_GPS, SMS, Bluetooth)
     * that has no Activity context to show a popup from.
     * <ul>
     *   <li>If permission is granted -> returns true.</li>
     *   <li>If connected to Andruav server -> sends an Andruav ERROR message and returns false.</li>
     *   <li>If NOT connected -> just logs and returns false (no popup possible without Activity).</li>
     * </ul>
     *
     * @param permission     Manifest.permission constant
     * @param errorNumber    {@link AndruavMessage_Error} error code
     * @param infoType       {@link INotification} info type
     * @param description    human-readable description
     * @return true if permission is granted, false otherwise
     */
    public static boolean reportMissingPermission (final String permission,
                                                   final int errorNumber, final int infoType,
                                                   final String description)
    {
        if (checkPermission(permission)) {
            return true;
        }

        if (AndruavEngine.isAndruavWSStatus(SOCKETSTATE_REGISTERED)) {
            AndruavFacade.sendErrorMessage(infoType, INotification.NOTIFICATION_TYPE_ERROR,
                    errorNumber, description + " (permission denied: " + permission + ")", null);
        } else {
            Log.d("CheckAppPermissions", "Missing permission (not connected): " + permission);
        }
        return false;
    }


    /***
     * Bulk non-blocking permission request for FirstScreen.
     * Triggers the system permission dialog for all runtime permissions at once
     * but does NOT block and does NOT check the result.
     */
    public static void requestAllPermissions (final Activity activity)
    {
        requestAllPermissions(activity, 1);
    }

    /***
     * Same as {@link #requestAllPermissions(Activity)} but lets the caller supply
     * a request code so it can identify the result in
     * {@link Activity#onRequestPermissionsResult(int, String[], int[])}.
     */
    public static void requestAllPermissions (final Activity activity, final int requestCode)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        final List<String> perms = new ArrayList<>();

        // Storage
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            perms.add(Manifest.permission.READ_MEDIA_AUDIO);
            perms.add(Manifest.permission.READ_MEDIA_IMAGES);
            perms.add(Manifest.permission.READ_MEDIA_VIDEO);
        }

        // Only request permissions for hardware the device actually has --
        // camera/GPS/SMS are no longer a "must" on devices lacking that hardware.
        if (DeviceFeatures.hasCamera) {
            perms.add(Manifest.permission.CAMERA);
        }
        perms.add(Manifest.permission.RECORD_AUDIO);
        if (DeviceFeatures.hasGPS || DeviceFeatures.hasLocation) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        perms.add(Manifest.permission.READ_PHONE_STATE);
        if (DeviceFeatures.hasSMSCapabilities) {
            perms.add(Manifest.permission.READ_SMS);
            perms.add(Manifest.permission.SEND_SMS);
        }

        // Bluetooth (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN);
            perms.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        // Notifications (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (perms.isEmpty()) return;

        ActivityCompat.requestPermissions(activity, perms.toArray(new String[0]), requestCode);
    }


}
