package ap.andruav_ap.helpers;

import android.util.Log;

import com.andruav.AndruavSettings;
import com.andruav.Constants;
import com.andruav.FeatureSwitch;

import ap.andruavmiddlelibrary.preference.Preference;

/**
 * Created by mhefny on 12/27/15.
 */
public class RemoteControl {


    /***
     * Loads dual rate data to @link AndruavSettings.RemoteControlDualRates[]
     *
     * <br>This function is used only in Drone mode. GCS does not need it as RemoteControl in GCS does not sendMessageToModule actual physical signal values.
     */
    public static void loadDualRates ()
    {
        for (int i=0;i<8;i=i+1) {

            AndruavSettings.RemoteControlDualRates[i] = Preference.getChannelDRValues(null, i);

        }
    }


    public static void loadRTC () {
        for (int i=0;i<8;i=i+1) {
            if (Preference.isChannelReturnToCenter(null,i)) {
                AndruavSettings.RemoteControlRTC = AndruavSettings.RemoteControlRTC + (0x01 << i);
            }

        }
    }

    /***
     * values from [0,1000] is mapped to [min,max] and RTC "return to center" is applied.
     * @param scalledChannels values comes from 0 to 1000 this is scaled values
     * @return
     */
    public static int[] calculateChannels(final int[] scalledChannels)
    {
        int[] output=new int[8];
        for (int i=0;i<8;++i) {
            final double dr = AndruavSettings.RemoteControlDualRates[i] / 100.0;
            int scalledChannel = scalledChannels[i];
            if (scalledChannels[i] == 0) {
                output[i] = 0;
                continue;
            }

            if (Preference.isChannelReturnToCenter(null,i))
            {
                // RTC Enabled
                scalledChannel = scalledChannel - 500; // range from [-500,500]
                output[i] = scalledChannel * ((Preference.getChannelmaxValue(null, i) - Preference.getChannelminValue(null, i)) / (Constants.Default_RC_RANGE_2));
                output[i] = (int) (output[i] * dr);
                if (Preference.isChannelReversed(null, i)) {
                    output[i] = (Preference.getChannelmaxValue(null, i) + Preference.getChannelminValue(null, i))/2  - output[i];
                } else {
                    output[i] = (Preference.getChannelmaxValue(null, i) + Preference.getChannelminValue(null, i))/2  + output[i];
                }


            }
            else
            {
                // RTC Disabled
                scalledChannel = scalledChannel;       // range from [0,1000]
                output[i] = scalledChannel * ((Preference.getChannelmaxValue(null, i) - Preference.getChannelminValue(null, i)) / Constants.Default_RC_RANGE);
                output[i] = (int) (output[i] * dr);

                if (Preference.isChannelReversed(null, i)) {
                    output[i] = Preference.getChannelmaxValue(null, i) - output[i];
                } else {
                    output[i] = output[i] + Preference.getChannelminValue(null, i);
                }
            }

            if (FeatureSwitch.DEBUG_MODE) {
                Log.d("RX:", String.format("CH #%d: org: %3d out: %3d", i, scalledChannels[i], output[i]));
            }
        }

        return output;
    }

    /***
     *
     * @param scalledChannels channel values . channel range from [1,1000] .. zero means release channel.
     * @return
     */
    public static int[] calculateChannels2(final int[] scalledChannels, final boolean ignoreDeadoBand)
    {
        int[] output=new int[8];
        for (int i=0;i<8;++i) {


            int scalledChannel = scalledChannels[i];
            if (scalledChannel == 0) {
                output[i] = 0;
                continue;
            }

            final double dr = AndruavSettings.RemoteControlDualRates[i] / 100.0;


            if (Preference.isChannelReturnToCenter(null,i))
            {
                // RTC Enabled
                scalledChannel = scalledChannel - 500; // range from [-500,500]

                if ( (!ignoreDeadoBand) && (Math.abs(scalledChannel  ) < 20) )
                {
                    scalledChannels[i] = 0;
                }

                output[i] = scalledChannel ; //* ((2000 - 1000) / (Constants.Default_RC_RANGE_2));
                output[i] = (int) (output[i] * dr);
                if (Preference.isChannelReversed(null, i)) {
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

                if (Preference.isChannelReversed(null, i)) {
                    output[i] = 2000 - output[i];
                } else {
                    output[i] = output[i] + 1000;
                }

            }
            //output[i] = Math.max(Math.min(Preference.getChannelmaxValue(null, i) , output[i] ), Preference.getChannelminValue(null, i));

            if (FeatureSwitch.DEBUG_MODE) {
                Log.d("RX:", String.format("CH #%d: org: %3d out: %3d", i, scalledChannels[i], output[i]));
            }
        }

        return output;
    }

    /***
     *
     * @param scalledChannels ranges from [0,1000]
     * @param ignoreDeadoBand
     * @return
     */
    public static int[] calculateChannels3(final int[] scalledChannels, final boolean ignoreDeadoBand)
    {
        int[] output=new int[8];
        for (int i=0;i<8;++i) {


            int scalledChannel = scalledChannels[i];
            if (scalledChannel == -999) {
                output[i] = 0;
                continue;
            }


                // RTC Enabled
                scalledChannel = scalledChannel - 500; // range from [-500,500]

                if ( (!ignoreDeadoBand) && (Math.abs(scalledChannel  ) < 20) )
                {
                    scalledChannel = 0;
                }

                final boolean ch_revers = Preference.isChannelReversed(null, i);

                if (ch_revers) {
                    scalledChannel = -scalledChannel;
                }

                output[i] = scaleInputToLimits (i, scalledChannel) + 1500;

            //output[i] = Math.max(Math.min(Preference.getChannelmaxValue(null, i) , output[i] ), Preference.getChannelminValue(null, i));

            if (FeatureSwitch.DEBUG_MODE) {
                Log.d("RX:", String.format("CH #%d: org: %3d out: %3d", i, scalledChannels[i], output[i]));
            }
        }

        return output;
    }


    /***
     *
     * @param channelNumber
     * @param value [-500 , 500]
     * @return
     */
    private static final int scaleInputToLimits (final int channelNumber, final int value)
    {
        final int min_value = 1500 - Preference.getChannelminValue(null, channelNumber);
        final int max_value = Preference.getChannelmaxValue(null, channelNumber) - 1500;

        if (value <= 0)
        {
            return (int)((min_value / 500.0f) * (value));
        }else
        {
            return (int)((max_value / 500.0f) * (value));
        }

    }

}
