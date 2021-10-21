/***********************************************************
 * Author: Mohammad S. Hefny
 * Date: Feb 2020
 */
package com.andruav.protocol.communication.uavos;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.protocol.commands.Andruav_Parser;
import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.protocol.commands.binaryMessages.AndruavBinary_2MR;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinaryBase;
import com.andruav.protocol.commands.textMessages.AndruavMessageBase;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;
import com.andruav.protocol.commands.textMessages.uavosCommands.AndruavModule_ID;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;


public abstract class AndruavUDPModuleBase extends AndruavUDPBase {

    public static int defaultUDPPort = AndruavSettings.DEFAULT_ANDRUAV_LAN_MODULE_UDP_PORT;
    public static int defaultUDPServerPort = AndruavSettings.DEFAULT_ANDRUAV_LAN_UDP_PORT;
    protected static InetAddress defaultUDPIP = null;


    private void initIP ()
    {
        final String ip = AndruavEngine.getPreference().getCommModuleIP();
        if (!ip.isEmpty())
        {
            try {
                defaultUDPIP = InetAddress.getByName(ip);
            } catch (UnknownHostException e) {
                defaultUDPIP = null;
            }
        }
    }


    public AndruavUDPModuleBase ()
    {
        super ();

        initIP ();
    }

    public AndruavUDPModuleBase(int port) {
        super(port);

        initIP ();
    }



    public void sendMessageToServer (final AndruavMessageBase andruavMessageBase, final AndruavUnitBase andruavUnitBase)
    {
        String target=null;
        if (andruavUnitBase != null)
        {
            target = andruavUnitBase.PartyID;
        }
        sendMessageToServer (andruavMessageBase, target);
    }



    public void sendMessageToServer (final AndruavMessageBase andruavMessageBase, final String target)
    {

        try {
            final Andruav_2MR andruav_2MR = new Andruav_2MR();
            andruav_2MR.MessageRouting = ProtocolHeaders.CMD_TYPE_INTERMODULE;
            if (target != null) {
                // no need to set message type as it will be set by CommModule
                // just tell him to whome you want to send it.
                andruav_2MR.targetName = target;
            }
            andruav_2MR.andruavMessageBase = andruavMessageBase;

            sendMessageToServer(defaultUDPIP, defaultUDPServerPort, andruav_2MR);
        }
        catch (final Exception e)
        {

        }
    }



    public void sendMessageToServer (final AndruavResalaBinaryBase andruavResalaBinaryBase, final AndruavUnitBase andruavUnitBase)
    {
        try {
            final AndruavBinary_2MR andruav_2MR = new AndruavBinary_2MR();
            andruav_2MR.MessageRouting = ProtocolHeaders.CMD_TYPE_INTERMODULE;
            if (andruavUnitBase != null)
            {
                // no need to set message type as it will be set by CommModule
                // just tell him to whome you want to send it.
                andruav_2MR.targetName = andruavUnitBase.PartyID;
            }
            andruav_2MR.andruavResalaBinaryBase = andruavResalaBinaryBase;
        }
        catch (final Exception e) {
        }
    }

    /***
     *Sends Andruav Text Data
     * @param destAddress
     * @param destPort
     * @param andruav2MR
     */
    protected void sendMessageToServer(final InetAddress destAddress, final int destPort, final Andruav_2MR andruav2MR) {
        try {
            final String msg = andruav2MR.getJscon(false);
            send (destAddress,destPort,msg.getBytes(), msg.length());
            return ;
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException("exception-udp", ex);

        }
    }


    protected abstract Andruav_2MR getModuleID ();

    @Override
    protected void onData(DatagramPacket packet, byte[] Buffer, int len) {
        final String msg = new String (packet.getData());
        try {

            final Andruav_2MR andruav_2MR = Andruav_Parser.parseText(msg);

            if (andruav_2MR.MessageRouting.equals(ProtocolHeaders.CMD_TYPE_INTERMODULE)) {

                processInterModuleMessages(andruav_2MR, packet.getAddress(), packet.getPort());
            }
            else
            {
                onMessageReceivedFromServerForInternalProcessing(andruav_2MR);
            }



            return ;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Modules sends messages that should be processed by communicator module as MsgType == CMD_TYPE_INTERMODULE.
     * Communicator can forward this message or keep it.
     * For example parts if TYPE_AndruavMessage_ID is recieved from sub modules especially FCB module.
     **/
    protected abstract void onMessageReceivedFromServerForInternalProcessing (final Andruav_2MR andruav_2MR);


    /**
     * Process Messages that is sent with MsgType == CMD_TYPE_INTERMODULE
     * @param andruav_2MR
     * @param moduleAddress
     * @param port
     */
    @Override
    protected void processInterModuleMessages(final Andruav_2MR andruav_2MR, final InetAddress moduleAddress, final int port) {
        try {
            switch (andruav_2MR.andruavMessageBase.messageTypeID) {

                case AndruavModule_ID.TYPE_AndruavModule_ID:
                    final AndruavModule_ID andruavModule_id = (AndruavModule_ID) andruav_2MR.andruavMessageBase;

                    defaultUDPIP = moduleAddress;
                    // dont change preference settings here. as it could be left blank on purpose to allow broadcast discovery.
                    //AndruavMo7arek.getPreference().setCommModuleIP (defaultUDPIP.getHostAddress());

                    if (andruavModule_id.SendBack) {
                        //sendMessageToModule(moduleAddress,port,getCommunicatorID());
                    }

                    /*UAVOSModuleUnit uavosModuleUnit = AndruavMo7arek.getUAVOSMapBase().get(andruavModule_id.ModuleId);
                    if (uavosModuleUnit == null) {
                        uavosModuleUnit = UAVOSModuleBuilderBase.getModule(andruavModule_id.ModuleClass);
                        uavosModuleUnit.ModuleId = andruavModule_id.ModuleId;
                        uavosModuleUnit.ModuleFeatures = andruavModule_id.ModuleFeatures;
                        uavosModuleUnit.ModuleCapturedMessages = andruavModule_id.ModuleCapturedMessages;
                        uavosModuleUnit.ModuleAddress = moduleAddress;
                        uavosModuleUnit.setModuleMessages(andruavModule_id.ModuleMessage);
                        uavosModuleUnit.Port = port;
                        uavosModuleUnit.lastActiveTime = System.currentTimeMillis();
                        AndruavMo7arek.getUAVOSMapBase().put(andruavModule_id.ModuleId, uavosModuleUnit);
                        //onModuleAdded (andruav_2MR, uavosModuleUnit);
                    } else {
                        uavosModuleUnit.ModuleFeatures = andruavModule_id.ModuleFeatures;
                        uavosModuleUnit.ModuleCapturedMessages = andruavModule_id.ModuleCapturedMessages;
                        uavosModuleUnit.ModuleAddress = moduleAddress;
                        uavosModuleUnit.setModuleMessages(andruavModule_id.ModuleMessage);
                        uavosModuleUnit.Port = port;
                        uavosModuleUnit.lastActiveTime = System.currentTimeMillis();
                        //onModuleUpdated (andruav_2MR, uavosModuleUnit);
                        AndruavMo7arek.getEventBus().post(new Event_UAVOSModuleUpdated(uavosModuleUnit));
                    }*/
                    break;

                default:
                    onMessageReceivedFromServerForInternalProcessing(andruav_2MR);
                    break;
            }

            return;
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
