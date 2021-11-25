package ap.andruav_ap.communication;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.TelemetryProtocol;
import com.andruav.interfaces.IControlBoardFactory;

import ap.andruav_ap.communication.controlBoard.ControlBoard_DroneKit;

/**
 * Created by M.Hefny on 04-Apr-15.
 */
public class ControlBoardFactory implements IControlBoardFactory {

    public void getFlightControlBoard(final AndruavUnitBase unit)
    {

        switch (unit.getTelemetry_protocol())
        {
            case TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry:
                unit.FCBoard = new ControlBoard_DroneKit(unit);
                unit.useFCBIMU(true);
                break;
            default:
                unit.FCBoard = null;
                break;
        }
    }

}
