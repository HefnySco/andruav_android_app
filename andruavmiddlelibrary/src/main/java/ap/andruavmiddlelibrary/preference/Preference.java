package ap.andruavmiddlelibrary.preference;

import android.os.Build;
import android.view.Surface;

import com.andruav.AndruavEngine;
import com.andruav.Constants;
import com.andruav.controlBoard.ControlBoardBase;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.controlBoard.shared.common.VehicleTypes;
import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.util.StringSplit;

import ap.andruavmiddlelibrary.R;
import com.andruav.FeatureSwitch;

/**
 * Created by M.Hefny on 01-Oct-14.
 */
public class Preference {

    public static final String PREFS_COUNT = "RCMOBILE_FPV";


    public static String getPreferredLanguage(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "DkLxYPJekUmUNQ4clQVm_Q", "en");
    }

    public static void setPreferredLanguage(final android.content.ContextWrapper contextWrapper, String appversion) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "DkLxYPJekUmUNQ4clQVm_Q", appversion);
    }


    public static boolean isAutoStart(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "vyqtl51lG0OWxf64b1j37g", false);
    }

    public static void isAutoStart(final android.content.ContextWrapper contextWrapper, final boolean bEnabled) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "vyqtl51lG0OWxf64b1j37g", bEnabled);
    }


    /**
     * is ground station
     *
     * @param contextWrapper
     * @return
     */
    public static boolean isGCS(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "kR0dv", true);
    }

    public static void isGCS(final android.content.ContextWrapper contextWrapper, final boolean bEnabled) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "kR0dv", bEnabled);
    }


    /**
     * is ground station
     *
     * @param contextWrapper
     * @return
     */
    public static int getVehicleType(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "WelG2IUCUUzrG9", 0);
    }

    public static void setVehicleType(final android.content.ContextWrapper contextWrapper, int vehicleType) {
        if (vehicleType == VehicleTypes.VEHICLE_GCS) vehicleType = VehicleTypes.VEHICLE_UNKNOWN; //  this is a forbidden value
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "WelG2IUCUUzrG9", vehicleType);
    }


    /**
     * UAVOS MODULE TYPE
     *
     * @param contextWrapper
     * @return
     */
    public static String getModuleType(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "WXXG2IUCUUzrG9", ProtocolHeaders.UAVOS_COMM_MODULE_CLASS);
    }

    public static void setModuleType(final android.content.ContextWrapper contextWrapper, String appversion) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "WXXG2IUCUUzrG9", appversion);
    }


    public static String getCommModuleIP(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "WXXG2IUCUUzrG1", "");
    }

    public static void setCommModuleIP(final android.content.ContextWrapper contextWrapper, String appversion) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "WXXG2IUCUUzrG1", appversion);
    }

    public static boolean isCommModuleIPAutoDetect(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "WSXG2IUCUUzrG1", true);
    }

    public static void isCommModuleIPAutoDetect(final android.content.ContextWrapper contextWrapper, final boolean bEnabled) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "WSXG2IUCUUzrG1", bEnabled);
    }

    ////////////////////Login Preference

    public static String getAppVersion(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "ow13e", "");
    }

    public static void setAppVersion(final android.content.ContextWrapper contextWrapper, String appversion) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "ow13e", appversion);
    }


    public static boolean isLoginAuto(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "o55U2jyjWET3", false);
    }

    public static void isLoginAuto(final android.content.ContextWrapper contextWrapper, final boolean bEnabled) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "o55U2jyjWET3", bEnabled);
    }


    public static String getLoginUserName(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "cYXL6dKs", "");
    }

    public static void setLoginUserName(final android.content.ContextWrapper contextWrapper, String UserName) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "cYXL6dKs", UserName);
    }


    public static String getLoginAccessCode(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "UKcmmoFWi9aoQ", "");
    }

    public static void setLoginAccessCode(final android.content.ContextWrapper contextWrapper, String LoginAccess) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "UKcmmoFWi9aoQ", LoginAccess);
    }

    public static String getLoginPartyID(final android.content.ContextWrapper contextWrapper)
    {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "owPg4M", "");
    }

    public static void setLoginPartyID(final android.content.ContextWrapper contextWrapper, String partyid)
    {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT,contextWrapper,"owPg4M",partyid);
    }


    ///////////////////WebSocket Preference

    public static String getWebServerUserName(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "CzUwS8xu", Build.MODEL);
    }

    public static void setWebServerUserName(final android.content.ContextWrapper contextWrapper, String webserverUserName) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "CzUwS8xu", webserverUserName);
    }


    public static String getWebServerGroupName(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "Ui7DVRAOOqhTw", AndruavEngine.getPreference().getContext().getString(R.string.pref_groupname).toLowerCase());
    }

    public static void setWebServerGroupName(final android.content.ContextWrapper contextWrapper, String webserverGroupName) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "Ui7DVRAOOqhTw", webserverGroupName.toLowerCase());
    }


    public static String getWebServerUserDescription(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "TtdipFnw", AndruavEngine.getPreference().getContext().getString(R.string.pref_description));
    }

    public static void setWebServerUserDescription(final android.content.ContextWrapper contextWrapper, String webserverGroupName) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "TtdipFnw", webserverGroupName);
    }

    public static String getAuthServerURL(final android.content.ContextWrapper contextWrapper) {

        AndruavEngine.getPreference().getContext().getString(R.string.pref_ws_URL);
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "afxrzG4B", AndruavEngine.getPreference().getContext().getString(R.string.pref_auth_URL));
    }

    public static void setAuthServerURL(final android.content.ContextWrapper contextWrapper, String webserverURL) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "afxrzG4B", webserverURL);
    }


    public static int getAuthServerPort(final android.content.ContextWrapper contextWrapper) {

        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "ai8pfERTTONDQ", AndruavEngine.getPreference().getContext().getResources().getInteger(R.integer.pref_auth_Port));
    }

    public static void setAuthServerPort(final android.content.ContextWrapper contextWrapper, final int servicePort) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "ai8pfERTTONDQ", servicePort);
    }

    /***
     * This function is not useful anymore as it is updated each time server connects to the authentication site.
     *
     * @param contextWrapper
     * @return
     */
    public static String getWebServerURL(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "wfxrzG4B", AndruavEngine.getPreference().getContext().getString(R.string.pref_ws_URL));
    }

    /***
     * This function is not useful anymore as it is updated each time server connects to the authentication site.
     *
     * @param contextWrapper
     * @param webserverURL
     */
    public static void setWebServerURL(final android.content.ContextWrapper contextWrapper, String webserverURL) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "wfxrzG4B", webserverURL);
    }


    public static int getWebServerPort(final android.content.ContextWrapper contextWrapper) {

        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "ki8pfERTTONDQ", AndruavEngine.getPreference().getContext().getResources().getInteger(R.integer.pref_ws_Port));
    }

    public static void setWebServerPort(final android.content.ContextWrapper contextWrapper, final int servicePort) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "ki8pfERTTONDQ", servicePort);
    }


    public static boolean isEnforceName(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "XcMTg", true);
    }

    public static void isEnforceName(final android.content.ContextWrapper contextWrapper, final boolean enforceName) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "XcMTg", enforceName);
    }

    public static boolean isEncryptedWS(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "kaU5w", false);
    }

    public static void isEncryptedWS(final android.content.ContextWrapper contextWrapper, final boolean encrypted) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "kaU5w", encrypted);
    }

    public static String getEncryptedWSKey(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "YYTMLIMd8k", "0123456789ABCDEF");
    }

    public static void setEncryptedWSKey(final android.content.ContextWrapper contextWrapper, String encryptedWSKey) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "YYTMLIMd8k", encryptedWSKey);
    }

    public static boolean isLocalServer(final android.content.ContextWrapper contextWrapper) {
        if (FeatureSwitch.Disable_Local_Server) return false;
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "CPKIdU1vmIOA", false);
    }

    public static void isLocalServer(final android.content.ContextWrapper contextWrapper, final boolean bCalibrated) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "CPKIdU1vmIOA", bCalibrated);
    }


    public static int getFirstServer(final android.content.ContextWrapper contextWrapper) {

        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "kZXCERTTONDQ",0);
    }

    public static void setFirstServer(final android.content.ContextWrapper contextWrapper, final int servicePort) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "kZXCERTTONDQ", servicePort);
    }





    ///////////////////EOF WebSocket Preference


    ///////////////////Sensors Preference

    public static boolean isGyroCalibrated(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "GaZIGw", false);
    }

    public static void isGyroCalibrated(final android.content.ContextWrapper contextWrapper, final boolean bCalibrated) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "GaZIGw", bCalibrated);
    }

    /***
     * @param contextWrapper
     * @return comma separated of calibration values
     */
    public static double[] getGyroCalibratedValue(final android.content.ContextWrapper contextWrapper) {
        return Preference.readSavedPreference(contextWrapper, "szUqjaYJGmxbAtA", "0.0,0.0,0.0");
    }

    /***
     * @param contextWrapper
     * @param calibrationValues comma separated of calibration values
     */
    public static void setGyroCalibratedValue(final android.content.ContextWrapper contextWrapper, double[] calibrationValues) {
        String values = calibrationValues[0] + "," + calibrationValues[1] + "," + calibrationValues[2];
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "szUqjaYJGmxbAtA", values);
    }


    public static boolean isAccCalibrated(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "I0GLFDTPCpP1Kw", false);
    }

    public static void isAccCalibrated(final android.content.ContextWrapper contextWrapper, final boolean bCalibrated) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "I0GLFDTPCpP1Kw", bCalibrated);
    }

    /***
     * @param contextWrapper
     * @return comma separated of calibration values
     */
    public static double[] getAccCalibratedValue(final android.content.ContextWrapper contextWrapper) {
        return Preference.readSavedPreference(contextWrapper, "yJph626I0GL", "0.0,0.0,0.0");
    }

    /***
     * @param contextWrapper
     * @param calibrationValues comma separated of calibration values
     */
    public static void setAccCalibratedValue(final android.content.ContextWrapper contextWrapper, double[] calibrationValues) {
        String values = calibrationValues[0] + "," + calibrationValues[1] + "," + calibrationValues[2];
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "yJph626I0GL", values);
    }

    /***
     * @param contextWrapper
     * @return
     */
    public static double[] getAccZeroTilt(final android.content.ContextWrapper contextWrapper) {
        return Preference.readSavedPreference(contextWrapper, "dM4eS068o", "0.0,0.0,0.0");
    }

    /***
     * @param contextWrapper
     * @param zeroTiltValues
     */
    public static void setAccZeroTilt(final android.content.ContextWrapper contextWrapper, double[] zeroTiltValues) {
        String values = zeroTiltValues[0] + "," + zeroTiltValues[1] + "," + zeroTiltValues[2];
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "dM4eS068o", values);
    }


    public static boolean isMagCalibrated(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "XEQS2msaX", false);
    }

    public static void isMagCalibrated(final android.content.ContextWrapper contextWrapper, final boolean bCalibrated) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "XEQS2msaX", bCalibrated);
    }

    /***
     * @param contextWrapper
     * @return comma separated of calibration values
     */
    public static double[] getMagCalibratedValue(final android.content.ContextWrapper contextWrapper) {
        return Preference.readSavedPreference(contextWrapper, "COO326LFJQ", "0.0,0.0,0.0");
    }

    /***
     * @param contextWrapper
     * @param calibrationValues comma separated of calibration values
     */
    public static void setMagCalibratedValue(final android.content.ContextWrapper contextWrapper, double[] calibrationValues) {
        String values = calibrationValues[0] + "," + calibrationValues[1] + "," + calibrationValues[2];
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "COO326LFJQ", values);
    }


    public static int getPreferredUnits(final android.content.ContextWrapper contextWrapper) {
        return Integer.parseInt(SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "frOdcS6H9DA", String.valueOf(Constants.Preferred_UNIT_METRIC_SYSTEM)));
    }

    public static void setPreferredUnits(final android.content.ContextWrapper contextWrapper, final int unitType) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "frOdcS6H9DA", String.valueOf(unitType));
    }

    /////////////////// EOFSensors Preference

    ///////////////////WebSocket Preference



    public static boolean isGPSInjecttionEnabled(final android.content.ContextWrapper contextWrapper) {
        if (FeatureSwitch.Disable_Drone_ExternalCam) return false;
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "LNScs17Oks", true);
    }

    public static void isGPSInjecttionEnabled(final android.content.ContextWrapper contextWrapper, boolean bEnabled) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "LNScs17Oks", bEnabled);
    }

    // a Drone property

    public static int getSmartMavlinkTelemetry(final android.content.ContextWrapper contextWrapper) {
        return Integer.parseInt(SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "Hdl8aR1dQUy" , String.valueOf(Constants.SMART_TELEMETRY_LEVEL_2)));
    }



    public static void setSmartMavlinkTelemetry(final android.content.ContextWrapper contextWrapper, final int value) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "Hdl8aR1dQUy" , String.valueOf(value));
    }

    /***
     * Circle Radius in meters
     * @param contextWrapper
     * @return
     */
    public static int getDefaultCircleRadius(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "bktnaD5SQ" , 30);
    }



    public static void setDefaultCircleRadius(final android.content.ContextWrapper contextWrapper, final int value) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "bktnaD5SQ" , value);
    }

    /***
     * Altitude in meters
     * @param contextWrapper
     * @return
     */
    public static int getDefaultClimbAlt(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "XxyZpawJOE", 30);
    }



    public static void setDefaultClimbAlt(final android.content.ContextWrapper contextWrapper, final int value) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "XxyZpawJOE", value);
    }

    /////////////////////////////////
    //////////////////// TRacking Preference

    public static int getTrackingValue(final android.content.ContextWrapper contextWrapper, final int paramNumer) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "BP18211SjU" + paramNumer, 0);
    }



    public static void setTrackingValue(final android.content.ContextWrapper contextWrapper, final int paramNumer, final int value) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "BP18211SjU" + paramNumer, value);
    }

    public static void setTrackingValue(final android.content.ContextWrapper contextWrapper, final int paramNumer, final String value) {
        setTrackingValue(contextWrapper,paramNumer, Integer.parseInt(value));
    }

        /////////////////////////////////
    //////////////////// FPV Preference

    public static int getMobileDirection(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "fA1jOGA", Surface.ROTATION_0);
    }

    public static void setMobileDirection(final android.content.ContextWrapper contextWrapper, final int RotationAngle) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "fA1jOGA", RotationAngle);
    }

    /**
     * Rotation value follows Surface.Rotation NOT ActivityInfo.SCREEN_ORIENTATION and is being translated via setScreenOrientation() in FPV activity
     *
     * @param contextWrapper
     * @return
     */
    public static int getFPVActivityRotation(final android.content.ContextWrapper contextWrapper) {
        return Integer.parseInt(SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "OkWrZGmycoEKtw", String.valueOf(Surface.ROTATION_0)));
    }

    /**
     * Rotation value follows Surface.Rotation NOT ActivityInfo.SCREEN_ORIENTATION and is being translated via setScreenOrientation() in FPV activity
     *
     * @param contextWrapper
     * @param RotationAngle
     */
    public static void setFPVActivityRotation(final android.content.ContextWrapper contextWrapper, final int RotationAngle) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "OkWrZGmycoEKtw", String.valueOf(RotationAngle));
    }


    /***
     * Use External Camera source such as IP Webcam
     *
     * @param contextWrapper
     * @return
     */
    public static boolean useExternalCam(final android.content.ContextWrapper contextWrapper) {
        if (FeatureSwitch.Disable_Drone_ExternalCam) return false;
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "LNScs17Ok", false);
    }

    /***
     * Use External Camera source such as IP Webcam
     *
     * @param contextWrapper
     * @param bEnabled
     */
    public static void useExternalCam(final android.content.ContextWrapper contextWrapper, boolean bEnabled) {
        if (FeatureSwitch.Disable_Drone_ExternalCam) {
            bEnabled = false;
        }
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "LNScs17Ok", bEnabled);
    }


    /***
     * doMirror Image of Drone WebRTC
     *
     * @param contextWrapper
     * @return
     */
    public static boolean getRTCCamMirrored(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "Nzz7jbzi", true);
    }

    /***
     * doMirror Image of Drone WebRTC
     *
     * @param contextWrapper
     * @param bEnabled
     */
    public static void setRTCCamMirrored(final android.content.ContextWrapper contextWrapper, final boolean bEnabled) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "Nzz7jbzi", bEnabled);
    }



    public static final int SCREEN_ORIENTATION_LANDSCAPE = 0;
    public static final int SCREEN_ORIENTATION_PORTRAIT = 1;
    /***
     * Camera Rotation 0,90,180,270 Degree
     *
     * @param contextWrapper
     * @return
     */
    public static int getRTCCamRotateCAM(final android.content.ContextWrapper contextWrapper) {
        return Integer.parseInt(SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "KvLOEuLDz25cJg",String.valueOf(0)));
    }

    /***
     * Camera Rotation 0,90,180,270 Degree
     *
     * @param contextWrapper
     * @param degree  0,90,180,270 Degree
     */
    public static void setRTCCamRotateCAM(final android.content.ContextWrapper contextWrapper, final int degree) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "KvLOEuLDz25cJg", String.valueOf(degree));
    }







    /***
     * Use UDP protocol for Video if source and destination in the same LAN
     *
     * @param contextWrapper
     * @return
     */
    public static boolean useUDPCamera(final android.content.ContextWrapper contextWrapper) {

        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "MihUMPX2", true);
    }

    /***
     * Either Turn on Internal Camera or not... for both GCS & FPV
     *
     * @param contextWrapper
     * @param bEnabled
     */
    public static void useUDPCamera(final android.content.ContextWrapper contextWrapper, final boolean bEnabled) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "MihUMPX2", bEnabled);
    }

    /////////////////// EOFFPV Preference


    //////////////////////// HUB COMMUNICATION

    /***
     * Connect to FCB automatically using last configurations.
     * @param contextWrapper
     * @return
     */
    public static boolean isAutoFCBConnect(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "skiuLMsCOCFVqQ", false);
    }

    public static void isAutoFCBConnect(final android.content.ContextWrapper contextWrapper, final boolean bEnabled) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "skiuLMsCOCFVqQ", bEnabled);
    }

    /***
     * DOnt access IMU or GPS from mobile -- even dont initialize it
     * @param contextWrapper
     * @return
     */
    public static boolean isMobileSensorsDisabled(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "mePMWRUHZFwA", false);
    }

    public static void isMobileSensorsDisabled(final android.content.ContextWrapper contextWrapper, final boolean bEnabled) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "mePMWRUHZFwA", bEnabled);
    }

    public static int getSerialServerPort(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "Mk7t653F7xw", 5760);
    }

    public static void setSerialServerPort(final android.content.ContextWrapper contextWrapper, final int portNum) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "Mk7t653F7xw", portNum);
    }

    /***
     * Used as a selector with the combo box appeared in the USB screen
     *
     * @param contextWrapper
     * @return
     */
    public static int getFCBUSBBaudRateSelector(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "p1TJeVgTu", 1);
    }

    public static void setFCBUSBBaudRateSelector(final android.content.ContextWrapper contextWrapper, final int baudIndex) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "p1TJeVgTu", baudIndex);
    }

    public static boolean isFCBUSBFTDI(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "ABhz6SgD9w", false);
    }

    public static void isFCBUSBFTDI(final android.content.ContextWrapper contextWrapper, final boolean bEnabled) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "ABhz6SgD9w", bEnabled);
    }

    public static String getFCBBlueToothName(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "ZkukRz0hZIDzuw", "");
    }

    public static void setFCBBlueToothName(final android.content.ContextWrapper contextWrapper, String blueToothMAC) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "ZkukRz0hZIDzuw", blueToothMAC);
    }


    public static String getFCBBlueToothMAC(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "neeMEWx6dV", "");
    }

    /***
     *
     *
     * @param contextWrapper
     * @param blueToothMAC
     */
    public static void setFCBBlueToothMAC(final android.content.ContextWrapper contextWrapper, String blueToothMAC) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "neeMEWx6dV", blueToothMAC);
    }

    public static String getFCBDroneTCPServerIP(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "z9bAf1rB8kqB", "10.1.1.1");
    }

    public static void setFCBDroneTCPServerIP(final android.content.ContextWrapper contextWrapper, String tcpServerIP) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "z9bAf1rB8kqB", tcpServerIP);
    }

    public static String getFCBDroneTCPServerPort(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "fcb_tcpserverport", "5760");
    }

    public static void setFCBDroneTCPServerPort(final android.content.ContextWrapper contextWrapper, String tcpServerPort) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "fcb_tcpserverport", tcpServerPort);
    }

    public static String getFCBDroneUDPServerPort(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "pSKrg", "14550");
    }

    public static void setFCBDroneUDPServerPort(final android.content.ContextWrapper contextWrapper, String udpServerPort) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "pSKrg", udpServerPort);
    }


    public static final int FCB_LIB_NATIVE = 1;
    public static final int FCB_LIB_3DR    = 2;
    public static int getFCBTargetLib(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "GZdFGZjFlOHw", 1);
    }

    public static void setFCBTargetLib(final android.content.ContextWrapper contextWrapper, final int targetLIB) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "GZdFGZjFlOHw", targetLIB);
    }


    public static final int FCB_COM_BT      = 1;
    public static final int FCB_COM_USB     = 2;
    public static final int FCB_COM_TCP     = 3;
    public static final int FCB_COM_UDP     = 4;
    public static int getFCBTargetComm(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "a8TQ", FCB_COM_BT);
    }

    public static void setFCBTargetComm(final android.content.ContextWrapper contextWrapper, final int targetComm) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "a8TQ", targetComm);
    }
    //////////////////////// EOF HUB COMMUNICATION


    //////////////////////// SMS RECOVERY

    /***
     * Normally value is saved from sttings activity
     *
     * @param contextWrapper
     * @return
     *
     */
    public static String getRecoveryPhoneNo(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "ZB8vM05KAdg", "");
    }

    public static void setRecoveryPhoneNo(final android.content.ContextWrapper contextWrapper, String phoneNum) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "ZB8vM05KAdg", phoneNum);
    }

    /***
     * Normally value is saved from sttings activity
     *
     * @param contextWrapper
     * @return
     * @see
     */
    public static boolean isEmergencySMSGPSEnabled(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "ZOHYJWnn", false);
    }

    public static void isEmergencySMSGPSEnabled(final android.content.ContextWrapper contextWrapper, final boolean enableSMS) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "ZOHYJWnn", enableSMS);
    }


    public static void isEmergencyFlashEnabled(final android.content.ContextWrapper contextWrapper, final boolean enableFlash) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "Ua22l1asZBc3w", enableFlash);
    }

    public static boolean isEmergencyFlashEnabled(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "Ua22l1asZBc3w", false);
    }

    public static void isEmergencyFlightModeFailSafeEnabled(final android.content.ContextWrapper contextWrapper, final int flightmode) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "PUMKXwYrA", String.valueOf(flightmode));
    }

    public static int isEmergencyFlightModeFailSafeEnabled(final android.content.ContextWrapper contextWrapper) {
        return  Integer.parseInt(SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "PUMKXwYrA" , String.valueOf(FlightMode.CONST_FLIGHT_CONTROL_RTL)));
    }


    public static boolean isEmergencySirenEnabled(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "f7LyU1wAGk", false);
    }

    public static void isEmergencySirenEnabled(final android.content.ContextWrapper contextWrapper, final boolean enableSiren) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "f7LyU1wAGk", enableSiren);
    }




    public static boolean isGoogleAnalytics(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "Rluej0CzDJ", false);
    }

    public static void isGoogleAnalytics(final android.content.ContextWrapper contextWrapper, final boolean enableGA) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "Rluej0CzDJ", enableGA);
    }

    public static boolean enableGroupName(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "DJ6U5tOdtg", false);
    }

    public static void enableGroupName(final android.content.ContextWrapper contextWrapper, final boolean enableGName) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "DJ6U5tOdtg", enableGName);
    }


    /////////////////////RC Camera Switch
    public static void isRCCamEnabled(final android.content.ContextWrapper contextWrapper, final boolean rcBlockEnabled) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "sw_cam_rc_en", rcBlockEnabled);
    }

    public static boolean isRCCamEnabled(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "sw_cam_rc_en", false);
    }

    public static void setChannelRCCam(final android.content.ContextWrapper contextWrapper, final int channelNumber) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "sw_cam_rc_num", String.valueOf(channelNumber));
    }

    public static int getChannelRCCam(final android.content.ContextWrapper contextWrapper) {
        return Integer.parseInt(SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "sw_cam_rc_num" , "8"));
    }


    public static void setChannelRCCam_min_value(final android.content.ContextWrapper contextWrapper, final int minvalue) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "sw_cam_rc_pwm", String.valueOf(minvalue));
    }

    public static int getChannelRCCam_min_value(final android.content.ContextWrapper contextWrapper) {
        return Integer.parseInt(SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "sw_cam_rc_pwm" , "1800"));
    }

    ///////////////////// FAILE SAFE RC BLOCK

    public static void isRCBlockEnabled(final android.content.ContextWrapper contextWrapper, final boolean rcBlockEnabled) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "RFdWXmZaN0", rcBlockEnabled);
    }

    public static boolean isRCBlockEnabled(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "RFdWXmZaN0", false);
    }

    public static void setChannelRCBlock(final android.content.ContextWrapper contextWrapper, final int channelNumber) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "p7wCvhb2Akm", String.valueOf(channelNumber));
    }

    public static int getChannelRCBlock(final android.content.ContextWrapper contextWrapper) {
        return Integer.parseInt(SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "p7wCvhb2Akm" , "8"));
    }


    public static void setChannelRCBlock_min_value(final android.content.ContextWrapper contextWrapper, final int minvalue) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "xokpINK9PECd", String.valueOf(minvalue));
    }

    public static int getChannelRCBlock_min_value(final android.content.ContextWrapper contextWrapper) {
        return Integer.parseInt(SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "xokpINK9PECd" , "1800"));
    }

    public static void setBattery_min_value(final android.content.ContextWrapper contextWrapper, final int minvalue) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "WiDVQ", String.valueOf(minvalue));
    }

    public static int getBattery_min_value(final android.content.ContextWrapper contextWrapper) {
        return Integer.parseInt(SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "WiDVQ" , "0"));
    }
    ///////////////////////////////////// EOF FAIL SAFE RC BLOCK

    /***
     * RE Remote Control Settings
     */
    public static boolean isChannelReversed(final android.content.ContextWrapper contextWrapper, final int channelNumber) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "Er2Q9vfTe" + channelNumber, false);
    }

    public static void isChannelReversed(final android.content.ContextWrapper contextWrapper, final int channelNumber, final boolean isReversed) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "Er2Q9vfTe" + channelNumber, isReversed);
    }

    public static boolean isChannelReturnToCenter(final android.content.ContextWrapper contextWrapper, final int channelNumber) {
        boolean defaultRTC = channelNumber != ControlBoardBase.CONST_CHANNEL_3_THROTTLE;

        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "Fe30ugi3cx6NAt7A" + channelNumber, defaultRTC);
    }

    public static void isChannelReturnToCenter(final android.content.ContextWrapper contextWrapper, final int channelNumber, final boolean isRTC) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "Fe30ugi3cx6NAt7A" + channelNumber, isRTC);
    }

    /***
     * Returns all RTC settings of remote control
     * @param contextWrapper
     * @return
     */
    public static boolean[] isChannelReturnToCenter(final android.content.ContextWrapper contextWrapper) {

        boolean[] rtc = new boolean[8];

        for (int i=0;i<8;++i)
        {
            rtc[i] = isChannelReturnToCenter(contextWrapper,i);
        }
        return rtc;
    }


    public static int getChannelminValue(final android.content.ContextWrapper contextWrapper, final int channelNumber) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "FaEiCLEc" + channelNumber, Constants.Default_RC_MIN_VALUE);
    }

    public static void setChannelminValue(final android.content.ContextWrapper contextWrapper, final int channelNumber, final int minValue) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "FaEiCLEc" + channelNumber, minValue);
    }


    public static int getChannelmaxValue(final android.content.ContextWrapper contextWrapper, final int channelNumber) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "eTXXPLV" + channelNumber, Constants.Default_RC_MAX_VALUE);
    }

    public static void setChannelmaxValue(final android.content.ContextWrapper contextWrapper, final int channelNumber, final int maxValue) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "eTXXPLV" + channelNumber, maxValue);
    }

    public static int getRemoteFlightMode(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "WR3K280y7ES8Jp", 2);
    }

    public static void setRemoteFlightMode(final android.content.ContextWrapper contextWrapper, final int flightMode) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "WR3K280y7ES8Jp", flightMode);
    }

    public static int getChannelDRValues(final android.content.ContextWrapper contextWrapper, final int channelNumber) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "hYG40eIV2" + channelNumber, 100);
    }

    public static void setChannelDRValues(final android.content.ContextWrapper contextWrapper, final int channelNumber, final int drValue) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "hYG40eIV2" + channelNumber, drValue);
    }


    /// GAME PAD


    public static float getGamePadChannelmaxValue(final android.content.ContextWrapper contextWrapper, final int channelNumber) {
        final String res = SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "IglpbCi" + channelNumber, "0.0f");
        try
        {
            return Float.parseFloat(res);
        }
        catch (Exception e)
        {
            return 0.0f;
        }
    }

    public static void setGamePadChannelmaxValue(final android.content.ContextWrapper contextWrapper, final int channelNumber, final float maxValue) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "IglpbCi" + channelNumber,  String.valueOf(maxValue));
    }

    public static float getGamePadChannelminValue(final android.content.ContextWrapper contextWrapper, final int channelNumber) {
        final String res =  SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "bCiIgkWiGC" + channelNumber, "0.0f");
        try
        {
            return Float.parseFloat(res);
        }
        catch (Exception e)
        {
            return 0.0f;
        }
    }

    public static void setGamePadChannelminValue(final android.content.ContextWrapper contextWrapper, final int channelNumber, final float minValue) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "bCiIgkWiGC" + channelNumber,  String.valueOf(minValue));
    }





    public static boolean useStreamVideoHD(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "BDhGXszAC", false);
    }

    public static void useStreamVideoHD(final android.content.ContextWrapper contextWrapper, final boolean value) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "BDhGXszAC" , value);
    }

    public static boolean useLocalStunServerOnly(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "AxU1KiBEH2w", false);
    }

    public static void useLocalStunServerOnly(final android.content.ContextWrapper contextWrapper, final boolean value) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "AxU1KiBEH2w" , value);
    }


    public static String getSTUNServer(final android.content.ContextWrapper contextWrapper) {
        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "ufcQwjT6FEe", "");
    }

    public static void setSTUNServer(final android.content.ContextWrapper contextWrapper, String stunServer) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "ufcQwjT6FEe", stunServer);
    }

    public static int getCameraNumber(final android.content.ContextWrapper contextWrapper) {
        return  Integer.parseInt(SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "MwESFWhA", String.valueOf(0)));
    }

    public static void setCameraNumber(final android.content.ContextWrapper contextWrapper, final int cameraNum) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "MwESFWhA", String.valueOf(cameraNum));
    }



    /***
     * Define Preview Image QUality
     * @param contextWrapper
     * @return 0: LOW (320x240) <br>1: Medium (640x480) <br>2: HD (960x720) <br>3: FHD (1920x1080)<br>4: MAX Resolution
     *                           <br> {@link FeatureSwitch#cameraHeights}  & {@link FeatureSwitch#cameraWidths}
     */
    public static int getVideoImageSizeQuality(final android.content.ContextWrapper contextWrapper) {


        return Integer.parseInt(SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "Hd1cvoCA", String.valueOf(0)));

    }

    /***
     *
     * @param contextWrapper
     * @param imageSizeQuality   0: LOW (320x240) <br>1: Medium (640x480) <br>2: HD (960x720) <br>3: FHD (1920x1080)<br>4: MAX Resolution
     *                           <br> {@link FeatureSwitch#cameraHeights}  & {@link FeatureSwitch#cameraWidths}
     */
    public static void setVideoImageSizeQuality(final android.content.ContextWrapper contextWrapper, final int imageSizeQuality) {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "Hd1cvoCA", String.valueOf(imageSizeQuality));
    }


    /////////////////// EOFFPV Preference




    ////////////////// Wizard ActivityMosa3ed Preference


    public static void gui_ShowWebRTCWarrning(final android.content.ContextWrapper contextWrapper, final boolean enabled)
    {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "qjHpePDoXU", enabled);
    }

    public static boolean gui_ShowWebRTCWarrning(final android.content.ContextWrapper contextWrapper) {

        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "qjHpePDoXU",  true);
    }

    public static void gui_ShowAndruavModeDialog(final android.content.ContextWrapper contextWrapper, final boolean enabled)
    {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "kpgTRwDho", enabled);
    }

    public static boolean gui_ShowAndruavModeDialog(final android.content.ContextWrapper contextWrapper) {

        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "kpgTRwDho",  true);
    }

    public static void gui_ShowAndruav3DRNotice(final android.content.ContextWrapper contextWrapper, final boolean enabled)
    {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "DhokmVexXWQr", enabled);
    }

    public static boolean gui_ShowAndruav3DRNotice(final android.content.ContextWrapper contextWrapper) {

        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "DhokmVexXWQr",  true);
    }


    ////////////////////////////////////


    ////////////////// DEBUGGUNG PREFERENCE

    public static void isAndruavLogEnabled(final android.content.ContextWrapper contextWrapper, final boolean enabled)
    {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, "ejUkyLdMe0", enabled);
    }

    public static boolean isAndruavLogEnabled(final android.content.ContextWrapper contextWrapper) {

        return SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, "ejUkyLdMe0",  true);
    }



    public static void setPreference(final android.content.ContextWrapper contextWrapper, String Key, final String value)
    {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, Key, value);
    }

    public static void setPreference(final android.content.ContextWrapper contextWrapper, String Key, final int value)
    {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, Key, value);
    }

    public static void setPreference(final android.content.ContextWrapper contextWrapper, String Key, final boolean value)
    {
        SharedPreferenceHelper.writeSavedPreference(PREFS_COUNT, contextWrapper, Key, value);
    }



    ///////////////// EOF Debugging Preference

    public static double[] readSavedPreference(final android.content.ContextWrapper contextWrapper, final String propertyName, final String defaultValue) {
        final String res = SharedPreferenceHelper.readSavedPreference(PREFS_COUNT, contextWrapper, propertyName, defaultValue);
        final String[] resarray = StringSplit.fastSplit(res, ',');
        final double[] doubleValues = new double[3];
        doubleValues[0] = Double.parseDouble(resarray[0]);
        doubleValues[1] = Double.parseDouble(resarray[1]);
        doubleValues[2] = Double.parseDouble(resarray[2]);
        return doubleValues;
    }

    public static void FactoryReset(final android.content.ContextWrapper contextWrapper) {
        Preference.isAutoStart(contextWrapper, false);
        FactoryReset_Connections(contextWrapper);
        FactoryReset_Tracker(contextWrapper);
        FactoryReset_Sensors(contextWrapper);
        FactoryReset_Login(contextWrapper);
        FactoryReset_Misc(contextWrapper);
        FactoryReset_RC(contextWrapper);
        FactoryReset_Telemetry(contextWrapper);
        FactoryReset_FPV(contextWrapper);
        FactoryReset_GUIWizard(contextWrapper);

    }

    /***
     * Retrieve original Andruav Settings for login
     * <br>empty email & AccessCode
     * <br>isGCS = false
     * <br>isLocalServer = false
     * @param contextWrapper
     */
    public static void FactoryReset_Login(final android.content.ContextWrapper contextWrapper) {
        Preference.isLoginAuto(contextWrapper, false);
        Preference.setLoginUserName(contextWrapper, "");
        Preference.setLoginAccessCode(contextWrapper, "");
        Preference.isGCS(contextWrapper, true);
        Preference.setVehicleType(null, VehicleTypes.VEHICLE_UNKNOWN);

    }

    public static void FactoryReset_Connections(final android.content.ContextWrapper contextWrapper) {
        Preference.setWebServerGroupName(contextWrapper, AndruavEngine.getPreference().getContext().getString(R.string.pref_groupname));
        Preference.setWebServerUserName(contextWrapper, Build.MODEL);
        Preference.setAuthServerURL(contextWrapper, AndruavEngine.getPreference().getContext().getString(R.string.pref_auth_URL));
        Preference.setAuthServerPort(contextWrapper, AndruavEngine.getPreference().getContext().getResources().getInteger(R.integer.pref_auth_Port));
        Preference.setWebServerURL(contextWrapper, AndruavEngine.getPreference().getContext().getString(R.string.pref_ws_URL));
        Preference.setWebServerPort(contextWrapper, AndruavEngine.getPreference().getContext().getResources().getInteger(R.integer.pref_ws_Port));
        Preference.setFirstServer(null, 0);
        Preference.isLocalServer(null, false);
        Preference.isEnforceName(contextWrapper, true);
        Preference.isEncryptedWS(contextWrapper, false);
        Preference.setEncryptedWSKey(contextWrapper, "0123456789ABCDEF");


        Preference.setSerialServerPort(contextWrapper, AndruavEngine.getPreference().getContext().getResources().getInteger(R.integer.pref_hub_Port));
        Preference.isAutoFCBConnect(contextWrapper, false);
        Preference.isMobileSensorsDisabled(contextWrapper, false);


        Preference.setFCBBlueToothName(contextWrapper, "");
        Preference.setFCBBlueToothMAC(contextWrapper, "");
        Preference.setFCBUSBBaudRateSelector(contextWrapper, -1);
        Preference.setFCBDroneTCPServerIP(contextWrapper, "10.1.1.1");
        Preference.setFCBDroneTCPServerPort(contextWrapper, "5760");
        Preference.setFCBDroneUDPServerPort(contextWrapper, "14550");
        Preference.setFCBTargetLib(contextWrapper, FCB_LIB_3DR);
        Preference.setFCBTargetComm(contextWrapper, FCB_COM_BT);

    }


    public static void FactoryReset_Tracker(final android.content.ContextWrapper contextWrapper)
    {
        Preference.setTrackingValue(contextWrapper,Constants.CONST_TRACKING_INDEX_X_P,50);
        Preference.setTrackingValue(contextWrapper,Constants.CONST_TRACKING_INDEX_X_I,10);
        Preference.setTrackingValue(contextWrapper,Constants.CONST_TRACKING_INDEX_X_D,1);
        Preference.setTrackingValue(contextWrapper,Constants.CONST_TRACKING_INDEX_Y_P,70);
        Preference.setTrackingValue(contextWrapper,Constants.CONST_TRACKING_INDEX_Y_I,15);
        Preference.setTrackingValue(contextWrapper,Constants.CONST_TRACKING_INDEX_Y_D,1);
    }

    public static void FactoryReset_Sensors(final android.content.ContextWrapper contextWrapper) {
        Preference.isAccCalibrated(contextWrapper, false);
        Preference.isGyroCalibrated(contextWrapper, false);
        Preference.isGyroCalibrated(contextWrapper, false);

        final double[] d = {0.0f, 0.0f, 0.0f};
        Preference.setAccCalibratedValue(contextWrapper, d);
        Preference.setGyroCalibratedValue(contextWrapper, d);
        Preference.setMagCalibratedValue(contextWrapper, d);
        Preference.setPreferredUnits(contextWrapper, Constants.Preferred_UNIT_METRIC_SYSTEM);

        Preference.setAccZeroTilt(contextWrapper, d);
        Preference.setMobileDirection(contextWrapper, Surface.ROTATION_0);


    }


    public static void FactoryReset_Misc(final android.content.ContextWrapper contextWrapper) {
        Preference.setRecoveryPhoneNo(contextWrapper, "");
        Preference.setBattery_min_value(contextWrapper, 0);
        Preference.isEmergencySMSGPSEnabled(contextWrapper, false);
        Preference.isEmergencyFlightModeFailSafeEnabled(contextWrapper, FlightMode.CONST_FLIGHT_CONTROL_RTL);
        Preference.isEmergencySMSGPSEnabled(contextWrapper, false);
        Preference.isEmergencyFlashEnabled(contextWrapper, false);
        Preference.isEmergencySirenEnabled(contextWrapper, false);
        Preference.isGoogleAnalytics(contextWrapper, false);
        Preference.enableGroupName(contextWrapper, false);
        Preference.isAndruavLogEnabled(contextWrapper, true);
        Preference.setModuleType(contextWrapper,ProtocolHeaders.UAVOS_COMM_MODULE_CLASS);
        Preference.setCommModuleIP(contextWrapper,"");
        Preference.isCommModuleIPAutoDetect(contextWrapper,false);
    }


    public static void FactoryReset_Telemetry (final android.content.ContextWrapper contextWrapper)
    {
        Preference.setSmartMavlinkTelemetry(contextWrapper, Constants.SMART_TELEMETRY_LEVEL_2);
        Preference.setDefaultCircleRadius(contextWrapper, 30);
        Preference.setDefaultClimbAlt(contextWrapper, 30);
        Preference.isGPSInjecttionEnabled( contextWrapper, true);

    }

    public static void FactoryReset_FPV(final android.content.ContextWrapper contextWrapper) {
        Preference.useExternalCam(contextWrapper, false);
        Preference.setRTCCamRotateCAM(contextWrapper, 0);
        Preference.setRTCCamMirrored(contextWrapper, false);
        Preference.useUDPCamera(contextWrapper, true);
        Preference.setVideoImageSizeQuality(contextWrapper, 0);
        Preference.useStreamVideoHD(contextWrapper, true);
        Preference.useLocalStunServerOnly(contextWrapper, false);
        Preference.setSTUNServer(contextWrapper, "");
        Preference.setCameraNumber(contextWrapper, 0); //backFacingCam
        Preference.setFPVActivityRotation(contextWrapper, Surface.ROTATION_0);
    }

    public static void FactoryReset_RC(final android.content.ContextWrapper contextWrapper) {
        for (int i = 1; i <= 4; i = i + 1) {
            Preference.setChannelmaxValue(contextWrapper, i, Constants.Default_RC_MAX_VALUE);
            Preference.setChannelminValue(contextWrapper, i, Constants.Default_RC_MIN_VALUE);
            Preference.isChannelReturnToCenter(contextWrapper, i, true);
            Preference.isChannelReversed(contextWrapper, i, false);
            Preference.setChannelDRValues(contextWrapper, i, Constants.Default_RC_DR_VALUE);

        }

        for (int i = 5; i <= 8; i = i + 1) {
            Preference.setChannelmaxValue(contextWrapper, i, 2000);
            Preference.setChannelminValue(contextWrapper, i, 1000);
            Preference.isChannelReturnToCenter(contextWrapper, i, false);
            Preference.isChannelReversed(contextWrapper, i, false);
        }
        // Keep throttle from return back
        Preference.isChannelReturnToCenter(contextWrapper, ControlBoardBase.CONST_CHANNEL_3_THROTTLE, false);

        setRemoteFlightMode(contextWrapper, 2);

        // GCS RemoteControl GamePad
        for (int i = 0 ; i < 8; i = i + 1) {
        Preference.setGamePadChannelmaxValue(contextWrapper,i, 0.0f);
        Preference.setGamePadChannelminValue(contextWrapper,i, 0.0f);
        }



        Preference.isRCBlockEnabled(contextWrapper,false);
        Preference.setChannelRCBlock(contextWrapper,8);
        Preference.setChannelRCBlock_min_value(contextWrapper,1800);

        Preference.isRCCamEnabled(contextWrapper,false);
        Preference.setChannelRCCam(contextWrapper,16);
        Preference.setChannelRCCam_min_value(contextWrapper,1800);

    }


    public static void FactoryReset_GUIWizard(final android.content.ContextWrapper contextWrapper) {
            Preference.gui_ShowWebRTCWarrning(contextWrapper,true);
            Preference.gui_ShowAndruavModeDialog(contextWrapper,true);
            Preference.gui_ShowAndruav3DRNotice(contextWrapper,true);
    }


}