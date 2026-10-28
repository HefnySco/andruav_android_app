package ap.andruav_ap.communication;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitMe;
import com.andruav.protocol.commands.Andruav_Parser;
import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.protocol.commands.binaryMessages.AndruavBinaryHelper;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import de.databus.CModule;
import de.databus.CUDPClient;
import de.databus.Messages;

/**
 * F1 (C4a, AN-0068): {@link AndruavDeBusClient} over a fake {@link CModule}.
 * <ul>
 *   <li>an inbound bus datagram (the raw frame de_comm forwards to a
 *   subscribed module) deserializes through
 *   {@link Andruav_Parser#parseText} unchanged - the {@code sd} party id and
 *   the {@code ts} timestamp key verified per the C1 risk note;</li>
 *   <li>outbound {@code socketSend*} calls reach the module as
 *   {@code sendJMSG/sendBMSG} with the target/mt/ms carried over
 *   unchanged.</li>
 * </ul>
 */
public class AndruavDeBusClientTest {

    private static final String OWN_PARTY_ID = "UNIT_PARTY_1";
    private static final String OWN_GROUP = "group1";

    /*** Records what the client hands to the module send helpers. */
    private static class Sent {
        String target;
        JSONObject jmsg;
        int messageType;
        boolean internal;
        byte[] bmsg;
        int bmsgLength;
        Object messageCmd;
        boolean sys;
    }

    private static class FakeModule extends CModule {
        final List<Sent> sent = new ArrayList<>();

        FakeModule() {
            // sendJMSG/sendBMSG are overridden below so the socket is never
            // touched; the field only has to be non-null for the client's
            // "transport up" check.
            this.cUDPClient = new CUDPClient();
        }

        @Override
        public void sendJMSG(String targetPartyID, JSONObject jmsg, int andruav_message_id,
                             boolean internal_message) {
            Sent s = new Sent();
            s.target = targetPartyID;
            s.jmsg = jmsg;
            s.messageType = andruav_message_id;
            s.internal = internal_message;
            sent.add(s);
        }

        @Override
        public void sendBMSG(String targetPartyID, byte[] bmsg, int bmsg_length,
                             int andruav_message_id, boolean internal_message, Object message_cmd) {
            Sent s = new Sent();
            s.target = targetPartyID;
            s.bmsg = bmsg;
            s.bmsgLength = bmsg_length;
            s.messageType = andruav_message_id;
            s.internal = internal_message;
            s.messageCmd = message_cmd;
            sent.add(s);
        }

        @Override
        public void send_sys_msg(JSONObject jmsg, int andruav_message_id) {
            Sent s = new Sent();
            s.jmsg = jmsg;
            s.messageType = andruav_message_id;
            s.sys = true;
            sent.add(s);
        }
    }

    /*** Captures the parsed messages instead of delegating to MessageDispatcher. */
    private static class CapturingClient extends AndruavDeBusClient {
        Andruav_2MR lastTextMessage;

        CapturingClient(final CModule module) {
            // direct executor: dispatchBusMessage runs inline, exactly what
            // the WS handler post would run.
            super(module, Runnable::run);
        }

        @Override
        protected void onTextMessage(final Andruav_2MR andruav2MR) {
            lastTextMessage = andruav2MR;
        }
    }

    /***
     * In the unit-test classpath org.json is the strict JVM jar whose
     * {@code getString} refuses numeric scalars, while Android's
     * JSONObject.getString coerces them - so {@code parseText} on this JVM
     * only survives frames whose {@code mt}/{@code ts} are strings. The wire
     * KEYS are identical either way; this helper writes them as strings so
     * the full parser path can be exercised here.
     */
    private static JSONObject scalarFrame(final String routing, final String sender,
                                          final String target, final int messageType,
                                          final JSONObject ms) throws Exception {
        return new JSONObject()
                .put(ProtocolHeaders.MSG_ROUTING, routing)
                .put(ProtocolHeaders.Sender, sender)
                .put(ProtocolHeaders.Group, "gcs_group")
                .put(ProtocolHeaders.MessageType, String.valueOf(messageType))
                .put(ProtocolHeaders.Target, target)
                .put(ProtocolHeaders.TimeStamp, "1730000000000")
                .put(ProtocolHeaders.Message, ms);
    }

    private CapturingClient newClient(final FakeModule module) {
        final CapturingClient client = new CapturingClient(module);
        client.setUnitIdentity(OWN_PARTY_ID, OWN_GROUP);
        return client;
    }


    /***
     * A server-sourced frame exactly as de_comm forwards it to a subscribed
     * module ({@code ty/sd/gr/mt/tg/ts/ms}) must come back out of
     * {@link Andruav_Parser#parseText} with the {@code sd} party id and the
     * {@code ts} timestamp intact.
     */
    @Test
    public void inboundServerFrameParsesSdAndTimestamp() throws Exception {
        final FakeModule module = new FakeModule();
        final CapturingClient client = newClient(module);

        final JSONObject frame = scalarFrame(
                ProtocolHeaders.CMD_COMM_INDIVIDUAL, "GCS_UNIT_1", OWN_PARTY_ID,
                Messages.TYPE_AndruavMessage_Signaling,
                new JSONObject().put("w", new JSONObject().put("channel", "cam0")));
        final byte[] datagram = frame.toString().getBytes(StandardCharsets.UTF_8);

        // same shape the CModule receive callback delivers
        client.onReceive(datagram, datagram.length,
                new JSONObject(new String(datagram, StandardCharsets.UTF_8)));

        assertNotNull(client.lastTextMessage);
        assertEquals("GCS_UNIT_1", client.lastTextMessage.partyID);
        assertEquals("1730000000000", client.lastTextMessage.timeStamp);
        assertEquals("gcs_group", client.lastTextMessage.groupName);
        assertEquals(OWN_PARTY_ID, client.lastTextMessage.targetName);
        assertTrue(client.lastTextMessage.IsReceived);

        // and the raw datagram itself parses unchanged through parseText
        final Andruav_2MR parsed = Andruav_Parser.parseText(
                new String(datagram, StandardCharsets.UTF_8));
        assertNotNull(parsed);
        assertEquals("GCS_UNIT_1", parsed.partyID);
        assertEquals("1730000000000", parsed.timeStamp);
    }


    /***
     * A module-sourced comm frame ({@code GU/tg/ty/mt/ms} - no {@code sd})
     * is stamped with the unit's own identity so the unit map keys it like
     * any remote unit.
     */
    @Test
    public void inboundModuleFrameGetsUnitSenderStamped() throws Exception {
        final FakeModule module = new FakeModule();
        final CapturingClient client = newClient(module);

        final JSONObject frame = new JSONObject()
                .put(Messages.INTERMODULE_MODULE_KEY, "de_mavlink")
                .put(Messages.ANDRUAV_PROTOCOL_TARGET_ID, "")
                .put(Messages.INTERMODULE_ROUTING_TYPE, Messages.CMD_COMM_GROUP)
                .put(Messages.ANDRUAV_PROTOCOL_MESSAGE_TYPE,
                        String.valueOf(Messages.TYPE_AndruavMessage_Signaling))
                .put(Messages.ANDRUAV_PROTOCOL_MESSAGE_CMD, new JSONObject()
                        .put("w", new JSONObject().put("channel", "cam0")));
        final byte[] datagram = frame.toString().getBytes(StandardCharsets.UTF_8);

        client.onReceive(datagram, datagram.length,
                new JSONObject(new String(datagram, StandardCharsets.UTF_8)));

        assertNotNull(client.lastTextMessage);
        assertEquals(OWN_PARTY_ID, client.lastTextMessage.partyID);
        assertEquals(OWN_GROUP, client.lastTextMessage.groupName);
    }


    /*** A de_comm 9100 module-ID reply, optionally carrying the ms.f party record. */
    private static void receiveModuleIdReply(final CapturingClient client, final JSONObject partyRecord)
            throws Exception {
        final JSONObject ms = new JSONObject().put(Messages.JSON_INTERMODULE_SOCKET_STATUS, -1);
        if (partyRecord != null) ms.put(Messages.JSON_INTERMODULE_PARTY_RECORD, partyRecord);
        final JSONObject frame = new JSONObject()
                .put(Messages.INTERMODULE_MODULE_KEY, "de_comm")
                .put(Messages.INTERMODULE_ROUTING_TYPE, Messages.CMD_TYPE_INTERMODULE)
                .put(Messages.ANDRUAV_PROTOCOL_MESSAGE_TYPE, Messages.TYPE_AndruavModule_ID)
                .put(Messages.ANDRUAV_PROTOCOL_MESSAGE_CMD, ms);
        final byte[] datagram = frame.toString().getBytes(StandardCharsets.UTF_8);
        client.onReceive(datagram, datagram.length,
                new JSONObject(new String(datagram, StandardCharsets.UTF_8)));
    }

    private static void receiveSiblingFrame(final CapturingClient client) throws Exception {
        final JSONObject frame = new JSONObject()
                .put(Messages.INTERMODULE_MODULE_KEY, "de_mavlink")
                .put(Messages.ANDRUAV_PROTOCOL_TARGET_ID, "")
                .put(Messages.INTERMODULE_ROUTING_TYPE, Messages.CMD_COMM_GROUP)
                .put(Messages.ANDRUAV_PROTOCOL_MESSAGE_TYPE,
                        String.valueOf(Messages.TYPE_AndruavMessage_Signaling))
                .put(Messages.ANDRUAV_PROTOCOL_MESSAGE_CMD, new JSONObject()
                        .put("w", new JSONObject().put("channel", "cam0")));
        final byte[] datagram = frame.toString().getBytes(StandardCharsets.UTF_8);
        client.onReceive(datagram, datagram.length,
                new JSONObject(new String(datagram, StandardCharsets.UTF_8)));
    }


    /***
     * AN-0106: de_comm's 9100 reports the unit's party id/group in ms.f;
     * later sibling frames are stamped with it and the me-unit takes it.
     */
    @Test
    public void moduleIdReplyPartyRecordBecomesUnitIdentity() throws Exception {
        final AndruavUnitMe savedMe = AndruavSettings.andruavWe7daBase;
        final AndruavUnitMe me = new AndruavUnitMe();
        me.PartyID = OWN_PARTY_ID;
        me.GroupName = OWN_GROUP;
        AndruavSettings.andruavWe7daBase = me;
        try {
            final CapturingClient client = newClient(new FakeModule());

            receiveModuleIdReply(client, new JSONObject()
                    .put(ProtocolHeaders.Sender, "phone1")
                    .put(ProtocolHeaders.Group, "simgroup"));
            receiveSiblingFrame(client);

            assertNotNull(client.lastTextMessage);
            assertEquals("phone1", client.lastTextMessage.partyID);
            assertEquals("simgroup", client.lastTextMessage.groupName);
            assertEquals("phone1", me.PartyID);
            assertEquals("simgroup", me.GroupName);
        } finally {
            AndruavSettings.andruavWe7daBase = savedMe;
        }
    }


    /*** AN-0106: a 9100 without a party record leaves the identity alone. */
    @Test
    public void moduleIdReplyWithoutPartyRecordKeepsIdentity() throws Exception {
        final AndruavUnitMe savedMe = AndruavSettings.andruavWe7daBase;
        final AndruavUnitMe me = new AndruavUnitMe();
        me.PartyID = OWN_PARTY_ID;
        me.GroupName = OWN_GROUP;
        AndruavSettings.andruavWe7daBase = me;
        try {
            final CapturingClient client = newClient(new FakeModule());

            receiveModuleIdReply(client, null);
            receiveSiblingFrame(client);

            assertNotNull(client.lastTextMessage);
            assertEquals(OWN_PARTY_ID, client.lastTextMessage.partyID);
            assertEquals(OWN_PARTY_ID, me.PartyID);
            assertEquals(OWN_GROUP, me.GroupName);
        } finally {
            AndruavSettings.andruavWe7daBase = savedMe;
        }
    }


    /*** Intermodule ("uv") traffic is not fed to the Andruav parser. */
    @Test
    public void intermoduleFramesAreNotParsedAsAndruav() throws Exception {
        final FakeModule module = new FakeModule();
        final CapturingClient client = newClient(module);

        final JSONObject frame = new JSONObject()
                .put(Messages.INTERMODULE_MODULE_KEY, "de_comm")
                .put(Messages.INTERMODULE_ROUTING_TYPE, Messages.CMD_TYPE_INTERMODULE)
                .put(Messages.ANDRUAV_PROTOCOL_MESSAGE_TYPE, Messages.TYPE_AndruavMessage_MODULE_STATE)
                .put(Messages.ANDRUAV_PROTOCOL_MESSAGE_CMD, new JSONObject().put("ns", "phone"));
        final byte[] datagram = frame.toString().getBytes(StandardCharsets.UTF_8);

        client.onReceive(datagram, datagram.length,
                new JSONObject(new String(datagram, StandardCharsets.UTF_8)));

        assertNull(client.lastTextMessage);
    }


    /***
     * Outbound: the WS frame produced by {@code sendMessageToIndividual}
     * reaches {@link CModule#sendJMSG} as (target=tg, jmsg=ms, mt, not
     * intermodule).
     */
    @Test
    public void outboundTextFrameReachesSendJMSG() throws Exception {
        final FakeModule module = new FakeModule();
        final AndruavDeBusClient client = newClient(module);

        final JSONObject ms = new JSONObject().put("a", true).put("b", 30);
        final JSONObject frame = new JSONObject()
                .put(ProtocolHeaders.MSG_ROUTING, ProtocolHeaders.CMD_COMM_INDIVIDUAL)
                .put(ProtocolHeaders.Sender, OWN_PARTY_ID)
                .put(ProtocolHeaders.Group, OWN_GROUP)
                .put(ProtocolHeaders.MessageType, Messages.TYPE_AndruavMessage_Arm)
                .put(ProtocolHeaders.Target, "GCS_UNIT_1")
                .put(ProtocolHeaders.Message, ms);

        client.socketSendTextMessage(frame.toString());

        assertEquals(1, module.sent.size());
        final Sent s = module.sent.get(0);
        assertFalse(s.sys);
        assertEquals("GCS_UNIT_1", s.target);
        assertEquals(Messages.TYPE_AndruavMessage_Arm, s.messageType);
        assertFalse(s.internal);
        assertTrue(s.jmsg.getBoolean("a"));
        assertEquals(30, s.jmsg.getInt("b"));
    }


    /*** A group broadcast frame (no tg) reaches sendJMSG with an empty target. */
    @Test
    public void outboundGroupFrameHasEmptyTarget() throws Exception {
        final FakeModule module = new FakeModule();
        final AndruavDeBusClient client = newClient(module);

        final JSONObject frame = new JSONObject()
                .put(ProtocolHeaders.MSG_ROUTING, ProtocolHeaders.CMD_COMM_GROUP)
                .put(ProtocolHeaders.Sender, OWN_PARTY_ID)
                .put(ProtocolHeaders.MessageType, Messages.TYPE_AndruavMessage_HomeLocation)
                .put(ProtocolHeaders.Message, new JSONObject().put("la", 30.1));

        client.socketSendTextMessage(frame.toString());

        assertEquals(1, module.sent.size());
        assertEquals("", module.sent.get(0).target);
        assertEquals(Messages.TYPE_AndruavMessage_HomeLocation, module.sent.get(0).messageType);
        assertFalse(module.sent.get(0).internal);
    }


    /*** System frames (ping & co) take the module's sys path, not sendJMSG. */
    @Test
    public void outboundSysFrameGoesToSendSysMsg() throws Exception {
        final FakeModule module = new FakeModule();
        final AndruavDeBusClient client = newClient(module);

        final JSONObject frame = new JSONObject()
                .put(ProtocolHeaders.MSG_ROUTING, ProtocolHeaders.CMD_TYPE_SYS)
                .put(ProtocolHeaders.Sender, OWN_PARTY_ID)
                .put(ProtocolHeaders.MessageType, Messages.TYPE_AndruavSystem_Ping)
                .put(ProtocolHeaders.Message, new JSONObject().put("ts", 123L));

        client.socketSendTextMessage(frame.toString());

        assertEquals(1, module.sent.size());
        assertTrue(module.sent.get(0).sys);
        assertEquals(Messages.TYPE_AndruavSystem_Ping, module.sent.get(0).messageType);
    }


    /*** Outbound binary frames reach sendBMSG with the binary body unchanged. */
    @Test
    public void outboundBinaryFrameReachesSendBMSG() throws Exception {
        final FakeModule module = new FakeModule();
        final AndruavDeBusClient client = newClient(module);

        final byte[] binaryBody = new byte[]{0x10, 0x20, 0x30, 0x7F};
        final JSONObject header = new JSONObject()
                .put(ProtocolHeaders.MSG_ROUTING, ProtocolHeaders.CMD_COMM_INDIVIDUAL)
                .put(ProtocolHeaders.Sender, OWN_PARTY_ID)
                .put(ProtocolHeaders.MessageType,
                        com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_LightTelemetry
                                .TYPE_AndruavMessage_LightTelemetry)
                .put(ProtocolHeaders.Target, "GCS_UNIT_1");
        final byte[] payload = AndruavBinaryHelper.joinBinarywithText(header.toString(), binaryBody);

        client.socketSendBinaryMessage(payload);

        assertEquals(1, module.sent.size());
        final Sent s = module.sent.get(0);
        assertEquals("GCS_UNIT_1", s.target);
        assertEquals(2022, s.messageType);
        assertFalse(s.internal);
        assertArrayEquals(binaryBody, s.bmsg);
        assertEquals(binaryBody.length, s.bmsgLength);
        assertNotNull(s.messageCmd);
    }
}
