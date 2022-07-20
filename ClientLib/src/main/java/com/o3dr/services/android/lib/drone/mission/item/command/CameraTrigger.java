package com.o3dr.services.android.lib.drone.mission.item.command;

import android.os.Parcel;

import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.mission.MissionItemType;

/**
 * Created by fhuya on 11/6/14.
 */
public class CameraTrigger extends MissionItem implements MissionItem.Command, android.os.Parcelable {

    // Camera trigger distance. 0 to stop triggering.
    private double triggerDistance = (0);

    // Camera shutter integration time. -1 or 0 to ignore
    private double shutter  = (0);

    // Trigger camera once immediately. (0 = no trigger, 1 = trigger)
    private double trigger  = (0);


    public CameraTrigger(){
        super(MissionItemType.CAMERA_TRIGGER);
    }

    public CameraTrigger(CameraTrigger copy){
        super(MissionItemType.CAMERA_TRIGGER);
        triggerDistance = copy.triggerDistance;
        shutter = copy.shutter;
        trigger = copy.trigger;
    }

    public double getTriggerDistance() {
        return triggerDistance;
    }

    public void setTriggerDistance(double triggerDistance, double shutter, double trigger) {
        this.triggerDistance = triggerDistance;
        this.shutter = shutter;
        this.trigger = trigger;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CameraTrigger)) return false;
        if (!super.equals(o)) return false;

        CameraTrigger that = (CameraTrigger) o;

        return (
                (Double.compare(that.triggerDistance, triggerDistance) == 0) &&
                        (Double.compare(that.shutter, shutter) == 0) &&
                        (Double.compare(that.trigger, trigger) == 0)
                );

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (triggerDistance);
        result = 31 * result + (int) (shutter);
        result = 31 * result + (int) (trigger);
        return result;
    }

    @Override
    public String toString() {
        return "CameraTrigger{" +
                "triggerDistance=" + triggerDistance +
                "shutter=" + shutter +
                "trigger=" + trigger +
                '}';
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeDouble(this.triggerDistance);
        dest.writeDouble(this.shutter);
        dest.writeDouble(this.trigger);
    }

    private CameraTrigger(Parcel in) {
        super(in);
        this.triggerDistance = in.readDouble();
        this.shutter = in.readDouble();
        this.trigger = in.readDouble();
    }

    @Override
    public MissionItem clone() {
        return new CameraTrigger(this);
    }

    public static final Creator<CameraTrigger> CREATOR = new Creator<CameraTrigger>() {
        public CameraTrigger createFromParcel(Parcel source) {
            return new CameraTrigger(source);
        }

        public CameraTrigger[] newArray(int size) {
            return new CameraTrigger[size];
        }
    };
}
