package ap.andruav_ap.communication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.MAVLink.common.msg_attitude;
import com.MAVLink.common.msg_gps_raw_int;
import com.MAVLink.common.msg_sys_status;
import com.MAVLink.enums.MAV_MODE_FLAG;
import com.MAVLink.enums.MAV_STATE;
import com.MAVLink.enums.MAV_SYS_STATUS_SENSOR;
import com.MAVLink.minimal.msg_heartbeat;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitMapBase;
import com.andruav.andruavUnit.AndruavUnitMe;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.controlBoard.shared.common.VehicleTypes;
import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.protocol.commands.binaryMessages.AndruavBinaryHelper;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import de.databus.CModule;
import de.databus.CUDPClient;
import de.databus.Messages;

/**
 * F2 (C4b, AN-0069): the shadow-unit path end to end over a fake
 * {@link CModule} - an ID frame (as de_comm fans it out) creates the
 * unit keyed by the own party id, a 6502 packed-MAVLink datagram (as
 * de_mavlink emits it) lands mode/arm/battery/GPS on that unit, and the
 * own-unit fields are mirrored onto {@code andruavWe7daBase}.
 */
public class BusUnitViewTest {

    private static final String OWN_PARTY_ID = "UNIT_PARTY_1";
    private static final String OWN_GROUP = "group1";

    private AndruavUnitMapBase mOldMap;

    /*** Minimal module - only the client's "transport up" check needs it. */
    private static class FakeModule extends CModule {
        FakeModule() {
            this.cUDPClient = new CUDPClient();
        }
    }

    private AndruavDeBusClient mClient;
    private AndruavUnitMe mMe;

    /*** Minimal preference so setTelemetry_protocol's log path is safe on the JVM. */
    private static class StubPreference implements com.andruav.interfaces.IPreference {
        @Override public boolean isAndruavLogEnabled() { return false; }
        @Override public String getVersionName() { return "test"; }
        @Override public android.content.Context getContext() { return null; }
        @Override public String getLoginUserName() { return "tester"; }
        @Override public String getModuleType() { return "drone"; }
        @Override public int getBattery_min_value() { return 0; }
        @Override public boolean isChannelReversed(int channelNumber) { return false; }
        @Override public boolean isChannelReturnToCenter(int channelNumber) { return false; }
        @Override public int getChannelmaxValue(int channelNumber) { return 0; }
        @Override public int getChannelminValue(int channelNumber) { return 0; }
        @Override public boolean getSendBackImages() { return false; }
        @Override public int getSmartMavlinkTelemetry() { return 0; }
        @Override public boolean recordUdpSendFeedback(long durationUs) { return false; }
        @Override public int getEffectiveTelemetryLevel() { return 0; }
        @Override public String TAG() { return "test"; }
    }

    @Before
    public void setUp() {
        mOldMap = AndruavEngine.getAndruavWe7daMapBase();
        AndruavEngine.setAndruavWe7daMapBase(new AndruavUnitMapBase());
        AndruavEngine.setAndruavWe7daMasna3(new AndruavUnitFactory());
        AndruavEngine.setPreference(new StubPreference());

        mMe = new AndruavUnitMe();
        mMe.PartyID = OWN_PARTY_ID;
        mMe.GroupName = OWN_GROUP;
        mMe.setLocalPermissions();
        AndruavSettings.andruavWe7daBase = mMe;

        mClient = new AndruavDeBusClient(new FakeModule(), Runnable::run);
        mClient.setUnitIdentity(OWN_PARTY_ID, OWN_GROUP);
        AndruavEngine.setAndruavWS(mClient);
    }

    @After
    public void tearDown() {
        AndruavEngine.setAndruavWS(null);
        AndruavEngine.setAndruavWe7daMapBase(mOldMap);
        AndruavSettings.andruavWe7daBase = null;
    }


    /*** A server-style frame exactly as de_comm fans a 1004 out to modules. */
    private static JSONObject idFrame(final JSONObject ms) throws Exception {
        return new JSONObject()
                .put(ProtocolHeaders.MSG_ROUTING, ProtocolHeaders.CMD_COMM_GROUP)
                .put(ProtocolHeaders.Sender, OWN_PARTY_ID)
                .put(ProtocolHeaders.Group, OWN_GROUP)
                .put(ProtocolHeaders.MessageType,
                        String.valueOf(Messages.TYPE_AndruavMessage_ID))
                .put(ProtocolHeaders.Target, "")
                .put(ProtocolHeaders.TimeStamp, "1730000000000")
                // Android's JSONObject.getString coerces the "ms" object to
                // its dump; the strict JVM jar throws and Andruav_2MR
                // silently swallows it - writing the payload as a string
                // exercises the identical setMessageText path on the JVM.
                .put(ProtocolHeaders.Message, ms.toString());
    }

    /*** A module-sourced (GU, no sd) 6502 datagram - header + 0x00 + MAVLink. */
    private static byte[] mavlinkDatagram(final byte[]... frames) throws Exception {
        final JSONObject header = new JSONObject()
                .put(Messages.INTERMODULE_MODULE_KEY, "de_mavlink")
                .put(Messages.ANDRUAV_PROTOCOL_TARGET_ID, "")
                .put(Messages.INTERMODULE_ROUTING_TYPE, Messages.CMD_COMM_GROUP)
                .put(Messages.ANDRUAV_PROTOCOL_MESSAGE_TYPE,
                        Messages.TYPE_AndruavMessage_MAVLINK)
                .put(Messages.ANDRUAV_PROTOCOL_MESSAGE_CMD, new JSONObject());
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (byte[] frame : frames) {
            body.write(frame);
        }
        return AndruavBinaryHelper.joinBinarywithText(header.toString(), body.toByteArray());
    }

    /*** Delivers a datagram the way CModule's receive callback does: jMsg = the JSON part. */
    private void receive(final byte[] datagram) throws Exception {
        int jsonLen = datagram.length;
        for (int i = 0; i < datagram.length; ++i) {
            if (datagram[i] == 0) {
                jsonLen = i;
                break;
            }
        }
        final JSONObject jMsg = new JSONObject(
                new String(datagram, 0, jsonLen, StandardCharsets.UTF_8));
        mClient.onReceive(datagram, datagram.length, jMsg);
    }


    private void receiveId() throws Exception {
        final JSONObject ms = new JSONObject()
                .put("VT", VehicleTypes.VEHICLE_QUAD)
                .put("GS", false)
                .put("UD", "uav1")
                .put("DS", "native unit")
                .put("p", "XXXXXXXXXXXX") // de_comm's own placeholder permission
                .put("FM", 9)
                .put("AR", 3)            // bit0 ready-to-arm, bit1 armed
                .put("GM", 2)
                .put("TP", 1)
                .put("C", 0)
                .put("B", false);
        final byte[] datagram = idFrame(ms).toString().getBytes(StandardCharsets.UTF_8);
        receive(datagram);
    }


    private static byte[] heartbeatFrame(final boolean armed, final long customMode) {
        final msg_heartbeat heartbeat = new msg_heartbeat();
        heartbeat.sysid = 1;
        heartbeat.compid = 1;
        heartbeat.type = 2; // MAV_TYPE_QUADROTOR
        heartbeat.base_mode = (short) (armed ? MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED : 0);
        heartbeat.system_status = (short) MAV_STATE.MAV_STATE_STANDBY;
        heartbeat.custom_mode = customMode;
        return heartbeat.pack().encodePacket();
    }

    private static byte[] sysStatusFrame() {
        final msg_sys_status sys = new msg_sys_status();
        sys.sysid = 1;
        sys.compid = 1;
        sys.onboard_control_sensors_health = MAV_SYS_STATUS_SENSOR.MAV_SYS_STATUS_PREARM_CHECK;
        sys.voltage_battery = 12600; // mV
        sys.current_battery = 340;   // cA -> 3.4A
        sys.battery_remaining = 85;
        return sys.pack().encodePacket();
    }

    private static byte[] gpsRawFrame() {
        final msg_gps_raw_int gps = new msg_gps_raw_int();
        gps.sysid = 1;
        gps.compid = 1;
        gps.fix_type = 3;
        gps.satellites_visible = 14;
        gps.lat = 300123456;
        gps.lon = 312123456;
        gps.alt = 120000;
        return gps.pack().encodePacket();
    }

    private static byte[] attitudeFrame() {
        final msg_attitude attitude = new msg_attitude();
        attitude.sysid = 1;
        attitude.compid = 1;
        attitude.roll = 0.1f;
        attitude.pitch = -0.2f;
        attitude.yaw = 1.5f;
        return attitude.pack().encodePacket();
    }


    /**
     * A 1004 fanned out by de_comm creates the shadow unit keyed by the
     * own party id, and the mirror lands mode/arm on the me-unit while
     * the app's own permission/GPS-mode settings are NOT clobbered.
     */
    @Test
    public void idFrameCreatesShadowAndMirrorsVehicleState() throws Exception {
        receiveId();

        final AndruavUnitBase shadow = AndruavEngine.getAndruavWe7daMapBase().get(OWN_PARTY_ID);
        assertNotNull(shadow);
        assertEquals("uav1", shadow.UnitID);
        assertEquals(VehicleTypes.VEHICLE_QUAD, shadow.getVehicleType());
        assertTrue(shadow.IsArmed());
        assertEquals(9, shadow.getFlightModeFromBoard());

        // mirrored onto me, through the setters so arm/mode events fire
        assertTrue(mMe.IsArmed());
        assertTrue(mMe.isReadyToArm());
        assertEquals(9, mMe.getFlightModeFromBoard());
        assertEquals(VehicleTypes.VEHICLE_QUAD, mMe.getVehicleType());

        // but de_comm's placeholder permission must not erase the app's
        assertTrue(mMe.canBeDrone());
        assertTrue(mMe.canControl());
    }


    /**
     * A 6502 packed-MAVLink datagram from de_mavlink lands arm/mode,
     * battery and GPS scalars on the shadow unit and on the me-unit.
     */
    @Test
    public void mavlinkFramesLandTelemetryOnShadowAndMe() throws Exception {
        receiveId(); // shadow must exist before telemetry is accepted

        final byte[] datagram = mavlinkDatagram(
                heartbeatFrame(true, 4), // armed, APM guided
                sysStatusFrame(),
                gpsRawFrame(),
                attitudeFrame());
        receive(datagram);

        final AndruavUnitBase shadow = AndruavEngine.getAndruavWe7daMapBase().get(OWN_PARTY_ID);
        assertNotNull(shadow);
        assertTrue(shadow.IsArmed());
        assertEquals(FlightMode.CONST_FLIGHT_CONTROL_GUIDED, shadow.getFlightModeFromBoard());
        assertEquals(14, shadow.LastEvent_FCB_IMU.SATC);
        assertEquals(3, shadow.LastEvent_FCB_IMU.GPS3DFix);
        assertEquals(Math.toDegrees(-0.2), shadow.LastEvent_FCB_IMU.P, 1e-4);
        assertEquals(Math.toDegrees(0.1), shadow.LastEvent_FCB_IMU.R, 1e-4);
        assertEquals(Math.toDegrees(1.5), shadow.LastEvent_FCB_IMU.Y, 1e-4);
        assertEquals(85.0, shadow.LastEvent_Battery.FCB_BatteryRemaining, 1e-6);
        assertEquals(12.6, shadow.LastEvent_Battery.FCB_BatteryVoltage, 1e-6);

        // mirrored onto the me-unit the screens read
        assertTrue(mMe.IsArmed());
        assertTrue(mMe.isReadyToArm());
        assertEquals(FlightMode.CONST_FLIGHT_CONTROL_GUIDED, mMe.getFlightModeFromBoard());
        assertEquals(14, mMe.LastEvent_FCB_IMU.SATC);
        assertEquals(85.0, mMe.LastEvent_Battery.FCB_BatteryRemaining, 1e-6);
        assertEquals(12.6, mMe.LastEvent_Battery.FCB_BatteryVoltage, 1e-6);
        assertEquals(3.4, mMe.LastEvent_Battery.FCB_CurrentConsumed, 1e-6);

        assertTrue(MavlinkTelemetryBridge.isOwnTelemetryLive());
    }


    /*** Telemetry before any 1004 is dropped - same rule as the map's update* helpers. */
    @Test
    public void mavlinkBeforeIdIsDropped() throws Exception {
        final byte[] datagram = mavlinkDatagram(heartbeatFrame(true, 4));
        receive(datagram);
        assertTrue(AndruavEngine.getAndruavWe7daMapBase().isEmpty());
        assertFalse(mMe.IsArmed());
    }


    /*** The subscription list covers the 6502 telemetry carrier. */
    @Test
    public void filterSubscribesToMavlink() {
        final org.json.JSONArray filter = AndruavDeBusClient.buildMessageFilter();
        boolean found = false;
        for (int i = 0; i < filter.length(); ++i) {
            if (filter.optInt(i) == Messages.TYPE_AndruavMessage_MAVLINK) {
                found = true;
            }
        }
        assertTrue(found);
    }
}
