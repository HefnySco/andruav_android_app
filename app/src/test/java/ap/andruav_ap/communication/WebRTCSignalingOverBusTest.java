package ap.andruav_ap.communication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.event.droneReport_Event.Event_Signalling;
import com.andruav.interfaces.IEventBus;
import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.uavos.modules.UAVOSConstants;
import com.andruav.uavos.modules.UAVOSHelper;
import com.andruav.uavos.modules.UAVOSModuleCamera;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import de.databus.CModule;
import de.databus.CUDPClient;
import de.databus.Messages;

/**
 * F4 (C4d, AN-0071): WebRTC signaling (1021) over the databus.
 * <ul>
 *   <li>an inbound 1021 bus frame from a known GCS is dispatched by the
 *   inherited {@code Execute()} to a sticky {@link Event_Signalling} for the
 *   local PeerConnectionManager - only when the dialed channel resolves to a
 *   built-in camera entry;</li>
 *   <li>the camera entry the 1012 reply and the 1021 dispatch share is the
 *   single stream entry whose "id" is the stream channel key (the B7
 *   CameraController channel surface), so {@link UAVOSHelper#getCameraByID}
 *   is the "UAVOSModuleCamera lookup by channel" the spec asks for;</li>
 *   <li>an outbound 1021 frame (offer/ICE/hangup payload) reaches
 *   {@link CModule#sendJMSG} with the dialed peer as target.</li>
 * </ul>
 */
public class WebRTCSignalingOverBusTest {

    private static final String OWN_PARTY_ID = "UNIT_PARTY_1";
    private static final String OWN_GROUP = "group1";
    private static final String GCS_PARTY_ID = "GCS_UNIT_1";
    private static final String STREAM_CHANNEL = "back";
    private static final String OTHER_CHANNEL = "front";

    /*** Captures post()/postSticky() instead of the app EventBus. */
    private static class CapturingEventBus implements IEventBus {
        final List<Object> posted = new ArrayList<>();
        final List<Object> sticky = new ArrayList<>();

        @Override
        public void post(final Object object) {
            posted.add(object);
        }

        @Override
        public void postSticky(final Object object) {
            sticky.add(object);
        }

        Event_Signalling lastSignalling() {
            for (int i = sticky.size() - 1; i >= 0; --i) {
                if (sticky.get(i) instanceof Event_Signalling) {
                    return (Event_Signalling) sticky.get(i);
                }
            }
            return null;
        }
    }

    /*** Records what the client hands to the module send helpers. */
    private static class FakeModule extends CModule {
        String lastTarget;
        JSONObject lastJmsg;
        int lastMessageType = -1;
        boolean lastInternal = true;

        FakeModule() {
            this.cUDPClient = new CUDPClient();
        }

        @Override
        public void sendJMSG(String targetPartyID, JSONObject jmsg, int andruav_message_id,
                             boolean internal_message) {
            lastTarget = targetPartyID;
            lastJmsg = jmsg;
            lastMessageType = andruav_message_id;
            lastInternal = internal_message;
        }
    }

    private IEventBus mPreviousEventBus;
    private CapturingEventBus mEventBus;

    @Before
    public void setUp() throws Exception {
        mPreviousEventBus = AndruavEngine.getEventBus();
        mEventBus = new CapturingEventBus();
        AndruavEngine.setEventBus(mEventBus);

        // the dialing GCS must already be a known unit, exactly as its 1004
        // ID would have registered it before any signaling arrives
        AndruavEngine.getAndruavWe7daMapBase()
                .put(GCS_PARTY_ID, new AndruavUnitShadow(OWN_GROUP, GCS_PARTY_ID, true));

        // the built-in camera module the way App.defineBuiltIntModules()
        // registers it: a single stream entry whose "id" is the stream
        // channel key from the B7 CameraController surface
        final UAVOSModuleCamera cameraModule = new UAVOSModuleCamera();
        cameraModule.ModuleId = UAVOSConstants.UAVOS_MODULE_TYPE_CAMERA + OWN_PARTY_ID;
        cameraModule.BuiltInModule = true;
        cameraModule.setModuleMessages(new JSONArray()
                .put(new JSONObject()
                        .put(UAVOSConstants.CAMERA_UNIQUE_NAME, STREAM_CHANNEL)
                        .put(UAVOSConstants.CAMERA_LOCAL_NAME, "camera 0")
                        .put(UAVOSConstants.CAMERA_SUPPORT_VIDEO, true)
                        .put(UAVOSConstants.CAMERA_TYPE, 2)));
        AndruavEngine.getUAVOSMapBase().put(cameraModule.ModuleId, cameraModule);
    }

    @After
    public void tearDown() {
        AndruavEngine.setEventBus(mPreviousEventBus);
        AndruavEngine.getAndruavWe7daMapBase().remove(GCS_PARTY_ID);
        AndruavEngine.getUAVOSMapBase()
                .remove(UAVOSConstants.UAVOS_MODULE_TYPE_CAMERA + OWN_PARTY_ID);
    }

    /***
     * Inbound frame: {@code ms} is written as its JSON text, not a nested
     * object - the strict JVM org.json {@code getString("ms")} refuses a
     * JSONObject value (silently skipping setPayloadTextMessage), while
     * Android's coerces it to this very string. On the wire de_comm sends
     * {@code ms} as an object and the device's getString produces the same
     * text the parser reads here.
     */
    private static JSONObject scalarFrame(final String sender, final String target,
                                          final int messageType, final JSONObject ms) throws Exception {
        return new JSONObject()
                .put(ProtocolHeaders.MSG_ROUTING, ProtocolHeaders.CMD_COMM_INDIVIDUAL)
                .put(ProtocolHeaders.Sender, sender)
                .put(ProtocolHeaders.Group, "gcs_group")
                .put(ProtocolHeaders.MessageType, String.valueOf(messageType))
                .put(ProtocolHeaders.Target, target)
                .put(ProtocolHeaders.TimeStamp, "1730000000000")
                .put(ProtocolHeaders.Message, ms.toString());
    }

    private static JSONObject signalingMs(final String channel, final JSONObject packet) throws Exception {
        // the "w" payload a web GCS sends: packet / channel / id / number
        return new JSONObject().put("w", new JSONObject()
                .put("packet", packet)
                .put("channel", channel)
                .put("id", OWN_PARTY_ID)
                .put("number", GCS_PARTY_ID));
    }

    private void feed(final AndruavDeBusClient client, final JSONObject frame) {
        final byte[] datagram = frame.toString().getBytes(StandardCharsets.UTF_8);
        // direct executor: dispatchBusMessage -> onTextMessage -> Execute inline
        client.onReceive(datagram, datagram.length, frame);
    }


    /***
     * A webclient "joinme" on the published stream channel reaches the local
     * PeerConnectionManager as a sticky Event_Signalling carrying the raw
     * signaling payload and the dialing unit.
     */
    @Test
    public void joinmeOnPublishedChannelPostsSignalling() throws Exception {
        final AndruavDeBusClient client = new AndruavDeBusClient(new FakeModule(), Runnable::run);

        feed(client, scalarFrame(GCS_PARTY_ID, OWN_PARTY_ID,
                Messages.TYPE_AndruavMessage_Signaling,
                signalingMs(STREAM_CHANNEL, new JSONObject().put("joinme", true))));

        final Event_Signalling signalling = mEventBus.lastSignalling();
        assertNotNull(signalling);
        assertEquals(STREAM_CHANNEL, signalling.jsonObject.getString("channel"));
        assertEquals(GCS_PARTY_ID, signalling.jsonObject.getString("number"));
        assertSame(AndruavEngine.getAndruavWe7daMapBase().get(GCS_PARTY_ID),
                signalling.andruavUnitBase);
    }


    /*** A channel that resolves to no built-in camera entry is dropped. */
    @Test
    public void joinmeOnUnknownChannelIsDropped() throws Exception {
        final AndruavDeBusClient client = new AndruavDeBusClient(new FakeModule(), Runnable::run);

        feed(client, scalarFrame(GCS_PARTY_ID, OWN_PARTY_ID,
                Messages.TYPE_AndruavMessage_Signaling,
                signalingMs(OTHER_CHANNEL, new JSONObject().put("joinme", true))));

        assertNull(mEventBus.lastSignalling());
    }


    /***
     * The 1012 camera list and the 1021 dispatch share one source:
     * {@link UAVOSHelper#getCameraList()} publishes the stream entry and
     * {@link UAVOSHelper#getCameraByID} resolves its channel key back to the
     * same built-in module.
     */
    @Test
    public void cameraListResolvesStreamChannelToBuiltInModule() throws Exception {
        final JSONArray cameraList = UAVOSHelper.getCameraList();
        assertEquals(1, cameraList.length());
        assertEquals(STREAM_CHANNEL,
                cameraList.getJSONObject(0).getString(UAVOSConstants.CAMERA_UNIQUE_NAME));

        final UAVOSModuleCamera resolved = UAVOSHelper.getCameraByID(STREAM_CHANNEL);
        assertNotNull(resolved);
        assertTrue(resolved.BuiltInModule);
        assertEquals(UAVOSConstants.UAVOS_MODULE_TYPE_CAMERA + OWN_PARTY_ID,
                resolved.ModuleId);

        assertNull(UAVOSHelper.getCameraByID(OTHER_CHANNEL));
    }


    /***
     * Outbound: a 1021 signaling frame (an SDP/ICE/hangup payload wrapped in
     * {@code w}) reaches sendJMSG with the peer as target on the comm path.
     */
    @Test
    public void outboundSignalingFrameReachesSendJMSG() throws Exception {
        final FakeModule module = new FakeModule();
        final AndruavDeBusClient client = new AndruavDeBusClient(module);

        final JSONObject frame = new JSONObject()
                .put(ProtocolHeaders.MSG_ROUTING, ProtocolHeaders.CMD_COMM_INDIVIDUAL)
                .put(ProtocolHeaders.Sender, OWN_PARTY_ID)
                .put(ProtocolHeaders.Group, OWN_GROUP)
                .put(ProtocolHeaders.MessageType, Messages.TYPE_AndruavMessage_Signaling)
                .put(ProtocolHeaders.Target, GCS_PARTY_ID)
                .put(ProtocolHeaders.Message, signalingMs(STREAM_CHANNEL,
                        new JSONObject().put("type", "offer").put("sdp", "v=0")));

        client.socketSendTextMessage(frame.toString());

        assertEquals(GCS_PARTY_ID, module.lastTarget);
        assertEquals(Messages.TYPE_AndruavMessage_Signaling, module.lastMessageType);
        assertEquals(false, module.lastInternal);
        assertEquals(STREAM_CHANNEL,
                module.lastJmsg.getJSONObject("w").getString("channel"));
    }
}
