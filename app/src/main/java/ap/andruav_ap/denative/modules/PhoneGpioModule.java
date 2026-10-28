package ap.andruav_ap.denative.modules;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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
 * <li>6519 GPIO_ACTION {i, a:2 PORT_WRITE, p|n, v}: write one pin; a:1 INFO
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

    // de_rpi_gpio gpio_driver.hpp: OUTPUT = 1, ENUM_GPIO_TYPE GENERIC = 0
    private static final int PIN_MODE_OUTPUT = 1;
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

        Pin(final int number, final String name, final String output, final String exclusive) {
            this.number = number;
            this.name = name;
            this.output = output;
            this.exclusive = exclusive;
        }
    }

    public static final String PIN_FLASH_STROBE = "flash_strobe";

    static final Pin[] PINS = {
            new Pin(1, PIN_CAMERA_FLASH, "torch", "torch_strobe"),   // steady on/off
            new Pin(2, "screen_flash", "screen", null),
            new Pin(3, "vibrate", "vibrate", null),
            new Pin(4, PIN_FLASH_STROBE, "torch_strobe", "torch"),   // blinking torch
    };

    /** The phone output bank, narrowed to what the pins need. */
    public interface Outputs {
        /** Current level, or null when the output does not exist on this phone. */
        Boolean read(String key);

        /** Null on success, else an error string. */
        String write(String key, boolean on, String src);
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
                if (pin == null || !ms.has("v")) return;
                final boolean on = ms.optInt("v") != 0;
                // one flash unit: steady and strobe cannot both own it
                final boolean freed = on && (pin.exclusive != null)
                        && Boolean.TRUE.equals(m_outputs.read(pin.exclusive));
                if (freed) m_outputs.write(pin.exclusive, false, SRC_GCS);
                m_outputs.write(pin.output, on, SRC_GCS);
                sendStatus(sender, freed ? null : pin);
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

    /** Polled: broadcasts the list when a pin changed outside a bus write (Emergency, actions). */
    public void pollChanges() {
        for (Pin pin : PINS) {
            final Integer last = m_last_sent.get(pin.number);
            final Boolean now = m_outputs.read(pin.output);
            if (now == null) continue;
            if (last == null || last != (now ? 1 : 0)) {
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
            final Boolean on = m_outputs.read(pin.output);
            if (on == null) continue;
            final int v = on ? 1 : 0;
            m_last_sent.put(pin.number, v);
            list.put(new JSONObject()
                    .put("i", MODULE_KEY)
                    .put("p", pin.number)
                    .put("b", pin.number)
                    .put("m", PIN_MODE_OUTPUT)
                    .put("t", GPIO_TYPE_GENERIC)
                    .put("d", 0)
                    .put("v", v)
                    .put("n", pin.name));
        }
        return new JSONObject().put("a", ACTION_INFO).put("s", list);
    }

    private Pin find(final JSONObject ms) {
        for (Pin pin : PINS) {
            if (ms.has("p") && ms.optInt("p", -1) == pin.number) return pin;
            if (!ms.has("p") && pin.name.equals(ms.optString("n", null))) return pin;
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
