package ap.andruav.denative;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumMap;

/**
 * Lives in the app's main process. Starts/stops the per-module services and
 * watches their death through a bound ServiceConnection; restarts a wanted
 * module with 1/2/5/10/30 s backoff (reset after 60 s alive).
 */
public class DeUnitSupervisor {

    public enum Module {DE_COMM, DE_MAVLINK}

    public enum State {RUNNING, STOPPED, RESTARTING}

    private static final String TAG = "DeUnitSupervisor";
    private static final long[] BACKOFF_MS = {1000, 2000, 5000, 10000, 30000};
    private static final long ALIVE_RESET_MS = 60000;

    private static final DeUnitSupervisor instance = new DeUnitSupervisor();

    /** Supplies the extra argv a module gets on every START (may return null). */
    public interface ArgsProvider {
        String[] argsFor(Module module);
    }

    public static DeUnitSupervisor get() {
        return instance;
    }

    private static class ModuleState {
        boolean wantRunning;
        boolean connected;
        int restartCount;
        int backoffIndex;
        ServiceConnection conn;
        Runnable pendingRestart;
        Runnable backoffReset;
    }

    private final EnumMap<Module, ModuleState> states = new EnumMap<>(Module.class);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Context appContext;
    private volatile ArgsProvider argsProvider;

    private DeUnitSupervisor() {
        for (Module m : Module.values()) {
            states.put(m, new ModuleState());
        }
    }

    public synchronized void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public void setArgsProvider(ArgsProvider provider) {
        argsProvider = provider;
    }

    /** The intent that starts a module, carrying the provider's argv. */
    public Intent startIntent(Context context, Module module) {
        Intent intent = new Intent(context, serviceClass(module)).setAction(DeModuleService.ACTION_START);
        final ArgsProvider p = argsProvider;
        final String[] args = p == null ? null : p.argsFor(module);
        if (args != null) {
            intent.putExtra(DeModuleService.EXTRA_ARGS, args);
        }
        return intent;
    }

    public synchronized void start(Module module) {
        final ModuleState state = states.get(module);
        state.wantRunning = true;
        startServiceProcess(module, state, DeModuleService.ACTION_START);
        bindDeathWatch(module, state);
    }

    public synchronized void stop(Module module) {
        final ModuleState state = states.get(module);
        state.wantRunning = false;
        cancelPending(state);
        startServiceProcess(module, state, DeModuleService.ACTION_STOP);
        unbindDeathWatch(state);
        // AN-0064: MIUI's AutoStartManager drops background startService
        // intents, so the ACTION_STOP above may never arrive. stopService
        // is not autostart-gated and reaches onDestroy directly, which runs
        // the same nativeRequestStop teardown.
        if (appContext != null) {
            appContext.stopService(new Intent(appContext, serviceClass(module)));
        }
        state.connected = false;
    }

    public synchronized State getState(Module module) {
        final ModuleState state = states.get(module);
        if (state.connected || getPid(module) > 0) {
            return State.RUNNING;
        }
        return state.wantRunning ? State.RESTARTING : State.STOPPED;
    }

    public synchronized int getRestartCount(Module module) {
        return states.get(module).restartCount;
    }

    // PID of the module's own process (ardupilot.andruav:de_<module>) or -1.
    public int getPid(Module module) {
        final String suffix = ":" + procSuffix(module);
        File proc = new File("/proc");
        File[] entries = proc.listFiles();
        if (entries == null) {
            return -1;
        }
        for (File entry : entries) {
            final String name = entry.getName();
            if (!name.matches("\\d+")) {
                continue;
            }
            File cmdline = new File(entry, "cmdline");
            try (BufferedReader reader = new BufferedReader(new FileReader(cmdline))) {
                String line = reader.readLine();
                if (line != null && line.trim().endsWith(suffix)) {
                    return Integer.parseInt(name);
                }
            } catch (IOException | NumberFormatException ignored) {
            }
        }
        return -1;
    }

    private static String procSuffix(Module module) {
        return module == Module.DE_COMM ? "de_comm" : "de_mavlink";
    }

    private static Class<?> serviceClass(Module module) {
        return module == Module.DE_COMM ? DeCommService.class : DeMavlinkService.class;
    }

    private void startServiceProcess(Module module, ModuleState state, String action) {
        if (appContext == null) {
            return;
        }
        Intent intent = DeModuleService.ACTION_START.equals(action)
                ? startIntent(appContext, module)
                : new Intent(appContext, serviceClass(module)).setAction(action);
        try {
            if (DeModuleService.ACTION_START.equals(action)) {
                appContext.startForegroundService(intent);
            } else {
                appContext.startService(intent);
            }
        } catch (IllegalStateException | SecurityException e) {
            Log.e(TAG, "cannot " + action + " " + procSuffix(module), e);
        }
    }

    private void bindDeathWatch(final Module module, final ModuleState state) {
        if (appContext == null || state.conn != null) {
            return;
        }
        state.conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                synchronized (DeUnitSupervisor.this) {
                    state.connected = true;
                    state.backoffReset = () -> {
                        synchronized (DeUnitSupervisor.this) {
                            if (state.connected) {
                                state.backoffIndex = 0;
                            }
                        }
                    };
                    handler.postDelayed(state.backoffReset, ALIVE_RESET_MS);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                onModuleGone(module, state);
            }

            @Override
            public void onBindingDied(ComponentName name) {
                onModuleGone(module, state);
            }

            @Override
            public void onNullBinding(ComponentName name) {
                onModuleGone(module, state);
            }
        };
        Intent intent = new Intent(appContext, serviceClass(module));
        appContext.bindService(intent, state.conn, 0);
    }

    private void unbindDeathWatch(ModuleState state) {
        cancelPending(state);
        if (state.conn != null && appContext != null) {
            try {
                appContext.unbindService(state.conn);
            } catch (IllegalArgumentException ignored) {
            }
        }
        state.conn = null;
    }

    private void cancelPending(ModuleState state) {
        if (state.pendingRestart != null) {
            handler.removeCallbacks(state.pendingRestart);
            state.pendingRestart = null;
        }
        if (state.backoffReset != null) {
            handler.removeCallbacks(state.backoffReset);
            state.backoffReset = null;
        }
    }

    private void onModuleGone(Module module, ModuleState state) {
        synchronized (this) {
            state.connected = false;
            if (state.backoffReset != null) {
                handler.removeCallbacks(state.backoffReset);
                state.backoffReset = null;
            }
            if (state.conn != null && appContext != null) {
                try {
                    appContext.unbindService(state.conn);
                } catch (IllegalArgumentException ignored) {
                }
                state.conn = null;
            }
            if (!state.wantRunning || state.pendingRestart != null) {
                return;
            }
            state.restartCount++;
            long delay = BACKOFF_MS[Math.min(state.backoffIndex, BACKOFF_MS.length - 1)];
            state.backoffIndex++;
            state.pendingRestart = () -> {
                synchronized (DeUnitSupervisor.this) {
                    state.pendingRestart = null;
                    if (state.wantRunning) {
                        startServiceProcess(module, state, DeModuleService.ACTION_START);
                        bindDeathWatch(module, state);
                    }
                }
            };
            Log.i(TAG, procSuffix(module) + " died; restart " + state.restartCount
                    + " in " + delay + " ms");
            handler.postDelayed(state.pendingRestart, delay);
        }
    }
}
