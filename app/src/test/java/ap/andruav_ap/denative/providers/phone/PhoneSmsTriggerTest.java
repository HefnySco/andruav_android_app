package ap.andruav_ap.denative.providers.phone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitMe;
import com.andruav.controlBoard.ControlBoardBase;
import com.andruav.controlBoard.IControlBoard_Callback;
import com.andruav.interfaces.ILog;

import ap.andruav_ap.communication.telemetry.AndruavSMSClientParser;
import ap.andruav_ap.communication.telemetry.SmsKeywords;
import de.databus.caps.CapabilityHost;

/**
 * AN-0128: the SMS-keyword path through {@link AndruavSMSClientParser}
 * into {@link PhoneProvider#onSmsKeyword}. An allowed contact sending
 * {@code ATT:<keyword>:<pin>} fires {@code phone.sms_<kw>.received}
 * with {@code {contact, age_s}}; refused senders, wrong PINs, old
 * messages and unknown keywords fire nothing; with no provider
 * attached the drop is logged and the sender gets a failure reply.
 */
public class PhoneSmsTriggerTest {

    private static final String MOM_NUMBER = "+201001110005";
    private static final String STRANGER = "+201009876543";
    private static final String PIN = "4321";

    /*** Records which FCBoard commands the parser reached. */
    private static class FakeBoard extends ControlBoardBase {
        boolean rtl, land, auto;
        int missionStep = -1;

        FakeBoard(final AndruavUnitMe me) { super(me); }
        @Override public boolean do_RCChannelBlocked() { return false; }
        @Override public void do_RTL(final IControlBoard_Callback cb) { rtl = true; }
        @Override public void do_Land(final IControlBoard_Callback cb) { land = true; }
        @Override public void do_Auto(final IControlBoard_Callback cb) { auto = true; }
        @Override public void do_SetCurrentMission(final int n) { missionStep = n; }
    }

    /*** Records every log line. */
    private static class FakeLog implements ILog {
        final List<String> lines = new ArrayList<>();
        @Override public void logException(final String tag, final Exception e) {}
        @Override public void logException(final Exception e) {}
        @Override public void logException(final String u, final String tag, final Throwable e) {}
        @Override public void logException(final String u, final String tag, final Exception e) {}
        @Override public void logException(final String u, final String tag, final VirtualMachineError e) {}
        @Override public void log(final String u, final String tag, final String text) {
            lines.add(text);
        }
        @Override public void log2(final String u, final String tag, final String text) {
            lines.add(text);
        }
        @Override public void LogDeviceInfo(final String tag, final String text) {
            lines.add(text);
        }
    }

    /*** Records fired facts (ns.event name + payload). */
    private static class FakeHost extends CapabilityHost {
        final List<String> names = new ArrayList<>();
        final List<JSONObject> payloads = new ArrayList<>();

        FakeHost() { super(null, null); }

        @Override
        public String fireEvent(String ns, String ev, JSONObject payload) {
            names.add(ns + "." + ev);
            payloads.add(payload);
            return "test-nonce";
        }
    }

    private AndruavSMSClientParser mParser;
    private PhoneProvider mProvider;
    private FakeHost mHost;
    private FakeBoard mBoard;
    private FakeLog mLog;
    private final List<String[]> mSmsSent = new ArrayList<>();
    private long mMaxAgeS = AndruavSMSClientParser.DEFAULT_MAX_AGE_S;

    private void setGate(final List<String> contacts) {
        AndruavSMSClientParser.setGateConfigProvider(
                new AndruavSMSClientParser.GateConfigProvider() {
                    @Override public List<String> contacts() { return contacts; }
                    @Override public String pin() { return PIN; }
                    @Override public List<String> keywords() {
                        return SmsKeywords.parse("go, abort, rtl");
                    }
                    @Override public long maxAgeS() { return mMaxAgeS; }
                });
    }

    @Before
    public void setUp() {
        mParser = new AndruavSMSClientParser();
        final AndruavUnitMe me = new AndruavUnitMe();
        mBoard = new FakeBoard(me);
        me.FCBoard = mBoard;
        AndruavSettings.andruavWe7daBase = me;
        mLog = new FakeLog();
        AndruavEngine.setLogHandler(mLog);
        mSmsSent.clear();
        AndruavSMSClientParser.setSmsSender(
                (n, m) -> mSmsSent.add(new String[]{n, m}));
        AndruavSMSClientParser.resetPinFailures();
        mMaxAgeS = AndruavSMSClientParser.DEFAULT_MAX_AGE_S;

        final PhoneSmsConfig sms = new PhoneSmsConfig();
        sms.keywords.addAll(SmsKeywords.parse("go, abort, rtl"));
        mProvider = new PhoneProvider(PhoneHealthConfig.defaults(), null, sms);
        mHost = new FakeHost();
        mProvider.attach(mHost);
        AndruavSMSClientParser.setKeywordFactListener(
                (keyword, contact, age_s) ->
                        mProvider.onSmsKeyword(keyword, contact, age_s));

        setGate(new ArrayList<>(Arrays.asList("mom=" + MOM_NUMBER)));
    }

    @After
    public void tearDown() {
        AndruavEngine.setLogHandler(null);
        AndruavSettings.andruavWe7daBase = null;
        AndruavSMSClientParser.setGateConfigProvider(null);
        AndruavSMSClientParser.setSmsSender(null);
        AndruavSMSClientParser.setKeywordFactListener(null);
        AndruavSMSClientParser.resetPinFailures();
    }

    private void exec(final String sender, final String msg, final long timestampMs) {
        mParser.executeCommand(sender, msg, timestampMs);
    }

    @Test
    public void validMessageFiresFactOnce() throws Exception {
        final long now = System.currentTimeMillis();
        exec(MOM_NUMBER, "ATT:GO:" + PIN, now - 5_000L);
        assertEquals(1, mHost.names.size());
        assertEquals("phone.sms_go.received", mHost.names.get(0));
        final JSONObject payload = mHost.payloads.get(0);
        assertEquals("mom", payload.getString("contact"));
        final int age_s = payload.getInt("age_s");
        assertTrue(age_s >= 5 && age_s <= 60);
        // the payload never carries the number or the message text
        assertFalse(payload.toString().contains(MOM_NUMBER));
        assertFalse(payload.toString().contains("ATT:"));
        assertFalse(mBoard.rtl || mBoard.land || mBoard.auto);
    }

    @Test
    public void refusedSenderAndWrongPinFireNoFact() {
        final long now = System.currentTimeMillis();
        exec(STRANGER, "ATT:GO:" + PIN, now);
        exec(MOM_NUMBER, "ATT:GO:9999", now);
        assertTrue(mHost.names.isEmpty());
        assertTrue(mSmsSent.isEmpty());
    }

    @Test
    public void oldMessageFiresNoFact() {
        final long now = System.currentTimeMillis();
        exec(MOM_NUMBER, "ATT:GO:" + PIN,
                now - (mMaxAgeS + 60L) * 1000L);
        assertTrue(mHost.names.isEmpty());
        // inside the window it fires
        exec(MOM_NUMBER, "ATT:GO:" + PIN, now - 5_000L);
        assertEquals(1, mHost.names.size());
    }

    @Test
    public void unknownKeywordAndReservedNamesFireNoFact() {
        final long now = System.currentTimeMillis();
        exec(MOM_NUMBER, "ATT:BOGUS:" + PIN, now);
        assertTrue(mHost.names.isEmpty());
        // "rtl" is configured as a keyword but reserved: the RTL
        // command still runs and no fact fires
        exec(MOM_NUMBER, "ATT:RTL:" + PIN, now);
        assertTrue(mBoard.rtl);
        assertTrue(mHost.names.isEmpty());
    }

    @Test
    public void noProviderAttachedLogsDropAndRepliesFailure() {
        AndruavSMSClientParser.setKeywordFactListener(null);
        exec(MOM_NUMBER, "ATT:GO:" + PIN, System.currentTimeMillis());
        assertTrue(mHost.names.isEmpty());
        boolean dropLogged = false;
        for (final String line : mLog.lines) {
            if (line.contains("dropped")) dropLogged = true;
        }
        assertTrue(dropLogged);
        assertEquals(1, mSmsSent.size());
        assertEquals(MOM_NUMBER, mSmsSent.get(0)[0]);
    }

    @Test
    public void keywordParseNormalizesAndDropsReserved() {
        final List<String> keywords = SmsKeywords.parse("GO, Abort ,rtl,go,bad!,lnd");
        assertTrue(keywords.contains("go"));
        assertTrue(keywords.contains("abort"));
        assertFalse(keywords.contains("rtl"));
        assertFalse(keywords.contains("lnd"));
        assertFalse(keywords.contains("bad!"));
        assertEquals(2, keywords.size());
    }
}
