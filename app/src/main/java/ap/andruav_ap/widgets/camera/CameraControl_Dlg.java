package ap.andruav_ap.widgets.camera;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.andruav.AndruavFacade;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.vi.swipenumberpicker.OnValueChangeListener;
import com.vi.swipenumberpicker.SwipeNumberPicker;

import ap.andruav_ap.R;


/**
 * Created by mhefny on 10/6/16.
 */

public class CameraControl_Dlg extends DialogFragment {

    private final CameraControl_Dlg Me;
    private SwipeNumberPicker imageInterval;
    private SwipeNumberPicker imageTotal;
    private Button imageShoot;
    private Button cameraSwap;

    private AndruavUnitShadow andruavWe7da;

    public CameraControl_Dlg() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
        Me = this;

    }

    public static CameraControl_Dlg newInstance(String title) {
        CameraControl_Dlg frag = new CameraControl_Dlg();

        final Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.modal_dialog_camera, container);
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
        cameraSwap = view.findViewById(R.id.mdlgcam_btn_swap);
        cameraSwap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFacade.switchCamera(Me.andruavWe7da,Me.andruavWe7da.PartyID);
            }
        });
        imageShoot = view.findViewById(R.id.mdlgcam_btn_shoot);
        imageShoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndruavFacade.takeImage(Me.andruavWe7da.imageTotal,Me.andruavWe7da.imageInterval,true,Me.andruavWe7da);
                Me.dismiss();
            }
        });
        imageInterval  = view.findViewById(R.id.mdlgcam__cardwheel_image_interval);
        imageInterval.setValue(Me.andruavWe7da.imageInterval,false);
        imageInterval.setOnValueChangeListener(new OnValueChangeListener() {
            @Override
            public boolean onValueChange(SwipeNumberPicker view, int oldValue, int newValue) {
                Me.andruavWe7da.imageInterval = newValue;
                return true;
            }
        });
        imageTotal = view.findViewById(R.id.mdlgcam__cardwheel_image_total);
        imageTotal.setValue(Me.andruavWe7da.imageTotal,false);
        imageTotal.setOnValueChangeListener(new OnValueChangeListener() {
            @Override
            public boolean onValueChange(SwipeNumberPicker view, int oldValue, int newValue) {
                Me.andruavWe7da.imageTotal = newValue;
                return true;
            }
        });


        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }


    public void setAndruavWe7da (final AndruavUnitShadow andruavWe7da)
    {
        this.andruavWe7da = andruavWe7da;
    }
}
