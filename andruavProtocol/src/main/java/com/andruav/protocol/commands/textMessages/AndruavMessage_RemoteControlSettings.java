package com.andruav.protocol.commands.textMessages;

import com.andruav.event.fcb_7adath._7adath_FCB_RemoteControlSettings;

import org.json.JSONException;
import org.json.JSONObject;


/***
 * Define remote control mode:
 *  {@link _7adath_FCB_RemoteControlSettings#RC_SUB_ACTION_CENTER_CHANNELS}
 *  {@link _7adath_FCB_RemoteControlSettings#RC_SUB_ACTION_CENTER_CHANNELS}
 *  {@link _7adath_FCB_RemoteControlSettings#RC_SUB_ACTION_FREEZE_CHANNELS}
 *  {@link _7adath_FCB_RemoteControlSettings#RC_SUB_ACTION_JOYSTICK_CHANNELS}
 *  {@link _7adath_FCB_RemoteControlSettings#RC_SUB_ACTION_JOYSTICK_CHANNELS_GUIDED}
 *
 * triggers when received event {@link _7adath_FCB_RemoteControlSettings}
 */
public class AndruavMessage_RemoteControlSettings extends AndruavMessageBase {
    public final static int TYPE_RemoteControlSettings = 1047;


    /*
     * check _7adath_FCB_RemoteControlSettings for event Details
     */
    public int rcSubAction;

    public AndruavMessage_RemoteControlSettings() {
        super();
        messageTypeID = TYPE_RemoteControlSettings;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);
        rcSubAction = json_receive_data.getInt("b");
    }

    /***
     * You can fill the data using direct throttle variable of using setData
     * that is why variables are used to fill data so it is valid all time.
     *
     * @return
     * @throws org.json.JSONException
     */
    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("b", rcSubAction);

        return json_data.toString();
    }
}
