package ap.andruav_ap.activities.phone;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import ap.andruav_ap.R;

/***
 * AN-0134 (TASK-113): GPS tab placeholder - TASK-115 fills it with the
 * phone fix status and the GPS injection toggles.
 */
public class PhoneGpsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final TextView placeholder = new TextView(getContext());
        placeholder.setText(R.string.phone_coming_soon);
        placeholder.setTextColor(ContextCompat.getColor(requireContext(), R.color.home_text_dim));
        placeholder.setTextSize(13);
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return placeholder;
    }
}
