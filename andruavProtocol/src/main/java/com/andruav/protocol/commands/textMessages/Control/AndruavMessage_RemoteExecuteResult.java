package com.andruav.protocol.commands.textMessages.Control;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * cmd: <b>1084</b>
 * <br>Generic result/ack reply for an {@link AndruavMessage_RemoteExecute}
 * command that has no dedicated status message of its own. Sent back to the
 * requesting party by whichever unit executed the command.
 * <br>Module-owned commands keep replying on their own STATUS message
 * (e.g. GPIO_STATUS); this is the catch-all ack for the rest.
 */
public class AndruavMessage_RemoteExecuteResult extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_RemoteExecute_Result = 1084;

    /*** command executed / dispatched */
    public final static int RESULT_OK = 0;
    /*** generic failure (see description) */
    public final static int RESULT_ERROR = 1;
    /*** no handler available to execute the command */
    public final static int RESULT_NOT_READY = 2;
    /*** refused (e.g. sender cannot control this unit) */
    public final static int RESULT_REJECTED = 3;
    /*** required location unavailable */
    public final static int RESULT_NO_LOCATION = 4;
    /*** device lacks the capability (e.g. no SMS hardware) */
    public final static int RESULT_NO_CAPABILITY = 5;
    /*** missing OS/user permission */
    public final static int RESULT_PERMISSION_DENIED = 6;
    /*** empty/invalid target (e.g. phone number) */
    public final static int RESULT_NO_RECIPIENT = 7;

    /***
     * Echoed RemoteCommand id being answered,
     * e.g. {@link AndruavMessage_RemoteExecute#RemoteCommand_SMSwGPS}.
     */
    public int RemoteCommandID;
    /***
     * Result code, one of RESULT_* constants. 0 means executed/queued.
     */
    public int resultCode;
    /***
     * Optional human-readable detail (e.g. "no location").
     */
    public String Description;
    /***
     * Echoed request id if the command carried one. -1 means absent.
     */
    public int requestId = -1;

    public AndruavMessage_RemoteExecuteResult() {
        super();
        messageTypeID = TYPE_AndruavMessage_RemoteExecute_Result;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);
        RemoteCommandID = json_receive_data.getInt("C");
        resultCode = json_receive_data.getInt("er");
        if (json_receive_data.has("DS")) Description = json_receive_data.getString("DS");
        if (json_receive_data.has("rq")) requestId = json_receive_data.getInt("rq");
    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("C", RemoteCommandID);
        json_data.accumulate("er", resultCode);
        if (Description != null) json_data.accumulate("DS", Description);
        if (requestId >= 0) json_data.accumulate("rq", requestId);

        return json_data.toString();
    }
}
