package ap.andruav_ap.denative.providers.phone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Test;

import de.databus.CapSchema;
import de.databus.caps.DeCapabilityProvider;

/**
 * The {@link PhoneProvider} databus surface on faked outputs (TASK
 * AN-0043): the de.cap/1 advert validates, invokes reach the choke
 * points, event commands dispatch, and fact/state wiring merges the
 * output levels into the namespace snapshot.
 */
public class PhoneProviderTest {

    private static final class FakeDriver implements PhoneOutput.Driver {
        final List<Boolean> calls = new ArrayList<>();

        @Override
        public String apply(boolean on) {
            calls.add(on);
            return null;
        }
    }

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

    /** Canned ResultSink capturing the first answer. */
    private static final class Sink implements DeCapabilityProvider.ResultSink {
        JSONObject ok;
        String err;

        @Override
        public void ok(JSONObject data) {
            this.ok = data;
        }

        @Override
        public void err(String err) {
            this.err = err;
        }
    }

    private FakeDriver m_driver;
    private ManualScheduler m_scheduler;
    private PhoneOutputBank m_bank;
    private PhoneProvider m_provider;

    private PhoneProvider makeProvider() {
        m_driver = new FakeDriver();
        m_scheduler = new ManualScheduler();
        m_bank = new PhoneOutputBank(PhoneDevices.defaultSpecs(),
                spec -> m_driver, m_scheduler);
        m_provider = new PhoneProvider(PhoneHealthConfig.defaults(), m_bank);
        return m_provider;
    }

    @Test
    public void advertValidates() throws Exception {
        final PhoneProvider provider = makeProvider();
        final List<Object> adverts = provider.adverts();
        assertEquals(1, adverts.size());
        assertTrue(adverts.get(0) instanceof JSONObject);
        assertEquals("", CapSchema.cap_validate_advert(adverts.get(0)));
    }

    @Test
    public void advertDeclaresResourcesPatternsAndActions() throws Exception {
        final JSONObject advert = (JSONObject) makeProvider().adverts().get(0);
        assertEquals("phone", advert.getString("ns"));

        final JSONObject resources = advert.getJSONObject("resources");
        assertEquals("out", resources.getString("screen"));
        assertEquals("out", resources.getString("torch"));
        assertEquals("out", resources.getString("vibrate"));

        final JSONObject patterns = advert.getJSONObject("event_patterns");
        assertEquals("command", patterns.getJSONObject("*.on").getString("kind"));
        assertEquals("command", patterns.getJSONObject("*.off").getString("kind"));
        assertEquals("command", patterns.getJSONObject("*.toggle").getString("kind"));
        assertEquals("command", patterns.getJSONObject("*.pulse").getString("kind"));
        assertEquals("fact", patterns.getJSONObject("*.went_on").getString("kind"));
        assertEquals("fact", patterns.getJSONObject("*.went_off").getString("kind"));

        final JSONObject actions = advert.getJSONObject("actions");
        assertEquals("action", actions.getJSONObject("screen").getString("claim"));
        assertEquals("action", actions.getJSONObject("torch").getString("claim"));
        assertEquals("action", actions.getJSONObject("vibrate").getString("claim"));
        assertTrue(actions.has("write"));
        assertTrue(actions.has("pulse"));
    }

    @Test
    public void invokeResourceOnWritesThroughChokePoint() throws Exception {
        final PhoneProvider provider = makeProvider();
        final Sink sink = new Sink();
        provider.invoke("i1", "phone", "screen",
                new JSONObject().put("on", true), 10000, sink);
        assertNull(sink.err);
        assertNotNull(sink.ok);
        assertEquals("screen", sink.ok.getString("pin"));
        assertTrue(sink.ok.getBoolean("on"));
        assertTrue(m_bank.output("screen").isOn());
        assertEquals(1, m_driver.calls.size());
    }

    @Test
    public void invokeGenericWriteResolvesPin() throws Exception {
        final PhoneProvider provider = makeProvider();
        final Sink sink = new Sink();
        provider.invoke("i2", "phone", "write",
                new JSONObject().put("pin", "torch").put("value", true), 10000, sink);
        assertNull(sink.err);
        assertTrue(m_bank.output("torch").isOn());
    }

    @Test
    public void invokeUnknownPinAndActionError() throws Exception {
        final PhoneProvider provider = makeProvider();
        Sink sink = new Sink();
        provider.invoke("i3", "phone", "write",
                new JSONObject().put("pin", "warp_drive").put("value", true), 10000, sink);
        assertNotNull(sink.err);

        sink = new Sink();
        provider.invoke("i4", "phone", "defrobnicate", new JSONObject(), 10000, sink);
        assertNotNull(sink.err);
    }

    @Test
    public void invokePulseBlocksAndRestores() throws Exception {
        final PhoneProvider provider = makeProvider();
        final Sink sink = new Sink();
        provider.invoke("i5", "phone", "screen",
                new JSONObject().put("on", true).put("pulse_ms", 40), 10000, sink);
        assertNull(sink.err);
        assertNotNull(sink.ok);
        // the invoke only returns after the restore ran
        assertFalse(sink.ok.getBoolean("on"));
        assertFalse(m_bank.output("screen").isOn());
    }

    @Test
    public void invokePulseRequiresOn() throws Exception {
        final PhoneProvider provider = makeProvider();
        final Sink sink = new Sink();
        provider.invoke("i6", "phone", "screen",
                new JSONObject().put("on", false).put("pulse_ms", 40), 10000, sink);
        assertNotNull(sink.err);
    }

    @Test
    public void eventCommandsDispatchByKeyAndVerb() throws Exception {
        final PhoneProvider provider = makeProvider();
        provider.onEvent("phone.vibrate.on", new JSONObject(), "n1");
        assertTrue(m_bank.output("vibrate").isOn());
        provider.onEvent("phone.vibrate.off", new JSONObject(), "n2");
        assertFalse(m_bank.output("vibrate").isOn());

        // pulse command: asserts now, restores via the scheduler
        provider.onEvent("phone.screen.pulse",
                new JSONObject().put("ms", 250), "n3");
        assertTrue(m_bank.output("screen").isOn());
        assertEquals(1, m_scheduler.queue.size());
        m_scheduler.runNext();
        assertFalse(m_bank.output("screen").isOn());
    }

    @Test
    public void eventFactNamesAreNotCommands() throws Exception {
        final PhoneProvider provider = makeProvider();
        // our own fact names must never be consumed as orders
        provider.onEvent("phone.screen.went_on", new JSONObject(), "n1");
        provider.onEvent("phone.battery_low", new JSONObject(), "n2");
        assertFalse(m_bank.output("screen").isOn());
        assertTrue(m_driver.calls.isEmpty());
    }

    @Test
    public void stateMergesOutputLevels() throws Exception {
        final PhoneProvider provider = makeProvider();
        m_bank.output("torch").write(true, "invoke");
        provider.tracker().onBattery(80, true, 31.0);
        final JSONObject state = provider.state("phone");
        assertEquals(1, state.getInt("torch"));
        assertEquals(0, state.getInt("screen"));
        // health keys still present
        assertEquals(80, state.getInt("battery"));
    }

    @Test
    public void bankFirstSpecWinsOnNameCollision() {
        final List<PhoneOutputSpec> specs = new ArrayList<>();
        specs.add(new PhoneOutputSpec("screen", PhoneOutputSpec.KIND_SCREEN));
        specs.add(new PhoneOutputSpec("screen", PhoneOutputSpec.KIND_TORCH));
        final PhoneOutputBank bank = new PhoneOutputBank(specs,
                spec -> null, (delay, task) -> {});
        assertEquals(1, bank.specs().size());
        assertEquals(PhoneOutputSpec.KIND_SCREEN, bank.specs().get(0).kind);
    }
}
