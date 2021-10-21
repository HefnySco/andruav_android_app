package com.andruav.protocol.commands.textMessages;

import com.andruav.controlBoard.ControlBoardBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by M.Hefny on 26-Apr-15.
 * <br>cmd: <b>1009</b>
 * <br>This command is used to represent Remote Control Signals
 * <br>Channels are generic and should be correctly mapped by different FCB adapters.
 * It is used by AndruabWebClient
 */
public class AndruavMessage_RemoteControl2 extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_RemoteControl2 = 1052;


    /***
     * values typically from 1000..2000 to represents ms in true system.
     */
    public int Throttle;
    /***
     * values typically from 1000..2000 to represents ms in true system.
     */
    public int Rudder;
    /***
     * values typically from 1000..2000 to represents ms in true system.
     */
    public int Aileron;
    /***
     * values typically from 1000..2000 to represents ms in true system.
     */
    public int Elevator;
    /***
     * Auxilary 1
     * <br>values typically from 1000..2000 to represents ms in true system.
     */
    public int Aux1 = -1;
    /***
     * Auxilary 2
     * <br>values typically from 1000..2000 to represents ms in true system.
     */
    public int Aux2 = -1;
    /***
     * Auxilary 3
     * <br>values typically from 1000..2000 to represents ms in true system.
     */
    public int Aux3 = -1;
    /***
     * Auxilary 4
     * <br>values typically from 1000..2000 to represents ms in true system.
     */
    public int Aux4 = -1;

    protected int[] mChannels;

    public AndruavMessage_RemoteControl2() {
        super();
        messageTypeID = TYPE_AndruavMessage_RemoteControl2;
    }

    public AndruavMessage_RemoteControl2(int[] channels, boolean engaged) {
        this();
        setData(channels);
    }

    public int[] getChannelsCopy() {
        return mChannels;
    }

    public void setData(int[] channels) {
        mChannels = channels;
        Throttle = channels[ControlBoardBase.CONST_CHANNEL_3_THROTTLE];
        Rudder = channels[ControlBoardBase.CONST_CHANNEL_4_YAW];
        Aileron = channels[ControlBoardBase.CONST_CHANNEL_1_ROLL];
        Elevator = channels[ControlBoardBase.CONST_CHANNEL_2_PITCH];
        Aux1 = channels[ControlBoardBase.CONST_CHANNEL_5_AUX1];
        Aux2 = channels[ControlBoardBase.CONST_CHANNEL_6_AUX2];
        Aux3 = channels[ControlBoardBase.CONST_CHANNEL_7_AUX3];
        Aux4 = channels[ControlBoardBase.CONST_CHANNEL_8_AUX4];
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {

        final JSONObject json_receive_data = new JSONObject(messageText);
        int[] channels = new int[8];
        channels[ControlBoardBase.CONST_CHANNEL_3_THROTTLE] = (int) json_receive_data.getDouble("T");    // Throttle
        channels[ControlBoardBase.CONST_CHANNEL_4_YAW] = (int) json_receive_data.getDouble("R");    // Rudder
        channels[ControlBoardBase.CONST_CHANNEL_1_ROLL] = (int) json_receive_data.getDouble("A");  // Aileron
        channels[ControlBoardBase.CONST_CHANNEL_2_PITCH] = (int) json_receive_data.getDouble("E"); // Elevator
        if (json_receive_data.has("w"))
            channels[ControlBoardBase.CONST_CHANNEL_5_AUX1] = (int) json_receive_data.getDouble("w");    // Aux1
        if (json_receive_data.has("x"))
            channels[ControlBoardBase.CONST_CHANNEL_6_AUX2] = (int) json_receive_data.getDouble("x");    // Aux2
        if (json_receive_data.has("y"))
            channels[ControlBoardBase.CONST_CHANNEL_7_AUX3] = (int) json_receive_data.getDouble("y");    // Aux3
        if (json_receive_data.has("z"))
            channels[ControlBoardBase.CONST_CHANNEL_8_AUX4] = (int) json_receive_data.getDouble("z");    // Aux4

        setData(channels);
    }

    /***
     * You can fill the data using direct throttle variable of using setData
     * that is why variables are used to fill data so it is valid all time.
     *
     * @return
     * @throws JSONException
     */
    @Override
    public String getJsonMessage() throws JSONException {
        final JSONObject json_data = new JSONObject();
        json_data.accumulate("T", Throttle);
        json_data.accumulate("R", Rudder);
        json_data.accumulate("A", Aileron);
        json_data.accumulate("E", Elevator);
        json_data.accumulate("w", Aux1);
        json_data.accumulate("x", Aux2);
        json_data.accumulate("y", Aux3);
        json_data.accumulate("z", Aux4);

        return json_data.toString();
    }

}
