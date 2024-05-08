package ap.andruavmiddlelibrary.sensors._7asasatEvents;

/*
  Created by M.Hefny on 24-Oct-14.
 */

/***
 * This is an internal application event between the UI and sensorService
 */
public class Event_IMU_CMD {

    public final static int IMU_CMD_UpdateZeroTilt  = 1;
    public final static int IMU_CMD_ReadGPS         = 2;


    public Object tag;
    public final int cmdID ;
    public Event_IMU_CMD(int defaultValue)
    {
        cmdID = defaultValue;
    }

}
