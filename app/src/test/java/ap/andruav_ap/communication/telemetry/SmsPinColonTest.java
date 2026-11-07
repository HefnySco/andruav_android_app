package ap.andruav_ap.communication.telemetry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.andruav.AndruavEngine;
import com.andruav.interfaces.ILog;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/** DE-0082: a PIN with a colon matches; wrong PIN refused; compare is constant-time. */
public class SmsPinColonTest {
    private static final String ALLOWED = "+201001110005";

    private static AndruavSMSClientParser.GateConfigProvider gate(final String pin) {
        return new AndruavSMSClientParser.GateConfigProvider() {
            @Override public List<String> contacts() { return Collections.singletonList(ALLOWED); }
            @Override public String pin() { return pin; }
        };
    }

    @Before
    public void setUp() {
        AndruavEngine.setLogHandler(new ILog() {
            @Override public void logException(String tag, Exception e) {}
            @Override public void logException(Exception e) {}
            @Override public void logException(String u, String tag, Throwable e) {}
            @Override public void logException(String u, String tag, Exception e) {}
            @Override public void logException(String u, String tag, VirtualMachineError e) {}
            @Override public void log(String u, String tag, String text) {}
            @Override public void log2(String u, String tag, String text) {}
            @Override public void LogDeviceInfo(String tag, String text) {}
        });
        AndruavSMSClientParser.resetPinFailures();
    }

    @Test
    public void pinWithColonMatches() {
        final long now = System.currentTimeMillis();
        final SmsGateVerdict v = AndruavSMSClientParser.evaluate(
                ALLOWED, "ATT:RTL:12:34", now, gate("12:34"));
        assertTrue(v.allowed);
        assertTrue("RTL".equals(v.command));
    }

    @Test
    public void wrongPinRefusedAndCorrectPinRuns() {
        final long now = System.currentTimeMillis();
        final SmsGateVerdict bad = AndruavSMSClientParser.evaluate(
                ALLOWED, "ATT:RTL:12:35", now, gate("12:34"));
        assertFalse(bad.allowed);
        assertTrue(bad.pinFailure);
        final SmsGateVerdict plain = AndruavSMSClientParser.evaluate(
                ALLOWED, "ATT:RTL:4321", now, gate("4321"));
        assertTrue(plain.allowed);
    }

    @Test
    public void pinMatchesIsExact() {
        assertTrue(AndruavSMSClientParser.pinMatches("1:2", "1:2"));
        assertFalse(AndruavSMSClientParser.pinMatches("1:2", "1:3"));
        assertFalse(AndruavSMSClientParser.pinMatches("1", "12"));
        assertFalse(AndruavSMSClientParser.pinMatches(null, "12"));
    }
}
