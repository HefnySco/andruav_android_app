package com.andruav.protocol.communication.uavos;

import com.andruav.AndruavFacadeBase;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.event.uavosModules.Event_UAVOSModuleUpdated;
import com.andruav.protocol.commands.Andruav_Parser;
import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.protocol.commands.textMessages.AndruavMessageBase;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Signaling;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;
import com.andruav.protocol.commands.textMessages.uavosCommands.AndruavModule_ID;
import com.andruav.uavos.modules.UAVOSException;
import com.andruav.uavos.modules.UAVOSMapBase;
import com.andruav.uavos.modules.UAVOSModuleBuilderBase;
import com.andruav.uavos.modules.UAVOSModuleUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.InetAddress;



/**
 * Created by M.Hefny on 08-Jan-15.
 * @author mhefny
 * This class is responsoble for UDP activities between andruav devices that exist on the same LAN.
 * It has the sender and listerner sockets.
 */
public abstract class AndruavUDPServerBase extends AndruavUDPBase {

    /***
     * Default Port used for UDP Server
     */
    public static int defaultUDPPort = AndruavSettings.DEFAULT_ANDRUAV_LAN_UDP_PORT;



    //////////BUS EVENT



    ///////////////////

    public AndruavUDPServerBase(InetAddress address, int port)  {
        super(port);
    }


    public AndruavUDPServerBase(InetAddress address) {
        super();
    }




    /***
     *Sends Andruav Text Data
     * @param destAddress
     * @param destPort
     * @param andruavMessageBase
     */
    protected void sendMessageToModule(final InetAddress destAddress, final int destPort, final AndruavMessageBase andruavMessageBase) {
        try {
            final Andruav_2MR andruav_2MR = new Andruav_2MR();
            andruav_2MR.MessageRouting = ProtocolHeaders.CMD_TYPE_INTERMODULE;

            andruav_2MR.andruavMessageBase = andruavMessageBase;

            sendMessageToModule(destAddress, destPort,andruav_2MR);

            return ;

        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException("exception-udp", ex);

        }
    }


    /**
     * forward messages from AndruavServer to UAVOS Modules.
     * TODO: you can enhance this part by filtering signalling messages and send it only to camera module owner not to all camera modules.
     * @param andruav2MR
     */
    public void sendMessageToModule (final Andruav_2MR andruav2MR)
    {
        try {
            final int messageID = andruav2MR.andruavMessageBase.messageTypeID;
            final UAVOSMapBase uavosMapBase = AndruavEngine.getUAVOSMapBase();
            final int moduleCount = uavosMapBase.size();
            for (int i=0;  i< moduleCount; ++i)
            {
                final UAVOSModuleUnit uavosModuleUnit2 = uavosMapBase.valueAt(i);
                if (uavosModuleUnit2.BuiltInModule) continue;
                final JSONArray jsonArray = uavosModuleUnit2.ModuleCapturedMessages;

                if (jsonArray == null) continue;

                final int len = jsonArray.length();
                for (int j=0; j< len; ++j)
                {
                    try {
                        final int requestedMessageID = (int) jsonArray.get(j);
                        if (requestedMessageID == messageID)
                        {
                            sendMessageToModule(uavosModuleUnit2, andruav2MR);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

            }

        } catch (UAVOSException e) {
            return ;
        }
    }

    public void sendMessageToModule (final UAVOSModuleUnit uavosModuleUnit, final Andruav_2MR andruav2MR)
    {


        sendMessageToModule(uavosModuleUnit.ModuleAddress, uavosModuleUnit.Port, andruav2MR);
    }

    /***
     *Sends Andruav Text Data
     * @param destAddress
     * @param destPort
     * @param andruav2MR
     */
    protected void sendMessageToModule(final InetAddress destAddress, final int destPort, final Andruav_2MR andruav2MR) {
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



    protected Andruav_2MR getCommunicatorID ()
    {
        final Andruav_2MR andruav_2MR = new Andruav_2MR();
        andruav_2MR.MessageRouting = ProtocolHeaders.CMD_TYPE_INTERMODULE;

        final AndruavModule_ID andruavModule_id = new AndruavModule_ID();
        andruavModule_id.ModuleId = ProtocolHeaders.UAVOS_COMM_MODULE_ID;
        andruavModule_id.ModuleClass = ProtocolHeaders.UAVOS_COMM_MODULE_CLASS;
        try {
            final JSONObject json_e = new JSONObject();
            json_e.accumulate("sd", AndruavSettings.andruavWe7daBase.PartyID);
            json_e.accumulate("gr", AndruavSettings.andruavWe7daBase.GroupName);
            andruavModule_id.ModuleMessage = json_e;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        andruav_2MR.andruavMessageBase = andruavModule_id;

        return andruav_2MR;
    }



    /***
     * Called when data is recieved from Socket
     * @param packet
     * @param buffer
     * @param len
     */
    @Override
    protected void onData(final DatagramPacket packet,final byte[] buffer, final int len)
    {
        final String msg = new String (packet.getData());
        try {

            final Andruav_2MR andruav_2MR = Andruav_Parser.parseText(msg);

            if (andruav_2MR.MessageRouting.equals(ProtocolHeaders.CMD_TYPE_INTERMODULE)) {

                processInterModuleMessages(andruav_2MR, packet.getAddress(), packet.getPort());
            }
            else
            {
                forwardMessageFromModuleToCommServer(andruav_2MR);
            }



            return ;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

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

                    if (andruavModule_id.SendBack)
                    {
                        sendMessageToModule(moduleAddress,port,getCommunicatorID());
                    }

                    UAVOSModuleUnit uavosModuleUnit =  AndruavEngine.getUAVOSMapBase().get(andruavModule_id.ModuleId);
                    if (uavosModuleUnit == null)
                    {
                        uavosModuleUnit = UAVOSModuleBuilderBase.getModule(andruavModule_id.ModuleClass);
                        uavosModuleUnit.ModuleId = andruavModule_id.ModuleId;
                        uavosModuleUnit.ModuleKey = andruavModule_id.ModuleKey;
                        uavosModuleUnit.ModuleFeatures = andruavModule_id.ModuleFeatures;
                        uavosModuleUnit.ModuleCapturedMessages = andruavModule_id.ModuleCapturedMessages;
                        uavosModuleUnit.ModuleAddress = moduleAddress;
                        uavosModuleUnit.setModuleMessages(andruavModule_id.ModuleMessage);
                        uavosModuleUnit.Port = port;
                        uavosModuleUnit.lastActiveTime = System.currentTimeMillis();
                        AndruavEngine.getUAVOSMapBase().put(andruavModule_id.ModuleId,uavosModuleUnit);
                        onModuleAdded (andruav_2MR, uavosModuleUnit);
                    }
                    else
                    {
                        uavosModuleUnit.ModuleKey = andruavModule_id.ModuleKey;
                        uavosModuleUnit.ModuleFeatures = andruavModule_id.ModuleFeatures;
                        uavosModuleUnit.ModuleCapturedMessages = andruavModule_id.ModuleCapturedMessages;                        uavosModuleUnit.ModuleAddress = moduleAddress;
                        uavosModuleUnit.setModuleMessages(andruavModule_id.ModuleMessage);
                        uavosModuleUnit.Port = port;
                        uavosModuleUnit.lastActiveTime = System.currentTimeMillis();
                        onModuleUpdated (andruav_2MR, uavosModuleUnit);
                        AndruavEngine.getEventBus().post(new Event_UAVOSModuleUpdated(uavosModuleUnit));
                    }
                    break;

                default:
                    onMessageReceivedFromModuleForInternalProcessing(andruav_2MR);
                    break;
            }

            return ;
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Modules sends messages that should be processed by communicator module as MsgType == CMD_TYPE_INTERMODULE.
     * Communicator can forward this message or keep it.
     * For example parts if TYPE_AndruavMessage_ID is recieved from sub modules especially FCB module.
     **/
    protected void onMessageReceivedFromModuleForInternalProcessing (final Andruav_2MR andruav_2MR)
    {
        switch (andruav_2MR.andruavMessageBase.messageTypeID) {

            case AndruavMessage_Signaling.TYPE_AndruavMessage_Signaling:
                //final AndruavResala_Signaling andruavResala_signaling = (AndruavResala_Signaling) andruav_2MR.andruavResalaBase;
                // add to video requester for this camera
                forwardMessageFromModuleToCommServer(andruav_2MR);
                break;

            default:
                forwardMessageFromModuleToCommServer(andruav_2MR);
            break;
        }
    }


    /**
     * Module sends this message without (MsgType != CMD_TYPE_INTERMODULE) processing it.
     * Note that same message ID can be sent as internal processing
     * or requires no processing based on:
     * MsgType ==  global.protocolMessages.CMD_TYPE_INTERMODULE
     */
    protected void forwardMessageFromModuleToCommServer(final Andruav_2MR andruav_2MR)
    {
        AndruavFacadeBase.sendMessage(andruav_2MR.andruavMessageBase, andruav_2MR.targetName, false);
    }


    protected abstract void onModuleAdded (final Andruav_2MR andruav_2MR, final UAVOSModuleUnit uavosModuleUnit);


    protected abstract void onModuleUpdated (final Andruav_2MR andruav_2MR, final UAVOSModuleUnit uavosModuleUnit);




}
