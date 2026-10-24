package com.andruav.protocol.commands.textMessages;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SoundPlayFileTest {

    /***
     * A 6512 message carries its file name under the "f" key and must decode
     * back to the same value.
     */
    @Test
    public void roundTripKeepsFileName() throws Exception {
        final AndruavMessage_Sound_PlayFile in = new AndruavMessage_Sound_PlayFile();
        in.setMessageText("{\"f\":\"x.mp3\"}");

        assertEquals("x.mp3", in.fileName);

        final String json = in.getJsonMessage();

        final org.json.JSONObject parsed = new org.json.JSONObject(json);
        assertTrue(parsed.has("f"));
        assertEquals("x.mp3", parsed.getString("f"));

        final AndruavMessage_Sound_PlayFile back = new AndruavMessage_Sound_PlayFile();
        back.setMessageText(json);
        assertEquals("x.mp3", back.fileName);
        assertEquals(AndruavMessage_Sound_PlayFile.TYPE_AndruavMessage_Sound_PlayFile, in.messageTypeID);
    }
}
