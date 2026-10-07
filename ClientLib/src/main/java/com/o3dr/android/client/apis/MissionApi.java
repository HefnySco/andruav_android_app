package com.o3dr.android.client.apis;


import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.model.AbstractCommandListener;

import java.util.concurrent.ConcurrentHashMap;


/**
 * Provides access to missions specific functionality.
 * Created by Fredia Huya-Kouadio on 1/19/15.
 */
public class MissionApi extends Api {

    private static final ConcurrentHashMap<Drone, MissionApi> missionApiCache = new ConcurrentHashMap<>();
    private static final Builder<MissionApi> apiBuilder = new Builder<MissionApi>() {
        @Override
        public MissionApi build(Drone drone) {
            return new MissionApi(drone);
        }
    };

    /**
     * Retrieves a MissionApi instance.
     * @param drone Target vehicle
     * @return a MissionApi instance.
     */
    public static MissionApi getApi(final Drone drone) {
        return getApi(drone, missionApiCache, apiBuilder);
    }

    private final Drone drone;

    private MissionApi(Drone drone){
        this.drone = drone;
    }

    /**
     * Generate action to update the mission property for the drone model in memory.
     *
     * @param mission     mission to upload to the drone.
     * @param pushToDrone if true, upload the mission to the connected device.
     */
    public void setMission(Mission mission, boolean pushToDrone) {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.setMission(mission, pushToDrone), null);
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
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.startMission(forceModeChange, forceArm, vehicleListener), listener);
    }

    /**
     * Load waypoints from the target vehicle.
     */
    public void loadWaypoints() {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.loadWaypoints(), null);
    }

    /**
     * Sets the mission to a specified speed
     * @param speed Speed to set mission in m/s
     * @param listener
     *
     * @since 2.8.0
     */
    public void setMissionSpeed(float speed, AbstractCommandListener listener) {
        drone.executeCommand((vehicle, vehicleListener) -> vehicle.changeMissionSpeed(speed, vehicleListener), listener);
    }
}
