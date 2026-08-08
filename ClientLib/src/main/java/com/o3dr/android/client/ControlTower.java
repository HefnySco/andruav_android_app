package com.o3dr.android.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.o3dr.android.client.interfaces.TowerListener;

import org.droidplanner.services.android.impl.api.DroidPlannerService;
import org.droidplanner.services.android.impl.api.DroneApi;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Entry point to the drone services.
 *
 * <p>
 * {@link DroidPlannerService} runs in this same process, so the binding below is a plain
 * {@code LocalBinder} handoff: once connected, every call on the returned {@link DroneApi} is a
 * direct virtual call. The bind itself is kept (rather than instantiating the service ourselves)
 * because {@link DroidPlannerService} is a foreground service - the binding is what keeps the
 * process at foreground priority while a vehicle is connected.
 * </p>
 *
 * Created by fhuya on 11/12/14.
 */
public class ControlTower {

    private static final String TAG = ControlTower.class.getSimpleName();

    private final ServiceConnection o3drServicesConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isServiceConnecting.set(false);

            o3drServices = ((DroidPlannerService.LocalBinder) service).getService();
            notifyTowerConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceConnecting.set(false);
            o3drServices = null;
            notifyTowerDisconnected();
        }
    };

    private final AtomicBoolean isServiceConnecting = new AtomicBoolean(false);

    private final Context context;
    private TowerListener towerListener;
    private DroidPlannerService o3drServices;

    public ControlTower(Context context) {
        this.context = context;
    }

    public boolean isTowerConnected() {
        return o3drServices != null;
    }

    void notifyTowerConnected() {
        if (towerListener == null)
            return;

        towerListener.onTowerConnected();
    }

    void notifyTowerDisconnected() {
        if (towerListener == null)
            return;

        towerListener.onTowerDisconnected();
    }

    public void registerDrone(Drone drone, Handler handler) {
        if (drone == null)
            return;

        if (!isTowerConnected())
            throw new IllegalStateException("Control Tower must be connected.");

        drone.init(this, handler);
        drone.start();
    }

    public void unregisterDrone(Drone drone) {
        if (drone != null)
            drone.destroy();
    }

    public void connect(TowerListener listener) {
        if (towerListener != null && (isServiceConnecting.get() || isTowerConnected()))
            return;

        if (listener == null) {
            throw new IllegalArgumentException("ServiceListener argument cannot be null.");
        }

        towerListener = listener;

        if (!isTowerConnected() && !isServiceConnecting.get()) {
            final Intent serviceIntent = new Intent(context, DroidPlannerService.class);
            isServiceConnecting.set(context.bindService(serviceIntent, o3drServicesConnection,
                    Context.BIND_AUTO_CREATE));
        }
    }

    public void disconnect() {
        o3drServices = null;

        notifyTowerDisconnected();

        towerListener = null;

        try {
            context.unbindService(o3drServicesConnection);
        } catch (Exception e) {
            Log.e(TAG, "Error occurred while unbinding from the drone services.");
        }
    }

    DroneApi registerDroneApi() {
        return o3drServices.registerDroneApi();
    }

    void releaseDroneApi() {
        if (o3drServices != null) {
            o3drServices.releaseDroneApi();
        }
    }
}
