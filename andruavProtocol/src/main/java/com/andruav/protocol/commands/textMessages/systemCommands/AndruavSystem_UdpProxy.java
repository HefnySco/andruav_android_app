package com.andruav.protocol.commands.textMessages.systemCommands;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class AndruavSystem_UdpProxy extends AndruavMessageBase {

    public final static int TYPE_AndruavSystem_UdpProxy = 9008;


    public String  address1;
    public String  address2;
    public int  port1;
    public int  port2;
    public boolean enabled = false;


    public AndruavSystem_UdpProxy () {
        messageTypeID = TYPE_AndruavSystem_UdpProxy;

        address1 = "0.0.0.0";
        address2 = "0.0.0.0";
        port1 = 0;
        port2 = 0;

    }

    @Override
    public void setMessageText(String messageText) throws JSONException, ParseException {

        JSONObject json_receive_data = new JSONObject(messageText);
        JSONObject server1, server2;

        server1 = json_receive_data.getJSONObject("socket1");
        address1 = server1.getString("address");
        port1 = server1.getInt("port");

        server2 = json_receive_data.getJSONObject("socket2");
        address2 = server2.getString("address");
        port2 = server2.getInt("port");

        enabled = json_receive_data.getBoolean("en");
    }


    @Override
    public String getJsonMessage () throws org.json.JSONException
    {
        JSONObject json_data= new JSONObject();

        JSONObject json_socket1= new JSONObject();
        json_socket1.accumulate("address",address1);
        json_socket1.accumulate("port",port1);
        JSONObject json_socket2= new JSONObject();
        json_socket2.accumulate("address",address2);
        json_socket2.accumulate("port",port2);

        json_data.accumulate("socket1",json_socket1);
        json_data.accumulate("socket2",json_socket2);
        json_data.accumulate("en", enabled);
        return json_data.toString();
    }
}
