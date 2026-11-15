package ap.andruav_ap.denative.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import de.databus.CapSchema;

/** The ns=microphone advert: de.cap/1 shape and the RECORD_AUDIO gate. */
public class PhoneAudioModuleTest {

    @Test
    public void advertValidatesAgainstCapSchema() throws Exception {
        final JSONObject advert = PhoneAudioModule.advert("1.0");
        assertEquals("", CapSchema.cap_validate_advert(advert));
        assertEquals(PhoneAudioModule.NS_MICROPHONE, advert.getString("ns"));
        assertTrue(advert.getJSONObject("state").has("active"));
        assertTrue(advert.getJSONObject("state").has("muted"));
        assertTrue(advert.getJSONObject("events").has("active_changed"));
    }

    @Test
    public void advertsGateOnMicUsable() {
        final PhoneAudioModule on = new PhoneAudioModule(() -> true, "1.0");
        assertEquals(1, on.adverts().size());

        final PhoneAudioModule off = new PhoneAudioModule(() -> false, "1.0");
        assertTrue(off.adverts().isEmpty());
    }
}
