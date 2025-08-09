package com.andruav.protocol.commands.textMessages;

public class AndruavMessage_Sound_PlayFile extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_Sound_PlayFile = 6512;

    public AndruavMessage_Sound_PlayFile() {
        super();
        messageTypeID = TYPE_AndruavMessage_Sound_PlayFile;
    }
}
