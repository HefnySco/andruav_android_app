package com.andruav.protocol.commands.textMessages.systemCommands;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by mhefny on 6/16/16.
 * Load saved Tasks based on a criteria.
 */
public class AndruavSystem_LoadTasks extends AndruavMessageBase {

    public final static int TYPE_AndruavSystem_LoadTasks = 9001;


    public int largerThan_SID;
    public String accessCode;
    public String accountID;
    public String party_sid;
    public String groupName;
    public String sender;
    public String receiver;
    public String messageType;
    private String task;
    public boolean isPermanent;


    public AndruavSystem_LoadTasks (int largerThan_SID,
                                    String accessCode,
                                    String accountID,
                                    String party_sid,
                                    String groupName,
                                    String sender,
                                    String receiver,
                                    String messageType,
                                    boolean isPermanent) throws Exception {
        this.largerThan_SID = largerThan_SID;
        this.party_sid = party_sid;
        this.accessCode = accessCode;
        this.accountID = accountID;
        this.groupName = groupName;
        this.sender = sender;
        this.receiver = receiver;
        this.messageType = messageType;
        this.isPermanent = isPermanent;

        messageTypeID = TYPE_AndruavSystem_LoadTasks;
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
        if (accessCode != null)     json_data.accumulate("ac",accessCode);
        if (accountID != null)      json_data.accumulate("ai",accountID);
        if (groupName != null)      json_data.accumulate("gn",groupName);
        if (sender != null)         json_data.accumulate("s",sender);
        if (receiver != null)       json_data.accumulate("r",receiver);
        if (messageType != null)    json_data.accumulate("mt",messageType);
        final int v = (isPermanent)?1:0;
        json_data.accumulate("ip",v);

        return json_data.toString();
    }

}