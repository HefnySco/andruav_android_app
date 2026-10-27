package ap.andruav_ap.communication;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.TelemetryProtocol;
import com.andruav.interfaces.IControlBoardFactory;

import ap.andruav_ap.communication.controlBoard.ControlBoard_DeBus;
import ap.andruav_ap.communication.controlBoard.ControlBoard_DroneKit;
import ap.andruavmiddlelibrary.preference.Preference;

/**
 * Created by M.Hefny on 04-Apr-15.
 */
public class ControlBoardFactory implements IControlBoardFactory {

    public void getFlightControlBoard(final AndruavUnitBase unit)
    {
        // F3 (AN-0070): in native mode the FC belongs to de_mavlink
        // (single-owner rule) - never build the DroneKit board here. The
        // bus-backed board sends the same commands a GCS sends, addressed
        // to the unit itself.
        if (Preference.isNativeUnitEnabled(null)) {
            if (unit.IsMe()) {
                unit.FCBoard = new ControlBoard_DeBus(unit);
                unit.useFCBIMU(true);
            }
            return;
        }

        if (unit.getTelemetry_protocol() == TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry) {
            unit.FCBoard = new ControlBoard_DroneKit(unit);
            unit.useFCBIMU(true);
        } else {
            unit.FCBoard = null;
        }
    }

}
