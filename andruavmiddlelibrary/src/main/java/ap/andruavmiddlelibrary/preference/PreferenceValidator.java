package ap.andruavmiddlelibrary.preference;

import com.andruav.AndruavSettings;

/**
 * Created by M.Hefny on 14-Oct-14.
 * Contains static functions that validate that preference has sufficient data for a given action.
 */
public class PreferenceValidator {

    public static boolean isInvalidLoginCode()
    {
        return (Preference.getLoginAccessCode(null).isEmpty());
    }

    /**
     * True if WS data is available
     * @return
     */
    public static boolean isValidWebSocket()
    {
        if (AndruavSettings.Account_SID.isEmpty()) return false;
        if (Preference.getWebServerUserName(null).isEmpty()) return false;
        if (Preference.getWebServerGroupName(null).isEmpty()) return false;
        if (Preference.getAuthServerURL(null).isEmpty()) return false;
        return !Preference.getWebServerURL(null).isEmpty();

    }



    public static boolean isValidIMU()
    {
        //TODO: FEATURE REVIEW ... you may need to change this later.
        if (Preference.isGCS(null)) return true; // no need for IMU settings if GCS

        if (!Preference.isAccCalibrated(null)) return false;
        if (!Preference.isGyroCalibrated(null)) return false;
        return Preference.isMagCalibrated(null);

    }


    public static boolean isValidWebRTC ()
    {
        if (Preference.useLocalStunServerOnly(null))
        {
            final String stunIP = Preference.getSTUNServer(null);
            return !stunIP.isEmpty() && !stunIP.equals("");

        }

        return true;
    }


}
