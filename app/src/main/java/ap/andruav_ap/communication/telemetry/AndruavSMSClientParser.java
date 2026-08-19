package ap.andruav_ap.communication.telemetry;

import android.location.Location;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.FeatureSwitch;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GPS;
import com.andruav.protocol.communication.sms.AndruavSMSClientParserBase;
import com.andruav.sensors.AndruavIMU;

import ap.andruav_ap.Emergency;
import ap.andruavmiddlelibrary.factory.communication.SMS;

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
        String cmd = parts[1].trim();

        // Handle "AUTO X" — engage auto mode and jump to mission step X
        String[] cmdTokens = cmd.split("\\s+");
        if (cmdTokens.length >= 2 && cmdTokens[0].equalsIgnoreCase("AUTO"))
        {
            int step;
            try {
                step = Integer.parseInt(cmdTokens[1]);
            } catch (NumberFormatException e) {
                return;
            }
            if (AndruavSettings.andruavWe7daBase.FCBoard != null) {
                AndruavSettings.andruavWe7daBase.FCBoard.do_SetCurrentMission(step);
                AndruavSettings.andruavWe7daBase.FCBoard.do_Auto(null);
            }
            return;
        }

        switch (cmd.toUpperCase())
        {
            case "HLP":
                sendHelp(sender);
                break;

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
                    AndruavEngine.getEmergency().sendSMSLocation(sender, true);
                }
            }
                break;

        }
    }

    /***
     * Replies to the sender with the list of supported SMS commands.
     * @param sender phone number to reply to.
     */
    private void sendHelp (final String sender)
    {
        final String helpText = "Andruav SMS Commands:\n"
                + "ATT:HLP - Show this help\n"
                + "ATT:RTL - Return to Launch\n"
                + "ATT:LND - Land\n"
                + "ATT:LOC - Send location\n"
                + "ATT:Auto X - Auto mission at step X";

        if (!FeatureSwitch.DEBUG_MODE) {
            SMS.sendSMS(sender, helpText);
        }
    }
}
