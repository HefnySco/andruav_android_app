package com.andruav.andruavUnit;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.FeatureSwitch;
import com.andruav.TelemetryProtocol;
import com.andruav.protocol.BuildConfig;

/**
 * Created by mhefny on 4/7/17.
 */

public class AndruavUnitMe extends AndruavUnitBase {



    public AndruavUnitMe(final boolean isGCS)
    {
        super(true,isGCS);
    }

    /*
    BAD AREAS enta FAHEM
     */
    public boolean mIssue = false;

    /***
     *  mIsModule True: Camera, AI, FCB...etc.
     *  Note: COMM_MODULE is NOT MODULE here.
     */
    public boolean mIsModule = false;

    @Override
    protected void Telemetry_protocol_changed(int telemetry_protocol)
    {

        disposeFCBBase();


        if (telemetry_protocol != TelemetryProtocol.TelemetryProtocol_No_Telemetry)
        {
            if ((!this.IsCGS) )
            {

                if (this.FCBoard != null)
                {

                }

                 AndruavEngine.getLo7etTa7akomMasna3().getFlightControlBoard(this);

            }
        }
    }


}
