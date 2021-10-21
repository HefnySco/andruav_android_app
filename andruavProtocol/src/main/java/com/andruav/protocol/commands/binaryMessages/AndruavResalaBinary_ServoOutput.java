package com.andruav.protocol.commands.binaryMessages;

import com.andruav.BinaryHelper;

import org.json.JSONException;

import static com.andruav.andruavUnit.AndruavUnitBase.SERVO_OUTPUT_NUMBER;

public class AndruavResalaBinary_ServoOutput extends AndruavResalaBinaryBase {

    public final static int TYPE_AndruavMessage_ServoOutput = 6501;


    public int[] ServosOutput = new int[8];


    public AndruavResalaBinary_ServoOutput() {
        super();
        messageTypeID = TYPE_AndruavMessage_ServoOutput;
    }


    @Override
    public void setMessage(byte[] binarymessage) throws JSONException {
        int[] channels = new int[SERVO_OUTPUT_NUMBER];
        for (int i=0;i<SERVO_OUTPUT_NUMBER;++i)
        {
            ServosOutput[i] = BinaryHelper.getInt(binarymessage, i*4);
        }
    }


    public byte[] getJsonMessage() throws org.json.JSONException {
        data = new byte[SERVO_OUTPUT_NUMBER*4];
        for (int i=0;i<SERVO_OUTPUT_NUMBER;++i) {
            BinaryHelper.putInt(ServosOutput[i], data, i*4);
        }

        return data;
    }


}
