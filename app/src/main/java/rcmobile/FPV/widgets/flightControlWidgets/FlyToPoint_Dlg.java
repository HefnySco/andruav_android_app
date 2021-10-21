package rcmobile.FPV.widgets.flightControlWidgets;

import android.app.Activity;
import android.app.Dialog;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.AppBarLayout;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.andruav.AndruavFCBControlFacade;
import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.andruavUnit.AndruavUnitMapBase;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.util.AndruavLatLngAlt;

import java.util.ArrayList;
import java.util.List;

import rcmobile.FPV.App;
import rcmobile.FPV.R;
import rcmobile.FPV.widgets.flytopoint_sliding.DroneFlyToPointUnitItem;
import rcmobile.FPV.widgets.flytopoint_sliding.DroneFlyToPointUnitList;

/**
 * Created by mhefny on 12/27/16.
 */

public class FlyToPoint_Dlg extends DialogFragment {

    private final FlyToPoint_Dlg Me;

    private Button btnApply;
    private DroneFlyToPointUnitList droneFlyToPointUnitList;
    private final List<DroneFlyToPointUnitItem> mDroneFlyToPointUnitItemArray = new ArrayList<>();
    private AndruavLatLngAlt mLatLngAltDestination;

    public FlyToPoint_Dlg() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
        Me = this;
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

    public static FlyToPoint_Dlg newInstance(final AndruavLatLngAlt andruavLatLngAlt) {
        FlyToPoint_Dlg frag = new FlyToPoint_Dlg();
        frag.setDestinationPoint (andruavLatLngAlt);
        final Bundle args = new Bundle();

        frag.setArguments(args);
        return frag;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.modal_dialog_flytopoint_control, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (view.isInEditMode()) return;

        btnApply = view.findViewById(R.id.mdlgflytopoint_btn_apply);
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int childcount = mDroneFlyToPointUnitItemArray.size();
                for (int i = 0; i < childcount; i++) {
                    final DroneFlyToPointUnitItem droneFlyToPointUnitItem = mDroneFlyToPointUnitItemArray.get(i);
                    if (droneFlyToPointUnitItem.getGotoPoint())
                    {
                        final AndruavUnitBase andruavUnitBase = droneFlyToPointUnitItem.getUnit();
                        final Location location = andruavUnitBase.getAvailableLocation();
                        if (location!= null) {
                            AndruavFCBControlFacade.do_FlyToPoint(mLatLngAltDestination.getLatitude(), mLatLngAltDestination.getLongitude(), location.getAltitude(), andruavUnitBase);
                        }
                    }
                }
            }
        });

        droneFlyToPointUnitList = view.findViewById(R.id.mdlgflytopoint_lst_units);

        fillListView ();


    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        //EventBus.getDefault().register(this);
    }


    @Override
    public void onDetach() {
        super.onDetach();

        //EventBus.getDefault().unregister(this);;
    }



    private void setDestinationPoint (final AndruavLatLngAlt andruavLatLngAltDestination)
    {
        mLatLngAltDestination = andruavLatLngAltDestination;
    }
    private void fillListView()
    {

        final AndruavUnitMapBase andruavUnitMapBase = AndruavEngine.getAndruavWe7daMapBase();
        final int s = andruavUnitMapBase.size();
        for (int i=0;i<s;++i)
        {
            final AndruavUnitShadow andruavWe7da = (AndruavUnitShadow) andruavUnitMapBase.valueAt(i);
            if ((!andruavWe7da.getIsCGS()) && (andruavWe7da.useFCBIMU()) && (andruavWe7da.getFlightModeFromBoard() == FlightMode.CONST_FLIGHT_CONTROL_GUIDED))
            {
                DroneFlyToPointUnitItem droneFlyToPointUnit = new DroneFlyToPointUnitItem(App.getAppContext(), andruavWe7da);
                mDroneFlyToPointUnitItemArray.add(droneFlyToPointUnit);
                droneFlyToPointUnitList.addView(droneFlyToPointUnit);
            }
        }
    }

}
