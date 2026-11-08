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
import de.databus.caps.CapabilityHost;
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

    /** AN-0128: a CapabilityHost that records fireEvent calls instead of
     *  putting them on a bus. */
    private static final class FakeHost extends CapabilityHost {
        final List<String> names = new ArrayList<>();
        final List<JSONObject> payloads = new ArrayList<>();

        FakeHost() {
            super(null, null);
        }

        @Override
        public String fireEvent(String ns, String ev, JSONObject payload) {
            names.add(ns + "." + ev);
            payloads.add(payload);
            return "test-nonce";
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
    public void torchTakesOnOnly() throws Exception {
        final PhoneProvider provider = makeProvider();
        final JSONObject params = ((JSONObject) provider.adverts().get(0))
                .getJSONObject("actions").getJSONObject("torch").getJSONObject("params");
        assertEquals(1, params.length());
        assertEquals("bool", params.getJSONObject("on").getString("type"));

        Sink sink = new Sink();
        provider.invoke("t1", "phone", "torch",
                new JSONObject().put("on", true).put("pulse_ms", 40), 10000, sink);
        assertNotNull(sink.err);

        sink = new Sink();
        provider.invoke("t2", "phone", "torch", new JSONObject().put("on", 5), 10000, sink);
        assertNotNull(sink.err);

        sink = new Sink();
        provider.invoke("t3", "phone", "torch", new JSONObject().put("on", true), 10000, sink);
        assertNull(sink.err);
        assertTrue(m_bank.output("torch").isOn());
    }

    @Test
    public void strobeTakesPeriodAndDutyPercent() throws Exception {
        final PhoneProvider provider = makeProvider();
        final JSONObject actions = ((JSONObject) provider.adverts().get(0)).getJSONObject("actions");
        assertFalse(actions.has("torch_strobe"));
        final JSONObject params = actions.getJSONObject("strobe").getJSONObject("params");
        assertEquals(2, params.length());
        assertEquals(100, params.getJSONObject("duty").getInt("max"));

        Sink sink = new Sink();
        provider.invoke("s1", "phone", "strobe",
                new JSONObject().put("pulse_ms", 200).put("duty", 25), 10000, sink);
        assertNull(sink.err);
        final PhoneOutput strobe = m_bank.output("strobe");
        assertTrue(strobe.isOn());
        assertEquals(5.0, strobe.pwmHz(), 0.001);
        assertEquals(256, strobe.pwmDuty());

        sink = new Sink();
        provider.invoke("s2", "phone", "strobe",
                new JSONObject().put("pulse_ms", 200).put("duty", 0), 10000, sink);
        assertNull(sink.err);
        assertFalse(strobe.isOn());

        sink = new Sink();
        provider.invoke("s3", "phone", "strobe",
                new JSONObject().put("pulse_ms", 79).put("duty", 50), 10000, sink);
        assertNotNull(sink.err);

        sink = new Sink();
        provider.invoke("s4", "phone", "strobe",
                new JSONObject().put("pulse_ms", 200).put("duty", 101), 10000, sink);
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

    // ------------------------------------------------------------------
    // AN-0128: SMS surface
    // ------------------------------------------------------------------

    private static final String MOM_NUMBER = "+201001110005";

    private PhoneSmsConfig makeSmsConfig(final List<String[]> sent) {
        final PhoneSmsConfig sms = new PhoneSmsConfig();
        sms.keywords.addAll(java.util.Arrays.asList("go", "abort"));
        sms.contacts.put("mom", MOM_NUMBER);
        if (sent != null) {
            sms.sender = (number, text) -> sent.add(new String[]{number, text});
        }
        return sms;
    }

    private PhoneProvider makeSmsProvider(final PhoneSmsConfig sms) {
        m_driver = new FakeDriver();
        m_scheduler = new ManualScheduler();
        m_bank = new PhoneOutputBank(PhoneDevices.defaultSpecs(),
                spec -> m_driver, m_scheduler);
        m_provider = new PhoneProvider(PhoneHealthConfig.defaults(), m_bank, sms);
        return m_provider;
    }

    @Test
    public void advertListsSmsKeywordsAndReceivedPattern() throws Exception {
        final JSONObject advert = (JSONObject) makeSmsProvider(
                makeSmsConfig(null)).adverts().get(0);
        assertEquals("", CapSchema.cap_validate_advert(advert));

        final JSONObject resources = advert.getJSONObject("resources");
        assertEquals("sms", resources.getString("sms_go"));
        assertEquals("sms", resources.getString("sms_abort"));

        final JSONObject pattern = advert.getJSONObject("event_patterns")
                .getJSONObject("*.received");
        assertEquals("fact", pattern.getString("kind"));
        assertEquals("sms", pattern.getJSONArray("res").getString(0));
        assertEquals("string", pattern.getJSONObject("payload")
                .getJSONObject("contact").getString("type"));
        assertEquals("int", pattern.getJSONObject("payload")
                .getJSONObject("age_s").getString("type"));

        final JSONObject smsSend = advert.getJSONObject("actions")
                .getJSONObject("sms_send");
        assertTrue(smsSend.getJSONObject("params").has("contact"));
        assertTrue(smsSend.getJSONObject("params").has("text"));
    }

    @Test
    public void smsKeywordFiresReceivedFactOnce() throws Exception {
        final PhoneProvider provider = makeSmsProvider(makeSmsConfig(null));
        final FakeHost host = new FakeHost();
        provider.attach(host);

        assertTrue(provider.onSmsKeyword("go", "mom", 5));
        assertEquals(1, host.names.size());
        assertEquals("phone.sms_go.received", host.names.get(0));
        final JSONObject payload = host.payloads.get(0);
        assertEquals("mom", payload.getString("contact"));
        assertEquals(5, payload.getInt("age_s"));
        // the payload never carries a number or the message text
        assertFalse(payload.toString().contains(MOM_NUMBER));
        assertFalse(payload.toString().contains("ATT:"));
    }

    @Test
    public void smsKeywordDropsWithoutHostOrKeyword() {
        final PhoneProvider provider = makeSmsProvider(makeSmsConfig(null));
        // no host attached: the parser treats this as "service down"
        assertFalse(provider.onSmsKeyword("go", "mom", 0));
        provider.attach(new FakeHost());
        // a keyword the advert does not know never fires
        assertFalse(provider.onSmsKeyword("bogus", "mom", 0));
    }

    @Test
    public void smsSendKnownContactSendsOnce() throws Exception {
        final List<String[]> sent = new ArrayList<>();
        final PhoneProvider provider = makeSmsProvider(makeSmsConfig(sent));
        final Sink sink = new Sink();
        provider.invoke("s1", "phone", "sms_send",
                new JSONObject().put("contact", "Mom").put("text", "on my way"),
                10000, sink);
        assertNull(sink.err);
        assertNotNull(sink.ok);
        assertEquals(1, sent.size());
        assertEquals(MOM_NUMBER, sent.get(0)[0]);
        assertEquals("on my way", sent.get(0)[1]);
    }

    @Test
    public void smsSendRejectsUnknownContactAndNumbers() throws Exception {
        final List<String[]> sent = new ArrayList<>();
        final PhoneProvider provider = makeSmsProvider(makeSmsConfig(sent));
        Sink sink = new Sink();
        provider.invoke("s2", "phone", "sms_send",
                new JSONObject().put("contact", "stranger").put("text", "x"),
                10000, sink);
        assertNotNull(sink.err);

        sink = new Sink();
        provider.invoke("s3", "phone", "sms_send",
                new JSONObject().put("contact", "+20100999").put("text", "x"),
                10000, sink);
        assertNotNull(sink.err);
        assertTrue(sent.isEmpty());
    }

    @Test
    public void smsSendRateLimitStopsTheLoop() throws Exception {
        final List<String[]> sent = new ArrayList<>();
        final PhoneSmsConfig sms = makeSmsConfig(sent);
        sms.send_limit = 2;
        sms.send_window_ms = 60_000L;
        final PhoneProvider provider = makeSmsProvider(sms);
        for (int i = 0; i < 2; i++) {
            final Sink ok = new Sink();
            provider.invoke("s" + i, "phone", "sms_send",
                    new JSONObject().put("contact", "mom").put("text", "x"),
                    10000, ok);
            assertNull(ok.err);
        }
        final Sink blocked = new Sink();
        provider.invoke("s9", "phone", "sms_send",
                new JSONObject().put("contact", "mom").put("text", "x"),
                10000, blocked);
        assertNotNull(blocked.err);
        assertEquals(2, sent.size());
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
