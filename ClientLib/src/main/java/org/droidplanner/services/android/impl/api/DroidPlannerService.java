package org.droidplanner.services.android.impl.api;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import android.text.TextUtils;

import com.o3dr.android.client.R;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.mission.item.complex.CameraDetail;

import org.droidplanner.services.android.impl.core.drone.DroneManager;
import org.droidplanner.services.android.impl.core.survey.CameraInfo;
import org.droidplanner.services.android.impl.utils.file.IO.CameraInfoLoader;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Drone services implementation.
 *
 * <p>
 * This runs in the app's own process and is bound through {@link LocalBinder}, so clients get a
 * direct reference to this instance and to the {@link DroneApi} it hands out - no AIDL, no binder
 * marshalling. It stays an Android {@link Service} because it is the app's only foreground service:
 * the ongoing notification is what keeps the process at foreground priority while a vehicle is
 * connected.
 * </p>
 */
public class DroidPlannerService extends Service {

        /**
     * Status bar notification id
     */
    private static final int FOREGROUND_ID = 101;

    /**
     * Handed to clients on bind, giving them a direct reference to this service instance.
     */
    public class LocalBinder extends Binder {
        public DroidPlannerService getService() {
            return DroidPlannerService.this;
        }
    }

    private final IBinder localBinder = new LocalBinder();

    /**
     * Stores drone api instances per connected client. The client are denoted by their app id.
     */
    DroneApi droneApiStore = null;
    /**
     * Caches drone managers per connection type.
     */
    DroneManager droneManager = null;

    private CameraInfoLoader cameraInfoLoader;
    private List<CameraDetail> cachedCameraDetails;

    /**
     * Generate a drone api instance for the connecting client.
     *
     * @return a DroneApi instance
     */
    public DroneApi registerDroneApi() {
        releaseDroneApi();

        DroneApi droneApi = new DroneApi(this);
        droneApiStore = droneApi;
        updateForegroundNotification();
        return droneApi;
    }

    /**
     * Release the drone api instance attached to the given app id.
     *
     */
    public void releaseDroneApi() {
        if (droneApiStore == null) return ;
        droneApiStore.destroy();
        droneApiStore = null;
    }

    /**
     * Establish a connection with a vehicle using the given connection parameter.
     *
     * @param connParams Parameters used to connect to the vehicle.
     * @param listener   Callback to receive drone events.
     * @return A DroneManager instance which acts as router between the connected vehicle and the listeneing client(s).
     */
    DroneManager connectDroneManager(ConnectionParameter connParams, DroneApi listener) {
        if (connParams == null  || listener == null)
            return null;

        if (droneManager == null) {
            droneManager = DroneManager.generateDroneManager(getApplicationContext(), connParams, new Handler(Looper.getMainLooper()));
        }
        else
        {
            droneManager.destroy();
        }


        Timber.d("Drone manager connection for appId");
        droneManager.connect(listener, connParams);
        return droneManager;
    }

    /**
     * Disconnect the given client from the vehicle managed by the given drone manager.
     *
     * @param droneMgr   Handler for the connected vehicle.
     * @param clientInfo Info of the disconnecting client.
     */
    void disconnectDroneManager(DroneManager droneMgr, DroneApi.ClientInfo clientInfo) {
        if (droneMgr == null || clientInfo == null )
            return;

        Timber.d("Drone manager disconnection for appId");
        droneMgr.disconnect();
        droneMgr.destroy();
        droneManager = null;
    }

    /**
     * Retrieves the set of camera info provided by the app.
     *
     * @return a list of {@link CameraDetail} objects.
     */
    synchronized List<CameraDetail> getCameraDetails() {
        if (cachedCameraDetails == null) {
            List<String> cameraInfoNames = cameraInfoLoader.getCameraInfoList();

            List<CameraInfo> cameraInfos = new ArrayList<>(cameraInfoNames.size());
            for (String infoName : cameraInfoNames) {
                try {
                    cameraInfos.add(cameraInfoLoader.openFile(infoName));
                } catch (Exception e) {
                    Timber.e(e, e.getMessage());
                }
            }

            List<CameraDetail> cameraDetails = new ArrayList<>(cameraInfos.size());
            for (CameraInfo camInfo : cameraInfos) {
                cameraDetails.add(new CameraDetail(camInfo.name, camInfo.sensorWidth,
                        camInfo.sensorHeight, camInfo.sensorResolution, camInfo.focalLength,
                        camInfo.overlap, camInfo.sidelap, camInfo.isInLandscapeOrientation));
            }

            cachedCameraDetails = cameraDetails;
        }

        return cachedCameraDetails;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.d("Binding intent: " + intent);
        return localBinder;
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreate() {
        super.onCreate();

        Timber.d("Creating drone services.");

        final Context context = getApplicationContext();

        this.cameraInfoLoader = new CameraInfoLoader(context);

        updateForegroundNotification();
    }

    @SuppressLint("NewApi")
    private void updateForegroundNotification() {
        final Context context = getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMyOwnForeground();
        }
        else {
            //Put the service in the foreground
            final NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
                    .setContentTitle("DroneKit-Android")
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setSmallIcon(R.drawable.ic_stat_notify);

            if (droneApiStore != null)
            {
                notifBuilder.setContentText(" connected apps");
            }

            final Notification notification = notifBuilder.build();
            startForeground(FOREGROUND_ID, notification);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        //chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        Notification.Builder notificationBuilder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle("DroneKit-Android")
                .setPriority(Notification.PRIORITY_MIN) //NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int type = computeForegroundServiceType();
            try {
                if (type != 0) {
                    startForeground(FOREGROUND_ID, notification, type);
                } else {
                    startForeground(FOREGROUND_ID, notification);
                }
            } catch (SecurityException e) {
                // A type's permission was revoked between check and call, or the system
                // requires a permission we couldn't check (e.g. per-device USB permission
                // for a USB telemetry radio without BLUETOOTH_CONNECT). Retry with the
                // untyped overload - on API 34+ with manifest-declared types this uses the
                // manifest types, which may still throw, but we've exhausted our options.
                startForeground(FOREGROUND_ID, notification);
            }
        } else {
            startForeground(FOREGROUND_ID, notification);
        }
    }

    /**
     * Computes the foreground service type mask for API 29+ based on which runtime permissions
     * are currently granted. The manifest declares {@code location|connectedDevice}, but on
     * Android 14+ the typed {@code startForeground} call throws
     * {@code ForegroundServiceTypeNotAllowed} if the permission backing a passed type isn't
     * granted. By passing only the subset whose permissions are held, the service can still
     * enter the foreground when only some permissions are available (e.g. a USB connection
     * without location permission, or location without Bluetooth).
     */
    private int computeForegroundServiceType() {
        int type = 0;
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            type |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
        }
        // BLUETOOTH_CONNECT is an API 31+ runtime permission. On API 29-30 the legacy
        // BLUETOOTH permission is install-time, so connectedDevice is always available.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                type |= ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
            }
        } else {
            type |= ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
        }
        return type;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("Destroying drone services.");

        if (droneApiStore != null) {
            droneApiStore.destroy();
            droneApiStore = null;
        }

        if (droneManager != null) {
            droneManager.destroy();
            droneManager = null;
        }

        stopForeground(true);
    }

}
