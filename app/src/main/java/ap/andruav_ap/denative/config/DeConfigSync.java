package ap.andruav_ap.denative.config;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import java.util.List;

import ap.andruav.denative.DeCommService;
import ap.andruav.denative.DeMavlinkService;
import ap.andruav.denative.DeModuleService;
import ap.andruav.denative.DeUnitSupervisor;

/**
 * Regenerates the module configs on any preference change and restarts the
 * modules whose files changed (AN-0050). Call {@link #start} once from the
 * main process, and only while the native unit is enabled (N8): a restart
 * starts a module that was not running. Not wired into Application yet.
 */
public final class DeConfigSync implements SharedPreferences.OnSharedPreferenceChangeListener {

    /** STOP kills the module process after at most 5 s; START must come after. */
    static final long RESTART_DELAY_MS = 6500;

    private static DeConfigSync instance;

    private final Context app;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable regenerate = this::regenerateNow;

    private DeConfigSync(Context app) {
        this.app = app;
    }

    public static synchronized void start(Context context) {
        if (instance != null) {
            return;
        }
        instance = new DeConfigSync(context.getApplicationContext());
        PreferenceManager.getDefaultSharedPreferences(instance.app)
                .registerOnSharedPreferenceChangeListener(instance);
        instance.app.getSharedPreferences(ap.andruavmiddlelibrary.preference.Preference.PREFS_COUNT, 0)
                .registerOnSharedPreferenceChangeListener(instance);
    }

    /**
     * AN-0064: disarm while the user has the stack disconnected - restart()
     * sends ACTION_START straight to the module services, bypassing the
     * supervisor's wanted flag, so a pref change would resurrect modules.
     */
    public static synchronized void stop(Context context) {
        if (instance == null) {
            return;
        }
        instance.handler.removeCallbacksAndMessages(null);
        PreferenceManager.getDefaultSharedPreferences(instance.app)
                .unregisterOnSharedPreferenceChangeListener(instance);
        instance.app.getSharedPreferences(ap.andruavmiddlelibrary.preference.Preference.PREFS_COUNT, 0)
                .unregisterOnSharedPreferenceChangeListener(instance);
        instance = null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        // Debounce bursts of changes (a settings screen saves several keys).
        handler.removeCallbacks(regenerate);
        handler.postDelayed(regenerate, 500);
    }

    private void regenerateNow() {
        // C6 (AN-0054): the 'Native unit' off-state no longer exists - the
        // listener only runs while the stack is wanted (start/stop arm it),
        // which is the guard the old pref check provided.
        List<String> changed = DeConfigGenerator.regenerate(app);
        for (String module : changed) {
            restart(module);
        }
    }

    private void restart(String module) {
        final Class<?> cls = DeConfigGenerator.COMM.equals(module)
                ? DeCommService.class : DeMavlinkService.class;
        app.startService(new Intent(app, cls).setAction(DeModuleService.ACTION_STOP));
        handler.postDelayed(() -> {
            Intent start = DeUnitSupervisor.get().startIntent(app,
                    cls == DeCommService.class ? DeUnitSupervisor.Module.DE_COMM
                            : DeUnitSupervisor.Module.DE_MAVLINK);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                app.startForegroundService(start);
            } else {
                app.startService(start);
            }
        }, RESTART_DELAY_MS);
    }
}
