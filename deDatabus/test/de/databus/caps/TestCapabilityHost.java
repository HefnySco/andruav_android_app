package de.databus.caps;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import de.databus.CModule;

import static de.databus.Messages.*;

/**
 * CapabilityHost dispatch tests: invoke idempotency through the real
 * CModule path, Sync_EventFire nonce dedupe + self-echo filter, state
 * publish. No bus - sendJMSG is recorded and wire messages (JSON +
 * NUL) are fed straight into module.onReceive.
 */
class TestCapabilityHost {

    private static final long POLL_TIMEOUT_MS = 3000;

    private static class Sent {
        final String target; final JSONObject jmsg; final int mid; final boolean internal;
        Sent(String t, JSONObject j, int m, boolean i) { target = t; jmsg = j; mid = m; internal = i; }
    }

    private static class RecordingModule extends CModule {
        final List<Sent> sent = Collections.synchronizedList(new ArrayList<>());
        volatile InvokeHandler invokeHandler;

        @Override
        public void sendJMSG(String targetPartyID, JSONObject jmsg, int andruav_message_id,
                             boolean internal_message) {
            sent.add(new Sent(targetPartyID, jmsg, andruav_message_id, internal_message));
        }

        @Override
        public void on_invoke(InvokeHandler handler) {
            super.on_invoke(handler);
            this.invokeHandler = handler;
        }

        List<Sent> sentOfType(int mid) {
            synchronized (sent) {
                List<Sent> out = new ArrayList<>();
                for (Sent s : sent) if (s.mid == mid) out.add(s);
                return out;
            }
        }
    }

    /** test_echo provider: echo {text} -> {text}; counts invokes/events. */
    private static class EchoProvider implements DeCapabilityProvider {
        final AtomicInteger invokes = new AtomicInteger();
        final AtomicInteger events = new AtomicInteger();
        volatile String lastEventName;
        volatile JSONObject lastEventPayload;
        volatile String lastEventNonce;

        @Override
        public List<Object> adverts() {
            return Collections.singletonList(new JSONObject(
                    "{\"schema\":\"de.cap/1\",\"ns\":\"test_echo\",\"module\":\"andruav_phone\","
                    + "\"ver\":\"1.0\",\"actions\":{\"echo\":{\"params\":"
                    + "{\"text\":{\"type\":\"string\"}}}}}"));
        }

        @Override
        public void invoke(String id, String ns, String act, JSONObject params,
                           long deadlineMs, ResultSink sink) {
            invokes.incrementAndGet();
            sink.ok(new JSONObject().put("text", params.optString("text")));
        }

        @Override
        public void onEvent(String name, JSONObject ed, String nonce) {
            events.incrementAndGet();
            lastEventName = name;
            lastEventPayload = ed;
            lastEventNonce = nonce;
        }

        @Override
        public JSONObject state(String ns) {
            return new JSONObject().put("echoes", invokes.get());
        }
    }

    private static JSONObject wireMsg(int mid, JSONObject cmd) {
        return new JSONObject()
                .put(INTERMODULE_ROUTING_TYPE, CMD_TYPE_INTERMODULE)
                .put(ANDRUAV_PROTOCOL_MESSAGE_TYPE, mid)
                .put(ANDRUAV_PROTOCOL_MESSAGE_CMD, cmd);
    }

    private static JSONObject invokeMsg(String id, String ns, String act, JSONObject params) {
        return wireMsg(TYPE_AndruavMessage_CAPABILITY_INVOKE, new JSONObject()
                .put("id", id).put("ns", ns).put("act", act)
                .put("p", params).put("dl", 5));
    }

    private static JSONObject eventMsg(String d, String nonce, JSONObject payload) {
        return wireMsg(TYPE_AndruavMessage_Sync_EventFire, new JSONObject()
                .put("d", d).put("n", nonce).put("ed", payload));
    }

    /** JSON header + NUL terminator, as de_comm puts it on the wire. */
    private static void feed(CModule module, JSONObject jMsg) {
        byte[] json = jMsg.toString().getBytes(StandardCharsets.UTF_8);
        byte[] wire = new byte[json.length + 1];
        System.arraycopy(json, 0, wire, 0, json.length);
        module.onReceive(wire, wire.length);
    }

    private static void await(Supplier<Boolean> cond) throws InterruptedException {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (cond.get()) return;
            Thread.sleep(10);
        }
        fail("timed out waiting for condition");
    }

    @Test
    void repeated_invoke_id_runs_the_handler_once_and_replays_the_result() throws Exception {
        RecordingModule module = new RecordingModule();
        EchoProvider provider = new EchoProvider();
        CapabilityHost host = new CapabilityHost(module, Collections.singletonList(provider));
        host.start();

        JSONObject invoke = invokeMsg("r:m:1", "test_echo", "echo",
                new JSONObject().put("text", "hi"));

        feed(module, invoke);
        await(() -> module.sentOfType(TYPE_AndruavMessage_CAPABILITY_RESULT).size() == 1);
        assertEquals(1, provider.invokes.get());

        // repeated id -> the idempotency ring replays the cached 6544;
        // the handler does not run again
        feed(module, invoke);
        await(() -> module.sentOfType(TYPE_AndruavMessage_CAPABILITY_RESULT).size() == 2);
        assertEquals(1, provider.invokes.get());

        List<Sent> results = module.sentOfType(TYPE_AndruavMessage_CAPABILITY_RESULT);
        assertEquals("r:m:1", results.get(0).jmsg.getString("id"));
        assertEquals("r:m:1", results.get(1).jmsg.getString("id"));
        assertTrue(results.get(0).jmsg.getBoolean("ok"));
        assertTrue(results.get(1).jmsg.getBoolean("ok"));
        assertEquals(results.get(0).jmsg.getJSONObject("data").getString("text"),
                results.get(1).jmsg.getJSONObject("data").getString("text"));
    }

    @Test
    void events_dedupe_by_nonce_and_self_emitted_events_are_not_consumed_back() throws Exception {
        RecordingModule module = new RecordingModule();
        EchoProvider provider = new EchoProvider();
        CapabilityHost host = new CapabilityHost(module, Collections.singletonList(provider));
        host.start();

        JSONObject ping = eventMsg("test_echo.ping", "n1", new JSONObject().put("k", 1));
        feed(module, ping);
        assertEquals(1, provider.events.get());
        assertEquals("test_echo.ping", provider.lastEventName);
        assertEquals("n1", provider.lastEventNonce);
        assertEquals(1, provider.lastEventPayload.getInt("k"));

        // same d|n again -> ignored
        feed(module, ping);
        assertEquals(1, provider.events.get());

        // a fresh nonce delivers again
        feed(module, eventMsg("test_echo.ping", "n2", new JSONObject()));
        assertEquals(2, provider.events.get());

        // self-echo: a nonce the host emitted is never consumed back
        String nonce = host.fireEvent("test_echo", "pong", new JSONObject().put("s", 2));
        List<Sent> emitted = module.sentOfType(TYPE_AndruavMessage_Sync_EventFire);
        assertEquals(1, emitted.size());
        assertEquals("test_echo.pong", emitted.get(0).jmsg.getString("d"));
        assertEquals(nonce, emitted.get(0).jmsg.getString("n"));
        feed(module, eventMsg("test_echo.pong", nonce, new JSONObject()));
        assertEquals(2, provider.events.get());

        // a foreign namespace is not delivered either
        feed(module, eventMsg("other.ping", "n9", new JSONObject()));
        assertEquals(2, provider.events.get());
    }

    @Test
    void publishState_sends_the_provider_snapshot() {
        RecordingModule module = new RecordingModule();
        EchoProvider provider = new EchoProvider();
        CapabilityHost host = new CapabilityHost(module, Collections.singletonList(provider));
        host.start();

        provider.invokes.set(3);
        host.publishState("test_echo");
        List<Sent> states = module.sentOfType(TYPE_AndruavMessage_MODULE_STATE);
        assertEquals(1, states.size());
        assertEquals("test_echo", states.get(0).jmsg.getString("ns"));
        assertEquals(3, states.get(0).jmsg.getJSONObject("s").getInt("echoes"));
        assertTrue(states.get(0).jmsg.getBoolean("full"));

        // unowned ns -> nothing sent
        host.publishState("not_owned");
        assertEquals(1, module.sentOfType(TYPE_AndruavMessage_MODULE_STATE).size());
    }

    @Test
    void invoke_for_an_unowned_namespace_fails() {
        RecordingModule module = new RecordingModule();
        EchoProvider provider = new EchoProvider();
        CapabilityHost host = new CapabilityHost(module, Collections.singletonList(provider));
        host.start();

        // the module's own advert resolution rejects unknown ns/act before
        // dispatch; the host's fallback covers a map/advert mismatch
        CModule.InvokeResult r = module.invokeHandler.onInvoke("r:m:9", "ghost_ns", "act",
                Collections.emptyMap(), System.currentTimeMillis() + 5000);
        assertNotNull(r.err);
        assertEquals("unknown action ghost_ns.act", r.err);
    }

    @Test
    void invalid_advert_is_rejected_and_never_published() {
        RecordingModule module = new RecordingModule();
        DeCapabilityProvider bad = new EchoProvider() {
            @Override
            public List<Object> adverts() {
                return Collections.singletonList(new JSONObject(
                        "{\"schema\":\"de.cap/1\",\"ns\":\"BAD NS!\",\"module\":\"x\",\"ver\":\"1\"}"));
            }
        };
        CapabilityHost host = new CapabilityHost(module, Collections.singletonList(bad));
        assertThrows(IllegalArgumentException.class, host::start);
        assertTrue(module.sentOfType(TYPE_AndruavMessage_MODULE_CAPABILITIES).isEmpty());
    }
}
