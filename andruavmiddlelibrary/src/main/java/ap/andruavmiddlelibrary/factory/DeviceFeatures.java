package ap.andruavmiddlelibrary.factory;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Created by M.Hefny on 05-Jan-15.
 */
public class DeviceFeatures {

    public static boolean hasValidData;
    public static boolean hasSMSCapabilities;
    public static boolean hasTelephoney;
    public static boolean hasBluetooth;
    public static boolean hasGPS;
    public static boolean hasLocation;
    public static boolean hasCamera;
    public static boolean hasBarometer;
    public static boolean hasGyro;
    public static boolean hasAccelerometer;
    public static boolean hasCompass;
    public static boolean hasUSB_Host;
    public static boolean hasWIFI;
    public static boolean hasMultitouch;

    public static void scanDevice(final Context context)
    {
        try {
             PackageManager pm = context.getPackageManager();

            hasSMSCapabilities = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_GSM) || pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA);
            hasTelephoney = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_GSM);
            hasGPS = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
            hasLocation = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION);
            hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);             // done
            hasBarometer = pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER);
            hasCompass = pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
            hasAccelerometer = pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
            hasGyro = pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
            hasUSB_Host = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST);
            hasBluetooth = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
            hasWIFI = pm.hasSystemFeature(PackageManager.FEATURE_WIFI);
            hasMultitouch = pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH);

            hasValidData = true;

        }
        catch (Exception e)
        {
            // This was making a FATAL Exception
            hasValidData = false;
        }
    }
}
