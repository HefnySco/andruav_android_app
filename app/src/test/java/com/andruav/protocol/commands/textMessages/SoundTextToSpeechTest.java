package com.andruav.protocol.commands.textMessages;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SoundTextToSpeechTest {

    /***
     * A 6511 message with all fields must encode to t,l,p,v keys and decode
     * back to the same values.
     */
    @Test
    public void roundTripKeepsAllFields() throws Exception {
        final AndruavMessage_Sound_TextToSpeech out = new AndruavMessage_Sound_TextToSpeech();
        out.text = "hello pilot";
        out.language = "en";
        out.pitch = 90;
        out.volume = 40;

        final String json = out.getJsonMessage();

        final AndruavMessage_Sound_TextToSpeech in = new AndruavMessage_Sound_TextToSpeech();
        in.setMessageText(json);

        assertEquals("hello pilot", in.text);
        assertEquals("en", in.language);
        assertEquals(90, in.pitch);
        assertEquals(40, in.volume);
    }

    /***
     * Regression: volume must be written under "v" — not under "p", which
     * used to clobber pitch.
     */
    @Test
    public void volumeIsEncodedUnderV() throws Exception {
        final AndruavMessage_Sound_TextToSpeech out = new AndruavMessage_Sound_TextToSpeech();
        out.text = "check keys";
        out.pitch = 30;
        out.volume = 70;

        final String json = out.getJsonMessage();

        final org.json.JSONObject parsed = new org.json.JSONObject(json);
        assertTrue(parsed.has("v"));
        assertEquals(70, parsed.getInt("v"));
        assertEquals(30, parsed.getInt("p"));
    }

    /***
     * A 6511 carrying only text must stay valid: no l/p/v keys are emitted
     * and decoding leaves the defaults intact.
     */
    @Test
    public void textOnlyMessageKeepsDefaults() throws Exception {
        final AndruavMessage_Sound_TextToSpeech out = new AndruavMessage_Sound_TextToSpeech();
        out.text = "text only";

        final String json = out.getJsonMessage();

        final org.json.JSONObject parsed = new org.json.JSONObject(json);
        assertEquals("text only", parsed.getString("t"));
        assertFalse(parsed.has("l"));
        assertFalse(parsed.has("p"));
        assertFalse(parsed.has("v"));

        final AndruavMessage_Sound_TextToSpeech in = new AndruavMessage_Sound_TextToSpeech();
        in.setMessageText(json);
        assertEquals("text only", in.text);
        assertEquals(-1, in.pitch);
        assertEquals(-1, in.volume);
    }
}
