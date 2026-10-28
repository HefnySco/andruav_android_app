package ap.andruav_ap.denative;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.andruav.AndruavEngine;
import com.andruav.AndruavFacade;
import com.andruav.protocol.communication.udpproxy.UDPProxy;
import com.andruav.protocol.communication.websocket.AndruavWSClientBase;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;

import ap.andruav.denative.DeUnitSupervisor;
import ap.andruav_ap.App;
import ap.andruav_ap.communication.AndruavDeBusClient;
import ap.andruav_ap.communication.telemetry.TelemetryModeer;
import ap.andruav_ap.denative.config.DeConfigGenerator;
import ap.andruav_ap.denative.config.DeConfigSync;
import ap.andruav_ap.services.DeAndroidModuleService;
import ap.andruavmiddlelibrary.preference.Preference;

/**
 * C3 (AN-0051): the "Native unit" switch and the mutual-exclusion engine
 * between the Java stack (AndruavWSClient + DroneKitServer/ControlBoard +
 * Java UDP proxy) and the native stack (DeUnitSupervisor modules +
 * DeAndroidModuleService), per decision N8.
 * <p>
 * The one rule: never both stacks online with the same unit id. Every
 * switch first stops the active stack and waits for its logout (WS socket
 * closed / module processes gone, with a timeout fallback) before the
 * other stack is started.
 * <p>
 * All methods must be called from the app's main process; the
 * {@code :de_*} module processes are gated out via {@link #isMainProcess}.
 */
public final class NativeUnitController {

    private static final String TAG = "NativeUnitCtrl";

    /** Poll cadence while waiting for the outgoing stack to go offline. */
    private static final long POLL_MS = 250;
    /**
     * Max wait for a clean logout before starting the other stack anyway -
     * a socket that never finishes closing is already not an online party.
     */
    private static final long WAIT_TIMEOUT_MS = 8000;

    private static final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * True when the Java link was up at the moment native was switched on;
     * switching back off then re-connects the session instead of leaving
     * the unit offline.
     */
    private static boolean sResumeJavaLink;

    /**
     * True when the Java FC link (DroneKitServer) was open at the moment
     * native was switched on. Tracked apart from {@link #sResumeJavaLink}:
     * a unit can have its FC up without a server link.
     */
    private static boolean sResumeFcLink;

    /*** Bumped per switch so a stale pending wait cannot fire after a newer toggle. */
    private static int sSwitchGeneration;

    /**
     * True while the user wants the native stack up (AN-0064). Explicit
     * disconnects clear it so auto-resume paths (the link guardian via
     * {@link App#resumeLink} and {@link App#startAndruavWS}) cannot
     * resurrect the stack; an explicit connect or the setting's ON switch
     * sets it again. In-memory only: a process restart defaults it true so
     * a pref-on unit still auto-starts on boot.
     */
    private static volatile boolean sNativeWanted = true;

    private NativeUnitController() {
    }

    public static boolean isNativeMode() {
        return Preference.isNativeUnitEnabled(null);
    }

    /**
     * The setting changed: persist it, then run the mutual-exclusion switch.
     */
    public static synchronized void applyMode(final Context context, final boolean nativeOn) {
        final Context app = context.getApplicationContext();
        // Snapshot what the Java stack had up only on the ON switch - the OFF
        // switch must read these, never overwrite them. A repeated ON while
        // already native keeps the first snapshot (the Java stack is down by then).
        if (nativeOn && !Preference.isNativeUnitEnabled(null)) {
            sResumeJavaLink = Preference.isLinkServiceDesired(null) || App.isAndruavWSConnected();
            sResumeFcLink = App.droneKitServer != null;
        }
        Preference.isNativeUnitEnabled(null, nativeOn);
        final int generation = ++sSwitchGeneration;
        if (nativeOn) {
            Log.i(TAG, "switching to native stack");
            stopJavaStack(app);
            // AN-0064: stopJavaStack transitively calls stopNativeStack via
            // the stopAndruavWS guard, which clears the wanted flag - arm it
            // only after that teardown so the pending start is not dropped.
            sNativeWanted = true;
            waitFor("java stack down", generation, NativeUnitController::isJavaStackDown,
                    () -> startNativeStack(app));
        } else {
            Log.i(TAG, "switching to java stack");
            stopNativeStack(app);
            waitFor("native stack down", generation, NativeUnitController::isNativeStackDown,
                    () -> restoreJavaStack(app));
        }
    }

    /**
     * Idempotent native-stack bring-up: regenerate module configs (catches
     * up edits made while the stack was off), arm the config sync listener,
     * start the supervised modules and the app module service.
     */
    public static void startNativeStack(final Context context) {
        if (!sNativeWanted) {
            // User disconnected; only an explicit connect re-arms it.
            return;
        }
        final Context app = context.getApplicationContext();
        final DeUnitSupervisor supervisor = DeUnitSupervisor.get();
        supervisor.init(app);
        // de_comm gets its connection settings and de_mavlink its FC link
        // (FCB screen) on the command line, read
        // fresh from the preferences on every (re)start.
        supervisor.setArgsProvider(module -> module == DeUnitSupervisor.Module.DE_COMM
                ? DeConfigGenerator.commArgs() : DeConfigGenerator.mavlinkArgs());

        List<String> changed = regenerateQuietly(app);
        DeConfigSync.start(app);

        final boolean commWasUp = supervisor.getState(DeUnitSupervisor.Module.DE_COMM)
                == DeUnitSupervisor.State.RUNNING;
        final boolean mavWasUp = supervisor.getState(DeUnitSupervisor.Module.DE_MAVLINK)
                == DeUnitSupervisor.State.RUNNING;

        supervisor.start(DeUnitSupervisor.Module.DE_COMM);
        supervisor.start(DeUnitSupervisor.Module.DE_MAVLINK);

        // A config that changed while a module was already running only
        // takes effect on process restart - bounce that module.
        if (commWasUp && changed.contains(DeConfigGenerator.COMM)) {
            bounceModule(supervisor, DeUnitSupervisor.Module.DE_COMM);
        }
        if (mavWasUp && changed.contains(DeConfigGenerator.MAVLINK)) {
            bounceModule(supervisor, DeUnitSupervisor.Module.DE_MAVLINK);
        }

        // B2: the app-side databus module (capability surface) joins the unit.
        final Intent moduleIntent = new Intent(app, DeAndroidModuleService.class)
                .setAction(DeAndroidModuleService.ACTION_START);
        try {
            ContextCompat.startForegroundService(app, moduleIntent);
        } catch (Exception e) {
            Log.e(TAG, "cannot start DeAndroidModuleService", e);
        }
        Log.i(TAG, "native stack start requested");
    }

    /**
     * User-initiated connect (AN-0064): re-arms {@link #sNativeWanted}, then
     * brings the stack up. Auto-resume paths call {@link #startNativeStack}
     * directly and stay gated by the flag.
     */
    public static void connectNative(final Context context) {
        sNativeWanted = true;
        startNativeStack(context);
    }

    /**
     * Idempotent native-stack teardown: app module first (it leaves
     * de_comm's registry cleanly), then the supervised module processes.
     */
    public static void stopNativeStack(final Context context) {
        sNativeWanted = false;
        final Context app = context.getApplicationContext();
        // AN-0064: the config sync fires ACTION_START straight at the module
        // services on a config change, bypassing the supervisor and the
        // wanted flag - disarm it while the user has the stack off.
        DeConfigSync.stop(app);
        app.startService(new Intent(app, DeAndroidModuleService.class)
                .setAction(DeAndroidModuleService.ACTION_STOP));
        final DeUnitSupervisor supervisor = DeUnitSupervisor.get();
        supervisor.init(app);
        supervisor.stop(DeUnitSupervisor.Module.DE_COMM);
        supervisor.stop(DeUnitSupervisor.Module.DE_MAVLINK);
        Log.i(TAG, "native stack stop requested");
    }

    /**
     * True while the native stack is wanted up - drives the home-screen
     * connect toggle in native mode.
     */
    public static boolean isNativeStackActive() {
        final DeUnitSupervisor supervisor = DeUnitSupervisor.get();
        return supervisor.getState(DeUnitSupervisor.Module.DE_COMM)
                != DeUnitSupervisor.State.STOPPED
                || supervisor.getState(DeUnitSupervisor.Module.DE_MAVLINK)
                != DeUnitSupervisor.State.STOPPED;
    }

    /**
     * AN-0052: the comm-server link state de_comm reports on the
     * databus (9100 {@code ms.g} socket status / 1061 {@code d_cl_*}
     * events, F5 AN-0072) - the "connected" signal for native mode.
     * The WS socket state cannot stand in for it:
     * {@link AndruavDeBusClient} ties it to the module socket lifetime,
     * which survives a dropped server link.
     */
    public static boolean isCommLineOnline() {
        return AndruavDeBusClient.commLineOnline();
    }

    /**
     * AN-0052: the {@link AndruavWSClientBase} socket state the legacy
     * connection gating (menu items, tile enablement, Com tile) should
     * apply in native mode: {@code SOCKETSTATE_REGISTERED} only while
     * the comm line is up, {@code SOCKETSTATE_DISCONNECTED} once the
     * module stack is off, {@code SOCKETSTATE_CONNECTED} while modules
     * run but the server link is down or not yet reported.
     */
    public static int effectiveSocketState() {
        if (!isNativeStackActive()) {
            return AndruavWSClientBase.SOCKETSTATE_DISCONNECTED;
        }
        return isCommLineOnline()
                ? AndruavWSClientBase.SOCKETSTATE_REGISTERED
                : AndruavWSClientBase.SOCKETSTATE_CONNECTED;
    }

    /** One-line stack state for the home screen, e.g. appended to the server subtitle. */
    public static String describeNativeState() {
        final DeUnitSupervisor supervisor = DeUnitSupervisor.get();
        return "native unit · de_comm " + supervisor.getState(DeUnitSupervisor.Module.DE_COMM)
                + " · de_mavlink " + supervisor.getState(DeUnitSupervisor.Module.DE_MAVLINK);
    }

    /**
     * Application.onCreate also runs inside the {@code :de_*} module
     * processes; only the main process may own the supervisor.
     */
    public static boolean isMainProcess(final Context context) {
        final String packageName = context.getPackageName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            final String proc = Application.getProcessName();
            return proc == null || packageName.equals(proc);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/cmdline"))) {
            String name = reader.readLine();
            if (name == null) {
                return false;
            }
            final int nul = name.indexOf('\0');
            if (nul > 0) {
                name = name.substring(0, nul);
            }
            return packageName.equals(name.trim());
        } catch (IOException e) {
            return false;
        }
    }

    private static void stopJavaStack(final Context app) {
        // FC link: DroneKitServer / ControlBoard_DroneKit connections.
        TelemetryModeer.closeAllConnections();
        // Java UDP proxy: stop the server-side feed, drop the relay token
        // hello loop and pause the forwarder.
        AndruavFacade.StopUdpProxyTelemetry();
        final UDPProxy proxy = AndruavEngine.getUDPProxy();
        if (proxy != null) {
            proxy.setToken(null);
            proxy.setPause(true);
        }
        // Java comm client + its link guardian.
        App.stopAndruavWS(true);
    }

    private static void restoreJavaStack(final Context app) {
        // AN-0106: native mode ran the me-unit under de_comm's party id;
        // the Java stack logs in with the saved one.
        App.updateWe7daInfo();
        final UDPProxy proxy = AndruavEngine.getUDPProxy();
        if (proxy != null) {
            proxy.setPause(false);
        }
        if (sResumeFcLink) {
            sResumeFcLink = false;
            TelemetryModeer.connectToPreferredConnection(app, true);
        }
        if (sResumeJavaLink) {
            sResumeJavaLink = false;
            App.startAndruavWS();
        }
    }

    private static boolean isJavaStackDown() {
        return AndruavEngine.getAndruavWS() == null
                || !AndruavEngine.getAndruavWS().isConnected();
    }

    private static boolean isNativeStackDown() {
        final DeUnitSupervisor supervisor = DeUnitSupervisor.get();
        return supervisor.getPid(DeUnitSupervisor.Module.DE_COMM) <= 0
                && supervisor.getPid(DeUnitSupervisor.Module.DE_MAVLINK) <= 0;
    }

    /**
     * Runs {@code then} on the main thread once de_comm and de_mavlink are
     * both gone (or after the usual timeout), e.g. to drop a "stopping" UI.
     */
    public static void whenNativeStackDown(final Runnable then) {
        waitFor("native stack down (ui)", sSwitchGeneration, NativeUnitController::isNativeStackDown, then);
    }

    private static void waitFor(final String what, final int generation, final BooleanSupplier down,
                                final Runnable then) {
        final long deadline = SystemClock.elapsedRealtime() + WAIT_TIMEOUT_MS;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (generation != sSwitchGeneration) {
                    return;
                }
                if (down.getAsBoolean()) {
                    then.run();
                } else if (SystemClock.elapsedRealtime() >= deadline) {
                    Log.w(TAG, "timeout waiting for " + what + " - starting anyway");
                    then.run();
                } else {
                    handler.postDelayed(this, POLL_MS);
                }
            }
        });
    }

    private static void bounceModule(final DeUnitSupervisor supervisor,
                                     final DeUnitSupervisor.Module module) {
        // STOP then START reach the service in order, so the module comes
        // back on the regenerated config without a manual delay.
        supervisor.stop(module);
        supervisor.start(module);
    }

    private static List<String> regenerateQuietly(final Context app) {
        try {
            return DeConfigGenerator.regenerate(app);
        } catch (Exception e) {
            Log.e(TAG, "config regeneration failed", e);
            return java.util.Collections.emptyList();
        }
    }
}
