package com.andruav.protocol.commands.textMessages.systemCommands;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by mhefny on 6/18/16.
 */
public class AndruavSystem_SaveTasks extends AndruavMessageBase {

    public final static int TYPE_AndruavSystem_SaveTasks = 9002;

    public int largerThan_SID;
    public String accessCode;
    public String accountID;
    public String groupName;
    public String party_sid;
    public String sender;
    public String receiver;
    public String messageType;
    private final String task;
    public boolean isPermanent;


    public AndruavSystem_SaveTasks (int largerThan_SID,
                                    String accessCode,
                                    String party_sid,
                                    String sender,
                                    String receiver,
                                    String messageType,
                                    Andruav_2MR andruav2MR,
                                    boolean isPermanent) throws Exception {
        this.largerThan_SID = largerThan_SID;
        this.party_sid = party_sid;
        this.accessCode = accessCode;
        this.sender = sender;
        this.receiver = receiver;
        this.messageType = messageType;
        this.task = andruav2MR.getJscon(false);
        this.isPermanent = isPermanent;

        messageTypeID = TYPE_AndruavSystem_SaveTasks;
    }

    public String  getTask()
    {
        return task;
    }

    @Override
    public void setMessageText(String messageText) throws JSONException, ParseException {


    }


    @Override
    public String getJsonMessage () throws org.json.JSONException
    {
        //Gson gson = new Gson();
        JSONObject json_data= new JSONObject();

        if (largerThan_SID > 0)     json_data.accumulate("lts",largerThan_SID);
        if (party_sid != null)      json_data.accumulate("ps",party_sid);
        if (accessCode != null)      json_data.accumulate("ac",accessCode);
        if (accountID != null)      json_data.accumulate("ai",accountID);
        if (groupName != null)      json_data.accumulate("gn",groupName);
        if (sender != null)         json_data.accumulate("s",sender);
        if (receiver != null)       json_data.accumulate("r",receiver);
        if (messageType != null)    json_data.accumulate("mt",messageType);
        if (task != null)           json_data.accumulate("t",task);
        final int v = (isPermanent)?1:0;
        json_data.accumulate("ip",v);

        return json_data.toString();
    }

}