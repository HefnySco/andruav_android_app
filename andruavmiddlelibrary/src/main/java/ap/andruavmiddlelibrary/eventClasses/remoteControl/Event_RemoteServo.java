package ap.andruavmiddlelibrary.eventClasses.remoteControl;

public class Event_RemoteServo {

    public int ChannelNumber;

    public int ChannelValue;

    public String PartyID;

    static public final int CONST_MINIMUM = 0;
    static public final int CONST_MAXIMUM = 9999;

    public Event_RemoteServo ()
    {

    }

    public Event_RemoteServo (final int channelNumber, final int channelValue)
    {
        ChannelNumber = channelNumber;
        ChannelValue = channelValue;
    }

}
