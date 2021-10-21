package com.andruav;

import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_RemoteExecute;

/**
 * Created by mhefny on 8/7/16.
 */

public class AndruavTaskManager {


    public static void loadTasksByScope (int scope, String taskType) {

        switch (scope)
        {
            case 0:
                loadGlobalTasks (taskType);
                break;
            case 1:
                loadGlobalAccountTasks (taskType);
                break;
            case 2:
                loadLocalGroupTasks (taskType);
                break;
            case 3:
                loadMyOwnTasksByPartyID (taskType);
                break;

        }
    }

    /***
     * Load Global commands for Vehicle / GCS based on my type.
     * <br> example: SELECT `SID`, `party_sid`, `GroupName`, `sender`, `receiver`, `messageType`, `task`, `isPermanent`, `Lastprocessed_Time`, `Creation_Time` FROM `task` WHERE  accountID = '_any_' AND  party_sid = '_any_' AND  GroupName = '_any_' AND  receiver = '_vehi_' AND  isPermanent = 1 AND  enabled = 1
     * TaskScope: 0  used by {@link AndruavMessage_RemoteExecute} in variable <b>ts</b>.
     * @param taskType
     */
    public static void loadGlobalTasks (String taskType) {
        loadTask(0,
                null,
                ProtocolHeaders.SPECIAL_NAME_ANY,
                ProtocolHeaders.SPECIAL_NAME_ANY,
                ProtocolHeaders.SPECIAL_NAME_ANY,
                null,
                AndruavSettings.andruavWe7daBase.getIsCGS() ? ProtocolHeaders.SPECIAL_NAME_GCS_RECEIVERS : ProtocolHeaders.SPECIAL_NAME_VEHICLE_RECEIVERS,
                taskType,
                true // it is global and should be Permanent
        );
    }


    /***
     * Load Task for AccessCode [Account Wide] e.g. Restricted area.
     * <br> example: SELECT `SID`, `party_sid`, `GroupName`, `sender`, `receiver`, `messageType`, `task`, `isPermanent`, `Lastprocessed_Time`, `Creation_Time` FROM `task` WHERE  accountID = 'mohammad.hefny@gmail.com' AND  party_sid = '_any_' AND  GroupName = '_any_' AND  receiver = '_vehi_' AND  isPermanent = 1 AND  enabled = 1
     * TaskScope: 1  used by {@link AndruavMessage_RemoteExecute} in variable <b>ts</b>.
     * @param taskType
     */
    public static void loadGlobalAccountTasks (String taskType)
    {
        loadTask(0,
                null,
                AndruavSettings.AccountName,
                ProtocolHeaders.SPECIAL_NAME_ANY,
                ProtocolHeaders.SPECIAL_NAME_ANY,
                null,   // any value here
                AndruavSettings.andruavWe7daBase.getIsCGS()?ProtocolHeaders.SPECIAL_NAME_GCS_RECEIVERS:ProtocolHeaders.SPECIAL_NAME_VEHICLE_RECEIVERS,
                taskType,
                true);
    }


    /***
     *  Load any Taks targets My Account-Group
     * <br> example: SELECT `SID`, `party_sid`, `GroupName`, `sender`, `receiver`, `messageType`, `task`, `isPermanent`, `Lastprocessed_Time`, `Creation_Time` FROM `task` WHERE  accountID = 'mohammad.hefny@gmail.com' AND  party_sid = '_any_' AND  GroupName = '4' AND  receiver = '_vehi_' AND  isPermanent = 1 AND  enabled = 1
     * TaskScope: 2  used by {@link AndruavMessage_RemoteExecute} in variable <b>ts</b>.
     * @param taskType
     */
    public static void loadLocalGroupTasks (String taskType)
    {
        // Load any Taks targets My Account-Group
        //SELECT `SID`, `party_sid`, `GroupName`, `sender`, `receiver`, `messageType`, `task`, `isPermanent`, `Lastprocessed_Time`, `Creation_Time` FROM `task` WHERE  accountID = 'mohammad.hefny@gmail.com' AND  party_sid = '_any_' AND  GroupName = '4' AND  receiver = '_vehi_' AND  isPermanent = 1 AND  enabled = 1
        loadTask(0,
                null,
                AndruavSettings.AccountName,
                ProtocolHeaders.SPECIAL_NAME_ANY,
                AndruavSettings.andruavWe7daBase.GroupName,
                null,   // any value here
                AndruavSettings.andruavWe7daBase.getIsCGS()?ProtocolHeaders.SPECIAL_NAME_GCS_RECEIVERS:ProtocolHeaders.SPECIAL_NAME_VEHICLE_RECEIVERS,
                taskType,
                true // it is local-global and should be Permanent
        );
    }

    /***
     *
     * TaskScope: 3  used by {@link AndruavMessage_RemoteExecute} in variable <b>ts</b>.
     * @param taskType
     */
    public static void loadMyOwnTasksByPartyID (String taskType)
    {
        // Tasks that has PartyID equals to my PartyID. Me AnyWhere
        loadTask(0,
                null, // for any account
                null,
                AndruavSettings.andruavWe7daBase.PartyID,
                null,
                null,
                null,
                taskType,
                true);
    }


    /***
     * Actual Task Loading
     * @param largerThan_SID
     * @param accessCode
     * @param accountID
     * @param party_sid
     * @param groupName
     * @param sender
     * @param receiver
     * @param messageType
     * @param isPermanent
     */
    private static void loadTask( int largerThan_SID,
                                  String accessCode,
                                  String accountID,
                                  String party_sid,
                                  String groupName,
                                  String sender,
                                  String receiver,
                                  String messageType,
                                  boolean isPermanent)
    {
        AndruavFacade.loadTasks(largerThan_SID,accessCode,accountID,party_sid,groupName,sender,receiver,messageType,isPermanent);

    }


    private static void deleteTask (int sid)
    {

    }

}

