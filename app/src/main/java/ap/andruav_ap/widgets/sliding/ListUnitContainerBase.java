package ap.andruav_ap.widgets.sliding;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created by mhefny on 3/24/16.
 * This is a Generic Sliding Menu... Can be used in many screens with different cwidget units inside.
 */
   public class ListUnitContainerBase extends LinearLayout {


    public ListUnitContainerBase(Context context) {
        super(context);
    }

    public ListUnitContainerBase(Context i, AttributeSet paramValue) {
        super(i, paramValue);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public ListUnitContainerBase(Context abcActionBarTabView, AttributeSet attributeSet, int mavStateGotCmdString) {
        super(abcActionBarTabView, attributeSet, mavStateGotCmdString);
    }



    public boolean containsKey(String msg)
    {
        final int itemsCount = this.getChildCount();
        ListUnitItemBase values;

        for (int i =0; i < itemsCount; i = i +1)
        {
            values = (ListUnitItemBase) this.getChildAt(i);
            if (values.getUnit().PartyID.equals(msg))
                return true;
        }

        return false;
    }

    public ListUnitItemBase getByKey(String partyID)
    {
        final int itemsCount = this.getChildCount();
        ListUnitItemBase unitWidget;

        for (int s =0; s < itemsCount; s = s +1)
        {
            unitWidget = (ListUnitItemBase) this.getChildAt(s);
            if (unitWidget.getUnit().PartyID.equals(partyID))
                return unitWidget;
        }

        return null;
    }

}
