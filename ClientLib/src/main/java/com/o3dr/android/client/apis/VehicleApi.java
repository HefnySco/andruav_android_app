package com.o3dr.android.client.apis;


import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Parameters;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.model.AbstractCommandListener;

import java.util.concurrent.ConcurrentHashMap;


/**
 * Provides access to the vehicle specific functionality.
 */
public class VehicleApi extends Api {

    private static final ConcurrentHashMap<Drone, VehicleApi> vehicleApiCache = new ConcurrentHashMap<>();
    private static final Builder<VehicleApi> apiBuilder = new Builder<VehicleApi>() {
        @Override
        public VehicleApi build(Drone drone) {
            return new VehicleApi(drone);
        }
    };

    /**
     * Retrieves a vehicle api instance.
     *
     * @param drone target vehicle
     * @return a VehicleApi instance.
     */
    public static VehicleApi getApi(final Drone drone) {
        return getApi(drone, vehicleApiCache, apiBuilder);
    }

    private final Drone drone;

    private VehicleApi(Drone drone) {
        this.drone = drone;
    }

    /**
     * Establish connection with the vehicle.
     *
     * @param parameter parameter for the connection.
     */
    public void connect(ConnectionParameter parameter) {
        drone.connect(parameter);
    }

    /**
     * Break connection with the vehicle.
     */
    public void disconnect() {
        drone.disconnect();
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
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.arm(arm, emergencyDisarm, vehicleListener), listener);
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
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.setVehicleMode(newMode, vehicleListener), listener);
    }

    /**
     * Generate action used to refresh the parameters for the connected drone.
     */
    public void refreshParameters() {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.refreshParameters(), null);
    }

    /**
     * Generate action used to write the given parameters to the connected drone.
     *
     * @param parameters parameters to write to the drone.
     * @return
     */
    public void writeParameters(Parameters parameters) {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.writeParameters(parameters), null);
    }

    /**
     * Changes the vehicle home location.
     *
     * @param homeLocation New home coordinate
     * @param listener     Register a callback to receive update of the command execution state.
     */
    public void setVehicleHome(final LatLongAlt homeLocation, final AbstractCommandListener listener) {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.setVehicleHome(homeLocation, vehicleListener), listener);
    }

}
