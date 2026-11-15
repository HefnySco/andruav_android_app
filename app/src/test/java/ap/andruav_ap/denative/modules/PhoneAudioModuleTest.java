package ap.andruav_ap.denative.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import de.databus.CapSchema;
import de.databus.caps.DeCapabilityProvider;

/** The voice namespaces: de.cap/1 shape, the RECORD_AUDIO gate, speaker mute. */
public class PhoneAudioModuleTest {

    private static final class FakeSpeaker implements PhoneAudioModule.Speaker {
        boolean muted;

        @Override
        public void setMuted(final boolean m) {
            muted = m;
        }

        @Override
        public boolean muted() {
            return muted;
        }
    }

    private static PhoneAudioModule module(final boolean micUsable,
                                           final PhoneAudioModule.Speaker speaker) {
        return new PhoneAudioModule(() -> micUsable, speaker, "1.0");
    }

    @Test
    public void advertsValidateAgainstCapSchema() throws Exception {
        assertEquals("", CapSchema.cap_validate_advert(PhoneAudioModule.microphoneAdvert("1.0")));
        assertEquals("", CapSchema.cap_validate_advert(PhoneAudioModule.speakerAdvert("1.0")));
        final JSONObject mic = PhoneAudioModule.microphoneAdvert("1.0");
        assertEquals(PhoneAudioModule.NS_MICROPHONE, mic.getString("ns"));
        assertTrue(mic.getJSONObject("state").has("active"));
        assertTrue(mic.getJSONObject("events").has("active_changed"));
        final JSONObject spk = PhoneAudioModule.speakerAdvert("1.0");
        assertEquals(PhoneAudioModule.NS_SPEAKER, spk.getString("ns"));
        assertTrue(spk.getJSONObject("actions").has("mute"));
        assertTrue(spk.getJSONObject("actions").has("unmute"));
        assertTrue(spk.getJSONObject("events").has("muted_changed"));
    }

    @Test
    public void micAdvertGatesOnUsableSpeakerAlwaysAdvertised() {
        final List<Object> on = module(true, new FakeSpeaker()).adverts();
        assertEquals(2, on.size());

        // speaker stays advertised with no usable mic - the two are independent
        final List<Object> off = module(false, new FakeSpeaker()).adverts();
        assertEquals(1, off.size());
        assertTrue(((JSONObject) off.get(0)).optString("ns")
                .equals(PhoneAudioModule.NS_SPEAKER));
    }

    @Test
    public void speakerMuteInvokeReachesTheSpeaker() throws Exception {
        final FakeSpeaker speaker = new FakeSpeaker();
        final PhoneAudioModule module = module(true, speaker);
        final JSONObject[] result = new JSONObject[1];
        final String[] err = new String[1];
        final DeCapabilityProvider.ResultSink sink = new DeCapabilityProvider.ResultSink() {
            @Override
            public void ok(final JSONObject data) {
                result[0] = data;
            }

            @Override
            public void err(final String e) {
                err[0] = e;
            }
        };

        module.invoke("i1", PhoneAudioModule.NS_SPEAKER, "mute", null, 0, sink);
        assertTrue(speaker.muted);
        assertTrue(result[0].getBoolean("muted"));
        assertTrue(module.state(PhoneAudioModule.NS_SPEAKER).getBoolean("muted"));

        module.invoke("i2", PhoneAudioModule.NS_SPEAKER, "unmute", null, 0, sink);
        assertFalse(speaker.muted);
        assertFalse(result[0].getBoolean("muted"));

        result[0] = null;
        module.invoke("i3", PhoneAudioModule.NS_SPEAKER, "bogus", null, 0, sink);
        assertEquals("unknown action speaker.bogus", err[0]);
    }
}
