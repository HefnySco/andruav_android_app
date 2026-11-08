package ap.andruav_ap.denative.modules;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ap.andruav_ap.denative.providers.phone.PhoneOutput;

/**
 * AN-0107: the phone's torch / screen strobe / vibrate as a DroneEngage GPIO
 * module (class "gpio") with the de_rpi_gpio wire surface, so the webclient's
 * GPIO tab and its flash (sun) button - which looks for pins named
 * {@value #PIN_CAMERA_FLASH} (steady torch) - drive a native unit;
 * {@value #PIN_FLASH_STROBE} blinks the same torch.
 * <p>
 * Messages (droneengage_rpi_gpio src/gpio/gpio_facade.cpp, webclient
 * js_commands_api.js):
 * <ul>
 * <li>6519 GPIO_ACTION {i, a:2 PORT_WRITE, n|p, v}: write one pin - 'n' is
 * matched first, 'p' only when 'n' is absent (gpio_parser.cpp); a:1 INFO
 * asks for the pin list.</li>
 * <li>6521 GPIO_REMOTE_EXECUTE {a:6520, p?, i?}: status request.</li>
 * <li>6520 GPIO_STATUS {a:1, s:[{i, p, b, m, t, d, v, n}]}: the reply, also
 * sent whenever a pin changes (from any source).</li>
 * </ul>
 * Writes go through the PhoneOutput choke points, so the bus, Emergency and
 * the capability surface share one state.
 */
public final class PhoneGpioModule {

    public static final String MODULE_ID = "andruav_gpio";
    public static final String MODULE_KEY = "andruav_gpio";

    public static final int TYPE_GPIO_ACTION = 6519;
    public static final int TYPE_GPIO_STATUS = 6520;
    public static final int TYPE_GPIO_REMOTE_EXECUTE = 6521;

    static final int ACTION_INFO = 1;
    static final int ACTION_PORT_WRITE = 2;

    // de_rpi_gpio gpio_driver.hpp: OUTPUT = 1, PWM_OUTPUT = 2, GENERIC = 0
    private static final int PIN_MODE_OUTPUT = 1;
    private static final int PIN_MODE_PWM = 2;
    private static final int GPIO_TYPE_GENERIC = 0;

    public static final String PIN_CAMERA_FLASH = "camera_flash";
    public static final String SRC_GCS = "gcs";

    /** A virtual pin: its number, the name the webclient sees, the PhoneOutput key. */
    static final class Pin {
        final int number;
        final String name;
        final String output;
        /** Output sharing the same hardware (turned off before this one goes on), or null. */
        final String exclusive;
        /** PIN_MODE_OUTPUT or PIN_MODE_PWM (de_rpi_gpio modes). */
        final int mode;

        Pin(final int number, final String name, final String output, final String exclusive,
            final int mode) {
            this.number = number;
            this.name = name;
            this.output = output;
            this.exclusive = exclusive;
            this.mode = mode;
        }
    }

    public static final String PIN_FLASH_STROBE = "flash_strobe";

    static final Pin[] PINS = {
            new Pin(1, PIN_CAMERA_FLASH, "torch", "strobe", PIN_MODE_OUTPUT),   // steady on/off
            new Pin(2, "screen_flash", "screen", null, PIN_MODE_OUTPUT),
            new Pin(3, "vibrate", "vibrate", null, PIN_MODE_OUTPUT),
            new Pin(4, PIN_FLASH_STROBE, "strobe", "torch", PIN_MODE_PWM),      // blinking torch
    };

    /** The phone output bank, narrowed to what the pins need. */
    public interface Outputs {
        /** Current level, or null when the output does not exist on this phone. */
        Boolean read(String key);

        /** Null on success, else an error string. */
        String write(String key, boolean on, String src);

        /** AN-0119: PWM write - hz = frequency, duty = lit share 0..1024. */
        String writeRate(String key, boolean on, double hz, int duty, String src);

        /** Last written rate (Hz) of a PWM output, or null. */
        Double rate(String key);

        /** Last written duty (0..1024) of a PWM output, or null. */
        Integer duty(String key);
    }

    /** Bus send surface (CModule.sendJMSG, not internal). */
    public interface Sender {
        void send(String targetPartyId, JSONObject ms, int messageType);
    }

    private final Outputs m_outputs;
    private final Sender m_sender;
    private final Map<Integer, Integer> m_last_sent = new HashMap<>();

    public PhoneGpioModule(final Outputs outputs, final Sender sender) {
        m_outputs = outputs;
        m_sender = sender;
    }

    /** Message types the module subscribes to. */
    public static JSONArray messageFilter() {
        return new JSONArray().put(TYPE_GPIO_ACTION).put(TYPE_GPIO_REMOTE_EXECUTE);
    }

    /** One inbound databus frame (header + ms). */
    public void onMessage(final JSONObject jMsg) {
        final int type = jMsg.optInt("mt", -1);
        final JSONObject ms = payload(jMsg);
        if (ms == null) return;
        final String sender = jMsg.optString("sd", "");
        // a request naming another module's pins is not ours
        if (ms.has("i") && !MODULE_KEY.equals(ms.optString("i"))) return;

        if (type == TYPE_GPIO_ACTION) {
            final int action = ms.optInt("a", -1);
            if (action == ACTION_PORT_WRITE) {
                final Pin pin = find(ms);
                if (pin == null) return;
                if (pin.mode == PIN_MODE_PWM) {
                    // rpi_gpio: a PWM write carries 'v' (frequency Hz) and
                    // 'd' (duty); a write without 'd' is ignored, v:0 is off
                    if (!ms.has("v") || !ms.has("d")) return;
                    writePwmPin(pin, ms.optDouble("v", 0), ms.optInt("d", 0), SRC_GCS);
                } else {
                    if (!ms.has("v")) return;
                    writePin(pin, ms.optInt("v") != 0, SRC_GCS);
                }
            } else if (action == ACTION_INFO) {
                sendStatus(sender, null);
            }
        } else if (type == TYPE_GPIO_REMOTE_EXECUTE) {
            if (ms.optInt("a", -1) != TYPE_GPIO_STATUS) return;
            sendStatus(sender, ms.has("p") || ms.has("n") ? find(ms) : null);
        }
    }

    /** Full pin list to every GCS (module start). */
    public void announce() {
        sendStatus("", null);
    }

    /**
     * AN-0120: the 1051 camera-flash command writes the camera_flash pin,
     * so the video toolbar and the GPIO buttons share one state (and the
     * strobe exclusivity still applies).
     */
    public void writeFlash(final boolean on) {
        for (Pin pin : PINS) {
            if (pin.name.equals(PIN_CAMERA_FLASH)) {
                writePin(pin, on, PhoneOutput.SRC_LEGACY);
                return;
            }
        }
    }

    private void writePin(final Pin pin, final boolean on, final String src) {
        // one flash unit: steady and strobe cannot both own it
        final boolean freed = on && (pin.exclusive != null)
                && Boolean.TRUE.equals(m_outputs.read(pin.exclusive));
        if (freed) m_outputs.write(pin.exclusive, false, src);
        m_outputs.write(pin.output, on, src);
        // de_rpi_gpio broadcasts the post-write status so every GCS sees the flip
        sendStatus("", freed ? null : pin);
    }

    private void writePwmPin(final Pin pin, final double hz, final int duty, final String src) {
        final boolean on = hz > 0;
        final boolean freed = on && (pin.exclusive != null)
                && Boolean.TRUE.equals(m_outputs.read(pin.exclusive));
        if (freed) m_outputs.write(pin.exclusive, false, src);
        m_outputs.writeRate(pin.output, on, hz, duty, src);
        sendStatus("", freed ? null : pin);
    }

    /** Polled: broadcasts the list when a pin changed outside a bus write (Emergency, actions). */
    public void pollChanges() {
        for (Pin pin : PINS) {
            final Integer last = m_last_sent.get(pin.number);
            final Integer now = pinValue(pin);
            if (now == null) continue;
            if (last == null || !last.equals(now)) {
                sendStatus("", null);
                return;
            }
        }
    }

    private void sendStatus(final String target, final Pin only) {
        try {
            m_sender.send(target, status(only), TYPE_GPIO_STATUS);
        } catch (JSONException ignored) {
            // put() on literal keys/values does not throw
        }
    }

    private JSONObject status(final Pin only) throws JSONException {
        final JSONArray list = new JSONArray();
        for (Pin pin : PINS) {
            if (only != null && only != pin) continue;
            final Integer v = pinValue(pin);
            if (v == null) continue;
            m_last_sent.put(pin.number, v);
            list.put(new JSONObject()
                    .put("i", MODULE_KEY)
                    .put("p", pin.number)
                    .put("b", pin.number)
                    .put("m", pin.mode)
                    .put("t", GPIO_TYPE_GENERIC)
                    .put("d", pinDuty(pin))
                    .put("v", v)
                    .put("n", pin.name));
        }
        return new JSONObject().put("a", ACTION_INFO).put("s", list);
    }

    /** The 'v' a pin reports: level for OUTPUT, blink rate (Hz, floored) for PWM. */
    private Integer pinValue(final Pin pin) {
        final Boolean on = m_outputs.read(pin.output);
        if (on == null) return null;
        if (pin.mode == PIN_MODE_PWM) {
            if (!on) return 0; // v:0 is off - report no rate on a stopped pin
            final Double rate = m_outputs.rate(pin.output);
            return (rate != null) ? (int) Math.floor(rate) : 0;
        }
        return on ? 1 : 0;
    }

    /** The 'd' a pin reports: blink duty for PWM, 0 for OUTPUT. */
    private int pinDuty(final Pin pin) {
        if (pin.mode != PIN_MODE_PWM) return 0;
        final Integer duty = m_outputs.duty(pin.output);
        return (duty != null) ? duty : 0;
    }

    private Pin find(final JSONObject ms) {
        // de_rpi_gpio: 'n' is matched first; 'p' is only a fallback when 'n' is absent
        if (ms.has("n")) {
            final String name = ms.optString("n", null);
            for (Pin pin : PINS) {
                if (pin.name.equals(name)) return pin;
            }
            return null;
        }
        if (!ms.has("p")) return null;
        final int number = ms.optInt("p", -1);
        for (Pin pin : PINS) {
            if (number == pin.number) return pin;
        }
        return null;
    }

    /** ms is an object on the bus; a string form is accepted too. */
    static JSONObject payload(final JSONObject jMsg) {
        final Object ms = jMsg.opt("ms");
        if (ms instanceof JSONObject) return (JSONObject) ms;
        if (ms instanceof String) {
            try {
                return new JSONObject((String) ms);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
