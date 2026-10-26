package ap.andruav_ap.denative.providers.phone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * The {@link PhoneOutput} choke point on faked hardware and a manual
 * scheduler (TASK AN-0043 / TO-BE section 5.2, gpio GF-04 rules):
 * edge-only facts with src, one pulse at a time, an explicit write
 * cancelling a pending pulse restore, and the {@code events} gate.
 */
public class PhoneOutputTest {

    /** Records every level applied. */
    private static final class FakeDriver implements PhoneOutput.Driver {
        final List<Boolean> calls = new ArrayList<>();
        String fail_err;

        @Override
        public String apply(boolean on) {
            calls.add(on);
            return fail_err;
        }
    }

    /** Records facts as "went_on|1|src" strings. */
    private static final class FakeFacts implements PhoneOutput.FactSink {
        final List<String> facts = new ArrayList<>();

        @Override
        public void onFact(String verb, int v, String src) {
            facts.add(verb + "|" + v + "|" + src);
        }
    }

    /** Queued restores the test runs by hand. */
    private static final class ManualScheduler implements PhoneOutput.Scheduler {
        final List<Runnable> queue = new ArrayList<>();

        @Override
        public void schedule(long delayMs, Runnable task) {
            queue.add(task);
        }

        void runNext() {
            queue.remove(0).run();
        }
    }

    private FakeDriver m_driver;
    private FakeFacts m_facts;
    private ManualScheduler m_scheduler;
    private PhoneOutput m_output;

    private PhoneOutput makeOutput() {
        return makeOutput(new PhoneOutputSpec("screen", PhoneOutputSpec.KIND_SCREEN));
    }

    private PhoneOutput makeOutput(final PhoneOutputSpec spec) {
        m_driver = new FakeDriver();
        m_facts = new FakeFacts();
        m_scheduler = new ManualScheduler();
        m_output = new PhoneOutput(spec, m_driver, m_facts, m_scheduler);
        return m_output;
    }

    @Test
    public void writeOnAppliesAndEmitsWentOn() {
        final PhoneOutput out = makeOutput();
        assertNull(out.write(true, PhoneOutput.SRC_INVOKE));
        assertTrue(out.isOn());
        assertEquals(1, m_driver.calls.size());
        assertEquals(Boolean.TRUE, m_driver.calls.get(0));
        assertEquals(1, m_facts.facts.size());
        assertEquals("went_on|1|invoke", m_facts.facts.get(0));
    }

    @Test
    public void writeSameLevelEmitsNothing() {
        final PhoneOutput out = makeOutput();
        assertNull(out.write(true, "invoke"));
        assertNull(out.write(true, "invoke"));
        assertEquals(1, m_driver.calls.size());
        assertEquals(1, m_facts.facts.size());
    }

    @Test
    public void writeOffEmitsWentOff() {
        final PhoneOutput out = makeOutput();
        out.write(true, "legacy");
        assertNull(out.write(false, "legacy"));
        assertFalse(out.isOn());
        assertEquals("went_on|1|legacy", m_facts.facts.get(0));
        assertEquals("went_off|0|legacy", m_facts.facts.get(1));
    }

    @Test
    public void toggleInverts() {
        final PhoneOutput out = makeOutput();
        assertNull(out.toggle("event"));
        assertTrue(out.isOn());
        assertNull(out.toggle("event"));
        assertFalse(out.isOn());
    }

    @Test
    public void pulseAssertsThenRestoresOff() {
        final PhoneOutput out = makeOutput();
        assertNull(out.pulse(2000));
        assertTrue(out.isOn());
        assertTrue(out.isPulseActive());
        assertEquals("went_on|1|pulse", m_facts.facts.get(0));

        m_scheduler.runNext();
        assertFalse(out.isOn());
        assertFalse(out.isPulseActive());
        assertEquals("went_off|0|pulse", m_facts.facts.get(1));
    }

    @Test
    public void secondPulseDuringPulseIsRejected() {
        final PhoneOutput out = makeOutput();
        assertNull(out.pulse(2000));
        final String err = out.pulse(500);
        assertEquals("pulse already in progress", err);
        // only the first pulse's assert reached the driver
        assertEquals(1, m_driver.calls.size());
        m_scheduler.runNext();
        // after the restore a new pulse is admitted again
        assertNull(out.pulse(500));
    }

    @Test
    public void writeDuringPulseCancelsRestore() {
        final PhoneOutput out = makeOutput();
        assertNull(out.pulse(2000));
        // explicit write mid-pulse - even to a different level - wins
        assertNull(out.write(false, "invoke"));
        assertEquals("went_off|0|invoke", m_facts.facts.get(1));

        m_scheduler.runNext(); // the pulse's restore must be skipped
        assertFalse(out.isPulseActive());
        assertFalse(out.isOn());
        assertEquals(2, m_driver.calls.size());   // on + off, no restore write
        assertEquals(2, m_facts.facts.size());
    }

    @Test
    public void writeSameLevelDuringPulseStillCancelsRestore() {
        final PhoneOutput out = makeOutput();
        assertNull(out.pulse(2000));
        // same-level write: no edge, but the generation bump cancels restore
        assertNull(out.write(true, "invoke"));
        m_scheduler.runNext();
        assertTrue(out.isOn());  // stays on - restore skipped
        assertEquals(1, m_facts.facts.size());
    }

    @Test
    public void pulseWhileOnRestoresToOn() {
        final PhoneOutput out = makeOutput();
        assertNull(out.write(true, "invoke"));
        assertNull(out.pulse(1000));
        // no edge facts: the level never actually left "on"
        m_scheduler.runNext();
        assertTrue(out.isOn());
        assertEquals(1, m_facts.facts.size());
    }

    @Test
    public void pulseBlockingSleepsAndRestores() {
        final PhoneOutput out = makeOutput(); // scheduler unused on this path
        final long t0 = System.currentTimeMillis();
        assertNull(out.pulseBlocking(30));
        assertTrue(System.currentTimeMillis() - t0 >= 25);
        assertFalse(out.isOn());
        assertEquals(2, m_facts.facts.size());
    }

    @Test
    public void driverErrorBlocksEdgeAndFact() {
        final PhoneOutput out = makeOutput();
        m_driver.fail_err = "no hardware";
        assertEquals("no hardware", out.write(true, "invoke"));
        assertFalse(out.isOn());
        assertTrue(m_facts.facts.isEmpty());
        // a later healthy write still edges from the unchanged state
        m_driver.fail_err = null;
        assertNull(out.write(true, "invoke"));
        assertTrue(out.isOn());
    }

    @Test
    public void eventsFlagSuppressesFacts() {
        final PhoneOutputSpec spec = new PhoneOutputSpec("screen", PhoneOutputSpec.KIND_SCREEN);
        spec.events = false;
        final PhoneOutput out = makeOutput(spec);
        assertNull(out.write(true, "invoke"));
        assertTrue(out.isOn());
        assertTrue(m_facts.facts.isEmpty());
    }

    @Test
    public void noDriverStillTracksStateAndFacts() {
        m_facts = new FakeFacts();
        final PhoneOutput out = new PhoneOutput(
                new PhoneOutputSpec("screen", PhoneOutputSpec.KIND_SCREEN),
                null, m_facts, new ManualScheduler());
        assertNull(out.write(true, "invoke"));
        assertTrue(out.isOn());
        assertEquals("went_on|1|invoke", m_facts.facts.get(0));
    }

    @Test
    public void bankDispatchCommandsAndGate() {
        final FakeDriver driver = new FakeDriver();
        final List<String> emitted = new ArrayList<>();
        final PhoneOutputSpec screen = new PhoneOutputSpec("screen", PhoneOutputSpec.KIND_SCREEN);
        final PhoneOutputSpec silent = new PhoneOutputSpec("torch", PhoneOutputSpec.KIND_TORCH);
        silent.events = false;
        final List<PhoneOutputSpec> specs = new ArrayList<>();
        specs.add(screen);
        specs.add(silent);
        final PhoneOutputBank bank = new PhoneOutputBank(specs,
                spec -> driver, (delay, task) -> {});

        assertNull(bank.command("screen", "on", null, "event"));
        assertTrue(bank.output("screen").isOn());
        assertNull(bank.command("screen", "toggle", null, "event"));
        assertFalse(bank.output("screen").isOn());
        assertEquals("events disabled", bank.command("torch", "on", null, "event"));
        assertNotNull(bank.command("nope", "on", null, "event"));
        assertNotNull(bank.command("screen", "frobnicate", null, "event"));
        assertNotNull(bank.command("screen", "pulse", 99999, "event"));
    }
}
