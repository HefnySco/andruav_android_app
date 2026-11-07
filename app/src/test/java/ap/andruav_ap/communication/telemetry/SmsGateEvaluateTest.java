package ap.andruav_ap.communication.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import static org.junit.Assert.assertTrue;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.EmergencyBase;
import com.andruav.andruavUnit.AndruavUnitMe;
import com.andruav.interfaces.ILog;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/***
 * AN-0137 (TASK-116): evaluate() is pure - no PIN count, no GCS notice, no
 * fact, no SMS - while executeCommand(..., testRun=true) runs the real path
 * with TEST:-prefixed GCS lines.
 */
public class SmsGateEvaluateTest {

    private static final String ALLOWED = "+201001110005";
    private static final String STRANGER = "+201009876543";
    private static final String PIN = "4321";

    private static class FakeLog implements ILog {
        @Override public void logException(final String tag, final Exception e) {}
        @Override public void logException(final Exception e) {}
        @Override public void logException(final String u, final String tag, final Throwable e) {}
        @Override public void logException(final String u, final String tag, final Exception e) {}
        @Override public void logException(final String u, final String tag, final VirtualMachineError e) {}
        @Override public void log(final String u, final String tag, final String text) {}
        @Override public void log2(final String u, final String tag, final String text) {}
        @Override public void LogDeviceInfo(final String tag, final String text) {}
    }

    private static class FakeEmergency extends EmergencyBase {
        int locCalls;
        @Override public void triggerEmergencyFlightModeFaileSafe(final boolean t) {}
        @Override public boolean getIsSirenActive() { return false; }
        @Override public boolean getIsFlashing() { return false; }
        @Override protected void playSiren(final boolean a, final boolean p) {}
        @Override protected void doFlash(final boolean e, final boolean p) {}
        @Override public int sendSMS(final boolean ignoreTiming) { return 0; }
        @Override public int sendSMSLocation(final String num, final boolean forced) {
            locCalls++;
            return 0;
        }
    }

    private AndruavSMSClientParser mParser;
    private final List<String[]> mSmsSent = new ArrayList<>();
    private final List<String[]> mGcsNotes = new ArrayList<>();
    private final List<String[]> mFacts = new ArrayList<>();
    private EmergencyBase mOldEmergency;

    private void setGate(final List<String> contacts, final String pin,
                         final List<String> keywords) {
        AndruavSMSClientParser.setGateConfigProvider(
                new AndruavSMSClientParser.GateConfigProvider() {
                    @Override public List<String> contacts() { return contacts; }
                    @Override public String pin() { return pin; }
                    @Override public List<String> keywords() { return keywords; }
                });
    }

    private AndruavSMSClientParser.GateConfigProvider gate(
            final List<String> contacts, final String pin,
            final List<String> keywords) {
        return new AndruavSMSClientParser.GateConfigProvider() {
            @Override public List<String> contacts() { return contacts; }
            @Override public String pin() { return pin; }
            @Override public List<String> keywords() { return keywords; }
        };
    }

    @Before
    public void setUp() {
        mParser = new AndruavSMSClientParser();
        AndruavSettings.andruavWe7daBase = new AndruavUnitMe();
        mOldEmergency = AndruavEngine.getEmergency();
        AndruavEngine.setEmergency(new FakeEmergency());
        AndruavEngine.setLogHandler(new FakeLog());
        mSmsSent.clear();
        AndruavSMSClientParser.setSmsSender(
                (n, m) -> mSmsSent.add(new String[]{n, m}));
        mGcsNotes.clear();
        AndruavSMSClientParser.setGcsNotifier(
                (t, text) -> mGcsNotes.add(new String[]{String.valueOf(t), text}));
        mFacts.clear();
        AndruavSMSClientParser.setKeywordFactListener(
                (kw, contact, age_s) -> {
                    mFacts.add(new String[]{kw, contact, String.valueOf(age_s)});
                    return true;
                });
        AndruavSMSClientParser.resetPinFailures();
    }

    @After
    public void tearDown() {
        AndruavEngine.setLogHandler(null);
        AndruavEngine.setEmergency(mOldEmergency);
        AndruavSettings.andruavWe7daBase = null;
        AndruavSMSClientParser.setGateConfigProvider(null);
        AndruavSMSClientParser.setSmsSender(null);
        AndruavSMSClientParser.setGcsNotifier(null);
        AndruavSMSClientParser.setKeywordFactListener(null);
        AndruavSMSClientParser.resetPinFailures();
    }

    private SmsGateVerdict eval(final String sender, final String msg) {
        return AndruavSMSClientParser.evaluate(sender, msg, System.currentTimeMillis(),
                gate(new ArrayList<>(Arrays.asList("me=" + ALLOWED)), PIN,
                        new ArrayList<>(Arrays.asList("go"))));
    }

    private SmsGateVerdict.Gate gate(final SmsGateVerdict v, final String name) {
        for (final SmsGateVerdict.Gate g : v.gates) {
            if (g.name.equals(name)) return g;
        }
        return null;
    }

    @Test
    public void wrongPinFailsAndTouchesNothing() {
        setGate(new ArrayList<>(Arrays.asList(ALLOWED)), PIN, new ArrayList<>());
        final SmsGateVerdict v = eval(ALLOWED, "ATT:RTL:9999");
        assertFalse(v.allowed);
        assertEquals(SmsGateVerdict.Status.FAIL, gate(v, "pin").status);
        assertTrue(v.pinFailure);

        /* evaluate itself left no trace: the PIN window is still clean
         * (a real wrong-PIN SMS would start it), and nothing notified. */
        mParser.executeCommand(ALLOWED, "ATT:RTL:" + PIN);
        // a real command still runs because no PIN failures were counted
        assertTrue(mGcsNotes.stream().anyMatch(n -> n[1].contains("SMS accepted")));
        assertTrue(mSmsSent.isEmpty());
    }

    @Test
    public void keywordEvaluatesToFactAndContact() {
        final SmsGateVerdict v = eval(ALLOWED, "ATT:go:" + PIN);
        assertTrue(v.allowed);
        assertEquals("go", v.keyword);
        assertEquals("phone.sms_go", v.fact);
        assertEquals("me", v.contact);
        for (final SmsGateVerdict.Gate g : v.gates) {
            assertEquals(SmsGateVerdict.Status.PASS, g.status);
        }
    }

    @Test
    public void strangerFailsAtSenderGate() {
        final SmsGateVerdict v = eval(STRANGER, "ATT:go:" + PIN);
        assertFalse(v.allowed);
        assertEquals(SmsGateVerdict.Status.FAIL, gate(v, "sender").status);
        assertEquals(SmsGateVerdict.Status.SKIP, gate(v, "format").status);
        assertEquals("", v.contact);
    }

    /*** DE-0093: after a FAIL, every later gate is listed as SKIP and the
     *  verdict always carries the full gate set, as the class doc says. */
    @Test
    public void gatesAfterFailAreListedSkip() {
        // sender gate fails: the four later gates are SKIP, nothing missing
        final SmsGateVerdict v = eval(STRANGER, "ATT:go:" + PIN);
        assertEquals(SmsGateVerdict.GATE_ORDER.length, v.gates.size());
        boolean seenFail = false;
        for (final SmsGateVerdict.Gate g : v.gates) {
            if (g.status == SmsGateVerdict.Status.FAIL) seenFail = true;
            else if (seenFail) assertEquals(SmsGateVerdict.Status.SKIP, g.status);
        }

        // receiver gate fails first: all six later gates are SKIP
        final SmsGateVerdict off = AndruavSMSClientParser.evaluate(ALLOWED, "ATT:go:" + PIN,
                System.currentTimeMillis(), new AndruavSMSClientParser.GateConfigProvider() {
                    @Override public List<String> contacts() {
                        return new ArrayList<>(Arrays.asList(ALLOWED));
                    }
                    @Override public String pin() { return PIN; }
                    @Override public boolean receiverOn() { return false; }
                });
        assertEquals(SmsGateVerdict.Status.FAIL, gate(off, "receiver").status);
        assertEquals(SmsGateVerdict.GATE_ORDER.length, off.gates.size());
        for (final SmsGateVerdict.Gate g : off.gates) {
            if (!g.name.equals("receiver")) {
                assertEquals(SmsGateVerdict.Status.SKIP, g.status);
            }
        }
    }

    @Test
    public void dryRunsNeverLockOut() {
        for (int i = 0; i < 5; i++) {
            eval(ALLOWED, "ATT:RTL:bad" + i);
        }
        // a real SMS after five bad dry runs still passes the lockout gate
        final SmsGateVerdict v = eval(ALLOWED, "ATT:RTL:" + PIN);
        assertTrue(v.allowed);
        assertEquals(SmsGateVerdict.Status.PASS, gate(v, "lockout").status);
    }

    @Test
    public void liveTestRunsFactAndPrefixesNotices() {
        setGate(new ArrayList<>(Arrays.asList("me=" + ALLOWED)), PIN,
                new ArrayList<>(Arrays.asList("go")));
        mParser.executeCommand(ALLOWED, "ATT:go:" + PIN, true);
        assertEquals(1, mFacts.size());
        assertEquals("go", mFacts.get(0)[0]);
        assertEquals("me", mFacts.get(0)[1]);
        assertTrue(mGcsNotes.size() >= 1);
        for (final String[] n : mGcsNotes) {
            assertTrue(n[1].startsWith("TEST: "));
        }
    }

    @Test
    public void realRunHasNoTestPrefix() {
        setGate(new ArrayList<>(Arrays.asList("me=" + ALLOWED)), PIN,
                new ArrayList<>(Arrays.asList("go")));
        mParser.executeCommand(ALLOWED, "ATT:go:" + PIN);
        assertEquals(1, mFacts.size());
        for (final String[] n : mGcsNotes) {
            assertFalse(n[1].startsWith("TEST:"));
        }
    }

    @Test
    public void liveTestRefusalIsPrefixedToo() {
        setGate(new ArrayList<>(Arrays.asList(ALLOWED)), PIN, new ArrayList<>());
        mParser.executeCommand(STRANGER, "ATT:RTL:" + PIN, true);
        assertEquals(1, mGcsNotes.size());
        assertTrue(mGcsNotes.get(0)[1].startsWith("TEST: "));
    }
}
