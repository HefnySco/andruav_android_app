package ap.andruav_ap.widgets.flightControlWidgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.sensors.AndruavIMU;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.R;

import com.andruav.event.droneReport_Event.Event_GPS_Ready;
import com.andruav.event.droneReport_Event.Event_IMU_Ready;

/**
 * Created by mhefny on 2/4/16.
 */

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class AttitudeWidget extends RelativeLayout {



    //////// Attributes
    AttitudeWidget Me;

    private LayoutInflater mInflater;
    private AndruavUnitBase mAndruavUnit;


    private Handler mhandle;

    private AttitudeIndicatorView attitudeIndicatorView;


    //////////BUS EVENT


    /***
     * Local IMU events
     * @param a7adath_imu_ready
     */
    public void onEvent (final Event_IMU_Ready a7adath_imu_ready)
    {
        // This is local IMU so return if local Unit is not selected
        if ((mAndruavUnit==null) || (!mAndruavUnit.Equals(a7adath_imu_ready.mAndruavWe7da)))
        {
            return ;
        }

        final Message msg = mhandle.obtainMessage();
        msg.obj = a7adath_imu_ready;
        if (mhandle != null)  mhandle.sendMessageDelayed(msg,0);
    }



    public void onEvent (final Event_GPS_Ready a7adath_gps_ready)
    {
        // This is local IMU so return if local Unit is not selected
        if ((mAndruavUnit==null) || (!mAndruavUnit.Equals(a7adath_gps_ready.mAndruavWe7da)))
        {
            return ;
        }

        final Message msg = mhandle.obtainMessage();
        msg.obj = a7adath_gps_ready;
        if (mhandle != null)  mhandle.sendMessageDelayed(msg,0);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void setAndruavUnit (final AndruavUnitBase andruavUnit)
    {
        mAndruavUnit = andruavUnit;

        if (andruavUnit==null)
        {
            mAndruavUnit = null;

        }else {

            if (andruavUnit.getIsCGS() && andruavUnit.IsMe()) {
                mAndruavUnit = null;
            }
        }

        updateAttitude();
    }


    private void UIHandler () {
        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);


                if (msg.obj instanceof Event_IMU_Ready) {
                    updateAttitude();
                    return;
                }


            }
        };
    }

    protected void   initGUI(Context context)
    {
        if (this.isInEditMode()) return ;

        Me = this;
        mInflater = LayoutInflater.from(context);
        mInflater.inflate(R.layout.widget_attitude_indicator, this, true);

        attitudeIndicatorView = findViewById(R.id.widget_attitude_indicator);

    }

    public AttitudeWidget(Context context) {
        super(context);

        initGUI(context);
    }

    public AttitudeWidget(Context context, AttributeSet attrs) {
        super(context, attrs);

        initGUI(context);
    }

    public AttitudeWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initGUI(context);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AttitudeWidget(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initGUI(context);
    }



    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        UIHandler();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mhandle.removeCallbacksAndMessages(null);
        mhandle = null;
        EventBus.getDefault().unregister(this);
    }

    protected  void updateAttitude ()
    {
        if (mAndruavUnit == null)
        {
            return;
        }

        final AndruavIMU andruavIMU = mAndruavUnit.getActiveIMU();


        attitudeIndicatorView.setAttitude((float) andruavIMU.R, (float) andruavIMU.P, (float) andruavIMU.Y, mAndruavUnit.useFCBIMU(), false);

    }

}
