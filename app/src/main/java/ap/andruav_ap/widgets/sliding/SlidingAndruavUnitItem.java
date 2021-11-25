package ap.andruav_ap.widgets.sliding;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.event.droneReport_7adath._7adath_GCSBlockedChanged;
import com.andruav.TelemetryProtocol;

import ap.andruav_ap.App;
import ap.andruav_ap.helpers.GUI;
import ap.andruav_ap.R;

/**
 * Created by M.Hefny on 22-Apr-15.
 */
public class SlidingAndruavUnitItem extends ListUnitItemBase {


    protected Button mMainButton;
    protected Button mMainRemote;
    protected Button mMainCamera;
    protected Button mMainVideoStreaming;
    protected Button mMainRecordVideo;
    protected Button mModeButton;

    protected boolean bItIsMe = false;
    /***
     * Remote is currently selected
     */
    protected boolean bRemoteEnable = false;
    protected boolean mAllowRemoteCGSSelect = true;

    /***
     * Internal function
     *
     * @param enable
     */
    void allowRemoteCGSSelect(boolean enable) {
        mAllowRemoteCGSSelect = enable;
        setMainButton();
    }

    public void setMainButton() {
        Drawable img = null;
        boolean bEnable = true;
        boolean bRemoteGCS = false;

        if (bItIsMe) {
            mMainButton.setText("Me");
        } else {
            // put label in all cases
            mMainButton.setText(GUI.getButtonText(mAndruavUnit));

            // disable select for remote GCS
            if (!mAllowRemoteCGSSelect && mAndruavUnit.getIsCGS()) {
                bRemoteGCS = true;
                bEnable = false;
                img = App.getAppContext().getResources().getDrawable(R.drawable.hand_gy_32x32);
            }
        }

        if (!bRemoteGCS) {
            if (mAndruavUnit.isGUIActivated) {
                img = App.getAppContext().getResources().getDrawable(R.drawable.hand_g_32x32);
            } else {
                if (mAndruavUnit.getisGCSBlockedFromBoard()) {
                    img = App.getAppContext().getResources().getDrawable(R.drawable.hand_r_32x32);
                }
                else
                {
                    img = App.getAppContext().getResources().getDrawable(R.drawable.hand_b_32x32);
                }

            }
        }


        mMainButton.setClickable(bEnable && !bRemoteGCS);
        mMainButton.setCompoundDrawablesWithIntrinsicBounds(null, null, img, null);
    }


    private void setModeButton() {
        Drawable img = App.getAppContext().getResources().getDrawable(R.drawable.flightcontrol_gr_32x32);
        int color = getResources().getColor(R.color.btn_TXT_GREY_DARK);
        boolean bEnable = false;

       if (mAndruavUnit.isControllable()) {   // Active Board is there
            bEnable = true;
            img = App.getAppContext().getResources().getDrawable(R.drawable.flightcontrol_g_32x32);
            color = getResources().getColor(R.color.btn_TXT_GREEN_DARKER);
        }



        mModeButton.setTextColor(color);
        mModeButton.setClickable(true); // always true to call connect to FCB.
        mModeButton.setCompoundDrawablesWithIntrinsicBounds(null, img, null, null);

    }

    private void setRemoteButton() {
        Drawable img;
        int color = getResources().getColor(R.color.btn_TXT_GREY_DARK);
        boolean bEnable = true;
        if (mAndruavUnit.getIsCGS()) {
            img = App.getAppContext().getResources().getDrawable(R.drawable.remote_gy_32x32);
            color = getResources().getColor(R.color.btn_TXT_GREY_DARK);
        } else if (bRemoteEnable) {
            img = App.getAppContext().getResources().getDrawable(R.drawable.remote_rg_32x32);
            color = getResources().getColor(R.color.btn_TXT_BLUE_DARKER);
        } else {
            switch (mAndruavUnit.getTelemetry_protocol()) {
               case TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry:
                    img = App.getAppContext().getResources().getDrawable(R.drawable.remote_b_32x32);
                    color = getResources().getColor(R.color.btn_TXT_BLUE_DARKER);
                    break;
               case TelemetryProtocol.TelemetryProtocol_Unknown_Telemetry:
                    img = App.getAppContext().getResources().getDrawable(R.drawable.remote_r1_32x32);
                    color = getResources().getColor(R.color.btn_TXT_ERROR);
                    bEnable = false;
                    break;
                case TelemetryProtocol.TelemetryProtocol_No_Telemetry:
                default:
                    img = App.getAppContext().getResources().getDrawable(R.drawable.remote_gy_32x32);
                    color = getResources().getColor(R.color.btn_TXT_GREY_DARK);
                    bEnable = false;
                    break;
            }
        }
        mMainRemote.setTextColor(color);
        mMainRemote.setClickable(bEnable);
        mMainRemote.setCompoundDrawablesWithIntrinsicBounds(null, img, null, null);

    }

    private void setCameraButton() {
        int color;
        Drawable img;
        boolean bEnable = true;

        if ((mAndruavUnit.getIsCGS()) || (mAndruavUnit.getIsShutdown())) {
            img = App.getAppContext().getResources().getDrawable(R.drawable.camera_gy_32x32);
            color = getResources().getColor(R.color.btn_TXT_GREY);
            bEnable = false;
        } else {
            img = App.getAppContext().getResources().getDrawable(R.drawable.camera_bg_32x32);
            color = getResources().getColor(R.color.btn_TXT_BLUE_DARKER);
        }

        mMainCamera.setTextColor(color);
        mMainCamera.setClickable(bEnable);
        mMainCamera.setCompoundDrawablesWithIntrinsicBounds(null, img, null, null);

    }

    private void setRecordVideo() {

        int color;
        Drawable img;
        boolean bEnable = true;

        if ((mAndruavUnit.getIsCGS()) || (mAndruavUnit.getIsShutdown())) {
            img = App.getAppContext().getResources().getDrawable(R.drawable.video_recording_disabled_32x32);
            color = getResources().getColor(R.color.btn_TXT_GREY);
            bEnable = false;
        } else {
            int imgID;
            if (mAndruavUnit.VideoRecording == AndruavUnitBase.VIDEORECORDING_OFF) {
                imgID = R.drawable.video_recording_enabled_32x32;
            } else {
                imgID = R.drawable.video_recording_active_32x32;
            }
            img = App.getAppContext().getResources().getDrawable(imgID);
            color = getResources().getColor(R.color.btn_TXT_BLUE_DARKER);
        }

        mMainRecordVideo.setTextColor(color);
        mMainRecordVideo.setClickable(bEnable);
        mMainRecordVideo.setCompoundDrawablesWithIntrinsicBounds(null, img, null, null);
    }


    public void updateVideoStreamingButton() {

        mMainVideoStreaming.setVisibility(VISIBLE);

        int color;
        Drawable img;
        boolean bEnable = true;

        if ((mAndruavUnit.getIsCGS()) || (mAndruavUnit.getIsShutdown())) {
            img = App.getAppContext().getResources().getDrawable(R.drawable.videocam_gr_32x32);
            color = getResources().getColor(R.color.btn_TXT_GREY);
            bEnable = false;
        } else {
            if (mAndruavUnit.VideoStreamingActivated) {
                img = App.getAppContext().getResources().getDrawable(R.drawable.videocam_active_32x32);
                color = getResources().getColor(R.color.btn_TXT_GREY);
            } else {
                img = App.getAppContext().getResources().getDrawable(R.drawable.videocam_gb_32x32);
                color = getResources().getColor(R.color.btn_TXT_BLUE_DARKER);
            }

        }

        mMainVideoStreaming.setTextColor(color);
        mMainVideoStreaming.setClickable(bEnable);
        mMainVideoStreaming.setCompoundDrawablesWithIntrinsicBounds(null, img, null, null);

    }


    public void setUnitID(AndruavUnitShadow andruavUnit, boolean allowRemoteGCSSelect) {
        mAllowRemoteCGSSelect = allowRemoteGCSSelect;
        setUnit(andruavUnit);
    }

    /***
     *
     * @param andruavUnit
     */
    @Override
    public void setUnit(AndruavUnitShadow andruavUnit) {
        mAndruavUnit = andruavUnit;
        if (mAndruavUnit != null) {
            bItIsMe = andruavUnit.IsMe();

            if ((andruavUnit.isGUIActivated == false) && (bItIsMe == false)) {
                bRemoteEnable = false;
            }
            setMainButton();
            setModeButton();
            setRemoteButton();
            setCameraButton();
            setRecordVideo();
            //updateVideoStreamingButton();
        }

    }

    public void enableRemote(boolean enabled) {
        bRemoteEnable = enabled;
        setRemoteButton();
    }

    public void enableVideoStreaming() {
        updateVideoStreamingButton();
    }


    /***
     * Callback when Main Unit is clicked.
     * @param onClickListener
     */
    public void setOnClickListener(OnClickListener onClickListener) {
        this.mMainButton.setOnClickListener(onClickListener);
    }

    /***
     * Callback when Mode button is clicked.
     * @param onClickListener
     */
    public void setOnModeClickListener(OnClickListener onClickListener) {
        this.mModeButton.setOnClickListener(onClickListener);
    }

    /***
     * Callback when remote button is clicked.
     * @param onClickListener
     */
    public void setOnRemoteClickListener(OnClickListener onClickListener) {
        this.mMainRemote.setOnClickListener(onClickListener);
    }

    /***
     * Callback when camera button is selected.
     * @param onClickListener
     */
    public void setOnCameraClickListener(OnClickListener onClickListener) {
        this.mMainCamera.setOnClickListener(onClickListener);
    }


    /***
     * Callback when video stream is clicked.
     * @param onClickListener
     */
    public void setOnVideoStreamingListener(OnClickListener onClickListener) {
        this.mMainVideoStreaming.setOnClickListener(onClickListener);
    }

    /***
     * Callback when video recording is clicked.
     * @param onClickListener
     */
    public void setOnRecordVideoClickListener(OnClickListener onClickListener) {
        this.mMainRecordVideo.setOnClickListener(onClickListener);
    }


    private void initGUI(Context context) {
        mInflater = LayoutInflater.from(context);
        mInflater.inflate(R.layout.slidingitem_andruav_unit, this, true);

        mMainButton = findViewById(R.id.slidingitem_andruavunit_btnUnit);
        mMainRemote = findViewById(R.id.slidingitem_andruavunit_btnRemote);
        mMainCamera = findViewById(R.id.slidingitem_andruavunit_btnCamera);
        mMainVideoStreaming = findViewById(R.id.slidingitem_andruavunit_btnVideoStream);
        mMainRecordVideo = findViewById(R.id.slidingitem_andruavunit_btnVideo);
        mModeButton = findViewById(R.id.slidingitem_andruavunit_btnMode);


        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            // a Lollypop Issue http://stackoverflow.com/questions/26958909/why-is-my-button-text-coerced-to-all-caps-on-lollipop
            mMainButton.setTransformationMethod(null);
        }
    }

    public SlidingAndruavUnitItem(Context context, AndruavUnitShadow andruavUnit) {
        super(context);

        initGUI(context);
        setUnit(andruavUnit);
    }

    public SlidingAndruavUnitItem(Context context) {
        super(context);

        initGUI(context);
        initGUI(context);
    }

    public SlidingAndruavUnitItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        initGUI(context);
    }

    public SlidingAndruavUnitItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initGUI(context);
    }

    @Override
    protected void OnHandleMessage (final Message msg)
    {
        if (msg.obj instanceof _7adath_GCSBlockedChanged)
        {
           setMainButton();
           setModeButton();
           setRemoteButton();

        }
    }

}