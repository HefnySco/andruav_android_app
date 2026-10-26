package ap.andruav_ap.denative.providers.camera;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import de.databus.CapSchema;
import de.databus.caps.DeCapabilityProvider;

/**
 * CameraProvider surface checks (AN-0046 / TO-BE 5.4, TASK-EV-CAM): the
 * de.cap/1 advert passes cap_schema and carries the per-channel surface,
 * invokes and camera.<ch>.* commands dispatch onto the backend, and
 * backend edges produce went_recording/went_idle/captured facts.
 */
public class CameraProviderTest {

    /** Recording backend double: channels back+front, records every call. */
    private static final class FakeBackend implements CameraBackend {
        final List<CameraChannel> channels = Arrays.asList(
                new CameraChannel("back", "0", "camera 0"),
                new CameraChannel("front", "1", "camera 1"));
        final List<String> calls = new ArrayList<>();
        final java.util.Map<String, Boolean> recording = new java.util.HashMap<>();
        Listener listener;
        boolean failNext;

        @Override
        public List<CameraChannel> channels() { return channels; }

        @Override
        public String defaultChannelKey() { return "back"; }

        @Override
        public String resolveChannelKey(String channel) {
            if (channel == null || channel.isEmpty()) return defaultChannelKey();
            for (CameraChannel c : channels) {
                if (c.key.equals(channel) || c.id.equals(channel)) return c.key;
            }
            return null;
        }

        @Override
        public String activeChannelKey() { return "back"; }

        @Override
        public void setListener(Listener l) { this.listener = l; }

        @Override
        public boolean startCapture(String channelKey, int count, int intervalMs,
                                    boolean gcsPreview, String invokeId, String src,
                                    String[] err) {
            calls.add("capture:" + channelKey + ":" + count + ":" + intervalMs
                    + ":" + gcsPreview + ":" + src + ":" + invokeId);
            return !failNext;
        }

        @Override
        public boolean setRecording(String channelKey, boolean on, String src, String[] err) {
            calls.add("record:" + channelKey + ":" + on + ":" + src);
            recording.put(channelKey, on);
            return !failNext;
        }

        @Override
        public boolean isRecording(String channelKey) {
            return Boolean.TRUE.equals(recording.get(channelKey));
        }

        @Override
        public boolean anyRecording() {
            return recording.containsValue(Boolean.TRUE);
        }

        @Override
        public boolean setZoom(String channelKey, double level, String[] err) {
            calls.add("zoom:" + channelKey + ":" + level);
            return !failNext;
        }
    }

    private static final class Sink implements DeCapabilityProvider.ResultSink {
        final AtomicReference<JSONObject> ok = new AtomicReference<>();
        final AtomicReference<String> err = new AtomicReference<>();

        @Override public void ok(JSONObject data) { ok.set(data); }
        @Override public void err(String error) { err.set(error); }
    }

    /** Provider subclass that captures emitted facts instead of a bus host. */
    private static final class TestProvider extends CameraProvider {
        final List<String> facts = new ArrayList<>();

        TestProvider(CameraBackend backend) {
            super(backend, "14.5.0");
        }

        @Override
        protected void emit(String name, JSONObject payload) {
            facts.add(name + " " + payload);
        }
    }

    private static TestProvider newProvider(FakeBackend backend) {
        return new TestProvider(backend);
    }

    private static Sink invoke(CameraProvider provider, String act, JSONObject params) {
        Sink sink = new Sink();
        provider.invoke("inv-1", "camera", act, params, Long.MAX_VALUE, sink);
        return sink;
    }

    @Test
    public void advertValidates() throws Exception {
        TestProvider provider = newProvider(new FakeBackend());
        List<Object> adverts = provider.adverts();
        assertEquals(1, adverts.size());
        JSONObject advert = new JSONObject((String) adverts.get(0));
        assertEquals("", CapSchema.cap_validate_advert(advert));
        assertEquals("de.cap/1", advert.getString("schema"));
        assertEquals("camera", advert.getString("ns"));

        JSONObject actions = advert.getJSONObject("actions");
        assertTrue(actions.has("capture"));
        assertTrue(actions.has("record"));
        assertTrue(actions.has("zoom"));
        assertTrue("per-channel action camera.back", actions.has("back"));
        assertTrue("per-channel action camera.front", actions.has("front"));

        JSONObject resources = advert.getJSONObject("resources");
        assertEquals("channel", resources.getString("back"));
        assertEquals("channel", resources.getString("front"));

        JSONObject patterns = advert.getJSONObject("event_patterns");
        assertEquals("command", patterns.getJSONObject("*.record_on").getString("kind"));
        assertEquals("fact", patterns.getJSONObject("*.went_idle").getString("kind"));

        JSONObject captured = advert.getJSONObject("events").getJSONObject("captured")
                .getJSONObject("payload");
        assertTrue(captured.has("file"));
        assertTrue(captured.has("pos"));
        assertTrue(captured.has("ch"));
    }

    @Test
    public void invokeCaptureDispatches() throws Exception {
        FakeBackend backend = new FakeBackend();
        TestProvider provider = newProvider(backend);

        Sink sink = invoke(provider, "capture",
                new JSONObject().put("count", 2).put("interval_ms", 500));
        assertNotNull(sink.ok.get());
        assertTrue(sink.ok.get().getBoolean("started"));
        assertEquals("back", sink.ok.get().getString("ch"));
        assertTrue(backend.calls.contains("capture:back:2:500:false:invoke:inv-1"));
    }

    @Test
    public void invokeRecordAndPerChannelAction() throws Exception {
        FakeBackend backend = new FakeBackend();
        TestProvider provider = newProvider(backend);

        Sink on = invoke(provider, "record",
                new JSONObject().put("on", true).put("channel", "back"));
        assertNotNull(on.ok.get());
        assertTrue(on.ok.get().getBoolean("recording"));

        Sink off = invoke(provider, "back", new JSONObject().put("record", false));
        assertNotNull(off.ok.get());
        assertEquals("back", off.ok.get().getString("ch"));

        assertTrue(backend.calls.contains("record:back:true:invoke"));
        assertTrue(backend.calls.contains("record:back:false:invoke"));
    }

    @Test
    public void invokeZoomDispatches() throws Exception {
        FakeBackend backend = new FakeBackend();
        TestProvider provider = newProvider(backend);
        Sink sink = invoke(provider, "zoom",
                new JSONObject().put("level", 3.0).put("channel", "front"));
        assertNotNull(sink.ok.get());
        assertTrue(backend.calls.contains("zoom:front:3.0"));
    }

    @Test
    public void invokeUnknownActionFails() throws Exception {
        TestProvider provider = newProvider(new FakeBackend());
        Sink sink = invoke(provider, "explode", new JSONObject());
        assertNull(sink.ok.get());
        assertNotNull(sink.err.get());
    }

    @Test
    public void invokeUnknownChannelFails() throws Exception {
        TestProvider provider = newProvider(new FakeBackend());
        Sink sink = invoke(provider, "record",
                new JSONObject().put("on", true).put("channel", "nose"));
        assertNull(sink.ok.get());
        assertNotNull(sink.err.get());
    }

    @Test
    public void eventCommandsDispatch() throws Exception {
        FakeBackend backend = new FakeBackend();
        TestProvider provider = newProvider(backend);

        provider.onEvent("camera.back.record_on", new JSONObject(), "n1");
        provider.onEvent("camera.back.record_off", new JSONObject(), "n2");
        provider.onEvent("camera.front.capture",
                new JSONObject().put("count", 2).put("interval_ms", 100), "n3");

        assertTrue(backend.calls.contains("record:back:true:event"));
        assertTrue(backend.calls.contains("record:back:false:event"));
        assertTrue(backend.calls.contains("capture:front:2:100:false:event:null"));
    }

    @Test
    public void eventUnknownChannelOrVerbIgnored() throws Exception {
        FakeBackend backend = new FakeBackend();
        TestProvider provider = newProvider(backend);

        provider.onEvent("camera.ghost.record_on", new JSONObject(), "n1");
        provider.onEvent("camera.back.explode", new JSONObject(), "n2");
        provider.onEvent("camera.back.capture",
                new JSONObject().put("count", 0), "n3");   // out of range
        provider.onEvent("sound.clip.play", new JSONObject(), "n4"); // wrong ns

        assertTrue(backend.calls.isEmpty());
    }

    @Test
    public void backendEdgesEmitFacts() throws Exception {
        FakeBackend backend = new FakeBackend();
        TestProvider provider = newProvider(backend);

        backend.listener.onWentRecording("back", "invoke", "v_1.mp4");
        backend.listener.onWentIdle("back", "invoke", "command", "v_1.mp4");
        backend.listener.onCaptured("back", "FPV_IMG_1.jpg",
                new double[]{30.0, 31.0, 25.0}, 1234L, 1, "inv-9");

        assertEquals(3, backend.listener != null ? provider.facts.size() : 0);

        JSONObject rec = new JSONObject(provider.facts.get(0).substring(
                provider.facts.get(0).indexOf(' ') + 1));
        assertTrue(provider.facts.get(0).startsWith("camera.back.went_recording "));
        assertEquals("invoke", rec.getString("src"));
        assertEquals("v_1.mp4", rec.getString("file"));

        JSONObject idle = new JSONObject(provider.facts.get(1).substring(
                provider.facts.get(1).indexOf(' ') + 1));
        assertTrue(provider.facts.get(1).startsWith("camera.back.went_idle "));
        assertEquals("command", idle.getString("reason"));

        JSONObject captured = new JSONObject(provider.facts.get(2).substring(
                provider.facts.get(2).indexOf(' ') + 1));
        assertTrue(provider.facts.get(2).startsWith("camera.captured "));
        assertEquals("FPV_IMG_1.jpg", captured.getString("file"));
        assertEquals("back", captured.getString("ch"));
        JSONArray pos = captured.getJSONArray("pos");
        assertEquals(30.0, pos.getDouble(0), 1e-6);
        assertEquals("inv-9", captured.getString("inv"));
    }

    @Test
    public void stateReflectsRecording() throws Exception {
        FakeBackend backend = new FakeBackend();
        TestProvider provider = newProvider(backend);
        assertEquals(false, provider.state("camera").getBoolean("recording"));
        backend.recording.put("back", true);
        assertEquals(true, provider.state("camera").getBoolean("recording"));
    }
}
