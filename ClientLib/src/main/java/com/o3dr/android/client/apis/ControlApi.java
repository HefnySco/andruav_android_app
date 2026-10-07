package com.o3dr.android.client.apis;


import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.error.CommandExecutionError;
import com.o3dr.services.android.lib.model.AbstractCommandListener;

import java.util.concurrent.ConcurrentHashMap;


/**
 * Provides access to the vehicle control functionality.
 * <p/>
 * Use of this api might required the vehicle to be in a specific flight mode (i.e: GUIDED)
 * <p/>
 * Created by Fredia Huya-Kouadio on 9/7/15.
 */
public class ControlApi extends Api {

    private static final ConcurrentHashMap<Drone, ControlApi> apiCache = new ConcurrentHashMap<>();
    private static final Builder<ControlApi> apiBuilder = new Builder<ControlApi>() {
        @Override
        public ControlApi build(Drone drone) {
            return new ControlApi(drone);
        }
    };

    /**
     * Retrieves a control api instance.
     *
     * @param drone
     * @return
     */
    public static ControlApi getApi(final Drone drone) {
        return getApi(drone, apiCache, apiBuilder);
    }

    private final Drone drone;

    private ControlApi(Drone drone) {
        this.drone = drone;
    }

    /**
     * Perform a guided take off.
     *
     * @param altitude altitude in meters
     * @param listener Register a callback to receive update of the command execution state.
     */
    public void takeoff(double altitude, AbstractCommandListener listener) {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.takeoff(altitude, vehicleListener), listener);
    }

    /**
     * Control Drone in Guided mode using velocity and yaw
     * @param vx  velocity in m/s
     * @param vy  velocity in m/s
     * @param vz  velocity in m/s
     * @param yaw target angle
     * @param listener
     */
    public void guidedVelocityInLocalFrame(final double vx, final double vy, final double vz, double yawRate, final double yaw, short  coordinateFrame, short typeMask, AbstractCommandListener listener)
    {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.sendGuidedVelocityInLocalFrame((float) vx, (float) vy, (float) vz,
                (float) yawRate, (float) yaw, coordinateFrame, typeMask, vehicleListener), listener);
    }


    /**
     * Control Drone in Guided mode using velocity and yaw
     * @param vx  velocity in m/s
     * @param vy  velocity in m/s
     * @param vz  velocity in m/s
     * @param yaw target angle
     * @param listener
     */
    public void guidedVelocityInGlobalFrame(final double vx, final double vy, final double vz, double yawRate, final double yaw, short  coordinateFrame, short typeMask, AbstractCommandListener listener)
    {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.sendGuidedVelocityInGlobalFrame((float) vx, (float) vy, (float) vz,
                (float) yawRate, (float) yaw, coordinateFrame, typeMask, vehicleListener), listener);
    }

    /**
     * Instructs the vehicle to go to the specified location.
     *
     * @param point    target location
     * @param force    true to enable guided mode is required.
     * @param listener Register a callback to receive update of the command execution state.
     */
    public void goTo(LatLong point, boolean force, AbstractCommandListener listener) {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.sendGuidedPoint(point, force, vehicleListener), listener);
    }

    /**
     * Instructs the vehicle to climb to the specified altitude.
     *
     * @param altitude altitude in meters
     */
    public void climbTo(double altitude) {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.setGuidedAltitude(altitude), null);
    }

    /**
     * Instructs the vehicle to turn to the specified target angle
     *
     * @param targetAngle Target angle in degrees [0-360], with 0 == north.
     * @param turnRate    Turning rate normalized to the range [-1.0f, 1.0f]. Positive values for clockwise turns, and negative values for counter-clockwise turns.
     * @param isRelative  True is the target angle is relative to the current vehicle attitude, false otherwise if it's absolute.
     * @param listener    Register a callback to receive update of the command execution state.
     */
    public void turnTo(float targetAngle, float turnRate, boolean isRelative, AbstractCommandListener listener) {
        if (!isWithinBounds(targetAngle, 0, 360) || !isWithinBounds(turnRate, -1.0f, 1.0f)) {
            postErrorEvent(CommandExecutionError.COMMAND_FAILED, listener);
            return;
        }

        drone.executeCommand((vehicle, vehicleListener) -> vehicle.setConditionYaw(targetAngle, turnRate, isRelative, vehicleListener), listener);
    }

    public void manualControl(final int x, final int y, final int z, final int r, final int buttons, final AbstractCommandListener listener) {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.manualControl(x, y, z, r, buttons, vehicleListener), listener);
    }

    /**
     * [Dis|En]able manual control on the vehicle.
     * The result of the action will be conveyed through the passed listener.
     *
     * @param enable   True to enable manual control, false to disable.
     * @param listener Register a callback to receive the result of the operation.
     * @since 2.6.9
     */
    public void enableManualControl(final boolean enable, final ManualControlStateListener listener) {
        AbstractCommandListener listenerWrapper = listener == null ? null
                : new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                listener.onManualControlToggled(enable);
            }

            @Override
            public void onError(int executionError) {
                if (enable) {
                    listener.onManualControlToggled(false);
                }
            }

            @Override
            public void onTimeout() {
                if (enable) {
                    listener.onManualControlToggled(false);
                }
            }
        };

        drone.executeCommand((vehicle, vehicleListener) -> vehicle.enableManualControl(enable, vehicleListener), listenerWrapper);
    }


    /*
     *   reset region of interest so yaw will follow default mode again
     */
    public void reset_roi (final AbstractCommandListener listener){
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.resetROI(vehicleListener), listener);
    }

    private static boolean isWithinBounds(float value, float lowerBound, float upperBound) {
        return value <= upperBound && value >= lowerBound;
    }

    /**
     * Used to monitor the state of manual control for the vehicle.
     *
     * @since 2.6.9
     */
    public interface ManualControlStateListener {
        /**
         * Manual control is toggled on the vehicle.
         * @param isEnabled True if manual control is enabled, false if disabled.
         */
        void onManualControlToggled(boolean isEnabled);
    }
}
