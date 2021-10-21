package com.andruav.protocol.commands.binaryMessages;


/**
 * Created by M.Hefny on 15-Nov-14.
 * <br>cmd: <b>2</b>
 */
public class AndruavResalaBinaryBase {

    public final static int TYPE_AndruavCMD_BASE = 2;

    public int messageTypeID;
    protected byte[] data;


    public AndruavResalaBinaryBase() {
        super();
        messageTypeID = TYPE_AndruavCMD_BASE;
    }


    public void setMessage(final byte[] message) throws Exception {

    }


    public byte[] getJsonMessage() throws org.json.JSONException {
        return null;
    }
}
