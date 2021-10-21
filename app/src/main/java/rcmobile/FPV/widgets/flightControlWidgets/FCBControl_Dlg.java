package rcmobile.FPV.widgets.flightControlWidgets;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.AppBarLayout;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.andruav.AndruavFCBControlFacade;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.Constants;
import com.andruav.event.droneReport_7adath._7adath_TRK_OnOff_Changed;
import com.andruav.event.droneReport_7adath._7adath_Vehicle_ARM_Changed;
import com.andruav.event.droneReport_7adath._7adath_Vehicle_Mode_Changed;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.controlBoard.shared.common.VehicleTypes;
import com.vi.swipenumberpicker.OnValueChangeListener;
import com.vi.swipenumberpicker.SwipeNumberPicker;

import de.greenrobot.event.EventBus;
import rcmobile.FPV.R;

import rcmobile.andruavmiddlelibrary.factory.math.UnitConversion;
import rcmobile.andruavmiddlelibrary.preference.Preference;

/**
 * Created by mhefny on 12/25/16.
 */

public class FCBControl_Dlg extends DialogFragment {

    private final FCBControl_Dlg Me;

    private AndruavUnitBase andruavWe7da;

    private SwipeNumberPicker climbAltitude;
    private SwipeNumberPicker loiterRadius;
    private TextView txtTitle;
    private Button mbtnArm;
    private Button mbtnClimb;
    private Button mbtnLand;
    private Button mbtnManual;
    private Button mbtnGuided;
    private Button mbtnLoiter;
    private Button mbtnBrake;
    private Button mbtnHold;
    private Button mbtnAuto;
    private Button mbtnRTL;
    private Button mbtnSRTL;
    private Button mbtnCruise;
    private Button mbtnFBWA;

    private final Handler mHandler;
    //////////BUS EVENT

    public void onEvent(final _7adath_Vehicle_ARM_Changed a7adath_vehicle_arm_changed) {
        if ((andruavWe7da== null) || (!andruavWe7da.equals(a7adath_vehicle_arm_changed.mAndruavWe7da))) return ;

        final Message msg = mHandler.obtainMessage();
        msg.obj = a7adath_vehicle_arm_changed;

        mHandler.sendMessage(msg);
    }


    public void onEvent (final _7adath_TRK_OnOff_Changed a7adath_trk_onOff_changed)
    {
        if ((andruavWe7da== null) || (!andruavWe7da.equals(a7adath_trk_onOff_changed.mAndruavWe7da))) return ;

        final Message msg = mHandler.obtainMessage();
        msg.obj = a7adath_trk_onOff_changed;

        mHandler.sendMessage(msg);
    }

    public void onEvent(final _7adath_Vehicle_Mode_Changed a7adath_vehicle_mode_changed) {
        if ((andruavWe7da== null) || (!andruavWe7da.equals(a7adath_vehicle_mode_changed.mAndruavWe7da))) return ;

        final Message msg = mHandler.obtainMessage();
        msg.obj = a7adath_vehicle_mode_changed;

        mHandler.sendMessage(msg);
    }



    public FCBControl_Dlg() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
        Me = this;
        mHandler =  new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
/*
                if (_7adath_Vehicle_ARM_Changed.class.isInstance(msg.obj)) {

                }
                else if (_7adath_Vehicle_Mode_Changed.class.isInstance(msg.obj)) {

                }
*/
                handleGUI ();

            }
        };

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        //Dialog dialog = super.onCreateDialog(savedInstanceState);
        Dialog dialog = new Dialog(getActivity(), R.style.DialogFragmentStyle);

        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        int width = AppBarLayout.LayoutParams.WRAP_CONTENT;
        int height = AppBarLayout.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setLayout(width, height);

        return dialog; //new Dialog(getActivity(), R.style.FullHeightDialog);
    }

    public static FCBControl_Dlg newInstance(AndruavUnitBase andruavWe7da) {
        FCBControl_Dlg frag = new FCBControl_Dlg();
        frag.setAndruavWe7da(andruavWe7da);
        final Bundle args = new Bundle();
        args.putString("title", andruavWe7da.UnitID);
        frag.setArguments(args);
        return frag;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.modal_dialog_fcb_control, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mbtnArm     = view.findViewById(R.id.mdlgfcbctrl_btn_arm);
        mbtnArm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFCBControlFacade.do_Arm(!andruavWe7da.IsArmed(), false , andruavWe7da);

            }
        });
        mbtnClimb   = view.findViewById(R.id.mdlgfcbctrl_btn_climb);
        mbtnClimb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFCBControlFacade.do_ChangeAltitude(Preference.getDefaultClimbAlt(null), andruavWe7da);

            }
        });

        mbtnLand    = view.findViewById(R.id.mdlgfcbctrl_btn_land);
        mbtnLand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFCBControlFacade.do_Land(andruavWe7da);

            }
        });

        mbtnLoiter  = view.findViewById(R.id.mdlgfcbctrl_btn_loiter);
        mbtnLoiter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFCBControlFacade.do_FlightMode(FlightMode.CONST_FLIGHT_CONTROL_LOITER,Preference.getDefaultCircleRadius(null), andruavWe7da);

            }
        });

        mbtnBrake = view.findViewById(R.id.mdlgfcbctrl_btn_brake);
        mbtnBrake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFCBControlFacade.do_FlightMode(FlightMode.CONST_FLIGHT_CONTROL_BRAKE, andruavWe7da);

            }
        });

        mbtnHold = view.findViewById(R.id.mdlgfcbctrl_btn_hold);
        mbtnHold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFCBControlFacade.do_FlightMode(FlightMode.CONST_FLIGHT_CONTROL_HOLD, andruavWe7da);

            }
        });



        mbtnManual  = view.findViewById(R.id.mdlgfcbctrl_btn_manual);
        mbtnManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFCBControlFacade.do_FlightMode(FlightMode.CONST_FLIGHT_CONTROL_MANUAL, andruavWe7da);

            }
        });


        mbtnGuided  = view.findViewById(R.id.mdlgfcbctrl_btn_guided);
        mbtnGuided.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFCBControlFacade.do_FlightMode(FlightMode.CONST_FLIGHT_CONTROL_GUIDED, andruavWe7da);

            }
        });

        mbtnAuto    = view.findViewById(R.id.mdlgfcbctrl_btn_auto);
        mbtnAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFCBControlFacade.do_FlightMode(FlightMode.CONST_FLIGHT_CONTROL_AUTO, andruavWe7da);
            }
        });

        mbtnRTL     = view.findViewById(R.id.mdlgfcbctrl_btn_rtl);
        mbtnRTL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFCBControlFacade.do_FlightMode(FlightMode.CONST_FLIGHT_CONTROL_RTL, andruavWe7da);

            }
        });


        mbtnSRTL     = view.findViewById(R.id.mdlgfcbctrl_btn_smart_rtl);
        mbtnSRTL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFCBControlFacade.do_FlightMode(FlightMode.CONST_FLIGHT_CONTROL_SMART_RTL, andruavWe7da);

            }
        });


        mbtnFBWA    = view.findViewById(R.id.mdlgfcbctrl_btn_FBWA);
        mbtnFBWA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFCBControlFacade.do_FlightMode(FlightMode.CONST_FLIGHT_CONTROL_FBWA, andruavWe7da);

            }
        });

        mbtnCruise  = view.findViewById(R.id.mdlgfcbctrl_btn_cruise);
        mbtnCruise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFCBControlFacade.do_FlightMode(FlightMode.CONST_FLIGHT_CONTROL_CRUISE, andruavWe7da);

            }
        });


        climbAltitude = view.findViewById(R.id.action_fcbctrl_cardwheel_alt);
        int alt = Preference.getDefaultClimbAlt(null);
        if (Preference.getPreferredUnits(null) == Constants.Preferred_UNIT_IMPERIAL_SYSTEM) {
            alt = (int) Math.ceil(alt * UnitConversion.MetersToFeet);
        }
        climbAltitude.setValue(alt,false);
        climbAltitude.setOnValueChangeListener(new OnValueChangeListener() {
            @Override
            public boolean onValueChange(SwipeNumberPicker view, int oldValue, int newValue) {
                int meterValue = newValue;
                if (Preference.getPreferredUnits(null) != Constants.Preferred_UNIT_IMPERIAL_SYSTEM) {
                    meterValue = (int) Math.ceil(meterValue * UnitConversion.FeetToMeters);
                }
                Preference.setDefaultClimbAlt(null,meterValue);
                return true;
            }
        });

        loiterRadius = view.findViewById(R.id.action_fcbctrl_cardwheel_diameter);

        int circle = Preference.getDefaultCircleRadius(null);
        if (Preference.getPreferredUnits(null) == Constants.Preferred_UNIT_IMPERIAL_SYSTEM) {
            circle = (int) Math.ceil(circle * UnitConversion.MetersToFeet);
        }

        loiterRadius.setValue(circle,false);
        loiterRadius.setOnValueChangeListener(new OnValueChangeListener() {
            @Override
            public boolean onValueChange(SwipeNumberPicker view, int oldValue, int newValue) {
                int meterValue = newValue;
                if (Preference.getPreferredUnits(null) != Constants.Preferred_UNIT_IMPERIAL_SYSTEM) {
                    meterValue = (int) Math.ceil(meterValue * UnitConversion.FeetToMeters);
                }
                Preference.setDefaultCircleRadius(null,meterValue);
                return true;
            }
        });

        txtTitle = view.findViewById(R.id.mdlgfcbctrl_txt_title);
        txtTitle.setText(andruavWe7da.UnitID + " ctrl");
        handleGUI();


    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        EventBus.getDefault().register(this);
    }


    @Override
    public void onDetach() {
        super.onDetach();

        EventBus.getDefault().unregister(this);
    }


    private void handleGUI ()
    {
        final int flightmode = andruavWe7da.getFlightModeFromBoard();
        if (andruavWe7da.IsArmed())
        {
            mbtnClimb.setVisibility(View.VISIBLE);
            mbtnManual.setVisibility(View.VISIBLE);
            mbtnLand.setVisibility(View.VISIBLE);
            mbtnLoiter.setVisibility(View.VISIBLE);
            mbtnGuided.setVisibility(View.VISIBLE);
            mbtnAuto.setVisibility(View.VISIBLE);
            mbtnRTL.setVisibility(View.VISIBLE);
            mbtnSRTL.setVisibility(View.VISIBLE);
            mbtnCruise.setVisibility(View.VISIBLE);
            mbtnFBWA.setVisibility(View.VISIBLE);
        }
        else
        {
            mbtnClimb.setVisibility(View.GONE);
            mbtnManual.setVisibility(View.VISIBLE);
            mbtnGuided.setVisibility(View.VISIBLE);
            mbtnRTL.setVisibility(View.GONE);
            mbtnLand.setVisibility(View.GONE);
            mbtnLoiter.setVisibility(View.GONE);
            mbtnAuto.setVisibility(View.VISIBLE);
            mbtnCruise.setVisibility(View.VISIBLE);
            mbtnFBWA.setVisibility(View.VISIBLE);
        }


        mbtnArm.setPressed(andruavWe7da.IsArmed());
        mbtnGuided.setPressed((flightmode == FlightMode.CONST_FLIGHT_CONTROL_GUIDED));
        mbtnRTL.setPressed((flightmode == FlightMode.CONST_FLIGHT_CONTROL_RTL));
        mbtnSRTL.setPressed((flightmode == FlightMode.CONST_FLIGHT_CONTROL_SMART_RTL));
        mbtnLand.setPressed((flightmode == FlightMode.CONST_FLIGHT_CONTROL_LAND));
        mbtnLoiter.setPressed((flightmode == FlightMode.CONST_FLIGHT_CONTROL_LOITER));
        mbtnBrake.setPressed((flightmode == FlightMode.CONST_FLIGHT_CONTROL_BRAKE));
        mbtnHold.setPressed((flightmode == FlightMode.CONST_FLIGHT_CONTROL_HOLD));
        mbtnAuto.setPressed((flightmode == FlightMode.CONST_FLIGHT_CONTROL_AUTO));
        mbtnManual.setPressed((flightmode == FlightMode.CONST_FLIGHT_CONTROL_MANUAL));
        mbtnCruise.setPressed((flightmode == FlightMode.CONST_FLIGHT_CONTROL_CRUISE));
        mbtnFBWA.setPressed((flightmode == FlightMode.CONST_FLIGHT_CONTROL_FBWA));

        switch (andruavWe7da.getVehicleType())
        {
            case VehicleTypes.VEHICLE_PLANE:
                mbtnBrake.setVisibility(View.GONE);
                mbtnHold.setVisibility(View.GONE);
                break;
            case VehicleTypes.VEHICLE_ROVER:
                mbtnLand.setVisibility(View.GONE);
                mbtnCruise.setVisibility(View.GONE);
                mbtnBrake.setVisibility(View.GONE);
                mbtnFBWA.setVisibility(View.GONE);
                break;
            case VehicleTypes.VEHICLE_TRI:
            case VehicleTypes.VEHICLE_QUAD:
                mbtnManual.setVisibility(View.GONE);
                mbtnCruise.setVisibility(View.GONE);
                mbtnHold.setVisibility(View.GONE);
                mbtnFBWA.setVisibility(View.GONE);
                break;
        }
    }


    private void setAndruavWe7da (final AndruavUnitBase andruavWe7da)
    {
        this.andruavWe7da = andruavWe7da;
    }
}
