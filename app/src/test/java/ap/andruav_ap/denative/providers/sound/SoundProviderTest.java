package ap.andruav_ap.denative.providers.sound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import de.databus.CapSchema;
import de.databus.caps.DeCapabilityProvider;

/**
 * SoundProvider surface checks (AN-0042 / TO-BE 5.1): the de.cap/1 advert
 * passes cap_schema, say/play/stop and the per-clip actions dispatch onto
 * the worker, the event commands enqueue or stop, and the muted toggle
 * answers {queued:false, muted:true}.
 */
public class SoundProviderTest {

    private static final long TIMEOUT_MS = 4000;

    /** Backend that finishes every item immediately and records it. */
    private static final class InstantBackend implements SoundWorker.Backend {
        final BlockingQueue<SoundWorker.Item> items = new LinkedBlockingQueue<>();

        @Override
        public SoundWorker.Playback start(SoundWorker.Item item) {
            items.offer(item);
            return new SoundWorker.Playback() {
                @Override
                public int awaitEnd() {
                    return RESULT_DONE;
                }

                @Override
                public void requestStop() {
                }
            };
        }

        SoundWorker.Item nextItem() throws InterruptedException {
            return items.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
    }

    private static final class Sink implements DeCapabilityProvider.ResultSink {
        final AtomicReference<JSONObject> ok = new AtomicReference<>();
        final AtomicReference<String> err = new AtomicReference<>();

        @Override
        public void ok(JSONObject data) {
            this.ok.set(data);
        }

        @Override
        public void err(String error) {
            this.err.set(error);
        }
    }

    private final java.util.List<SoundProvider> providers = new java.util.ArrayList<>();

    private SoundProvider newProvider(JSONObject config, SoundWorker.Backend backend,
                                      AtomicBoolean muted) {
        SoundProvider provider = new SoundProvider(config, "14.5.0", backend,
                Arrays.asList("en", "de"), muted::get);
        providers.add(provider);
        return provider;
    }

    private static JSONObject advertOf(SoundProvider provider) throws Exception {
        List<Object> adverts = provider.adverts();
        assertEquals(1, adverts.size());
        Object advert = adverts.get(0);
        return advert instanceof JSONObject
                ? (JSONObject) advert : new JSONObject(String.valueOf(advert));
    }

    @After
    public void tearDown() {
        for (SoundProvider provider : providers) provider.stop();
        providers.clear();
    }

    @Test
    public void advertPassesCapSchema() throws Exception {
        JSONObject config = new JSONObject().put("sound_files", new JSONArray()
                .put(new JSONObject()
                        .put("name", "Facility Siren")
                        .put("file", "facility.wav")));
        SoundProvider provider = newProvider(config, new InstantBackend(), new AtomicBoolean());

        JSONObject advert = advertOf(provider);
        String error = CapSchema.cap_validate_advert(advert);
        assertEquals("", error);

        assertEquals(CapSchema.CAP_SCHEMA_ID, advert.optString("schema"));
        assertEquals("sound", advert.optString("ns"));

        JSONObject actions = advert.optJSONObject("actions");
        assertNotNull(actions);
        assertTrue(actions.has("say"));
        assertTrue(actions.has("play"));
        assertTrue(actions.has("stop"));
        // one per-clip action per surfaced clip: builtins + the configured one
        assertTrue(actions.has("siren"));
        assertTrue(actions.has("beep"));
        assertTrue(actions.has("error"));
        assertTrue(actions.has("facility_siren"));

        JSONObject resources = advert.optJSONObject("resources");
        assertNotNull(resources);
        assertEquals("clip", resources.optString("siren"));
        assertEquals("clip", resources.optString("facility_siren"));

        JSONObject patterns = advert.optJSONObject("event_patterns");
        assertNotNull(patterns);
        assertEquals("command", patterns.optJSONObject("*.play").optString("kind"));
        assertEquals("command", patterns.optJSONObject("*.stop").optString("kind"));
        assertEquals("fact", patterns.optJSONObject("*.started").optString("kind"));
        assertEquals("fact", patterns.optJSONObject("*.finished").optString("kind"));

        assertTrue(advert.optJSONObject("state").has("playing"));
    }

    @Test
    public void clipKeysFollowSlugRules() throws Exception {
        JSONObject config = new JSONObject().put("sound_files", new JSONArray()
                .put(new JSONObject().put("name", "Facility Siren").put("file", "a.wav"))
                .put(new JSONObject().put("key", "siren").put("name", "Second Siren")
                        .put("file", "b.wav"))                       // key taken by the builtin
                .put(new JSONObject().put("name", "9th Alarm").put("file", "c.wav")));
        SoundProvider provider = newProvider(config, new InstantBackend(), new AtomicBoolean());

        List<SoundClips.Surface> surfaces = provider.surfaces();
        // builtins first: siren, beep, error
        assertEquals("siren", surfaces.get(0).key);
        assertEquals("beep", surfaces.get(1).key);
        assertEquals("error", surfaces.get(2).key);
        // "Facility Siren" -> slug
        assertEquals("facility_siren", surfaces.get(3).key);
        // digit-leading slug gains the clip_ prefix
        boolean found9 = false;
        for (SoundClips.Surface surface : surfaces) {
            if ("clip_9th_alarm".equals(surface.key)) found9 = true;
        }
        assertTrue(found9);
        // the duplicate "siren" key loses its surface - first wins
        long sirens = surfaces.stream().filter(s -> "siren".equals(s.key)).count();
        assertEquals(1, sirens);
        assertFalse(provider.warnings().isEmpty());
    }

    @Test
    public void reservedKeyClipKeepsEventsButNoAction() throws Exception {
        JSONObject config = new JSONObject().put("sound_files", new JSONArray()
                .put(new JSONObject().put("key", "play").put("name", "Play Clip")
                        .put("file", "play.wav")));
        SoundProvider provider = newProvider(config, new InstantBackend(), new AtomicBoolean());

        JSONObject advert = advertOf(provider);
        // "play" stays the reserved play action, not a per-clip one
        JSONObject play = advert.optJSONObject("actions").optJSONObject("play");
        assertNotNull(play.optJSONObject("params").optJSONObject("file"));
        // but the clip is surfaced: resources + commands keep working
        assertEquals("clip", advert.optJSONObject("resources").optString("play"));
        assertTrue(CapSchema.cap_validate_advert(advert).isEmpty());
    }

    @Test
    public void sayInvokeEnqueuesSpeech() throws Exception {
        InstantBackend backend = new InstantBackend();
        SoundProvider provider = newProvider(new JSONObject(), backend, new AtomicBoolean());

        Sink sink = new Sink();
        provider.invoke("i1", "sound", "say",
                new JSONObject().put("text", "test").put("pitch", 80), 0, sink);

        assertNull(sink.err.get());
        assertNotNull(sink.ok.get());
        assertTrue(sink.ok.get().optBoolean("queued"));

        SoundWorker.Item item = backend.nextItem();
        assertNotNull(item);
        assertEquals(SoundWorker.KIND_SAY, item.kind);
        assertEquals("test", item.text);
        assertEquals(80, item.pitch);
        assertEquals(SoundWorker.SRC_INVOKE, item.src);
    }

    @Test
    public void mutedSayReturnsQueuedFalseMuted() throws Exception {
        InstantBackend backend = new InstantBackend();
        AtomicBoolean muted = new AtomicBoolean(true);
        SoundProvider provider = newProvider(new JSONObject(), backend, muted);

        Sink sink = new Sink();
        provider.invoke("i2", "sound", "say",
                new JSONObject().put("text", "test"), 0, sink);

        assertNull(sink.err.get());
        JSONObject data = sink.ok.get();
        assertNotNull(data);
        assertFalse(data.optBoolean("queued"));
        assertTrue(data.optBoolean("muted"));
        assertNull(backend.items.poll());
    }

    @Test
    public void playInvokeResolvesClipAndQueues() throws Exception {
        InstantBackend backend = new InstantBackend();
        SoundProvider provider = newProvider(new JSONObject(), backend, new AtomicBoolean());

        Sink sink = new Sink();
        provider.invoke("i3", "sound", "play",
                new JSONObject().put("file", "beep"), 0, sink);

        assertNull(sink.err.get());
        assertTrue(sink.ok.get().optBoolean("queued"));
        SoundWorker.Item item = backend.nextItem();
        assertNotNull(item);
        assertEquals("beep", item.clip_key);
        assertEquals("builtin:beep", item.file);
        assertEquals(SoundWorker.SRC_INVOKE, item.src);
    }

    @Test
    public void playInvokeUnknownFileFails() throws Exception {
        SoundProvider provider = newProvider(new JSONObject(), new InstantBackend(),
                new AtomicBoolean());
        Sink sink = new Sink();
        provider.invoke("i4", "sound", "play",
                new JSONObject().put("file", "nope"), 0, sink);
        assertNull(sink.ok.get());
        assertTrue(sink.err.get().contains("unknown sound"));
    }

    @Test
    public void perClipActionEnqueuesWithLoop() throws Exception {
        InstantBackend backend = new InstantBackend();
        SoundProvider provider = newProvider(new JSONObject(), backend, new AtomicBoolean());

        Sink sink = new Sink();
        provider.invoke("i5", "sound", "siren",
                new JSONObject().put("loop", true), 0, sink);

        assertNull(sink.err.get());
        assertTrue(sink.ok.get().optBoolean("queued"));
        assertEquals("siren", sink.ok.get().optString("clip"));
        SoundWorker.Item item = backend.nextItem();
        assertNotNull(item);
        assertTrue(item.loop);
    }

    @Test
    public void eventPlayEnqueuesAndEventStopEndsClip() throws Exception {
        InstantBackend backend = new InstantBackend();
        SoundProvider provider = newProvider(new JSONObject(), backend, new AtomicBoolean());

        provider.onEvent("sound.siren.play", new JSONObject().put("loop", true), "n1");
        SoundWorker.Item item = backend.nextItem();
        assertNotNull(item);
        assertEquals("siren", item.clip_key);
        assertTrue(item.loop);
        assertEquals(SoundWorker.SRC_EVENT, item.src);

        provider.onEvent("sound.siren.stop", null, "n2");
        // the worker drops the loop on the next iteration boundary
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (provider.worker().isClipPlaying("siren")
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertFalse(provider.worker().isClipPlaying("siren"));
    }

    @Test
    public void eventUnknownClipIsIgnored() throws Exception {
        InstantBackend backend = new InstantBackend();
        SoundProvider provider = newProvider(new JSONObject(), backend, new AtomicBoolean());

        provider.onEvent("sound.ghost.play", new JSONObject(), "n3");
        provider.onEvent("sound.siren.explode", new JSONObject(), "n4");
        provider.onEvent("other.siren.play", new JSONObject(), "n5");
        assertNull(backend.items.poll(400, TimeUnit.MILLISECONDS));
    }

    @Test
    public void eventsDisabledGateSilencesCommands() throws Exception {
        InstantBackend backend = new InstantBackend();
        JSONObject config = new JSONObject().put("events_enabled", false);
        SoundProvider provider = newProvider(config, backend, new AtomicBoolean());

        provider.onEvent("sound.siren.play", new JSONObject().put("loop", true), "n6");
        assertNull(backend.items.poll(400, TimeUnit.MILLISECONDS));
    }

    @Test
    public void mutedEventPlayIsIgnored() throws Exception {
        InstantBackend backend = new InstantBackend();
        SoundProvider provider = newProvider(new JSONObject(), backend, new AtomicBoolean(true));

        provider.onEvent("sound.siren.play", new JSONObject(), "n7");
        assertNull(backend.items.poll(400, TimeUnit.MILLISECONDS));
    }

    @Test
    public void stateReportsPlaying() throws Exception {
        InstantBackend backend = new InstantBackend();
        SoundProvider provider = newProvider(new JSONObject(), backend, new AtomicBoolean());

        assertFalse(provider.state("sound").optBoolean("playing"));
        // a hanging playback flips state to playing
        SoundProvider hanging = newProvider(new JSONObject(), new SoundWorker.Backend() {
            @Override
            public SoundWorker.Playback start(SoundWorker.Item item) {
                return new SoundWorker.Playback() {
                    private final java.util.concurrent.CountDownLatch done =
                            new java.util.concurrent.CountDownLatch(1);

                    @Override
                    public int awaitEnd() throws InterruptedException {
                        done.await();
                        return RESULT_DONE;
                    }

                    @Override
                    public void requestStop() {
                        done.countDown();
                    }
                };
            }
        }, new AtomicBoolean());
        Sink sink = new Sink();
        hanging.invoke("i6", "sound", "siren", new JSONObject(), 0, sink);
        assertTrue(sink.ok.get().optBoolean("queued"));
        // wait for the worker to pick the item up
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (!hanging.state("sound").optBoolean("playing")
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(hanging.state("sound").optBoolean("playing"));
    }
}
