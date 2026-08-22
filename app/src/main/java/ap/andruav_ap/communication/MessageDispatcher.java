package ap.andruav_ap.communication;

import com.andruav.AndruavDroneFacade;
import com.andruav.AndruavEngine;
import com.andruav.AndruavFacade;
import com.andruav.AndruavMeFacade;
import com.andruav.AndruavSettings;
import com.andruav.Constants;
import com.andruav.event.droneReport_Event.Event_TelemetryGCSRequest;
import com.andruav.event.droneReport_Event.Event_WayPointReached;
import com.andruav.event.fcb_event._7adath_FCB_2AMR;
import com.andruav.event.fcb_event.Event_FCBData;
import com.andruav.event.fcb_event.Event_SocketData;
import com.andruav.event.fpv7adath.Event_FPV_CMD;
import com.andruav.event.fpv7adath._7adath_InitAndroidCamera;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.controlBoard.shared.missions.MissionBase;
import com.andruav.protocol.commands.binaryMessages.AndruavBinary_2MR;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMG;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_IMUStatistics;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_LightTelemetry;
import com.andruav.protocol.commands.textMessages.AndruavMessage_DroneReport;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraList;
import com.andruav.protocol.commands.textMessages.AndruavMessage_ServoChannel;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Sound_TextToSpeech;
import com.andruav.protocol.commands.textMessages.AndruavMessage_UDPProxy_Info;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;
import com.andruav.protocol.commands.textMessages.Configuration.AndruavMessage_Config_COM;
import com.andruav.protocol.commands.textMessages.Configuration.AndruavMessage_Config_Preference;
import com.andruav.protocol.commands.textMessages.Configuration.AndruavMessage_Config_UnitID;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_Ctrl_Camera;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_RemoteExecute;

import org.greenrobot.eventbus.EventBus;

import ap.andruav_ap.App;
import ap.andruav_ap.Emergency;
import ap.andruavmiddlelibrary.eventClasses.fpvEvent.Event_FPV_Image;
import ap.andruavmiddlelibrary.eventClasses.fpvEvent.Event_FPV_VideoURL;
import ap.andruavmiddlelibrary.eventClasses.remoteControl.Event_RemoteServo;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.sensors._7asasatEvents.Event_IMU_CMD;

/**
 * Owns the inbound message dispatch logic that used to live inline in
 * {@link AndruavWSClient_TooTallNate}.
 * <p>
 * The WebSocket client ({@code AndruavWSClient_TooTallNate}) is now responsible
 * only for transport: connect / disconnect / send / receive and the EventBus
 * subscribers that drive outbound telemetry. Every received message that needs
 * protocol-level handling is delegated here.
 * <p>
 * The four entry points mirror the {@code protected abstract} hooks on
 * {@code AndruavWSClientBase}:
 * <ul>
 *   <li>{@link #onBinaryMessage(AndruavBinary_2MR)} - per-binary-message receive hook.</li>
 *   <li>{@link #executeInternalBinaryCommand(AndruavBinary_2MR)} - binary command switch.</li>
 *   <li>{@link #executeInternalCommand(Andruav_2MR)} - text command switch.</li>
 *   <li>{@link #executeRemoteExecuteCMD(Andruav_2MR)} - remote-execute command switch.</li>
 * </ul>
 * All of them operate purely through the static Andruav facades ({@code AndruavEngine},
 * {@code AndruavFacade}, {@code AndruavDroneFacade}, {@code EventBus}, {@code Preference},
 * {@code App.notification}, {@code Emergency}) so the dispatcher carries no transport
 * state of its own.
 */
public class MessageDispatcher {

    private final UnitDiscoveryHandler unitDiscoveryHandler;

    public MessageDispatcher() {
        this(new UnitDiscoveryHandler());
    }

    public MessageDispatcher(final UnitDiscoveryHandler unitDiscoveryHandler) {
        this.unitDiscoveryHandler = unitDiscoveryHandler;
    }


    void onBinaryMessage(final AndruavBinary_2MR andruavBinary2MR)
    {

        final AndruavUnitShadow andruavUnit =(AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(andruavBinary2MR.partyID);
        if (andruavUnit == null) {
            //no need tp make anything here, as logic of adding new units
            // should be in App.andruavWSClient.andruavUnitMap.get function.
            return;
        }

        if (andruavBinary2MR.andruavResalaBinaryBase.messageTypeID == AndruavResalaBinary_IMG.TYPE_AndruavMessage_IMG) {
            final AndruavResalaBinary_IMG andruavMessage_img = (AndruavResalaBinary_IMG) andruavBinary2MR.andruavResalaBinaryBase;
            final Event_FPV_Image event_fpv_image = new Event_FPV_Image();
            event_fpv_image.Sender = andruavUnit.PartyID;
            event_fpv_image.andruavUnit = andruavUnit;
            event_fpv_image.isLocalImage = false;
            event_fpv_image.isVideo = false;
            event_fpv_image.ImageBytes = andruavMessage_img.getImage();
            event_fpv_image.Description = andruavMessage_img.Description;
            event_fpv_image.ImageLocation = andruavMessage_img.ImageLocation;

            EventBus.getDefault().post(event_fpv_image);

            andruavBinary2MR.processed = true;

        }
    }


    /***
     * Executes commands embedded in the message section.
     * @param andruav_2MR
     */
    void executeInternalBinaryCommand(final AndruavBinary_2MR andruav_2MR) {
        switch (andruav_2MR.andruavResalaBinaryBase.messageTypeID)
        {

            case AndruavResalaBinary_IMUStatistics.TYPE_AndruavMessage_IMUStatistics:
                // Update Statistics

                andruav_2MR.processed = true;
                AndruavEngine.getAndruavWe7daMapBase().updateIMUStatistics(andruav_2MR);

                andruav_2MR.processed = true;

                break;

            case AndruavResalaBinary_LightTelemetry.TYPE_AndruavMessage_LightTelemetry: {
                andruav_2MR.processed = true;
                final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(andruav_2MR.partyID);
                if (andruavUnit == null) {
                    // telemetry from Unknown unit ignore it
                    andruav_2MR.processed = true;
                    break;
                }
                AndruavResalaBinary_LightTelemetry andruavResalaBinary_lightTelemetry = (AndruavResalaBinary_LightTelemetry) andruav_2MR.andruavResalaBinaryBase;

                if (AndruavSettings.andruavWe7daBase.getIsCGS())
                {
                    Event_FCBData event_FCBData = new Event_FCBData();
                    event_FCBData.senderWe7da = andruavUnit;
                    event_FCBData.IsLocal = Event_SocketData.SOURCE_REMOTE;
                    event_FCBData.Data = andruavResalaBinary_lightTelemetry.getData();
                    event_FCBData.DataLength = event_FCBData.Data.length;
                    EventBus.getDefault().post(event_FCBData);
                }
                else {
                    Event_SocketData event_socketData = new Event_SocketData();
                    event_socketData.senderWe7da = andruavUnit;
                    event_socketData.IsLocal = Event_SocketData.SOURCE_REMOTE;
                    event_socketData.Data = andruavResalaBinary_lightTelemetry.getData();
                    event_socketData.DataLength = event_socketData.Data.length;
                    EventBus.getDefault().post(event_socketData);

                }
                andruav_2MR.processed = true;
                break;
            }

            /*case AndruavResalaBinary_Telemetry.TYPE_AndruavMessage_Telemetry: {

                andruav_2MR.processed = true;
                final AndruavWe7daShadow andruavUnit = (AndruavWe7daShadow) AndruavMo7arek.getAndruavWe7daMapBase().get(andruav_2MR.senderName);
                if (andruavUnit==null)
                {
                    // telemetry from Unknown unit ignore it
                    andruav_2MR.processed = true;
                    break;
                }
                AndruavResalaBinary_Telemetry andruavMessage_telemetry = (AndruavResalaBinary_Telemetry) andruav_2MR.andruavResalaBinaryBase;

                int source = andruavMessage_telemetry.getSource();
                if (source == AndruavResalaBinary_Telemetry.SOURCE_FCB) {   // Data from Drone .... This part of Code executed only in GCS Mode.
                    // SOME TIMES Drone broadcasts Telemetry ... that means it will reach other Drone ...
                    if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
                        Event_FCBData event_FCBData = new Event_FCBData();
                        event_FCBData.senderWe7da = andruavUnit;
                        event_FCBData.IsLocal = Event_SocketData.SOURCE_REMOTE;
                        event_FCBData.Data = andruavMessage_telemetry.getData();
                        event_FCBData.DataLength = event_FCBData.Data.length;
                        EventBus.getDefault().post(event_FCBData);
                    }

                } else if (source == AndruavResalaBinary_Telemetry.SOURCE_GCS) {   // Data from GCS .... This part of Code executed only in Drone Mode.
                    Event_SocketData event_socketData = new Event_SocketData();
                    event_socketData.senderWe7da = andruavUnit;
                    event_socketData.IsLocal = Event_SocketData.SOURCE_REMOTE;
                    event_socketData.Data = andruavMessage_telemetry.getData();
                    event_socketData.DataLength = event_socketData.Data.length;
                    EventBus.getDefault().post(event_socketData);

                }

                andruav_2MR.processed = true;

            }
            break;*/



//            case AndruavResalaBinary_RemoteControl.TYPE_AndruavMessage_RemoteControl:
//                final AndruavWe7daShadow andruavUnit = (AndruavWe7daShadow) AndruavMo7arek.getAndruavWe7daMapBase().get(andruav_2MR.partyID);
//                if ((andruavUnit!=null) && (!AndruavSettings.andruavWe7daBase.canControl()))
//                {
//                    andruav_2MR.processed = true;
//                }
//
//                EventRemote_ChannelsCMD eventRemote_Channels_cmd = new EventRemote_ChannelsCMD(
//                        RemoteControl.calculateChannels2(((AndruavResalaBinary_RemoteControl) andruav_2MR.andruavResalaBinaryBase).getChannelsCopy(),false));
//                // DONT CHANGE VALUE then UNCOMMENT      ((AndruavResalaBinary_RemoteControl) andruav_2MR.andruavResalaBinaryBase).getChannelsCopy());
//
//                eventRemote_Channels_cmd.PartyID = AndruavSettings.andruavWe7daBase.PartyID; // ME
//                eventRemote_Channels_cmd.Engaged = ((AndruavResalaBinary_RemoteControl)andruav_2MR.andruavResalaBinaryBase).isEngaged;
//                EventBus.getDefault().post(eventRemote_Channels_cmd);
//
//                andruav_2MR.processed = true;
//
//                break;


            default:
                // Other message are either reply, IMU or other info data.
        }

    }




    /***
     * Executes commands embedded in the message section.
     * @param andruav2MR
     */
    void executeInternalCommand(final Andruav_2MR andruav2MR) {


        switch (andruav2MR.andruavMessageBase.messageTypeID)
        {

            case AndruavMessage_Config_Preference.TYPE_AndruavResala_Config_Preference:

                andruav2MR.processed = true;
                final AndruavMessage_Config_Preference andruavResala_config_preference = (AndruavMessage_Config_Preference) andruav2MR.andruavMessageBase;

                switch (andruavResala_config_preference.TValue)
                {
                    case 1:
                        Preference.setPreference(null,andruavResala_config_preference.KEYID,andruavResala_config_preference.IValue);
                        break;
                    case 2:
                        Preference.setPreference(null,andruavResala_config_preference.KEYID,andruavResala_config_preference.BValue);
                        break;
                    case 3:
                        Preference.setPreference(null,andruavResala_config_preference.KEYID,andruavResala_config_preference.SValue);
                        break;
                }

                break;

            case AndruavMessage_Config_UnitID.TYPE_AndruavMessage_Config_UnitID:
                andruav2MR.processed = true;

                // This is a request from outside [Drone or GCS] to all or a specific
                // fence detail. fencename is specific is sent in a variable "fn" -fence name-.
                if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
                    final AndruavMessage_Config_UnitID andruavResala_Config_unitID = (AndruavMessage_Config_UnitID) andruav2MR.andruavMessageBase;
                    AndruavSettings.andruavWe7daBase.UnitID = andruavResala_Config_unitID.UnitID.toLowerCase();
                    AndruavSettings.andruavWe7daBase.Description = andruavResala_Config_unitID.Description;
                    AndruavSettings.andruavWe7daBase.GroupName = andruavResala_Config_unitID.GroupName.toLowerCase();

                    // Save permanent
                    Preference.setWebServerUserName(null,AndruavSettings.andruavWe7daBase.UnitID);
                    Preference.setWebServerUserDescription(null, andruavResala_Config_unitID.Description);
                    Preference.setWebServerGroupName(null, andruavResala_Config_unitID.GroupName);

                    // BUG: CALL RECONNECT HERE

                }
                break;

            case AndruavMessage_Config_COM.TYPE_AndruavResala_Config_COM:
            {
                andruav2MR.processed = true;
                if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
                    final AndruavMessage_Config_COM andruavResala_config_com = (AndruavMessage_Config_COM) andruav2MR.andruavMessageBase;

                    // YOU DONT NEED TO RESET [AndruavSettings.WebServerURL .... ]
                    // You NEED TO RECONNECT.

                    Preference.setWebServerPort(null,andruavResala_config_com.Port);
                    Preference.setWebServerURL(null,andruavResala_config_com.ServerIP);
                    Preference.isLocalServer(null,andruavResala_config_com.IsLocalServer);
                    Preference.setWebServerPort(null,andruavResala_config_com.Port);

                    // BUG: CALL RECONNECT HERE
                }
            }
            break;

            case AndruavMessage_DroneReport.TYPE_AndruavMessage_DroneReport:
            {
                andruav2MR.processed = true;
                final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(andruav2MR.partyID);
                if (andruavUnit != null) {
                    AndruavMessage_DroneReport andruavMessage_droneReport = (AndruavMessage_DroneReport) andruav2MR.andruavMessageBase;

                    try {
                        final MissionBase missionBase = andruavUnit.getMohemmaMapBase().valueAt(andruavMessage_droneReport.mParameter1);
                        if (missionBase !=null)
                        {
                            missionBase.Status = andruavMessage_droneReport.mReportType;

                        }
                        else
                        {
                            AndruavFacade.requestWayPoints(andruavUnit);
                        }

                    }
                    catch (final  java.lang.ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException)
                    {
                        // ignore
                        // TODO: request new Mission Map
                    }
                    catch (final Exception ex)
                    {
                        AndruavEngine.log().logException("apm_mission", ex);
                    }
                    EventBus.getDefault().post(new Event_WayPointReached(andruavUnit, andruavMessage_droneReport.mParameter1)); // inform all that a data is ready


                }

            }
            break;



            case AndruavMessage_Error.TYPE_AndruavMessage_Error: {
                andruav2MR.processed = true;
                final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(andruav2MR.partyID);
                if ((andruavUnit != null) && (AndruavSettings.andruavWe7daBase.getIsCGS())) {

                    // dont display errors of other units in a  mobile working in drone mode. very confusing.
                    AndruavMessage_Error andruavMessage_error = ((AndruavMessage_Error) (andruav2MR.andruavMessageBase));
                    String err = andruavMessage_error.Description;
                    App.notification.displayNotification(andruavMessage_error.notification_Type, andruavUnit.UnitID, err, true, andruavMessage_error.infoType, false);
                    AndruavEngine.notification().Speak(err);
                }
            }
            break;


            case AndruavMessage_ServoChannel.TYPE_AndruavMessage_ServoChannel:
                andruav2MR.processed = true;
                Event_RemoteServo event_remoteServo = new Event_RemoteServo(((AndruavMessage_ServoChannel) andruav2MR.andruavMessageBase).channelNumber, ((AndruavMessage_ServoChannel) andruav2MR.andruavMessageBase).channelValue);
                event_remoteServo.PartyID = AndruavSettings.andruavWe7daBase.PartyID; // ME
                EventBus.getDefault().post(event_remoteServo);

                break;

            case AndruavMessage_CameraList.TYPE_AndruavCMD_CameraList: {
                andruav2MR.processed = true;
                final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(andruav2MR.partyID);

                if (andruavUnit == null) break;

                AndruavMessage_CameraList andruavMessage_cameraList = (AndruavMessage_CameraList) andruav2MR.andruavMessageBase;

                if (andruavMessage_cameraList.isReply) {
                    // UAVIS can sendMessageToModule isReply = false to inform GCS about available cameras.



                    Event_FPV_VideoURL event_fpv_videoURL = new Event_FPV_VideoURL();
                    event_fpv_videoURL.andruavUnit = andruavUnit;
                    event_fpv_videoURL.ExternalType = andruavMessage_cameraList.ExternalType;
                    event_fpv_videoURL.VideoTracks = andruavMessage_cameraList.videoTracks;
                    event_fpv_videoURL.isReply = andruavMessage_cameraList.isReply;
                    EventBus.getDefault().post(event_fpv_videoURL);
                }
            }
            break;

            case AndruavMessage_Sound_TextToSpeech.TYPE_AndruavMessage_Sound_TextToSpeech:
                andruav2MR.processed = true;
                AndruavMessage_Sound_TextToSpeech andruavMessage_textToSpeech = (AndruavMessage_Sound_TextToSpeech) andruav2MR.andruavMessageBase;
                AndruavEngine.notification().SpeakNow(andruavMessage_textToSpeech.text);
                break;

            default:
                // Other message are either reply, IMU or other info data.
                // andruav2MR.processed = false; << THIS IS A BUG
                // just update the unit... some commands are not executed here
                // such as TYPE_AndruavMessage_Telemetry
                //andruavUnitMap.updateLastActiveTime (andruav2MR.PartyID);
        }
    }



    /***
     * Executes commands sent from remote users via [TYPE_AndruavMessage_RemoteExecute]
     * @param andruav_2MR
     */
    void executeRemoteExecuteCMD(final Andruav_2MR andruav_2MR) {
        try {

            Event_FPV_CMD event_fpv_cmd;

            switch (andruav_2MR.andruavMessageBase.messageTypeID) {

                // this is data from Another Andruav
                case AndruavMessage_RemoteExecute.TYPE_AndruavMessage_RemoteExecute: {


                    AndruavMessage_RemoteExecute andruavResala_remoteExecute = ((AndruavMessage_RemoteExecute) (andruav_2MR.andruavMessageBase));
                    int CMD_ID = andruavResala_remoteExecute.RemoteCommandID;
                    final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(andruav_2MR.partyID);

                    switch (CMD_ID) {


                        case AndruavMessage_UDPProxy_Info.TYPE_AndruavMessage_UdpProxy_Info:
                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break;
                            AndruavFacade.sendUdpProxyStatus(andruavUnit);
                            break;

                        case AndruavMessage_CameraList.TYPE_AndruavCMD_CameraList:
                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break; // not a valid command to GCSevent_fpv_cmd = new _7adath_FPV_CMD(_7adath_FPV_CMD.FPV_CMD_TAKEIMAGE);

                            AndruavDroneFacade.sendCameraList(true,andruavUnit);
                            EventBus.getDefault().post(new _7adath_InitAndroidCamera());
                            break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_MAKETILT:

                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break;
                            EventBus.getDefault().post(new Event_IMU_CMD(Event_IMU_CMD.IMU_CMD_UpdateZeroTilt));
                            break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_MAKEBEEP: {
                            if ((andruavUnit != null) && (!andruavUnit.canControl())) break;
                            final Emergency emergency = (Emergency) AndruavEngine.getEmergency();
                            if (emergency != null) {
                                emergency.triggerSirenByGCS(!emergency.getIsSirenActive());
                            }
                        }
                        break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_MAKEFLASH: {
                            if ((andruavUnit != null) && (!andruavUnit.canControl())) break;
                            final Emergency emergency = (Emergency) AndruavEngine.getEmergency();
                            if (emergency != null) {
                                emergency.triggerFlashByGCS(!emergency.getIsFlashing());
                            }
                        }
                        break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_TAKEIMAGE:
                            // DEPRECATED As command is too complex to me included in another command
                            break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_SET_GPS_SOURCE:
                            if ((andruavUnit != null) && (!andruavUnit.canControl())) break;
                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break;
                            AndruavSettings.andruavWe7daBase.setGPSMode(Integer.parseInt(((AndruavMessage_RemoteExecute) (andruav_2MR.andruavMessageBase)).Variables.get("s")));
                            break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_CONNECT_FCB:
                            if ((andruavUnit != null) && (!(andruavUnit.canControl() || andruavUnit.canTelemetry()))) break;
                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break;
                            final _7adath_FCB_2AMR adath_fcb_2AMR = new _7adath_FCB_2AMR();
                            adath_fcb_2AMR.enForceConnection = true;
                            EventBus.getDefault().post(adath_fcb_2AMR);
                            break;


                        // StartStop recording video
                        case AndruavMessage_RemoteExecute.RemoteCommand_RECORDVIDEO:
                            if ((andruavUnit != null) && (!andruavUnit.canVideo())) break;
                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break;

                            event_fpv_cmd = new Event_FPV_CMD(Event_FPV_CMD.FPV_CMD_RECORDVIDEO);
                            event_fpv_cmd.Requester = andruavUnit;
                            event_fpv_cmd.ACT = andruavResala_remoteExecute.getBooleanValue("Act");
                            EventBus.getDefault().post(event_fpv_cmd);
                            break;


                        case AndruavMessage_RemoteExecute.RemoteCommand_STREAMVIDEORESUME:
                            if ((andruavUnit != null) && (!andruavUnit.canVideo())) break;
                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break;
                            break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_ChangeUnitID:

                            break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_STREAMVIDEO:
                            if ((andruavUnit != null) && (!andruavUnit.canVideo())) break; // not permitted
                            if (AndruavSettings.andruavWe7daBase.getIsCGS())
                                break;

                            if (andruavUnit != null) {

                                if (andruavResala_remoteExecute.getBooleanValue("Act")) {
                                    EventBus.getDefault().post(new _7adath_InitAndroidCamera());

                                    if (AndruavSettings.mVideoRequests.get(andruavUnit.PartyID) == null) {

                                        // add requester to the List
                                        AndruavSettings.mVideoRequests.put(andruavUnit.PartyID, andruavUnit);
                                    }
                                } else {
                                    // this is WRONG not after UAVOS update
                                    AndruavSettings.mVideoRequests.remove(andruavUnit.PartyID);
                                    // Do NOT auto-stop on viewer disconnect. Stopping the service
                                    // kills the PeerConnectionManager but the FPV Activity keeps the
                                    // service alive (BIND_AUTO_CREATE), leaving a shell that can't
                                    // restream when the web reconnects. Keep capture alive — the
                                    // user stops streaming via the FPV exit button.
                                }


                                if (andruavResala_remoteExecute.Variables.containsKey("CH")
                                        && andruavResala_remoteExecute.Variables.get("CH").equals(AndruavSettings.andruavWe7daBase.PartyID))
                                {

                                    return ;
                                }

                            } else {
                                unitDiscoveryHandler.handleUnknownUnit(andruav_2MR.partyID);
                            }
                            // should be called last as FPV form checks on mVideoRequests to turn on video or not.
                            // So this function should eb called anyway unless u use external cam.

                            andruav_2MR.processed = true;

                            break;


                        case AndruavMessage_RemoteExecute.RemoteCommand_ROTATECAM:
                            if ((andruavUnit != null) && (!andruavUnit.canVideo())) break;
                            //sendMessageToModule internal command to FPV activity
                            event_fpv_cmd = new Event_FPV_CMD(Event_FPV_CMD.FPV_CMD_ROTATECAM);
                            event_fpv_cmd.Requester = andruavUnit;
                            EventBus.getDefault().post(event_fpv_cmd);

                            andruav_2MR.processed = true;
                            break;


                        case AndruavMessage_RemoteExecute.RemoteCommand_IMUCTRL:
                            if (andruavUnit != null) {
                                if (andruavResala_remoteExecute.getBooleanValue("Act")) {
                                    if (!AndruavSettings.mIMURequests.contains(andruavUnit)) {
                                        AndruavSettings.mIMURequests.add(andruavUnit);
                                    }
                                } else {
                                    AndruavSettings.mIMURequests.remove(andruavUnit);
                                }
                            } else {
                                unitDiscoveryHandler.handleUnknownUnit(andruav_2MR.partyID);
                            }

                            andruav_2MR.processed = true;

                            break;


                        case AndruavMessage_RemoteExecute.RemoteCommand_TELEMETRYCTRL:
                            if (andruavUnit != null)  {
                                final int request = andruavResala_remoteExecute.getIntValue("Act");
                                switch (request)
                                {
                                    case Event_TelemetryGCSRequest.REQUEST_END: {
                                        if (AndruavSettings.mTelemetryRequests.contains(andruavUnit)) {
                                            AndruavSettings.mTelemetryRequests.remove(andruavUnit);
                                            AndruavEngine.getEventBus().post(new Event_TelemetryGCSRequest(andruavUnit, Event_TelemetryGCSRequest.REQUEST_END));
                                        }
                                    }
                                    break;

                                    case Event_TelemetryGCSRequest.ADJUST_RATE: {
                                        final int LVL = andruavResala_remoteExecute.getIntValue("LVL", Constants.SMART_TELEMETRY_LEVEL_NEGLECT);
                                        if (LVL != Constants.SMART_TELEMETRY_LEVEL_NEGLECT) {
                                            Preference.setSmartMavlinkTelemetry(null, LVL);
                                        }
                                    }
                                        break;
                                    case Event_TelemetryGCSRequest.REQUEST_RESUME: {
                                        if (!AndruavSettings.andruavWe7daBase.isUdpProxyEnabled()) {
                                            // proxy not created on server yet — request creation.
                                            // server response triggers setUdpConfig -> sendUdpProxyStatus broadcast.
                                            AndruavFacade.StartUdpProxyTelemetry();
                                        } else {
                                            AndruavEngine.getUDPProxy().setPause(false);
                                            AndruavFacade.sendUdpProxyStatus(andruavUnit);
                                        }
                                        if (andruavUnit.canTelemetry()) {
                                            if (!AndruavSettings.mTelemetryRequests.contains(andruavUnit)) {
                                                AndruavSettings.mTelemetryRequests.add(andruavUnit);
                                                AndruavEngine.getEventBus().post(new Event_TelemetryGCSRequest(andruavUnit, Event_TelemetryGCSRequest.REQUEST_START));
                                            } else {
                                                AndruavEngine.getEventBus().post(new Event_TelemetryGCSRequest(andruavUnit, Event_TelemetryGCSRequest.REQUEST_RESUME));
                                            }
                                            final int LVL = andruavResala_remoteExecute.getIntValue("LVL", Constants.SMART_TELEMETRY_LEVEL_NEGLECT);
                                            if (LVL != Constants.SMART_TELEMETRY_LEVEL_NEGLECT) {
                                                Preference.setSmartMavlinkTelemetry(null, LVL);
                                            }
                                        }
                                    }
                                    break;
                                    case Event_TelemetryGCSRequest.REQUEST_PAUSE: {
                                        if (AndruavSettings.andruavWe7daBase.isUdpProxyEnabled()) {
                                            AndruavEngine.getUDPProxy().setPause(true);
                                        }
                                        AndruavFacade.sendUdpProxyStatus(andruavUnit);
                                    }
                                    break;
                                    default:
                                    {
                                        if (andruavUnit.canTelemetry()) {
                                            // add or resume both make sure they are added in our request list
                                            if (!AndruavSettings.mTelemetryRequests.contains(andruavUnit)) {
                                                AndruavSettings.mTelemetryRequests.add(andruavUnit);
                                                AndruavEngine.getEventBus().post(new Event_TelemetryGCSRequest(andruavUnit, Event_TelemetryGCSRequest.REQUEST_START));
                                            } else {
                                                AndruavEngine.getEventBus().post(new Event_TelemetryGCSRequest(andruavUnit, request));
                                            }

                                            // Here Dont SEND ... That is why I replicated [AndruavMo7arek.getEventBus().post(new _7adath_TelemetryGCSRequest(andruavUnit,add ));]  instead of making a bool value
                                            // AndruavMo7arek.getEventBus().post(new _7adath_TelemetryGCSRequest(andruavUnit,add ));
                                            //

                                            // Update Smart Telemetry Level if Requested
                                            // Adjust Rate also if required
                                            final int LVL = andruavResala_remoteExecute.getIntValue("LVL", Constants.SMART_TELEMETRY_LEVEL_NEGLECT);
                                            if (LVL != Constants.SMART_TELEMETRY_LEVEL_NEGLECT) {
                                                Preference.setSmartMavlinkTelemetry(null, LVL);
                                            }

                                            // Create UDP proxy on server if not already created.
                                            // server response triggers setUdpConfig -> sendUdpProxyStatus broadcast.
                                            if (!AndruavSettings.andruavWe7daBase.isUdpProxyEnabled()) {
                                                AndruavFacade.StartUdpProxyTelemetry();
                                            } else {
                                                AndruavFacade.sendUdpProxyStatus(andruavUnit);
                                            }
                                        }
                                    }
                                        break;
                                }

                            } else {
                                unitDiscoveryHandler.handleUnknownUnit(andruav_2MR.partyID);
                            }

                            andruav_2MR.processed = true;

                            break;


                        case AndruavMessage_RemoteExecute.RemoteCommand_SENDSMS:
                            if ((andruavUnit != null) && (!andruavUnit.canControl())) {
                                AndruavEngine.log().log2(andruav_2MR.partyID, "sms_skip", "SENDSMS skipped: unit not controllable");
                                break;
                            }

                            // sendMessageToModule SMS immediate long as there is a current mLocation defined.
                            andruav_2MR.processed = true;
                            final Emergency emergency = (Emergency) AndruavEngine.getEmergency();
                            if (emergency != null) {
                                emergency.sendSMS(true);
                            } else {
                                AndruavEngine.log().log2(andruav_2MR.partyID, "sms_skip", "SENDSMS skipped: Emergency module is null");
                            }


                            break;

                        case AndruavMessage_RemoteExecute.RemoteCommand_SMSwGPS: {
                            if ((andruavUnit != null) && (!andruavUnit.canControl())) {
                                AndruavEngine.log().log2(andruav_2MR.partyID, "sms_skip", "SMSwGPS skipped: unit not controllable");
                                break;
                            }

                            andruav_2MR.processed = true;
                            final Emergency emergencySMSwGPS = (Emergency) AndruavEngine.getEmergency();
                            if (emergencySMSwGPS != null) {
                                // Optional variable "n" selects a custom receiver phone number.
                                // When omitted, falls back to the unit's configured recovery number.
                                if (andruavResala_remoteExecute.Variables.containsKey("n")) {
                                    final String receiverNum = andruavResala_remoteExecute.Variables.get("n");
                                    if (receiverNum != null && !receiverNum.isEmpty()) {
                                        AndruavEngine.log().log2(andruav_2MR.partyID, "sms_cmd", "SMSwGPS: sending to " + receiverNum);
                                        emergencySMSwGPS.sendSMSLocation(receiverNum, true);
                                        break;
                                    }
                                }
                                AndruavEngine.log().log2(andruav_2MR.partyID, "sms_cmd", "SMSwGPS: no 'n' variable, falling back to recovery number");
                                emergencySMSwGPS.sendSMS(true);
                            } else {
                                AndruavEngine.log().log2(andruav_2MR.partyID, "sms_skip", "SMSwGPS skipped: Emergency module is null");
                            }
                        }
                        break;


                        default:
                            // unknown command ... maybe a new protocol version
                            // just ignore
                            //TODO: sendMessageToModule message UNKNOWN command to sender to understand that he is talking new language
                            break;
                    }
                }
                break;

                case AndruavMessage_Ctrl_Camera.TYPE_AndruavResala_Ctrl_Camera:
                    final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(andruav_2MR.partyID);
                    if ((andruavUnit != null) && (!andruavUnit.canImage())) break;

                    andruav_2MR.processed = true;

                    final AndruavMessage_Ctrl_Camera andruavResala_ctrl_camera = (AndruavMessage_Ctrl_Camera) (andruav_2MR.andruavMessageBase);
                    if (AndruavSettings.andruavWe7daBase.getIsCGS())
                        break;


                    EventBus.getDefault().post(new _7adath_InitAndroidCamera());

                    event_fpv_cmd = new Event_FPV_CMD(Event_FPV_CMD.FPV_CMD_TAKEIMAGE);

                    event_fpv_cmd.CameraSource    = andruavResala_ctrl_camera.CameraSource;
                    event_fpv_cmd.NumberOfImages    = andruavResala_ctrl_camera.NumberOfImages;
                    event_fpv_cmd.TimeBetweenShotes = andruavResala_ctrl_camera.TimeBetweenShotes;
                    event_fpv_cmd.DistanceBetweenShotes = andruavResala_ctrl_camera.DistanceBetweenShotes;
                    event_fpv_cmd.SendBackImages =  andruavResala_ctrl_camera.SendBackImages;

                    event_fpv_cmd.Requester = andruavUnit;

                    AndruavMeFacade.Ctrl_Camera(event_fpv_cmd);



                    break;
            }
        } catch (Exception ex) {
            AndruavEngine.log().logException("RemoteExecuteCMD", ex);
        }
    }
}
