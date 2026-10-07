package com.o3dr.android.client;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;
import androidx.annotation.NonNull;

import com.MAVLink.enums.MAV_MOUNT_MODE;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.ManualControlStateListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeEventExtra;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.attribute.error.CommandExecutionError;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Parameter;
import com.o3dr.services.android.lib.drone.property.Parameters;
import com.o3dr.services.android.lib.drone.property.Signal;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.gcs.link.LinkEvent;
import com.o3dr.services.android.lib.gcs.link.LinkEventExtra;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;
import com.o3dr.services.android.lib.model.ICommandListener;
import com.o3dr.services.android.lib.model.IObserver;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import org.droidplanner.services.android.impl.api.DroneApi;
import org.droidplanner.services.android.impl.core.drone.DroneManager;
import org.droidplanner.services.android.impl.core.drone.autopilot.MavLinkDrone;
import org.droidplanner.services.android.impl.utils.CommonApiUtils;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by fhuya on 11/4/14.
 */
public class Drone {
    private static final String CLAZZ_NAME = Drone.class.getName();
    private static final String TAG = Drone.class.getSimpleName();

    public interface OnAttributeRetrievedCallback<T extends Parcelable> {
        void onRetrievalSucceed(T attribute);

        void onRetrievalFailed();
    }

    public static class AttributeRetrievedListener<T extends Parcelable> implements OnAttributeRetrievedCallback<T> {

        @Override
        public void onRetrievalSucceed(T attribute) {
        }

        @Override
        public void onRetrievalFailed() {
        }
    }

    public interface GimbalOrientationListener {
        /**
         * Called when the gimbal orientation is updated.
         * @param orientation GimbalOrientation object
         */
        void onGimbalOrientationUpdate(GimbalOrientation orientation);

        /**
         * Indicates errors occurring from attempting to set the gimbal orientation.
         * @param error @see {@link com.o3dr.services.android.lib.drone.attribute.error.CommandExecutionError}
         */
        void onGimbalOrientationCommandError(int error);
    }

    /**
     * Stores the gimbal orientation angles.
     */
    public static class GimbalOrientation {
        private float pitch;
        private float roll;
        private float yaw;

        public float getPitch() {
            return pitch;
        }

        public float getRoll() {
            return roll;
        }

        public float getYaw() {
            return yaw;
        }

        private void updateOrientation(float pitch, float roll, float yaw) {
            this.pitch = pitch;
            this.roll = roll;
            this.yaw = yaw;
        }

        private GimbalOrientation(){}

        private GimbalOrientation(GimbalOrientation source){
            this.pitch = source.pitch;
            this.roll = source.roll;
            this.yaw = source.yaw;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GimbalOrientation)) return false;

            GimbalOrientation that = (GimbalOrientation) o;

            if (Float.compare(that.pitch, pitch) != 0) return false;
            if (Float.compare(that.roll, roll) != 0) return false;
            return Float.compare(that.yaw, yaw) == 0;

        }

        @Override
        public int hashCode() {
            int result = (pitch != +0.0f ? Float.floatToIntBits(pitch) : 0);
            result = 31 * result + (roll != +0.0f ? Float.floatToIntBits(roll) : 0);
            result = 31 * result + (yaw != +0.0f ? Float.floatToIntBits(yaw) : 0);
            return result;
        }

        @Override
        public String toString() {
            return "GimbalOrientation{" +
                    "pitch=" + pitch +
                    ", roll=" + roll +
                    ", yaw=" + yaw +
                    '}';
        }
    }

    /**
     * A command to run against the live vehicle.
     */
    private interface VehicleCommand {
        void execute(MavLinkDrone vehicle, ICommandListener listener);
    }

    public static final int COLLISION_SECONDS_BEFORE_COLLISION = 2;
    public static final double COLLISION_DANGEROUS_SPEED_METERS_PER_SECOND = -3.0;
    public static final double COLLISION_SAFE_ALTITUDE_METERS = 1.0;

    public static final String ACTION_GROUND_COLLISION_IMMINENT = CLAZZ_NAME + ".ACTION_GROUND_COLLISION_IMMINENT";
    public static final String EXTRA_IS_GROUND_COLLISION_IMMINENT = "extra_is_ground_collision_imminent";

    private final ConcurrentLinkedQueue<DroneListener> droneListeners = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<GimbalOrientationListener> gimbalListeners = new ConcurrentLinkedQueue<>();
    private final GimbalOrientation gimbalOrientation = new GimbalOrientation();
    private final DroneListener gimbalEventListener = new DroneListener() {
        @Override
        public void onDroneEvent(String event, Bundle extras) {
            if (AttributeEvent.GIMBAL_ORIENTATION_UPDATED.equals(event)) {
                final float pitch = extras.getFloat(AttributeEventExtra.EXTRA_GIMBAL_ORIENTATION_PITCH);
                final float roll = extras.getFloat(AttributeEventExtra.EXTRA_GIMBAL_ORIENTATION_ROLL);
                final float yaw = extras.getFloat(AttributeEventExtra.EXTRA_GIMBAL_ORIENTATION_YAW);
                gimbalOrientation.updateOrientation(pitch, roll, yaw);
                notifyGimbalOrientationUpdated(gimbalOrientation);
            }
        }

        @Override
        public void onDroneServiceInterrupted(String errorMsg) {

        }
    };

    private Handler handler;
    private ControlTower serviceMgr;
    private DroneObserver droneObserver;

    private final AtomicReference<DroneApi> droneApiRef = new AtomicReference<>(null);
    private ConnectionParameter connectionParameter;
    private LinkListener linkListener;
    private ExecutorService asyncScheduler;

    // flightTimer
    // ----------------
    private long startTime = 0;
    private long elapsedFlightTime = 0;

    private final Context context;
    private final ClassLoader contextClassLoader;

    /**
     * Creates a Drone instance.
     *
     * @param context Application context
     */
    public Drone(Context context) {
        this.context = context;
        this.contextClassLoader = context.getClassLoader();
    }

    void init(ControlTower controlTower, Handler handler) {
        this.handler = handler;
        this.serviceMgr = controlTower;
        this.droneObserver = new DroneObserver(this);
    }

    Context getContext() {
        return this.context;
    }

    synchronized void start() {
        if (!serviceMgr.isTowerConnected()) {
            throw new IllegalStateException("Service manager must be connected.");
        }

        DroneApi droneApi = droneApiRef.get();
        if (isStarted(droneApi)) {
            return;
        }

        droneApi = serviceMgr.registerDroneApi();
        if (droneApi == null) {
            throw new IllegalStateException("Unable to retrieve a valid drone handle.");
        }

        if (asyncScheduler == null || asyncScheduler.isShutdown()) {
            asyncScheduler = Executors.newFixedThreadPool(1);
        }

        addAttributesObserver(droneApi, this.droneObserver);
        resetFlightTimer();

        droneApiRef.set(droneApi);
    }

    synchronized void destroy() {
        DroneApi droneApi = droneApiRef.get();

        removeAttributesObserver(droneApi, this.droneObserver);

        if (isStarted(droneApi)) {
            serviceMgr.releaseDroneApi();
        }

        if (asyncScheduler != null) {
            asyncScheduler.shutdownNow();
            asyncScheduler = null;
        }

        droneApiRef.set(null);
    }

    private void checkForGroundCollision() {
        Speed speed = getAttribute(AttributeType.SPEED);
        Altitude altitude = getAttribute(AttributeType.ALTITUDE);
        if (speed == null || altitude == null) {
            return;
        }

        double verticalSpeed = speed.getVerticalSpeed();
        double altitudeValue = altitude.getAltitude();

        boolean isCollisionImminent = altitudeValue
            + (verticalSpeed * COLLISION_SECONDS_BEFORE_COLLISION) < 0
            && verticalSpeed < COLLISION_DANGEROUS_SPEED_METERS_PER_SECOND
            && altitudeValue > COLLISION_SAFE_ALTITUDE_METERS;

        Bundle extrasBundle = new Bundle(1);
        extrasBundle.putBoolean(EXTRA_IS_GROUND_COLLISION_IMMINENT, isCollisionImminent);
        notifyAttributeUpdated(ACTION_GROUND_COLLISION_IMMINENT, extrasBundle);
    }

    public double getSpeedParameter() {
        Parameters params = getAttribute(AttributeType.PARAMETERS);
        if (params != null) {
            Parameter speedParam = params.getParameter("WPNAV_SPEED");
            if (speedParam != null) {
                return speedParam.getValue();
            }
        }

        return 0;
    }

    /**
     * Causes the Runnable to be added to the message queue.
     *
     * @param action Runnabl that will be executed.
     */
    public void post(Runnable action) {
        if (handler == null || action == null) {
            return;
        }

        handler.post(action);
    }

    /**
     * Reset the vehicle flight timer.
     */
    public void resetFlightTimer() {
        elapsedFlightTime = 0;
        startTime = SystemClock.elapsedRealtime();
    }

    private void stopTimer() {
        // lets calc the final elapsed timer
        elapsedFlightTime += SystemClock.elapsedRealtime() - startTime;
        startTime = SystemClock.elapsedRealtime();
    }

    /**
     * @return Vehicle flight time in seconds.
     */
    public long getFlightTime() {
        State droneState = getAttribute(AttributeType.STATE);
        if (droneState != null && droneState.isFlying()) {
            // calc delta time since last checked
            elapsedFlightTime += SystemClock.elapsedRealtime() - startTime;
            startTime = SystemClock.elapsedRealtime();
        }
        return elapsedFlightTime / 1000;
    }

    public <T extends Parcelable> T getAttribute(String type) {
        final DroneApi droneApi = droneApiRef.get();
        if (!isStarted(droneApi) || type == null) {
            return this.getAttributeDefaultValue(type);
        }

        @SuppressWarnings("unchecked")
        T attribute = (T) droneApi.getAttribute(type);

        return attribute == null ? this.getAttributeDefaultValue(type) : attribute;
    }

    public <T extends Parcelable> void getAttributeAsync(final String attributeType,
                                                         final OnAttributeRetrievedCallback<T> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback must be non-null.");
        }

        final DroneApi droneApi = droneApiRef.get();
        if (!isStarted(droneApi)) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onRetrievalFailed();
                }
            });
            return;
        }

        asyncScheduler.execute(new Runnable() {
            @Override
            public void run() {
                final T attribute = getAttribute(attributeType);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (attribute == null) {
                            callback.onRetrievalFailed();
                        } else {
                            callback.onRetrievalSucceed(attribute);
                        }
                    }
                });
            }
        });
    }

    private <T extends Parcelable> T getAttributeDefaultValue(String attributeType) {
        if (attributeType == null) {
            return null;
        }

        switch (attributeType) {
            case AttributeType.ALTITUDE:
                return (T) new Altitude();

            case AttributeType.GPS:
                return (T) new Gps();

            case AttributeType.STATE:
                return (T) new State();

            case AttributeType.PARAMETERS:
                return (T) new Parameters();

            case AttributeType.SPEED:
                return (T) new Speed();

            case AttributeType.ATTITUDE:
                return (T) new Attitude();

            case AttributeType.HOME:
                return (T) new Home();

            case AttributeType.BATTERY:
                return (T) new Battery();

            case AttributeType.MISSION:
                return (T) new Mission();

            case AttributeType.SIGNAL:
                return (T) new Signal();

            case AttributeType.GUIDED_STATE:
                return (T) new GuidedState();

            case AttributeType.TYPE:
                return (T) new Type();



            default:
                return null;
        }
    }

    /**
     * Connect to a vehicle using a specified {@link ConnectionParameter}.
     *
     * @param connParams Specified parameters to determine how to connect the vehicle.
     */
    public void connect(final ConnectionParameter connParams) {
        connect(connParams, null);
    }

    /**
     * Connect to a vehicle using a specified {@link ConnectionParameter} and a {@link LinkListener}
     * callback.
     *
     * @param connParams Specified parameters to determine how to connect the vehicle.
     * @param linkListener A callback that will update the caller on the state of the link connection.
     */
    public void connect(ConnectionParameter connParams, LinkListener linkListener) {
        final DroneApi droneApi = droneApiRef.get();
        if (isStarted(droneApi)) {
            droneApi.connect(connParams);
        }
        this.connectionParameter = connParams;
        this.linkListener = linkListener;
    }

    /**
     * Disconnect from the vehicle.
     */
    public void disconnect() {
        final DroneApi droneApi = droneApiRef.get();
        if (isStarted(droneApi)) {
            droneApi.disconnect();
        }
        this.connectionParameter = null;
        this.linkListener = null;
    }

    private static AbstractCommandListener wrapListener(final Handler handler, final AbstractCommandListener listener) {
        AbstractCommandListener wrapperListener = listener;
        if (handler != null && listener != null) {
            wrapperListener = new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onSuccess();
                        }
                    });
                }

                @Override
                public void onError(final int executionError) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onError(executionError);
                        }
                    });
                }

                @Override
                public void onTimeout() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onTimeout();
                        }
                    });
                }
            };
        }

        return wrapperListener;
    }

    /**
     * Runs a command against the live vehicle. The listener is wrapped so its callbacks are posted
     * on the drone handler.
     *
     * @param command  Command to run.
     * @param listener Receives the result of the command. Can be null.
     * @return false if the drone is not started, in which case the command is not run and the
     * listener is never called.
     */
    private boolean executeCommand(VehicleCommand command, AbstractCommandListener listener) {
        final DroneApi droneApi = droneApiRef.get();
        if (!isStarted(droneApi)) {
            return false;
        }

        final ICommandListener wrappedListener = wrapListener(this.handler, listener);
        final MavLinkDrone vehicle = getVehicle(droneApi);
        if (vehicle == null) {
            CommonApiUtils.postErrorEvent(CommandExecutionError.COMMAND_FAILED, wrappedListener);
        } else {
            command.execute(vehicle, wrappedListener);
        }
        return true;
    }

    private MavLinkDrone getVehicle(DroneApi droneApi) {
        final DroneManager droneMgr = droneApi.getDroneManager();
        if (droneMgr == null) {
            return null;
        }

        final Object vehicle = droneMgr.getDrone();
        return vehicle instanceof MavLinkDrone ? (MavLinkDrone) vehicle : null;
    }

    /**
     * Arm or disarm the connected drone.
     *
     * @param arm true to arm, false to disarm.
     */
    public void arm(boolean arm) {
        arm(arm, null);
    }

    /**
     * Arm or disarm the connected drone.
     *
     * @param arm      true to arm, false to disarm.
     * @param listener Register a callback to receive update of the command execution state.
     */
    public void arm(boolean arm, AbstractCommandListener listener) {
        arm(arm, false, listener);
    }

    /**
     * Arm or disarm the connected drone.
     *
     * @param arm             true to arm, false to disarm.
     * @param emergencyDisarm true to skip landing check and disarm immediately,
     *                        false to disarm only if it is safe to do so.
     * @param listener        Register a callback to receive update of the command execution state.
     */
    public void arm(boolean arm, boolean emergencyDisarm, AbstractCommandListener listener) {
        executeCommand((vehicle, vehicleListener) -> vehicle.arm(arm, emergencyDisarm, vehicleListener), listener);
    }

    /**
     * Change the vehicle mode for the connected drone.
     *
     * @param newMode new vehicle mode.
     */
    public void setVehicleMode(VehicleMode newMode) {
        setVehicleMode(newMode, null);
    }

    /**
     * Change the vehicle mode for the connected drone.
     *
     * @param newMode  new vehicle mode.
     * @param listener Register a callback to receive update of the command execution state.
     */
    public void setVehicleMode(VehicleMode newMode, AbstractCommandListener listener) {
        executeCommand((vehicle, vehicleListener) -> vehicle.setVehicleMode(newMode, vehicleListener), listener);
    }

    /**
     * Generate action used to refresh the parameters for the connected drone.
     */
    public void refreshParameters() {
        executeCommand((vehicle, vehicleListener) -> vehicle.refreshParameters(), null);
    }

    /**
     * Generate action used to write the given parameters to the connected drone.
     *
     * @param parameters parameters to write to the drone.
     * @return
     */
    public void writeParameters(Parameters parameters) {
        executeCommand((vehicle, vehicleListener) -> vehicle.writeParameters(parameters), null);
    }

    /**
     * Changes the vehicle home location.
     *
     * @param homeLocation New home coordinate
     * @param listener     Register a callback to receive update of the command execution state.
     */
    public void setVehicleHome(final LatLongAlt homeLocation, final AbstractCommandListener listener) {
        executeCommand((vehicle, vehicleListener) -> vehicle.setVehicleHome(homeLocation, vehicleListener), listener);
    }

    /**
     * Perform a guided take off.
     *
     * @param altitude altitude in meters
     * @param listener Register a callback to receive update of the command execution state.
     */
    public void takeoff(double altitude, AbstractCommandListener listener) {
        executeCommand((vehicle, vehicleListener) -> vehicle.takeoff(altitude, vehicleListener), listener);
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
        executeCommand((vehicle, vehicleListener) -> vehicle.sendGuidedVelocityInLocalFrame((float) vx, (float) vy, (float) vz,
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
        executeCommand((vehicle, vehicleListener) -> vehicle.sendGuidedVelocityInGlobalFrame((float) vx, (float) vy, (float) vz,
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
        executeCommand((vehicle, vehicleListener) -> vehicle.sendGuidedPoint(point, force, vehicleListener), listener);
    }

    /**
     * Instructs the vehicle to climb to the specified altitude.
     *
     * @param altitude altitude in meters
     */
    public void climbTo(double altitude) {
        executeCommand((vehicle, vehicleListener) -> vehicle.setGuidedAltitude(altitude), null);
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
            if (listener != null) {
                listener.onError(CommandExecutionError.COMMAND_FAILED);
            }
            return;
        }

        executeCommand((vehicle, vehicleListener) -> vehicle.setConditionYaw(targetAngle, turnRate, isRelative, vehicleListener), listener);
    }

    public void manualControl(final int x, final int y, final int z, final int r, final int buttons, final AbstractCommandListener listener) {
        executeCommand((vehicle, vehicleListener) -> vehicle.manualControl(x, y, z, r, buttons, vehicleListener), listener);
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

        executeCommand((vehicle, vehicleListener) -> vehicle.enableManualControl(enable, vehicleListener), listenerWrapper);
    }


    /*
     *   reset region of interest so yaw will follow default mode again
     */
    public void reset_roi (final AbstractCommandListener listener){
        executeCommand((vehicle, vehicleListener) -> vehicle.resetROI(vehicleListener), listener);
    }

    private static boolean isWithinBounds(float value, float lowerBound, float upperBound) {
        return value <= upperBound && value >= lowerBound;
    }

    /**
     * Generate action to update the mission property for the drone model in memory.
     *
     * @param mission     mission to upload to the drone.
     * @param pushToDrone if true, upload the mission to the connected device.
     */
    public void setMission(Mission mission, boolean pushToDrone) {
        executeCommand((vehicle, vehicleListener) -> vehicle.setMission(mission, pushToDrone), null);
    }

    /**
     * Starts the mission. The vehicle will only accept this command if armed and in Auto mode.
     * note: This command is only supported by APM:Copter V3.3 and newer.
     *
     * @param forceModeChange Change to Auto mode if not in Auto.
     * @param forceArm Arm the vehicle if it is disarmed.
     * @param listener
     */
    public void startMission(boolean forceModeChange, boolean forceArm, AbstractCommandListener listener){
        executeCommand((vehicle, vehicleListener) -> vehicle.startMission(forceModeChange, forceArm, vehicleListener), listener);
    }

    /**
     * Load waypoints from the target vehicle.
     */
    public void loadWaypoints() {
        executeCommand((vehicle, vehicleListener) -> vehicle.loadWaypoints(), null);
    }

    /**
     * Sets the mission to a specified speed
     * @param speed Speed to set mission in m/s
     * @param listener
     *
     * @since 2.8.0
     */
    public void setMissionSpeed(float speed, AbstractCommandListener listener) {
        executeCommand((vehicle, vehicleListener) -> vehicle.changeMissionSpeed(speed, vehicleListener), listener);
    }

    /**
     * Triggers the camera.
     */
    public void triggerCamera() {
        executeCommand((vehicle, vehicleListener) -> vehicle.triggerCamera(), null);
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
        executeCommand((vehicle, vehicleListener) -> vehicle.sendMavlinkMessage(messageWrapper), null);
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
        executeCommand((vehicle, vehicleListener) -> vehicle.setServo(channel, pwm, vehicleListener), listener);
    }

    /**
     * Enables control of the gimbal. After calling this method, use {@link Drone#updateGimbalOrientation(float, float, float, GimbalOrientationListener)}
     * to update the gimbal orientation.
     * @param listener non-null GimbalStatusListener callback.
     */
    public void startGimbalControl(final GimbalOrientationListener listener){
        registerDroneListener(gimbalEventListener);

        if(listener == null)
            throw new NullPointerException("Listener can't be null.");

        final Type vehicleType = this.getAttribute(AttributeType.TYPE);
        if(vehicleType.getDroneType() != Type.TYPE_COPTER){
            post(new Runnable() {
                @Override
                public void run() {
                    listener.onGimbalOrientationCommandError(CommandExecutionError.COMMAND_UNSUPPORTED);
                }
            });
            return;
        }

        gimbalListeners.add(listener);

        configureGimbalMountMode(listener);
    }

    private void configureGimbalMountMode(final GimbalOrientationListener listener){
        executeCommand((vehicle, vehicleListener) -> vehicle.setGimbalMountMode(MAV_MOUNT_MODE.MAV_MOUNT_MODE_MAVLINK_TARGETING, vehicleListener), new SimpleCommandListener() {
            @Override
            public void onTimeout() {
                listener.onGimbalOrientationCommandError(CommandExecutionError.COMMAND_FAILED);
            }

            @Override
            public void onError(int error) {
                listener.onGimbalOrientationCommandError(error);
            }
        });
    }

    /**
     * Set the orientation of the gimbal
     * @param orientation Desired orientation values.
     * @param listener Register a callback to receive update of the command execution state. Must be non-null.
     * @since 2.8.0
     */
    public void updateGimbalOrientation(GimbalOrientation orientation, @NonNull final GimbalOrientationListener listener) {
        updateGimbalOrientation(orientation.pitch, orientation.roll, orientation.yaw, listener);
    }

    /**
     * Set the orientation of a gimbal
     *
     * @param pitch       the desired gimbal pitch in degrees. 0 is straight forwards, -90 is straight down
     * @param roll       the desired gimbal roll in degrees
     * @param yaw       the desired gimbal yaw in degrees
     * @param listener Register a callback to receive update of the command execution state. Must be non-null.
     * @since 2.5.0
     */
    public void updateGimbalOrientation(float pitch, float roll, float yaw, @NonNull final GimbalOrientationListener listener){
        registerDroneListener(gimbalEventListener);

        if(listener == null)
            throw new NullPointerException("Listener must be non-null.");

        if(!gimbalListeners.contains(listener)){
            post(new Runnable() {
                @Override
                public void run() {
                    listener.onGimbalOrientationCommandError(CommandExecutionError.COMMAND_DENIED);
                }
            });
            return;
        }

        executeCommand((vehicle, vehicleListener) -> vehicle.setGimbalOrientation(pitch, roll, yaw, vehicleListener), new SimpleCommandListener(){
            @Override
            public void onTimeout(){
                listener.onGimbalOrientationCommandError(CommandExecutionError.COMMAND_FAILED);
            }

            @Override
            public void onError(int error){
                listener.onGimbalOrientationCommandError(error);
            }
        });
    }

    private void notifyGimbalOrientationUpdated(GimbalOrientation orientation){
        if(gimbalListeners.isEmpty())
            return;

        for(GimbalOrientationListener listener: gimbalListeners){
            listener.onGimbalOrientationUpdate(orientation);
        }
    }

    private boolean isStarted(DroneApi droneApi) {
        return droneApi != null;
    }

    public boolean isStarted() {
        return isStarted(droneApiRef.get());
    }

    public boolean isConnected() {
        final DroneApi droneApi = droneApiRef.get();
        State droneState = getAttribute(AttributeType.STATE);
        return isStarted(droneApi) && droneState.isConnected();
    }

    public ConnectionParameter getConnectionParameter() {
        return this.connectionParameter;
    }

    public void registerDroneListener(DroneListener listener) {
        if (listener == null) {
            return;
        }

        if (!droneListeners.contains(listener)) {
            droneListeners.add(listener);
        }
    }

    private void addAttributesObserver(DroneApi droneApi, IObserver observer) {
        if (isStarted(droneApi)) {
            droneApi.addAttributesObserver(observer);
        }
    }

    public void addMavlinkObserver(MavlinkObserver observer) {
        final DroneApi droneApi = droneApiRef.get();
        if (isStarted(droneApi)) {
            droneApi.addMavlinkObserver(observer);
        }
    }

    public void removeMavlinkObserver(MavlinkObserver observer) {
        final DroneApi droneApi = droneApiRef.get();
        if (isStarted(droneApi)) {
            droneApi.removeMavlinkObserver(observer);
        }
    }

    public void unregisterDroneListener(DroneListener listener) {
        if (listener == null) {
            return;
        }

        droneListeners.remove(listener);
    }

    private void removeAttributesObserver(DroneApi droneApi, IObserver observer) {
        if (isStarted(droneApi)) {
            droneApi.removeAttributesObserver(observer);
        }
    }

    public Handler getHandler() {
        return handler;
    }

    public ExecutorService getAsyncScheduler(){
        return asyncScheduler;
    }

    void notifyAttributeUpdated(final String attributeEvent, final Bundle extras) {
        //Update the bund
        // le classloader
        if (extras != null) {
            extras.setClassLoader(contextClassLoader);
        }

        switch (attributeEvent) {
            case AttributeEvent.STATE_UPDATED:
                getAttributeAsync(AttributeType.STATE, new OnAttributeRetrievedCallback<State>() {
                    @Override
                    public void onRetrievalSucceed(State state) {
                        if (state.isFlying()) {
                            resetFlightTimer();
                        } else {
                            stopTimer();
                        }
                    }

                    @Override
                    public void onRetrievalFailed() {
                        stopTimer();
                    }
                });
                break;

            case AttributeEvent.SPEED_UPDATED:
                checkForGroundCollision();
                break;

            case LinkEvent.LINK_STATE_UPDATED:
                sendLinkEventToListener(extras);
                return;
        }

        sendDroneEventToListeners(attributeEvent, extras);
    }

    private void sendDroneEventToListeners(final String attributeEvent, final Bundle extras) {
        if (droneListeners.isEmpty()) {
            return;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (DroneListener listener : droneListeners) {
                    try {
                        listener.onDroneEvent(attributeEvent, extras);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            }
        });
    }

    private void sendLinkEventToListener(Bundle extras) {
        if (linkListener == null) {
            return;
        }

        if (extras != null) {
            final LinkConnectionStatus status = extras.getParcelable(LinkEventExtra.EXTRA_CONNECTION_STATUS);
            if (status != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        linkListener.onLinkStateUpdated(status);
                    }
                });
            }
        }
    }

    void notifyDroneServiceInterrupted(final String errorMsg) {
        if (droneListeners.isEmpty()) {
            return;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (DroneListener listener : droneListeners)
                    listener.onDroneServiceInterrupted(errorMsg);
            }
        });
    }
}
