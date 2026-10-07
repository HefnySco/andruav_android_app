package com.o3dr.android.client.apis;


import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;
import com.o3dr.services.android.lib.model.AbstractCommandListener;

import java.util.concurrent.ConcurrentHashMap;


/**
 * Contains drone commands with no defined interaction model yet.
 */
public class ExperimentalApi extends Api {
    private static final ConcurrentHashMap<Drone, ExperimentalApi> experimentalApiCache = new ConcurrentHashMap<>();
    private static final Builder<ExperimentalApi> apiBuilder = new Builder<ExperimentalApi>() {
        @Override
        public ExperimentalApi build(Drone drone) {
            return new ExperimentalApi(drone);
        }
    };

    /**
     * Retrieves an ExperimentalApi instance.
     *
     * @param drone target vehicle.
     * @return a ExperimentalApi instance.
     */
    public static ExperimentalApi getApi(final Drone drone) {
        return getApi(drone, experimentalApiCache, apiBuilder);
    }

    private final Drone drone;

    private ExperimentalApi(Drone drone) {
        this.drone = drone;
    }

    /**
     * Triggers the camera.
     */
    public void triggerCamera() {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.triggerCamera(), null);
    }

    /**
     * This is an advanced/low-level method to send raw mavlink to the vehicle.
     * <p/>
     * This method is included as an ‘escape hatch’ to allow developers to make progress if we’ve
     * somehow missed providing some essential operation in the rest of this API. Callers do
     * not need to populate sysId/componentId/crc in the packet, this method will take care of that
     * before sending.
     * <p/>
     * If you find yourself needing to use this method please contact the drone-platform google
     * group and we’ll see if we can support the operation you needed in some future revision of
     * the API.
     *
     * @param messageWrapper A MAVLinkMessage wrapper instance. No need to fill in
     *                       sysId/compId/seqNum - the API will take care of that.
     */
    public void sendMavlinkMessage(final MavlinkMessageWrapper messageWrapper) {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.sendMavlinkMessage(messageWrapper), null);
    }

    /**
     * Move a servo to a particular pwm value
     *
     * @param channel the output channel the servo is attached to
     * @param pwm     PWM value to output to the servo. Servo’s generally accept pwm values between 1000 and 2000
     */
    public void setServo(final int channel, final int pwm) {
        setServo(channel, pwm, null);
    }

    /**
     * Move a servo to a particular pwm value
     *
     * @param channel  the output channel the servo is attached to
     * @param pwm      PWM value to output to the servo. Servo’s generally accept pwm values between 1000 and 2000
     * @param listener Register a callback to receive update of the command execution state.
     */
    public void setServo(final int channel, final int pwm, final AbstractCommandListener listener) {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.setServo(channel, pwm, vehicleListener), listener);
    }
}
