package ap.andruav_ap.communication;

import com.andruav.AndruavEngine;
import com.andruav.event.networkEvent.EventSocketState;
import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.protocol.commands.binaryMessages.AndruavBinary_2MR;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ID;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;
import com.andruav.protocol.communication.websocket.AndruavWSClientBase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

import de.databus.CModule;
import de.databus.Messages;

/**
 * F1 (C4a, AN-0068): {@link AndruavWSClientBase} over the DroneEngage
 * databus instead of a WebSocket. In native mode the unit's comm link is
 * de_comm's job; this client lets every {@code AndruavEngine.getAndruavWS()}
 * and {@code AndruavFacade} caller keep working unchanged:
 * <ul>
 *   <li>outbound: {@link #socketSendTextMessage}/{@link #socketSendBinaryMessage}
 *   receive the same frames the WS client would put on the wire
 *   ({@code ty/sd/gr/mt/tg/ms}), unwrap them, and hand
 *   {@code target/mt/ms} to {@link CModule#sendJMSG}/{@link CModule#sendBMSG}
 *   which add the bus envelope ({@code GU/tg/ty/mt/ms}) themselves;</li>
 *   <li>inbound: {@link CModule#setOnReceive} runs on the {@code de_databus-rx}
 *   UDP thread, so each datagram is posted to the WS handler thread before the
 *   inherited {@link #onTextMessage(String)}/{@link #onBinaryMessage(byte[])}
 *   run the normal parser + {@code Execute()} dispatch.</li>
 * </ul>
 * The module socket itself is owned by {@code DeAndroidModuleService} - this
 * class only borrows it: {@code connect}/{@code socketDisconnect} are
 * therefore no-ops, and {@code isConnected()} follows the module lifetime.
 */
public class AndruavDeBusClient extends AndruavWSClientBase implements CModule.OnReceiveCallback {

    /*** Owns inbound dispatch - same collaborator the WS client delegates to. */
    private final MessageDispatcher messageDispatcher;

    /*** The databus module this client rides on; owned by the hosting service. */
    private CModule mModule;

    /***
     * Executor that moves inbound datagrams off the UDP receive thread.
     * Null means "post on the WS handler thread" (see {@link #start()});
     * unit tests inject a direct executor.
     */
    private final Executor mInboundExecutor;

    /*** This unit's party id / group - used to stamp module-sourced frames. */
    private String mUnitPartyId;
    private String mUnitGroupName;


    public AndruavDeBusClient(final CModule module) {
        this(module, null);
    }

    /***
     * @param module          the databus module (already {@code defineModule}d).
     * @param inboundExecutor executor for inbound dispatch; {@code null} posts
     *                        each datagram on the WS handler thread created by
     *                        {@link #start()}.
     */
    public AndruavDeBusClient(final CModule module, final Executor inboundExecutor) {
        super();
        mModule = module;
        mInboundExecutor = inboundExecutor;
        messageDispatcher = new MessageDispatcher();

        synchronized (mSocketStateSync) {
            mkillMe = false;
            setSocketState(SOCKETSTATE_CONNECTED);
            setSocketAction(SOCKETACTION_NONE);
        }
    }


    /***
     * Identity used to stamp frames that arrive from a sibling module
     * (see {@link #stampBusSender}).
     */
    public void setUnitIdentity(final String partyId, final String groupName) {
        mUnitPartyId = partyId;
        mUnitGroupName = groupName;
    }


    /***
     * Spins up the inherited handler thread (send queue + scheduled tasks).
     * Separate from the constructor so unit tests can use the client without
     * an Android Looper.
     */
    public void start() {
        if (mhandler == null) {
            initHandler();
        }
    }


    /***
     * The module's {@code m_message_filter} subscription list for native mode:
     * every type {@code Execute()} handles for a unit view - own-unit
     * telemetry/identity (shadow unit fed by de_mavlink), missions, geofence,
     * 1061 events, 1021 WebRTC signaling, and the GCS commands the Java side
     * still answers (camera, remote-execute, TTS, config).
     */
    public static JSONArray buildMessageFilter() {
        return new JSONArray()
                // intermodule: sibling modules' position reports (B8 gate)
                .put(Messages.TYPE_AndruavModule_Location_Info)
                // unit view: identity + telemetry
                .put(Messages.TYPE_AndruavMessage_ID)
                .put(Messages.TYPE_AndruavMessage_GPS)
                .put(Messages.TYPE_AndruavMessage_POWER)
                .put(Messages.TYPE_AndruavMessage_NAV_INFO)
                .put(Messages.TYPE_AndruavMessage_HomeLocation)
                .put(Messages.TYPE_AndruavMessage_SET_HOME_LOCATION)
                .put(Messages.TYPE_AndruavMessage_DistinationLocation)
                .put(Messages.TYPE_AndruavMessage_CommSignalsStatus)
                .put(Messages.TYPE_AndruavMessage_SensorsStatus)
                .put(Messages.TYPE_AndruavMessage_DroneReport)
                .put(Messages.TYPE_AndruavMessage_Error)
                // waypoints & mission status
                .put(Messages.TYPE_AndruavMessage_WayPoints)
                .put(Messages.TYPE_AndruavMessage_UploadWayPoints)
                .put(com.andruav.protocol.commands.textMessages.AndruavMessage_ExternalCommand_WayPoints
                        .TYPE_AndruavResala_ExternalCommand_WayPoints)
                .put(Messages.TYPE_AndruavMessage_Mission_Item_Sequence)
                // geofence
                .put(Messages.TYPE_AndruavMessage_GeoFence)
                .put(Messages.TYPE_AndruavMessage_ExternalGeoFence)
                .put(Messages.TYPE_AndruavMessage_GEOFenceHit)
                .put(Messages.TYPE_AndruavMessage_GeoFenceAttachStatus)
                // events + webrtc signaling
                .put(Messages.TYPE_AndruavMessage_Sync_EventFire)
                .put(Messages.TYPE_AndruavMessage_Signaling)
                .put(Messages.TYPE_AndruavMessage_UDPProxy_Info)
                // commands issued to the unit
                .put(Messages.TYPE_AndruavMessage_RemoteExecute)
                .put(Messages.TYPE_AndruavMessage_FlightControl)
                .put(Messages.TYPE_AndruavMessage_Arm)
                .put(Messages.TYPE_AndruavMessage_ChangeAltitude)
                .put(Messages.TYPE_AndruavMessage_Land)
                .put(Messages.TYPE_AndruavMessage_GuidedPoint)
                .put(Messages.TYPE_AndruavMessage_CirclePoint)
                .put(Messages.TYPE_AndruavMessage_DoYAW)
                .put(Messages.TYPE_AndruavMessage_ChangeSpeed)
                .put(com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_GimbalCtrl
                        .TYPE_AndruavMessage_GimbalCtrl)
                .put(Messages.TYPE_AndruavMessage_RemoteControl2)
                .put(Messages.TYPE_AndruavMessage_RemoteControlSettings)
                .put(Messages.TYPE_AndruavMessage_CameraList)
                .put(Messages.TYPE_AndruavMessage_CameraZoom)
                .put(Messages.TYPE_AndruavMessage_CameraSwitch)
                .put(Messages.TYPE_AndruavMessage_CameraFlash)
                .put(com.andruav.protocol.commands.textMessages.AndruavMessage_Sound_TextToSpeech
                        .TYPE_AndruavMessage_Sound_TextToSpeech)
                // GCS-driven config of the app itself
                .put(Messages.TYPE_AndruavMessage_ConfigCOM)
                .put(Messages.TYPE_AndruavMessage_ConfigFCB)
                .put(com.andruav.protocol.commands.textMessages.Configuration.AndruavMessage_Config_UnitID
                        .TYPE_AndruavMessage_Config_UnitID)
                .put(com.andruav.protocol.commands.textMessages.Configuration.AndruavMessage_Config_Preference
                        .TYPE_AndruavResala_Config_Preference)
                // binary family (ids not defined in de.databus.Messages)
                .put(Messages.TYPE_AndruavMessage_IMG)
                .put(com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMU
                        .TYPE_AndruavMessage_BinaryIMU)
                .put(com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_WayPoints
                        .TYPE_AndruavMessageBinary_WayPoints)
                .put(com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMUStatistics
                        .TYPE_AndruavMessage_IMUStatistics)
                .put(com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_RemoteControl
                        .TYPE_AndruavMessage_RemoteControl)
                .put(com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_WayPointsUpdates
                        .TYPE_AndruavMessage_WayPointsUpates)
                .put(com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_ExternalCommand_WayPoints
                        .TYPE_AndruavResalaBinary_ExternalCommand_WayPoints)
                .put(com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_RemoteControlSettings
                        .TYPE_AndruavMessage_RemoteControlSettings)
                .put(Messages.TYPE_AndruavMessage_LightTelemetry)
                .put(Messages.TYPE_AndruavMessage_ServoChannel);
    }


    // ------------------------------------------------------------------
    // Transport hooks (AndruavWSClientBase)
    // ------------------------------------------------------------------

    /***
     * The payload is a complete Andruav WS frame built by
     * {@link Andruav_2MR#getJscon} ({@code ty/sd/gr/mt/tg/ts/ms}).
     * {@link CModule#sendJMSG} re-wraps {@code ms} with the bus envelope
     * ({@code GU/tg/ty/mt}), so a forwarded message is the same one a GCS
     * sends: non-intermodule, addressed by {@code tg} (empty = group).
     */
    @Override
    protected void socketSendTextMessage(final String payload) {
        final CModule module = mModule;
        if ((module == null) || (module.cUDPClient == null)) return;

        try {
            final JSONObject frame = new JSONObject(payload);
            if (!frame.has(ProtocolHeaders.Message)) return;

            final int messageType = frame.optInt(ProtocolHeaders.MessageType, -1);
            if (messageType < 0) {
                // string mt (de_auth & friends) has no bus equivalent
                return;
            }
            if (messageType == AndruavMessage_ID.TYPE_AndruavMessage_ID) {
                // In native mode the unit ID is owned by de_comm (fed by
                // de_mavlink); a Java-side ID payload lacks the fields its
                // 1004 handler reads unconditionally, so it must never
                // reach the bus.
                return;
            }

            final Object messageCmd = frame.get(ProtocolHeaders.Message);
            final JSONObject jmsg = (messageCmd instanceof JSONObject)
                    ? (JSONObject) messageCmd : new JSONObject();

            if (ProtocolHeaders.CMD_TYPE_SYS.equals(frame.optString(ProtocolHeaders.MSG_ROUTING))) {
                // system frames (ping & co) take the module sys path to _SYS_
                module.send_sys_msg(jmsg, messageType);
                return;
            }

            final String target = frame.optString(ProtocolHeaders.Target, "");
            module.sendJMSG(target, jmsg, messageType, false);
        } catch (Exception e) {
            AndruavEngine.log().logException("debus_sendtext", e);
        }
    }


    /***
     * The payload is {@code JSON header + 0x00 + binary} built by
     * {@link AndruavBinary_2MR#getJscon}. The header is unwrapped and the
     * binary body goes to {@link CModule#sendBMSG} which rebuilds the same
     * datagram on the bus.
     */
    @Override
    protected void socketSendBinaryMessage(final byte[] payload) {
        final CModule module = mModule;
        if ((module == null) || (module.cUDPClient == null) || (payload == null)) return;

        try {
            final int splitIndex = splitIndexOf(payload, payload.length);
            if (splitIndex < 0) return;

            final JSONObject header = new JSONObject(
                    new String(payload, 0, splitIndex, StandardCharsets.UTF_8));
            final int messageType = header.optInt(ProtocolHeaders.MessageType, -1);
            if (messageType < 0) return;

            final String target = header.optString(ProtocolHeaders.Target, "");
            final int bodyLength = payload.length - splitIndex - 1;
            final byte[] body = new byte[bodyLength];
            if (bodyLength > 0) {
                System.arraycopy(payload, splitIndex + 1, body, 0, bodyLength);
            }
            final Object messageCmd = header.opt(ProtocolHeaders.Message);

            module.sendBMSG(target, body, bodyLength, messageType, false,
                    (messageCmd instanceof JSONObject) ? messageCmd : new JSONObject());
        } catch (Exception e) {
            AndruavEngine.log().logException("debus_sendbinary", e);
        }
    }


    /*** The module socket is owned by the hosting service - nothing to close here. */
    @Override
    protected void socketDisconnect() {
    }


    // ------------------------------------------------------------------
    // Databus receive side
    // ------------------------------------------------------------------

    /***
     * {@link CModule.OnReceiveCallback} entry point - runs on the
     * {@code de_databus-rx} UDP thread, so the message is handed off before
     * {@link #dispatchBusMessage} does any parsing.
     */
    @Override
    public void onReceive(final byte[] message, final int len, final JSONObject jMsg) {
        if (mkillMe) return;

        final Runnable task = new Runnable() {
            @Override
            public void run() {
                dispatchBusMessage(message, len, jMsg);
            }
        };

        if (mInboundExecutor != null) {
            mInboundExecutor.execute(task);
            return;
        }
        if (mhandler != null) {
            mhandler.post(task);
        }
    }


    /***
     * Runs one received datagram through the inherited WS receive path.
     * Intermodule ({@code "uv"}) traffic belongs to the capability host -
     * except the 9100 module-ID reply, which is the bus equivalent of the
     * server's registration welcome and flips the socket state to
     * {@link #SOCKETSTATE_REGISTERED}.
     */
    void dispatchBusMessage(final byte[] message, final int len, final JSONObject jMsg) {
        try {
            TotalPacketsRecieved += 1;
            TotalBytesRecieved += len;

            if (Messages.CMD_TYPE_INTERMODULE.equals(jMsg.optString(Messages.INTERMODULE_ROUTING_TYPE))) {
                if (jMsg.optInt(Messages.ANDRUAV_PROTOCOL_MESSAGE_TYPE)
                        == Messages.TYPE_AndruavModule_ID) {
                    if (getSocketState() != SOCKETSTATE_REGISTERED) {
                        setSocketState(SOCKETSTATE_REGISTERED);
                        setSocketAction(SOCKETACTION_NONE);
                    }
                }
                return;
            }

            stampBusSender(jMsg);

            final int splitIndex = splitIndexOf(message, len);
            if (splitIndex < 0) {
                // pure JSON text frame
                onTextMessage(jMsg.toString());
            } else {
                // JSON header + 0x00 + binary payload: re-serialize the
                // (possibly sender-stamped) header, keep the binary bytes.
                final byte[] jsonPart = jMsg.toString().getBytes(StandardCharsets.UTF_8);
                final int binaryLength = len - splitIndex - 1;
                final byte[] fullMessage = new byte[jsonPart.length + 1 + binaryLength];
                System.arraycopy(jsonPart, 0, fullMessage, 0, jsonPart.length);
                if (binaryLength > 0) {
                    System.arraycopy(message, splitIndex + 1, fullMessage, jsonPart.length + 1, binaryLength);
                }
                onBinaryMessage(fullMessage);
            }
        } catch (Exception e) {
            AndruavEngine.log().logException("debus_receive", e);
        }
    }


    /***
     * Comm frames de_comm fans out to modules are forwarded unchanged:
     * server-sourced ones carry {@code sd/gr}; module-sourced ones carry
     * {@code GU} (sender module key) and no {@code sd} at all - de_comm
     * only adds the sender fields on the server leg. On a unit-local bus a
     * module-sourced comm message IS this unit talking, so stamp the
     * unit's own party id/group for the unit-map path (the "shadow unit"
     * of the C1 spike).
     */
    private void stampBusSender(final JSONObject jMsg) throws org.json.JSONException {
        if ((mUnitPartyId != null) && !jMsg.has(ProtocolHeaders.Sender)) {
            jMsg.put(ProtocolHeaders.Sender, mUnitPartyId);
        }
        if ((mUnitGroupName != null) && !jMsg.has(ProtocolHeaders.Group)) {
            jMsg.put(ProtocolHeaders.Group, mUnitGroupName);
        }
    }


    /*** Index of the {@code 0x00} separating a JSON header from binary data; -1 for pure text. */
    private static int splitIndexOf(final byte[] message, final int len) {
        final int limit = Math.min(len, message.length);
        for (int i = 0; i < limit; i++) {
            if (message[i] == 0) return i;
        }
        return -1;
    }


    // ------------------------------------------------------------------
    // Connection lifecycle - the transport is the service's module socket.
    // ------------------------------------------------------------------

    @Override
    public boolean isConnected() {
        return (mModule != null) && (mModule.cUDPClient != null) && !mkillMe;
    }

    /*** "Connect" only re-marks the client alive; the module link is not ours. */
    @Override
    public void connect(final String url) {
        synchronized (mSocketStateSync) {
            mkillMe = false;
            setSocketState(SOCKETSTATE_CONNECTED);
            setSocketAction(SOCKETACTION_NONE);
        }
    }

    @Override
    public void connect(final URI url) {
        connect(url != null ? url.toString() : null);
    }

    @Override
    public void shutDown() {
        super.shutDown();
        mModule = null;
        if (AndruavEngine.getAndruavWS() == this) {
            AndruavEngine.setAndruavWS(null);
        }
    }

    @Override
    protected void onError(final int code, final String reason) {
        AndruavEngine.getEventBus().post(
                new EventSocketState(EventSocketState.ENUM_SOCKETSTATE.onError, reason));
    }


    // ------------------------------------------------------------------
    // Inbound dispatch - delegated to MessageDispatcher like the WS client.
    // ------------------------------------------------------------------

    @Override
    protected void onBinaryMessage(final AndruavBinary_2MR andruavBinary2MR) {
        messageDispatcher.onBinaryMessage(andruavBinary2MR);
    }

    @Override
    protected void onTextMessage(final Andruav_2MR andruav2MR) {
    }

    @Override
    protected void executeInternalBinaryCommand(final AndruavBinary_2MR andruav_2MR) {
        messageDispatcher.executeInternalBinaryCommand(andruav_2MR);
    }

    @Override
    protected void executeInternalCommand(final Andruav_2MR andruav2MR) {
        messageDispatcher.executeInternalCommand(andruav2MR);
    }

    @Override
    protected void executeRemoteExecuteCMD(final Andruav_2MR andruav_2MR) {
        messageDispatcher.executeRemoteExecuteCMD(andruav_2MR);
    }
}
