package org.droidplanner.services.android.impl.core.drone.manager;

import com.MAVLink.common.msg_command_ack;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_RESULT;
import com.o3dr.services.android.lib.model.ICommandListener;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DroneCommandTrackerTest {

    private static final int CMD = MAV_CMD.MAV_CMD_DO_SET_MODE;

    /** Scheduler with a manual clock: runnables run only when time is advanced. */
    private static class FakeScheduler implements DroneCommandTracker.Scheduler {
        private static class Task {
            final Runnable r;
            final long at;
            Task(Runnable r, long at) { this.r = r; this.at = at; }
        }

        private final List<Task> tasks = new ArrayList<>();
        private long now = 0;

        @Override
        public void post(Runnable r) {
            postDelayed(r, 0);
        }

        @Override
        public void postDelayed(Runnable r, long delayMillis) {
            tasks.add(new Task(r, now + delayMillis));
        }

        @Override
        public void removeCallbacks(Runnable r) {
            tasks.removeIf(t -> t.r == r);
        }

        void advance(long ms) {
            now += ms;
            boolean ran;
            do {
                ran = false;
                for (Task t : new ArrayList<>(tasks)) {
                    if (t.at <= now) {
                        tasks.remove(t);
                        t.r.run();
                        ran = true;
                    }
                }
            } while (ran);
        }
    }

    private static class RecordingListener implements ICommandListener {
        final List<String> events = new ArrayList<>();

        @Override public void onSuccess() { events.add("success"); }
        @Override public void onError(int executionError) { events.add("error:" + executionError); }
        @Override public void onTimeout() { events.add("timeout"); }
    }

    private FakeScheduler scheduler;
    private DroneCommandTracker tracker;
    private RecordingListener listener;

    @Before
    public void setUp() {
        scheduler = new FakeScheduler();
        tracker = new DroneCommandTracker(scheduler);
        listener = new RecordingListener();

        final msg_command_long cmd = new msg_command_long();
        cmd.command = CMD;
        tracker.onCommandSubmitted(cmd, listener);
    }

    private void ack(int result) {
        final msg_command_ack ack = new msg_command_ack();
        ack.command = CMD;
        ack.result = (short) result;
        ack.progress = 255;
        tracker.onCommandAck(msg_command_ack.MAVLINK_MSG_ID_COMMAND_ACK, ack);
    }

    @Test
    public void plainAccepted_completesWithSuccess() {
        ack(MAV_RESULT.MAV_RESULT_ACCEPTED);
        scheduler.advance(0);
        assertEquals(List.of("success"), listener.events);

        scheduler.advance(10_000);
        assertEquals(List.of("success"), listener.events);
    }

    @Test
    public void inProgressThenAccepted_completesWithSuccess() {
        ack(MAV_RESULT.MAV_RESULT_IN_PROGRESS);
        scheduler.advance(0);
        assertTrue(listener.events.isEmpty());

        ack(MAV_RESULT.MAV_RESULT_ACCEPTED);
        scheduler.advance(0);
        assertEquals(List.of("success"), listener.events);
    }

    @Test
    public void inProgressThenFailed_completesWithError() {
        ack(MAV_RESULT.MAV_RESULT_IN_PROGRESS);
        scheduler.advance(0);
        assertTrue(listener.events.isEmpty());

        ack(MAV_RESULT.MAV_RESULT_FAILED);
        scheduler.advance(0);
        assertEquals(List.of("error:" + MAV_RESULT.MAV_RESULT_FAILED), listener.events);
    }

    @Test
    public void inProgress_restartsTimeout() {
        // Progress keeps arriving every 1.5 s for 6 s: longer than the 2 s timeout overall.
        for (int i = 0; i < 4; i++) {
            scheduler.advance(1500);
            ack(MAV_RESULT.MAV_RESULT_IN_PROGRESS);
        }
        assertTrue(listener.events.isEmpty());

        ack(MAV_RESULT.MAV_RESULT_ACCEPTED);
        scheduler.advance(0);
        assertEquals(List.of("success"), listener.events);
    }

    @Test
    public void inProgressThenSilence_timesOut() {
        ack(MAV_RESULT.MAV_RESULT_IN_PROGRESS);
        scheduler.advance(1999);
        assertTrue(listener.events.isEmpty());

        scheduler.advance(1);
        assertEquals(List.of("timeout"), listener.events);
    }

    @Test
    public void noAck_timesOut() {
        scheduler.advance(2000);
        assertEquals(List.of("timeout"), listener.events);
    }
}
