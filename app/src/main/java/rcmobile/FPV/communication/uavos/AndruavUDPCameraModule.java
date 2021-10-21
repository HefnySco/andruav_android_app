package rcmobile.FPV.communication.uavos;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.event.droneReport_7adath._7adath_CameraZoom;
import com.andruav.event.droneReport_7adath._7adath_Signalling;
import com.andruav.event.fpv7adath._7adath_FPV_CMD;
import com.andruav.event.fpv7adath._7adath_InitAndroidCamera;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraFlash;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraSwitch;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraZoom;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Signaling;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_RemoteExecute;
import com.andruav.protocol.commands.textMessages.uavosCommands.AndruavModule_ID;
import com.andruav.protocol.communication.uavos.AndruavUDPModuleBase;
import com.andruav.uavos.modules.UAVOSHelper;
import com.andruav.uavos.modules.UAVOSModuleCamera;

import org.json.JSONArray;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import rcmobile.andruavmiddlelibrary.factory.communication.NetInfoAdapter;

import static com.andruav.protocol.commands.textMessages.AndruavMessage_CameraFlash.TYPE_AndruavResala_CameraFlash;
import static com.andruav.protocol.commands.textMessages.AndruavMessage_CameraSwitch.TYPE_AndruavMessage_CameraSwitch;
import static com.andruav.protocol.commands.textMessages.AndruavMessage_CameraZoom.TYPE_AndruavMessage_CameraZoom;
import static com.andruav.protocol.commands.textMessages.AndruavMessage_Signaling.TYPE_AndruavMessage_Signaling;
import static com.andruav.protocol.commands.textMessages.Control.AndruavMessage_Ctrl_Camera.TYPE_AndruavResala_Ctrl_Camera;
import static com.andruav.protocol.commands.textMessages.Control.AndruavMessage_RemoteExecute.TYPE_AndruavMessage_RemoteExecute;
import static com.andruav.uavos.modules.UAVOSConstants.UAVOS_MODULE_TYPE_CAMERA;

public class AndruavUDPCameraModule extends AndruavUDPModuleBase {


    protected String Module_GUID = UUID.randomUUID().toString();

    public AndruavUDPCameraModule() {
        super(defaultUDPPort);
    }

    @Override
    public void broadCast()
    {
        try {
            if (defaultUDPIP == null)
            {
                final String broadcastIP = NetInfoAdapter.getWifiIPBroadcast();
                if (broadcastIP.isEmpty()) return ;
                defaultUDPIP = InetAddress.getByName(broadcastIP);
            }
            sendMessageToServer(defaultUDPIP, defaultUDPServerPort, getModuleID());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    /***
     * Returns Internal Command {@link AndruavModule_ID} that is used to Identify this Module to Comm Module.
     * @return
     */
    @Override
    protected Andruav_2MR getModuleID ()
    {
        final Andruav_2MR andruav_2MR = new Andruav_2MR();
        andruav_2MR.MessageRouting = ProtocolHeaders.CMD_TYPE_INTERMODULE;

        final AndruavModule_ID andruavModule_id = new AndruavModule_ID(true);
        andruavModule_id.ModuleId = AndruavSettings.andruavWe7daBase.PartyID;
        andruavModule_id.ModuleKey = Module_GUID ;
        andruavModule_id.ModuleClass = ProtocolHeaders.UAVOS_CAMERA_MODULE_CLASS;


        try {
            final JSONArray json_MessageCapture = new JSONArray();
            json_MessageCapture.put("C");
            json_MessageCapture.put("V");
            andruavModule_id.ModuleFeatures= json_MessageCapture;

            // Messages Needed By Camera Module.
            final JSONArray json_e = new JSONArray();
            json_e.put(TYPE_AndruavMessage_RemoteExecute);
            json_e.put(TYPE_AndruavResala_Ctrl_Camera);
            json_e.put(TYPE_AndruavMessage_Signaling);
            json_e.put(TYPE_AndruavMessage_CameraZoom);
            json_e.put(TYPE_AndruavResala_CameraFlash);
            json_e.put(TYPE_AndruavMessage_CameraSwitch);
            andruavModule_id.ModuleCapturedMessages  = json_e;

            UAVOSModuleCamera uavosModuleCamera = (UAVOSModuleCamera) AndruavEngine.getUAVOSMapBase().get(UAVOS_MODULE_TYPE_CAMERA + AndruavSettings.andruavWe7daBase.PartyID);

            andruavModule_id.ModuleMessage = uavosModuleCamera.getModuleMessages();
        } catch (Exception e) {
            e.printStackTrace();
        }

        andruav_2MR.andruavMessageBase = andruavModule_id;

        return andruav_2MR;
    }


    @Override
    protected void onMessageReceivedFromServerForInternalProcessing (final Andruav_2MR andruav_2MR)
    {
        switch (andruav_2MR.andruavMessageBase.messageTypeID) {

            case TYPE_AndruavMessage_Signaling:
                try {
                    final AndruavMessage_Signaling andruavMessage_signaling = (AndruavMessage_Signaling) andruav_2MR.andruavMessageBase;
                    final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMasna3().createAndruavUnitClass(andruav_2MR.groupName, andruav_2MR.partyID,true);

                    UAVOSModuleCamera cameraModule = UAVOSHelper.getCameraByID(andruavMessage_signaling.getJsonResala().getString("channel"));
                    if (cameraModule == null) {
                        // camera is not available
                        return;
                    }
                    _7adath_Signalling a7adath_signalling = new _7adath_Signalling(andruavMessage_signaling.getJsonResala(), andruavUnit);
                    AndruavEngine.getEventBus().post(a7adath_signalling);
                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                }
                break;

            case AndruavMessage_RemoteExecute.TYPE_AndruavMessage_RemoteExecute: {
                AndruavMessage_RemoteExecute andruavResala_remoteExecute = ((AndruavMessage_RemoteExecute) (andruav_2MR.andruavMessageBase));
                int CMD_ID = andruavResala_remoteExecute.RemoteCommandID;
                switch (CMD_ID)
                {
                    case AndruavMessage_RemoteExecute.RemoteCommand_STREAMVIDEO:
                    {
                        if (andruavResala_remoteExecute.getBooleanValue("Act")) {
                            EventBus.getDefault().post(new _7adath_InitAndroidCamera());
                        }
                    }
                }
            }
            break;

            case AndruavMessage_CameraZoom.TYPE_AndruavMessage_CameraZoom:
            {
                try {
                    final AndruavMessage_CameraZoom andruavMessage_cameraZoom = (AndruavMessage_CameraZoom) andruav_2MR.andruavMessageBase;
                    UAVOSModuleCamera cameraModule = UAVOSHelper.getCameraByID(andruavMessage_cameraZoom.CameraUniqueName);
                    if (cameraModule == null) {
                        // camera is not available
                        return;
                    }

                    // This is a local camera for this Andruav Device
                    _7adath_CameraZoom adath_cameraZoom = new _7adath_CameraZoom(andruavMessage_cameraZoom);
                    AndruavEngine.getEventBus().post(adath_cameraZoom);

                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                }
            }
            break;

            case AndruavMessage_CameraFlash.TYPE_AndruavResala_CameraFlash:
            {
                try
                {
                    final AndruavMessage_CameraFlash andruavMessage_cameraFlash = (AndruavMessage_CameraFlash) andruav_2MR.andruavMessageBase;

                    UAVOSModuleCamera cameraModule = UAVOSHelper.getCameraByID(andruavMessage_cameraFlash.CameraUniqueName);
                    if (cameraModule == null) {
                        // camera is not available
                        return;
                    }

                    _7adath_FPV_CMD a7adath_fpv_cmd = new _7adath_FPV_CMD(_7adath_FPV_CMD.FPV_CMD_FLASHCAM);
                    a7adath_fpv_cmd.ACT = (andruavMessage_cameraFlash.FlashOn == AndruavMessage_CameraFlash.FLASH_ON);
                    a7adath_fpv_cmd.Requester = andruav_2MR.partyID;

                    AndruavEngine.getEventBus().post(a7adath_fpv_cmd);
                }
                catch (final Exception ex)
                {
                    ex.printStackTrace();
                }
            }

            break;

            case TYPE_AndruavMessage_CameraSwitch:
            {
                try
                {
                    final AndruavMessage_CameraSwitch andruavMessage_cameraSwitch = (AndruavMessage_CameraSwitch) andruav_2MR.andruavMessageBase;
                    _7adath_FPV_CMD a7adath_fpv_cmd = new _7adath_FPV_CMD(_7adath_FPV_CMD.FPV_CMD_SWITCHCAM);

                    UAVOSModuleCamera cameraModule = UAVOSHelper.getCameraByID(andruavMessage_cameraSwitch.CameraUniqueName);
                    if (cameraModule == null) {
                        // camera is not available
                        return;
                    }

                    a7adath_fpv_cmd.Variables.put("SendBackTo", andruavMessage_cameraSwitch.CameraUniqueName);
                    a7adath_fpv_cmd.Requester = andruav_2MR.partyID;

                    AndruavEngine.getEventBus().post(a7adath_fpv_cmd);
                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                }
            }

            break;

            default:
                break;
        }
    }

}
