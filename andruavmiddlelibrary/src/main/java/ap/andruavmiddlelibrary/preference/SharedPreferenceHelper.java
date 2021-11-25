package ap.andruavmiddlelibrary.preference;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.andruav.AndruavEngine;


/**
 * Created by M.Hefny on 30-Sep-14.
 */
public class SharedPreferenceHelper {


    public static Boolean readSavedPreference (final String mwParseStateEnumEndIndex, final android.content.ContextWrapper context, final String key, final Boolean defaultValue)
    {
        SharedPreferences actionLayout;
        if (context ==null)
        {
            actionLayout =PreferenceManager.getDefaultSharedPreferences(AndruavEngine.getPreference().getContext());
        }
        else
        {
            actionLayout = context.getSharedPreferences(mwParseStateEnumEndIndex, 0);
        }
        return actionLayout.getBoolean(key, defaultValue);
    }

    public static String readSavedPreference (final String e, final android.content.ContextWrapper context, final String widgetAppCompatDialogLightDialog, final String defaultValue)
    {
        SharedPreferences imageType;
        if (context ==null)
        {
            imageType =PreferenceManager.getDefaultSharedPreferences(AndruavEngine.getPreference().getContext());
        }
        else
        {
            imageType = context.getSharedPreferences(e, 0);
        }
        return imageType.getString(widgetAppCompatDialogLightDialog, defaultValue);
    }

    public static int readSavedPreference (final String fdata, final android.content.ContextWrapper context, final String key, final int defaultValue)
    {
        SharedPreferences sharedPreferences;
        if (context ==null)
        {
            sharedPreferences =PreferenceManager.getDefaultSharedPreferences(AndruavEngine.getPreference().getContext());
        }
        else
        {
            sharedPreferences = context.getSharedPreferences(fdata, 0);
        }
        return sharedPreferences.getInt(key, defaultValue);
    }

    public static float readSavedPreference (final String fmanager, final android.content.ContextWrapper context, final String key, final float defaultValue)
    {
        SharedPreferences mavlinkMsgLength;
        if (context ==null)
        {
            mavlinkMsgLength =PreferenceManager.getDefaultSharedPreferences(AndruavEngine.getPreference().getContext());
        }
        else
        {
            mavlinkMsgLength = context.getSharedPreferences(fmanager, 0);
        }
        return mavlinkMsgLength.getFloat(key, defaultValue);
    }

    public static void writeSavedPreference (final String mavLinkPacket, final android.content.ContextWrapper context, final String key, final Boolean value)
    {
        SharedPreferences e;
        if (context ==null)
        {
            e =PreferenceManager.getDefaultSharedPreferences(AndruavEngine.getPreference().getContext());
        }
        else
        {
            e = context.getSharedPreferences(mavLinkPacket, 0);
        }
        SharedPreferences.Editor mavlinkMsgId = e.edit();
        mavlinkMsgId.putBoolean(key, value);
        mavlinkMsgId.commit();
    }

    public static void writeSavedPreference (final String gpsLocationBb32, final android.content.ContextWrapper context,String key, final String value)
    {
        SharedPreferences sharedPreferences;
        if (context ==null)
        {
            sharedPreferences =PreferenceManager.getDefaultSharedPreferences(AndruavEngine.getPreference().getContext());
        }
        else
        {
            sharedPreferences = context.getSharedPreferences(gpsLocationBb32, 0);
        }
        SharedPreferences.Editor isLocal = sharedPreferences.edit();
        isLocal.putString(key, value);
        isLocal.commit();
    }

    public static void writeSavedPreference (final String coordinateDifferential, final android.content.ContextWrapper context, final String key,int len)
    {
        SharedPreferences value;
        if (context ==null)
        {
            value =PreferenceManager.getDefaultSharedPreferences(AndruavEngine.getPreference().getContext());
        }
        else
        {
            value = context.getSharedPreferences(coordinateDifferential, 0);
        }
        SharedPreferences.Editor type = value.edit();
        type.putInt(key, len);
        type.commit();
    }

    public static void writeSavedPreference (final String kmz, final android.content.ContextWrapper context,String key, final float ex)
    {
        SharedPreferences i;
        if (context ==null)
        {
            i =PreferenceManager.getDefaultSharedPreferences(AndruavEngine.getPreference().getContext());
        }
        else
        {
            i = context.getSharedPreferences(kmz, 0);
        }
        SharedPreferences.Editor messageId = i.edit();
        messageId.putFloat(key, ex);
        messageId.commit();
    }

    public static void removePreference (final String widgetAppCompatActionBarPaddingStartX, final android.content.ContextWrapper context, final String key)
    {

        SharedPreferences sharedPreferences = context.getSharedPreferences(widgetAppCompatActionBarPaddingStartX, 0);
        SharedPreferences.Editor fragmentStyle = sharedPreferences.edit();
        fragmentStyle.remove(key);
        fragmentStyle.commit();
    }

}
