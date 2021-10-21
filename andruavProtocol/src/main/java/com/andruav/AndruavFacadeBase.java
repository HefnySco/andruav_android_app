package com.andruav;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinaryBase;
import com.andruav.protocol.commands.textMessages.AndruavMessageBase;
import com.andruav.protocol.communication.uavos.AndruavUDPModuleBase;
import com.andruav.protocol.communication.websocket.AndruavWSClientBase;

/**
 * Created by mhefny on 12/25/16.
 */

public class AndruavFacadeBase {


    public static void sendMessage(final AndruavMessageBase andruavMessageBase, final AndruavUnitBase andruavUnitBase, final boolean instant)
    {
        if (AndruavSettings.andruavWe7daBase.mIsModule)
        {
            ((AndruavUDPModuleBase)(AndruavEngine.getAndruavUDP())).sendMessageToServer(andruavMessageBase, andruavUnitBase);
        }
        else
        {
            sendMessageToCommServer(andruavMessageBase, andruavUnitBase, instant);
        }
    }

    public static void sendMessage(final AndruavResalaBinaryBase andruavResalaBinaryBase, final AndruavUnitBase andruavUnitBase, final boolean instant)
    {
        if (AndruavSettings.andruavWe7daBase.mIsModule)
        {
            ((AndruavUDPModuleBase)(AndruavEngine.getAndruavUDP())).sendMessageToServer(andruavResalaBinaryBase, andruavUnitBase);
        }
        else {
            sendMessageToCommServer(andruavResalaBinaryBase, andruavUnitBase, instant);
        }
    }

    public static void sendMessage(final AndruavMessageBase andruavMessageBase, final String target, final boolean instant)
    {
        if (AndruavSettings.andruavWe7daBase.mIsModule)
        {
            ((AndruavUDPModuleBase)(AndruavEngine.getAndruavUDP())).sendMessageToServer(andruavMessageBase,target);
        }
        else {
            sendMessageToCommServer(andruavMessageBase, target, instant);
        }
    }

    public static void sendSystemCommandToCommServer(final AndruavMessageBase andruavMessageBase, final boolean addTime, final boolean instant) {
        if (AndruavEngine.getAndruavWS() == null) return; // not connected yet
        if (AndruavEngine.isAndruavWSStatus(AndruavWSClientBase.SOCKETSTATE_REGISTERED)) {
            AndruavEngine.getAndruavWS().sendSysCMD (andruavMessageBase, addTime, instant);
        }
    }


    private static void sendMessageToCommServer(final AndruavMessageBase andruavMessageBase, final String target, final boolean instant)
    {
        //TODO: maybe you want to queue these messages
        if (AndruavEngine.getAndruavWS()==null) return ; // not connected yet
        if (AndruavEngine.isAndruavWSStatus(AndruavWSClientBase.SOCKETSTATE_REGISTERED)) {
            if (target == null) {
                AndruavEngine.getAndruavWS().broadcastMessageToGroup(andruavMessageBase, false,instant);
            } else {
                AndruavEngine.getAndruavWS().sendMessageToIndividual(andruavMessageBase, target,false, instant);
            }
        }
    }

    private static void sendMessageToCommServer(final AndruavMessageBase andruavMessageBase, final AndruavUnitBase andruavUnitBase, final boolean instant)
    {
        //TODO: maybe you want to queue these messages
        if (AndruavEngine.getAndruavWS()==null) return ; // not connected yet
        if (AndruavEngine.isAndruavWSStatus(AndruavWSClientBase.SOCKETSTATE_REGISTERED)) {
            if (andruavUnitBase == null) {
                AndruavEngine.getAndruavWS().broadcastMessageToGroup(andruavMessageBase, false,instant);
            } else {
                AndruavEngine.getAndruavWS().sendMessageToIndividual(andruavMessageBase, andruavUnitBase.PartyID,false, instant);
            }
        }
    }




    private static void sendMessageToCommServer(final AndruavResalaBinaryBase andruavResalaBinaryBase, final AndruavUnitBase andruavUnitBase, final boolean instant)
    {
        //TODO: maybe you want to queue these messages
        if (AndruavEngine.getAndruavWS()==null) return ; // not connected yet
        if (AndruavEngine.isAndruavWSStatus(AndruavWSClientBase.SOCKETSTATE_REGISTERED)) {
            if (andruavUnitBase == null) {
                AndruavEngine.getAndruavWS().broadcastMessageToGroup(andruavResalaBinaryBase, false);
            } else {
                AndruavEngine.getAndruavWS().sendMessageToIndividual(andruavResalaBinaryBase, andruavUnitBase.PartyID, false,instant);
            }
        }
    }
}
