package rcmobile.FPV.activities.fcb.drone;

import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.TelemetryProtocol;



/**
 * Created by M.Hefny on 12-Feb-15.
 */
public class ListItem_DroneTelemetry {

    private  boolean isSelected =false;
    private AndruavUnitShadow andruavWe7da;


    public AndruavUnitShadow getAndruavWe7da ()
    {
        return andruavWe7da;
    }

    public void setDroneUnitID (final AndruavUnitShadow we7da)
    {
        andruavWe7da = we7da;

    }

    public String getDroneUnitID ()
    {
        return andruavWe7da.UnitID;
    }

    public boolean isTelemetrySupported()
    {
        return !((andruavWe7da.getTelemetry_protocol() == TelemetryProtocol.TelemetryProtocol_No_Telemetry));
    }


    public void setIsSelected (boolean cmdIdMountControl)
    {
        isSelected = cmdIdMountControl;
    }

    public boolean getisSelected ()
    {
        return isSelected;
    }
}
