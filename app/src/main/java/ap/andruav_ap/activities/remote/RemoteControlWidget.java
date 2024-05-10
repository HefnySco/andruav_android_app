package ap.andruav_ap.activities.remote;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;

import android.widget.RelativeLayout;

import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.Constants;
import com.andruav.event.droneReport_Event.Event_RemoteControlSettingsReceived;
import com.andruav.controlBoard.ControlBoardBase;

import de.greenrobot.event.EventBus;
import com.andruav.AndruavFacade;

import ap.andruav_ap.App;
import ap.andruavmiddlelibrary.eventClasses.remoteControl.Event_RemoteEngaged_CMD;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruav_ap.R;
import ap.andruav_ap.widgets.JoystickClickedListener;
import ap.andruav_ap.widgets.JoystickMovedListener;
import ap.andruav_ap.widgets.JoystickView;

/**
 * Created by M.Hefny on 26-Mar-15.
 */
public class RemoteControlWidget extends RelativeLayout implements JoystickMovedListener, JoystickClickedListener {



    private static final int CONST_MSG_SENDSTATUS = 1;

    private final int maxChannels =8;
    private final float[] curValues = new float[maxChannels];
    private final float[] minValues = new float[maxChannels];
    private final float[] maxValues = new float[maxChannels];

    private LayoutInflater mInflater;

    private JoystickView stickL;
    private JoystickView stickR;

    private boolean mNewData = false;

    private Handler mhandler;
    private HandlerThread mhandlerThread;
    Boolean mkillMe = false;

    private final int[] mChannels = new int[8];
    private int mRemoteMode;  //1,2,3,4

    private boolean mEngaged;
    public  boolean isEngaged()
    {
        return mEngaged;
    }


    //private String mUnitID;
    private AndruavUnitShadow mAndruavUnit;

    private RemoteControlWidget Me;


    public void onEvent (final Event_RemoteControlSettingsReceived a7adath_remoteControlSettingsReceived)
    {
        if ((mAndruavUnit == null) || (!mAndruavUnit.Equals(a7adath_remoteControlSettingsReceived.mAndruavWe7da))) {
            return;
        }

        updateSettings();
    }

    private void loadPreference()
    {
        for (int i = 0 ; i < 8; i = i + 1) {
            minValues[i] = Preference.getGamePadChannelminValue(null,i);
            maxValues[i] = Preference.getGamePadChannelmaxValue(null,i);
        }
    }

    public AndruavUnitBase getEngagedPartyID()
    {
        return mAndruavUnit;
    }

    public void setRemoteMode(int mode)
    {
        switch (mode)
        {
            case 1:
                stickL.verticalChannel = ControlBoardBase.CONST_CHANNEL_2_PITCH;
                stickL.horizontalChannel = ControlBoardBase.CONST_CHANNEL_4_YAW;
                stickR.verticalChannel = ControlBoardBase.CONST_CHANNEL_3_THROTTLE;
                stickR.horizontalChannel = ControlBoardBase.CONST_CHANNEL_1_ROLL;
                break;

            case 2:
                stickL.verticalChannel = ControlBoardBase.CONST_CHANNEL_3_THROTTLE;
                stickL.horizontalChannel = ControlBoardBase.CONST_CHANNEL_4_YAW;
                stickR.verticalChannel = ControlBoardBase.CONST_CHANNEL_2_PITCH;
                stickR.horizontalChannel = ControlBoardBase.CONST_CHANNEL_1_ROLL;
                break;

            case 3:
                stickL.verticalChannel = ControlBoardBase.CONST_CHANNEL_2_PITCH;
                stickL.horizontalChannel = ControlBoardBase.CONST_CHANNEL_1_ROLL;
                stickR.verticalChannel = ControlBoardBase.CONST_CHANNEL_3_THROTTLE;
                stickR.horizontalChannel = ControlBoardBase.CONST_CHANNEL_4_YAW;
                break;

            case 4:
                stickL.verticalChannel = ControlBoardBase.CONST_CHANNEL_3_THROTTLE;
                stickL.horizontalChannel = ControlBoardBase.CONST_CHANNEL_1_ROLL;
                stickR.verticalChannel = ControlBoardBase.CONST_CHANNEL_2_PITCH;
                stickR.horizontalChannel = ControlBoardBase.CONST_CHANNEL_4_YAW;
                 break;
        }

        stickL.invalidate();
        stickR.invalidate();
    }

    public int getRemoteMode()
    {
        return mRemoteMode;
    }


    protected void initHandler ()
    {
        mhandlerThread = new HandlerThread("RemoteControl");
        mhandlerThread.start(); //NOTE: mhandlerThread.getLooper() will return null if not started.

        mhandler = new Handler(mhandlerThread.getLooper())
        {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (mkillMe || (!mEngaged)) return;

                if ((mNewData) && (msg.what==CONST_MSG_SENDSTATUS))
                {
                    mNewData = false;

                    AndruavFacade.sendRemoteControlMessage(Me.mChannels, true, mAndruavUnit);
                }
                mhandler.sendEmptyMessageDelayed(CONST_MSG_SENDSTATUS, Constants.CONST_REMOTECONTROL_RATE);

            }
        };

        mhandler.sendEmptyMessageDelayed(CONST_MSG_SENDSTATUS,Constants.CONST_REMOTECONTROL_RATE);
    }


    protected void shutDown()
    {
        mkillMe = true;
        if (mhandler != null) {
            mhandler.removeCallbacksAndMessages(null);
            mhandler = null;
        }

        if (mhandlerThread != null)
        {
            mhandlerThread.quit();
        }

    }

    public void startEngage (AndruavUnitShadow andruavWe7da)
    {
        if ((andruavWe7da == null) || (andruavWe7da.getIsCGS()))
        {
            stopEngage();
            return ;
        }

        if ((mEngaged) && (mAndruavUnit.Equals(andruavWe7da)))
        {
            //stopEngage();  I AM ALREADY ENGAGED
            return ;
        }


        stopEngage(); // disengage OLD if exists

        mEngaged = true;
        mAndruavUnit = andruavWe7da;

        AndruavFacade.engageGamePad(andruavWe7da);

        // Read settings of Drone RemoteControlSettings
        updateSettings();
        loadPreference();
        initHandler();
        AndruavEngine.notification().Speak(App.getAppContext().getString(R.string.action_engaged));

        if (!EventBus.getDefault().isRegistered(this)) {
            // FATAL ISSUE - Just Sainty check ... but it should have been correcting by stopEngane previous connection            EventBus.getDefault().register(this);
        }
    }

    public void stopEngage()
    {
        if (!mEngaged) return ;
        mEngaged = false;


        AndruavFacade.disengageGamePad(mAndruavUnit);
        mAndruavUnit = null;

        AndruavEngine.notification().Speak(App.getAppContext().getString(R.string.action_disengaged));

        EventBus.getDefault().unregister(this);
    }

    public RemoteControlWidget(Context context) {
        super(context);

        initGUI(context);
    }

    public RemoteControlWidget(Context context, AttributeSet attrs) {
        super(context, attrs);

        initGUI(context);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public RemoteControlWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initGUI(context);
    }


    private void initGUI(Context context)
    {
        if (this.getRootView().isInEditMode() ) return ;

        Me = this;

        for (int i=0;i<4;++i)
        {
            mChannels[i] = -999;
        }
        mInflater = LayoutInflater.from(context);
        mInflater.inflate(R.layout.widget_remote_control, this, true);

        stickL = findViewById(R.id.widget_remote_control_stickL);
        stickR = findViewById(R.id.widget_remote_control_stickR);

        stickL.reverseChannel2 =true;
        stickL.reverseChannel1 = true;

        stickL.TAG = "L";
        stickL.id=0;
        stickR.TAG = "R";
        stickR.id=1;
        stickL.setPointerId(JoystickView.INVALID_POINTER_ID);
        stickR.setPointerId(JoystickView.INVALID_POINTER_ID);
        stickL.setOnJostickMovedListener(this);
        stickR.setOnJostickMovedListener(this);
        stickR.setBackgroundColor(Color.TRANSPARENT);
        stickL.setBackgroundColor(Color.TRANSPARENT);

        updateSettings();


    }

    /***
     * Updates remote settings from preference data.
     */
    public void updateSettings()
    {
        if (this.isInEditMode()) return ;
        setRemoteMode(Preference.getRemoteFlightMode(null));

        updateJoyStick(stickL);
        updateJoyStick(stickR);
    }

    protected void updateJoyStick(JoystickView joystick)
    {

        boolean[] RTC = null;
        if (mAndruavUnit != null)
        {
            RTC = mAndruavUnit.getRTC()  ;
        }
        else
        {

        }

        joystick.reverseChannel1 = Preference.isChannelReversed(null, joystick.horizontalChannel);
        joystick.reverseChannel2 = Preference.isChannelReversed(null, joystick.verticalChannel);
        if (RTC != null) {
            joystick.returnHandleToCenterChannel1 = RTC[joystick.horizontalChannel];
            joystick.returnHandleToCenterChannel2 =RTC[joystick.verticalChannel];
        }
        else
        {
            joystick.returnHandleToCenterChannel1 = Preference.isChannelReturnToCenter(null, joystick.horizontalChannel);
            joystick.returnHandleToCenterChannel2 = Preference.isChannelReturnToCenter(null, joystick.verticalChannel);
        }

        joystick.minValueChannel1 = 1;      // zero means release
        joystick.maxValueChannel1 = 1000;
        joystick.minValueChannel2 = 1;      // zero means release
        joystick.maxValueChannel2 = 1000;


    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        super.onLayout(changed, l, t, r, b);

        if (getRootView().isInEditMode()) return ;

        if (!changed) return ;
        stickR.setTouchOffset(stickR.getLeft(), stickR.getTop());
        stickL.setTouchOffset(stickL.getLeft(), stickL.getTop());
    }


    @Override
    public void OnClicked(JoystickView sender) {

    }

    @Override
    public void OnMoved(JoystickView sender,int X, int Y, double radial, double angle) {


    }

    @Override
    public void OnMoveX(JoystickView sender, int value){
        mChannels[sender.horizontalChannel]= value;
        mNewData = true;
    }



    @Override
    public void OnMovedY(JoystickView sender, int value){
        mChannels[sender.verticalChannel]= value;
        mNewData = true;
    }

    @Override
    public void OnReleased(JoystickView sender) {

    }

    @Override
    public void OnReturnedToCenterX(JoystickView sender) {

        mChannels[sender.horizontalChannel]= 500;
        mNewData = true;
    }

    @Override
    public void OnReturnedToCenterY(JoystickView sender) {

        mChannels[sender.verticalChannel]= 500;
        mNewData = true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        boolean l = stickL.dispatchTouchEvent(ev);
        boolean r = stickR.dispatchTouchEvent(ev);
        return l || r;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        boolean l = stickL.onTouchEvent(ev);
        boolean r = stickR.onTouchEvent(ev);
        return l || r;
    }



    protected void reportOnEngaged()
    {

        Event_RemoteEngaged_CMD event_RemoteEngaged_CMD = new Event_RemoteEngaged_CMD(this.mEngaged);
        EventBus.getDefault().post(event_RemoteEngaged_CMD);
    }
}