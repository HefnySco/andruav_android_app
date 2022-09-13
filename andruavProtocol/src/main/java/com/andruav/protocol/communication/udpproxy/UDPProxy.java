package com.andruav.protocol.communication.udpproxy;

import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_REGISTERED;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.event.fcb_event.Event_SocketData;
import com.andruav.protocol.communication.udpserver.UDPServerBase;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class UDPProxy extends UDPServerBase {


    public UDPProxy ()
    {
        super(0);
    }

    @Override
    public void broadCast() throws IllegalAccessException {

    }

    public void sendMessage(final byte[] msg, final int length) {
        if (!AndruavSettings.andruavWe7daBase.isUdpProxyEnabled()) return ;
        {
            InetAddress destAddress = AndruavSettings.andruavWe7daBase.getUdp_iaddress_socket_unit();
            if (destAddress==null) return ;

            send (destAddress, AndruavSettings.andruavWe7daBase.getUdp_socket_port_unit(), msg, length);
        }
    }


    @Override
    protected void onData(final DatagramPacket packet, final byte[] buffer, final int len) {
        final int status = AndruavEngine.getAndruavWSStatus();
        if (status != SOCKETSTATE_REGISTERED) return ;

        Event_SocketData event_socketData = new Event_SocketData();
        event_socketData.IsLocal = Event_SocketData.SOURCE_REMOTE;
        event_socketData.Data = buffer;
        event_socketData.DataLength = len;
        AndruavSettings.andruavWe7daBase.updateUdpProxyLastReceiveTime();
        AndruavEngine.getEventBus().post(event_socketData);
    }
}