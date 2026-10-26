package ap.andruav_ap.denative.providers.phone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The phone output surface: one {@link PhoneOutput} choke point per
 * configured resource name, plus the command/action dispatch shared by
 * the databus provider ({@link PhoneProvider}) and the legacy callers
 * (Emergency). Pure Java - the Android drivers and the bus fact channel
 * are injected - so the whole dispatch is JVM-testable.
 *
 * src values (which surface caused a change): legacy | invoke | event |
 * pulse | input | emergency - same as gpio.
 */
public final class PhoneOutputBank {

    /** Builds the hardware driver for a configured resource. */
    public interface DriverFactory {
        PhoneOutput.Driver createDriver(PhoneOutputSpec spec);
    }

    /** Edge-fact channel toward the databus; {@code emit(key, "went_on", v, src)}. */
    public interface FactEmitter {
        void emit(String key, String verb, int v, String src);
    }

    private final Map<String, PhoneOutput> m_outputs = new LinkedHashMap<>();
    private final List<PhoneOutputSpec> m_specs = new ArrayList<>();
    private volatile FactEmitter m_fact_emitter;

    /**
     * @param specs     configured resources; the first spec wins on a
     *                  duplicate name (same rule as gpio pin keys)
     * @param factory   kind -> hardware driver; may return null for a
     *                  stubbed/missing device
     * @param scheduler pulse-restore scheduler for every choke point
     */
    public PhoneOutputBank(final List<PhoneOutputSpec> specs, final DriverFactory factory,
                           final PhoneOutput.Scheduler scheduler) {
        if (specs != null) {
            for (final PhoneOutputSpec spec : specs) {
                if (spec == null || spec.name == null || spec.name.isEmpty()) continue;
                if (m_outputs.containsKey(spec.name)) continue; // first wins on a key collision
                final PhoneOutput.Driver driver =
                        factory != null ? factory.createDriver(spec) : null;
                final PhoneOutput output = new PhoneOutput(spec, driver,
                        (verb, v, src) -> _emit_fact(spec.name, verb, v, src), scheduler);
                m_outputs.put(spec.name, output);
                m_specs.add(spec);
            }
        }
    }

    /** Attach/replace the fact channel (the provider wires this once a host exists). */
    public void setFactEmitter(final FactEmitter emitter) {
        m_fact_emitter = emitter;
    }

    public PhoneOutput output(final String key) {
        return m_outputs.get(key);
    }

    public boolean has(final String key) {
        return m_outputs.containsKey(key);
    }

    /** Advertised order preserved. */
    public List<PhoneOutputSpec> specs() {
        return Collections.unmodifiableList(m_specs);
    }

    /** Current level snapshot for the namespace state (6545). */
    public Map<String, Integer> stateMap() {
        final Map<String, Integer> state = new LinkedHashMap<>();
        for (final Map.Entry<String, PhoneOutput> entry : m_outputs.entrySet()) {
            state.put(entry.getKey(), entry.getValue().isOn() ? 1 : 0);
        }
        return state;
    }

    /** Explicit write on a named resource. Returns null or an error string. */
    public String write(final String key, final boolean on, final String src) {
        final PhoneOutput output = m_outputs.get(key);
        if (output == null) return "unknown resource '" + key + "'";
        return output.write(on, src);
    }

    /**
     * Async pulse on a named resource ({@code *.pulse} command and the
     * generic {@code pulse} action can also pick the blocking variant).
     * {@code pulseMs} <= 0 means "the resource's configured default".
     */
    public String pulse(final String key, final int pulseMs) {
        final PhoneOutput output = m_outputs.get(key);
        if (output == null) return "unknown resource '" + key + "'";
        return output.pulse(_resolve_pulse_ms(output, pulseMs));
    }

    /** Invoke variant: blocks until the restore ran. */
    public String pulseBlocking(final String key, final int pulseMs) {
        final PhoneOutput output = m_outputs.get(key);
        if (output == null) return "unknown resource '" + key + "'";
        return output.pulseBlocking(_resolve_pulse_ms(output, pulseMs));
    }

    /**
     * An event command {@code <ns>.<key>.<verb>} - on | off | toggle |
     * pulse {ms?}. The spec's {@code events} flag gates this whole
     * surface. Returns null when handled, else the reason it was ignored.
     */
    public String command(final String key, final String verb, final Integer ms, final String src) {
        final PhoneOutput output = m_outputs.get(key);
        if (output == null) return "unknown resource '" + key + "'";
        if (!output.spec().events) return "events disabled";

        switch (verb) {
            case "on":     return output.write(true, src);
            case "off":    return output.write(false, src);
            case "toggle": return output.toggle(src);
            case "pulse": {
                final int pulse_ms = (ms != null) ? ms : output.spec().pulseMs;
                if (pulse_ms < 1 || pulse_ms > PhoneOutput.PULSE_MS_MAX) {
                    return "pulse ms must be 1.." + PhoneOutput.PULSE_MS_MAX;
                }
                return output.pulse(pulse_ms);
            }
            default:
                return "unknown verb '" + verb + "'";
        }
    }

    /** Cancel pending restores; current levels stay. */
    public void shutdown() {
        for (final PhoneOutput output : m_outputs.values()) {
            output.shutdown();
        }
    }

    private void _emit_fact(final String key, final String verb, final int v, final String src) {
        final FactEmitter emitter = m_fact_emitter;
        if (emitter != null) {
            emitter.emit(key, verb, v, src);
        }
    }

    private static int _resolve_pulse_ms(final PhoneOutput output, final int pulse_ms) {
        return pulse_ms > 0 ? pulse_ms : output.spec().pulseMs;
    }
}
