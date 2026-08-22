package com.andruav.notification;

import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.protocol.R;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;
import com.andruav.interfaces.INotification;

import com.andruav.AndruavFacade;



/**
 * Created by M.Hefny on 22-Jul-15.
 * <br>This class is responsible of all PANIC and warning.
 *
 */
public class PanicFacade {

    private static String[] getErrorLevel () {
        return new String[] {
            AndruavEngine.AppContext.getString(R.string.err_level_error),
            AndruavEngine.AppContext.getString(R.string.err_level_error),
            AndruavEngine.AppContext.getString(R.string.err_level_error),
            AndruavEngine.AppContext.getString(R.string.err_level_error),
            AndruavEngine.AppContext.getString(R.string.err_level_warning),
            AndruavEngine.AppContext.getString(R.string.err_level_message),
            AndruavEngine.AppContext.getString(R.string.err_level_message)
        };
    }
    public static void cannotStartCamera ()
    {
        String error = AndruavEngine.getPreference().getContext().getString(R.string.andruav_error_cameraopen);
        AndruavEngine.notification().displayNotification(INotification.NOTIFICATION_TYPE_ERROR, getErrorLevel()[INotification.NOTIFICATION_TYPE_ERROR], error, true, INotification.INFO_TYPE_CAMERA, false);
        AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_CAMERA, INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_CAMERA, error, null);
        AndruavEngine.notification().Speak(error);

    }

    public static void cannotStartCamera (final int notification_Type, final int errorNumber, final String description, final String target)
    {
        AndruavEngine.notification().displayNotification(notification_Type, getErrorLevel()[notification_Type], description, true, INotification.INFO_TYPE_CAMERA, false);
        AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_CAMERA, notification_Type, errorNumber, description, null);
        AndruavEngine.notification().Speak(description);

    }


    public static void cannotDoAutopilotAction (final String error)
    {
        AndruavEngine.notification().displayNotification(INotification.NOTIFICATION_TYPE_ERROR, getErrorLevel()[INotification.NOTIFICATION_TYPE_ERROR], error, true, INotification.INFO_TYPE_Lo7etTa7akom, false);
        AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_Lo7etTa7akom, INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_Lo7etTa7akom, error, null);
        AndruavEngine.notification().Speak(error);

    }



    public static void andruavModuleAdded (final int notification_Type, final int errorNumber, final String description, final String target)
    {
        AndruavEngine.notification().displayNotification(notification_Type, getErrorLevel()[notification_Type], description, true, INotification.INFO_TYPE_CAMERA, false);
        AndruavEngine.notification().Speak(description);

    }

    public static void cannotDoAutopilotAction (final int notification_Type, final int errorNumber, final String description, final String target)
    {
        AndruavEngine.notification().displayNotification(notification_Type, getErrorLevel()[notification_Type], description, true, INotification.INFO_TYPE_CAMERA, false);
        AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_Lo7etTa7akom, notification_Type, errorNumber, description, null);
        AndruavEngine.notification().Speak(description);

    }




    public static void telemetryPanic(final int notification_Type, final int errorNumber, final String description, final AndruavUnitBase target)
    {

        AndruavEngine.notification().displayNotification(notification_Type, getErrorLevel()[notification_Type], description, true, INotification.INFO_TYPE_TELEMETRY, false);
        AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_TELEMETRY, notification_Type, errorNumber, description, target);
        AndruavEngine.notification().Speak(description);
    }


    public static void hitGEOFence (final String fenceName, final  boolean unsafe)
    {
        String menuText;

        if (unsafe)
        {
            menuText = AndruavEngine.AppContext.getString(R.string.andruav_error_geofence_unsafe);
            AndruavEngine.notification().displayNotification(INotification.NOTIFICATION_TYPE_WARNING, fenceName, menuText, true, INotification.INFO_TYPE_GEOFENCE, false);
            AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_GEOFENCE, INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_GEOFENCEERROR, menuText, null);
            AndruavEngine.notification().Speak(menuText);
        }
        else
        {
            menuText = AndruavEngine.AppContext.getString(R.string.andruav_error_geofence_safe);
            AndruavEngine.notification().displayNotification(INotification.NOTIFICATION_TYPE_NORMAL, fenceName, menuText, true, INotification.INFO_TYPE_GEOFENCE, false);
            AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_GEOFENCE, INotification.NOTIFICATION_TYPE_NORMAL, AndruavMessage_Error.ERROR_GEOFENCEERROR, menuText, null);
            AndruavEngine.notification().Speak(menuText);
        }
    }

    public static void gpsModeChanged (AndruavUnitBase andruavUnitBase)
    {
        String mode = "GPS";
        final int gpsmode = andruavUnitBase.getGPSMode();
        int type = INotification.NOTIFICATION_TYPE_NORMAL;
        switch (gpsmode)
        {
            case AndruavUnitBase.GPS_MODE_AUTO:
                mode = AndruavEngine.AppContext.getString(R.string.gps_mode_auto);
                type = INotification.NOTIFICATION_TYPE_NORMAL;
                break;
            case AndruavUnitBase.GPS_MODE_MOBILE:
                mode = AndruavEngine.AppContext.getString(R.string.gps_mode_mobile);
                break;
            case AndruavUnitBase.GPS_MODE_FCB:
                mode = AndruavEngine.AppContext.getString(R.string.gps_mode_fcb);
                break;

        }

        AndruavEngine.notification().displayNotification(INotification.NOTIFICATION_TYPE_WARNING, getErrorLevel()[type], mode, true, INotification.INFO_TYPE_GPS, false);
        AndruavEngine.notification().Speak(mode);

    }


    public static void cannotDoFollowAction (final String error)
    {
        AndruavEngine.notification().displayNotification(INotification.NOTIFICATION_TYPE_ERROR, getErrorLevel()[INotification.NOTIFICATION_TYPE_ERROR], error, true, INotification.INFO_TYPE_NAVIGATIONLOGIC, false);
        AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_NAVIGATIONLOGIC, INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_NAVIGATION, error, null);
        AndruavEngine.notification().Speak(error);

    }

    public static void confirmFollowAction (final String msg)
    {
        AndruavEngine.notification().displayNotification(INotification.NOTIFICATION_TYPE_NORMAL, getErrorLevel()[INotification.NOTIFICATION_TYPE_NORMAL], msg, true, INotification.INFO_TYPE_NAVIGATIONLOGIC, false);
        AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_NAVIGATIONLOGIC, INotification.NOTIFICATION_TYPE_NORMAL, AndruavMessage_Error.ERROR_NAVIGATION, msg, null);
        AndruavEngine.notification().Speak(msg);

    }


}
