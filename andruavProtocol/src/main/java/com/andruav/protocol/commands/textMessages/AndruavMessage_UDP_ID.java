package com.andruav.protocol.commands.textMessages;

import com.andruav.andruavUnit.AndruavUnitBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by M.Hefny on 20-Jul-15.
 * <br>cmd: <b>1011</b>
 * <br>This is ID message used for UDP packets
 * <br>The SourceIP is saved together with ID & Group.
 */
@Deprecated
public class AndruavMessage_UDP_ID extends AndruavMessageBase {

    public final static int TYPE_AndruavCMD_UDP_ID = 1011;

    public String UnitID;

    public String Group;

    public AndruavMessage_UDP_ID() {
        messageTypeID = TYPE_AndruavCMD_UDP_ID;
    }

    public AndruavMessage_UDP_ID(final AndruavUnitBase andruavUnitBase) {
        this();
        Group = andruavUnitBase.GroupName;
        UnitID = andruavUnitBase.UnitID;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        UnitID = json_receive_data.getString("ID");
        Group = json_receive_data.getString("Gr");
    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("ID", UnitID);
        json_data.accumulate("Gr", Group);

        return json_data.toString();
    }

}
