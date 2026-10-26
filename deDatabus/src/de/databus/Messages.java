package de.databus;

/**
 * Java port of de_databus/python/messages.py — the wire constants the
 * module uses. Short names are wire/protocol fields and stay unchanged
 * (de_common messages.hpp parity).
 */
public final class Messages {

    private Messages() {}

    // InterModules command
    public static final String CMD_TYPE_INTERMODULE = "uv";
    public static final String CMD_TYPE_SYSTEM_MSG = "s";

    // JSON InterModule Fields
    public static final String JSON_INTERMODULE_MODULE_ID = "a";
    public static final String JSON_INTERMODULE_MODULE_CLASS = "b";
    public static final String JSON_INTERMODULE_MODULE_MESSAGES_LIST = "c";
    public static final String JSON_INTERMODULE_MODULE_FEATURES = "d";
    public static final String JSON_INTERMODULE_MODULE_KEY = "e";
    public static final String JSON_INTERMODULE_PARTY_RECORD = "f";
    public static final String JSON_INTERMODULE_SOCKET_STATUS = "g";
    public static final String JSON_INTERMODULE_HARDWARE_ID = "s";
    public static final String JSON_INTERMODULE_HARDWARE_TYPE = "t";
    public static final String JSON_INTERMODULE_VERSION = "v";
    public static final String JSON_INTERMODULE_TIMESTAMP_INSTANCE = "u";
    public static final String JSON_INTERMODULE_RESEND = "z";

    public static final int MIN_JSON_HEADER_LEANGTH = 10;

    // Communication Commands
    public static final String CMD_COMM_GROUP = "g";
    public static final String CMD_COMM_INDIVIDUAL = "i";
    public static final String CMD_COMM_SYSTEM = "s";

    // Andruav Protocol Fields
    public static final String ANDRUAV_PROTOCOL_GROUP_ID = "gr";
    public static final String ANDRUAV_PROTOCOL_SENDER = "sd";
    public static final String ANDRUAV_PROTOCOL_TARGET_ID = "tg";
    public static final String ANDRUAV_PROTOCOL_MESSAGE_TYPE = "mt";
    public static final String ANDRUAV_PROTOCOL_MESSAGE_CMD = "ms";
    public static final String ANDRUAV_PROTOCOL_MESSAGE_PERMISSION = "p";
    public static final String INTERMODULE_ROUTING_TYPE = "ty";
    public static final String INTERMODULE_MODULE_KEY = "GU";
    public static final String WAITING_EVENT = "ew";
    public static final String FIRE_EVENT = "ef";
    public static final String LINKED_TO_STEP = "ls";

    // Reserved Target Values
    public static final String ANDRUAV_PROTOCOL_SENDER_ALL_GCS = "_GCS_";
    public static final String ANDRUAV_PROTOCOL_SENDER_ALL_AGENTS = "_AGN_";
    public static final String ANDRUAV_PROTOCOL_SENDER_ALL = "_GD_";
    public static final String ANDRUAV_PROTOCOL_SENDER_COMM_SERVER = "_SYS_";
    public static final String SPECIAL_NAME_SYS_NAME = ANDRUAV_PROTOCOL_SENDER_COMM_SERVER;

    // SOCKET STATUS
    public static final int SOCKET_STATUS_FREASH = 1;
    public static final int SOCKET_STATUS_CONNECTING = 2;
    public static final int SOCKET_STATUS_DISCONNECTING = 3;
    public static final int SOCKET_STATUS_DISCONNECTED = 4;
    public static final int SOCKET_STATUS_CONNECTED = 5;
    public static final int SOCKET_STATUS_REGISTERED = 6;
    public static final int SOCKET_STATUS_UNREGISTERED = 7;
    public static final int SOCKET_STATUS_ERROR = 8;

    // System Messages
    public static final int TYPE_AndruavSystem_LoadTasks = 9001;
    public static final int TYPE_AndruavSystem_SaveTasks = 9002;
    public static final int TYPE_AndruavSystem_DeleteTasks = 9003;
    public static final int TYPE_AndruavSystem_DisableTasks = 9004;
    public static final int TYPE_AndruavSystem_Ping = 9005;
    public static final int TYPE_AndruavSystem_LogoutCommServer = 9006;
    public static final int TYPE_AndruavSystem_ConnectedCommServer = 9007;
    public static final int TYPE_AndruavSystem_UDPProxy = 9008;
    public static final int TYPE_AndruavSystem_LocalServer = 9009;

    // Inter Module Commands
    public static final int TYPE_AndruavModule_ID = 9100;
    public static final int TYPE_AndruavModule_RemoteExecute = 9101;
    public static final int TYPE_AndruavModule_Location_Info = 9102;

    // Andruav Messages
    public static final int TYPE_AndruavMessage_GPS = 1002;
    public static final int TYPE_AndruavMessage_POWER = 1003;
    public static final int TYPE_AndruavMessage_ID = 1004;
    public static final int TYPE_AndruavMessage_RemoteExecute = 1005;
    public static final int TYPE_AndruavMessage_IMG = 1006;
    public static final int TYPE_AndruavMessage_Error = 1008;
    public static final int TYPE_AndruavMessage_FlightControl = 1010;
    public static final int TYPE_AndruavMessage_CameraList = 1012;
    public static final int TYPE_AndruavMessage_DroneReport = 1020;
    public static final int TYPE_AndruavMessage_Signaling = 1021;
    public static final int TYPE_AndruavMessage_HomeLocation = 1022;
    public static final int TYPE_AndruavMessage_GeoFence = 1023;
    public static final int TYPE_AndruavMessage_ExternalGeoFence = 1024;
    public static final int TYPE_AndruavMessage_GEOFenceHit = 1025;
    public static final int TYPE_AndruavMessage_WayPoints = 1027;
    public static final int TYPE_AndruavMessage_GeoFenceAttachStatus = 1029;
    public static final int TYPE_AndruavMessage_Arm = 1030;
    public static final int TYPE_AndruavMessage_ChangeAltitude = 1031;
    public static final int TYPE_AndruavMessage_Land = 1032;
    public static final int TYPE_AndruavMessage_GuidedPoint = 1033;
    public static final int TYPE_AndruavMessage_CirclePoint = 1034;
    public static final int TYPE_AndruavMessage_DoYAW = 1035;
    public static final int TYPE_AndruavMessage_NAV_INFO = 1036;
    public static final int TYPE_AndruavMessage_DistinationLocation = 1037;
    public static final int TYPE_AndruavMessage_ConfigCOM = 1038;
    public static final int TYPE_AndruavMessage_ConfigFCB = 1039;
    public static final int TYPE_AndruavMessage_ChangeSpeed = 1040;
    public static final int TYPE_AndruavMessage_Ctrl_Cameras = 1041;
    public static final int TYPE_AndruavMessage_TrackingTarget_ACTION = 1042;
    public static final int TYPE_AndruavMessage_TrackingTargetLocation = 1043;
    public static final int TYPE_AndruavMessage_TrackingTarget_STATUS = 1044;
    public static final int TYPE_AndruavMessage_UploadWayPoints = 1046;
    public static final int TYPE_AndruavMessage_RemoteControlSettings = 1047;
    public static final int TYPE_AndruavMessage_SET_HOME_LOCATION = 1048;
    public static final int TYPE_AndruavMessage_CameraZoom = 1049;
    public static final int TYPE_AndruavMessage_CameraSwitch = 1050;
    public static final int TYPE_AndruavMessage_CameraFlash = 1051;
    public static final int TYPE_AndruavMessage_RemoteControl2 = 1052;
    public static final int TYPE_AndruavMessage_SensorsStatus = 1053;
    public static final int TYPE_AndruavMessage_FollowHim_Request = 1054;
    public static final int TYPE_AndruavMessage_FollowMe_Guided = 1055;
    public static final int TYPE_AndruavMessage_Make_Swarm = 1056;
    public static final int TYPE_AndruavMessage_SwarmReport = 1057;
    public static final int TYPE_AndruavMessage_UpdateSwarm = 1058;
    public static final int TYPE_AndruavMessage_CommSignalsStatus = 1059;
    public static final int TYPE_AndruavMessage_Sync_EventFire = 1061;
    public static final int TYPE_AndruavMessage_SearchTargetList = 1062;
    public static final int TYPE_AndruavMessage_Prepherials = 1070;
    public static final int TYPE_AndruavMessage_UDPProxy_Info = 1071;
    public static final int TYPE_AndruavMessage_Unit_Name = 1072;
    public static final int TYPE_AndruavMessage_Ping_Unit = 1073;
    public static final int TYPE_AndruavMessage_Upload_DE_Mission = 1075;
    public static final int TYPE_AndruavMessage_AI_Recognition_ACTION = 1076;
    public static final int TYPE_AndruavMessage_AI_Recognition_STATUS = 1077;
    public static final int TYPE_AndruavMessage_Viewlink_ACTION = 1079;
    public static final int TYPE_AndruavMessage_Viewlink_STATUS = 1080;
    public static final int TYPE_AndruavMessage_DEPilot_Control = 1081;

    // Phase 2 - Mission Supervisor (GCS <-> de_comm)
    public static final int TYPE_AndruavMessage_DE_Package_Upload = 1085;
    public static final int TYPE_AndruavMessage_DE_Supervisor_Action = 1086;
    public static final int TYPE_AndruavMessage_DE_Supervisor_Status = 1087;
    public static final int TYPE_AndruavMessage_DE_Supervisor_Journal = 1088;

    // Phase 3 - unit capability registry view (de_comm <-> GCS)
    public static final int TYPE_AndruavMessage_UNIT_CAPABILITIES = 1089;

    public static final int TYPE_AndruavMessage_LightTelemetry = 2022;

    public static final int TYPE_AndruavMessage_ServoChannel = 6001;  // OBSOLETE

    public static final int TYPE_AndruavMessage_MAVLINK = 6502;
    public static final int TYPE_AndruavMessage_SWARM_MAVLINK = 6503;
    public static final int TYPE_AndruavMessage_INTERNAL_MAVLINK = 6504;
    public static final int TYPE_AndruavMessage_P2P_ACTION = 6505;
    public static final int TYPE_AndruavMessage_P2P_STATUS = 6506;
    public static final int TYPE_AndruavMessage_P2P_InRange_BSSID = 6507;
    public static final int TYPE_AndruavMessage_P2P_InRange_Node = 6508;

    public static final int TYPE_AndruavMessage_Communication_Line_Set = 6509;
    public static final int TYPE_AndruavMessage_Communication_Line_Status = 6510;

    public static final int TYPE_AndruavMessage_SOUND_TEXT_TO_SPEECH = 6511;
    public static final int TYPE_AndruavMessage_SOUND_PLAY_FILE = 6512;
    public static final int TYPE_AndruavMessage_SOUND_LIST = 6530;

    public static final int TYPE_AndruavMessage_SDR_ACTION = 6514;
    public static final int TYPE_AndruavMessage_SDR_REMOTE_EXECUTE = 6515;
    public static final int TYPE_AndruavMessage_SDR_SPECTRUM = 6516;

    public static final int TYPE_AndruavMessage_P2P_INFO = 6517;

    public static final int TYPE_AndruavMessage_Mission_Item_Sequence = 6518;

    public static final int TYPE_AndruavMessage_GPIO_ACTION = 6519;
    public static final int TYPE_AndruavMessage_GPIO_STATUS = 6520;
    public static final int TYPE_AndruavMessage_GPIO_REMOTE_EXECUTE = 6521;

    public static final int TYPE_AndruavMessage_LocalServer_ACTION = 6522;
    public static final int TYPE_AndruavMessage_LocalServer_STATUS = 6523;
    public static final int TYPE_AndruavMessage_LocalServer_REMOTE_EXECUTE = 6524;

    public static final int TYPE_AndruavMessage_CONFIG_ACTION = 6525;
    public static final int TYPE_AndruavMessage_CONFIG_STATUS = 6526;

    public static final int TYPE_AndruavMessage_MAVLINK_EVENTS = 6527;

    // 6528-6529 reserved for IR_CAMERA_MI48_* (de_comm, to be reconciled later)

    // Phase 2 - nav adapter (de_comm <-> de_mavlink, intermodule only)
    public static final int TYPE_AndruavMessage_CAMERA_OVERLAY_ACTION = 6539;
    public static final int TYPE_AndruavMessage_NAV_ADAPTER_ACTION = 6540;
    public static final int TYPE_AndruavMessage_NAV_ADAPTER_STATUS = 6541;

    // Phase 3 - capability adverts / invoke / state (intermodule only)
    public static final int TYPE_AndruavMessage_MODULE_CAPABILITIES = 6542;
    public static final int TYPE_AndruavMessage_CAPABILITY_INVOKE = 6543;
    public static final int TYPE_AndruavMessage_CAPABILITY_RESULT = 6544;
    public static final int TYPE_AndruavMessage_MODULE_STATE = 6545;

    // per-frame AI observations (perceiver -> de_comm, intermodule only)
    public static final int TYPE_AndruavMessage_AI_OBSERVATIONS = 6546;

    public static final int TYPE_AndruavMessage_DUMMY = 9999;
    public static final int TYPE_AndruavMessage_USER_RANGE_START = 80000;
    public static final int TYPE_AndruavMessage_USER_RANGE_END = 90000;

    // TYPE_AndruavMessage_CONFIG_ACTION
    public static final int CONFIG_ACTION_Restart = 0;
    public static final int CONFIG_ACTION_APPLY_CONFIG = 1;
    public static final int CONFIG_REQUEST_FETCH_CONFIG_TEMPLATE = 2;
    public static final int CONFIG_REQUEST_FETCH_CONFIG = 3;
    public static final int CONFIG_ACTION_SHUT_DOWN_HW = 4;
    public static final int CONFIG_ACTION_RESTART_HW = 5;

    // TYPE_AndruavMessage_CONFIG_STATUS
    public static final int CONFIG_STATUS_FETCH_CONFIG_TEMPLATE = 0;
    public static final int CONFIG_STATUS_FETCH_CONFIG = 1;

    public static final int ERROR_TYPE_LO7ETTA7AKOM = 5;
    public static final int ERROR_3DR = 7;
    public static final int ERROR_GPS = 10;
    public static final int ERROR_POWER = 11;
    public static final int ERROR_RCCONTROL = 12;
    public static final int ERROR_TYPE_ERROR_MODULE = 13;
    public static final int ERROR_TYPE_ERROR_P2P = 23;
    public static final int ERROR_TYPE_ERROR_SDR = 24;
    public static final int ERROR_GEO_FENCE_ERROR = 100;
    public static final int ERROR_USER_DEFINED = 1000;

    public static final int NOTIFICATION_TYPE_EMERGENCY = 0;
    public static final int NOTIFICATION_TYPE_ALERT = 1;
    public static final int NOTIFICATION_TYPE_CRITICAL = 2;
    public static final int NOTIFICATION_TYPE_ERROR = 3;
    public static final int NOTIFICATION_TYPE_WARNING = 4;
    public static final int NOTIFICATION_TYPE_NOTICE = 5;
    public static final int NOTIFICATION_TYPE_INFO = 6;
    public static final int NOTIFICATION_TYPE_DEBUG = 7;
    public static final int NOTIFICATION_TYPE_REGISTRATION = 22;
    public static final int NOTIFICATION_TYPE_TELEMETRY = 33;
    public static final int NOTIFICATION_TYPE_PROTOCOL = 44;
    public static final int NOTIFICATION_TYPE_LO7ETTA7AKOM = 77;
    public static final int NOTIFICATION_TYPE_GEO_FENCE = 88;

    // Precision Landing Message Types
    public static final int TYPE_AndruavMessage_PRECLAND_ACTION = 6536;
    public static final int TYPE_AndruavMessage_PRECLAND_TARGET = 6537;
    public static final int TYPE_AndruavMessage_PRECLAND_STATUS = 6538;

    // PRECLAND_ACTION codes
    public static final int PRECLAND_ACTION_DISABLE = 0;
    public static final int PRECLAND_ACTION_ENABLE = 1;
    public static final int PRECLAND_ACTION_SET_TARGET = 2;
    public static final int PRECLAND_ACTION_SELFTEST = 3;
    public static final int PRECLAND_ACTION_CALIBRATE = 4;
    public static final int PRECLAND_ACTION_CALIBRATE_CANCEL = 5;

    // PRECLAND_CALIB_STATUS codes
    public static final int PRECLAND_CALIB_STATUS_IDLE = 0;
    public static final int PRECLAND_CALIB_STATUS_CAPTURING = 1;
    public static final int PRECLAND_CALIB_STATUS_COMPUTING = 2;
    public static final int PRECLAND_CALIB_STATUS_DONE = 3;
    public static final int PRECLAND_CALIB_STATUS_FAILED = 4;

    // PRECLAND_STATUS codes
    public static final int PRECLAND_STATUS_DISABLED = 0;
    public static final int PRECLAND_STATUS_SEARCHING = 1;
    public static final int PRECLAND_STATUS_LOCKED = 2;
    public static final int PRECLAND_STATUS_DEGRADED = 3;
    public static final int PRECLAND_STATUS_ERROR = 4;

    // PRECLAND_REASON codes
    public static final int PRECLAND_REASON_NOMINAL = 0;
    public static final int PRECLAND_REASON_NO_TARGET = 1;
    public static final int PRECLAND_REASON_STALE = 2;
    public static final int PRECLAND_REASON_MIN_TAGS = 3;
    public static final int PRECLAND_REASON_RMSE = 4;
    public static final int PRECLAND_REASON_ERROR = 5;
}
