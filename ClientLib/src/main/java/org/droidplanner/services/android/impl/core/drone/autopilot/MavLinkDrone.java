package org.droidplanner.services.android.impl.core.drone.autopilot;

import com.MAVLink.Messages.MAVLinkMessage;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.property.Parameters;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;
import com.o3dr.services.android.lib.model.ICommandListener;

import org.droidplanner.services.android.impl.communication.model.DataLink;
import org.droidplanner.services.android.impl.core.MAVLink.WaypointManager;
import org.droidplanner.services.android.impl.core.drone.profiles.ParameterManager;
import org.droidplanner.services.android.impl.core.drone.variables.GuidedPoint;
import org.droidplanner.services.android.impl.core.drone.variables.MissionStats;
import org.droidplanner.services.android.impl.core.drone.variables.State;
import org.droidplanner.services.android.impl.core.drone.variables.StreamRates;
import org.droidplanner.services.android.impl.core.firmware.FirmwareType;
import org.droidplanner.services.android.impl.core.mission.MissionImpl;

public interface MavLinkDrone extends Drone {

    boolean isConnectionAlive();

    int getMavlinkVersion();

    void onMavLinkMessageReceived(MAVLinkMessage message);

    short getSysid();

    short getCompid();

    State getState();

    ParameterManager getParameterManager();

    int getType();

    FirmwareType getFirmwareType();

    DataLink.DataLinkProvider<MAVLinkMessage> getMavClient();

    WaypointManager getWaypointManager();

    MissionImpl getMission();

    StreamRates getStreamRates();

    MissionStats getMissionStats();

    GuidedPoint getGuidedPoint();

    String getFirmwareVersion();

    //************ Commands ************//
    // Commands the autopilot doesn't support post CommandExecutionError.COMMAND_UNSUPPORTED to the
    // listener (when there is one).

    // MISSION COMMANDS
    void changeMissionSpeed(float speed, ICommandListener listener);

    void loadWaypoints();

    void setMission(Mission mission, boolean pushToDrone);

    void startMission(boolean forceModeChange, boolean forceArm, ICommandListener listener);

    // STATE COMMANDS
    void arm(boolean arm, boolean emergencyDisarm, ICommandListener listener);

    void setVehicleMode(VehicleMode newMode, ICommandListener listener);

    void setVehicleHome(LatLongAlt homeLocation, ICommandListener listener);

    // PARAMETER COMMANDS
    void refreshParameters();

    void writeParameters(Parameters parameters);

    // CONTROL COMMANDS
    void takeoff(double altitude, ICommandListener listener);

    void setConditionYaw(float targetAngle, float yawRate, boolean isRelative, ICommandListener listener);

    void enableManualControl(boolean enable, ICommandListener listener);

    void manualControl(int x, int y, int z, int r, int buttons, ICommandListener listener);

    void resetROI(ICommandListener listener);

    void sendGuidedPoint(LatLong point, boolean force, ICommandListener listener);

    void sendGuidedVelocityInLocalFrame(float vx, float vy, float vz, float yawRate, float yaw,
                                        short coordinateFrame, short typeMask, ICommandListener listener);

    void sendGuidedVelocityInGlobalFrame(float vx, float vy, float vz, float yawRate, float yaw,
                                         short coordinateFrame, short typeMask, ICommandListener listener);

    void setGuidedAltitude(double altitude);

    // GIMBAL COMMANDS
    void setGimbalOrientation(float pitch, float roll, float yaw, ICommandListener listener);

    void setGimbalMountMode(int mountMode, ICommandListener listener);

    void resetGimbalMountMode(ICommandListener listener);

    // EXPERIMENTAL COMMANDS
    void triggerCamera();

    void setServo(int channel, int pwm, ICommandListener listener);

    void sendMavlinkMessage(MavlinkMessageWrapper messageWrapper);

}
