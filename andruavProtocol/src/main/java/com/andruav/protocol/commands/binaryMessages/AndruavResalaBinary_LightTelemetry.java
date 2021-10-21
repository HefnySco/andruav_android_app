package com.andruav.protocol.commands.binaryMessages;

import org.json.JSONException;

/**
 * This is a Telemetry message that is ALWAYS sent from GCS for simpler packet format.
 * Created by mhefny on 3/24/17.
 */

public class AndruavResalaBinary_LightTelemetry extends AndruavResalaBinaryBase {

    public final static int TYPE_AndruavMessage_LightTelemetry = 2022;




    public AndruavResalaBinary_LightTelemetry() {
        super();
        messageTypeID = TYPE_AndruavMessage_LightTelemetry;
    }



    public byte[] getData() {
        return data;
    }

    public void setData(final byte[] image) {
        data = image;
    }

    @Override
    public void setMessage(final byte[] binarymessage) throws JSONException {

        data = new byte[binarymessage.length];
        System.arraycopy(binarymessage, 0, data, 0, data.length);
    }

    @Override
    public byte[] getJsonMessage() throws org.json.JSONException {

        return data;
    }
}

