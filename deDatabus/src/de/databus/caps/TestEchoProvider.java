package de.databus.caps;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;

/**
 * Debug-only provider exercising the de.cap/1 host path end to end:
 * namespace "test_echo" with a single "echo" action that returns its
 * "text" param unchanged. Wired into DeAndroidModuleService only when
 * BuildConfig.DEBUG.
 */
public class TestEchoProvider implements DeCapabilityProvider {

    private static final String ADVERT = "{"
            + "\"schema\":\"de.cap/1\","
            + "\"ns\":\"test_echo\","
            + "\"module\":\"andruav_phone\","
            + "\"ver\":\"1.0.0\","
            + "\"actions\":{"
            + "  \"echo\":{\"params\":{\"text\":{\"type\":\"string\"}}}"
            + "}"
            + "}";

    private final AtomicInteger m_echo_count = new AtomicInteger(0);
    private final AtomicInteger m_event_count = new AtomicInteger(0);

    @Override
    public List<Object> adverts() {
        List<Object> adverts = new ArrayList<>(1);
        adverts.add(ADVERT);
        return adverts;
    }

    @Override
    public void invoke(String id, String ns, String act, JSONObject params,
                       long deadlineMs, ResultSink sink) {
        if (!"echo".equals(act)) {
            sink.err("unknown action " + ns + "." + act);
            return;
        }
        m_echo_count.incrementAndGet();
        sink.ok(new JSONObject().put("text", params.optString("text", "")));
    }

    @Override
    public void onEvent(String name, JSONObject ed, String nonce) {
        m_event_count.incrementAndGet();
    }

    @Override
    public JSONObject state(String ns) {
        return new JSONObject().put("echoes", m_echo_count.get());
    }
}
