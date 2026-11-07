package ap.andruav_ap.activities;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.StyleRes;
import androidx.preference.PreferenceManager;

import ap.andruav_ap.R;

/**
 * Theme choice (AN-0147): Dark, Light, Sun or Follow system, stored in the
 * default shared preferences under {@link #PREF_KEY}. A themed activity
 * calls {@link #apply(Activity, boolean)} in onCreate before setContentView
 * and {@link #recreateIfChanged(Activity)} in onResume, so a change applies
 * at once with no app restart.
 */
public final class AppThemes {

    public static final String PREF_KEY = "pref_app_theme";

    public static final String VALUE_DARK = "dark";
    public static final String VALUE_LIGHT = "light";
    public static final String VALUE_SUN = "sun";
    public static final String VALUE_SYSTEM = "system";

    private AppThemes() {
    }

    public static String get(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_KEY, VALUE_DARK);
    }

    public static void set(final Context context, final String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREF_KEY, value).apply();
    }

    /** Resolves the stored choice to a Theme.Andruav style. Follow system
     *  maps the system night mode to Dark and day mode to Light. */
    @StyleRes
    public static int styleFor(final Context context, final boolean noActionBar) {
        String value = get(context);
        if (VALUE_SYSTEM.equals(value)) {
            final int night = context.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            value = night == Configuration.UI_MODE_NIGHT_YES ? VALUE_DARK : VALUE_LIGHT;
        }
        switch (value) {
            case VALUE_LIGHT:
                return noActionBar ? R.style.Theme_Andruav_Light_NoActionBar : R.style.Theme_Andruav_Light;
            case VALUE_SUN:
                return noActionBar ? R.style.Theme_Andruav_Sun_NoActionBar : R.style.Theme_Andruav_Sun;
            case VALUE_DARK:
            default:
                return noActionBar ? R.style.Theme_Andruav_Dark_NoActionBar : R.style.Theme_Andruav_Dark;
        }
    }

    /** Applies the stored theme to an activity. Call before setContentView. */
    public static void apply(final Activity activity, final boolean noActionBar) {
        activity.setTheme(styleFor(activity, noActionBar));
        activity.getIntent().putExtra(PREF_KEY, styleFor(activity, noActionBar));
    }

    /** AN-0149: Sun draws text about 8% larger. fontScale scales every sp
     *  size, so it also covers views with an explicit textSize and the
     *  Java-built rows, where no theme attribute can reach. A themed
     *  activity wraps its attachBaseContext with this; a theme change
     *  recreates the activity, so the wrap is re-evaluated on every
     *  (re)create and needs no reset. */
    private static final float SUN_FONT_SCALE = 1.08f;

    public static Context wrapFontScale(final Context newBase) {
        if (!VALUE_SUN.equals(get(newBase))) {
            return newBase;
        }
        final Configuration override = new Configuration(
                newBase.getResources().getConfiguration());
        override.fontScale *= SUN_FONT_SCALE;
        return newBase.createConfigurationContext(override);
    }

    /** Recreates the activity when the stored theme no longer matches the
     *  one applied at onCreate - covers sheets changed on another screen. */
    public static void recreateIfChanged(final Activity activity, final boolean noActionBar) {
        final int applied = activity.getIntent().getIntExtra(PREF_KEY, styleFor(activity, noActionBar));
        if (applied != styleFor(activity, noActionBar)) {
            activity.recreate();
        }
    }
}
