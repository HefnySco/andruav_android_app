package com.andruav.protocol.commands.binaryMessages;

import com.andruav.BinaryHelper;

import org.json.JSONException;

/**
 * Created by mhefny on 5/3/16.
 */
public class AndruavResalaBinary_RemoteControlSettings extends AndruavResalaBinaryBase {

    public final static int TYPE_AndruavMessage_RemoteControlSettings = 2021;

    /***
     * RTC: Return To Center
     */
    private boolean[] mRTC;


    public AndruavResalaBinary_RemoteControlSettings() {
        super();
        messageTypeID = TYPE_AndruavMessage_RemoteControlSettings;

       mRTC = new boolean[8];
    }


    public AndruavResalaBinary_RemoteControlSettings(final boolean[] rtc) {
        super();
        messageTypeID = TYPE_AndruavMessage_RemoteControlSettings;

        mRTC = rtc;
    }


    public boolean[] getRTC()
    {
        return  mRTC;
    }

    public void setRTC (boolean[] rtc)
    {
        mRTC = rtc;
    }

    @Override
    public void setMessage(byte[] binarymessage) throws JSONException {

        for (int i=0;i<8;++i)
        {
            mRTC[i] =  BinaryHelper.getBoolean(binarymessage, i);
        }

     }


    @Override
    public byte[] getJsonMessage() throws org.json.JSONException {
        data = new byte[8];

        for (int i=0;i<8;++i)
        {
            BinaryHelper.putBoolean(mRTC[i], data, i);
        }

        return data;
    }

}
