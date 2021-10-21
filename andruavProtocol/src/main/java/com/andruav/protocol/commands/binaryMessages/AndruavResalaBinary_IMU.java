package com.andruav.protocol.commands.binaryMessages;

import com.andruav.BinaryHelper;

import org.json.JSONException;

/**
 * Created by M.Hefny on 28-Jul-15.
 * <br>cmd: <b>1013</b>
 * <br>Holds IMU data.
 */
public class AndruavResalaBinary_IMU extends AndruavResalaBinaryBase {

    public final static int TYPE_AndruavMessage_BinaryIMU = 1013;

    /**
     * Pitch
     */
    public double Pitch;
    /**
     * Roll
     */
    public double Roll;
    /**
     * Y
     */
    public double Yaw;
    /**
     * Pitch Tilt
     */
    public double PitchTilt;
    /**
     * Roll Tilt
     */
    public double RollTilt;

    /**
     * true if the source of IMU data is FCB "flight control board"
     */
    public boolean useFCBIMU;

    /**
     * Follows :
     * <br>Surface.ROTATION_0   : 0
     * <br>Surface.ROTATION_90  : 1
     * <br>Surface.ROTATION_180 : 2
     * <br>Surface.ROTATION_270 : 3
     */
    public int MobileDirection;

    public AndruavResalaBinary_IMU() {
        super();
        messageTypeID = TYPE_AndruavMessage_BinaryIMU;
        useFCBIMU = false;
    }

    @Override
    public void setMessage(byte[] binarymessage) throws JSONException {
        Pitch = BinaryHelper.getDouble(binarymessage, 0);   //0-7
        Roll = BinaryHelper.getDouble(binarymessage, 8);   //8-15
        Yaw = BinaryHelper.getDouble(binarymessage, 16);   //16-23
        PitchTilt = BinaryHelper.getDouble(binarymessage, 24);  //24-27
        RollTilt = BinaryHelper.getDouble(binarymessage, 32);  //28-31
        useFCBIMU = BinaryHelper.getBoolean(binarymessage, 40); //32-33
        MobileDirection = BinaryHelper.getInt(binarymessage, 41);    //41-44

    }

    @Override
    public byte[] getJsonMessage() throws org.json.JSONException {
        data = new byte[45];
        BinaryHelper.putDouble(Pitch, data, 0);
        BinaryHelper.putDouble(Roll, data, 8);
        BinaryHelper.putDouble(Yaw, data, 16);
        BinaryHelper.putDouble(PitchTilt, data, 24);
        BinaryHelper.putDouble(RollTilt, data, 32);
        BinaryHelper.putBoolean(useFCBIMU, data, 40);
        BinaryHelper.putInt(MobileDirection, data, 41);

        return data;
    }
}
