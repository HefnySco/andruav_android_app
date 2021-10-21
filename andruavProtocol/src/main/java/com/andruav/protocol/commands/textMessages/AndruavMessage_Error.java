package com.andruav.protocol.commands.textMessages;

import com.andruav.interfaces.INotification;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by M.Hefny on 07-Feb-15.
 * <br>cmd: <b>1008</b>
 * <br> This command is used to report error, or warning status to other units.
 */
public class AndruavMessage_Error extends AndruavMessageBase {

    /***
     * request from an individual andruav to sendMessageToModule complete ID info.
     */
    public final static int TYPE_AndruavMessage_Error = 1008;


    /***
     * Camera Error
     */
    public final static int ERROR_CAMERA                = 1;
    /***
     * FCB Communication Error
     */
    public final static int ERROR_BLUETOOTH             = 2;
    /***
     * USB Errors
     */
    public final static int ERROR_USBERROR              = 3;
    /***
     * KML Files Errors.
     */
    public final static int ERROR_KMLERROR              = 4;

    public final static int ERROR_Lo7etTa7akom          = 5;
    public final static int ERROR_DJI                   = 6;
    public final static int ERROR_3DR                   = 7;
    public final static int ERROR_UDP                   = 8;
    public final static int ERROR_TCP                   = 9;
    public final static int ERROR_GPS                   = 10;
    public final static int ERROR_POWER                 = 11;
    public final static int ERROR_RCControl             = 12;
    public final static int ERROR_MODULES               = 13;
    public final static int ERROR_NAVIGATION            = 14;



    public final static int ERROR_GEOFENCEERROR         =100;


    /***
     * error No
     */
    public int errorNo;
    /***
     * from {@link INotification} INFO_TYPES
     * <br>When display a warning for example as a notification in tool bar this is used as its ID, so we keep an icon for each notification.
     */
    public int infoType;
    /***
     * from {@link INotification} INFO_TYPES
     * <br>public final static int NOTIFICATION_TYPE_ERROR = 1;
     * <br>public final static int NOTIFICATION_TYPE_WARNING = 2;
     * <br>public final static int NOTIFICATION_TYPE_NORMAL = 3;
     */
    public int notification_Type;
    /***
     * Text description
     */
    public String Description;

    public AndruavMessage_Error() {
        super();
        messageTypeID = TYPE_AndruavMessage_Error;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);
        errorNo = json_receive_data.getInt("EN");
        infoType = json_receive_data.getInt("IT");
        notification_Type = json_receive_data.getInt("NT");
        Description = json_receive_data.getString("DS");
    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("EN", errorNo);
        json_data.accumulate("IT", infoType);
        json_data.accumulate("NT", notification_Type);
        json_data.accumulate("DS", Description);

        return json_data.toString();
    }

}
