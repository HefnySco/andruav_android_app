package org.droidplanner.services.android.impl.core.mission.waypoints;

import com.MAVLink.common.msg_mission_item_int;
import org.droidplanner.services.android.impl.core.MAVLink.MavLinkMissionItemInt;

import org.droidplanner.services.android.impl.core.mission.MissionImpl;
import org.droidplanner.services.android.impl.core.mission.MissionItemImpl;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;

import java.util.List;

public abstract class SpatialCoordItem extends MissionItemImpl {

    protected LatLongAlt coordinate;

    public SpatialCoordItem(MissionImpl missionImpl, LatLongAlt coord) {
        super(missionImpl);
        this.coordinate = coord;
    }

    public SpatialCoordItem(MissionItemImpl item) {
        super(item);
        if (item instanceof SpatialCoordItem) {
            coordinate = ((SpatialCoordItem) item).getCoordinate();
        } else {
            coordinate = new LatLongAlt(0, 0, 0);
        }
    }

    public void setCoordinate(LatLongAlt coordNew) {
        coordinate = coordNew;
    }

    public LatLongAlt getCoordinate() {
        return coordinate;
    }

    @Override
    public List<msg_mission_item_int> packMissionItem() {
        List<msg_mission_item_int> list = super.packMissionItem();
        msg_mission_item_int mavMsg = list.get(0);
        mavMsg.x = MavLinkMissionItemInt.toDegE7(coordinate.getLatitude());
        mavMsg.y = MavLinkMissionItemInt.toDegE7(coordinate.getLongitude());
        mavMsg.z = (float) coordinate.getAltitude();
        return list;
    }

    @Override
    public void unpackMAVMessage(msg_mission_item_int mavMsg) {
        setCoordinate(new LatLongAlt(MavLinkMissionItemInt.fromDegE7(mavMsg.x),
                MavLinkMissionItemInt.fromDegE7(mavMsg.y), mavMsg.z));
    }

    public void setAltitude(double altitude) {
        coordinate.setAltitude(altitude);
    }

    public void setPosition(LatLong position) {
        coordinate.set(position);
    }

}