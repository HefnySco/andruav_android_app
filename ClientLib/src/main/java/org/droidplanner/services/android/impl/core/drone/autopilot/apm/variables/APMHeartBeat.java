package org.droidplanner.services.android.impl.core.drone.autopilot.apm.variables;

import android.os.Handler;

import org.droidplanner.services.android.impl.core.drone.DroneInterfaces;
import org.droidplanner.services.android.impl.core.drone.autopilot.MavLinkDrone;
import org.droidplanner.services.android.impl.core.drone.autopilot.apm.ArduPilot;
import org.droidplanner.services.android.impl.core.drone.variables.HeartBeat;

/**
 * Created by Fredia Huya-Kouadio on 10/24/15.
 */
public class APMHeartBeat extends HeartBeat {

    private static final long HEARTBEAT_IMU_CALIBRATION_TIMEOUT = 35000L; //ms

    protected static final int IMU_CALIBRATION = 3;

    public APMHeartBeat(ArduPilot myDrone, Handler handler) {
        super(myDrone, handler);
    }

    @Override
    public void onDroneEvent(DroneInterfaces.DroneEventsType event, MavLinkDrone drone){
        if (event == DroneInterfaces.DroneEventsType.CALIBRATION_IMU) {//Set the heartbeat in imu calibration mode.
            heartbeatState = IMU_CALIBRATION;
            restartWatchdog(HEARTBEAT_IMU_CALIBRATION_TIMEOUT);
        } else {
            super.onDroneEvent(event, drone);
        }
    }

    @Override
    protected void onHeartbeatTimeout(){
        if (heartbeatState == IMU_CALIBRATION) {
            restartWatchdog(HEARTBEAT_IMU_CALIBRATION_TIMEOUT);
            myDrone.notifyDroneEvent(DroneInterfaces.DroneEventsType.CALIBRATION_TIMEOUT);
        } else {
            super.onHeartbeatTimeout();
        }
    }
}
