package com.andruav.interfaces;

import android.text.Spanned;

/**
 * Created by M.Hefny on 08-Feb-15.
 */
public interface  INotification {
    // MAVLINK COMPLIANT
    /*
        0	MAV_SEVERITY_EMERGENCY	System is unusable. This is a "panic" condition.
        1	MAV_SEVERITY_ALERT	Action should be taken immediately. Indicates error in non-critical systems.
        2	MAV_SEVERITY_CRITICAL	Action must be taken immediately. Indicates failure in a primary system.
        3	MAV_SEVERITY_ERROR	Indicates an error in secondary/redundant systems.
        4	MAV_SEVERITY_WARNING	Indicates about a possible future error if this is not resolved within a given timeframe. Example would be a low battery warning.
        5	MAV_SEVERITY_NOTICE	An unusual event has occurred, though not an error condition. This should be investigated for the root cause.
        6	MAV_SEVERITY_INFO	Normal operational messages. Useful for logging. No action is required for these messages.
        7	MAV_SEVERITY_DEBUG	Useful non-operational messages that can assist in debugging. These should not occur during normal operation.
     */
    int NOTIFICATION_TYPE_ERROR = 3;
    int NOTIFICATION_TYPE_WARNING = 4;
    int NOTIFICATION_TYPE_NORMAL = 5;
    int NOTIFICATION_TYPE_GENERIC =6;


    int INFO_TYPE_REGISTRATION  = 22;
    int INFO_TYPE_TELEMETRY  = 33;
    int INFO_TYPE_PROTOCOL = 44;
    int INFO_TYPE_CAMERA = 55;
    int INFO_TYPE_KMLFILE = 66;
    int INFO_TYPE_Lo7etTa7akom = 77;
    int INFO_TYPE_GEOFENCE = 88;
    int INFO_TYPE_GPS = 99;
    int INFO_TYPE_NAVIGATIONLOGIC = 110;


    void displayNotification(int smallLogo, Spanned title, Spanned text, boolean Sound, int Id, boolean isPresistant);
    void displayNotification(int smallLogo, String title, String text, boolean Sound, int Id, boolean isPresistant);
    void displayNotification(String title, String text, boolean Sound, int Id, boolean isPresistant);
    void Cancel(int Id);

    void Speak(String message);
}
