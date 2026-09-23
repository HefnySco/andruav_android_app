package com.andruav.interfaces;

import android.content.Context;

/**
 * Created by mhefny on 1/31/16.
 */
public interface IPreference {

    boolean isAndruavLogEnabled();

    String getVersionName();

    Context getContext();

    String getLoginUserName();

    String getModuleType();

    int getBattery_min_value();

    boolean isChannelReversed(final int channelNumber);

    boolean isChannelReturnToCenter(final int channelNumber);

    int getChannelmaxValue(final int channelNumber);

    int getChannelminValue(final int channelNumber);

    boolean getSendBackImages();

    int getSmartMavlinkTelemetry();

    /**
     * Feeds one UDP proxy send() result (duration in usec, negative on failure) into the
     * AUTO traffic-optimization controller. No-op unless AUTO mode is active.
     * @return true when the effective optimization level changed.
     */
    boolean recordUdpSendFeedback(final long durationUs);

    /**
     * Current effective traffic-optimization level (0-3), valid whether or not AUTO mode is active.
     */
    int getEffectiveTelemetryLevel();
    /***
      * Used to LOG in android Log
      * @return
      */
     String TAG();


}
