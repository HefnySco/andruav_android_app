package com.andruav.event.networkEvent;

/**
 * Created by M.Hefny on 04-Oct-14.
 */
public class EventSocketState {

    public enum ENUM_SOCKETSTATE  {onConnect,onDisconnect,onError,onMessage,onRegistered}

    public String Message;
    public ENUM_SOCKETSTATE SocketState;

    public EventSocketState (ENUM_SOCKETSTATE blip, String whichButtonColor)
    {
        Message = whichButtonColor;
        SocketState = blip;
    }
}
