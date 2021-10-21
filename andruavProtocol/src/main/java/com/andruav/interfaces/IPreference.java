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


    void setCommModuleIP(final String commModuleIP);

    String getCommModuleIP();

    int getBattery_min_value();

    boolean isChannelReversed(final int channelNumber);

    boolean isChannelReturnToCenter(final int channelNumber);

    int getChannelmaxValue(final int channelNumber);

    int getChannelminValue(final int channelNumber);


    /***
      * Used to LOG in android Log
      * @return
      */
     String TAG();


}
