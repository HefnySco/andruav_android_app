package ap.andruav_ap.communication;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.interfaces.IControlBoardFactory;

import ap.andruav_ap.communication.controlBoard.ControlBoard_DeBus;

/**
 * Created by M.Hefny on 04-Apr-15.
 * <br>
 * C6 (AN-0054): the native stack is the only stack - the FC belongs to
 * de_mavlink (single-owner rule) and the board is always the bus-backed
 * {@link ControlBoard_DeBus}, which sends the same commands a GCS sends,
 * addressed to the unit itself. The DroneKit board lives unreferenced in
 * {@code src/legacy/java}.
 */
public class ControlBoardFactory implements IControlBoardFactory {

    public void getFlightControlBoard(final AndruavUnitBase unit)
    {
        if (unit.IsMe()) {
            unit.FCBoard = new ControlBoard_DeBus(unit);
            unit.useFCBIMU(true);
        } else {
            unit.FCBoard = null;
        }
    }

}
