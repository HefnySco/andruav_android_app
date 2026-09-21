package org.droidplanner.services.android.impl.utils;

import com.MAVLink.common.msg_mission_item;
import com.MAVLink.enums.MAV_CMD;

import org.droidplanner.services.android.impl.core.mission.MissionImpl;
import org.droidplanner.services.android.impl.core.mission.MissionItemImpl;
import org.droidplanner.services.android.impl.core.mission.commands.CameraControlImpl;
import org.droidplanner.services.android.impl.core.mission.commands.CameraTriggerImpl;
import org.droidplanner.services.android.impl.core.mission.commands.ChangeSpeedImpl;
import org.droidplanner.services.android.impl.core.mission.commands.ConditionYawImpl;
import org.droidplanner.services.android.impl.core.mission.commands.DoJumpImpl;
import org.droidplanner.services.android.impl.core.mission.commands.EpmGripperImpl;
import org.droidplanner.services.android.impl.core.mission.commands.ReturnToHomeImpl;
import org.droidplanner.services.android.impl.core.mission.commands.SetRelayImpl;
import org.droidplanner.services.android.impl.core.mission.commands.SetServoImpl;
import org.droidplanner.services.android.impl.core.mission.commands.TakeoffImpl;
import org.droidplanner.services.android.impl.core.mission.waypoints.CircleImpl;
import org.droidplanner.services.android.impl.core.mission.waypoints.DoLandStartImpl;
import org.droidplanner.services.android.impl.core.mission.waypoints.LandImpl;
import org.droidplanner.services.android.impl.core.mission.waypoints.RegionOfInterestImpl;
import org.droidplanner.services.android.impl.core.mission.waypoints.SplineWaypointImpl;
import org.droidplanner.services.android.impl.core.mission.waypoints.WaypointImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fhuya on 8/13/2016.
 */
public class MissionUtils {

    private MissionUtils(){}

    public static List<MissionItemImpl> processMavLinkMessages(MissionImpl missionImpl, List<msg_mission_item> msgs) {
        List<MissionItemImpl> received = new ArrayList<MissionItemImpl>();
        for (msg_mission_item msg : msgs) {
            switch (msg.command) {
                case MAV_CMD.MAV_CMD_DO_SET_SERVO:
                    received.add(new SetServoImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_NAV_WAYPOINT:
                    received.add(new WaypointImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_NAV_SPLINE_WAYPOINT:
                    received.add(new SplineWaypointImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_NAV_LAND:
                    received.add(new LandImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_DO_LAND_START:
                    received.add(new DoLandStartImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_NAV_TAKEOFF:
                    received.add(new TakeoffImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_DO_CHANGE_SPEED:
                    received.add(new ChangeSpeedImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_DO_SET_CAM_TRIGG_DIST:
                    received.add(new CameraTriggerImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_DO_DIGICAM_CONTROL:
                    received.add(new CameraControlImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_DO_GRIPPER:
                    received.add(new EpmGripperImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_DO_SET_ROI:
                    received.add(new RegionOfInterestImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_NAV_LOITER_TURNS:
                    received.add(new CircleImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_NAV_RETURN_TO_LAUNCH:
                    received.add(new ReturnToHomeImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_CONDITION_YAW:
                    received.add(new ConditionYawImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_DO_SET_RELAY:
                    received.add(new SetRelayImpl(msg, missionImpl));
                    break;
                case MAV_CMD.MAV_CMD_DO_JUMP:
                    received.add(new DoJumpImpl(msg, missionImpl));
                    break;

                default:
                    break;
            }
        }
        return received;
    }
}
