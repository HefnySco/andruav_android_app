package com.andruav.protocol.commands.binaryMessages;

import com.andruav.BinaryHelper;

import org.json.JSONException;

/**
 * Created by mhefny on 9/16/15.
 * <br>cmd: <b>1016</b>
 * <br>Sends IMU Statistics. This image is sent in lower rate than {@link AndruavResalaBinary_IMU}
 */
public class AndruavResalaBinary_IMUStatistics extends AndruavResalaBinaryBase {

    public final static int TYPE_AndruavMessage_IMUStatistics = 1016;


    public boolean useFCBIMU;

    /***
     * in meters
     */
    public double GroundAltitude_max = 0.0f;
    /***
     * in meters per second
     */
    public float GroundSpeed_max = 0.0f;
    /***
     * in meters per second
     */
    public float GroundSpeed_avg = 0.0f;
    /***
     * in milliseconds
     */
    public long IdleDuration = 0;
    /***
     * in milliseconds
     */
    public long IdleTotalDuration = 0;


    public AndruavResalaBinary_IMUStatistics() {
        super();
        messageTypeID = TYPE_AndruavMessage_IMUStatistics;

    }


    @Override
    public void setMessage(byte[] binarymessage) throws JSONException {

        GroundAltitude_max = BinaryHelper.getDouble(binarymessage, 0);      //0-7
        GroundSpeed_max = BinaryHelper.getFloat(binarymessage, 8);      //8-11
        GroundSpeed_avg = BinaryHelper.getFloat(binarymessage, 12);      //12-15
        IdleDuration = BinaryHelper.getLong(binarymessage, 16);      //16-23
        IdleTotalDuration = BinaryHelper.getLong(binarymessage, 24);      //24-31
        useFCBIMU = BinaryHelper.getBoolean(binarymessage, 32);   //24-31

    }

    @Override
    public byte[] getJsonMessage() throws org.json.JSONException {
        data = new byte[33];
        BinaryHelper.putDouble(GroundAltitude_max, data, 0);
        BinaryHelper.putFloat(GroundSpeed_max, data, 8);
        BinaryHelper.putFloat(GroundSpeed_avg, data, 12);
        BinaryHelper.putLong(IdleDuration, data, 16);
        BinaryHelper.putLong(IdleTotalDuration, data, 24);
        BinaryHelper.putBoolean(useFCBIMU, data, 32);


        return data;
    }


}
