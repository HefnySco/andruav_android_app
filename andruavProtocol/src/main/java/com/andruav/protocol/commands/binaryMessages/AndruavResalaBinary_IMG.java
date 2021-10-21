package com.andruav.protocol.commands.binaryMessages;

import android.location.Location;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by M.Hefny on 25-Dec-14.
 * <br>cmd: <b>1006</b>
 * <br>Holds image data, it can be image, GPS data and description can be attached.
 */
public class AndruavResalaBinary_IMG extends AndruavResalaBinaryBase {

    public final static int TYPE_AndruavMessage_IMG = 1006;

    /***
     * ImageLocation nullable data
     */
    public Location ImageLocation;
    /***
     * Nullable
     */
    public String Description;


    public AndruavResalaBinary_IMG() {
        super();
        messageTypeID = TYPE_AndruavMessage_IMG;
    }

    public byte[] getImage() {
        return data;
    }

    public void setImage(byte[] image) {
        data = image;
    }

    @Override
    public void setMessage(byte[] binarymessage) throws JSONException {

        final int i = AndruavBinaryHelper.getSplitIndex(binarymessage);
        if (i == -1) throw new JSONException("No JSON found");
        String messageText = new String(binarymessage, 0, i);


        JSONObject json_receive_data = new JSONObject(messageText);

        if (json_receive_data.has("des")) {
            Description = json_receive_data.getString("des");
        }
        if (json_receive_data.has("prv")) {
            ImageLocation = new Location(json_receive_data.getString("prv"));
            ImageLocation.setLatitude(json_receive_data.getDouble("lat")); // please correct similar to GPS Message
            ImageLocation.setLongitude(json_receive_data.getDouble("lng"));
            ImageLocation.setAltitude(json_receive_data.getDouble("alt"));
            ImageLocation.setTime(json_receive_data.getLong("tim"));
            ImageLocation.setSpeed((float) json_receive_data.getDouble("spd"));
            ImageLocation.setBearing((float) json_receive_data.getDouble("ber"));
            ImageLocation.setAccuracy((float) json_receive_data.getDouble("acc"));
        }

        data = new byte[binarymessage.length - i - 1];
        System.arraycopy(binarymessage, i + 1, data, 0, data.length);
    }


    @Override
    public byte[] getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();

        json_data.accumulate("des", Description);
        if (ImageLocation != null) {
            json_data.accumulate("lat", ImageLocation.getLatitude());
            json_data.accumulate("lng", ImageLocation.getLongitude());
            json_data.accumulate("prv", ImageLocation.getProvider());
            json_data.accumulate("tim", ImageLocation.getTime());
            json_data.accumulate("alt", ImageLocation.getAltitude());
            json_data.accumulate("spd", ImageLocation.getSpeed());
            json_data.accumulate("ber", ImageLocation.getBearing());
            json_data.accumulate("acc", ImageLocation.getAccuracy());
        }


        return AndruavBinaryHelper.joinBinarywithText(json_data.toString(), data);
    }

}
