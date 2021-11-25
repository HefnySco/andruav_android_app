package ap.andruav_ap.widgets.mohemma;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ap.andruav_ap.R;

/**
 * Created by mhefny on 4/5/16.
 */
public class TakeoffFragment extends Fragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup vg,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_editor_mission_takeoff, vg, false);
    }

}