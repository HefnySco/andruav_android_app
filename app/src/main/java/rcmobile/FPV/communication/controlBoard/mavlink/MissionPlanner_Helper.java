package rcmobile.FPV.communication.controlBoard.mavlink;

import com.mavlink.common.msg_mission_item;
import com.mavlink.enums.MAV_CMD;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;

import org.droidplanner.services.android.impl.core.mission.MissionImpl;
import org.droidplanner.services.android.impl.core.mission.MissionItemImpl;
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
import org.droidplanner.services.android.impl.utils.ProxyUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by mhefny on 9/26/16.
 */
public class MissionPlanner_Helper {


    static int currentMissionSequence=-1;
    static int requestedMissionSequence=0;
    static int waypoints_counts=0;
    static int waypoints_retries=0;


    static void  reset ()
    {
        currentMissionSequence=-1;
        requestedMissionSequence=0;
        waypoints_counts=0;
        waypoints_retries=0;
    }

    static boolean isWaitingForMission ()
    {
        if (waypoints_counts==0) return false;

        return requestedMissionSequence != (currentMissionSequence - 1);

    }


    static boolean isRecievedAllWayPoints () {

        return requestedMissionSequence != (currentMissionSequence - 1);

    }



    public static Mission parseMissionWayPointsText(final String missionText)
    {
        Mission mission;
        mission = parseMissionPlannerWayPointsText(missionText);
        if (mission == null)
        {
            mission = parseQGroundControlPlanFile(missionText);
        }

        return mission;
    }

    public static Mission parseQGroundControlPlanFile (final String missionText)
    {

        try {
            JSONObject jsonObject = new JSONObject (missionText);
            if (!jsonObject.get("fileType").equals("Plan"))
            {
                return null;
            }

            List<msg_mission_item> rawMissionItems = new LinkedList<>();

            JSONObject jsonMission = jsonObject.getJSONObject("mission");
            JSONArray jsonItems = jsonMission.getJSONArray("items");
            final int itemLength = jsonItems.length();
            for (int i=1;i<itemLength; ++i)
            {
                final msg_mission_item msg = new msg_mission_item();
                final JSONObject item = (JSONObject) jsonItems.get(i);
                final JSONArray params = item.getJSONArray("params");

                msg.seq = i;
                msg.command = item.getInt("command");
                msg.frame = (short) item.getInt("frame");

                if (!params.isNull(0))
                {
                    msg.param1 = Float.parseFloat(params.get(0).toString());
                }
                if (!params.isNull(1))
                {
                    msg.param2 = Float.parseFloat(params.get(1).toString());
                }
                if (!params.isNull(2))
                {
                    msg.param3 = Float.parseFloat(params.get(2).toString());
                }
                if (!params.isNull(3))
                {
                    msg.param4 = Float.parseFloat(params.get(3).toString());
                }
                if (!params.isNull(4))
                {
                    msg.x= Float.parseFloat(params.get(4).toString());
                }
                if (!params.isNull(5))
                {
                    msg.y= Float.parseFloat(params.get(5).toString());
                }
                if (!params.isNull(6))
                {
                    msg.z = Float.parseFloat(params.get(6).toString());
                }

                msg.autocontinue = 1;
                rawMissionItems.add(msg);
            }

            return fromRawMissionItems(rawMissionItems);

        } catch (JSONException e) {
            return null;
        }


    }

    public static Mission parseMissionPlannerWayPointsText(final String missionText)
    {

        /*

            http://qgroundcontrol.org/mavlink/waypoint_protocol#waypoint_file_format

            QGC WPL <VERSION>
            <INDEX> <CURRENT WP> <COORD FRAME> <COMMAND> <PARAM1> <PARAM2> <PARAM3> <PARAM4> <PARAM5/X/LONGITUDE> <PARAM6/Y/LATITUDE> <PARAM7/Z/ALTITUDE> <AUTOCONTINUE>


            e.g.:
                QGC WPL 110
                0	1	0	0	0	0	0	0	0	0	0	1
                1	0	3	16	0.000000	0.000000	0.000000	0.000000	30.921076	32.431641	100.000000	1
                2	0	3	16	0.000000	0.000000	0.000000	0.000000	30.751278	32.541504	100.000000	1
                3	0	3	16	0.000000	0.000000	0.000000	0.000000	30.600094	32.651367	100.000000	1
                4	0	3	16	0.000000	0.000000	0.000000	0.000000	30.164126	32.783203	100.000000	1
                5	0	3	16	0.000000	0.000000	0.000000	0.000000	30.315988	33.178711	100.000000	1
                6	0	3	21	0.000000	0.000000	0.000000	0.000000	30.977609	33.420410	100.000000	1
         */

        final String[] lines = missionText.split(System.getProperty("line.separator"));
        if (lines[0].indexOf("QGC WPL 110")== -1)
        {
            // BAD MISSION FILE NAME // PANIC
            return null;
        }

        List<msg_mission_item> rawMissionItems = new LinkedList<>();

        for (int i=1,len = lines.length;i<len;++i)
        {
            final String[] rowData = lines[i].split("\t");
            final msg_mission_item msg = new msg_mission_item();

            msg.seq = (Short.parseShort(rowData[0]));
            msg.current = (Byte.parseByte(rowData[1]));
            msg.frame = (Byte.parseByte(rowData[2]));
            msg.command = (Short.parseShort(rowData[3]));

            msg.param1 = (Float.parseFloat(rowData[4]));
            msg.param2 = (Float.parseFloat(rowData[5]));
            msg.param3 = (Float.parseFloat(rowData[6]));
            msg.param4 = (Float.parseFloat(rowData[7]));

            msg.x = (Float.parseFloat(rowData[8]));
            msg.y = (Float.parseFloat(rowData[9]));
            msg.z = (Float.parseFloat(rowData[10]));

            msg.autocontinue = (Byte.parseByte(rowData[11].trim()));

            rawMissionItems.add(msg);

        }
        return fromRawMissionItems(rawMissionItems);
    }




    private static Mission fromRawMissionItems(List<msg_mission_item> rawMissionItems){
        Mission mission = new Mission();
        if(rawMissionItems == null || rawMissionItems.isEmpty())
            return mission;

        List<MissionItemImpl> impls = processMavLinkMessages(new MissionImpl(null), rawMissionItems);
        if(!impls.isEmpty()) {
            for (MissionItemImpl impl: impls) {
                MissionItem missionItem = ProxyUtils.getProxyMissionItem(impl);
                if(missionItem != null){
                    mission.addMissionItem(missionItem);
                }
            }
        }
        return mission;
    }

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
