package org.droidplanner.services.android.impl.core.mission.commands;

import com.MAVLink.common.msg_mission_item;
import com.MAVLink.enums.MAV_CMD;

import org.droidplanner.services.android.impl.core.mission.MissionImpl;
import org.droidplanner.services.android.impl.core.mission.MissionItemImpl;
import org.droidplanner.services.android.impl.core.mission.MissionItemType;

import java.util.List;

public class CameraTriggerImpl extends MissionCMD {

    // Camera trigger distance. 0 to stop triggering.
    private double triggerDistance = (0);

    // Camera shutter integration time. -1 or 0 to ignore
    private double shutter  = (0);

    // Trigger camera once immediately. (0 = no trigger, 1 = trigger)
    private double trigger  = (0);

    public CameraTriggerImpl(MissionItemImpl item) {
        super(item);
    }

    public CameraTriggerImpl(msg_mission_item msg, MissionImpl missionImpl) {
        super(missionImpl);
        unpackMAVMessage(msg);
    }

    public CameraTriggerImpl(MissionImpl missionImpl, double triggerDistance, double shutter, double trigger) {
        super(missionImpl);
        this.triggerDistance = triggerDistance;
        this.shutter = shutter;
        this.trigger = trigger;
    }

    @Override
    public List<msg_mission_item> packMissionItem() {
        List<msg_mission_item> list = super.packMissionItem();
        msg_mission_item mavMsg = list.get(0);
        mavMsg.command = MAV_CMD.MAV_CMD_DO_SET_CAM_TRIGG_DIST;
        mavMsg.param1 = (float) triggerDistance;
        mavMsg.param2 = (float) shutter;
        mavMsg.param3 = (float) trigger;
        return list;
    }

    @Override
    public void unpackMAVMessage(msg_mission_item mavMsg) {
        triggerDistance = (mavMsg.param1);
        shutter = (mavMsg.param2);
        trigger = (mavMsg.param3);
    }

    @Override
    public MissionItemType getType() {
        return MissionItemType.CAMERA_TRIGGER;
    }

    public double getTriggerDistance() {
        return triggerDistance;
    }

    public void setTriggerDistance(double triggerDistance) {
        this.triggerDistance = triggerDistance;
    }

    public double getShutter() {
        return shutter;
    }

    public void setShutter(double shutter) {
        this.shutter = shutter;
    }

    public double getTrigger() {
        return trigger;
    }

    public void setTrigger(double trigger) {
        this.trigger = trigger;
    }
}