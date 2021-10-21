package rcmobile.FPV.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.FeatureSwitch;
import com.andruav.util.Maths;

import rcmobile.FPV.App;
import rcmobile.FPV.R;

/**
 * Created by M.Hefny on 22-Mar-15.
 */
public class JoystickView extends View {

    public static final int INVALID_POINTER_ID = -1;
    private static final boolean D = false;
    public String TAG = "JoystickView";
    public int id;
    private final JoystickView Me;

    // Stick Parameters
    public boolean returnHandleToCenterChannel1=false;
    public boolean returnHandleToCenterChannel2=false;
    public boolean reverseChannel2;
    public boolean reverseChannel1;
    public int minValueChannel1;
    public int minValueChannel2;
    public int maxValueChannel1;
    public int maxValueChannel2;

    public int horizontalChannel;  // horizontal stick
    public int verticalChannel;  // vertical stick

    // joystick model
    private Paint bgPaint;
    private Paint statusText;

    private        Paint  handlePaint;
    //private        Paint  basePaint;
    private static Bitmap handleBitmap;
    private static Bitmap handleResizedBitmap;
    private        Matrix scaleMatrix;
    private int handleRadius;
    private int movementDiameter;
    private int movementRadius;
    private int handleInnerBoundaries;

    //Last touch point in edtMinValue coordinates
    private int pointerId = INVALID_POINTER_ID;
    private float touchX, touchY;

    //Last reported position in edtMinValue coordinates (allows different reporting sensitivities)
    private float reportX, reportY;

    //Handle center in edtMinValue coordinates
    private float handleX, handleY;

    //Center of the edtMinValue in edtMinValue coordinates
    private int cX, cY;

    // onPointer_UP last x & y
    private boolean bCapture = true;
    private int oldX, oldY;
    private int handleFinderOffsetX, handleFinderOffsetY;

    //Size of the edtMinValue in edtMinValue coordinates
    private int dimX, dimY, dimmin;

    //Cartesian coordinates of last touch point - joystick center is (0,0)
    private int cartX, cartY;

    //Polar coordinates of the touch point from joystick center
    private double radial;
    private double angle;

    //# of pixels movement required between reporting to the listener
    private float moveResolution;

    //User coordinates of last touch point
    private int userX, userY;

    //Records touch pressure for click handling
    private float   touchPressure;
    private boolean clicked;
    private float   clickThreshold;

    //Offset co-ordinates (used when touch events are received from parent's coordinate origin)
    private int offsetX;
    private int offsetY;

    //Max range of movement in user coordinate system
    public final static int CONSTRAIN_BOX    = 0;
    public final static int CONSTRAIN_CIRCLE = 1;
    private int   movementConstraint;
    private float movementRangeChannel1;
    private float movementRangeChannel2;

    public final static int COORDINATE_CARTESIAN    = 0;        //Regular cartesian coordinates
    public final static int COORDINATE_DIFFERENTIAL = 1;    //Uses polar rotation of 45 degrees to calc differential drive paramaters
    private int userCoordinateSystem;


    private final String[] mLabels = {"AIL", "ELE", "THR", "RUD"};
    private boolean showCircle = true;
    private JoystickMovedListener moveListener;
    private JoystickClickedListener clickListener;

    public void setPointerId(int id)
    {
        this.pointerId = id;
    }

    public int getPointerId()
    {
        return pointerId;
    }

    public void setTouchOffset(int x, int y)
    {
        offsetX = x;
        offsetY = y;
    }

    public JoystickView(Context context) {
        super(context);
        Me =this;
        initJoystickView();
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Me =this;
        initJoystickView();
    }

    public JoystickView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Me =this;
        initJoystickView();
    }

    public void setOnJostickMovedListener(JoystickMovedListener listener)
    {
        this.moveListener = listener;
    }

    public void setOnJostickClickedListener(JoystickClickedListener listener)
    {
        this.clickListener = listener;
    }


    @SuppressLint("ResourceAsColor")
    private void initJoystickView()
    {

        setFocusable(true);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.GREEN);
        bgPaint.setStrokeWidth(2);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setDither(true);

        handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handlePaint.setColor(App.context.getResources().getColor(R.color.btn_TX_HANDLER));
        handlePaint.setStrokeWidth(1);
        handlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        handlePaint.setDither(true);

        statusText = new Paint((Paint.ANTI_ALIAS_FLAG));
        statusText.setColor(Color.WHITE);
        if (this.isInEditMode()==false) {
            statusText.setTextSize(15.0f * App.getAppContext().getResources().getDisplayMetrics().density);
        }
        statusText.setTextAlign(Paint.Align.CENTER);

        //Grey Size
        moveResolution = 10.0f;
        movementRangeChannel1 = 50;
        movementRangeChannel2 = 50;
        clickThreshold=0.4f;

        // Stick Parameters
        returnHandleToCenterChannel1=false;
        returnHandleToCenterChannel2=false;
        reverseChannel2 =false;
        reverseChannel1 =false;
        minValueChannel1=1;  // 0 is used for Releasing
        minValueChannel2=1;  // 0 is used for Releasing
        maxValueChannel1=1000;
        maxValueChannel2=1000;



    }

    private int measure(int measureSpec)
    {
        int result;
        // Decode the measurement specifications.
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (FeatureSwitch.DEBUG_MODE) {
            Log.d(TAG, String.format("measure(%d,%d)", specMode, specSize));
        }


        if (specMode == MeasureSpec.UNSPECIFIED)
        {
            // Return a default size of 200 if no bounds are specified.
            result = 200;
        }
        else
        {
            // As you want to fill the available space
            // always return the full available bounds.
            result = specSize;
        }
        return result;
    }


    /***
     * When relese a handle the value does not jump, but goes from point A to center smoothly.
     * This what this function is doing.
     */
    private void returnHandleToCenter()
    {
        if (returnHandleToCenterChannel1 || returnHandleToCenterChannel2)
        {
            final int numberOfFrames = 3; //5;
            final double intervalsX = (0 - touchX) / numberOfFrames;
            final double intervalsY = (0 - touchY) / numberOfFrames;

            for (int i = 0; i < numberOfFrames; i++)
            {
                final int j = i;
                postDelayed(new Runnable()
                {
                    public void run()
                    {
                        if (returnHandleToCenterChannel1) {touchX += intervalsX; oldX+=intervalsX;}
                        if (returnHandleToCenterChannel2) {touchY += intervalsY; oldY+=intervalsY;}

                        reportOnMoved();
                        invalidate();

                        if (moveListener != null && j == numberOfFrames - 1)
                        {
                            if (returnHandleToCenterChannel1) {moveListener.OnReturnedToCenterX(Me);}
                            if (returnHandleToCenterChannel2) {moveListener.OnReturnedToCenterY(Me);}
                        }
                    }
                }, i * 40);
            }

            if (moveListener != null)
            {
                moveListener.OnReleased(Me);
            }
        }
    }


    private void calcUserCoordinates()
    {
        movementRangeChannel1 = (maxValueChannel1 - minValueChannel1);
        movementRangeChannel2 = (maxValueChannel2 - minValueChannel2);

        //First convert to cartesian coordinates
        cartX = (int) (((touchX + cX) / movementDiameter) * movementRangeChannel1) ;
        cartY = (int) (((touchY + cY)/ movementDiameter) * movementRangeChannel2) ;

        radial = Math.sqrt((cartX * cartX) + (cartY * cartY));
        angle = Math.atan2(cartY, cartX);

        //Invert X axis if requested
        if (reverseChannel1)
        {
            cartX = maxValueChannel1 - cartX;
        }
        else
        {
            cartX = cartX + minValueChannel1;
        }

        // by default Y is reversed as Y = 0 position on screen is the max in real remote
        if (!reverseChannel2)
        {
            cartY = maxValueChannel2 - cartY;
        }
        else
        {
            cartY = cartY + minValueChannel2;
        }


        /*
        if (userCoordinateSystem == COORDINATE_CARTESIAN)
        {
            userX = cartX;
            userY = cartY;
        }
        else if (userCoordinateSystem == COORDINATE_DIFFERENTIAL)
        {
            userX = cartY + cartX / 4;
            userY = cartY - cartX / 4;

            if (userX < -movementRange) userX = (int) -movementRange;
            if (userX > movementRange) userX = (int) movementRange;

            if (userY < -movementRange) userY = (int) -movementRange;
            if (userY > movementRange) userY = (int) movementRange;
        }
        */
    }

    private void reportOnMoved()
    {
        if (moveListener != null)
        {
            boolean rx = Math.abs(touchX - reportX) >= moveResolution;
            boolean ry = Math.abs(touchY - reportY) >= moveResolution;
            if (rx || ry)
            {
                this.reportX = touchX;
                this.reportY = touchY;


                calcUserCoordinates();

                if (D) Log.d(TAG, String.format("moveListener.OnMoved(%d,%d)", userX, userY));
                //moveListener.OnMoved(userX, userY);
               // moveListener.OnMoved(this,cartX, cartY,radial,angle);
                if (rx)
                {
                    moveListener.OnMoveX(this, (int) Maths.Constraint(minValueChannel1, cartX, maxValueChannel1));
                }
                if (ry)
                {
                    moveListener.OnMovedY(this, (int) Maths.Constraint(minValueChannel2,cartY,maxValueChannel2));
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Here we make sure that we have a perfect circle
        int measuredWidth  = measure(widthMeasureSpec);
        int measuredHeight = measure(heightMeasureSpec);
        measuredHeight = Math.min(measuredHeight,measuredWidth);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }


    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        if (D)
            Log.d(TAG, String.format("onLayout: (left:%d, top:%d  right:%d  bottom:%d)", left,top,right,bottom));

        dimX = getMeasuredWidth();
        dimY = getMeasuredHeight();
        dimmin = Math.min(dimX, dimY);


        cX = dimX / 2;
        cY = dimY / 2;

        oldX = cX + offsetX;
        oldY = cY + offsetY;

        //bgRadius = d / 2 - innerPadding;
        //handleRadius = (int)(d * 0.25);
        handleRadius = (int) (dimmin * 0.12);
        handleInnerBoundaries = handleRadius;
        movementDiameter = (int) (dimmin * 0.9);
        movementRadius = dimmin / 2;

        if (handleBitmap != null)
            handleResizedBitmap = Bitmap.createScaledBitmap(handleBitmap, handleRadius * 2, handleRadius * 2, false);
        else showCircle = true;
    }


    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas)
    {
        canvas.save();

        canvas.drawCircle(cX,cY,dimmin/2,bgPaint);
        // Draw the background
        //canvas.drawCircle(cX, cY, bgRadius, bgPaint);

        // Draw the handle
        handleX = touchX + cX;
        handleY = touchY + cY;
        //canvas.drawCircle(cX, cY, handleRadius >> 1, basePaint);
        //canvas.drawCircle(cX, cY, handleRadius >> 1, baseStrokePaint);
        //canvas.drawLine(cX, cY, handleX, handleY, stickPaint);

        if (showCircle) canvas.drawCircle(handleX, handleY, handleRadius, handlePaint);
        else
        {
            scaleMatrix = new Matrix();
            scaleMatrix.postTranslate(handleX - handleRadius, handleY - handleRadius);
            canvas.drawBitmap(handleResizedBitmap, scaleMatrix, null);
        }

        if (D)
        {
            Log.d(TAG, String.format("(%.0f, %.0f)", touchX, touchY));
            Log.d(TAG, String.format("(%.0f, %.0f\u00B0)", radial, angle * 180.0 / Math.PI));
            Log.d(TAG, String.format("touch(%f,%f)", touchX, touchY));
            Log.d(TAG, String.format("onDraw(%.1f,%.1f)\n\n", handleX, handleY));
        }
        canvas.restore();

        canvas.save();
        canvas.drawText(mLabels[verticalChannel], cX, cY/3, statusText);
        canvas.restore();
        canvas.save();
        canvas.drawText(mLabels[horizontalChannel], cX/3, cY, statusText);
       // canvas.rotate(90);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_MOVE:
            {
                return processMoveEvent(ev);
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            {

                if (processPointerUp(ev, action)) return true;
                break;
            }
            case MotionEvent.ACTION_DOWN:
            {
                if (pointerId == INVALID_POINTER_ID)
                {
                    setPointerId(0);
                    return processPointerDown(ev, action);

                }
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN:
            {
                if (pointerId == INVALID_POINTER_ID)
                {
                    final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                    final int pointerID = ev.getPointerId(pointerIndex);
                    if (pointerID == -1) return false;
                    pointerId = pointerID;
                    return processPointerDown(ev, action);

                }
                break;
            }
        }
        return false;
    }




    //Simple pressure click
    protected void reportOnPressure()
    {
        if (D) Log.d(TAG, String.format("touchPressure=%.2f", this.touchPressure));
        if (clickListener != null)
        {
            if (clicked && touchPressure < clickThreshold)
            {
                clickListener.OnReleased(Me);
                this.clicked = false;
                if (D) Log.d(TAG, "reset click");
                invalidate();
            }
            else if (!clicked && touchPressure >= clickThreshold)
            {
                clicked = true;
                clickListener.OnClicked(Me);
                if (D) Log.d(TAG, "click");
                invalidate();
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
        }
    }

    private boolean processPointerDown (MotionEvent ev, int action) {

       try {
               int x = (int) ev.getX(pointerId);
               int y = (int) ev.getY(pointerId);
               if ((x >= offsetX && x < offsetX + dimX)
                       && (y >= offsetY && y < offsetY + dimY)) {
                       // Touch is inside the edtMinValue control


               /* if (((Math.abs(oldY - y) <= handleRadius*3)
                           && (Math.abs(oldX - x) <= handleRadius*3)
                )) {//Touch inside the stick
                // Touch is over the handler itself
                if (D) Log.d(TAG, "ACTION_POINTER_DOWN: " + pointerId);

                   return true;
               }
                */
                   return true;
                }
           setPointerId(INVALID_POINTER_ID);
           return false;
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException(AndruavSettings.AccessCode,"exception_joystick", ex);
            return false;
        }
    }


    private boolean processPointerUp(MotionEvent ev, int action) {
        try {

            if (pointerId != INVALID_POINTER_ID) {
                final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == this.pointerId) {
                    if (D) Log.d(TAG, "ACTION_POINTER_UP: " + pointerId);

                    oldX = (int) handleX + offsetX;
                    oldY = (int) handleY + offsetY;

                    returnHandleToCenter();
                    setPointerId(INVALID_POINTER_ID);
                    bCapture = true;
                    return true;
                }
            }
            return false;
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException(AndruavSettings.AccessCode,"exception_joystick", ex);
            return false;
        }

    }

    private long lasttime=0;
    private boolean processMoveEvent(MotionEvent ev)
    {
        try {


            if (pointerId != INVALID_POINTER_ID) {

                long now = System.currentTimeMillis();
                if (now - lasttime < 200)
                {
                    return true;
                }
                lasttime = now;


                final int pointerIndex = ev.findPointerIndex(pointerId);
                if (pointerIndex ==-1) return false;
                float x = ev.getX(pointerIndex);
                float y = ev.getY(pointerIndex);
                if (bCapture) {
                    handleFinderOffsetX = (int) x - oldX;
                    handleFinderOffsetY = (int) y - oldY;
                    bCapture = false;
                }
                // Translate touch position to center of edtMinValue
                float temptouchX = x - cX - offsetX - handleFinderOffsetX;
                float temptouchY = y - cY - offsetY - handleFinderOffsetY;
                if (Math.abs(temptouchX) < movementRadius)
                {
                    touchX = temptouchX;
                }
                if (Math.abs(temptouchY) < movementRadius)
                {
                    touchY = temptouchY;
                }

                if (D)
                    Log.d(TAG, String.format("ACTION_MOVE: (%03.0f, %03.0f) => (%03.0f, %03.0f)", x, y, touchX, touchY));

                reportOnMoved();
                invalidate();

                touchPressure = ev.getPressure(pointerIndex);
                reportOnPressure();

                return true;
            }
            return false;
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException(AndruavSettings.AccessCode,"exception_joystick", ex);
            return false;
        }
    }
}
