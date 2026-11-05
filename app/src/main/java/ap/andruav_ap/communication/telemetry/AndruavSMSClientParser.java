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

    /* AN-0127 — a sender number can be faked, so a command needs BOTH an
     * allow-listed number AND the configured PIN, in the form
     * ATT:<command>:<pin>. Fail closed: an empty contact list or an
     * empty PIN refuses everything, a refused sender never gets a reply,
     * and log lines never carry the PIN or the message text.
     * AN-0128 — contact entries may be "name=number"; the name goes into
     * the fact payload (never the number). keywords() is the configured
     * trigger keyword list, maxAgeS() the accepted message age.
     * Contacts/PIN come from the phone settings (Preference) by default;
     * tests inject their own provider via setGateConfigProvider. */
    public interface GateConfigProvider {
        java.util.List<String> contacts();
        String pin();
        default java.util.List<String> keywords() {
            return java.util.Collections.emptyList();
        }
        default long maxAgeS() {
            return DEFAULT_MAX_AGE_S;
        }
    }

    /** AN-0128: default accepted message age (seconds). */
    public static final long DEFAULT_MAX_AGE_S = 120L;

    private static GateConfigProvider sGateConfig = null;
    public static void setGateConfigProvider(final GateConfigProvider p) {
        sGateConfig = p;
    }

    private static GateConfigProvider gateConfig() {
        if (sGateConfig != null) return sGateConfig;
        return new GateConfigProvider() {
            @Override public java.util.List<String> contacts() {
                return ap.andruavmiddlelibrary.preference.Preference
                        .getSmsAllowedContacts(null);
            }
            @Override public String pin() {
                return ap.andruavmiddlelibrary.preference.Preference
                        .getSmsPin(null);
            }
            @Override public java.util.List<String> keywords() {
                return SmsKeywords.parse(
                        ap.andruavmiddlelibrary.preference.Preference
                                .getSmsKeywords(null));
            }
            @Override public long maxAgeS() {
                return ap.andruavmiddlelibrary.preference.Preference
                        .getSmsMaxAgeS(null);
            }
        };
    }

    /* AN-0128: where a matched keyword goes. DeAndroidModuleService
     * installs a listener that fires the fact through the running
     * PhoneProvider and clears it on stop; a null listener (or a false
     * return) means the event could not be delivered and the sender
     * gets a short failure reply. */
    public interface KeywordFactListener {
        boolean onKeyword(String keyword, String contact, long age_s);
    }

    private static volatile KeywordFactListener sKeywordListener = null;
    public static void setKeywordFactListener(final KeywordFactListener l) {
        sKeywordListener = l;
    }

    /** Reply sent when a keyword event cannot reach the bus. */
    private static final String KEYWORD_FAIL_REPLY =
            "Andruav: event not delivered (unit offline).";

    /* wrong-PIN rate limiting: 5 failures inside the window locks every
     * command (even correct ones) for the same window length */
    private static final int PIN_FAIL_MAX = 5;
    private static final long PIN_WINDOW_MS = 10L * 60L * 1000L;
    private static final java.util.List<Long> sPinFailures = new java.util.ArrayList<>();
    private static long sLockoutUntilMs = 0;

    /** test hook: clear the rate-limit state. */
    public static void resetPinFailures() {
        sPinFailures.clear();
        sLockoutUntilMs = 0;
    }

    private static boolean lockedOut(final long nowMs) {
        return nowMs < sLockoutUntilMs;
    }

    private static void notePinFailure(final long nowMs) {
        sPinFailures.add(nowMs);
        while (!sPinFailures.isEmpty()
                && sPinFailures.get(0) < nowMs - PIN_WINDOW_MS) {
            sPinFailures.remove(0);
        }
        if (sPinFailures.size() >= PIN_FAIL_MAX) {
            sLockoutUntilMs = nowMs + PIN_WINDOW_MS;
            sPinFailures.clear();
        }
    }

    private static void skip(final String reason, final String sender) {
        /* AN-0127: never log the PIN or the message text - only why the
         * command was refused and who sent it */
        AndruavEngine.log().log2("AndruavSMSClientParser", "sms_skip",
                reason + ", from=" + sender);
    }

    private static boolean senderAllowed(final String sender,
                                         final java.util.List<String> contacts) {
        if (contacts == null) return false;
        for (final String c : contacts) {
            if (numbersMatch(sender, SmsKeywords.entryNumber(c))) {
                return true;
            }
        }
        return false;
    }

    /* AN-0128: the configured name of the entry the sender matched -
     * what a fact may carry as "contact". Bare-number entries have no
     * name and yield "". The number itself never leaves the device. */
    private static String contactName(final String sender,
                                      final java.util.List<String> contacts) {
        if (contacts == null) return "";
        for (final String c : contacts) {
            if (numbersMatch(sender, SmsKeywords.entryNumber(c))) {
                return SmsKeywords.entryName(c);
            }
        }
        return "";
    }

    /* PhoneNumberUtils.compare is the device implementation; unit tests
     * run against the stub android.jar, which throws - fall back to an
     * exact match there. */
    private static boolean numbersMatch(final String a, final String b) {
        try {
            return android.telephony.PhoneNumberUtils.compare(a, b);
        } catch (RuntimeException e) {
            return a.equals(b);
        }
    }

    @Override
    public void executeCommand (final String sender, final String sms_msg)
    {
        /* No delivery timestamp: treated as fresh (the old pre-AN-0128
         * path had no age check at all). */
        executeCommand(sender, sms_msg, System.currentTimeMillis());
    }

    /**
     * AN-0128 form: timestampMs is SmsMessage.getTimestampMillis (the
     * SMSC time). A message older than the configured age is dropped -
     * SMS can arrive very late. timestampMs &lt;= 0 skips the check.
     */
    public void executeCommand (final String sender, final String sms_msg,
                                final long timestampMs)
    {
        if (sender == null || sender.isEmpty()) {
            skip("executeCommand skipped: empty sender", "");
            return ;
        }
        final long nowMs = System.currentTimeMillis();
        final GateConfigProvider cfg = gateConfig();
        final String pin = cfg.pin();
        if (cfg.contacts() == null || cfg.contacts().isEmpty()
                || pin == null || pin.isEmpty()) {
            skip("executeCommand skipped: no allow-list or PIN configured", sender);
            return ;
        }
        if (lockedOut(nowMs)) {
            skip("executeCommand skipped: PIN lockout", sender);
            return ;
        }
        if (!senderAllowed(sender, cfg.contacts())) {
            skip("executeCommand skipped: sender not allowed", sender);
            return ;
        }

        // Split the input string by colon delimiter
        String[] parts = sms_msg != null ? sms_msg.split(":") : new String[0];

        // ATT:<command>:<pin> - exact prefix, command, then the PIN
        if (parts.length < 3 || !parts[0].trim().equalsIgnoreCase("ATT")) {
            skip("executeCommand skipped: bad format (needs ATT:<cmd>:<pin>)", sender);
            return ;
        }
        if (!parts[parts.length - 1].trim().equals(pin)) {
            notePinFailure(nowMs);
            skip("executeCommand skipped: wrong PIN", sender);
            return ;
        }
        final long maxAgeMs = cfg.maxAgeS() * 1000L;
        if (timestampMs > 0 && maxAgeMs > 0 && nowMs - timestampMs > maxAgeMs) {
            skip("executeCommand skipped: message too old", sender);
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
                skip("AUTO skipped: bad step number", sender);
                return;
            }
            if (AndruavSettings.andruavWe7daBase.FCBoard != null) {
                if (AndruavSettings.andruavWe7daBase.FCBoard.do_RCChannelBlocked()) {
                    skip("AUTO skipped: RC channel blocked (manual override active) step=" + step, sender);
                    return;
                }
                AndruavSettings.andruavWe7daBase.FCBoard.do_SetCurrentMission(step);
                AndruavSettings.andruavWe7daBase.FCBoard.do_Auto(logCallback("AUTO " + step, sender));
            } else {
                skip("AUTO skipped: no FCBoard step=" + step, sender);
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
                        skip("RTL skipped: RC channel blocked (manual override active)", sender);
                        break;
                    }
                    AndruavSettings.andruavWe7daBase.FCBoard.do_RTL(logCallback("RTL", sender));
                } else {
                    skip("RTL skipped: no FCBoard", sender);
                }
                break;

            case "LND":
            case "LAND":
                if (AndruavSettings.andruavWe7daBase.FCBoard!= null) {
                    if (AndruavSettings.andruavWe7daBase.FCBoard.do_RCChannelBlocked()) {
                        skip("LAND skipped: RC channel blocked (manual override active)", sender);
                        break;
                    }
                    AndruavSettings.andruavWe7daBase.FCBoard.do_Land(logCallback("LAND", sender));
                } else {
                    skip("LAND skipped: no FCBoard", sender);
                }
                break;

            case "GOTO":
                break;

            case "LOC": {
                final AndruavIMU andruavIMU = AndruavSettings.andruavWe7daBase.getActiveGPS();
                if (andruavIMU == null) {
                    skip("LOC skipped: no active GPS", sender);
                    return;
                }

                if (andruavIMU.hasCurrentLocation()) {
                    AndruavEngine.getEmergency().sendSMSLocation(sender, true);
                } else {
                    skip("LOC skipped: no current location", sender);
                }
            }
                break;

            default: {
                /* AN-0128: a non-command token is a keyword candidate.
                 * Reserved names land in the switch above, so a keyword
                 * can never shadow a command. The fact rides the V2
                 * capability path via the listener the module service
                 * installs - with none attached the event is dropped,
                 * logged, and the sender gets a short failure reply. */
                final String keyword = SmsKeywords.match(cmd, cfg.keywords());
                if (keyword == null) {
                    skip("unknown command", sender);
                    return;
                }
                final KeywordFactListener listener = sKeywordListener;
                final long age_s = (timestampMs > 0)
                        ? Math.max(0L, (nowMs - timestampMs) / 1000L) : 0L;
                final String contact = contactName(sender, cfg.contacts());
                if (listener == null
                        || !listener.onKeyword(keyword, contact, age_s)) {
                    skip("keyword event dropped: no provider attached", sender);
                    sSmsSender.send(sender, KEYWORD_FAIL_REPLY);
                }
                return;
            }

        }
    }

    /** Sends the help reply through this seam - tests swap it to record. */
    public interface SmsSender { void send(String phoneNo, String msg); }
    private static SmsSender sSmsSender =
            (phoneNo, msg) -> SMS.sendSMS(phoneNo, msg);
    public static void setSmsSender(final SmsSender sender) {
        sSmsSender = sender != null ? sender
                : (phoneNo, msg) -> SMS.sendSMS(phoneNo, msg);
    }

    /***
     * Replies to the sender with the list of supported SMS commands.
     * @param sender phone number to reply to.
     */
    private void sendHelp (final String sender)
    {
        final String helpText = "Andruav SMS Commands (PIN required):\n"
                + "ATT:<cmd>:<PIN> - e.g. ATT:RTL:1234\n"
                + "ATT:HLP:<PIN> - Show this help\n"
                + "ATT:RTL:<PIN> - Return to Launch\n"
                + "ATT:LND:<PIN> - Land\n"
                + "ATT:LOC:<PIN> - Send location\n"
                + "ATT:Auto X:<PIN> - Auto mission at step X";

        sSmsSender.send(sender, helpText);
    }
}
