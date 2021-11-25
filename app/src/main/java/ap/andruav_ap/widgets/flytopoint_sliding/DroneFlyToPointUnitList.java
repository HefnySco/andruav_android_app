package ap.andruav_ap.widgets.flytopoint_sliding;

import android.content.Context;
import android.util.AttributeSet;

import ap.andruav_ap.widgets.sliding.ListUnitContainerBase;

/**
 * Created by mhefny on 12/27/16.
 */

public class DroneFlyToPointUnitList extends ListUnitContainerBase {
    public DroneFlyToPointUnitList(Context context) {
        super(context);
    }

    public DroneFlyToPointUnitList(Context i, AttributeSet paramValue) {
        super(i, paramValue);
    }

    public DroneFlyToPointUnitList(Context abcActionBarTabView, AttributeSet attributeSet, int mavStateGotCmdString) {
        super(abcActionBarTabView, attributeSet, mavStateGotCmdString);
    }

    public void addView(final DroneFlyToPointUnitItem droneFlyToPointUnitItem) {
        if (droneFlyToPointUnitItem == null) return ;
        super.addView(droneFlyToPointUnitItem);
    }


    public void clear() {
        super.detachAllViewsFromParent();
    }
}
