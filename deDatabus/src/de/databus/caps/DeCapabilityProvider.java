package de.databus.caps;

import java.util.List;
import org.json.JSONObject;

/**
 * A capability namespace owner on the Android unit. The CapabilityHost
 * routes 6543 invokes, 1061 events and 6545 state reads to the provider
 * that advertised the namespace.
 */
public interface DeCapabilityProvider {

    /** Result channel for an invoke — call exactly once. */
    interface ResultSink {
        /** ok result; data may be null. */
        void ok(JSONObject data);
        /** failure result; err is machine-readable text. */
        void err(String err);
    }

    /** One de.cap/1 advert per owned namespace (JSON string or JSONObject). */
    List<Object> adverts();

    /** Handle a 6543 invoke. Params arrive validated with defaults filled;
     *  deadlineMs is the wall-clock time (epoch millis) the answer is due
     *  by. Must be safe to call on a worker thread; must answer the sink. */
    void invoke(String id, String ns, String act, JSONObject params,
                long deadlineMs, ResultSink sink);

    /** A locally delivered Sync_EventFire for an owned namespace:
     *  d="<ns>.<ev>", ed payload, n nonce. */
    void onEvent(String name, JSONObject ed, String nonce);

    /** Current state snapshot for a namespace (may be empty, not null). */
    JSONObject state(String ns);
}
