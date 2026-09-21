package org.droidplanner.services.android.impl.core.drone.autopilot.apm;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import com.MAVLink.Messages.MAVLinkMessage;
import com.github.zafarkhaja.semver.Version;
import com.o3dr.services.android.lib.drone.action.ControlActions;
import com.o3dr.services.android.lib.drone.attribute.error.CommandExecutionError;
import com.o3dr.services.android.lib.drone.property.Parameter;
import com.o3dr.services.android.lib.model.ICommandListener;

import org.droidplanner.services.android.impl.communication.model.DataLink;
import org.droidplanner.services.android.impl.core.MAVLink.MavLinkCommands;
import org.droidplanner.services.android.impl.core.drone.DroneInterfaces;
import org.droidplanner.services.android.impl.core.drone.LogMessageListener;
import org.droidplanner.services.android.impl.core.drone.profiles.ParameterManager;
import org.droidplanner.services.android.impl.core.drone.variables.ApmModes;
import org.droidplanner.services.android.impl.core.drone.variables.State;
import org.droidplanner.services.android.impl.core.firmware.FirmwareType;
import org.droidplanner.services.android.impl.core.model.AutopilotWarningParser;
import org.droidplanner.services.android.impl.utils.CommonApiUtils;

/**
 * Created by Fredia Huya-Kouadio on 7/27/15.
 */
public class ArduCopter extends ArduPilot {
    private static final Version ARDU_COPTER_V3_3 = Version.forIntegers(3,3,0);
    private static final Version ARDU_COPTER_V3_4 = Version.forIntegers(3,4,0);


    private ICommandListener manualControlState = null;

    public ArduCopter(String droneId, Context context, DataLink.DataLinkProvider<MAVLinkMessage> mavClient, Handler handler, AutopilotWarningParser warningParser, LogMessageListener logListener) {
        super(droneId, context, mavClient, handler, warningParser, logListener);
    }

    @Override
    public FirmwareType getFirmwareType() {
        return FirmwareType.ARDU_COPTER;
    }

    @Override
    public void destroy(){
        super.destroy();
        manualControlState = null;
    }

    @Override
    protected boolean enableManualControl(Bundle data, ICommandListener listener){
        boolean enable = data.getBoolean(ControlActions.EXTRA_DO_ENABLE);

        State state = getState();
        ApmModes vehicleMode = state.getMode();
        if(enable){
            if(vehicleMode == ApmModes.ROTOR_GUIDED){
                CommonApiUtils.postSuccessEvent(listener);
            }
            else{
                state.changeFlightMode(ApmModes.ROTOR_GUIDED, listener);
            }

            if(listener != null) {
                manualControlState = listener;
            }
        }
        else{
            manualControlState = null;

            if(vehicleMode != ApmModes.ROTOR_GUIDED){
                CommonApiUtils.postSuccessEvent(listener);
            }
            else{
                state.changeFlightMode(ApmModes.ROTOR_LOITER, listener);
            }
        }

        return true;
    }

    @Override
    public void notifyDroneEvent(DroneInterfaces.DroneEventsType event){
        if (event == DroneInterfaces.DroneEventsType.MODE) {//Listen for vehicle mode updates, and update the manual control state listeners appropriately
            ApmModes currentMode = getState().getMode();
            if (manualControlState != null) {
                if (currentMode == ApmModes.ROTOR_GUIDED) {
                    CommonApiUtils.postSuccessEvent(manualControlState);
                } else {
                    CommonApiUtils.postErrorEvent(CommandExecutionError.COMMAND_FAILED, manualControlState);
                }
            }
        }

        super.notifyDroneEvent(event);
    }

}
