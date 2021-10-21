package rcmobile.FPV;

import android.os.Build;

import com.andruav.FeatureSwitch;
import rcmobile.andruavmiddlelibrary.factory.DeviceFeatures;

/**
 * Created by M.Hefny on 05-Jan-15.
 */
public class DeviceManagerFacade {


    public static boolean hasValidData ()
    {
        //TODO: Make use of this data
        return DeviceFeatures.hasValidData;
    }

    public static final boolean canBeDroneAndruav()
    {
        if (FeatureSwitch.Disable_DroneMode_Check)
        {
            return true;
        }
        final boolean bFeature= DeviceFeatures.hasCompass && DeviceFeatures.hasLocation;
        return bFeature;
        //return true;
    }

    public static final boolean hasGPS () {
        final boolean bFeature = DeviceFeatures.hasGPS;
        return bFeature;
    }

    public static final boolean haveIMU () {
        final boolean bFeature = DeviceFeatures.hasCompass && DeviceFeatures.hasAccelerometer && DeviceFeatures.hasGPS;
        return bFeature;
    }


    public static final boolean hasCamera () {
        final boolean bFeature = DeviceFeatures.hasCamera;
        return bFeature;
    }

    public static final boolean hasUSBHost() {
        final boolean bFeature = DeviceFeatures.hasUSB_Host;
        return bFeature;
    }

    public static final boolean hasBlueTooth() {
        final boolean bFeature = DeviceFeatures.hasBluetooth;
        return bFeature;
    }

    public static final boolean hasMultitouch() {
        final boolean bFeature = DeviceFeatures.hasMultitouch;
        return bFeature;
    }


    public static boolean isRunningOnEmulator()
    {
        boolean result= Build.MODEL.contains("google_sdk")
                        || Build.MODEL.contains("Emulator")
                        || Build.MODEL.contains("Android SDK built for x86");
        if(result)
            return true;
        result = Build.BRAND.startsWith("generic")&&Build.DEVICE.startsWith("generic");
        if(result)
            return true;
        result = "google_sdk".equals(Build.PRODUCT);
        return result;

    }
}
