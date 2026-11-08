package ap.andruav_ap.denative.providers.phone;

/**
 * Single choke point for one named phone output resource, mirroring the
 * de_rpi_gpio pin rules carried over to the {@code phone} namespace
 * (TO-BE section 5.2 / PROTOCOL.md "Android unit namespaces"):
 *
 *  - every surface (invoke, event command, legacy Emergency) ends here;
 *  - edge-only facts {@code went_on} / {@code went_off} {@code {v, src}};
 *  - a pulse asserts the on-level for {@code ms} then restores the level
 *    that was there before; a second pulse while one runs is rejected;
 *  - any explicit (non-pulse) write bumps the generation counter, so a
 *    write during a pulse - same level or not - cancels the pulse's
 *    restore (GF-04);
 *  - {@code events:false} in the spec gates the whole event surface -
 *    no facts and no event commands (invokes are unaffected).
 *
 * Pure Java on purpose (no Android, no org.json) so the choke point is
 * unit-testable on the JVM with faked drivers and a manual scheduler.
 */
public final class PhoneOutput {

    /** Hardware surface: applies the logical level; returns null or an error string. */
    public interface Driver {
        String apply(boolean on);
    }

    /**
     * AN-0119: a driver that also takes a timed-blink rate — a PWM pin in
     * the registry sense (v = frequency Hz, d = duty 0..1024), not
     * hardware PWM.
     */
    public interface RateDriver extends Driver {
        String apply(boolean on, double hz, int duty);
    }

    /** Max blink rate: the 40 ms minimum half period of TorchDriver. */
    public static final double MAX_STROBE_HZ = 12.5;

    /** Edge-fact channel; {@code verb} is "went_on" or "went_off". */
    public interface FactSink {
        void onFact(String verb, int v, String src);
    }

    /** Delayed-work surface for the pulse restore (a manual queue in tests). */
    public interface Scheduler {
        void schedule(long delayMs, Runnable task);
    }

    // src values, shared with the gpio surface (PROTOCOL.md).
    public static final String SRC_LEGACY = "legacy";
    public static final String SRC_INVOKE = "invoke";
    public static final String SRC_EVENT = "event";
    public static final String SRC_PULSE = "pulse";
    public static final String SRC_INPUT = "input";
    public static final String SRC_EMERGENCY = "emergency";

    /** pulse {@code ms} / {@code pulse_ms} / {@code length_ms} bound (PROTOCOL.md). */
    public static final int PULSE_MS_MAX = 4000;

    /** {@code phone.strobe} full blink period: the shortest is {@link #MAX_STROBE_HZ}. */
    public static final int STROBE_PERIOD_MIN_MS = 80;
    public static final int STROBE_PERIOD_MAX_MS = 4000;
    public static final int STROBE_PERIOD_DEFAULT_MS = 250;
    /** {@code phone.strobe} duty cycle in percent. */
    public static final int STROBE_DUTY_DEFAULT = 50;

    private final PhoneOutputSpec m_spec;
    private final Driver m_driver;
    private final FactSink m_fact_sink;
    private final Scheduler m_scheduler;

    private boolean m_on;
    private long m_generation;
    private boolean m_pulse_active;
    private boolean m_pulse_restore_on;
    private double m_pwm_hz;
    private int m_pwm_duty;

    /** One in-flight pulse's restore bookkeeping. */
    private static final class PulseRun {
        long generation;
        boolean restore_on;
        String err;
    }

    public PhoneOutput(final PhoneOutputSpec spec, final Driver driver,
                       final FactSink fact_sink, final Scheduler scheduler) {
        this.m_spec = spec;
        this.m_driver = driver;
        this.m_fact_sink = fact_sink;
        this.m_scheduler = scheduler;
    }

    public PhoneOutputSpec spec() {
        return m_spec;
    }

    public synchronized boolean isOn() {
        return m_on;
    }

    public synchronized boolean isPulseActive() {
        return m_pulse_active;
    }

    /** Last written blink rate (Hz, clamped); what the pin status reports. */
    public synchronized double pwmHz() {
        return m_pwm_hz;
    }

    /** Last written blink duty (0..1024). */
    public synchronized int pwmDuty() {
        return m_pwm_duty;
    }

    /**
     * AN-0119: PWM-mode write - {@code hz} is the blink frequency (clamped
     * to {@link #MAX_STROBE_HZ}), {@code duty} the lit share 0..1024,
     * {@code on} is {@code hz > 0}. Same edge/fact rules as
     * {@link #write}; a plain driver gets the level only.
     */
    public String writePwm(final boolean on, final double hz, final int duty, final String src) {
        synchronized (this) {
            m_generation++;
            final double rate = Math.min(Math.max(hz, 0), MAX_STROBE_HZ);
            final int share = Math.max(0, Math.min(duty, 1024));
            if (on == m_on && rate == m_pwm_hz && share == m_pwm_duty) return null;
            if (m_driver != null) {
                final String err;
                try {
                    err = (m_driver instanceof RateDriver)
                            ? ((RateDriver) m_driver).apply(on, rate, share)
                            : m_driver.apply(on);
                } catch (Exception e) {
                    return "driver: " + e;
                }
                if (err != null) return err;
            }
            m_pwm_hz = rate;
            m_pwm_duty = share;
            if (on != m_on) {
                m_on = on;
                if (m_spec.events && m_fact_sink != null) {
                    m_fact_sink.onFact(on ? "went_on" : "went_off", on ? 1 : 0, src);
                }
            }
            return null;
        }
    }

    /**
     * Explicit write - the {@code on}/{@code off} commands, {@code write}
     * invokes and every legacy caller. Always bumps the generation so it
     * cancels a pending pulse restore; applies and emits a fact only on a
     * real edge. Returns null or an error string.
     */
    public String write(final boolean on, final String src) {
        synchronized (this) {
            m_generation++;
            return _apply_locked(on, src);
        }
    }

    /** Explicit write to the opposite level. */
    public String toggle(final String src) {
        synchronized (this) {
            m_generation++;
            return _apply_locked(!m_on, src);
        }
    }

    /**
     * Assert the on-level for {@code pulseMs} then restore the level that
     * was there before; the restore runs on the injected scheduler.
     * Returns an error string when another pulse is still in flight.
     */
    public String pulse(final int pulseMs) {
        final PulseRun run = _begin_pulse();
        if (run == null) return "pulse already in progress";
        if (m_scheduler != null) {
            m_scheduler.schedule(pulseMs, () -> endPulse(run));
        } else {
            endPulse(run);
        }
        return run.err;
    }

    /**
     * Invoke variant of {@link #pulse}: blocks the calling invoke worker
     * until the restore ran (like de_rpi_gpio's invokePin), so the result
     * goes out after the resource is back at its previous level.
     */
    public String pulseBlocking(final int pulseMs) {
        final PulseRun run = _begin_pulse();
        if (run == null) return "pulse already in progress";
        try {
            Thread.sleep(pulseMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        endPulse(run);
        return run.err;
    }

    /** Stop tracking a pending restore; the current level stays as-is. */
    public synchronized void shutdown() {
        m_generation++;
        m_pulse_active = false;
    }

    private PulseRun _begin_pulse() {
        final PulseRun run = new PulseRun();
        synchronized (this) {
            if (m_pulse_active) return null;
            m_pulse_active = true;
            run.restore_on = m_on;
            run.err = _apply_locked(true, SRC_PULSE);
            // snapshot AFTER the assert write: the assert is CAUSE_PULSE and
            // must not bump the generation it then compares against
            run.generation = m_generation;
        }
        return run;
    }

    private void endPulse(final PulseRun run) {
        synchronized (this) {
            m_pulse_active = false;
            if (m_generation == run.generation) {
                _apply_locked(run.restore_on, SRC_PULSE);
            }
        }
    }

    /**
     * The only place the level changes: hardware first, state update and
     * edge fact only on success. Caller holds the monitor.
     */
    private String _apply_locked(final boolean on, final String src) {
        if (on == m_on) return null;
        if (m_driver != null) {
            final String err;
            try {
                err = m_driver.apply(on);
            } catch (Exception e) {
                return "driver: " + e;
            }
            if (err != null) return err;
        }
        m_on = on;
        if (m_spec.events && m_fact_sink != null) {
            m_fact_sink.onFact(on ? "went_on" : "went_off", on ? 1 : 0, src);
        }
        return null;
    }
}
