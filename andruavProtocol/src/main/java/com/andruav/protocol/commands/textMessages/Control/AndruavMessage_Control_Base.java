package com.andruav.protocol.commands.textMessages.Control;

import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

/**
 * Created by mhefny on 12/11/16.
 */

public class AndruavMessage_Control_Base extends AndruavMessageBase {


    public AndruavMessage_Control_Base()
    {
        super();
        messageDomain = DOMAIN_RESALA_REMOTE_EXECUTE;
    }
}
