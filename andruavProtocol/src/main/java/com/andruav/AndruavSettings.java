package com.andruav;

import android.view.Surface;

import com.andruav.andruavUnit.AndruavUnitMe;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.protocol.commands.ProtocolHeaders;

import java.util.ArrayList;

/**
 * Created by M.Hefny on 18-Jan-15.
 * Contains identification info.
 */
public class AndruavSettings {


    public static final int DEFAULT_ANDRUAV_LAN_UDP_PORT = 60000;
    public static final int DEFAULT_ANDRUAV_LAN_MODULE_UDP_PORT = 60001;


    public static int mobileDirection = Surface.ROTATION_0;


    /**
     * Used to count number of video frames sent/recieved.
     * <br>Later should be attached to every AndruavUnit
     */
    public static  byte[] encryptionkey;
    public static  Boolean encryptionEnabled=false;

    /****************
     * REGISTRATION FEATURES
     */

    public static boolean UnLockerExists = false;
    /***********************************************/

    /***
     * SID used for subscribing in Webservice.
     * retrieved from AccessCode -pwd-& Email-username-
     */
    public static String Account_SID="";


    /***
     * Used as the ID "key" in Andruav Server
     */
    public static String AccessCode="New Me";

    /***
     * Used as the ID "key" in Andruav Server
     */
    public static String AccountName="";

    /***
     * This is the IsMe  = true
     */
    public static AndruavUnitMe andruavWe7daBase;

    public static String andruavLocalCameraModuleID;

    /*
        DRONE MODE
        List of units that needs IMU data.
        I will store AndruavUser class in order to be able to detect dead points
    */
    public final static  ArrayList<AndruavUnitBase> mIMURequests = new ArrayList<>();
    /*
        DRONE MODE
        List of units that needs Video data.
    */
    public final static  SimpleArrayMapRequests mVideoRequests = new SimpleArrayMapRequests();
    /*
        DRONE MODE
        List of units that needs Telemetry
    */
    public final static ArrayList<AndruavUnitBase> mTelemetryRequests = new ArrayList<>();

    /*
            DRONE MODE
            List of units that are in my SWARM.... I am the leader Drone here.
    */
    public final static ArrayList<AndruavUnitBase> mSwarmMembers = new ArrayList<>();

    public final static int[] RemoteControlDualRates = new int [8];

    /***
     * This is a bitwise for RemoteChannel RTC
     */
    public static int RemoteControlRTC =0;

    /***
     * Andruav Drone that is sending Telemetry Data to me [GCS]
     */
    public static AndruavUnitShadow remoteTelemetryAndruavWe7da=null;

    public static String WebServerURL = "www.andruav.com";
    public static String WebServerPort = "9210";
    public static String WEBMOFTA7 = "000000000000-0000-0000-0000-000000000000";


    public static int videoCameraRotationDegree = 0;

    public static void loadGenericPermanentTasks()
    {
        // Load Global commands for Vehicle / GCS based on my type.
        //SELECT `SID`, `party_sid`, `GroupName`, `sender`, `receiver`, `messageType`, `task`, `isPermanent`, `Lastprocessed_Time`, `Creation_Time` FROM `task` WHERE  accountID = '_any_' AND  party_sid = '_any_' AND  GroupName = '_any_' AND  receiver = '_vehi_' AND  isPermanent = 1 AND  enabled = 1
        AndruavTaskManager.loadGlobalTasks(null);

        // Load Task for AccessCode [Account Wide] e.g. Restricted area.
        //SELECT `SID`, `party_sid`, `GroupName`, `sender`, `receiver`, `messageType`, `task`, `isPermanent`, `Lastprocessed_Time`, `Creation_Time` FROM `task` WHERE  accountID = 'mohammad.hefny@gmail.com' AND  party_sid = '_any_' AND  GroupName = '_any_' AND  receiver = '_vehi_' AND  isPermanent = 1 AND  enabled = 1
        AndruavTaskManager.loadGlobalAccountTasks (null);

        // Load any Taks targets My Account-Group
        // example: SELECT `SID`, `party_sid`, `GroupName`, `sender`, `receiver`, `messageType`, `task`, `isPermanent`, `Lastprocessed_Time`, `Creation_Time` FROM `task` WHERE  accountID = 'mohammad.hefny@gmail.com' AND  party_sid = '_any_' AND  GroupName = '4' AND  receiver = '_vehi_' AND  isPermanent = 1 AND  enabled = 1
        AndruavTaskManager.loadLocalGroupTasks(null);

    }


    public static void loadMyPermanentTasks()
    {
       /*
        // Tasks that has receiver equal to my receiver name  & in my account!!
        loadTask(0,
                null,
                AndruavSettings.AccountName,
                null,
                null,
                null,
                andruavWe7daBase.UnitID,
                null,
                true);
           */

    }

    public static void loadMyPermanentTasksByPartyID()
    {
        // Tasks that has PartyID equals to my PartyID. Me AnyWhere
        AndruavTaskManager.loadMyOwnTasksByPartyID (null);
    }


    public static boolean isValidAndruavUnitName (final String Name)
    {
        for (int i = 0; i < ProtocolHeaders.reserverUnitNames.length; i++) {
            if (ProtocolHeaders.reserverUnitNames[i].equals(Name)) return false;
        }

        return  false;
    }



}
