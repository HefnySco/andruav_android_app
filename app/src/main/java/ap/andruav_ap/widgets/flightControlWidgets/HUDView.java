package ap.andruav_ap.widgets.flightControlWidgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.Constants;
import com.andruav.FeatureSwitch;
import com.andruav.sensors.AndruavIMU;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.App;
import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.preference.Preference;
import com.andruav.event.droneReport_Event.Event_IMU_Ready;


/**
 * Created by M.Hefny on 17-Sep-14.
 */

@Deprecated
public class HUDView extends View {


    private HUDView Me;
    private AndruavUnitBase mAndruavUnit;
    private Handler mhandle;

    private AndruavIMU mevent_IMU;
    //private ScopeThread renderer;
    public int HorizonBackGroundColor = Color.rgb(20, 20, 20);
    private int width;
    private int height;
    private Rect srcRect;

    int rollRadius;// = (int)((double)width * 0.35); //250;

    double roll = 0, pitch = 0, yaw = 0;

    final Paint grid_paint = new Paint();
    final Paint ground = new Paint();
    final Paint sky = new Paint();
    final Paint white = new Paint();
    final Paint whiteCenter = new Paint();
    final Paint whitebar = new Paint();
    final Paint whiteStroke = new Paint();
    final Paint statusText = new Paint();
    final Paint errorText = new Paint();


    final Paint plane = new Paint();
    final Paint redSolid = new Paint();
    private final String altitude = "";
    //private double remainBatt = 0;
    //private double battVolt = 0;
    //private String gpsFix = "";
    private boolean mDisplayVideo = false;
    private Bitmap imageDisplay;

    private final long debuglastOnDraw =0;
    private final long debugOnDraw =0;
    /***
     * Set True by external control to indicate that Video is being captured.
     */
    public final boolean isRecording=false;
    private long mlastFPS=0;
    private long FPS=0;
    private static final long VIDEO_TIMEOUT =5000;
    private boolean useFCBIMU = false;
    private int measureUnit = Constants.Preferred_UNIT_METRIC_SYSTEM;


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

        }else {

            if (andruavUnit.getIsCGS() && andruavUnit.IsMe()) {
                mAndruavUnit = null;
            }
        }
        newFlightData();

    }




    public synchronized void setDisplayVideo(final boolean enable)
    {
        mDisplayVideo =enable;
    }

    public synchronized boolean getDisplayVideo(final boolean enable)
    {
        return mDisplayVideo;
    }

    public synchronized void setImageDisplay (final Bitmap image)
    {
        if ((imageDisplay!= null) && (!imageDisplay.isRecycled()))
        {
            imageDisplay.recycle();
        }
        imageDisplay = image;
        if (imageDisplay != null)
        {

            final long now = System.currentTimeMillis();
            try {

                FPS = 1000 / (now - mlastFPS);

            }
            catch (ArithmeticException ex)
            {
              FPS=0;
            }

            mlastFPS = now;

            srcRect = new Rect (0,0,imageDisplay.getWidth(),imageDisplay.getHeight());
        }


    }


    public HUDView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
       // getHolder().addCallback(this);

        if (isInEditMode()) return ;

        grid_paint.setColor(Color.rgb(100, 100, 100));
        //ground.setARGB(220, 148, 193, 31);
        ground.setColor(App.context.getResources().getColor(R.color.btn_TXT_GREEN_HUD_LAND));
       // sky.setARGB(220, 0, 113, 188);

        sky.setColor(App.context.getResources().getColor(R.color.btn_TXT_GREEN_HUD_SKY));
        whitebar.setARGB(64, 255, 255, 255);

        white.setColor(Color.WHITE);
        //white.setColor(Color.YELLOW);
        white.setTextSize(15.0f * context.getResources().getDisplayMetrics().density);

        //whiteCenter.setColor(Color.YELLOW);
        whiteCenter.setColor(Color.WHITE);
        whiteCenter.setTextSize(15.0f * context.getResources().getDisplayMetrics().density);
        whiteCenter.setTextAlign(Align.CENTER);

        //statusText.setColor(Color.YELLOW);
        statusText.setColor(Color.WHITE);
        statusText.setTextSize(15.0f * context.getResources().getDisplayMetrics().density);

        errorText.setColor(Color.RED);
        errorText.setTextSize(20.0f * context.getResources().getDisplayMetrics().density);


        whiteStroke.setColor(Color.WHITE);
        //whiteStroke.setColor(Color.YELLOW);
        whiteStroke.setStyle(Style.STROKE);
        whiteStroke.setStrokeWidth(3);

        plane.setColor(Color.RED);
        plane.setStyle(Style.STROKE);
        plane.setStrokeWidth(3);

        redSolid.setColor(Color.RED);

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        width= MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

        rollRadius = (int)((double)width * 0.35); //250;

        Rect bounds = new Rect();
        statusText.getTextBounds("Dy", 0, 2, bounds);  // high and low boundry in English
        heightUnit = bounds.height() * 1.2f;
        heightBottomMargin = (float)bounds.height() * 0.3f;
        widthMargin = (float)bounds.height() * 0.5f;

    }

    @Override
    protected void onDraw(Canvas canvas) {


        //final long now = System.currentTimeMillis();
        //debugOnDraw = 1000/ (now - debuglastOnDraw);
        //debuglastOnDraw = now;
        if (FeatureSwitch.DEBUG_MODE) {
            Log.d("A_FPV", String.format("debugOnDraw: %d", debugOnDraw));
        }


        canvas.drawColor(Color.TRANSPARENT);
            if (isRecording)
            {
                drawText(canvas, 3, App.getAppContext().getString(R.string.action_video_shoot),errorText,  false);
            }


        // (0,0) is screen center
        canvas.translate(width / 2, height / 2);

        canvas.save();
        drawPitch(canvas);
        canvas.restore();
        canvas.save();
        drawRoll(canvas);
        canvas.restore();
        canvas.save();
        drawText(canvas);
        canvas.restore();
        canvas.save();
        drawPlane(canvas);
        canvas.restore();


    }

    private void drawPlane(Canvas canvas) {
        canvas.drawCircle(0, 0,  10,plane);

        canvas.drawLine(-10,   0, -25,   0, plane);
        canvas.drawLine(10, 0, 25, 0, plane);
        canvas.drawLine(  0, -10,   0, -25, plane);

    }


    static float heightUnit = 0.0f;
    static float heightBottomMargin = 0.0f;
    static float widthMargin = 0.0f;

    /***
     *
     * Draw Text on Canvas
     * @param canvas
     * @param i line order 0 is bottom and more positive is higher...i.e. less y
     * @param text
     * @param p
     * @param left
     */
    private void drawText(Canvas canvas, int i, String text, Paint p, boolean left){
        Rect bounds = new Rect();
        p.getTextBounds(text, 0, text.length(), bounds);

        //float y = (float) (height/2.0 - i * bounds.height()*1.2) - (float)bounds.height()*0.3f;
        float y = (float) (height/2.0 - i * heightUnit) - heightBottomMargin;

        if(left)
            canvas.drawText(text, (float)(-width/2.0 + widthMargin), y, p);
        else
            canvas.drawText(text, (float) (width / 2.0 - bounds.width() - widthMargin), y, p);

    }


    private void drawText(Canvas canvas) {



       // drawText(canvas, 2, "GPS: " + gpsFix, statusText, true);
       // drawText(canvas, 1, "GPS-Alt: " + maxAltitude, statusText, true);
        //drawText(canvas, 0, "Nav: " + navMode,  statusText, true);

       // drawText(canvas, 1, remainBatt +"%", statusText, false);

      /*  String speedText="speed: ";
        String avgSpeed = "avg speed: ";

        if (measureUnit == Constants.Preferred_UNIT_METRIC_SYSTEM) {
            speedText = speedText.concat(String.format("%3.1f m/s", gpsSpeed));
            if (mevent_IMU != null) {
                avgSpeed = avgSpeed + String.format("%3.1f m/s", mevent_IMU.GroundSpeed_avg );
            }

        } else {
            speedText = speedText.concat(String.format("%3.1f mph", gpsSpeed * UnitConversion.Speed_MetersPerSecondToMilePerHour));
            if (mevent_IMU != null) {
                avgSpeed = avgSpeed + String.format("%3.1f mph", mevent_IMU.GroundSpeed_avg * UnitConversion.Speed_MetersPerSecondToMilePerHour);
            }
        }


        drawText(canvas, 3, avgSpeed, statusText, false);
        drawText(canvas, 2, speedText, statusText, false);
*/

        if (mDisplayVideo) {
            drawText(canvas, 5, String.format("FPS:%02d ",FPS), statusText, false);
        }
        //drawText(canvas, 1, "bat: " +  String.format("%3.0f",remainBatt)  +"% " + String.format("%2.2f",battVolt) + "v", statusText, false);


    }



    private void drawRoll(Canvas canvas) {

        final RectF rec = new RectF(-rollRadius, -height/2 + 60 ,
               rollRadius, -height/2 + 60 + 2*rollRadius);

        //Draw the arc
        canvas.drawArc(rec, -180+45, 90, false, whiteStroke);

        //draw the ticks
        //The center of the circle is at:
        // 0, -height/2 + 60 + r

        float centerY = -height/2 + 60+ rollRadius;
        for(int i = -45; i<= 45; i+= 15){
            // Draw ticks
            float dx = (float)Math.sin(i * Math.PI / 180) * rollRadius;
            float dy = (float)Math.cos(i * Math.PI / 180) * rollRadius;
            canvas.drawLine( dx, centerY - dy,
                    (dx + (dx/25)), centerY - (dy + dy/25),
                    whiteStroke);

            //Draw the labels
            if( i != 0){
                dx = (float)Math.sin(i * Math.PI / 180) * (rollRadius-30);
                dy = (float)Math.cos(i * Math.PI / 180) * (rollRadius-30);
                canvas.drawText(Math.abs(i)+"",
                        dx, centerY - dy,
                        whiteCenter);

            }
        }

        final float dx = (float)Math.sin(-roll * Math.PI / 180) * rollRadius;
        final float dy = (float)Math.cos(-roll * Math.PI / 180) * rollRadius;
        canvas.drawCircle(dx, centerY-dy, 10, redSolid);

    }

    private void drawPitch(Canvas canvas) {

        final int step = 40; //Pixels per 5 degree step

        canvas.translate(0, (int)(-pitch * (step / 5)));
        canvas.rotate(-(int)roll);
        final int w5 = 10 * width;
        final int h5 = 10 * height;


        canvas.drawRect(-width, -20,
                width,  20, whitebar);

        // Draw the vertical grid
        canvas.drawLine(-width, 0, width, 0, white);
        //canvas.f


        for(int i = -step*20; i < step*20; i+=step){
            if(i !=0 ){
                if( i%(2*step) == 0){
                    canvas.drawLine(-50, i, 50, i, white);
                    canvas.drawText(( 5 * i / -step ) +"", -90, i+5, white);

                }else
                    canvas.drawLine(-20, i, 20, i, white);
            }
        }
    }


    protected void newFlightData ()
    {
        if (mAndruavUnit == null) return;

        final AndruavIMU andruavIMU = mAndruavUnit.getActiveIMU();


        newFlightData(andruavIMU.R - andruavIMU.RT, -andruavIMU.P + andruavIMU.PT, andruavIMU.Y, mAndruavUnit.useFCBIMU());

    }


    /**
     * Receive current copter orientation
     * @param roll
     * @param pitch
     * @param yaw
     */
    protected void newFlightData(final double roll, final double pitch, final double yaw, final boolean useFCBIMU) {

        this.roll = roll * 180.0 / Math.PI;
        this.pitch = pitch * 180.0 / Math.PI;
        this.yaw = ( yaw * 180.0 / Math.PI);
        if (this.useFCBIMU != useFCBIMU) {
            this.useFCBIMU = useFCBIMU;
            if (useFCBIMU) {
                white.setColor(Color.YELLOW);
                whiteStroke.setColor(Color.YELLOW);
                whiteCenter.setColor(Color.YELLOW);

            }
            else
            {
                white.setColor(Color.WHITE);
                whiteStroke.setColor(Color.WHITE);
                whiteCenter.setColor(Color.WHITE);
            }

        }
        this.invalidate();
    }



    private void UIHandler () {
        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);


                if (msg.obj instanceof Event_IMU_Ready) {
                    newFlightData();
                    return;
                }


            }
        };
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (isInEditMode()) return;

        measureUnit = Preference.getPreferredUnits(null);
        UIHandler();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (isInEditMode()) return;

        mhandle.removeCallbacksAndMessages(null);
        mhandle = null;
        EventBus.getDefault().unregister(this);
    }

}
