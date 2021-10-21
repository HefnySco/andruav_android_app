package com.andruav.event.networkEvent;

import androidx.collection.SimpleArrayMap;

/**
 * Created by M.Hefny on 12-Oct-14.
 */
public class EventLoginClient {

    public  int LastError;
    public  String LastMessage;
    public  int Cmd;
    public String AccountName;
    public String AccessCode;

    public SimpleArrayMap<String,String> Parameters;


    public EventLoginClient ()
    {

    }

    public EventLoginClient (int actionViewGo, String classic, String i, int lastError,String msgLength,SimpleArrayMap<String,String> msg)
    {
        Cmd = actionViewGo;
        AccountName = classic;
        AccessCode = i;
        LastError = lastError;
        LastMessage = msgLength;
        Parameters = msg;


    }

}
