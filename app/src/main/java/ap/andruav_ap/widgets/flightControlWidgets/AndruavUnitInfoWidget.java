package ap.andruav_ap.widgets.flightControlWidgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import androidx.fragment.app.FragmentManager;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.Constants;
import com.andruav.event.droneReport_7adath._7adath_FCB_Changed;
import com.andruav.event.droneReport_7adath._7adath_GCSBlockedChanged;
import com.andruav.event.droneReport_7adath._7adath_UnitShutDown;
import com.andruav.event.droneReport_7adath._7adath_Vehicle_Flying_Changed;
import com.andruav.event.droneReport_7adath._7adath_Vehicle_Mode_Changed;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.controlBoard.shared.common.VehicleTypes;
import com.andruav.sensors.AndruavBattery;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.App;
import ap.andruav_ap.R;
import ap.andruav_ap.helpers.GUI;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.factory.util.HtmlPro;
import ap.andruavmiddlelibrary.factory.math.UnitConversion;
import com.andruav.event.droneReport_7adath._7adath_Battery_Ready;
import com.andruav.event.droneReport_7adath._7adath_GPS_Ready;
import com.andruav.event.droneReport_7adath._7adath_IMU_Ready;

/**
 * Created by mhefny on 1/13/16.
 */
public class AndruavUnitInfoWidget extends RelativeLayout {



    //////// Attributes
    AndruavUnitInfoWidget Me;

    private LayoutInflater mInflater;
    private AndruavUnitBase andruavWe7da;


    private TextView mMobileBattery;
    private TextView mFCBBattery;
    private TextView mFCBMode;
    private TextView mIMUAltitude;
    private TextView mAndruavInfo;
    private TextView mGPS;


    private boolean useMeterUnits = true;
    public boolean isRecording = false;

    private boolean useFCBIMU = false;
    private int preferredUnits;

    private Handler mhandler;

    private int mHighlighted = 0;
    private static final int  CONST_HIGHLIGHT_DIM     = 0;
    private static final int  CONST_HIGHLIGHT_MEDIUM  = 1;
    private static final int  CONST_HIGHLIGHT_BRIGHT  = 2;

    private FragmentManager parentFragmentManager;


    //////////BUS EVENT


    public void onEvent (final _7adath_FCB_Changed a7adath_fcb_changed)
    {
        if ((andruavWe7da == null) || (!andruavWe7da.Equals(a7adath_fcb_changed.andruavUnitBase)))
        {
            return ;
        }

        final Message msg = mhandler.obtainMessage();
        msg.obj = a7adath_fcb_changed;
        if (mhandler != null)  mhandler.sendMessageDelayed(msg,0);
    }

    public void onEvent (final _7adath_Vehicle_Flying_Changed a7adath_vehicle_flying_changed)
    {
        if ((andruavWe7da == null) || (!andruavWe7da.Equals(a7adath_vehicle_flying_changed.mAndruavWe7da)))
        {
            return ;
        }

        final Message msg = mhandler.obtainMessage();
        msg.obj = a7adath_vehicle_flying_changed;
        if (mhandler != null)  mhandler.sendMessageDelayed(msg,0);
    }

    public void onEvent (final _7adath_Vehicle_Mode_Changed a7adath_vehicle_mode_changed)
    {
        if ((andruavWe7da == null) || (!andruavWe7da.Equals(a7adath_vehicle_mode_changed.mAndruavWe7da)))
        {
            return ;
        }

        final Message msg = mhandler.obtainMessage();
        msg.obj = a7adath_vehicle_mode_changed;
        if (mhandler != null)  mhandler.sendMessageDelayed(msg,0);
    }


    public void onEvent (final _7adath_GCSBlockedChanged a7adath_gcsBlockedChanged)
    {
        if ((andruavWe7da == null) || (!andruavWe7da.Equals(a7adath_gcsBlockedChanged.andruavUnitBase)))
        {
            return ;
        }

        final Message msg = mhandler.obtainMessage();
        msg.obj = a7adath_gcsBlockedChanged;
        if (mhandler != null)  mhandler.sendMessageDelayed(msg,0);
    }

    /***
     * Local IMU events
     * @param a7adath_imu_ready
     */
    public void onEvent (final _7adath_IMU_Ready a7adath_imu_ready)
    {
        // This is local IMU so return if local Unit is not selected
        if ((andruavWe7da ==null) || (!andruavWe7da.Equals(a7adath_imu_ready.mAndruavWe7da)))
        {
            return ;
        }

        final Message msg = mhandler.obtainMessage();
        msg.obj = a7adath_imu_ready;
        if (mhandler != null)  mhandler.sendMessageDelayed(msg,0);
    }


    public void onEvent (final _7adath_GPS_Ready a7adath_gps_ready)
    {
        // This is local IMU so return if local Unit is not selected
        if ((andruavWe7da ==null) || (!andruavWe7da.Equals(a7adath_gps_ready.mAndruavWe7da)))
        {
            return ;
        }

        final Message msg = mhandler.obtainMessage();
        msg.obj = a7adath_gps_ready;
        if (mhandler != null)  mhandler.sendMessageDelayed(msg,0);
    }

    /***
     * Local Battery Info
     * @param a7adath_battery_ready
     */
    public void onEvent (final _7adath_Battery_Ready a7adath_battery_ready) {

        // This is local IMU so return if local Unit is not selected
        if ((andruavWe7da ==null) || (!andruavWe7da.Equals(a7adath_battery_ready.mAndruavWe7da)))
        {
            return ;
        }

        final Message msg = mhandler.obtainMessage();
        msg.obj = a7adath_battery_ready;
        if (mhandler != null)  mhandler.sendMessageDelayed(msg,0);
    }


    public void onEvent (final _7adath_UnitShutDown a7adath_unitShutDown)
    {
        if ((andruavWe7da ==null) || (!andruavWe7da.Equals(a7adath_unitShutDown.andruavUnitBase)))
        {
            return;
        }

        final Message msg = mhandler.obtainMessage();
        msg.obj = a7adath_unitShutDown;
        if (mhandler != null)  mhandler.sendMessageDelayed(msg,0);
    }

    //////////////////////////////////////////////////////////////////////

    public void setParentFragmentManager(FragmentManager parentFragmentManager) {
        this.parentFragmentManager = parentFragmentManager;
    }

    public void setAndruavUnit (final AndruavUnitBase andruavUnit)
    {
        andruavWe7da = andruavUnit;

        if (andruavUnit==null)
        {
            andruavWe7da = null;
            applyHighlight(0);

        }else {

            if (andruavUnit.getIsCGS() && andruavUnit.IsMe()) {
                andruavWe7da = null;
            }
        }

        updateInfo();
    }


    private void initGUI (final Context context)
    {
        if (this.isInEditMode()) return ;

        Me = this;


        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (andruavWe7da ==null) return ; // ignore if no unit is selected.

                toggleHighlight();
            }
        });

        preferredUnits = Preference.getPreferredUnits(null);

        mInflater = LayoutInflater.from(context);
        mInflater.inflate(R.layout.widget_andruav_unit_info_bar, this, true);


        mMobileBattery  = findViewById(R.id.widgetandruavinfo_mobilebat);
        mFCBBattery     = findViewById(R.id.widgetandruavinfo_fcbbat);
        mFCBMode = findViewById(R.id.widgetandruavinfo_fcb);
        mIMUAltitude    = findViewById(R.id.widgetandruavinfo_altitude);
        mAndruavInfo    = findViewById(R.id.widgetandruavinfo_name);
        mGPS            = findViewById(R.id.widgetandruavinfo_gps);

        OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mHighlighted == CONST_HIGHLIGHT_DIM)
                {   // change units only if the control is medium or bright ... as if it is hidden then most propably the use is trying to highlight the control.
                    toggleHighlight();
                }
                else {
                    if (Me.useMeterUnits == true) {
                        Preference.setPreferredUnits(null, Constants.Preferred_UNIT_IMPERIAL_SYSTEM); // use miles
                        Me.useMeterUnits = false;
                    } else {
                        Preference.setPreferredUnits(null, Constants.Preferred_UNIT_METRIC_SYSTEM); // use meters
                        Me.useMeterUnits = true;
                    }
                }
                preferredUnits = Preference.getPreferredUnits(null);
                updateIMUStatus();
            }
        };
        mIMUAltitude.setOnClickListener(onClickListener);
        mFCBMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (andruavWe7da == null) return ;
                if (parentFragmentManager != null) {

                    FCBControl_Dlg fcbControl_dlg = FCBControl_Dlg.newInstance(andruavWe7da);
                    fcbControl_dlg.show(parentFragmentManager, "fragment_edit_name");
                }
            }
        });


        mGPS.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (andruavWe7da == null) return ;
                if (parentFragmentManager != null) {

                    GPSSourceControl_Dlg gpsSourceControl_dlg = GPSSourceControl_Dlg.newInstance(andruavWe7da);
                    gpsSourceControl_dlg.show(parentFragmentManager, "fragment_edit_name");
                }

            }
        });
        updateInfo();
    }

    /**
     * Here we toggle value of highlight
     */
    private void toggleHighlight()
    {
        switch (mHighlighted)
        {
            case CONST_HIGHLIGHT_DIM:
                Me.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                Me.setAlpha(1.0f);
                mHighlighted = 1;
                break;

            case CONST_HIGHLIGHT_MEDIUM:
                Me.setBackgroundColor(getResources().getColor(R.color.btn_TXT_WHITE));
                Me.setAlpha(0.8f);
                mHighlighted = 2;
                break;

            case CONST_HIGHLIGHT_BRIGHT:
                Me.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                Me.setAlpha(0.1f);
                mHighlighted = 0;
                break;
        }
    }




    /***
     * Here we apply value without changing it.
     * @param value
     */
    private void applyHighlight (final int value)
    {
        mHighlighted=value;
        toggleHighlight();
    }


    public AndruavUnitInfoWidget(Context context) {
        super(context);

        initGUI(context);
    }

    public AndruavUnitInfoWidget(Context context, AttributeSet attrs) {
        super(context, attrs);

        initGUI(context);
    }

    public AndruavUnitInfoWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AndruavUnitInfoWidget(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initGUI(context);
    }




    protected void disableMobileBatteryStatus ()
    {
        mMobileBattery.setText(App.context.getString(R.string.empty_content));
        final Drawable imgbat = App.context.getResources().getDrawable(R.drawable.battery_gr_32x32);
        mMobileBattery.setCompoundDrawablesWithIntrinsicBounds(imgbat, null, null, null);

    }


    protected void disableFCBBatteryStatus ()
    {

       mFCBBattery.setText(App.context.getString(R.string.empty_content));
       final Drawable imgbat = App.context.getResources().getDrawable(R.drawable.battery_gr_32x32);
       mFCBBattery.setCompoundDrawablesWithIntrinsicBounds(imgbat, null, null, null);

    }


    protected  Drawable getBatteryIcon (final double BatteryLevel, final boolean isCharching)
    {
        int imgID;

        if (andruavWe7da.getIsCGS())
        {
            imgID = R.drawable.battery_gr_32x32;
        }
        else {
            if (isCharching) {
                imgID = R.drawable.battery_bg_32x32;
            } else {
                if (BatteryLevel > 80) {
                    imgID = R.drawable.battery_g_32x32;
                } else if (BatteryLevel > 50) {
                    imgID = R.drawable.battery_rg_32x32;
                } else if (BatteryLevel > 25) {
                    imgID = R.drawable.battery_rg_3_32x32;
                } else {
                    imgID = R.drawable.battery_r_32x32;
                }
            }
        }

        return App.context.getResources().getDrawable(imgID);
    }

    protected Spanned getBatteryText (final AndruavBattery andruavBattery, final boolean isFCB)
    {

        final StringBuffer text = new StringBuffer();

        if (andruavWe7da.getIsCGS())
        {
            text.append(GUI.getFont(App.context.getString(R.string.str_TXT_BLUE), true, false)).append(" n/a").append("</b></font>");
        }
        else {
            if (isFCB == true)
            {
                text.append(GUI.getFont(App.context.getString(R.string.str_TXT_BLUE), true, false)).append(String.format("%3.0f", andruavBattery.FCB_BatteryRemaining) + "% ").append(String.format("%2.2f", andruavBattery.FCB_BatteryVoltage / 1000.0) + "v").append("</b></font>");
                double cur=andruavBattery.FCB_CurrentConsumed;
                if (andruavBattery.FCB_CurrentConsumed==-1)
                {
                    cur =0;
                }
                text.append("<br>").append(GUI.getFont(App.context.getString(R.string.str_TXT_BLUE), true, false)).append(String.format("%2.2f", cur / 1000.0f ) + " Ah").append("</b></font>");

            }
            else {
                text.append(GUI.getFont(App.context.getString(R.string.str_TXT_BLUE), true, false)).append(String.format("%3.0f", andruavBattery.BatteryLevel) + "% ").append(String.format("%2.2f", andruavBattery.Voltage / 1000.0) + "v").append("</b></font>");
                if (andruavBattery.Charging) {
                    text.append("<br>").append(GUI.getFont(App.context.getString(R.string.str_TXT_GREEN_DARKER), true, false)).append(App.context.getString(R.string.action_bat_charging));
                }
            }
        }
        return Html.fromHtml(text.toString());
    }

    protected  Drawable getUnitIcon () {

        int imgID;
        if (andruavWe7da ==null)
        {
            imgID = R.drawable.plane_gr_32x32;
        }
        else {
            if (andruavWe7da.getIsCGS()) {
                imgID = R.drawable.gcs_32x32;
            } else {
                switch (andruavWe7da.getVehicleType()) {
                    case VehicleTypes.VEHICLE_HELI:
                        imgID = R.drawable.heli_1_32x32;
                        break;
                    case VehicleTypes.VEHICLE_TRI:
                    case VehicleTypes.VEHICLE_QUAD:
                        imgID = R.drawable.drone_q1_32x32;
                        break;
                    case VehicleTypes.VEHICLE_ROVER:
                        imgID = R.drawable.car_3_32x32;
                        break;
                    case VehicleTypes.VEHICLE_PLANE:
                    case VehicleTypes.VEHICLE_UNKNOWN:
                    default:
                        imgID = R.drawable.plane_b_32x32;
                        break;

                }
            }
        }

        return App.context.getResources().getDrawable(imgID);
    }



    protected Spanned getGPSDescription()
    {
        final StringBuffer text = new StringBuffer();
        int icon = R.drawable.gps_no_32x32;

        if (andruavWe7da.getAvailableLocation() == null)
        {
            text.append(GUI.getFont(App.context.getString(R.string.str_TXT_GREY), false, false)).append("No GPS").append("</b></font>");
            mGPS.setCompoundDrawablesWithIntrinsicBounds(App.context.getResources().getDrawable(R.drawable.gps_no_32x32), null, null, null);

        }
        else
        {
            if (andruavWe7da.getActiveGPS().GPS3DFix == 1) {
                text.append(HtmlPro.AddLine(App.context.getString(R.string.str_TXT_ERROR), "No-Fix", false, false));
                icon = R.drawable.gps_bad_32x32;
            } else
            if (andruavWe7da.getActiveGPS().GPS3DFix == 2) {
                text.append(HtmlPro.AddLine(App.context.getString(R.string.str_TXT_GREEN_DARKER), "2D-Fix", false, false));
                icon = R.drawable.gps_nofix_32x32;
            } else
            if (andruavWe7da.getActiveGPS().GPS3DFix >= 3) {
                text.append(HtmlPro.AddLine(App.context.getString(R.string.str_TXT_BLUE_DARKEST), "3D-Fix", false, false));
                icon = R.drawable.gps_fix_32x32;
            }


            mGPS.setCompoundDrawablesWithIntrinsicBounds(App.context.getResources().getDrawable(icon), null, null, null);


            //if (mevent_IMU.getCurrentLocation().hasAccuracy()) {
            //    s = s + String.format(" %3.1f",mevent_IMU.getCurrentLocation().getAccuracy()) + " m";
           // }
        }

        double accuracy=0.0;
        String straccuracy="0.0";
        if ((andruavWe7da.getAvailableLocation()!=null) && (andruavWe7da.getAvailableLocation().hasAccuracy())) {
            accuracy = andruavWe7da.getAvailableLocation().getAccuracy();

            if (preferredUnits != Constants.Preferred_UNIT_METRIC_SYSTEM) {
                accuracy = accuracy * UnitConversion.MetersToFeet;
            }
        }

        if (preferredUnits != Constants.Preferred_UNIT_METRIC_SYSTEM) {
            straccuracy = String.format(" %3.1f",accuracy) + "ft ";
        }
        else
        {
            straccuracy = String.format(" %3.1f",accuracy) + "m ";
        }



        text.append(HtmlPro.AddLine(App.context.getString(R.string.str_TXT_GREEN_DARKER), straccuracy, false, false));
        text.append(HtmlPro.AddLine(App.context.getString(R.string.str_TXT_BLUE), "sat:", false, false));
        text.append(HtmlPro.AddLine(App.context.getString(R.string.str_TXT_GREEN_HUD_LAND), String.valueOf(andruavWe7da.getActiveIMU().SATC), false, false));


        if (andruavWe7da.getActiveGPS().getUseFCBIMU())
        {
            text.append(HtmlPro.AddLine(App.context.getString(R.string.str_TXT_ERROR), " board gps", true, false));
        }
        else
        {
            text.append(HtmlPro.AddLine(App.context.getString(R.string.str_TXT_ERROR), " mobile gps", true, false));
        }


        return Html.fromHtml(text.toString());
    }


    protected void updateGCSBlockedStatus()
    {
        if (andruavWe7da == null)
        {
            mMobileBattery.setVisibility(View.INVISIBLE);
            mFCBBattery.setVisibility(View.INVISIBLE);
            return ;
        }

        updateIMUStatus();
    }

    protected void updateBatteryStatus()
    {

        if (andruavWe7da == null)
        {
            mMobileBattery.setVisibility(View.INVISIBLE);
            mFCBBattery.setVisibility(View.INVISIBLE);
            return ;
        }

        mMobileBattery.setText(getBatteryText(andruavWe7da.LastEvent_Battery, false));
        mMobileBattery.setCompoundDrawablesWithIntrinsicBounds(getBatteryIcon(andruavWe7da.LastEvent_Battery.BatteryLevel, andruavWe7da.LastEvent_Battery.Charging), null, null, null);
        mMobileBattery.setVisibility(View.VISIBLE);

        if (andruavWe7da.useFCBIMU()) {

            mFCBBattery.setVisibility(View.VISIBLE);

            mFCBBattery.setCompoundDrawablesWithIntrinsicBounds(getBatteryIcon(andruavWe7da.LastEvent_Battery.FCB_BatteryRemaining, false), null, null, null);
            mFCBBattery.setText(getBatteryText(andruavWe7da.LastEvent_Battery, true));
            //mFCBBattery.setTextColor(0xffffff);

        }
        else
        {

            mFCBBattery.setVisibility(View.INVISIBLE);
            //disableFCBBatteryStatus();
        }

        useFCBIMU = andruavWe7da.useFCBIMU();

    }

    protected void updateGPSStatus()
    {

        if (andruavWe7da == null)
        {
            mGPS.setVisibility(View.INVISIBLE);

            //mGPS.setCompoundDrawablesWithIntrinsicBounds(App.context.getResources().getDrawable(R.drawable.gps_gr_32x32), null, null, null);
            //mGPS.setText(App.context.getString(R.string.empty_content));
            return ;
        }

        mGPS.setVisibility(View.VISIBLE);
        mGPS.setText(getGPSDescription());
    }

    protected void updateIMUStatus()
    {

        if (andruavWe7da == null)
        {
            mFCBMode.setVisibility(View.INVISIBLE);
            mIMUAltitude.setVisibility(View.INVISIBLE);
            return ;
        }


        final Location loc = andruavWe7da.getAvailableLocation();


        String speedText;
        String avgSpeed;
        String height;
        String flyingTime;
        float gps_speed;

        if (loc != null) {
            gps_speed = loc.getSpeed();
            if (preferredUnits == Constants.Preferred_UNIT_METRIC_SYSTEM) {
                speedText = String.format("%3.1f m/s", gps_speed);
                height = String.format("%6.0f m", andruavWe7da.getActiveIMU().getAltitude());
                // avgSpeed = avgSpeed + String.format("%3.1f m/s", mevent_IMU.GroundSpeed_avg );

            } else {
                speedText = (String.format("%3.1f mph", gps_speed * UnitConversion.Speed_MetersPerSecondToMilePerHour));
                height = String.format("%6.0f feet", andruavWe7da.getActiveIMU().getAltitude() * UnitConversion.MetersToFeet);
                // if (mevent_IMU != null) {
                //     avgSpeed = avgSpeed + String.format("%3.1f mph", mevent_IMU.GroundSpeed_avg * UnitConversion.Speed_MetersPerSecondToMilePerHour);
                //  }
            }
        }
        else
        {
            speedText = "na";
            height = "na";
        }


        // Altitude ActivityMosa3ed
        final StringBuffer textFCBMode = new StringBuffer();


        textFCBMode.append(GUI.getFont(App.context.getString(R.string.str_TXT_GREEN_DARKER), true, false)).append(FlightMode.getFlightModeText(andruavWe7da.getFlightModeFromBoard())).append("</b></font>");
        if (andruavWe7da.IsFlying())
        {
            flyingTime = GUI.elapsed2( System.currentTimeMillis(),andruavWe7da.getFlyingStartTime());
            textFCBMode.append(GUI.getFont(App.context.getString(R.string.str_TXT_BLUE), true, false)).append(" Flying " + flyingTime + "</font>");

        }
        if (andruavWe7da.IsArmed())
        {
            textFCBMode.append("<br>").append(GUI.getFont(App.context.getString(R.string.str_TXT_ERROR), true, false)).append("ARMED</b></font>");
        }
        if (andruavWe7da.getisGCSBlockedFromBoard())
        {
            textFCBMode.append("<br>").append(GUI.getFont(App.context.getString(R.string.str_TXT_ERROR), true, false)).append(" BLOCKED</b></font>");
        }

        textFCBMode.append("<br>").append(GUI.getFont(App.context.getString(R.string.str_TXT_GREEN_DARKER), true, false)).append(" </b></font>");

        mFCBMode.setText(Html.fromHtml(textFCBMode.toString()));
        mFCBMode.setVisibility(View.VISIBLE);

        final Drawable imgimu = App.context.getResources().getDrawable(R.drawable.speed_b_32x32);
        mFCBMode.setCompoundDrawablesWithIntrinsicBounds(imgimu, null, null, null);



       // textFCBMode.append(ActivityMosa3ed.getFont(App.context.getString(R.string.str_TXT_BLUE), true, false)).append("<br>alt: ").append("</b></font>");
       // textFCBMode.append(ActivityMosa3ed.getFont(App.context.getString(R.string.str_TXT_GREEN_DARKER), true, false)).append(height).append("</b></font>");
        // Speed & Altitude ActivityMosa3ed
        final StringBuffer textSpeedAlt = new StringBuffer();
        textSpeedAlt.append(GUI.getFont(App.context.getString(R.string.str_TXT_BLUE), true, false)).append("speed: ").append("</b></font>");
        textSpeedAlt.append(GUI.getFont(App.context.getString(R.string.str_TXT_GREEN_DARKER), true, false)).append(speedText).append("</b></font>");

        textSpeedAlt.append(GUI.getFont(App.context.getString(R.string.str_TXT_BLUE), true, false)).append("<br>alt: ").append("</b></font>");
        textSpeedAlt.append(GUI.getFont(App.context.getString(R.string.str_TXT_GREEN_DARKER), true, false)).append(height).append("</b></font>");
        mIMUAltitude.setText(Html.fromHtml(textSpeedAlt.toString()));
        mIMUAltitude.setVisibility(View.VISIBLE);
        final Drawable imgalt = App.context.getResources().getDrawable(R.drawable.height_b_32x32);
        mIMUAltitude.setCompoundDrawablesWithIntrinsicBounds(imgalt, null, null, null);


    }

    protected void updateInfoStatus()
    {
        final Drawable img = getUnitIcon();

        if (andruavWe7da == null)
        {
            mAndruavInfo.setVisibility(View.INVISIBLE);
        }
        else
        {
            mAndruavInfo.setVisibility(View.VISIBLE);
            mAndruavInfo.setText(GUI.getAndruavUnitColored(andruavWe7da));
        }

        mAndruavInfo.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);

    }


    private int exception_err_counter =0;

    protected void updateInfo ()
    {
        try {

        updateBatteryStatus();
        updateGPSStatus();
        updateIMUStatus();
        updateInfoStatus();
        }
        catch (Exception ex)
        {
            if (exception_err_counter > 5) return ;
            exception_err_counter += 1;
            AndruavEngine.log().logException("infowidget", ex);
        }
    }

    private void UIHandler () {
        mhandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);


                if (msg.obj instanceof _7adath_IMU_Ready) {
                    updateIMUStatus();
                    updateInfoStatus();
                    return;
                }

                if (msg.obj instanceof _7adath_GPS_Ready) {
                    updateGPSStatus();
                    return;
                }

                if (msg.obj instanceof _7adath_Battery_Ready) {
                    updateBatteryStatus();
                    return;
                }

                if (msg.obj instanceof _7adath_GCSBlockedChanged)
                {
                    updateGCSBlockedStatus();
                    return;
                }

                if (msg.obj instanceof _7adath_UnitShutDown)
                {
                    updateInfoStatus();
                    return;
                }


                if (msg.obj instanceof _7adath_Vehicle_Mode_Changed)
                {
                    updateInfoStatus();
                    return;
                }

                if (msg.obj instanceof _7adath_Vehicle_Flying_Changed)
                {
                    updateInfoStatus();
                    return;
                }


                if (msg.obj instanceof _7adath_FCB_Changed)
                {
                    updateInfoStatus();
                    updateIMUStatus();
                    return;
                }

            }
        };
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

        EventBus.getDefault().unregister(this);
        mhandler.removeCallbacksAndMessages(null);
        mhandler = null;

    }


}
