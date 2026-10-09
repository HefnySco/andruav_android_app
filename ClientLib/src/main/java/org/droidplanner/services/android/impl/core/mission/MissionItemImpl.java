package org.droidplanner.services.android.impl.core.mission;

import com.MAVLink.common.msg_mission_item_int;
import com.MAVLink.enums.MAV_FRAME;

import java.util.ArrayList;
import java.util.List;

public abstract class MissionItemImpl implements Comparable<MissionItemImpl> {

	protected final MissionImpl missionImpl;

	public MissionItemImpl(MissionImpl missionImpl) {
		this.missionImpl = missionImpl;
	}

	public MissionItemImpl(MissionItemImpl item) {
		this(item.missionImpl);
	}

	/**
	 * Return a new list (one or more) of MAVLinkMessage msg_mission_item_int that
	 * represent this MissionItem
	 * 
	 * @return
	 */
	public List<msg_mission_item_int> packMissionItem() {
		List<msg_mission_item_int> list = new ArrayList<msg_mission_item_int>();
		msg_mission_item_int mavMsg = new msg_mission_item_int();
		list.add(mavMsg);
		mavMsg.autocontinue = 1;
		mavMsg.frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT;
		return list;
	}

	/**
	 * Gets data from MAVLinkMessage msg_mission_item_int for this MissionItem
	 * 
	 * @return
	 */
	public abstract void unpackMAVMessage(msg_mission_item_int mavMsg);

	public abstract MissionItemType getType();

	public MissionImpl getMission() {
		return missionImpl;
	}

	@Override
	public int compareTo(MissionItemImpl another) {
		return missionImpl.getOrder(this) - missionImpl.getOrder(another);
	}

}
