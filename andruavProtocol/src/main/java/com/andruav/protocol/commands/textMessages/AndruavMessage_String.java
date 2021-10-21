package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;

/**
 * Created by M.Hefny on 27-Oct-14.
 * <br>cmd: <b>1000</b>
 */
public class AndruavMessage_String extends AndruavMessageBase {

    public final static int TYPE_AndruavCMD_STRING = 0;

    public String MessageText;


    public AndruavMessage_String() {
        messageTypeID = TYPE_AndruavCMD_STRING;
    }

    public AndruavMessage_String(String messageText) {
        messageTypeID = TYPE_AndruavCMD_STRING;
        MessageText = messageText;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        MessageText = messageText;
    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        return MessageText;
    }

}
