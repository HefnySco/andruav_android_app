package rcmobile.FPV.communication;


import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.andruavUnit.AndruavUnitMapBase;
import com.andruav.controlBoard.ControlBoardBase;
import com.andruav.controlBoard.shared.missions.MohemmaMapBase;
import com.andruav.protocol.commands.binaryMessages.AndruavBinary_2MR;
import com.andruav.protocol.commands.binaryMessages.AndruavResalaBinary_WayPointsUpdates;

/**
 * Created by M.Hefny on 30-Oct-14.
 */
public class AndruavUnitMap extends AndruavUnitMapBase {






    public AndruavUnitShadow updatehWayPoint(AndruavBinary_2MR andruavCMD)
    {
        final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) get(andruavCMD.partyID);
        if (andruavUnit == null) {
            // Object is not in the defined markers...
            // either because :
            // 1- No Message ID has been sent yet.
            // 2- future reason: permissions and security related issues.
            // We can add it as update list to increase response time but I prefer to leave it as is
            // for future security and permissions updates.
            // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
            return null;
        }


        AndruavResalaBinary_WayPointsUpdates wayPoints = (AndruavResalaBinary_WayPointsUpdates) andruavCMD.andruavResalaBinaryBase;
        andruavUnit.getMohemmaMapBase().clear();
        MohemmaMapBase mohemmaMapBase = wayPoints.getWayPoints();
        if (mohemmaMapBase == null) return andruavUnit;
        final int size = mohemmaMapBase.size();
        //BUG: concurrency issue can happen here betweem MapActivity and this function.
        for (int i=0; i<size;++i)
        {
            andruavUnit.getMohemmaMapBase().remove(mohemmaMapBase.keyAt(i));
            andruavUnit.getMohemmaMapBase().Put(mohemmaMapBase.keyAt(i), mohemmaMapBase.valueAt(i));
        }


        return andruavUnit;
    }







    public ControlBoardBase updateTelemetry(String partyID, int telemetryProtocol) {

        AndruavUnitShadow andruavUnit = (AndruavUnitShadow) get(partyID);
        if (andruavUnit == null) {
            // Object is not in the defined markers...
            // either because :
            // 1- ScheduledTasks has not run yet
            // 2- No Message ID has been sent yet.
            // 3- future reason: permissions and security related issues.
            // We can add it as update list to increase response time but I prefer to leave it as is
            // for future security and permissions updates.
            // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
            return null;
        }

        andruavUnit.setTelemetry_protocol(telemetryProtocol);

        return andruavUnit.FCBoard;
    }



}
