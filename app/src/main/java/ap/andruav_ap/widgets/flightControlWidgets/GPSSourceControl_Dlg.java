package ap.andruav_ap.widgets.flightControlWidgets;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.AppBarLayout;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import com.andruav.AndruavFacade;
import com.andruav.andruavUnit.AndruavUnitBase;

import ap.andruav_ap.R;



/**
 * Created by mhefny on 12/2/16.
 */

public class GPSSourceControl_Dlg extends DialogFragment {

    private final GPSSourceControl_Dlg Me;

    private int gpsMode;
    private  boolean isVisible = false;

    private RadioButton rb_GPS_Auto;
    private RadioButton rb_GPS_Mobile;
    private RadioButton rb_GPS_FCB;
    private TextView txt_Description;
    private TextView txtTitle;
    private Button btnApply;

    private static AndruavUnitBase mAndruavWe7da;


    public static GPSSourceControl_Dlg newInstance(AndruavUnitBase andruavWe7da) {
        GPSSourceControl_Dlg frag = new GPSSourceControl_Dlg();

        mAndruavWe7da = andruavWe7da;
        final Bundle args = new Bundle();
        args.putString("title", "GPS Source");
        frag.setArguments(args);
        return frag;
    }


    public GPSSourceControl_Dlg() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
        Me = this;

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.modal_dialog_gpssource, container);
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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get field from view
        // Fetch arguments from bundle and set title
        String title = getArguments().getString("title", "Enter Name");
        getDialog().setTitle(title);
        // Show soft keyboard automatically and request focus to field
        //mEditText.requestFocus();
        rb_GPS_Auto = view.findViewById(R.id.mdlggps_auto);
        rb_GPS_Mobile = view.findViewById(R.id.mdlggps_mobile);
        rb_GPS_FCB = view.findViewById(R.id.mdlggps_fcb);

        rb_GPS_Auto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gpsMode = AndruavUnitBase.GPS_MODE_AUTO;
                    Me.gui_adjustRadios();
                }});
        rb_GPS_Mobile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gpsMode = AndruavUnitBase.GPS_MODE_MOBILE;
                    Me.gui_adjustRadios();
                }});
        rb_GPS_FCB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpsMode = AndruavUnitBase.GPS_MODE_FCB;
                Me.gui_adjustRadios();
            }});

        txt_Description = view.findViewById(R.id.mdlggps_desc);
        txtTitle = view.findViewById(R.id.mdlggps_txt_title);

        txtTitle.setText(mAndruavWe7da.UnitID + " - GPS");
        btnApply = view.findViewById(R.id.mdlggps_btn_apply);

        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);



        btnApply.setVisibility(View.INVISIBLE);
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFacade.setGPSMode(mAndruavWe7da,gpsMode);
                Me.dismiss();
            }
        });


        isVisible = true;
        gpsMode = mAndruavWe7da.getGPSMode();

        gui_adjustRadios();
    }


    private void gui_adjustRadios ()
    {
        if (!isVisible) return;

       if (mAndruavWe7da != null)
        {

            switch (gpsMode)
            {
                case AndruavUnitBase.GPS_MODE_AUTO:
                    rb_GPS_Auto.setChecked(true);
                    rb_GPS_Mobile.setChecked(false);
                    rb_GPS_FCB.setChecked(false);
                    txt_Description.setText(getString(R.string.gen_GPS_auto_desc));
                    break;
                case AndruavUnitBase.GPS_MODE_MOBILE:
                    rb_GPS_Auto.setChecked(false);
                    rb_GPS_Mobile.setChecked(true);
                    rb_GPS_FCB.setChecked(false);
                    txt_Description.setText(getString(R.string.gen_GPS_mobile_desc));
                    break;
                case AndruavUnitBase.GPS_MODE_FCB:
                    rb_GPS_Auto.setChecked(false);
                    rb_GPS_Mobile.setChecked(false);
                    rb_GPS_FCB.setChecked(true);
                    txt_Description.setText(getString(R.string.gen_GPS_fcb_desc));
                    break;
            }

            btnApply.setVisibility(View.VISIBLE);


        }

    }
}
