package ap.andruav_ap.communication.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.EmergencyBase;
import com.andruav.andruavUnit.AndruavUnitMe;
import com.andruav.controlBoard.ControlBoardBase;
import com.andruav.controlBoard.IControlBoard_Callback;
import com.andruav.interfaces.ILog;
import com.andruav.sensors.AndruavIMU;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AN-0127: SMS command gate - allow-list + PIN, ATT:<cmd>:<pin> format,
 * rate-limit lockout, RC-block stop, and log hygiene (no PIN, no message).
 */
public class SMSCommandGateTest {

    private static final String ALLOWED = "+201001234567";
    private static final String STRANGER = "+201009876543";
    private static final String PIN = "4321";

    /*** Records which FCBoard commands the parser reached. */
    private static class FakeBoard extends ControlBoardBase {
        boolean rtl, land, auto;
        int missionStep = -1;
        boolean rcBlocked;

        FakeBoard(final AndruavUnitMe me) { super(me); }
        @Override public boolean do_RCChannelBlocked() { return rcBlocked; }
        @Override public void do_RTL(final IControlBoard_Callback cb) { rtl = true; }
        @Override public void do_Land(final IControlBoard_Callback cb) { land = true; }
        @Override public void do_Auto(final IControlBoard_Callback cb) { auto = true; }
        @Override public void do_SetCurrentMission(final int n) { missionStep = n; }
    }

    /*** Records every log line so PIN/message leakage is assertable. */
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

    /*** Records location-reply attempts (LOC sends through Emergency). */
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

    /*** An IMU that always reports a current location for the LOC path. */
    private static class FakeIMU extends AndruavIMU {
        FakeIMU() { super(true, false, false); }
        @Override public Boolean hasCurrentLocation() { return true; }
    }

    /*** Unit whose active GPS is the fake IMU (LastEvent_IMU is final). */
    private static class FakeMe extends AndruavUnitMe {
        private final AndruavIMU mImu = new FakeIMU();
        @Override public AndruavIMU getActiveGPS() { return mImu; }
    }

    private AndruavSMSClientParser mParser;
    private FakeBoard mBoard;
    private FakeEmergency mEmergency;
    private FakeLog mLog;
    private final List<String[]> mSmsSent = new ArrayList<>();
    private EmergencyBase mOldEmergency;

    private void setGate(final List<String> contacts, final String pin) {
        AndruavSMSClientParser.setGateConfigProvider(
                new AndruavSMSClientParser.GateConfigProvider() {
                    @Override public List<String> contacts() { return contacts; }
                    @Override public String pin() { return pin; }
                });
    }

    @Before
    public void setUp() {
        mParser = new AndruavSMSClientParser();
        final AndruavUnitMe me = new FakeMe();
        mBoard = new FakeBoard(me);
        me.FCBoard = mBoard;
        AndruavSettings.andruavWe7daBase = me;
        mEmergency = new FakeEmergency();
        mOldEmergency = AndruavEngine.getEmergency();
        AndruavEngine.setEmergency(mEmergency);
        mLog = new FakeLog();
        AndruavEngine.setLogHandler(mLog);
        mSmsSent.clear();
        AndruavSMSClientParser.setSmsSender(
                (n, m) -> mSmsSent.add(new String[]{n, m}));
        AndruavSMSClientParser.resetPinFailures();
        setGate(new ArrayList<>(Arrays.asList(ALLOWED)), PIN);
    }

    @After
    public void tearDown() {
        AndruavEngine.setLogHandler(null);
        AndruavEngine.setEmergency(mOldEmergency);
        AndruavSettings.andruavWe7daBase = null;
        AndruavSMSClientParser.setGateConfigProvider(null);
        AndruavSMSClientParser.setSmsSender(null);
        AndruavSMSClientParser.resetPinFailures();
    }

    private void exec(final String sender, final String msg) {
        mParser.executeCommand(sender, msg);
    }

    private boolean anyCommandRan() {
        return mBoard.rtl || mBoard.land || mBoard.auto
                || mBoard.missionStep >= 0;
    }

    @Test
    public void strangerCannotRunAnything() {
        exec(STRANGER, "ATT:RTL:" + PIN);
        exec(STRANGER, "ATT:LND:" + PIN);
        exec(STRANGER, "ATT:Auto 2:" + PIN);
        exec(STRANGER, "ATT:LOC:" + PIN);
        assertFalse(anyCommandRan());
        assertEquals(0, mEmergency.locCalls);
        assertTrue(mSmsSent.isEmpty());
    }

    @Test
    public void wrongPinRunsNothing() {
        exec(ALLOWED, "ATT:RTL:9999");
        assertFalse(anyCommandRan());
        assertTrue(mSmsSent.isEmpty());
        exec(ALLOWED, "ATT:RTL");            // old format, no PIN field
        assertFalse(anyCommandRan());
    }

    @Test
    public void correctPinRunsEachCommand() {
        exec(ALLOWED, "ATT:RTL:" + PIN);
        assertTrue(mBoard.rtl);
        exec(ALLOWED, "ATT:LND:" + PIN);
        assertTrue(mBoard.land);
        exec(ALLOWED, "ATT:Auto 3:" + PIN);
        assertTrue(mBoard.auto);
        assertEquals(3, mBoard.missionStep);
        exec(ALLOWED, "ATT:LOC:" + PIN);
        assertEquals(1, mEmergency.locCalls);
    }

    @Test
    public void emptyConfigRefusesAll() {
        setGate(new ArrayList<>(), PIN);
        exec(ALLOWED, "ATT:RTL:" + PIN);
        assertFalse(anyCommandRan());
        setGate(new ArrayList<>(Arrays.asList(ALLOWED)), "");
        exec(ALLOWED, "ATT:RTL:" + PIN);
        assertFalse(anyCommandRan());
        assertTrue(mSmsSent.isEmpty());
    }

    @Test
    public void xattPrefixRefused() {
        exec(ALLOWED, "XATT:RTL:" + PIN);
        assertFalse(anyCommandRan());
    }

    @Test
    public void fiveWrongPinsLockEverything() {
        for (int i = 0; i < 5; i++) {
            exec(ALLOWED, "ATT:RTL:wrong" + i);
        }
        exec(ALLOWED, "ATT:RTL:" + PIN);     // correct PIN, still locked
        assertFalse(anyCommandRan());
    }

    @Test
    public void rcBlockStopsCommands() {
        mBoard.rcBlocked = true;
        exec(ALLOWED, "ATT:RTL:" + PIN);
        exec(ALLOWED, "ATT:LND:" + PIN);
        exec(ALLOWED, "ATT:Auto 4:" + PIN);
        assertFalse(anyCommandRan());
    }

    @Test
    public void logsNeverCarryPinOrMessage() {
        exec(STRANGER, "ATT:RTL:" + PIN);
        exec(ALLOWED, "ATT:RTL:9999");
        exec(ALLOWED, "ATT:BOGUS:" + PIN);
        for (final String ln : mLog.lines) {
            assertFalse(ln.contains(PIN));
            assertFalse(ln.contains("9999"));
            assertFalse(ln.contains("ATT:BOGUS"));
            assertFalse(ln.contains("ATT:RTL"));
        }
    }

    @Test
    public void helpGoesOnlyToAllowedSenderWithPin() {
        exec(STRANGER, "ATT:HLP:" + PIN);
        assertTrue(mSmsSent.isEmpty());
        exec(ALLOWED, "ATT:HLP:9999");
        assertTrue(mSmsSent.isEmpty());
        exec(ALLOWED, "ATT:HLP:" + PIN);
        assertEquals(1, mSmsSent.size());
        assertTrue(mSmsSent.get(0)[1].contains("ATT:<cmd>:<PIN>"));
    }
}
