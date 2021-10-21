package rcmobile.FPV.communication;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.interfaces.IAndruavWe7daMasna3;

/**
 * Created by M.Hefny on 14-Feb-15.
 */
public class AndruavUnitFactory implements IAndruavWe7daMasna3 {
    @Override
    public AndruavUnitBase createAndruavUnitClass(final String groupName, final String senderName, final boolean isGCS) {
        return new AndruavUnitShadow(groupName, senderName,isGCS);
    }
}
