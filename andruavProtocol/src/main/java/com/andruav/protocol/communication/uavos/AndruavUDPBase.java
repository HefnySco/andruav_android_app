package com.andruav.protocol.communication.uavos;

import com.andruav.protocol.commands.textMessages.Andruav_2MR;
import com.andruav.protocol.communication.udpserver.UDPServerBase;

import java.net.InetAddress;

public abstract class AndruavUDPBase extends UDPServerBase {
    public AndruavUDPBase(int port) {
        super(port);
    }

    public AndruavUDPBase() {
        super();
    }


    protected abstract void processInterModuleMessages(final Andruav_2MR andruav_2MR, final InetAddress moduleAddress, final int port);
}