package rcmobile.FPV.activities.camera;

/**
 * Created by mhefny on 2/16/17.
 */

public class Event_RecordVideoStatus {


    public static final int CONST_READY_TO_RECORD   = 1;
    public static final int CONST_IS_RECORDING      = 2;
    public static final int CONST_STOP_RECORDING    = 3;
    public static final int CONST_ERROR_RECORDING    = 4;
    public  int status;


    public Event_RecordVideoStatus(final int status)
    {
        this.status = status;
    }

}