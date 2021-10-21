package com.andruav.util;


import android.util.Log;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.FeatureSwitch;

/**
 * Created by hamada on 07/03/18.
 */

public class RemoteControl {

    /***
     *
     * @param scalledChannels channel values . channel range from [0,1000] .. IMPORTAANT  "-1  means release channel."
     * @return
     */
    public static int[] calculateChannels2(final int[] scalledChannels, final boolean ignoreDeadoBand)
    {
        int[] output=new int[8];
        for (int i=0;i<8;++i) {


            int scalledChannel = scalledChannels[i];
            if (scalledChannel == -1) {
                output[i] = 0;
                continue;
            }

            final double dr = AndruavSettings.RemoteControlDualRates[i] / 100.0;


            if (AndruavEngine.getPreference().isChannelReturnToCenter(i))
            {
                // RTC Enabled
                scalledChannel = scalledChannel - 500; // range from [-500,500]

                if ( (!ignoreDeadoBand) && (Math.abs(scalledChannel  ) < 20) )
                {
                    scalledChannels[i] = 0;
                }

                output[i] = scalledChannel ; //* ((2000 - 1000) / (Constants.Default_RC_RANGE_2));
                output[i] = (int) (output[i] * dr);
                if (AndruavEngine.getPreference().isChannelReversed( i)) {
                    output[i] = 1500  - output[i];
                } else {
                    output[i] = 1500  + output[i];
                }


            }
            else
            {
                // RTC Disabled
                scalledChannel = scalledChannel;       // range from [0,1000]
                output[i] = scalledChannel; // * ((2000-1000) / Constants.Default_RC_RANGE);
                output[i] = (int) (output[i] * dr);

                if (AndruavEngine.getPreference().isChannelReversed(i)) {
                    output[i] = 2000 - output[i];
                } else {
                    output[i] = output[i] + 1000;
                }

            }

            output[i] = Math.max(Math.min(AndruavEngine.getPreference().getChannelmaxValue(i) , output[i] ), AndruavEngine.getPreference().getChannelminValue(i));

            if (FeatureSwitch.DEBUG_MODE) {
                Log.d("RX:", String.format("CH #%d: org: %3d out: %3d", i, scalledChannels[i], output[i]));
            }
        }

        return output;
    }

}


