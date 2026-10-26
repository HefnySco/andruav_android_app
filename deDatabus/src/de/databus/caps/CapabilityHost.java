package de.databus.caps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;

import de.databus.CapSchema;
import de.databus.CModule;

import static de.databus.Messages.*;

/**
 * Host side of the de.cap/1 surface: owns a CModule's capability
 * registration and routes between the intermodule bus and a set of
 * DeCapabilityProvider, one provider per advertised namespace.
 *
 * start() collects and validates every provider advert (an invalid
 * advert is never published), registers them through
 * CModule.set_capabilities, then takes over invoke dispatch (6543 ->
 * the InvokeHandler -> the provider owning the invoke's ns) and
 * Sync_EventFire consumption (1061 -> onEvent of the provider owning
 * the event name's ns, deduped on "d|n" - GF-06).
 */
public class CapabilityHost implements CModule.OnReceiveCallback {

    private static final int EVENT_RING_SIZE = 256;

    /** Bounded nonce dedupe ring: seen() checks, add() inserts and
     *  evicts the oldest past capacity. Thread-safe. */
    private static final class NonceRing {
        private final int m_capacity;
        private final LinkedHashSet<String> m_keys = new LinkedHashSet<>();

        NonceRing(int capacity) { this.m_capacity = capacity; }

        synchronized boolean seen(String key) { return this.m_keys.contains(key); }

        synchronized void add(String key) {
            if (key == null || key.isEmpty() || this.m_keys.contains(key)) return;
            this.m_keys.add(key);
            while (this.m_keys.size() > this.m_capacity) {
                this.m_keys.remove(this.m_keys.iterator().next());
            }
        }
    }

    private final CModule m_module;
    private final List<DeCapabilityProvider> m_providers = new ArrayList<>();
    private final Map<String, DeCapabilityProvider> m_ns_providers = new HashMap<>();
    private final NonceRing m_inbound_ring = new NonceRing(EVENT_RING_SIZE);
    private final NonceRing m_self_ring = new NonceRing(EVENT_RING_SIZE);
    private final Object m_lock = new Object();

    public CapabilityHost(CModule module, Collection<DeCapabilityProvider> providers) {
        this.m_module = module;
        if (providers != null) this.m_providers.addAll(providers);
    }

    /** Collect + validate all provider adverts, register them on the
     *  module, and take over invoke + event routing.
     *  Throws IllegalArgumentException on an invalid advert or a
     *  namespace claimed by two providers. */
    public void start() {
        _apply_adverts();
        this.m_module.on_invoke(this::_dispatch_invoke);
        this.m_module.setOnReceive(this);
    }

    /** Add a provider; call refreshAdverts() to (re)publish its adverts. */
    public void addProvider(DeCapabilityProvider provider) {
        synchronized (m_lock) {
            this.m_providers.add(provider);
        }
    }

    /** Re-collect adverts from all providers, re-register them on the
     *  module and republish them on MODULE_CAPABILITIES. Returns false
     *  when the module rejected the set. */
    public boolean refreshAdverts() {
        if (!_apply_adverts()) return false;
        this.m_module.send_capabilities();
        return true;
    }

    /** Publish a full state snapshot for an owned namespace (6545). */
    public void publishState(String ns) {
        publishState(ns, true);
    }

    /** Publish the owning provider's state snapshot; full marks it a
     *  complete snapshot rather than changed keys. No-op when the ns is
     *  not owned. */
    public void publishState(String ns, boolean full) {
        DeCapabilityProvider provider = _provider_for(ns);
        if (provider == null) return;
        JSONObject snapshot = provider.state(ns);
        this.m_module.publish_state(ns,
                snapshot != null ? snapshot : new JSONObject(), full);
    }

    /** Fire "<ns>.<ev>" on the bus; the nonce joins the self-echo ring
     *  so the host never consumes its own events back. Returns the
     *  nonce sent. */
    public String fireEvent(String name, JSONObject payload) {
        int dot = name.indexOf('.');
        if (dot <= 0 || dot == name.length() - 1)
            throw new IllegalArgumentException("event name must be '<ns>.<ev>': " + name);
        return fireEvent(name.substring(0, dot), name.substring(dot + 1), payload);
    }

    /** fireEvent("<ns>.<ev>", payload) split form. */
    public String fireEvent(String ns, String ev, JSONObject payload) {
        String nonce = this.m_module.fire_event(ns, ev, payload);
        this.m_self_ring.add(ns + "." + ev + "|" + nonce);
        return nonce;
    }

    /** CModule.OnReceiveCallback: consumes Sync_EventFire (1061,
     *  intermodule) for owned namespaces with "d|n" nonce dedupe.
     *  Payload is cmd.ed, then cmd.m, then {}. Runs on the module's
     *  receive thread - providers must not block. */
    @Override
    public void onReceive(byte[] message, int len, JSONObject jMsg) {
        if (jMsg.optInt(ANDRUAV_PROTOCOL_MESSAGE_TYPE) != TYPE_AndruavMessage_Sync_EventFire) return;
        if (!CMD_TYPE_INTERMODULE.equals(jMsg.optString(INTERMODULE_ROUTING_TYPE))) return;
        Object cmdObj = jMsg.opt(ANDRUAV_PROTOCOL_MESSAGE_CMD);
        if (!(cmdObj instanceof JSONObject)) return;
        JSONObject cmd = (JSONObject) cmdObj;

        String d = cmd.optString("d", "");
        int dot = d.indexOf('.');
        if (dot <= 0) return;
        DeCapabilityProvider provider = _provider_for(d.substring(0, dot));
        if (provider == null) return;

        String nonce = cmd.optString("n", "");
        if (!nonce.isEmpty()) {
            String key = d + "|" + nonce;
            // self-echo: never consume back, and never occupy the inbound ring
            if (this.m_self_ring.seen(key)) return;
            if (this.m_inbound_ring.seen(key)) return;
            this.m_inbound_ring.add(key);
        }

        Object payloadObj = cmd.opt("ed");
        if (!(payloadObj instanceof JSONObject)) payloadObj = cmd.opt("m");
        JSONObject payload = (payloadObj instanceof JSONObject)
                ? (JSONObject) payloadObj : new JSONObject();
        provider.onEvent(d, payload, nonce);
    }

    /** CModule.InvokeHandler: routes by namespace to the owning
     *  provider and bridges its async ResultSink back to a synchronous
     *  InvokeResult, bounded by the invoke deadline. */
    private CModule.InvokeResult _dispatch_invoke(String id, String ns, String act,
                                                  Map<String, Object> params, long deadlineMs) {
        DeCapabilityProvider provider = _provider_for(ns);
        if (provider == null)
            return CModule.InvokeResult.fail("unknown action " + ns + "." + act);

        final CountDownLatch answered = new CountDownLatch(1);
        final AtomicReference<CModule.InvokeResult> result = new AtomicReference<>();
        DeCapabilityProvider.ResultSink sink = new DeCapabilityProvider.ResultSink() {
            @Override
            public void ok(JSONObject data) {
                if (result.compareAndSet(null, CModule.InvokeResult.ok(data))) answered.countDown();
            }
            @Override
            public void err(String err) {
                if (result.compareAndSet(null, CModule.InvokeResult.fail(err))) answered.countDown();
            }
        };

        provider.invoke(id, ns, act, new JSONObject(params), deadlineMs, sink);

        long remaining = deadlineMs - System.currentTimeMillis();
        try {
            if (remaining > 0) answered.await(remaining, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CModule.InvokeResult.fail("interrupted");
        }
        CModule.InvokeResult r = result.get();
        return r != null ? r : CModule.InvokeResult.fail("timeout");
    }

    /** Collect adverts from all providers, pre-validate each one, then
     *  hand the set to the module. The ns -> provider map is rebuilt
     *  only when registration succeeds. */
    private boolean _apply_adverts() {
        List<Object> adverts = new ArrayList<>();
        Map<String, DeCapabilityProvider> ns_map = new HashMap<>();
        synchronized (m_lock) {
            for (DeCapabilityProvider provider : this.m_providers) {
                List<Object> provider_adverts = provider.adverts();
                if (provider_adverts == null) continue;
                for (Object advert : provider_adverts) {
                    JSONObject parsed = _parse_advert(provider, advert);
                    String err = CapSchema.cap_validate_advert(parsed);
                    if (!err.isEmpty())
                        throw new IllegalArgumentException("invalid advert from "
                                + provider.getClass().getName() + ": " + err);
                    String ns = parsed.optString("ns");
                    if (ns_map.containsKey(ns))
                        throw new IllegalArgumentException("namespace '" + ns
                                + "' claimed by more than one provider");
                    ns_map.put(ns, provider);
                    adverts.add(advert);
                }
            }
        }
        if (!this.m_module.set_capabilities(adverts)) return false;
        synchronized (m_lock) {
            this.m_ns_providers.clear();
            this.m_ns_providers.putAll(ns_map);
        }
        return true;
    }

    private static JSONObject _parse_advert(DeCapabilityProvider provider, Object advert) {
        if (advert instanceof JSONObject) return (JSONObject) advert;
        try {
            return new JSONObject(String.valueOf(advert));
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid advert from "
                    + provider.getClass().getName() + ": " + e.getMessage());
        }
    }

    private DeCapabilityProvider _provider_for(String ns) {
        synchronized (m_lock) {
            return this.m_ns_providers.get(ns);
        }
    }
}
