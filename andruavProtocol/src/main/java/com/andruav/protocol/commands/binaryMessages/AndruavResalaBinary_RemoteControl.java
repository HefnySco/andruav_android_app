package com.andruav.protocol.commands.binaryMessages;

import com.andruav.BinaryHelper;
import com.andruav.controlBoard.ControlBoardBase;

import org.json.JSONException;

/**
 * Created by mhefny on 12/27/15.
 */
@Deprecated
public class AndruavResalaBinary_RemoteControl extends AndruavResalaBinaryBase {

    public final static int TYPE_AndruavMessage_RemoteControl = 1017;

    /***
     * Remote is activated
     */
    public boolean isEngaged;

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
    public int Aux1;
    /***
     * Auxilary 2
     * <br>values typically from 1000..2000 to represents ms in true system.
     */
    public int Aux2;
    /***
     * Auxilary 3
     * <br>values typically from 1000..2000 to represents ms in true system.
     */
    public int Aux3;
    /***
     * Auxilary 4
     * <br>values typically from 1000..2000 to represents ms in true system.
     */
    public int Aux4;


    protected int[] mChannels;

    public AndruavResalaBinary_RemoteControl() {
        super();
        messageTypeID = TYPE_AndruavMessage_RemoteControl;
    }

    /***
     *
     * @param channels
     * @param engaged
     */
    public AndruavResalaBinary_RemoteControl(int[] channels, boolean engaged) {
        this();
        isEngaged = engaged;
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
    public void setMessage(byte[] binarymessage) throws JSONException {
        int[] channels = new int[9];
        isEngaged = BinaryHelper.getBoolean(binarymessage, 0);
        channels[ControlBoardBase.CONST_CHANNEL_3_THROTTLE] = BinaryHelper.getInt(binarymessage, 1);    // Throttle
        channels[ControlBoardBase.CONST_CHANNEL_4_YAW] = BinaryHelper.getInt(binarymessage, 5);  // Rudder
        channels[ControlBoardBase.CONST_CHANNEL_1_ROLL] = BinaryHelper.getInt(binarymessage, 9);  // Aileron
        channels[ControlBoardBase.CONST_CHANNEL_2_PITCH] = BinaryHelper.getInt(binarymessage, 13); // Elevator
        channels[ControlBoardBase.CONST_CHANNEL_5_AUX1] = BinaryHelper.getInt(binarymessage, 17);  // Aux1
        channels[ControlBoardBase.CONST_CHANNEL_6_AUX2] = BinaryHelper.getInt(binarymessage, 21);   // Aux2
        channels[ControlBoardBase.CONST_CHANNEL_7_AUX3] = BinaryHelper.getInt(binarymessage, 25);    // Aux3
        channels[ControlBoardBase.CONST_CHANNEL_8_AUX4] = BinaryHelper.getInt(binarymessage, 29); // Aux4
      //  int rtc = BinaryHelper.getInt(binarymessage, 33); // RTC

        setData(channels);

        //    Log.d("RX:", AndruavSettings.andruavWe7daBase.UnitID + " " + String.valueOf(Throttle));
    }

    /***
     * You can fill the data using direct throttle variable of using setData
     * that is why variables are used to fill data so it is valid all time.
     *
     * @return
     * @throws org.json.JSONException
     */
    @Override
    public byte[] getJsonMessage() throws org.json.JSONException {
        data = new byte[33];
        BinaryHelper.putBoolean(isEngaged, data, 0);
        BinaryHelper.putInt(Throttle, data, 1);
        BinaryHelper.putInt(Rudder, data, 5);
        BinaryHelper.putInt(Aileron, data, 9);
        BinaryHelper.putInt(Elevator, data, 13);
        BinaryHelper.putInt(Aux1, data, 17);
        BinaryHelper.putInt(Aux2, data, 21);
        BinaryHelper.putInt(Aux3, data, 25);
        BinaryHelper.putInt(Aux4, data, 29);
       // BinaryHelper.putInt(RTC, data, 33);

        //  Log.d("RX:", AndruavSettings.andruavWe7daBase.UnitID + " " + String.valueOf(Throttle));

        return data;
    }

}
