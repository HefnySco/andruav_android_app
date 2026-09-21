package org.droidplanner.services.android.impl.core.drone.manager;

import android.content.Context;
import android.os.Handler;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_command_ack;
import com.o3dr.services.android.lib.drone.action.GimbalActions;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.action.Action;

import org.droidplanner.services.android.impl.api.DroneApi;
import org.droidplanner.services.android.impl.communication.service.MAVLinkClient;
import org.droidplanner.services.android.impl.core.MAVLink.MavLinkMsgHandler;
import org.droidplanner.services.android.impl.core.drone.DroneInterfaces;
import org.droidplanner.services.android.impl.core.drone.DroneManager;
import org.droidplanner.services.android.impl.core.drone.autopilot.MavLinkDrone;
import org.droidplanner.services.android.impl.core.drone.autopilot.apm.ArduCopter;
import org.droidplanner.services.android.impl.core.drone.autopilot.apm.ArduPlane;
import org.droidplanner.services.android.impl.core.drone.autopilot.apm.ArduRover;
import org.droidplanner.services.android.impl.core.drone.autopilot.apm.ArduSub;
import org.droidplanner.services.android.impl.core.drone.autopilot.generic.GenericMavLinkDrone;
import org.droidplanner.services.android.impl.core.drone.autopilot.px4.Px4Native;
import org.droidplanner.services.android.impl.core.drone.profiles.ParameterManager;
import org.droidplanner.services.android.impl.core.drone.variables.StreamRates;
import org.droidplanner.services.android.impl.core.firmware.FirmwareType;
import org.droidplanner.services.android.impl.core.gcs.GCSHeartbeat;
import org.droidplanner.services.android.impl.utils.AndroidApWarningParser;

import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

/**
 * Created by Fredia Huya-Kouadio on 12/17/15.
 */
public class MavLinkDroneManager extends DroneManager<MavLinkDrone, MAVLinkPacket> {

    private static final int DEFAULT_STREAM_RATE = 2; //Hz

    private final MAVLinkClient mavClient;
    private final MavLinkMsgHandler mavLinkMsgHandler;
    private final DroneCommandTracker commandTracker;

    private final GCSHeartbeat gcsHeartbeat;

    private final AtomicInteger droneStreamRate = new AtomicInteger(DEFAULT_STREAM_RATE); //Hz

    public MavLinkDroneManager(Context context, ConnectionParameter connParams, Handler handler) {
        super(context, connParams, handler);

        commandTracker = new DroneCommandTracker(handler);

        mavClient = new MAVLinkClient(context, this, connParams, commandTracker);

        this.gcsHeartbeat = new GCSHeartbeat(mavClient, 1);

        this.mavLinkMsgHandler = new MavLinkMsgHandler(this);
        updateDroneStreamRate(connParams);
    }

    public void onVehicleTypeReceived(FirmwareType type) {
        if (drone != null) {
            return;
        }

        final String droneId = connectionParameter.getUniqueId() + ":" + type.getType();

        switch (type) {
            case ARDU_COPTER:
                Timber.i("Instantiating ArduCopter autopilot.");
                this.drone = new ArduCopter(droneId, context, mavClient, handler, new AndroidApWarningParser(), this);
                break;

            case ARDU_PLANE:
                Timber.i("Instantiating ArduPlane autopilot.");
                this.drone = new ArduPlane(droneId, context, mavClient, handler, new AndroidApWarningParser(), this);
                break;

            case ARDU_ROVER:
                Timber.i("Instantiating ArduPlane autopilot.");
                this.drone = new ArduRover(droneId, context, mavClient, handler, new AndroidApWarningParser(), this);
                break;

            case ARDU_SUB:
                Timber.i("Instantiating ArduSub autopilot.");
                this.drone = new ArduSub(droneId, context, mavClient, handler, new AndroidApWarningParser(), this);
                break;

            case PX4_NATIVE:
                Timber.i("Instantiating PX4 Native autopilot.");
                this.drone = new Px4Native(droneId, context, handler, mavClient, new AndroidApWarningParser(), this);
                break;

            case GENERIC:
                Timber.i("Instantiating Generic mavlink autopilot.");
                this.drone = new GenericMavLinkDrone(droneId, context, handler, mavClient, new AndroidApWarningParser(), this);
                break;
        }

        StreamRates streamRates = drone.getStreamRates();
        if (streamRates != null) {
            streamRates.setRates(new StreamRates.Rates(droneStreamRate.get()));
        }

        drone.addDroneListener(this);
        drone.setAttributeListener(this);

        ParameterManager parameterManager = drone.getParameterManager();
        if (parameterManager != null) {
            parameterManager.setParameterListener(this);
        }

    }

    @Override
    protected void doConnect(DroneApi listener, ConnectionParameter connParams) {
        if (mavClient.isDisconnected()) {
            Timber.i("Opening connection for app");
            mavClient.openConnection();
        } else {
            if (isConnected()) {
                listener.onDroneEvent(DroneInterfaces.DroneEventsType.CONNECTED, drone);
                if (!drone.isConnectionAlive())
                    listener.onDroneEvent(DroneInterfaces.DroneEventsType.HEARTBEAT_TIMEOUT, drone);
            }
        }

        mavClient.registerForTLogLogging("appId", connParams.getTLogLoggingUri());

        updateDroneStreamRate(connParams);
    }

    private void updateDroneStreamRate(ConnectionParameter connParams) {
        long eventsDispatchingPeriod = connParams.getEventsDispatchingPeriod();
        if (eventsDispatchingPeriod <= 0) {
            // Keep the current rate.
            return;
        }

        int eventsDispatchingRate = Math.round(1000L / eventsDispatchingPeriod);

        boolean updateComplete;
        do {
            updateComplete = true;

            int currentRate = droneStreamRate.get();
            if (eventsDispatchingRate > currentRate) {
                updateComplete = droneStreamRate.compareAndSet(currentRate, eventsDispatchingRate);
                if (updateComplete && drone != null) {
                    StreamRates rates = drone.getStreamRates();
                    if (rates != null) {
                        rates.setRates(new StreamRates.Rates(droneStreamRate.get()));
                    }
                }
            }
        }while(!updateComplete);
    }

    @Override
    protected void doDisconnect(DroneApi listener) {

        if (listener != null) {
            //TODO:MHEFNY Remove APPID
            mavClient.unregisterForTLogLogging("appId");
            if (isConnected()) {
                listener.onDroneEvent(DroneInterfaces.DroneEventsType.DISCONNECTED, drone);
            }
        }

        if (mavClient.isConnected()  && (connectedApp==null)) {
            //Reset the gimbal mount mode
            executeAsyncAction(new Action(GimbalActions.ACTION_RESET_GIMBAL_MOUNT_MODE), null);

            mavClient.closeConnection();
        }
    }


    private void handleCommandAck(msg_command_ack ack) {
        if (ack != null) {
            commandTracker.onCommandAck(msg_command_ack.MAVLINK_MSG_ID_COMMAND_ACK, ack);
        }
    }

    @Override
    public void notifyReceivedData(MAVLinkPacket packet) {
        MAVLinkMessage receivedMsg = packet.unpack();
        if (receivedMsg == null)
            return;

        if (receivedMsg.msgid == msg_command_ack.MAVLINK_MSG_ID_COMMAND_ACK) {
            msg_command_ack commandAck = (msg_command_ack) receivedMsg;
            handleCommandAck(commandAck);
        } else {
            this.mavLinkMsgHandler.receiveData(receivedMsg);
            if (this.drone != null) {
                this.drone.onMavLinkMessageReceived(receivedMsg);
            }
        }

        if (connectedApp!= null) {
            connectedApp.onReceivedMavLinkMessage(receivedMsg);
        }
    }

    @Override
    public void onConnectionStatus(LinkConnectionStatus connectionStatus) {
        super.onConnectionStatus(connectionStatus);

        switch (connectionStatus.getStatusCode()) {
            case LinkConnectionStatus.DISCONNECTED:
                this.gcsHeartbeat.setActive(false);
                break;

            case LinkConnectionStatus.CONNECTED:
                this.gcsHeartbeat.setActive(true);
                break;
        }
    }

}
