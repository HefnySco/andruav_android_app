package com.andruav.event.fcb_event;

/**
 * Created by M.Hefny on 29-Dec-14.
 */
public class Event_SocketAction {
    public static final int SOCKETACTION_STARTED                = 1;
    public static final int SOCKETACTION_CLOSED                 = 2;
    public static final int SOCKETACTION_CLIENT_CONNECTED       = 4;
    public static final int SOCKETACTION_CLIENT_DISCONNECTED    = 5;

    public int socketAction;
    public String clientSocketIP;


    public Event_SocketAction(int remoteControlSetting)
    {
        this.socketAction = remoteControlSetting;

    }





}
