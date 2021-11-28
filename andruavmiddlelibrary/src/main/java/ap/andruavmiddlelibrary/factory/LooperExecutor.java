package ap.andruavmiddlelibrary.factory;

/**
 * Created by mhefny on 10/31/16.
 *  FROM: https://github.com/nubomedia-vtt/webrtcpeer-android.git
 */

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.andruav.FeatureSwitch;

import java.util.concurrent.Executor;

/**
 * Looper based executor class.
 */
public class LooperExecutor extends Thread implements Executor {
    private static final String TAG = "LooperExecutor";
    // Object used to signal that looper thread has started and Handler instance
    // associated with looper thread has been allocated.
    private final Object looperStartedEvent = new Object();
    private Handler handler = null;
    private boolean running = false;
    private long threadId;

    /**
     *
     */
    @Override
    public void run() {
        Looper.prepare();
        synchronized (looperStartedEvent) {
            Log.d(TAG, "Looper thread started.");
            handler = new Handler();
            threadId = Thread.currentThread().getId();
            looperStartedEvent.notify();
        }
        Looper.loop();
    }

    /**
     *
     */
    public synchronized void requestStart() {
        if (running) {
            return;
        }
        running = true;
        handler = null;
        start();
        // Wait for Hander allocation.
        synchronized (looperStartedEvent) {
            while (handler == null) {
                try {
                    looperStartedEvent.wait();
                } catch (InterruptedException e) {
                    //Log.e(TAG, "Can not start looper thread");
                    running = false;
                }
            }
        }
    }

    /**
     *
     */
    public synchronized void requestStop() {
        if (!running) {
            return;
        }
        running = false;
        handler.post(new Runnable() {
            @Override
            public void run() {
                Looper.myLooper().quit();
                if (FeatureSwitch.DEBUG_MODE) {
                   Log.d(TAG, "Looper thread finished.");
                }
            }
        });
    }

    /**
     * Checks if current thread is a looper thread.
     * @return True if the current thread is a looper
     */
    public boolean checkOnLooperThread() {
        return (Thread.currentThread().getId() == threadId);
    }

    /**
     *
     * @param runnable The runnable object to be executed
     */
    @Override
    public synchronized void execute(final Runnable runnable) {
        if (!running) {
            if (FeatureSwitch.DEBUG_MODE) {
                Log.w(TAG, "Running looper executor without calling requestStart()");
            }
            return;
        }
        if (Thread.currentThread().getId() == threadId) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }
}