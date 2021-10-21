package rcmobile.FPV.communication.uavos;

import com.andruav.AndruavEngine;
import com.andruav.interfaces.INotification;
import com.andruav.notification.PanicFacade;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;
import com.andruav.protocol.commands.textMessages.uavosCommands.AndruavModule_ID;
import com.andruav.protocol.communication.uavos.AndruavUDPServerBase;
import com.andruav.uavos.modules.UAVOSModuleUnit;

import java.net.InetAddress;

import rcmobile.FPV.R;

import static com.andruav.uavos.modules.UAVOSConstants.UAVOS_MODULE_TYPE_CAMERA;
import static com.andruav.uavos.modules.UAVOSConstants.UAVOS_MODULE_TYPE_DNN_TRK;


/**
 * Created by M.Hefny on 10-Aug-15.
 */
public class AndruavUDPServer extends AndruavUDPServerBase {

    public AndruavUDPServer(InetAddress address) {
        super(address);
    }


    @Override
    protected void onModuleAdded (final Andruav_2MR andruav_2MR, final UAVOSModuleUnit uavosModuleUnit)
    {
        final AndruavModule_ID andruavModule_id = (AndruavModule_ID) andruav_2MR.andruavMessageBase;

        switch (uavosModuleUnit.ModuleClass)
        {
            case UAVOS_MODULE_TYPE_CAMERA:
                PanicFacade.andruavModuleAdded(INotification.NOTIFICATION_TYPE_NORMAL, AndruavMessage_Error.ERROR_MODULES, AndruavEngine.AppContext.getString(R.string.andruav_module_added),null);
                break;

            case UAVOS_MODULE_TYPE_DNN_TRK:
                PanicFacade.andruavModuleAdded(INotification.NOTIFICATION_TYPE_NORMAL, AndruavMessage_Error.ERROR_MODULES, AndruavEngine.AppContext.getString(R.string.andruav_module_added),null);
                break;
        }

    }

    @Override
    public void broadCast() throws IllegalAccessException {
        throw new IllegalAccessException("NOT USED ON COMM SERVER");
    }

    @Override
    protected void onModuleUpdated (final Andruav_2MR andruav_2MR, final UAVOSModuleUnit uavosModuleUnit)
    {
        if (uavosModuleUnit.ModuleClass.equals(UAVOS_MODULE_TYPE_CAMERA))
        {
            //AndruavDroneFacade.sendCameraList(false, null);
        }
    }
}
