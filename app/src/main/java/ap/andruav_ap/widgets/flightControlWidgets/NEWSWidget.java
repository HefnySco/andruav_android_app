package ap.andruav_ap.widgets.flightControlWidgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;

import de.greenrobot.event.EventBus;

import ap.andruavmiddlelibrary.factory.math.Angles;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.droneReport_Event.Event_GPS_Ready;
import com.andruav.event.droneReport_Event.Event_IMU_Ready;

/**
 * Created by M.Hefny on 20-Sep-14.
 */
@Deprecated
public class NEWSWidget extends View {

    NEWSWidget Me;
    private AndruavUnitBase mAndruavUnit;
    private Handler mhandle;

    //private ScopeThread renderer;
    private int width;
    private int height;
    double  yaw = 0;

    Paint sky = new Paint();
    Paint white = new Paint();
    Paint whiteCenter = new Paint();
    Paint NEWS = new Paint();

    Paint plane = new Paint();




    public void onEvent (final Event_GPS_Ready event_GPS)
    {
        // This is local IMU so return if local Unit is not selected
        if ((mAndruavUnit==null) || (!mAndruavUnit.Equals(event_GPS.mAndruavWe7da)))
        {
            return ;
        }

        final Message msg = mhandle.obtainMessage();
        msg.obj = event_GPS;
        if (mhandle != null)  mhandle.sendMessageDelayed(msg,0);
    }

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




    public void setAndruavUnit (final AndruavUnitBase andruavUnit)
    {
        mAndruavUnit = andruavUnit;

        if (andruavUnit==null)
        {
            mAndruavUnit = null;

        }
        else {

            if (andruavUnit.getIsCGS() && andruavUnit.IsMe()) {
                mAndruavUnit = null;
            }
        }

        newFlightData();

    }

    private void UIHandler () {
        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);


                newFlightData();

            }
        };
    }


    public NEWSWidget(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        Me = this;

        sky.setARGB(220, 0, 113, 188);

        white.setColor(Color.WHITE);
        white.setTextSize(15.0f * context.getResources().getDisplayMetrics().density);

        whiteCenter.setColor(Color.WHITE);
        whiteCenter.setTextSize(15.0f * context.getResources().getDisplayMetrics().density);
        whiteCenter.setTextAlign(Paint.Align.CENTER);

        //NEWS.setColor(Color.WHITE);
        NEWS.setARGB(220, 255, 137, 142);

        NEWS.setTextSize(15.0f * context.getResources().getDisplayMetrics().density);
        NEWS.setTextAlign(Paint.Align.CENTER);
        NEWS.setUnderlineText(true);



    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        width= MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

    }

    int monGUICounter = 1;
    @Override
    protected void onDraw(Canvas canvas) {

        try {

            canvas.drawCircle(0, 0, 20, sky);
            //  canvas.drawRect(-width, -height/2, width,  -height/2 + 30, sky);
            //  canvas.drawLine(-width, -height/2+30, width, -height/2+30, white);

            canvas.drawRect(0, 0, width, height / 2 + 10, sky);
            canvas.drawLine(0, height / 2 + 10, width, height / 2 + 10, white);


            // width / 2 == yawPosition
            // then round to nearest 5 degrees, and draw it.

            double centerDegrees = yaw;
            double numDegreesToShow = 50;
            double degreesPerPixel = (double) width / numDegreesToShow;
            String[] compass = {"N", "E", "S", "W"};

            double mod = yaw % 5;
            for (double angle = (centerDegrees - mod) - numDegreesToShow / 2.0;
                 angle <= (centerDegrees - mod) + numDegreesToShow / 2.0;
                 angle += 5) {

                // protect from wraparound
                double workAngle = (angle + 360.0);
                if (workAngle >= 360)
                    while (workAngle >= 360) workAngle -= 360;
                else
                    while (workAngle < -360) workAngle += 360;

                //while( workAngle >= 360)
                //    workAngle -= 360.0;

                // need to draw "angle"
                // How many pixels from center should it be?
                int distanceToCenter = (int) ((angle - centerDegrees) * degreesPerPixel) + width / 2;

                canvas.drawLine(distanceToCenter, height / 2, distanceToCenter, height, white);

                if (workAngle % 90 == 0) {
                    int index = (int) (workAngle / 90);
                    canvas.drawText(compass[index], distanceToCenter, height / 2, NEWS);

                } else
                    canvas.drawText((int) (workAngle) + "", distanceToCenter, height / 2, whiteCenter);


            }

            // Draw the center line
            canvas.drawLine(0, 0, 0, 40, plane);
        }
        catch (Exception e)
        {
            if (monGUICounter >  0) return ;
            monGUICounter = monGUICounter - 1;
            AndruavEngine.log().log(AndruavSettings.Account_SID, "NewsWidget", String.valueOf(yaw));
        }
    }

    /**
     * Receive current copter orientation
     *
     */
    public void newFlightData() {
        if (mAndruavUnit == null) return;

        this.yaw = ( mAndruavUnit.getActiveIMU().Y * Angles.RADIANS_TO_DEGREES);
        this.invalidate();
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
}
