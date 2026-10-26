package ap.andruav.denative;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Hosts one DroneEngage native module in its own process (megaplan N1).
 * The concrete subclass names the module and its argv; the process is set
 * in the manifest (android:process=":de_<module>").
 */
public abstract class DeModuleService extends Service {

    public static final String ACTION_START = "ap.andruav.denative.START";
    public static final String ACTION_STOP = "ap.andruav.denative.STOP";

    private static final String TAG = "DeModuleService";
    private static final String CHANNEL_ID = "de_modules";
    private static final int NOTIFICATION_ID = 4201;
    private static final long STOP_GRACE_MS = 5000;

    static {
        System.loadLibrary("de_host");
    }

    private final IBinder binder = new Binder();
    private Thread moduleThread;

    protected abstract String getModuleName();

    protected abstract String[] getModuleArgv();

    /**
     * Runs after configs are prepared and before the native thread starts.
     * Modules that need a Java-side companion (e.g. the FC link bridge)
     * start it here.
     */
    protected void onModuleStarting(File workDir) {
    }

    private native int nativeRun(String libName, String workDir, String[] argv);

    private native void nativeRequestStop();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundNotification();
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopModule();
        } else {
            startModule();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        nativeRequestStop();
        super.onDestroy();
    }

    private void startModule() {
        if (moduleThread != null && moduleThread.isAlive()) {
            return;
        }
        final File workDir = new File(getFilesDir(), getModuleName());
        //noinspection ResultOfMethodCallIgnored
        workDir.mkdirs();
        copyAssetsIfMissing(workDir);
        applyOverrides(workDir);
        onModuleStarting(workDir);
        moduleThread = new Thread(() -> {
            int rc = nativeRun(getModuleName(), workDir.getAbsolutePath(), getModuleArgv());
            Log.i(TAG, getModuleName() + " nativeRun returned " + rc);
        }, "de_" + getModuleName());
        moduleThread.start();
    }

    private void stopModule() {
        nativeRequestStop();
        final Thread t = moduleThread;
        if (t != null) {
            try {
                t.join(STOP_GRACE_MS);
            } catch (InterruptedException ignored) {
            }
        }
        Process.killProcess(Process.myPid());
    }

    private void copyAssetsIfMissing(File workDir) {
        final AssetManager assets = getAssets();
        try {
            String[] files = assets.list(getModuleName());
            if (files == null) {
                return;
            }
            for (String name : files) {
                File dest = new File(workDir, name);
                if (dest.exists()) {
                    continue;
                }
                copyStream(assets.open(getModuleName() + "/" + name), dest);
            }
        } catch (IOException e) {
            Log.e(TAG, "copyAssetsIfMissing failed for " + getModuleName(), e);
        }
    }

    // Files placed in Android/data/<pkg>/files/de_override/<module>/ (e.g. via
    // adb push by the sim) replace the stock config on the next START.
    private void applyOverrides(File workDir) {
        File overrideDir = new File(getExternalFilesDir("de_override"), getModuleName());
        File[] files = overrideDir.listFiles();
        if (files == null) {
            return;
        }
        for (File src : files) {
            try {
                copyStream(new java.io.FileInputStream(src), new File(workDir, src.getName()));
            } catch (IOException e) {
                Log.e(TAG, "applyOverrides failed on " + src.getName(), e);
            }
        }
    }

    private void copyStream(InputStream in, File dest) throws IOException {
        try (InputStream input = in; OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = input.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "DroneEngage modules", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void startForegroundNotification() {
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("DroneEngage " + getModuleName())
                    .setContentText("native module running")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("DroneEngage " + getModuleName())
                    .setContentText("native module running")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .build();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }
}
