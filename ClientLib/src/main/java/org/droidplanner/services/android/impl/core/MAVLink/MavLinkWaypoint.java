package org.droidplanner.services.android.impl.core.MAVLink;

import com.MAVLink.common.msg_mission_ack;
import com.MAVLink.common.msg_mission_count;
import com.MAVLink.common.msg_mission_request_int;
import com.MAVLink.common.msg_mission_request_list;
import com.MAVLink.common.msg_mission_set_current;
import com.MAVLink.enums.MAV_MISSION_RESULT;

import org.droidplanner.services.android.impl.core.drone.autopilot.MavLinkDrone;

public class MavLinkWaypoint {

	public static void sendAck(MavLinkDrone drone) {
		msg_mission_ack msg = new msg_mission_ack();
		msg.target_system = drone.getSysid();
		msg.target_component = drone.getCompid();
		msg.type = MAV_MISSION_RESULT.MAV_MISSION_ACCEPTED;
		drone.getMavClient().sendMessage(msg, null);

	}

	public static void requestWayPoint(MavLinkDrone drone, int index) {
		// MISSION_REQUEST_INT: the autopilot answers with MISSION_ITEM_INT (degE7),
		// replacing the deprecated MISSION_REQUEST / float32 MISSION_ITEM pair.
		msg_mission_request_int msg = new msg_mission_request_int();
		msg.target_system = drone.getSysid();
		msg.target_component = drone.getCompid();
		msg.seq = index;
		drone.getMavClient().sendMessage(msg, null);
	}

	public static void requestWaypointsList(MavLinkDrone drone) {
		msg_mission_request_list msg = new msg_mission_request_list();
		msg.target_system = drone.getSysid();
		msg.target_component = drone.getCompid();
		drone.getMavClient().sendMessage(msg, null);
	}

	public static void sendWaypointCount(MavLinkDrone drone, int count) {
		msg_mission_count msg = new msg_mission_count();
		msg.target_system = drone.getSysid();
		msg.target_component = drone.getCompid();
		msg.count = count;
		drone.getMavClient().sendMessage(msg, null);
	}

	public static void sendSetCurrentWaypoint(MavLinkDrone drone, short i) {
		msg_mission_set_current msg = new msg_mission_set_current();
		msg.target_system = drone.getSysid();
		msg.target_component = drone.getCompid();
		msg.seq = i;
		drone.getMavClient().sendMessage(msg, null);
	}

}
