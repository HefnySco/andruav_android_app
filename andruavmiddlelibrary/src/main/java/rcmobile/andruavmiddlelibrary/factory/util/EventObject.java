package rcmobile.andruavmiddlelibrary.factory.util;

/**
 * Created by M.Hefny on 14-Sep-14.
 */


/**
 * This class is used with Event Bus to provide a generic way to pass classified events.
 * More complex classes can be used if needed contains event source and datetime.
 */

public class EventObject {

    public long classType;

    public Object object;

    public EventObject (long ClassType, Object object)
    {
        this.classType = ClassType;
        this.object = object;
    }

}
