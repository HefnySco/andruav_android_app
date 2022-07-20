package org.droidplanner.services.android.impl.core.mission.commands;

import com.mavlink.common.msg_mission_item;
import com.mavlink.enums.MAV_CMD;

import org.droidplanner.services.android.impl.core.mission.MissionImpl;
import org.droidplanner.services.android.impl.core.mission.MissionItemImpl;
import org.droidplanner.services.android.impl.core.mission.MissionItemType;

import java.util.List;

public class CameraControlImpl extends MissionCMD {

    private double sessionControl   = (0);
    private double zoomAbsolute     = (0);
    private double zoomRelative     = (0);
    private double focus            = (0);
    private double shootCommand     = (0);
    private double commandIdentity  = (0);
    private double shotID           = (0);

    public CameraControlImpl(MissionItemImpl item) {
        super(item);
    }

    public CameraControlImpl(msg_mission_item msg, MissionImpl missionImpl) {
        super(missionImpl);
        unpackMAVMessage(msg);
    }

    public CameraControlImpl(MissionImpl missionImpl, double sessionControl, double zoomAbsolute, double zoomRelative, double focus, double shootCommand, double commandIdentity, double shotID) {
        super(missionImpl);
        this.sessionControl     = sessionControl;
        this.zoomAbsolute       = zoomAbsolute;
        this.zoomRelative       = zoomRelative;
        this.focus              = focus;
        this.shootCommand       = shootCommand;
        this.commandIdentity    = commandIdentity;
        this.shotID             = shotID;

    }

    @Override
    public List<msg_mission_item> packMissionItem() {
        List<msg_mission_item> list = super.packMissionItem();
        msg_mission_item mavMsg = list.get(0);
        mavMsg.command  = MAV_CMD.MAV_CMD_DO_DIGICAM_CONTROL;
        mavMsg.param1   = (float) sessionControl;
        mavMsg.param2   = (float) zoomAbsolute;
        mavMsg.param3   = (float) zoomRelative;
        mavMsg.param4   = (float) focus;
        mavMsg.x        = (float) shootCommand;
        mavMsg.y        = (float) commandIdentity;
        mavMsg.z        = (float) shotID;
        return list;
    }

    @Override
    public void unpackMAVMessage(msg_mission_item mavMsg) {

        sessionControl  = (mavMsg.param1);
        zoomAbsolute    = (mavMsg.param2);
        zoomRelative    = (mavMsg.param3);
        focus           = (mavMsg.param4);
        shootCommand    = (mavMsg.x);
        commandIdentity = (mavMsg.y);
        shotID          = (mavMsg.z);

    }

    @Override
    public MissionItemType getType() {
        return MissionItemType.CAMERA_CONTROL;
    }

    public double getSessionControl() {
        return sessionControl;
    }

    public void setSessionControl(double sessionControl) {
        this.sessionControl = sessionControl;
    }
    public double getZoomAbsolute() {
        return zoomAbsolute;
    }

    public void setZoomAbsolute(double zoomAbsolute) {
        this.zoomAbsolute = zoomAbsolute;
    }
    public double getZoomRelative() {
        return zoomRelative;
    }

    public void setZoomRelative(double zoomRelative) {
        this.zoomRelative = zoomRelative;
    }
    public double getFocus() {
        return focus;
    }

    public void setFocus(double focus) {
        this.focus = focus;
    }
    public double getShootCommand() {
        return shootCommand;
    }

    public void setShootCommand(double shootCommand) {
        this.shootCommand = shootCommand;
    }
    public double getCommandIdentity() {
        return commandIdentity;
    }

    public void setCommandIdentity(double commandIdentity) {
        this.commandIdentity = commandIdentity;
    }
    public double getShotID() {
        return shotID;
    }

    public void setShotID(double shotID) {
        this.shotID = shotID;
    }

}
