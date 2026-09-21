package com.o3dr.android.client.apis;

import android.os.Bundle;

import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.action.Action;

import java.util.concurrent.ConcurrentHashMap;

import static com.o3dr.services.android.lib.drone.mission.action.MissionActions.ACTION_CHANGE_MISSION_SPEED;
import static com.o3dr.services.android.lib.drone.mission.action.MissionActions.ACTION_GOTO_WAYPOINT;
import static com.o3dr.services.android.lib.drone.mission.action.MissionActions.ACTION_LOAD_WAYPOINTS;
import static com.o3dr.services.android.lib.drone.mission.action.MissionActions.ACTION_SET_MISSION;
import static com.o3dr.services.android.lib.drone.mission.action.MissionActions.ACTION_START_MISSION;
import static com.o3dr.services.android.lib.drone.mission.action.MissionActions.EXTRA_FORCE_ARM;
import static com.o3dr.services.android.lib.drone.mission.action.MissionActions.EXTRA_FORCE_MODE_CHANGE;
import static com.o3dr.services.android.lib.drone.mission.action.MissionActions.EXTRA_MISSION;
import static com.o3dr.services.android.lib.drone.mission.action.MissionActions.EXTRA_MISSION_ITEM_INDEX;
import static com.o3dr.services.android.lib.drone.mission.action.MissionActions.EXTRA_MISSION_SPEED;
import static com.o3dr.services.android.lib.drone.mission.action.MissionActions.EXTRA_PUSH_TO_DRONE;

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
        Bundle params = new Bundle();
        params.putParcelable(EXTRA_MISSION, mission);
        params.putBoolean(EXTRA_PUSH_TO_DRONE, pushToDrone);
        drone.performAsyncAction(new Action(ACTION_SET_MISSION, params));
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
        Bundle params = new Bundle();
        params.putBoolean(EXTRA_FORCE_MODE_CHANGE, forceModeChange);
        params.putBoolean(EXTRA_FORCE_ARM, forceArm);
        drone.performAsyncActionOnDroneThread(new Action(ACTION_START_MISSION, params), listener);
    }

    /**
     * Jump to the desired command in the mission list. Repeat this action only the specified number of times
     * @param waypoint command to jump to
     * @param listener
     */
    public void gotoWaypoint(int waypoint, AbstractCommandListener listener){
        Bundle params = new Bundle();
        params.putInt(EXTRA_MISSION_ITEM_INDEX, waypoint);
        drone.performAsyncActionOnDroneThread(new Action(ACTION_GOTO_WAYPOINT, params), listener);
    }

    /**
     * Load waypoints from the target vehicle.
     */
    public void loadWaypoints() {
        drone.performAsyncAction(new Action(ACTION_LOAD_WAYPOINTS));
    }

    /**
     * Stops the vehicle at the current location. The vehicle will remain in Auto mode
     * @param listener
     *
     * @since 2.8.0
     */
    public void pauseMission(AbstractCommandListener listener) {
        Bundle params = new Bundle();
        params.putFloat(EXTRA_MISSION_SPEED, 0);
        drone.performAsyncActionOnDroneThread(new Action(ACTION_CHANGE_MISSION_SPEED, params), listener);
    }

    /**
     * Sets the mission to a specified speed
     * @param speed Speed to set mission in m/s
     * @param listener
     *
     * @since 2.8.0
     */
    public void setMissionSpeed(float speed, AbstractCommandListener listener) {
        Bundle params = new Bundle();
        params.putFloat(EXTRA_MISSION_SPEED, speed);
        drone.performAsyncActionOnDroneThread(new Action(ACTION_CHANGE_MISSION_SPEED, params), listener);
    }
}
