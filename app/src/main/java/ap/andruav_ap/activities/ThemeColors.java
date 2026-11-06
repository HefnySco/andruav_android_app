package ap.andruav_ap.activities;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

/**
 * Resolves a Theme.Andruav palette attribute (deX, declared in
 * res/values/attrs.xml) to a colour for Java code — the same way
 * ?attr/deX does in layouts and drawables.
 */
public final class ThemeColors {

    private ThemeColors() {
    }

    @ColorInt
    public static int get(final Context context, @AttrRes final int attr) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return value.data;
        }
        return ContextCompat.getColor(context, value.resourceId);
    }

    public static float getDimen(final Context context, @AttrRes final int attr) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.getDimension(context.getResources().getDisplayMetrics());
    }
}
