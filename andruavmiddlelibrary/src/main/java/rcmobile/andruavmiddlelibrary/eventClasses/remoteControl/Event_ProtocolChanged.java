package rcmobile.andruavmiddlelibrary.eventClasses.remoteControl;

/**
 *
 * This is an Andruav Drone Internal Event. Triggered when Drone -Me- is connected/disconnected to FCB
 * Created by M.Hefny on 15-Apr-15.
 *
 */
public class Event_ProtocolChanged {


    public boolean mNormalAction;

    public Event_ProtocolChanged (final boolean isNormal )
    {
        mNormalAction = isNormal;
    }

}
