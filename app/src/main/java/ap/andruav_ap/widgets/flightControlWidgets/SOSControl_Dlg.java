package ap.andruav_ap.widgets.flightControlWidgets;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.AppBarLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import androidx.fragment.app.DialogFragment;

import com.andruav.AndruavFacade;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.droneReport_7adath._7adath_Emergency_Changed;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.R;


/**
 * Created by mhefny on 1/23/17.
 */

public class SOSControl_Dlg extends DialogFragment {

    private final SOSControl_Dlg Me;



    private AndruavUnitBase andruavWe7da;

    private Button mbtnSMS;
    private Button mbtnWhistle;
    private Button mbtnFlash;


    private final Handler mHandler;



    public static SOSControl_Dlg newInstance(final AndruavUnitBase andruavWe7da) {
        SOSControl_Dlg frag = new SOSControl_Dlg();
        frag.setAndruavWe7da(andruavWe7da);
        final Bundle args = new Bundle();
        args.putString("title", andruavWe7da.UnitID);
        frag.setArguments(args);
        return frag;
    }



    public void onEvent (_7adath_Emergency_Changed a7adath_emergency_changed)
    {
        if ((andruavWe7da== null) || (!andruavWe7da.equals(a7adath_emergency_changed.mAndruavUnitBase))) return ;

        final Message msg = mHandler.obtainMessage();
        mHandler.sendMessage(msg);
    }

    public SOSControl_Dlg() {
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
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        int width = AppBarLayout.LayoutParams.WRAP_CONTENT;
        int height = AppBarLayout.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setLayout(width, height);

        return dialog; //new Dialog(getActivity(), R.style.FullHeightDialog);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.modal_dialog_sos, container);
    }



    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mbtnSMS = view.findViewById(R.id.mdlgsos_btn_sms);
        mbtnSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFacade.sendSMS (andruavWe7da);
            }
        });

        mbtnWhistle = view.findViewById(R.id.mdlgsos_btn_whistle);
        mbtnWhistle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFacade.makeWhisle (andruavWe7da);
            }
        });

        mbtnFlash= view.findViewById(R.id.mdlgsos_btn_flash);
        mbtnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFacade.makeFlash (andruavWe7da);
            }
        });
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



    private void handleGUI () {

        boolean enable = true;

        if (andruavWe7da == null)
         {
             mbtnFlash.setEnabled(false);
             mbtnWhistle.setEnabled(false);
             mbtnSMS.setEnabled(false);

             return;
         }

        mbtnFlash.setEnabled(!andruavWe7da.getIsShutdown());
        mbtnWhistle.setEnabled(!andruavWe7da.getIsShutdown());
        mbtnFlash.setPressed(andruavWe7da.getIsFlashing() && (!andruavWe7da.getIsShutdown()));
        mbtnWhistle.setPressed(andruavWe7da.getIsWhisling() && (!andruavWe7da.getIsShutdown()));

        mbtnSMS.setEnabled(!andruavWe7da.getIsShutdown());

    }


    private void setAndruavWe7da (final AndruavUnitBase andruavWe7da)
    {
        this.andruavWe7da = andruavWe7da;
    }


}
