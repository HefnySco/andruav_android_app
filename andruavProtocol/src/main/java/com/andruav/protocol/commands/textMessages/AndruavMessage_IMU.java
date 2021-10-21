package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by M.Hefny on 25-Oct-14.
 * cmd: 1001
 */
@Deprecated
public class AndruavMessage_IMU extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_IMU = 1001;

    /**
     * Pitch
     */
    public float Pitch;
    /**
     * Roll
     */
    public float Roll;
    /**
     * Y
     */
    public float Yaw;
    /**
     * Pitch Tilt
     */
    public float PitchTilt;
    /**
     * Roll Tilt
     */
    public float RollTilt;

    public boolean useFCBIMU;

    /**
     * Follows :
     * Surface.ROTATION_0   : 0
     * Surface.ROTATION_90  : 1
     * Surface.ROTATION_180 : 2
     * Surface.ROTATION_270 : 3
     */
    public int MobileDirection;

    public AndruavMessage_IMU() {
        super();
        messageTypeID = TYPE_AndruavMessage_IMU;
        useFCBIMU = false;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        useFCBIMU = json_receive_data.getBoolean("I");
        Pitch = (float) json_receive_data.getDouble("P");
        Roll = (float) json_receive_data.getDouble("R");
        Yaw = (float) json_receive_data.getDouble("Y");
        PitchTilt = (float) json_receive_data.getDouble("PT");
        RollTilt = (float) json_receive_data.getDouble("RT");
        MobileDirection = json_receive_data.getInt("MD");
    }

    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("I", useFCBIMU);
        json_data.accumulate("P", Pitch);
        json_data.accumulate("R", Roll);
        json_data.accumulate("Y", Yaw);
        json_data.accumulate("PT", PitchTilt);
        json_data.accumulate("RT", RollTilt);
        json_data.accumulate("MD", MobileDirection);

        return json_data.toString();
    }

}
