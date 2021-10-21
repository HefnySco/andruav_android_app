package com.andruav.protocol.commands.binaryMessages;

import com.andruav.BinaryHelper;
import com.andruav.controlBoard.shared.missions.MissionBase;
import com.andruav.controlBoard.shared.missions.MissionEkla3;
import com.andruav.controlBoard.shared.missions.MissionHoboot;
import com.andruav.controlBoard.shared.missions.MohemmaMapBase;
import com.andruav.controlBoard.shared.missions.MissionRTL;
import com.andruav.controlBoard.shared.missions.WayPointStep;

import org.json.JSONException;

/**
 * Created by M.Hefny on 23-Aug-15.
 * <br>cmd: <b>1015</b>
 * <br>Contain waypoints for a drone.
 * <br>This is used to communicate with Andruav, and not forwarded directly to FCB.
 * <br> for 3rd Mission planner it uses {@link  AndruavResalaBinary_LightTelemetry to sendMessageToModule waypoints to FCB directly}
 */
@Deprecated
public class AndruavResalaBinary_WayPoints extends AndruavResalaBinaryBase {

    public final static int TYPE_AndruavMessageBinary_WayPoints = 1015;
    /***
     * This is the maximum size of a {@link MissionBase size}
     */
    protected final int WAYPOINT_SIZE = 41;
    protected MohemmaMapBase mMohemmaMapBase;


    public AndruavResalaBinary_WayPoints() {
        super();
        messageTypeID = TYPE_AndruavMessageBinary_WayPoints;
    }

    public MohemmaMapBase getWayPoints() {
        return mMohemmaMapBase;
    }

    public void setWayPoints(MohemmaMapBase mohemmaMapBase) {
        mMohemmaMapBase = mohemmaMapBase;
    }

    @Override
    public void setMessage(byte[] binarymessage) throws JSONException {

        mMohemmaMapBase = new MohemmaMapBase();
        final byte[] data = binarymessage;
        final int size = binarymessage.length;
        int numberOfRecords = binarymessage.length / WAYPOINT_SIZE;
        for (int i = 0, byteIndex = 0; i < numberOfRecords; ++i) {

            MissionBase missionBase;
            byteIndex = i * WAYPOINT_SIZE;

            byte waypointType = BinaryHelper.getByte(data, byteIndex);
            if (waypointType == WayPointStep.TYPE_WAYPOINTSTEP) {
                WayPointStep wayPointStep = new WayPointStep();
                wayPointStep.Sequence = BinaryHelper.getInt(data, byteIndex + 1);
                wayPointStep.Longitude = BinaryHelper.getDouble(data, byteIndex + 5);
                wayPointStep.Latitude = BinaryHelper.getDouble(data, byteIndex + 13);
                wayPointStep.Altitude = BinaryHelper.getDouble(data, byteIndex + 21);
                wayPointStep.Heading = BinaryHelper.getFloat(data, byteIndex + 29);
                wayPointStep.TimeToStay = BinaryHelper.getDouble(data, byteIndex + 33);

                missionBase = wayPointStep;

            } else if (waypointType == MissionEkla3.TYPE_EKLA3) {
                final MissionEkla3 mohemmaEkla3 = new MissionEkla3();
                mohemmaEkla3.Sequence = BinaryHelper.getInt(data, byteIndex + 1);
                mohemmaEkla3.setAltitude(BinaryHelper.getDouble(data, byteIndex + 5));
                mohemmaEkla3.setPitch(BinaryHelper.getDouble(data, byteIndex + 13));

                missionBase = mohemmaEkla3;

            } else if (waypointType == MissionHoboot.TYPE_HOBOOT) {
                final MissionHoboot mohemmaHoboot = new MissionHoboot();
                mohemmaHoboot.Sequence = BinaryHelper.getInt(data, byteIndex + 1);

                missionBase = mohemmaHoboot;

            } else if (waypointType == MissionRTL.TYPE_RTL) {
                final MissionRTL mohemmaRTL = new MissionRTL();
                mohemmaRTL.Sequence = BinaryHelper.getInt(data, byteIndex + 1);

                missionBase = mohemmaRTL;
            } else
            {
                missionBase = new MissionBase();
                missionBase.Sequence = BinaryHelper.getInt(data, byteIndex + 1);
            }


            mMohemmaMapBase.put(String.valueOf(missionBase.Sequence), missionBase);
            missionBase = null;
        }
    }


    @Override
    public byte[] getJsonMessage() throws org.json.JSONException {
        final int numberOfRecords = mMohemmaMapBase.size();
        final int sizeBytes = numberOfRecords * WAYPOINT_SIZE;

        byte[] data = new byte[sizeBytes];

        for (int i = 0, byteIndex = 0; i < numberOfRecords; ++i) {
            byteIndex = i * WAYPOINT_SIZE;
            final MissionBase missionBase = mMohemmaMapBase.valueAt(i);

            if (missionBase instanceof WayPointStep) {
                final WayPointStep wayPointStep= (WayPointStep) missionBase;

                BinaryHelper.putByte(WayPointStep.TYPE_WAYPOINTSTEP, data, byteIndex);
                BinaryHelper.putInt(wayPointStep.Sequence, data, byteIndex + 1);
                BinaryHelper.putDouble(wayPointStep.Longitude, data, byteIndex+5);
                BinaryHelper.putDouble(wayPointStep.Latitude, data, byteIndex + 13);
                BinaryHelper.putDouble(wayPointStep.Altitude, data, byteIndex + 21);
                BinaryHelper.putFloat(wayPointStep.Heading, data, byteIndex + 29);
                BinaryHelper.putDouble(wayPointStep.TimeToStay, data, byteIndex + 33);

            }
            else if (missionBase instanceof MissionEkla3)
            {
                final MissionEkla3 mohemmaEkla3 = (MissionEkla3) missionBase;
                BinaryHelper.putByte(MissionEkla3.TYPE_EKLA3, data, byteIndex);
                BinaryHelper.putInt(mohemmaEkla3.Sequence, data, byteIndex + 1);
                BinaryHelper.putDouble(mohemmaEkla3.getAltitude(), data, byteIndex + 5);
                BinaryHelper.putDouble(mohemmaEkla3.getPitch(), data, byteIndex + 13);
            }
            else if (missionBase instanceof MissionHoboot)
            {
                final MissionHoboot mohemmaHoboot = (MissionHoboot) missionBase;
                BinaryHelper.putByte(MissionHoboot.TYPE_HOBOOT, data, byteIndex);
                BinaryHelper.putInt(mohemmaHoboot.Sequence, data, byteIndex + 1);
            }
            else if (missionBase instanceof MissionRTL)
            {
                final MissionRTL mohemmaRTL = (MissionRTL) missionBase;
                BinaryHelper.putByte(MissionRTL.TYPE_RTL, data, byteIndex);
                BinaryHelper.putInt(mohemmaRTL.Sequence, data, byteIndex + 1);
            }
            else
            {
                BinaryHelper.putByte(MissionBase.TYPE_UNKNOWN, data, byteIndex);
            }

        }
        return data;
    }

}
