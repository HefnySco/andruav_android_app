package com.andruav.protocol.commands.textMessages;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class AndruavMessage_UDPProxy_Info extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_UdpProxy_Info = 1071;

    public String  udp_ip;
    public int     udp_port;
    public int     udp_optimization_level = -1;
    public boolean udp_enabled = false;

    public boolean udp_paused  = false;

    public AndruavMessage_UDPProxy_Info () {
        messageTypeID = TYPE_AndruavMessage_UdpProxy_Info;

    }

    public AndruavMessage_UDPProxy_Info (final String ip, final int port, final int optimization_level, final boolean enabled, final boolean paused) {
        messageTypeID = TYPE_AndruavMessage_UdpProxy_Info;

        udp_ip = ip;
        udp_port = port;
        udp_enabled = enabled;
        udp_paused = paused;
        udp_optimization_level = optimization_level;
    }

    @Override
    public void setMessageText(String messageText) throws JSONException, ParseException {

        JSONObject json_receive_data = new JSONObject(messageText);
        JSONObject server1, server2;

        udp_enabled = json_receive_data.getBoolean("en");

        if (json_receive_data.has("z")) {
            udp_paused = json_receive_data.getBoolean("z");
        }

        if (json_receive_data.has("a")) {
            udp_ip = json_receive_data.getString("a");
        }

        if (json_receive_data.has("o")) {
            udp_optimization_level = json_receive_data.getInt("o");
        }

        if (json_receive_data.has("p")){
            udp_port = json_receive_data.getInt("p");
        }

    }


    @Override
    public String getJsonMessage () throws org.json.JSONException
    {
        JSONObject json_data= new JSONObject();

        json_data.accumulate("a",udp_ip);
        json_data.accumulate("p",udp_port);
        json_data.accumulate("o", udp_optimization_level);
        json_data.accumulate("en", udp_enabled);
        json_data.accumulate("z", udp_paused);

        return json_data.toString();
    }
}
