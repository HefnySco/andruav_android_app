package rcmobile.FPV.widgets.mohemma;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.andruav.Constants;
import com.andruav.controlBoard.shared.missions.WayPointStep;
import com.vi.swipenumberpicker.OnValueChangeListener;
import com.vi.swipenumberpicker.SwipeNumberPicker;

import rcmobile.FPV.R;
import rcmobile.FPV.activities.map.MarkerWaypoint;
import rcmobile.FPV.widgets.CardWheelWidget;
import com.andruav.FeatureSwitch;
import rcmobile.andruavmiddlelibrary.factory.math.UnitConversion;
import rcmobile.andruavmiddlelibrary.preference.Preference;

/**
 * Created by mhefny on 4/5/16.
 */
public class WayPointFragment extends Fragment {

    private View Me;
    private WayPointStep mWayPointStep;

    private TextView missionNumber;
    private CardWheelWidget altitudeAttribute;
    private CardWheelWidget delayAttribute;

    private final int preferredUnit = Preference.getPreferredUnits(null);


    private void initGUI()
    {

        missionNumber = Me.findViewById(R.id.mission_waypoint_number);
        altitudeAttribute = Me.findViewById(R.id.mission_waypoint_altitude);
        delayAttribute = Me.findViewById(R.id.mission_waypoint_timetostay);


        altitudeAttribute.setOnValueChangeListener(new OnValueChangeListener() {
            @Override
            public boolean onValueChange(SwipeNumberPicker view, int oldValue, int newValue) {
                if ((newValue < 0) || (newValue > FeatureSwitch.Default_MAX_ALTITUDE))
                {
                    return false;
                }
                if ( preferredUnit == Constants.Preferred_UNIT_METRIC_SYSTEM) {
                    mWayPointStep.Altitude = newValue;
                }
                else
                {
                    mWayPointStep.Altitude = newValue * UnitConversion.FeetToMeters;
                }

                return true;
            }
        });

        delayAttribute.setAttributeValue(0,FeatureSwitch.Default_MAX_Delay,(int)mWayPointStep.TimeToStay);
        delayAttribute.setOnValueChangeListener(new OnValueChangeListener() {
            @Override
            public boolean onValueChange(SwipeNumberPicker view, int oldValue, int newValue) {
                if ((newValue < 0) || (newValue > FeatureSwitch.Default_MAX_Delay))
                {
                    return false;
                }
                mWayPointStep.TimeToStay = newValue;
                return true;
            }
        });

        missionNumber.setText(String.valueOf(mWayPointStep.Sequence));

        final String altitude =getString(R.string.gen_altitude);
        double altitudevalue = mWayPointStep.Altitude;
        if ( preferredUnit == Constants.Preferred_UNIT_METRIC_SYSTEM) {
            altitudeAttribute.setAttributeName(altitude+ " (m)");
        }
        else
        {
            altitudeAttribute.setAttributeName(altitude+ " (ft)");
            altitudevalue =  UnitConversion.MetersToFeet;
        }


        altitudeAttribute.setAttributeValue(0,FeatureSwitch.Default_MAX_ALTITUDE,(int)altitudevalue);

        delayAttribute.setAttributeName(R.string.gen_delay);
        delayAttribute.setAttributeValue(0,FeatureSwitch.Default_MAX_Delay,(int)mWayPointStep.TimeToStay);



    }

    public WayPointFragment()
    {
        super();
    }

    public void setMarkerWaypoint (MarkerWaypoint wayPointStep)
    {

        mWayPointStep = (WayPointStep) wayPointStep.wayPointStep;

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup vg,
                             Bundle savedInstanceState) {
        Me =  inflater.inflate(R.layout.fragment_editor_mission_waypoint, vg, false);

        return Me;
    }


    @Override
    public void onStart()
    {
        super.onStart();

        //http://stackoverflow.com/questions/13303469/edittext-settext-not-working-with-fragment
        // BUG: if you move this to onCreateView you will find text bix is corrupted.
        initGUI();



    }


}
