package ap.andruav_ap.denative.providers.phone;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.databus.CapSchema;

import ap.andruav_ap.App;

/**
 * Process-wide owner of the {@code phone} output choke points. Both the
 * databus provider ({@link PhoneProvider}, hosted by
 * DeAndroidModuleService) and the legacy callers (Emergency) reach the
 * same {@link PhoneOutputBank} through here, so every surface shares one
 * resource state.
 *
 * The bank is built lazily on first use - Emergency can flash before the
 * module service exists, and the provider attaches the fact channel when
 * it comes up.
 */
public final class PhoneDevices {

    private static final String TAG = "PhoneDevices";

    private static final Object s_lock = new Object();
    private static volatile PhoneOutputBank s_bank;

    private PhoneDevices() {
    }

    /**
     * The shared bank, or null when no app context exists yet (before
     * {@code App.onCreate}) - legacy callers keep their fallback in that
     * window.
     */
    public static PhoneOutputBank shared() {
        PhoneOutputBank bank = s_bank;
        if (bank == null) {
            final Context context = App.getAppContext();
            if (context == null) return null;
            bank = shared(context);
        }
        return bank;
    }

    /** Context-explicit variant for the service that hosts the provider. */
    public static PhoneOutputBank shared(final Context context) {
        PhoneOutputBank bank = s_bank;
        if (bank == null) {
            synchronized (s_lock) {
                bank = s_bank;
                if (bank == null) {
                    bank = _build(context.getApplicationContext());
                    s_bank = bank;
                }
            }
        }
        return bank;
    }

    /** One named output, or null. */
    public static PhoneOutput output(final String key) {
        final PhoneOutputBank bank = shared();
        return bank != null ? bank.output(key) : null;
    }

    /**
     * Default resources per TO-BE section 5.2: screen strobe, torch,
     * vibrate. A {@code phone_outputs[]} config block (module config
     * plumbing is a later task) can rename/extend these via
     * {@link #specsFromConfig}.
     */
    public static List<PhoneOutputSpec> defaultSpecs() {
        final List<PhoneOutputSpec> specs = new ArrayList<>(4);
        specs.add(new PhoneOutputSpec("screen", PhoneOutputSpec.KIND_SCREEN));
        // AN-0107: the torch is steady on/off; blinking is its own resource
        // over the same flash unit (the strobeHz default is the screen's)
        final PhoneOutputSpec torch = new PhoneOutputSpec("torch", PhoneOutputSpec.KIND_TORCH);
        torch.strobeHz = 0;
        specs.add(torch);
        specs.add(new PhoneOutputSpec("torch_strobe", PhoneOutputSpec.KIND_TORCH));
        specs.add(new PhoneOutputSpec("vibrate", PhoneOutputSpec.KIND_VIBRATE));
        return specs;
    }

    /**
     * Parse a {@code phone_outputs[]} config array:
     * {@code {name, kind: screen|torch|vibrate, pulse_ms?, events?,
     * strobe_hz?, color?}}. Invalid names/kinds are skipped and logged;
     * an absent/empty array falls back to {@link #defaultSpecs()}.
     */
    public static List<PhoneOutputSpec> specsFromConfig(final JSONArray config) {
        if (config == null || config.length() == 0) return defaultSpecs();
        final List<PhoneOutputSpec> specs = new ArrayList<>();
        for (int i = 0; i < config.length(); i++) {
            final Object entry = config.opt(i);
            if (!(entry instanceof JSONObject)) continue;
            final JSONObject item = (JSONObject) entry;
            final String name = item.optString("name", "");
            final String kind = item.optString("kind", "");
            if (!CapSchema.cap_is_valid_name(name)) {
                Log.w(TAG, "phone_outputs[" + i + "]: bad name '" + name + "' - skipped");
                continue;
            }
            if (!kind.equals(PhoneOutputSpec.KIND_SCREEN)
                    && !kind.equals(PhoneOutputSpec.KIND_TORCH)
                    && !kind.equals(PhoneOutputSpec.KIND_VIBRATE)) {
                Log.w(TAG, "phone_outputs[" + i + "]: bad kind '" + kind + "' - skipped");
                continue;
            }
            final PhoneOutputSpec spec = new PhoneOutputSpec(name, kind);
            spec.pulseMs = item.optInt("pulse_ms", spec.pulseMs);
            spec.events = item.optBoolean("events", spec.events);
            spec.strobeHz = item.optDouble("strobe_hz", spec.strobeHz);
            spec.color = _parse_color(item.optString("color", null), spec.color);
            specs.add(spec);
        }
        return specs.isEmpty() ? defaultSpecs() : specs;
    }

    private static PhoneOutputBank _build(final Context context) {
        // pulse restores run on a daemon scheduler; drivers marshal to the
        // main looper themselves where the device API needs it
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                runnable -> {
                    final Thread thread = new Thread(runnable, "phone-pulse");
                    thread.setDaemon(true);
                    return thread;
                });
        final PhoneOutput.Scheduler scheduler =
                (delay_ms, task) -> executor.schedule(task, delay_ms, TimeUnit.MILLISECONDS);
        return new PhoneOutputBank(defaultSpecs(),
                spec -> _driver_for(context, spec), scheduler);
    }

    private static PhoneOutput.Driver _driver_for(final Context context, final PhoneOutputSpec spec) {
        switch (spec.kind) {
            case PhoneOutputSpec.KIND_SCREEN:
                return new ScreenStrobeDriver(context, spec.strobeHz, spec.color);
            case PhoneOutputSpec.KIND_TORCH:
                return new TorchDriver(context, spec.strobeHz);
            case PhoneOutputSpec.KIND_VIBRATE:
                return new VibrateDriver(context);
            default:
                return on -> "no driver for kind '" + spec.kind + "'";
        }
    }

    private static int _parse_color(final String text, final int fallback) {
        if (text == null || text.isEmpty()) return fallback;
        try {
            return Color.parseColor(text.startsWith("#") ? text : "#" + text) & 0xFFFFFF;
        } catch (Exception e) {
            return fallback;
        }
    }
}
