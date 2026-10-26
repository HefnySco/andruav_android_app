package de.databus;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static de.databus.Messages.TYPE_AndruavMessage_Sync_EventFire;

/**
 * Port of de_databus/python/test_de_module.py — fire_event tests with a
 * recording sendJMSG; no bus needed.
 */
class TestCModule {

    private static class Sent {
        final String target; final JSONObject jmsg; final int mid; final boolean internal;
        Sent(String t, JSONObject j, int m, boolean i) { target = t; jmsg = j; mid = m; internal = i; }
    }

    private static class RecordingModule extends CModule {
        final List<Sent> sent = new ArrayList<>();
        @Override
        public void sendJMSG(String targetPartyID, JSONObject jmsg, int andruav_message_id,
                             boolean internal_message) {
            sent.add(new Sent(targetPartyID, jmsg, andruav_message_id, internal_message));
        }
    }

    @Test
    void fire_event_returns_the_nonce_it_sent() {
        RecordingModule module = new RecordingModule();
        JSONObject payload = new JSONObject().put("src", "event").put("loop", false);
        String nonce = module.fire_event("sound", "beep.started", payload);

        assertEquals(1, module.sent.size());
        Sent s = module.sent.get(0);
        assertEquals(TYPE_AndruavMessage_Sync_EventFire, s.mid);
        assertTrue(s.internal);
        assertEquals("sound.beep.started", s.jmsg.getString("d"));
        assertEquals("event", s.jmsg.getJSONObject("ed").getString("src"));
        assertFalse(s.jmsg.getJSONObject("ed").getBoolean("loop"));
        assertNotNull(nonce);
        assertEquals(nonce, s.jmsg.getString("n"));
    }

    @Test
    void fire_event_nonces_are_unique() {
        RecordingModule module = new RecordingModule();
        String first = module.fire_event("sound", "a.finished", new JSONObject());
        String second = module.fire_event("sound", "b.finished", new JSONObject());

        assertNotEquals(first, second);
        assertEquals(first, module.sent.get(0).jmsg.getString("n"));
        assertEquals(second, module.sent.get(1).jmsg.getString("n"));
    }
}
