package ap.andruav_ap.denative.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** AN-0107: the gpio / snd module wire surface against the rpi_gpio and sound-module formats. */
public class PhoneBusModulesTest {

    private static final String GCS = "gcs_session_1";

    private final Map<String, Boolean> mOutputs = new HashMap<>();
    private final Map<String, Double> mRates = new HashMap<>();
    private final Map<String, Integer> mDuties = new HashMap<>();
    private final List<String> mWriteSrc = new ArrayList<>();
    private final List<Object[]> mSent = new ArrayList<>();
    private PhoneGpioModule mGpio;

    @Before
    public void setUp() throws Exception {
        mOutputs.put("torch", false);
        mOutputs.put("torch_strobe", false);
        mOutputs.put("screen", false);
        mOutputs.put("vibrate", false);
        mGpio = new PhoneGpioModule(new PhoneGpioModule.Outputs() {
            @Override
            public Boolean read(String key) {
                return mOutputs.get(key);
            }

            @Override
            public String write(String key, boolean on, String src) {
                mOutputs.put(key, on);
                mWriteSrc.add(src);
                return null;
            }

            @Override
            public String writeRate(String key, boolean on, double hz, int duty, String src) {
                mOutputs.put(key, on);
                // clamp like PhoneOutput.writePwm does (12.5 Hz max)
                mRates.put(key, Math.min(Math.max(hz, 0), 12.5));
                mDuties.put(key, Math.max(0, Math.min(duty, 1024)));
                return null;
            }

            @Override
            public Double rate(String key) {
                return mRates.get(key);
            }

            @Override
            public Integer duty(String key) {
                return mDuties.get(key);
            }
        }, (target, ms, type) -> mSent.add(new Object[]{target, ms, type}));
    }

    private static JSONObject frame(int type, JSONObject ms) throws Exception {
        return new JSONObject().put("mt", type).put("sd", GCS).put("ms", ms);
    }

    private JSONArray lastStatusList(String expectTarget) throws Exception {
        final Object[] last = mSent.get(mSent.size() - 1);
        assertEquals(expectTarget, last[0]);
        assertEquals(PhoneGpioModule.TYPE_GPIO_STATUS, last[2]);
        final JSONObject ms = (JSONObject) last[1];
        assertEquals(1, ms.getInt("a")); // GPIO_ACTION_INFO
        return ms.getJSONArray("s");
    }

    /** webclient flash button: 6519 {i, a:2, p, v} on the camera_flash pin. */
    @Test
    public void writeDrivesTorchAndBroadcastsStatus() throws Exception {
        mGpio.onMessage(frame(PhoneGpioModule.TYPE_GPIO_ACTION, new JSONObject()
                .put("i", PhoneGpioModule.MODULE_KEY).put("a", 2).put("p", 1).put("v", 1)));

        assertTrue(mOutputs.get("torch"));
        assertEquals(Arrays.asList(PhoneGpioModule.SRC_GCS), mWriteSrc);
        // de_rpi_gpio broadcasts the post-write status to all GCS, not only the requester
        final JSONArray list = lastStatusList("");
        assertEquals(1, list.length());
        final JSONObject pin = list.getJSONObject(0);
        assertEquals(PhoneGpioModule.MODULE_KEY, pin.getString("i"));
        assertEquals(1, pin.getInt("p"));
        assertEquals(1, pin.getInt("b"));
        assertEquals(1, pin.getInt("m")); // OUTPUT
        assertEquals(1, pin.getInt("v"));
        assertEquals(PhoneGpioModule.PIN_CAMERA_FLASH, pin.getString("n"));

        mGpio.onMessage(frame(PhoneGpioModule.TYPE_GPIO_ACTION, new JSONObject()
                .put("i", PhoneGpioModule.MODULE_KEY).put("a", 2).put("p", 1).put("v", 0)));

        assertFalse(mOutputs.get("torch"));
        assertEquals(PhoneGpioModule.PIN_CAMERA_FLASH,
                lastStatusList("").getJSONObject(0).getString("n"));
        assertEquals(0, lastStatusList("").getJSONObject(0).getInt("v"));
    }

    /** AN-0120: the 1051 camera-flash command goes through the one torch
     * choke point - same write, same exclusivity, same 6520 status. */
    @Test
    public void flashCommandDrivesTorchAndBroadcastsStatus() throws Exception {
        mGpio.writeFlash(true);

        assertTrue(mOutputs.get("torch"));
        final JSONArray list = lastStatusList("");
        final JSONObject pin = list.getJSONObject(0);
        assertEquals(1, pin.getInt("p"));
        assertEquals(1, pin.getInt("v"));
        assertEquals(PhoneGpioModule.PIN_CAMERA_FLASH, pin.getString("n"));

        mGpio.writeFlash(false);
        assertFalse(mOutputs.get("torch"));
        assertEquals(0, lastStatusList("").getJSONObject(0).getInt("v"));
    }

    /** AN-0120: toolbar flash on while flash_strobe is on stops the strobe. */
    @Test
    public void flashCommandStopsStrobe() throws Exception {
        mOutputs.put("torch_strobe", true);

        mGpio.writeFlash(true);

        assertFalse(mOutputs.get("torch_strobe"));
        assertTrue(mOutputs.get("torch"));
        // the exclusive free + the write both happened; status carries all pins
        final JSONArray list = lastStatusList("");
        assertTrue(list.length() > 1);
    }

    /** AN-0119: flash_strobe reports as a PWM pin (m:2) with v=Hz, d=duty. */
    @Test
    public void strobeReportsPwmWithRateAndDuty() throws Exception {
        mGpio.onMessage(frame(PhoneGpioModule.TYPE_GPIO_ACTION, new JSONObject()
                .put("a", 2).put("n", PhoneGpioModule.PIN_FLASH_STROBE)
                .put("v", 2).put("d", 512)));

        assertTrue(mOutputs.get("torch_strobe"));
        assertEquals(2.0, mRates.get("torch_strobe"), 0.001);
        assertEquals(512, (int) mDuties.get("torch_strobe"));

        JSONObject strobe = null;
        final JSONArray list = lastStatusList("");
        for (int i = 0; i < list.length(); i++) {
            if (PhoneGpioModule.PIN_FLASH_STROBE.equals(list.getJSONObject(i).getString("n"))) {
                strobe = list.getJSONObject(i);
            }
        }
        assertTrue(strobe != null);
        assertEquals(2, strobe.getInt("m")); // PWM_OUTPUT
        assertEquals(2, strobe.getInt("v")); // Hz
        assertEquals(512, strobe.getInt("d")); // duty

        // v:0 stops the blink
        mGpio.onMessage(frame(PhoneGpioModule.TYPE_GPIO_ACTION, new JSONObject()
                .put("a", 2).put("n", PhoneGpioModule.PIN_FLASH_STROBE)
                .put("v", 0).put("d", 512)));
        assertFalse(mOutputs.get("torch_strobe"));
    }

    /** AN-0119: a rate above the 12.5 Hz limit is clamped and reported clamped. */
    @Test
    public void strobeRateAboveLimitIsClamped() throws Exception {
        mGpio.onMessage(frame(PhoneGpioModule.TYPE_GPIO_ACTION, new JSONObject()
                .put("a", 2).put("n", PhoneGpioModule.PIN_FLASH_STROBE)
                .put("v", 50).put("d", 256)));

        assertEquals(12.5, mRates.get("torch_strobe"), 0.001);
        final JSONArray list = lastStatusList("");
        for (int i = 0; i < list.length(); i++) {
            final JSONObject pin = list.getJSONObject(i);
            if (PhoneGpioModule.PIN_FLASH_STROBE.equals(pin.getString("n"))) {
                assertEquals(12, pin.getInt("v"));
            }
        }
    }

    /** AN-0119: a PWM write without 'd' is ignored, like rpi_gpio. */
    @Test
    public void strobeWriteWithoutDutyIsIgnored() throws Exception {
        mGpio.onMessage(frame(PhoneGpioModule.TYPE_GPIO_ACTION, new JSONObject()
                .put("a", 2).put("n", PhoneGpioModule.PIN_FLASH_STROBE).put("v", 4)));

        assertFalse(mOutputs.get("torch_strobe"));
        assertTrue(mRates.get("torch_strobe") == null);
    }

    /** AN-0119: camera_flash and flash_strobe stay exclusive across the PWM path. */
    @Test
    public void strobeAndFlashStayExclusive() throws Exception {
        mOutputs.put("torch", true);

        mGpio.onMessage(frame(PhoneGpioModule.TYPE_GPIO_ACTION, new JSONObject()
                .put("a", 2).put("n", PhoneGpioModule.PIN_FLASH_STROBE)
                .put("v", 3).put("d", 512)));

        assertFalse(mOutputs.get("torch"));
        assertTrue(mOutputs.get("torch_strobe"));

        // and back: camera_flash on frees the strobe
        mGpio.writeFlash(true);
        assertFalse(mOutputs.get("torch_strobe"));
        assertTrue(mOutputs.get("torch"));
    }

    @Test
    public void writeByNameAndForeignModuleKey() throws Exception {
        mGpio.onMessage(frame(PhoneGpioModule.TYPE_GPIO_ACTION, new JSONObject()
                .put("a", 2).put("n", "vibrate").put("v", 1)));
        assertTrue(mOutputs.get("vibrate"));

        final int sent = mSent.size();
        mGpio.onMessage(frame(PhoneGpioModule.TYPE_GPIO_ACTION, new JSONObject()
                .put("i", "rpi_gpio_key").put("a", 2).put("p", 2).put("v", 1)));
        assertFalse(mOutputs.get("screen"));
        assertEquals(sent, mSent.size());
    }

    /** de_rpi_gpio matches 'n' first; 'p' is a fallback only when 'n' is absent. */
    @Test
    public void pinLookupPrefersNameOverNumber() throws Exception {
        // both present: the name wins, so pin 1 (camera_flash) must stay off
        mGpio.onMessage(frame(PhoneGpioModule.TYPE_GPIO_ACTION, new JSONObject()
                .put("a", 2).put("n", "vibrate").put("p", 1).put("v", 1)));
        assertTrue(mOutputs.get("vibrate"));
        assertFalse(mOutputs.get("torch"));

        // an unknown name does not fall back to the number
        mGpio.onMessage(frame(PhoneGpioModule.TYPE_GPIO_ACTION, new JSONObject()
                .put("a", 2).put("n", "no_such_pin").put("p", 1).put("v", 1)));
        assertFalse(mOutputs.get("torch"));
    }

    /** 6521 {a:6520} returns the whole pin list. */
    @Test
    public void statusRequestReturnsAllPins() throws Exception {
        mGpio.onMessage(frame(PhoneGpioModule.TYPE_GPIO_REMOTE_EXECUTE,
                new JSONObject().put("a", PhoneGpioModule.TYPE_GPIO_STATUS)));
        final JSONArray list = lastStatusList(GCS);
        assertEquals(4, list.length());
        assertEquals("camera_flash", list.getJSONObject(0).getString("n"));
        assertEquals("screen_flash", list.getJSONObject(1).getString("n"));
        assertEquals("vibrate", list.getJSONObject(2).getString("n"));
        assertEquals("flash_strobe", list.getJSONObject(3).getString("n"));
    }

    /** Steady and strobe share one flash unit: turning one on turns the other off. */
    @Test
    public void steadyAndStrobeAreExclusive() throws Exception {
        mGpio.onMessage(frame(PhoneGpioModule.TYPE_GPIO_ACTION, new JSONObject()
                .put("a", 2).put("n", "flash_strobe").put("v", 4).put("d", 512)));
        assertTrue(mOutputs.get("torch_strobe"));

        mGpio.onMessage(frame(PhoneGpioModule.TYPE_GPIO_ACTION, new JSONObject()
                .put("a", 2).put("p", 1).put("v", 1)));
        assertTrue(mOutputs.get("torch"));
        assertFalse(mOutputs.get("torch_strobe"));
        // both pins changed, so the broadcast carries the whole list
        final JSONArray list = lastStatusList("");
        assertEquals(4, list.length());
        assertEquals(1, list.getJSONObject(0).getInt("v"));
        assertEquals(0, list.getJSONObject(3).getInt("v"));
    }

    /** A change made outside the bus (Emergency, capability action) is broadcast once. */
    @Test
    public void pollBroadcastsOutsideChanges() throws Exception {
        mGpio.announce();
        final int sent = mSent.size();
        mGpio.pollChanges();
        assertEquals(sent, mSent.size());

        mOutputs.put("screen", true);
        mGpio.pollChanges();
        assertEquals(sent + 1, mSent.size());
        assertEquals(1, lastStatusList("").getJSONObject(1).getInt("v"));
        mGpio.pollChanges();
        assertEquals(sent + 1, mSent.size());
    }

    @Test
    public void soundPlaysFileAndAnswersListRequest() throws Exception {
        final List<String> played = new ArrayList<>();
        final List<String> said = new ArrayList<>();
        final PhoneSoundModule sound = new PhoneSoundModule(new PhoneSoundModule.Sound() {
            @Override
            public void say(String text, String language, int pitch, int volume, String src) {
                said.add(text + "|" + language + "|" + pitch + "|" + volume + "|" + src);
            }

            @Override
            public void playFile(String file, String src) {
                played.add(file + "|" + src);
            }

            @Override
            public List<PhoneSoundModule.Clip> library() {
                return Arrays.asList(new PhoneSoundModule.Clip("Siren", "sounds/siren.mp3"));
            }
        }, (target, ms, type) -> mSent.add(new Object[]{target, ms, type}));

        sound.onMessage(frame(PhoneSoundModule.TYPE_SOUND_TEXT_TO_SPEECH,
                new JSONObject().put("t", "hello").put("l", "en").put("p", 40).put("v", 80)));
        sound.onMessage(frame(PhoneSoundModule.TYPE_SOUND_TEXT_TO_SPEECH,
                new JSONObject().put("t", "plain")));
        assertEquals(Arrays.asList("hello|en|40|80|gcs", "plain|null|-1|-1|gcs"), said);

        sound.onMessage(frame(PhoneSoundModule.TYPE_SOUND_PLAY_FILE,
                new JSONObject().put("f", "sounds/siren.mp3")));
        assertEquals(Arrays.asList("sounds/siren.mp3|gcs"), played);

        sound.onMessage(frame(PhoneSoundModule.TYPE_REMOTE_EXECUTE,
                new JSONObject().put("C", PhoneSoundModule.TYPE_SOUND_LIST)));
        final Object[] last = mSent.get(mSent.size() - 1);
        assertEquals(GCS, last[0]);
        assertEquals(PhoneSoundModule.TYPE_SOUND_LIST, last[2]);
        final JSONObject ms = (JSONObject) last[1];
        assertTrue(ms.getBoolean("R"));
        assertEquals("Siren", ms.getJSONArray("T").getJSONObject(0).getString("n"));
        assertEquals("sounds/siren.mp3", ms.getJSONArray("T").getJSONObject(0).getString("f"));
    }
}
