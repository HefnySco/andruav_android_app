package com.andruav.protocol.commands;


import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinaryBase;
import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

/**
 * Created by M.Hefny on 09-Jul-15.
 * <br>This class defines field names used in Andruav Protocol.
 * <br>They are  put here to provide single point of change.
 */
final public  class ProtocolHeaders {

    /***
     * Command value.
     * <br> Tis command is executed by the server. Not the destination units.
     */
    public static final String CMD = "cm";
    /***
     * Command Type either <b>system</b> or <b>communication</b>.
     */
    public static final String MSG_ROUTING = "ty";
    /***
     * Unit name of the sender
     */
    public static final String Sender = "sd";
    /***
     * Unit name of target(s)
     * <br> can me comma separated.
     */
    public static final String Target = "tg";
    /***
     * Group name of the sender
     */
    public static final String Group = "gr";
    /***
     * [optional] used by Destination to parse the carried command.
     * <br>Determines the message that is contained in {@link #Message}
     * <br>This is mainly is messageTypeID defined in  {@link AndruavMessageBase} or {@link AndruavResalaBinaryBase}
     */
    public static final String MessageType = "mt";
    /***
     * [optional] value.
     * <br>used in ping/pong system messages.
     */
    public static final String TimeStamp = "ts";
    /***
     * [optional]
     * <br>when true means the {@link #Message} content is encrypted using a key that is only knowo to source and target units.
     * <br>Server <b>cannot and is not able to</b> parse this message content. It can only forward it to destination.
     */
    public static final String Encryption = "en";
    /***
     * For system commands it holds requests and reply messages reply from Server.
     * <br>Reply messages either starts with OK or ERR.
     * <br>It also holds Messages of  {@link AndruavMessageBase} and {@link AndruavResalaBinaryBase}sent to targets.
     */
    public static final String Message = "ms";

    // Values for CMD_TYPE
    /**
     Communication Command
     <br>example:
     <br>{@link #CMD_COMM_INDIVIDUAL}, {@link #CMD_COMM_GROUP} , {@link #CMD_COMM_GLOBAL} , {@link #CMD_COMM_ACCOUNT}
     */
     //public static final String CMD_TYPE_COMM = "c";
    /***
     * System command
     * <br>example:
     * <br> add, addd, del, delll, ping
     */
    public static final String CMD_TYPE_SYS = "s";

    /***
     * InterModules command
     * <br>example:
     * <br> ModuleID or any other normal AndruavProtocol Messages that needs to be interpreted by another module
     */
    public static final String CMD_TYPE_INTERMODULE = "uv";

    //******************************************   COMMAND VALUES

    /***
     * Broadcast message to all group members.
     * <br>This is a value of {@link #CMD}
     */
    public static final String CMD_COMM_GROUP = "g";
    /***
     * Send message to a specific target.
     * <br>It should be possible to specify one or more targets - comma separated-
     * <br>The server will forward the message to those specific targets only.
     * <br>This is a value of {@link #CMD}
     */
    public static final String CMD_COMM_INDIVIDUAL = "i";
    /***
     * Broadcast message in all groups in all accounts.
     * <br>This is a value of {@link #CMD}
     */
    public static final String CMD_COMM_ACCOUNT = "a";
    /***
     * Broadcast message in all server.
     * <br>This is a value of {@link #CMD}
     */
    public static final String CMD_COMM_GLOBAL = "b";



    //******************************************   SYSTEM COMMAND VALUES
    public static final String CMD_SYS_TASKS                            = "tsk";


    /***
     * ALL NAMES LOWER CASE ... as in some parts of code comparision is case insensitive
     * if Sender equals to this then it is a system message and does not have {@link AndruavUnitBase}
     */
    // SYSTEM ID
    public static final String SPECIAL_NAME_ANY                         = "_any_";
    public static final String SPECIAL_NAME_SYS_NAME                    = "_sys_";
    public static final String SPECIAL_NAME_ALL_RECEIVERS               = "_generic_";
    public static final String SPECIAL_NAME_VEHICLE_RECEIVERS           = "_drone_";
    public static final String SPECIAL_NAME_GCS_RECEIVERS               = "_gcs_";


    // if update then update in resources as well
    public static final String UAVOS_COMM_MODULE_ID = "MT_A";
    public static final String UAVOS_COMM_MODULE_CLASS = "comm";

    public static final String UAVOS_CAMERA_MODULE_CLASS = "camera";
    public static final String UAVOS_FCB_MODULE_CLASS = "fcb";

    public static final String[]  reserverUnitNames = {SPECIAL_NAME_SYS_NAME,SPECIAL_NAME_ALL_RECEIVERS,SPECIAL_NAME_VEHICLE_RECEIVERS,SPECIAL_NAME_GCS_RECEIVERS};

}
