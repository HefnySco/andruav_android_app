package ap.andruavmiddlelibrary;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;

/**
 * Created by mhefny on 5/18/16.
 */
public class Voting {


    static boolean  MapDisplayed = false;
    static boolean  ConnectedSuccessfully = false;
    static boolean  DisConnectedSuccessfully = false;
    static int      ImageRecieved = 0;
    static boolean  VideoRecieved = false;


    /***
     * The user has successfully connected to Andruav Websocket Server
     */
    public static void onConnectToServer()
    {
        ConnectedSuccessfully = true;
    }


    /***
     * The user has successfully disconnected without failure
     */
    public static void onDisconnecFromServerSafely()
    {
        if (ConnectedSuccessfully)
        {
            DisConnectedSuccessfully = true;

            //AndruavMo7arek.log().log(AndruavSettings.AccessCode, "voting", "ConnectedSuccessfully");

        }
    }

    /***
     * The user recived Video on Device.
     */
    public static void onVideoRecieved()
    {
        VideoRecieved = true;

        //AndruavMo7arek.log().log(AndruavSettings.AccessCode, "voting-VID", "onVideoRecieved");
    }


    public static void onCameraIssue()
    {
        VideoRecieved = false;

        AndruavEngine.log().log(AndruavSettings.AccessCode, "Cam-Issue", "onCameraIssue");
    }


    /***
     * The user recived Image on Device.
     */
    public static void onImageRecieved()
    {
        ImageRecieved += 1;

        //AndruavMo7arek.log().log(AndruavSettings.AccessCode, "voting-IMG", "onImageRecieved");
    }


    /***
     * The user opened Map Screen and has units on it.
     */
    public static void onMapHasObjectsRecieved()
    {
        MapDisplayed = true;
    }
}
