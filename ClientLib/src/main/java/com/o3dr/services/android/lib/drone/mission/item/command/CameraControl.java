package com.o3dr.services.android.lib.drone.mission.item.command;

import android.os.Parcel;

import com.o3dr.services.android.lib.drone.mission.MissionItemType;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;

public class CameraControl extends MissionItem implements MissionItem.Command, android.os.Parcelable {

    private double sessionControl   = (0);
    private double zoomAbsolute     = (0);
    private double zoomRelative     = (0);
    private double focus            = (0);
    private double shootCommand     = (0);
    private double commandIdentity  = (0);
    private double shotID           = (0);

    public CameraControl(){
        super(MissionItemType.CAMERA_CONTROL);
    }

    public CameraControl(CameraControl copy){
        super(MissionItemType.CAMERA_CONTROL);
        sessionControl  = copy.sessionControl;
        zoomAbsolute    = copy.zoomAbsolute;
        zoomRelative    = copy.zoomRelative;
        focus           = copy.focus;
        shootCommand    = copy.shootCommand;
        commandIdentity = copy.commandIdentity;
        shotID          = copy.shotID;
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CameraControl)) return false;
        if (!super.equals(o)) return false;

        CameraControl that = (CameraControl) o;

        return (
                (Double.compare(that.sessionControl, sessionControl) == 0) &&
                (Double.compare(that.zoomAbsolute, zoomAbsolute) == 0) &&
                (Double.compare(that.zoomRelative, zoomRelative) == 0) &&
                (Double.compare(that.focus, focus) == 0) &&
                (Double.compare(that.shootCommand, shootCommand) == 0) &&
                (Double.compare(that.commandIdentity, commandIdentity) == 0) &&
                (Double.compare(that.shotID, shotID) == 0)
        );

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (sessionControl);
        result = 31 * result + (int) (zoomAbsolute);
        result = 31 * result + (int) (zoomRelative);
        result = 31 * result + (int) (focus);
        result = 31 * result + (int) (shootCommand);
        result = 31 * result + (int) (commandIdentity);
        result = 31 * result + (int) (shotID);
        return result;
    }

    @Override
    public String toString() {
        return "CameraControl{"     +
                "sessionControl="   + sessionControl +
                "zoomAbsolute="     + zoomAbsolute +
                "zoomRelative="     + zoomRelative +
                "focus="            + focus +
                "shootCommand="     + shootCommand +
                "commandIdentity="  + commandIdentity +
                "shotID="           + shotID +
                '}';
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeDouble(this.sessionControl);
        dest.writeDouble(this.zoomAbsolute);
        dest.writeDouble(this.zoomRelative);
        dest.writeDouble(this.focus);
        dest.writeDouble(this.shootCommand);
        dest.writeDouble(this.commandIdentity);
        dest.writeDouble(this.shotID);

    }

    private CameraControl(Parcel in) {
        super(in);
        this.sessionControl  = in.readDouble();
        this.zoomAbsolute    = in.readDouble();
        this.zoomRelative    = in.readDouble();
        this.focus           = in.readDouble();
        this.shootCommand    = in.readDouble();
        this.commandIdentity = in.readDouble();
        this.shotID          = in.readDouble();
    }

    @Override
    public MissionItem clone() {
        return new CameraControl(this);
    }

    public static final Creator<CameraControl> CREATOR = new Creator<CameraControl>() {
        public CameraControl createFromParcel(Parcel source) {
            return new CameraControl(source);
        }

        public CameraControl[] newArray(int size) {
            return new CameraControl[size];
        }
    };
}
