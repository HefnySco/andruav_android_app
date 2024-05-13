package ap.andruav_ap.communication.telemetry;

import android.location.Location;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GPS;
import com.andruav.protocol.communication.sms.AndruavSMSClientParserBase;
import com.andruav.sensors.AndruavIMU;

import ap.andruav_ap.Emergency;

public class AndruavSMSClientParser extends AndruavSMSClientParserBase {

    public AndruavSMSClientParser ()
    {
        super();

    }
    @Override
    public void executeCommand (final String sender, final String sms_msg)
    {
        if (sender == "") return ;

        // Split the input string by colon delimiter
        String[] parts = sms_msg.split(":");

        if (parts.length<2)
        {
            // bad command format.
            return ;
        }

        if (!parts[0].toUpperCase().contains("ATT"))
        {
            // bad command format.
            return ;
        }
        // Extract CMD field
        String cmd = parts[1];

        switch (cmd.toUpperCase())
        {
            case "RTL":
                if (AndruavSettings.andruavWe7daBase.FCBoard!= null) {
                    AndruavSettings.andruavWe7daBase.FCBoard.do_RTL(null);
                }
                break;

            case "LND":
            case "LAND":
                if (AndruavSettings.andruavWe7daBase.FCBoard!= null) {
                    AndruavSettings.andruavWe7daBase.FCBoard.do_Land(null);
                }
                break;

            case "GOTO":
                break;

            case "LOC": {
                final AndruavIMU andruavIMU = AndruavSettings.andruavWe7daBase.getActiveGPS();
                if (andruavIMU == null) return;

                if (andruavIMU.hasCurrentLocation()) {
                    ((Emergency) AndruavEngine.getEmergency()).sendSMSLocation(sender);
                }
            }
                break;

        }
    }
}
