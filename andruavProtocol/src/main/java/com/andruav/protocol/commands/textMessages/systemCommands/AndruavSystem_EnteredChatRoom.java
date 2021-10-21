package com.andruav.protocol.commands.textMessages.systemCommands;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

public class AndruavSystem_EnteredChatRoom extends AndruavMessageBase {

    public final static int TYPE_AndruavSystem_EnteredChatRoom = 9008;

    public AndruavSystem_EnteredChatRoom () {
        messageTypeID = TYPE_AndruavSystem_EnteredChatRoom;
    }


}
