package rcmobile.FPV.widgets.sliding;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import com.andruav.andruavUnit.AndruavUnitBase;

/**
 * Created by M.Hefny on 22-Apr-15.
 */
public class SlidingAndruavUnitList extends ListUnitContainerBase {

    /***
     * in FPV you dont need IMU from GCS
     * in Map you need to locate it
     */
    private boolean mAllowRemoteCGSSelect = true;

    public SlidingAndruavUnitList(Context eventFpvImageFileManager) {
        super(eventFpvImageFileManager);
    }

    public SlidingAndruavUnitList(Context i, AttributeSet paramValue) {
        super(i, paramValue);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public SlidingAndruavUnitList(Context context, AttributeSet attributeSet, int mavStateGotCmdString) {
        super(context, attributeSet, mavStateGotCmdString);
    }

    public void disableVideos (String exceptPartyID)
    {
        int providerClass = this.getChildCount();
        SlidingAndruavUnitItem unitWidget;

        for (int s =0; s < providerClass; s = s +1)
        {
            unitWidget = (SlidingAndruavUnitItem) this.getChildAt(s);

            if (!unitWidget.getUnit().PartyID.equals(exceptPartyID)) {
               final AndruavUnitBase andruavUnitBase = unitWidget.getUnit();
                unitWidget.enableVideoStreaming();
            }
        }

    }


    /***
     * CGS connected to my group can be selected.
     * This is normally true in MAP & False in FPV screen
     * @param enable
     */
    public void allowRemoteCGSSelect(boolean enable)
    {
        mAllowRemoteCGSSelect = enable;
    }

    public void addView(final SlidingAndruavUnitItem slidingItemAndruavUnitWidget) {
        if (slidingItemAndruavUnitWidget == null) return ;
        slidingItemAndruavUnitWidget.allowRemoteCGSSelect(mAllowRemoteCGSSelect);
        super.addView(slidingItemAndruavUnitWidget);
    }

    public void addView(final SlidingAndruavUnitMapPlanningItem slidingItemAndruavUnitMapPlanningWidget) {
        if (slidingItemAndruavUnitMapPlanningWidget == null) return ;
        super.addView(slidingItemAndruavUnitMapPlanningWidget);
    }


    public void clear() {
        super.detachAllViewsFromParent();
    }
}
