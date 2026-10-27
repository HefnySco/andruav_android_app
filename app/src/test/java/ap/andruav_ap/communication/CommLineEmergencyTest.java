package ap.andruav_ap.communication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitMapBase;
import com.andruav.andruavUnit.AndruavUnitMe;
import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_RemoteExecute;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import ap.andruav_ap.Emergency;
import ap.andruav_ap.denative.providers.phone.PhoneDevices;
import ap.andruav_ap.denative.providers.phone.PhoneOutput;
import ap.andruav_ap.denative.providers.phone.PhoneOutputBank;
import ap.andruav_ap.denative.providers.phone.PhoneOutputSpec;
import de.databus.CModule;
import de.databus.CUDPClient;
import de.databus.Messages;

/**
 * F5 (C4e, AN-0072): the Emergency comm-lost input on the databus.
 * <ul>
 *   <li>de_comm's 9100 module-ID broadcast carries {@code ms.g} - the
 *   comm-server socket status - on every change: ERROR/DISCONNECTED raise
 *   the connection emergency, REGISTERED clears it, transient states do
 *   not flap it;</li>
 *   <li>a 1061 Sync_EventFire with {@code ms.d} = "d_cl_off_r" /
 *   "d_cl_off_e" / "d_cl_on" drives the same input;</li>
 *   <li>a GCS RemoteExecute (1005) MAKEFLASH/MAKEBEEP arrives over the
 *   bus through the normal parser + dispatcher path;</li>
 *   <li>NO mode change is emitted for comm-loss - the flight-mode
 *   failsafe is de_mavlink's job (failsafe_on_comm_lost, C4g/F6).</li>
 * </ul>
 */
public class CommLineEmergencyTest {

    private static final String OWN_PARTY_ID = "UNIT_PARTY_1";
    private static final String OWN_GROUP = "group1";
    private static final String GCS_PARTY_ID = "GCS_PARTY_1";

    /*** Records module sends so "no mode change went out" is assertable. */
    private static class FakeModule extends CModule {
        final List<JSONObject> sent = new ArrayList<>();

        FakeModule() {
            this.cUDPClient = new CUDPClient();
        }

        @Override
        public void sendJMSG(String targetPartyID, JSONObject jmsg, int andruav_message_id,
                             boolean internal_message) {
            try {
                sent.add(new JSONObject()
                        .put("target", targetPartyID)
                        .put("ms", jmsg)
                        .put("mt", andruav_message_id)
                        .put("internal", internal_message));
            } catch (Exception ignored) {
            }
        }
    }

    /***
     * An {@link Emergency} whose Android-bound outputs are recorded:
     * playSiren/doFlash stand in for siren+flash(+vibrate), sendSMS and
     * the (now native-owned) flight-mode failsafe count their calls.
     * Subclassing the app class also satisfies MessageDispatcher's
     * {@code (Emergency) getEmergency()} cast.
     */
    private static class FakeEmergency extends Emergency {
        boolean sirenOn;
        boolean flashOn;
        int sirenCalls;
        int flashCalls;
        int smsCalls;
        int failsafeCalls;

        @Override public boolean getIsSirenActive() { return sirenOn; }
        @Override public boolean getIsFlashing() { return flashOn; }
        @Override public void playSiren(final boolean active, final boolean ignorePermission) {
            sirenOn = active;
            sirenCalls++;
        }
        @Override protected void doFlash(final boolean enable, final boolean ignorePermission) {
            flashOn = enable;
            flashCalls++;
        }
        @Override public void triggerEmergencyFlightModeFaileSafe(final boolean ignoreTiming) {
            failsafeCalls++;
        }
        @Override public int sendSMS(final boolean ignoreTiming) {
            smsCalls++;
            return 0;
        }
        @Override public int sendSMSLocation(final String receiver_num, final boolean b_forced) {
            return 0;
        }
    }

    private com.andruav.EmergencyBase mOldEmergency;
    private AndruavUnitMapBase mOldMap;
    private FakeModule mModule;
    private AndruavDeBusClient mClient;
    private FakeEmergency mEmergency;

    @Before
    public void setUp() {
        mOldEmergency = AndruavEngine.getEmergency();
        mOldMap = AndruavEngine.getAndruavWe7daMapBase();
        AndruavEngine.setAndruavWe7daMapBase(new AndruavUnitMapBase());

        final AndruavUnitMe me = new AndruavUnitMe();
        me.PartyID = OWN_PARTY_ID;
        me.GroupName = OWN_GROUP;
        AndruavSettings.andruavWe7daBase = me;

        mEmergency = new FakeEmergency();
        AndruavEngine.setEmergency(mEmergency);

        mModule = new FakeModule();
        mClient = new AndruavDeBusClient(mModule, Runnable::run);
        mClient.setUnitIdentity(OWN_PARTY_ID, OWN_GROUP);
    }

    @After
    public void tearDown() {
        AndruavEngine.setEmergency(mOldEmergency);
        AndruavEngine.setAndruavWe7daMapBase(mOldMap);
        AndruavSettings.andruavWe7daBase = null;
    }

    private void receive(final JSONObject frame) throws Exception {
        final byte[] datagram = frame.toString().getBytes(StandardCharsets.UTF_8);
        mClient.onReceive(datagram, datagram.length, frame);
    }

    /*** The 9100 module-ID broadcast de_comm emits on every server-status change. */
    private static JSONObject moduleIdFrame(final int socketStatus) throws Exception {
        return new JSONObject()
                .put(Messages.INTERMODULE_MODULE_KEY, "de_comm")
                .put(Messages.INTERMODULE_ROUTING_TYPE, Messages.CMD_TYPE_INTERMODULE)
                .put(Messages.ANDRUAV_PROTOCOL_MESSAGE_TYPE, Messages.TYPE_AndruavModule_ID)
                .put(Messages.ANDRUAV_PROTOCOL_MESSAGE_CMD, new JSONObject()
                        .put(Messages.JSON_INTERMODULE_SOCKET_STATUS, socketStatus)
                        .put(Messages.JSON_INTERMODULE_PARTY_RECORD, new JSONObject()
                                .put(Messages.ANDRUAV_PROTOCOL_SENDER, OWN_PARTY_ID)
                                .put(Messages.ANDRUAV_PROTOCOL_GROUP_ID, OWN_GROUP)));
    }

    /*** A 1061 Sync_EventFire as a module fires it on the unit bus (intermodule). */
    private static JSONObject eventFireFrame(final String eventName) throws Exception {
        return new JSONObject()
                .put(Messages.INTERMODULE_MODULE_KEY, "de_comm")
                .put(Messages.INTERMODULE_ROUTING_TYPE, Messages.CMD_TYPE_INTERMODULE)
                .put(Messages.ANDRUAV_PROTOCOL_MESSAGE_TYPE, Messages.TYPE_AndruavMessage_Sync_EventFire)
                .put(Messages.ANDRUAV_PROTOCOL_MESSAGE_CMD, new JSONObject()
                        .put("d", eventName)
                        .put("n", "1730000000000-1"));
    }

    /***
     * A server-sourced GCS RemoteExecute frame (ty/sd/gr/mt/tg/ts/ms).
     * {@code ms} goes in as a string so the strict JVM org.json reads it
     * the same way Android coerces the object form (see BusUnitViewTest).
     */
    private static JSONObject remoteExecuteFrame(final int remoteCommandId) throws Exception {
        return new JSONObject()
                .put(ProtocolHeaders.MSG_ROUTING, ProtocolHeaders.CMD_COMM_INDIVIDUAL)
                .put(ProtocolHeaders.Sender, GCS_PARTY_ID)
                .put(ProtocolHeaders.Group, "gcs_group")
                .put(ProtocolHeaders.MessageType,
                        String.valueOf(Messages.TYPE_AndruavMessage_RemoteExecute))
                .put(ProtocolHeaders.Target, OWN_PARTY_ID)
                .put(ProtocolHeaders.TimeStamp, "1730000000000")
                .put(ProtocolHeaders.Message,
                        new JSONObject().put("C", remoteCommandId).toString());
    }


    /*** Comm-off via the ms.g socket status: emergency raises, siren+flash fire. */
    @Test
    public void linkStatusErrorTriggersConnectionEmergency() throws Exception {
        receive(moduleIdFrame(Messages.SOCKET_STATUS_ERROR));

        assertTrue(mEmergency.sirenOn);
        assertTrue(mEmergency.flashOn);
        assertEquals(1, mEmergency.smsCalls);
    }

    /*** Comm-on clears what comm-off raised. */
    @Test
    public void linkStatusRegisteredClearsConnectionEmergency() throws Exception {
        receive(moduleIdFrame(Messages.SOCKET_STATUS_ERROR));
        assertTrue(mEmergency.sirenOn);

        receive(moduleIdFrame(Messages.SOCKET_STATUS_REGISTERED));

        assertFalse(mEmergency.sirenOn);
        assertFalse(mEmergency.flashOn);
    }

    /*** Repeated / transient broadcasts do not retrigger or flap the alarm. */
    @Test
    public void repeatedAndTransientStatusesDoNotFlap() throws Exception {
        receive(moduleIdFrame(Messages.SOCKET_STATUS_ERROR));
        final int sirenCalls = mEmergency.sirenCalls;

        receive(moduleIdFrame(Messages.SOCKET_STATUS_ERROR));
        receive(moduleIdFrame(Messages.SOCKET_STATUS_DISCONNECTED));
        receive(moduleIdFrame(Messages.SOCKET_STATUS_CONNECTING));

        assertEquals(sirenCalls, mEmergency.sirenCalls);
        assertTrue(mEmergency.sirenOn); // still raised

        receive(moduleIdFrame(Messages.SOCKET_STATUS_REGISTERED));
        assertFalse(mEmergency.sirenOn);
    }

    /*** The 1061 d_cl_off_* / d_cl_on events drive the same input. */
    @Test
    public void commLineEventFireDrivesConnectionEmergency() throws Exception {
        receive(eventFireFrame("d_cl_off_e"));
        assertTrue(mEmergency.sirenOn);
        assertTrue(mEmergency.flashOn);

        receive(eventFireFrame("d_cl_on"));
        assertFalse(mEmergency.sirenOn);
        assertFalse(mEmergency.flashOn);

        receive(eventFireFrame("d_cl_off_r"));
        assertTrue(mEmergency.sirenOn);

        // unrelated events are ignored
        final int sirenCalls = mEmergency.sirenCalls;
        receive(eventFireFrame("detect.detected"));
        receive(eventFireFrame("d_f_ms"));
        assertEquals(sirenCalls, mEmergency.sirenCalls);
        assertTrue(mEmergency.sirenOn);
    }

    /*** A GCS MAKEFLASH/MAKEBEEP reaches Emergency over the normal bus dispatch. */
    @Test
    public void gcsRemoteExecuteTriggersEmergencyOutputs() throws Exception {
        receive(remoteExecuteFrame(AndruavMessage_RemoteExecute.RemoteCommand_MAKEFLASH));
        assertTrue(mEmergency.flashOn);

        receive(remoteExecuteFrame(AndruavMessage_RemoteExecute.RemoteCommand_MAKEBEEP));
        assertTrue(mEmergency.sirenOn);
    }

    /***
     * Comm-loss sends nothing on the bus: the flight-mode failsafe moved
     * into de_mavlink (failsafe_on_comm_lost, C4g/F6) - no mode change,
     * no failsafe call.
     */
    @Test
    public void commOffSendsNoModeChange() throws Exception {
        receive(moduleIdFrame(Messages.SOCKET_STATUS_ERROR));
        receive(eventFireFrame("d_cl_off_e"));

        assertTrue(mModule.sent.isEmpty());
        assertEquals(0, mEmergency.failsafeCalls);
    }

    /***
     * The real {@link Emergency} flash path lands on the phone.screen
     * choke point when a bank exists (B4) - a GCS MAKEFLASH reports
     * src "legacy".
     */
    @Test
    public void gcsFlashGoesThroughPhoneScreenChokePoint() throws Exception {
        final List<Boolean> driverCalls = new ArrayList<>();
        final List<String> facts = new ArrayList<>();
        final PhoneOutputBank bank = new PhoneOutputBank(
                java.util.Collections.singletonList(
                        new PhoneOutputSpec("screen", PhoneOutputSpec.KIND_SCREEN)),
                spec -> on -> {
                    driverCalls.add(on);
                    return null;
                },
                (delayMs, task) -> task.run());
        bank.setFactEmitter((key, verb, v, src) -> facts.add(key + "." + verb + "." + src));

        final Field bankField = PhoneDevices.class.getDeclaredField("s_bank");
        bankField.setAccessible(true);
        bankField.set(null, bank);
        try {
            final Emergency emergency = new Emergency();
            emergency.triggerFlashByGCS(true);
        } finally {
            bankField.set(null, null);
        }

        assertEquals(java.util.Collections.singletonList(true), driverCalls);
        assertNotNull(bank.output("screen"));
        assertTrue(bank.output("screen").isOn());
        assertTrue(facts.contains("screen.went_on.legacy"));
    }
}
