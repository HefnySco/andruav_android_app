package ap.andruav_ap.communication.telemetry;

import android.location.Location;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.controlBoard.IControlBoard_Callback;
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

    /***
     * Builds a callback that logs the outcome of a FCBoard command issued from an SMS,
     * so success/failure/timeout is visible in the DAO log instead of being silently dropped
     * (e.g. FCBoard.do_X() no-ops with a null callback when the drone is disconnected or
     * RC-channel-blocked).
     */
    private IControlBoard_Callback logCallback (final String commandName, final String sender)
    {
        return new IControlBoard_Callback() {
            @Override
            public void OnSuccess() {
                AndruavEngine.log().log2("AndruavSMSClientParser", "sms_cmd", commandName + " succeeded, from=" + sender);
            }

            @Override
            public void OnFailue(int code) {
                AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", commandName + " failed (code=" + code + "), from=" + sender);
            }

            @Override
            public void OnTimeout() {
                AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", commandName + " timed out, from=" + sender);
            }
        };
    }

    @Override
    public void executeCommand (final String sender, final String sms_msg)
    {
        if (sender == null || sender.isEmpty()) {
            AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", "executeCommand skipped: empty sender, msg=" + sms_msg);
            return ;
        }

        // Split the input string by colon delimiter
        String[] parts = sms_msg.split(":");

        if (parts.length<2)
        {
            // bad command format.
            AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", "executeCommand skipped: bad format (no colon), from=" + sender + " msg=" + sms_msg);
            return ;
        }

        if (!parts[0].toUpperCase().contains("ATT"))
        {
            // bad command format.
            AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", "executeCommand skipped: missing ATT prefix, from=" + sender + " msg=" + sms_msg);
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
                AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", "AUTO skipped: bad step number, from=" + sender + " cmd=" + cmd);
                return;
            }
            if (AndruavSettings.andruavWe7daBase.FCBoard != null) {
                if (AndruavSettings.andruavWe7daBase.FCBoard.do_RCChannelBlocked()) {
                    AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", "AUTO skipped: RC channel blocked (manual override active), from=" + sender + " step=" + step);
                }
                AndruavSettings.andruavWe7daBase.FCBoard.do_SetCurrentMission(step);
                AndruavSettings.andruavWe7daBase.FCBoard.do_Auto(logCallback("AUTO " + step, sender));
            } else {
                AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", "AUTO skipped: no FCBoard, from=" + sender + " step=" + step);
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
                    if (AndruavSettings.andruavWe7daBase.FCBoard.do_RCChannelBlocked()) {
                        AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", "RTL skipped: RC channel blocked (manual override active), from=" + sender);
                    }
                    AndruavSettings.andruavWe7daBase.FCBoard.do_RTL(logCallback("RTL", sender));
                } else {
                    AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", "RTL skipped: no FCBoard, from=" + sender);
                }
                break;

            case "LND":
            case "LAND":
                if (AndruavSettings.andruavWe7daBase.FCBoard!= null) {
                    if (AndruavSettings.andruavWe7daBase.FCBoard.do_RCChannelBlocked()) {
                        AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", "LAND skipped: RC channel blocked (manual override active), from=" + sender);
                    }
                    AndruavSettings.andruavWe7daBase.FCBoard.do_Land(logCallback("LAND", sender));
                } else {
                    AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", "LAND skipped: no FCBoard, from=" + sender);
                }
                break;

            case "GOTO":
                break;

            case "LOC": {
                final AndruavIMU andruavIMU = AndruavSettings.andruavWe7daBase.getActiveGPS();
                if (andruavIMU == null) {
                    AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", "LOC skipped: no active GPS, from=" + sender);
                    return;
                }

                if (andruavIMU.hasCurrentLocation()) {
                    AndruavEngine.getEmergency().sendSMSLocation(sender, true);
                } else {
                    AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", "LOC skipped: no current location, from=" + sender);
                }
            }
                break;

            default:
                AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip", "unknown command, from=" + sender + " cmd=" + cmd);
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

        SMS.sendSMS(sender, helpText);
    }
}
