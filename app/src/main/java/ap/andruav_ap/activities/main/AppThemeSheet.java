package ap.andruav_ap.activities.main;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import ap.andruav_ap.R;
import ap.andruav_ap.activities.AppThemes;
import ap.andruav_ap.activities.ThemeColors;

/**
 * Theme selector sheet (AN-0147): Dark, Light, Sun or Follow system, in
 * the FCB-sheet style. A tap stores the choice in pref_app_theme, then
 * recreates the open activity so the switch needs no app restart.
 */
public class AppThemeSheet extends BottomSheetDialogFragment {

    public static final String TAG = "AppThemeSheet";

    public static AppThemeSheet newInstance() {
        return new AppThemeSheet();
    }

    private View mDotDark;
    private View mDotLight;
    private View mDotSun;
    private View mDotSystem;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_app_theme, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDotDark = view.findViewById(R.id.theme_dot_dark);
        mDotLight = view.findViewById(R.id.theme_dot_light);
        mDotSun = view.findViewById(R.id.theme_dot_sun);
        mDotSystem = view.findViewById(R.id.theme_dot_system);

        view.findViewById(R.id.theme_row_dark).setOnClickListener(v -> onPick(AppThemes.VALUE_DARK));
        view.findViewById(R.id.theme_row_light).setOnClickListener(v -> onPick(AppThemes.VALUE_LIGHT));
        view.findViewById(R.id.theme_row_sun).setOnClickListener(v -> onPick(AppThemes.VALUE_SUN));
        view.findViewById(R.id.theme_row_system).setOnClickListener(v -> onPick(AppThemes.VALUE_SYSTEM));

        tintDots();
    }

    private void onPick(final String value) {
        AppThemes.set(requireContext(), value);
        dismiss();
        requireActivity().recreate();
    }

    private void tintDots() {
        final String current = AppThemes.get(requireContext());
        tintDot(mDotDark, AppThemes.VALUE_DARK.equals(current));
        tintDot(mDotLight, AppThemes.VALUE_LIGHT.equals(current));
        tintDot(mDotSun, AppThemes.VALUE_SUN.equals(current));
        tintDot(mDotSystem, AppThemes.VALUE_SYSTEM.equals(current));
    }

    private void tintDot(final View dot, final boolean selected) {
        setColor(dot, ThemeColors.get(requireContext(),
                selected ? R.attr.deAccent : R.attr.deBorderInput));
    }

    private void setColor(final View view, @ColorInt final int color) {
        final Drawable bg = view.getBackground();
        if (bg instanceof GradientDrawable) {
            ((GradientDrawable) bg.mutate()).setColor(color);
        }
    }
}
