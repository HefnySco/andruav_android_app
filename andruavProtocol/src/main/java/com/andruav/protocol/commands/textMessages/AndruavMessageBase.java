package com.andruav.protocol.commands.textMessages;

import org.json.JSONException;

import java.text.ParseException;

/**
 * Created by M.Hefny on 27-Oct-14.
 * <br>cmd: <b>1000</b>
 */
public class AndruavMessageBase {

    public final static int TYPE_AndruavCMD_BASE = 0;

    /////// INTERNAL USE

    public final static int DOMAIN_RESALA_INFO_MESSAGE    = 1;
    public final static int DOMAIN_RESALA_REMOTE_EXECUTE  = 2;


    protected int messageDomain;

    public int getMessageDomain() {
        return messageDomain;
    }
    // END OF INTERNAL USE


    public int messageTypeID;




    protected AndruavMessageBase() {
        messageTypeID = TYPE_AndruavCMD_BASE;
        messageDomain = DOMAIN_RESALA_INFO_MESSAGE;
    }

    /***
     * Takes string of the command and fills the attributes.
     *
     * @param messageText
     * @throws JSONException
     * @throws ParseException
     */
    public void setMessageText(String messageText) throws JSONException, ParseException {

    }

    /***
     * Serializes data
     *
     * @return
     * @throws org.json.JSONException
     */
    public String getJsonMessage() throws org.json.JSONException {
        return null;
    }
}