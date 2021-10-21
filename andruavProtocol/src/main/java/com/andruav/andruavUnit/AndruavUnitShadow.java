package com.andruav.andruavUnit;

import com.andruav.AndruavEngine;
import com.andruav.TelemetryProtocol;
import com.andruav.controlBoard.shared.common.FlightMode;

/**
 * Created by mhefny on 4/7/17.
 */

public class AndruavUnitShadow extends AndruavUnitBase {



    public AndruavUnitShadow(final String groupName, final String partyID, final boolean isGCS)
    {
        super (groupName,partyID,isGCS);

    }






    @Override
    public void setFlightModeFromBoard (final int flightMode)
    {
        final boolean changed = (flightMode != getFlightModeFromBoard());

        super.setFlightModeFromBoard(flightMode);
        if (isGUIActivated
                && changed
                // && !IsMe()  Shadow Can never be ME
                )
        {
            AndruavEngine.notification().Speak(this.UnitID + " flight mode is " + FlightMode.getFlightModeText(this.getFlightModeFromBoard()));
        }
    }


    @Override
    protected void Telemetry_protocol_changed(int telemetry_protocol)
    {

        disposeFCBBase();


        if (telemetry_protocol != TelemetryProtocol.TelemetryProtocol_No_Telemetry)
        {
//            if ((!this.getIsCGS()) )
//            {
//                this.FCBoardShadow = AndruavMo7arek.getLo7etTa7akomMasna3().getFlightControlBoardShadow(this);
//
//            }
        }


    }
}
