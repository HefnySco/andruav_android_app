package rcmobile.FPV.widgets.sliding;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.event.droneReport_7adath._7adath_GCSBlockedChanged;

import de.greenrobot.event.EventBus;


/**
 * Represent a core unit that should be inherited
 * Created by mhefny on 3/24/16.
 */
public class ListUnitItemBase extends RelativeLayout {

    protected LayoutInflater mInflater;

    protected AndruavUnitShadow mAndruavUnit;

    protected Handler mhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            OnHandleMessage(msg);
        }
    };


    protected void OnHandleMessage (final Message msg)
    {

    }



    public void onEvent(final _7adath_GCSBlockedChanged a7adath_gcsBlockedChanged) {

        if ((mAndruavUnit == null) || (!mAndruavUnit.equals(a7adath_gcsBlockedChanged.andruavUnitBase))) return;

        Message msg = mhandler.obtainMessage();
        msg.obj = a7adath_gcsBlockedChanged;
        mhandler.sendMessage(msg);
    }

    public AndruavUnitShadow getUnit()
    {
        return mAndruavUnit;
    }

    public void setUnit(AndruavUnitShadow andruavUnit) {
    }

    public ListUnitItemBase(Context context) {
        super(context);
    }

    public ListUnitItemBase(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListUnitItemBase(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ListUnitItemBase(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
    }
}
