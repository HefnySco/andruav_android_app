package ap.andruav_ap.communication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.controlBoard.shared.common.FlightMode;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import ap.andruav_ap.communication.controlBoard.ControlBoard_DeBus;
import de.databus.CModule;
import de.databus.CUDPClient;
import de.databus.Messages;

/**
 * F3 (C4c, AN-0070): {@link ControlBoard_DeBus} dispatch on a fake
 * {@link CModule}. Each {@code do_*} must reach the module as a
 * {@code sendJMSG} with:
 * <ul>
 *   <li>the unit's own party id as {@code tg} - de_comm fans the datagram
 *   out to every locally subscribed module (de_mavlink), so the command
 *   executes with no server round trip;</li>
 *   <li>{@code internal_message=false} - a normal comm message, so the
 *   same datagram is ALSO relayed to the comm server when one is
 *   connected (a GCS sees the command the unit sent itself);</li>
 *   <li>{@code p} stamped at envelope level - the unit-local equivalent
 *   of the permission the comm server injects for a logged-in party;
 *   without it de_mavlink's permission gate drops the command.</li>
 * </ul>
 */
public class ControlBoardDeBusTest {

    private static final String OWN_PARTY_ID = "UNIT_PARTY_1";
    private static final String OWN_GROUP = "group1";
    private static final long PERMISSION_FULL_CONTROL = 0xffffffffL;

    private static class Sent {
        String target;
        JSONObject jmsg;
        int messageType;
        boolean internal;
    }

    private static class FakeModule extends CModule {
        final List<Sent> sent = new ArrayList<>();

        FakeModule() {
            // sendJMSG is overridden below so the socket is never touched;
            // the field only has to be non-null for the client's
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
    }

    private static class Fixture {
        FakeModule module = new FakeModule();
        AndruavDeBusClient client = new AndruavDeBusClient(module);
        AndruavUnitBase unit = new AndruavUnitBase(OWN_GROUP, OWN_PARTY_ID, false);
        ControlBoard_DeBus board = new ControlBoard_DeBus(unit);

        Fixture() {
            AndruavEngine.setAndruavWS(client);
        }
    }

    @After
    public void tearDown() {
        AndruavEngine.setAndruavWS(null);
    }

    private static Sent lastSent(final FakeModule module) throws Exception {
        assertEquals(1, module.sent.size());
        final Sent s = module.sent.get(0);
        assertEquals(OWN_PARTY_ID, s.target);
        assertFalse(s.internal);
        return s;
    }


    /***
     * The client stamps the sender permission onto the module's outbound
     * envelope - the "p" the comm server would inject for a logged-in
     * party. de_* readers validate it as unsigned, so it must be
     * 4294967295, not -1.
     */
    @Test
    public void outboundEnvelopeStampsFullControlPermission() throws Exception {
        final Fixture f = new Fixture();

        assertEquals(PERMISSION_FULL_CONTROL,
                f.module.getOutboundEnvelope()
                        .getLong(Messages.ANDRUAV_PROTOCOL_MESSAGE_PERMISSION));
    }


    /*** Arm: same ms a GCS sends ({A:true}), to the own unit, non-intermodule. */
    @Test
    public void armReachesModuleAsGCSArm() throws Exception {
        final Fixture f = new Fixture();

        f.board.do_ARM(null);

        final Sent s = lastSent(f.module);
        assertEquals(1030, s.messageType); // TYPE_AndruavMessage_Arm
        assertTrue(s.jmsg.getBoolean("A"));
    }


    /*** Disarm carries A:false + the emergency flag in D. */
    @Test
    public void disarmCarriesEmergencyFlag() throws Exception {
        final Fixture f = new Fixture();

        f.board.do_Disarm(true, null);

        final Sent s = lastSent(f.module);
        assertEquals(1030, s.messageType);
        assertFalse(s.jmsg.getBoolean("A"));
        assertTrue(s.jmsg.getBoolean("D"));
    }


    /*** Mode change: FlightControl {F:mode} - de_mavlink maps F via CFCBModes. */
    @Test
    public void modeGotoReachesModule() throws Exception {
        final Fixture f = new Fixture();

        f.board.do_Mode(FlightMode.CONST_FLIGHT_CONTROL_GUIDED, null);

        final Sent s = lastSent(f.module);
        assertEquals(1010, s.messageType); // TYPE_AndruavMessage_FlightControl
        assertEquals(FlightMode.CONST_FLIGHT_CONTROL_GUIDED, s.jmsg.getInt("F"));
    }


    /*** Guided goto: the GCS wire layout is a=lat, g=lng, l=alt. */
    @Test
    public void guidedPointUsesGCSWireLayout() throws Exception {
        final Fixture f = new Fixture();

        f.board.do_FlytoHere(32.5, 30.25, 15.0, -1, -1, -1, -1, -1);

        final Sent s = lastSent(f.module);
        assertEquals(1033, s.messageType); // TYPE_AndruavMessage_GuidedPoint
        assertEquals(30.25, s.jmsg.getDouble("a"), 1e-6);
        assertEquals(32.5, s.jmsg.getDouble("g"), 1e-6);
        assertEquals(15.0, s.jmsg.getDouble("l"), 1e-6);
    }


    /*** Set-current-mission: RemoteExecute {C:504} with n as a JSON number. */
    @Test
    public void setCurrentMissionSendsNumericSeq() throws Exception {
        final Fixture f = new Fixture();

        f.board.do_SetCurrentMission(3);

        final Sent s = lastSent(f.module);
        assertEquals(1005, s.messageType); // TYPE_AndruavMessage_RemoteExecute
        assertEquals(504, s.jmsg.getInt("C")); // SET_START_MISSION_ITEM
        // de_mavlink validates "n" as number_unsigned - a string would
        // fail validation, so it must serialize as a number.
        assertEquals(3, s.jmsg.getInt("n"));
        assertTrue(s.jmsg.get("n") instanceof Number);
    }


    /*** Mission reload: RemoteExecute {C:501}. */
    @Test
    public void readMissionReloadsFromFCB() throws Exception {
        final Fixture f = new Fixture();

        f.board.do_ReadMission();

        final Sent s = lastSent(f.module);
        assertEquals(1005, s.messageType);
        assertEquals(501, s.jmsg.getInt("C")); // RELOAD_WAY_POINTS_FROM_FCB
    }


    /*** No bus client (native stack down): the command drops, no throw. */
    @Test
    public void commandDropsWithoutBusClient() throws Exception {
        final Fixture f = new Fixture();
        AndruavEngine.setAndruavWS(null);

        f.board.do_ARM(null);

        assertTrue(f.module.sent.isEmpty());
    }
}
