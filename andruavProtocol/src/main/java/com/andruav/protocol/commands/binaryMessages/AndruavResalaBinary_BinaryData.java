package com.andruav.protocol.commands.binaryMessages;

/**
 * Created by M.Hefny on 25-Dec-14.
 * <br>cmd: <b>2</b>
 * <br>Holds generic binary data.
 */
public class AndruavResalaBinary_BinaryData extends AndruavResalaBinaryBase {

    public final static int TYPE_AndruavMessageBinary_Binary = 2;


    public AndruavResalaBinary_BinaryData() {
        super();
        messageTypeID = TYPE_AndruavMessageBinary_Binary;
    }

    @Override
    public void setMessage(byte[] binarymessage) {
        data = binarymessage;
    }

    @Override
    public byte[] getJsonMessage() {
        return data;
    }
}
