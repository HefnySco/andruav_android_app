package com.andruav.event;

public class Event_Remote_ChannelsCMD {
    public String partyID;
    public int[]    channels;
    public boolean engaged;

    public Event_Remote_ChannelsCMD(String partyID)
    {
        this.partyID = partyID;
        engaged = false;
    }



    public Event_Remote_ChannelsCMD(int[] divider)
    {
        this.channels = divider;
        engaged = true;
    }
}
