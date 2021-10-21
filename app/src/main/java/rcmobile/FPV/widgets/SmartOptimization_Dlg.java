package rcmobile.FPV.widgets;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.andruav.AndruavFacade;
import com.andruav.AndruavSettings;

import rcmobile.FPV.App;
import rcmobile.FPV.R;
import rcmobile.andruavmiddlelibrary.preference.Preference;

/**
 * Created by mhefny on 3/29/17.
 */

public class SmartOptimization_Dlg extends DialogFragment {

    private static final CharSequence[] smartTeleOptimizationItems = {"OFF", "Lvl 1 - lots of data", "Lvl 2 - best option", "Lvl 3 - small data"};

    private final SmartOptimization_Dlg Me;
    private Spinner spinOptimizationLevel;

    public SmartOptimization_Dlg() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
        Me = this;

    }

    public static SmartOptimization_Dlg newInstance(String title) {
        SmartOptimization_Dlg frag = new SmartOptimization_Dlg();

        final Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.modal_dialog_smarttelemetry, container);

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String title = getArguments().getString("title", "Enter Name");
        getDialog().setTitle(title);


        spinOptimizationLevel = view.findViewById(R.id.smartdlg_lbSmartTelemetry);
        final ArrayAdapter vt = new ArrayAdapter(App.activeActivity, android.R.layout.simple_spinner_item , smartTeleOptimizationItems);
        spinOptimizationLevel.setAdapter(vt);
        spinOptimizationLevel.setSelection(Preference.getSmartMavlinkTelemetry(null));

        spinOptimizationLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Preference.setSmartMavlinkTelemetry(null,position);
                if (AndruavSettings.andruavWe7daBase.getIsCGS())
                {
                    if (AndruavSettings.remoteTelemetryAndruavWe7da != null)
                    {
                        AndruavFacade.ResumeTelemetry(Preference.getSmartMavlinkTelemetry(null));
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Preference.setSmartMavlinkTelemetry(null, Preference.getSmartMavlinkTelemetry(null));
            }
        });


    }


    }
