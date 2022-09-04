package com.andruav.protocol.commands;


import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.interfaces.INotification;
import com.andruav.protocol.commands.binaryMessages.AndruavBinary_2MR;
import com.andruav.protocol.commands.binaryMessages.AndruavBinaryHelper;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinaryBase;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_BinaryData;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_ExternalCommand_WayPoints;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMG;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMU;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMUStatistics;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_RemoteControl;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_RemoteControlSettings;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_ServoOutput;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_WayPointsUpdates;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_WayPoints;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_LightTelemetry;
import com.andruav.protocol.commands.textMessages.AndruavMessageBase;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraFlash;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraSwitch;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraZoom;
import com.andruav.protocol.commands.textMessages.AndruavMessage_DroneReport;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ExternalCommand_WayPoints;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ExternalCommand_GeoFence;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraList;
import com.andruav.protocol.commands.textMessages.AndruavMessage_NAV_INFO;
import com.andruav.protocol.commands.textMessages.AndruavMessage_DistinationLocation;
import com.andruav.protocol.commands.textMessages.AndruavMessage_RemoteControl2;
import com.andruav.protocol.commands.textMessages.AndruavMessage_RemoteControlSettings;
import com.andruav.protocol.commands.textMessages.AndruavMessage_SensorsStatus;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ServoChannel;
import com.andruav.protocol.commands.textMessages.AndruavMessage_SetHomeLocation;
import com.andruav.protocol.commands.textMessages.AndruavMessage_UploadWayPoints;
import com.andruav.protocol.commands.textMessages.Configuration.AndruavMessage_Config_COM;
import com.andruav.protocol.commands.textMessages.Configuration.AndruavMessage_Config_FCB;
import com.andruav.protocol.commands.textMessages.Configuration.AndruavMessage_Config_Preference;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_GimbalCtrl;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_Ctrl_Camera;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_Arm;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_ChangeSpeed;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_CirclePoint;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_DoYAW;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_FlightControl;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GEOFenceHit;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GPS;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GeoFence;
import com.andruav.protocol.commands.textMessages.AndruavMessage_GeoFenceAttachStatus;
import com.andruav.protocol.commands.textMessages.AndruavMessage_HomeLocation;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ID;
import com.andruav.protocol.commands.textMessages.AndruavMessage_IMU;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_RemoteExecute;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Signaling;
import com.andruav.protocol.commands.textMessages.AndruavMessage_UDP_ID;
import com.andruav.protocol.commands.textMessages.Configuration.AndruavMessage_Config_UnitID;
import com.andruav.protocol.commands.textMessages.AndruavMessage_WayPoints;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;
import com.andruav.protocol.commands.textMessages.AndruavMessage_POW;
import com.andruav.protocol.commands.textMessages.AndruavMessage_String;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_GuidedPoint;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_Land;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_ChangeAltitude;
import com.andruav.protocol.commands.textMessages.systemCommands.AndruavSystem_ConnectedCommServer;
import com.andruav.protocol.commands.textMessages.systemCommands.AndruavSystem_LogoutCommServer;
import com.andruav.protocol.commands.textMessages.systemCommands.AndruavSystem_Ping;
import com.andruav.protocol.commands.textMessages.systemCommands.AndruavSystem_UdpProxy;
import com.andruav.protocol.commands.textMessages.uavosCommands.AndruavModule_ID;

import org.json.JSONObject;

import javax.crypto.BadPaddingException;

/**
 * This is base class for parsers. It should include all parsing for all devfined commands in module {@link com.andruav.protocol.commands}
 * <br><br>Created by M.Hefny on 02-Oct-14.
 */
public class Andruav_Parser {

    /*
        2019 COMMENT:

        // 6000 is serial for Andruav new messages.
        // 6500 is for new binary messages

    */

    public Andruav_Parser() {

    }

    /***
     * Inistantiate binary command based on message type. This function is called by parser functions.
     *
     * @param messageType each command has a message identification that is unique over all commands regardless text & binary.
     * @return an empty instance of the command
     */
    public static AndruavResalaBinaryBase getAndruavMessageBinary(int messageType) {

        AndruavResalaBinaryBase andruavResalaBinaryBase;

        switch (messageType) {
            case AndruavResalaBinary_BinaryData.TYPE_AndruavMessageBinary_Binary:
                andruavResalaBinaryBase = new AndruavResalaBinary_BinaryData();
                break;

            case AndruavResalaBinary_IMG.TYPE_AndruavMessage_IMG:
                andruavResalaBinaryBase = new AndruavResalaBinary_IMG();
                break;

            case AndruavResalaBinary_IMU.TYPE_AndruavMessage_BinaryIMU:
                andruavResalaBinaryBase = new AndruavResalaBinary_IMU();
                break;

            case AndruavResalaBinary_WayPoints.TYPE_AndruavMessageBinary_WayPoints:
                andruavResalaBinaryBase = new AndruavResalaBinary_WayPoints();
                break;

            case AndruavResalaBinary_IMUStatistics.TYPE_AndruavMessage_IMUStatistics:
                andruavResalaBinaryBase = new AndruavResalaBinary_IMUStatistics();
                break;

            case AndruavResalaBinary_RemoteControl.TYPE_AndruavMessage_RemoteControl:
                andruavResalaBinaryBase = new AndruavResalaBinary_RemoteControl();
                break;


            case AndruavResalaBinary_WayPointsUpdates.TYPE_AndruavMessage_WayPointsUpates:
                andruavResalaBinaryBase = new AndruavResalaBinary_WayPointsUpdates();
                break;

            case AndruavResalaBinary_ExternalCommand_WayPoints.TYPE_AndruavResalaBinary_ExternalCommand_WayPoints:
                andruavResalaBinaryBase = new AndruavResalaBinary_ExternalCommand_WayPoints();
                break;

            case AndruavResalaBinary_RemoteControlSettings.TYPE_AndruavMessage_RemoteControlSettings:
                andruavResalaBinaryBase = new AndruavResalaBinary_RemoteControlSettings();
                break;

            case AndruavResalaBinary_LightTelemetry.TYPE_AndruavMessage_LightTelemetry:
                andruavResalaBinaryBase = new AndruavResalaBinary_LightTelemetry();
                break;

            case AndruavResalaBinary_ServoOutput.TYPE_AndruavMessage_ServoOutput:
                andruavResalaBinaryBase = new AndruavResalaBinary_ServoOutput();
                break;

            default:
                throw new IllegalArgumentException("unknown messageType:" + messageType);

        }

        return andruavResalaBinaryBase;
    }


    /**
     * Inistantiate text command based on message type. This function is called by parser functions.
     *
     * @param messageType each command has a message identification that is unique over all commands regardless text & binary.
     * @return an empty instance of the command
     */
    public static AndruavMessageBase getAndruavMessage(int messageType) {
        AndruavMessageBase andruavMessageBase;
        switch (messageType) {

            case AndruavMessage_String.TYPE_AndruavCMD_STRING:
                andruavMessageBase = new AndruavMessage_String();
                break;

            case AndruavMessage_IMU.TYPE_AndruavMessage_IMU:
                andruavMessageBase = new AndruavMessage_IMU();
                break;

            case AndruavMessage_GPS.TYPE_AndruavMessage_GPS:
                andruavMessageBase = new AndruavMessage_GPS();
                break;

            case AndruavMessage_POW.TYPE_AndruavMessage_POW:
                andruavMessageBase = new AndruavMessage_POW();
                break;

            case AndruavMessage_ID.TYPE_AndruavMessage_ID:
                andruavMessageBase = new AndruavMessage_ID();
                break;

            case AndruavMessage_RemoteExecute.TYPE_AndruavMessage_RemoteExecute:
                andruavMessageBase = new AndruavMessage_RemoteExecute();
                break;

            case AndruavMessage_Error.TYPE_AndruavMessage_Error:
                andruavMessageBase = new AndruavMessage_Error();
                break;

            case AndruavMessage_FlightControl.TYPE_AndruavMessage_FlightControl:
                andruavMessageBase = new AndruavMessage_FlightControl();
                break;

            case AndruavMessage_UDP_ID.TYPE_AndruavCMD_UDP_ID:
                andruavMessageBase = new AndruavMessage_UDP_ID();
                break;

            case AndruavMessage_CameraList.TYPE_AndruavCMD_CameraList:
                andruavMessageBase = new AndruavMessage_CameraList();
                break;

            case AndruavMessage_DroneReport.TYPE_AndruavMessage_DroneReport:
                andruavMessageBase = new AndruavMessage_DroneReport();
                break;

            case AndruavMessage_Signaling.TYPE_AndruavMessage_Signaling:
                andruavMessageBase = new AndruavMessage_Signaling();
                break;

            case AndruavMessage_HomeLocation.TYPE_AndruavMessage_HomeLocation:
                andruavMessageBase = new AndruavMessage_HomeLocation();
                break;

            case AndruavMessage_GeoFence.TYPE_AndruavMessage_GeoFence:
                andruavMessageBase = new AndruavMessage_GeoFence();
                break;

            case AndruavMessage_ExternalCommand_GeoFence.TYPE_AndruavMessage_ExternalGeoFence:
                andruavMessageBase = new AndruavMessage_ExternalCommand_GeoFence();
                break;

            case AndruavMessage_GEOFenceHit.TYPE_AndruavResala_GEOFenceHit:
                andruavMessageBase = new AndruavMessage_GEOFenceHit();
                break;

            case AndruavMessage_Config_UnitID.TYPE_AndruavMessage_Config_UnitID:
                andruavMessageBase = new AndruavMessage_Config_UnitID();
                break;

            case AndruavMessage_WayPoints.TYPE_AndruavMessage_WayPoints:
            case AndruavMessage_ExternalCommand_WayPoints.TYPE_AndruavResala_ExternalCommand_WayPoints:
                andruavMessageBase = new AndruavMessage_WayPoints();
                break;


            case AndruavMessage_GeoFenceAttachStatus.TYPE_AndruavResala_GeoFenceAttachStatus:
                andruavMessageBase = new AndruavMessage_GeoFenceAttachStatus();
                break;

            case AndruavMessage_Arm.TYPE_AndruavMessage_Arm:
                andruavMessageBase = new AndruavMessage_Arm();
                break;

            case AndruavMessage_ChangeAltitude.TYPE_AndruavMessage_ChangeAltitude:
                andruavMessageBase = new AndruavMessage_ChangeAltitude();
                break;

            case AndruavMessage_Land.TYPE_AndruavMessage_Land:
                andruavMessageBase = new AndruavMessage_Land();
                break;

            case AndruavMessage_GuidedPoint.TYPE_AndruavMessage_GuidedPoint:
                andruavMessageBase = new AndruavMessage_GuidedPoint();
                break;

            case AndruavMessage_CirclePoint.TYPE_AndruavMessage_CirclePoint:
                andruavMessageBase = new AndruavMessage_CirclePoint();
                break;

            case AndruavMessage_DoYAW.TYPE_AndruavMessage_DoYAW:
                andruavMessageBase = new AndruavMessage_DoYAW();
                break;

            case AndruavMessage_NAV_INFO.TYPE_AndruavMessage_NAV_INFO:
                andruavMessageBase = new AndruavMessage_NAV_INFO();
                break;

            case AndruavMessage_DistinationLocation.TYPE_AndruavMessage_DistinationLocation:
                andruavMessageBase = new AndruavMessage_DistinationLocation();
                break;

            case AndruavMessage_Config_COM.TYPE_AndruavResala_Config_COM:
                andruavMessageBase = new AndruavMessage_Config_COM();
                break;

            case AndruavMessage_Config_FCB.TYPE_AndruavResala_Config_FCB:
                andruavMessageBase = new AndruavMessage_Config_FCB();
                break;

            case AndruavMessage_ChangeSpeed.TYPE_AndruavResala_ChangeSpeed:
                andruavMessageBase = new AndruavMessage_ChangeSpeed();
                break;

            case AndruavMessage_Ctrl_Camera.TYPE_AndruavResala_Ctrl_Camera:
                andruavMessageBase = new AndruavMessage_Ctrl_Camera();
                break;

            case AndruavMessage_GimbalCtrl.TYPE_AndruavMessage_GimbalCtrl:
                andruavMessageBase = new AndruavMessage_GimbalCtrl();
                break;
            case AndruavMessage_UploadWayPoints.TYPE_AndruavMessage_UploadWayPoints:
                andruavMessageBase = new AndruavMessage_UploadWayPoints();
                break;
            case AndruavMessage_RemoteControlSettings.TYPE_RemoteControlSettings:
                andruavMessageBase = new AndruavMessage_RemoteControlSettings();
                break;
            case AndruavMessage_SetHomeLocation.TYPE_AndruavMessage_SetHomeLocation:
                andruavMessageBase = new AndruavMessage_SetHomeLocation();
                break;
            case AndruavMessage_CameraZoom.TYPE_AndruavMessage_CameraZoom:
                andruavMessageBase = new AndruavMessage_CameraZoom();
                break;
            case AndruavMessage_CameraSwitch.TYPE_AndruavMessage_CameraSwitch:
                andruavMessageBase = new AndruavMessage_CameraSwitch();
                break;
            case AndruavMessage_CameraFlash.TYPE_AndruavResala_CameraFlash:
                andruavMessageBase = new AndruavMessage_CameraFlash();
                break;
            case AndruavMessage_RemoteControl2.TYPE_AndruavMessage_RemoteControl2:
                andruavMessageBase = new AndruavMessage_RemoteControl2();
                break;
            case AndruavMessage_SensorsStatus.TYPE_AndruavMessage_SensorsStatus:
                andruavMessageBase = new AndruavMessage_SensorsStatus();
                break;
            case AndruavMessage_ServoChannel.TYPE_AndruavMessage_ServoChannel:
                andruavMessageBase = new AndruavMessage_ServoChannel();
                break;
            case AndruavMessage_Config_Preference.TYPE_AndruavResala_Config_Preference:
                andruavMessageBase = new AndruavMessage_Config_Preference();
                break;
            case AndruavSystem_Ping.TYPE_AndruavSystem_Ping:
                andruavMessageBase = new AndruavSystem_Ping();
                break;
            case AndruavSystem_LogoutCommServer.TYPE_AndruavSystem_LogoutCommServer:
                andruavMessageBase = new AndruavSystem_LogoutCommServer();
                break;
            case AndruavSystem_ConnectedCommServer.TYPE_AndruavSystem_ConnectedCommServer:
                andruavMessageBase = new AndruavSystem_ConnectedCommServer();
                break;
            case AndruavSystem_UdpProxy.TYPE_AndruavSystem_UdpProxy:
                andruavMessageBase = new AndruavSystem_UdpProxy();
                break;
            case AndruavModule_ID.TYPE_AndruavModule_ID:
                andruavMessageBase = new AndruavModule_ID();
                break;

            default:
                throw new IllegalArgumentException("unknown messageType:" + messageType);

        }

        return andruavMessageBase;
    }


    private static int log_exceptionparser_counter = 5;

    /**
     * Parses binary message and put it in a binary command.
     * <br> it make use of {@link #getAndruavMessageBinary(int)}
     *
     * @param messageCMD array of bytes of received command.
     * @return An Instance of {@link AndruavBinary_2MR} or an inherited class of it.
     */
    public static AndruavBinary_2MR parseBinary(byte[] messageCMD) {

        try {

            AndruavBinary_2MR andruavBinary_2MR = new AndruavBinary_2MR();


            final int i = AndruavBinaryHelper.getSplitIndex(messageCMD);
            if (i == -1) return null; // bad message
            String messageJson = new String(messageCMD, 0, i);

            final JSONObject json_receive_data = new JSONObject(messageJson);

            if (json_receive_data.has(ProtocolHeaders.MessageType)) {

                int messageType = Integer.parseInt(json_receive_data.getString(ProtocolHeaders.MessageType));
                andruavBinary_2MR.andruavResalaBinaryBase = getAndruavMessageBinary(messageType);
            } else {
                // no internal message exist.
                andruavBinary_2MR.andruavResalaBinaryBase = new AndruavResalaBinaryBase();
            }


            if (json_receive_data.has(ProtocolHeaders.MSG_ROUTING)) {
                andruavBinary_2MR.MessageRouting = json_receive_data.getString(ProtocolHeaders.MSG_ROUTING);
            }
            if (json_receive_data.has(ProtocolHeaders.Sender)) {
                andruavBinary_2MR.partyID = json_receive_data.getString(ProtocolHeaders.Sender);
            }
            if (json_receive_data.has(ProtocolHeaders.Group)) {
                andruavBinary_2MR.groupName = json_receive_data.getString(ProtocolHeaders.Group);
            }
            if (json_receive_data.has(ProtocolHeaders.TimeStamp)) {
                andruavBinary_2MR.timeStamp = json_receive_data.getString(ProtocolHeaders.TimeStamp);
                //andruavCMD.timeDiff = System.currentTimeMillis() - Long.parseLong(andruavCMD.timeStamp);
            }
            if (json_receive_data.has(ProtocolHeaders.Target)) {
                andruavBinary_2MR.targetName = json_receive_data.getString(ProtocolHeaders.Target);
            }

            final byte[] binaryMessage = new byte[messageCMD.length - i - 1];
            System.arraycopy(messageCMD, i + 1, binaryMessage, 0, binaryMessage.length);

            andruavBinary_2MR.setMessageText(binaryMessage);

            return andruavBinary_2MR;
        } catch (BadPaddingException e) {
            AndruavEngine.notification().displayNotification(INotification.NOTIFICATION_TYPE_ERROR, "Error", "Bad encryption key", true, INotification.INFO_TYPE_PROTOCOL, false);
            return null;
        } catch (Exception e) {
            if (log_exceptionparser_counter==0) return null;
            log_exceptionparser_counter = log_exceptionparser_counter -1;
            AndruavEngine.log().logException(AndruavSettings.Account_SID, "exception-parser", e);
            return null;
        }
    }

    /***
     * Parses binary message and put it in a text command.
     * <br><b>IF YOU WANT TO CHANGE COMMAND SYNTAX YOU NEED TO EDIT ALSO Andruav_2MR {@link Andruav_2MR}</b>
     *
     * @param messageCMD a string of received command that is JSON based format.
     * @return An Instance of {@link Andruav_2MR} or an inherited class of it.
     */
    public static Andruav_2MR parseText(final String messageCMD) {
        try {
            Andruav_2MR andruav2MR = new Andruav_2MR();

            andruav2MR.setMessageText(messageCMD);


            return andruav2MR;
        }
        catch (Exception e) {
            // TODO: add error event here.
            if (log_exceptionparser_counter==0) return null;
            log_exceptionparser_counter = log_exceptionparser_counter -1;
            AndruavEngine.log().logException(AndruavSettings.Account_SID, "exception-parser", e);

            return null;
        }


    }
}
