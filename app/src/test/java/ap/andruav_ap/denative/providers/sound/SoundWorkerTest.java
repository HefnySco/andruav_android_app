package ap.andruav_ap.denative.providers.sound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;

/**
 * SoundWorker choke-point checks on a fake backend (AN-0042): dispatch,
 * the started/finished fact pairs, loop stop/preempt/limit endings, the
 * muted-side drop happening upstream (covered in SoundProviderTest), and
 * the bounded FIFO's drop-oldest policy.
 */
public class SoundWorkerTest {

    private static final long TIMEOUT_MS = 4000;

    /** Latch-driven playback: the test decides when it ends. */
    private static final class FakePlayback implements SoundWorker.Playback {
        final CountDownLatch ended = new CountDownLatch(1);
        final AtomicInteger result = new AtomicInteger(RESULT_DONE);
        volatile boolean stop_requested;

        @Override
        public int awaitEnd() throws InterruptedException {
            ended.await();
            return result.get();
        }

        @Override
        public void requestStop() {
            stop_requested = true;
            finish(RESULT_STOPPED);
        }

        void finish(int r) {
            result.set(r);
            ended.countDown();
        }
    }

    /** Records every started item and hands out FakePlayback handles,
     *  auto-finishing them after autoFinishMs when > 0. */
    private static final class FakeBackend implements SoundWorker.Backend {
        final BlockingQueue<SoundWorker.Item> items = new LinkedBlockingQueue<>();
        final BlockingQueue<FakePlayback> playbacks = new LinkedBlockingQueue<>();
        volatile long autoFinishMs = 0;
        volatile int autoResult = SoundWorker.Playback.RESULT_DONE;
        private final ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        @Override
        public SoundWorker.Playback start(SoundWorker.Item item) {
            items.offer(item);
            final FakePlayback playback = new FakePlayback();
            playbacks.offer(playback);
            if (autoFinishMs > 0) {
                scheduler.schedule(() -> playback.finish(autoResult),
                        autoFinishMs, TimeUnit.MILLISECONDS);
            }
            return playback;
        }

        SoundWorker.Item nextItem() throws InterruptedException {
            return items.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }

        FakePlayback nextPlayback() throws InterruptedException {
            return playbacks.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
    }

    private static final class FactRecorder implements SoundWorker.Listener {
        final BlockingQueue<String> facts = new LinkedBlockingQueue<>();
        final BlockingQueue<Boolean> playing = new LinkedBlockingQueue<>();

        @Override
        public void onFact(String clip_key, String fact, Map<String, Object> payload) {
            facts.offer(clip_key + "." + fact + " " + payload);
        }

        @Override
        public void onPlaying(boolean p) {
            playing.offer(p);
        }

        String nextFact() throws InterruptedException {
            return facts.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
    }

    private final java.util.List<SoundWorker> workers = new java.util.ArrayList<>();

    private SoundWorker newWorker(SoundWorker.Backend backend, long loopMaxMs) {
        SoundWorker worker = new SoundWorker(backend, loopMaxMs);
        workers.add(worker);
        worker.start();
        return worker;
    }

    @After
    public void tearDown() {
        for (SoundWorker worker : workers) worker.shutdown();
        workers.clear();
    }

    @Test
    public void playItemEmitsStartedFinishedPair() throws Exception {
        FakeBackend backend = new FakeBackend();
        FactRecorder recorder = new FactRecorder();
        SoundWorker worker = newWorker(backend, 60000);
        worker.setListener(recorder);

        worker.enqueue(SoundWorker.Item.play("siren", "builtin:siren",
                SoundWorker.SRC_EVENT, false));

        SoundWorker.Item item = backend.nextItem();
        assertNotNull(item);
        assertEquals("siren", item.clip_key);
        assertEquals("event", item.src);

        assertEquals("siren.started {src=event, loop=false}", recorder.nextFact());
        backend.nextPlayback().finish(SoundWorker.Playback.RESULT_DONE);
        assertEquals("siren.finished {src=event, reason=done}", recorder.nextFact());
    }

    @Test
    public void sayItemEmitsFactsUnderSayKey() throws Exception {
        FakeBackend backend = new FakeBackend();
        FactRecorder recorder = new FactRecorder();
        SoundWorker worker = newWorker(backend, 60000);
        worker.setListener(recorder);

        worker.enqueue(SoundWorker.Item.say("test", null, 80, -1,
                SoundWorker.SRC_GCS));

        SoundWorker.Item item = backend.nextItem();
        assertNotNull(item);
        assertEquals(SoundWorker.KIND_SAY, item.kind);
        assertEquals("test", item.text);
        assertEquals(80, item.pitch);

        assertEquals("say.started {src=gcs, loop=false}", recorder.nextFact());
        backend.nextPlayback().finish(SoundWorker.Playback.RESULT_DONE);
        assertEquals("say.finished {src=gcs, reason=done}", recorder.nextFact());
    }

    @Test
    public void stopClipEndsLoopWithStopped() throws Exception {
        FakeBackend backend = new FakeBackend();
        backend.autoFinishMs = 20;
        FactRecorder recorder = new FactRecorder();
        SoundWorker worker = newWorker(backend, 60000);
        worker.setListener(recorder);

        worker.enqueue(SoundWorker.Item.play("siren", "builtin:siren",
                SoundWorker.SRC_INVOKE, true));
        assertEquals("siren.started {src=invoke, loop=true}", recorder.nextFact());

        // let a couple of iterations pass, then stop the clip
        assertNotNull(backend.nextItem());
        Thread.sleep(60);
        assertTrue(worker.isClipPlaying("siren"));
        assertTrue(worker.stopClip("siren"));

        String finished = recorder.nextFact();
        assertNotNull(finished);
        assertEquals("siren.finished {src=invoke, reason=stopped}", finished);
    }

    @Test
    public void queuedItemPreemptsLoopBetweenIterations() throws Exception {
        FakeBackend backend = new FakeBackend();
        FactRecorder recorder = new FactRecorder();
        SoundWorker worker = newWorker(backend, 60000);
        worker.setListener(recorder);

        worker.enqueue(SoundWorker.Item.play("siren", "builtin:siren",
                SoundWorker.SRC_EVENT, true));
        assertEquals("siren.started {src=event, loop=true}", recorder.nextFact());
        assertNotNull(backend.nextItem());

        // queue a follow-up while the loop's first iteration is playing
        worker.enqueue(SoundWorker.Item.play("beep", "builtin:beep",
                SoundWorker.SRC_INVOKE, false));
        backend.nextPlayback().finish(SoundWorker.Playback.RESULT_DONE);

        assertEquals("siren.finished {src=event, reason=preempted}", recorder.nextFact());
        assertEquals("beep.started {src=invoke, loop=false}", recorder.nextFact());
    }

    @Test
    public void loopHitsTheLoopMaxLimit() throws Exception {
        FakeBackend backend = new FakeBackend();
        backend.autoFinishMs = 15;
        FactRecorder recorder = new FactRecorder();
        // loop_max 120 ms -> a few 15 ms iterations, then "limit"
        SoundWorker worker = newWorker(backend, 120);
        worker.setListener(recorder);

        worker.enqueue(SoundWorker.Item.play("siren", "builtin:siren",
                SoundWorker.SRC_INVOKE, true));
        assertEquals("siren.started {src=invoke, loop=true}", recorder.nextFact());
        String finished = recorder.nextFact();
        assertEquals("siren.finished {src=invoke, reason=limit}", finished);
    }

    @Test
    public void stopAllEndsCurrentAndDropsQueue() throws Exception {
        FakeBackend backend = new FakeBackend();
        FactRecorder recorder = new FactRecorder();
        SoundWorker worker = newWorker(backend, 60000);
        worker.setListener(recorder);

        worker.enqueue(SoundWorker.Item.play("siren", "builtin:siren",
                SoundWorker.SRC_GCS, false));
        assertEquals("siren.started {src=gcs, loop=false}", recorder.nextFact());
        assertNotNull(backend.nextItem());
        worker.enqueue(SoundWorker.Item.play("beep", "builtin:beep",
                SoundWorker.SRC_GCS, false));

        assertTrue(worker.stopAll());
        assertEquals("siren.finished {src=gcs, reason=stopped}", recorder.nextFact());
        // the queued beep was dropped - no more facts arrive
        assertNull(recorder.facts.poll(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void unkeyedFilePlayStaysSilent() throws Exception {
        FakeBackend backend = new FakeBackend();
        backend.autoFinishMs = 20;
        FactRecorder recorder = new FactRecorder();
        SoundWorker worker = newWorker(backend, 60000);
        worker.setListener(recorder);

        // legacy play of a file that maps to no clip key: plays but no facts
        worker.enqueue(SoundWorker.Item.play(null, "random.wav",
                SoundWorker.SRC_GCS, false));
        assertNotNull(backend.nextItem());
        assertNull(recorder.facts.poll(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void boundedFifoDropsOldestOnOverflow() throws Exception {
        FakeBackend backend = new FakeBackend();
        SoundWorker worker = new SoundWorker(backend, 60000, 2);
        workers.add(worker);
        worker.start();

        // first item is taken immediately and blocks on its playback
        worker.enqueue(SoundWorker.Item.play("siren", "builtin:siren",
                SoundWorker.SRC_GCS, false));
        assertNotNull(backend.nextItem());

        worker.enqueue(SoundWorker.Item.play("c1", "builtin:beep", SoundWorker.SRC_GCS, false));
        worker.enqueue(SoundWorker.Item.play("c2", "builtin:beep", SoundWorker.SRC_GCS, false));
        worker.enqueue(SoundWorker.Item.play("c3", "builtin:beep", SoundWorker.SRC_GCS, false));

        // capacity 2: "c1" was evicted for "c3"
        FakePlayback first = backend.nextPlayback();
        assertNotNull(first);
        first.finish(SoundWorker.Playback.RESULT_DONE);

        SoundWorker.Item second = backend.nextItem();
        assertNotNull(second);
        assertEquals("c2", second.clip_key);
        // c2 is now the item in progress; end it so the worker takes c3
        FakePlayback secondPlayback = backend.nextPlayback();
        assertNotNull(secondPlayback);
        secondPlayback.finish(SoundWorker.Playback.RESULT_DONE);
        SoundWorker.Item third = backend.nextItem();
        assertNotNull(third);
        assertEquals("c3", third.clip_key);
    }

    @Test
    public void backendErrorEndsItemWithErrorReason() throws Exception {
        FakeBackend backend = new FakeBackend();
        backend.autoFinishMs = 10;
        backend.autoResult = SoundWorker.Playback.RESULT_ERROR;
        FactRecorder recorder = new FactRecorder();
        SoundWorker worker = newWorker(backend, 60000);
        worker.setListener(recorder);

        worker.enqueue(SoundWorker.Item.play("beep", "builtin:beep",
                SoundWorker.SRC_INVOKE, false));
        assertEquals("beep.started {src=invoke, loop=false}", recorder.nextFact());
        assertEquals("beep.finished {src=invoke, reason=error}", recorder.nextFact());
    }

    @Test
    public void playingStateTracksTheQueue() throws Exception {
        FakeBackend backend = new FakeBackend();
        FactRecorder recorder = new FactRecorder();
        SoundWorker worker = newWorker(backend, 60000);
        worker.setListener(recorder);

        assertFalse(worker.isPlaying());
        worker.enqueue(SoundWorker.Item.play("beep", "builtin:beep",
                SoundWorker.SRC_GCS, false));
        assertEquals(Boolean.TRUE, recorder.playing.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNotNull(backend.nextItem());
        assertTrue(worker.isPlaying());
        backend.nextPlayback().finish(SoundWorker.Playback.RESULT_DONE);
        assertEquals(Boolean.FALSE, recorder.playing.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
