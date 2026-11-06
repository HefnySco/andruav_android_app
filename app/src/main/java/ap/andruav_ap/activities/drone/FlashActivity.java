package ap.andruav_ap.activities.drone;


import androidx.appcompat.app.AppCompatActivity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.concurrent.atomic.AtomicBoolean;

import ap.andruav_ap.App;

/**
 * The {@code phone.screen} output surface: a dedicated full-screen strobe
 * that works headless — it runs with showWhenLocked + turnScreenOn so a
 * mission/emergency can strobe on a LOCKED phone (TO-BE §4.3 / §5.2).
 *
 * Started/stopped through {@link #requestStrobe}/{@link #stopStrobe} by
 * the phone provider's screen driver; launch extras carry the configured
 * {@code strobe_hz} (default 4) and {@code color} (default white). The
 * screen runs at full brightness and toggles between the configured
 * color and black.
 */
public class FlashActivity extends AppCompatActivity {

    private static final String TAG = "FlashActivity";

    public static final String EXTRA_STROBE_HZ = "strobe_hz";
    public static final String EXTRA_COLOR = "color";

    private static final double DEFAULT_STROBE_HZ = 4.0;
    private static final int DEFAULT_COLOR = 0xFFFFFF;
    private static final long MIN_HALF_PERIOD_MS = 40;

    /** Set by the choke point before launch / on stop; a stale launch self-finishes. */
    private static final AtomicBoolean sStrobeRequested = new AtomicBoolean(false);
    private static volatile FlashActivity sInstance;

    private static final String STROBE_CHANNEL_ID = "screen_strobe";
    private static final int STROBE_NOTIFY_ID = 7041;

    private static Intent strobeIntent(final Context context, final double strobeHz, final int color) {
        return new Intent(context, FlashActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_STROBE_HZ, (float) strobeHz)
                .putExtra(EXTRA_COLOR, color);
    }

    /* AN-0116: Android 10+ silently blocks startActivity from a
     * background app - the strobe then never shows although the pin
     * reports on. A high-priority notification with a full-screen
     * intent is the sanctioned path over the lockscreen (needs the
     * USE_FULL_SCREEN_INTENT grant on API 34+; below that it lands as a
     * heads-up notification the user can open). The direct start stays
     * for the foreground case; the notification is cancelled once the
     * activity is up or the strobe stops. */
    private static void postStrobeNotification(final Context context, final Intent intent) {
        final NotificationManager nm =
                context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(
                    STROBE_CHANNEL_ID, "Screen strobe",
                    NotificationManager.IMPORTANCE_HIGH));
        }
        final PendingIntent pi = PendingIntent.getActivity(context, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
        final Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, STROBE_CHANNEL_ID)
                : new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Screen strobe")
                .setContentText("Tap to show")
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_ALARM)
                .setFullScreenIntent(pi, true)
                .setContentIntent(pi)
                .setAutoCancel(true);
        nm.notify(STROBE_NOTIFY_ID, b.build());
    }

    private static void cancelStrobeNotification(final Context context) {
        final NotificationManager nm =
                context.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(STROBE_NOTIFY_ID);
    }

    /** Turn the screen strobe on: mark it requested and bring this activity up. */
    public static void requestStrobe(final Context context, final double strobeHz, final int color) {
        sStrobeRequested.set(true);
        final Intent intent = strobeIntent(context, strobeHz, color);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "direct strobe launch failed", e);
        }
        postStrobeNotification(context, intent);
    }

    /***
     * Turn the screen strobe off. The notification is cancelled on every
     * stop - when the direct launch failed and the activity never came up,
     * a stop must still remove it from the shade. The activity, when it is
     * running, is finished too.
     */
    public static void stopStrobe(final Context context) {
        sStrobeRequested.set(false);
        cancelStrobeNotification(context.getApplicationContext());
        final FlashActivity instance = sInstance;
        if (instance != null) {
            instance.runOnUiThread(instance::finish);
        }
    }

    public static boolean isStrobeRequested() {
        return sStrobeRequested.get();
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private View mStrobeView;
    private int mColorOn = Color.WHITE;
    private long mHalfPeriodMs = 125;
    private boolean mLit;

    private final Runnable mStrobe = new Runnable() {
        @Override
        public void run() {
            if (mStrobeView == null) return;
            mLit = !mLit;
            mStrobeView.setBackgroundColor(mLit ? mColorOn : Color.BLACK);
            mHandler.postDelayed(this, mHalfPeriodMs);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // a launch that arrived after the choke point already turned off
        // (e.g. a very short pulse) never becomes visible
        if (!sStrobeRequested.get()) {
            super.onCreate(savedInstanceState);
            finish();
            return;
        }
        super.onCreate(savedInstanceState);
        sInstance = this;
        cancelStrobeNotification(this);

        App.activeActivity = this;
        App.ForceLanguage();

        // locked-screen visibility: runtime calls (API 27+) plus the
        // manifest attrs and the legacy window flags for older devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        final WindowManager.LayoutParams attrs = win.getAttributes();
        attrs.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        win.setAttributes(attrs);

        applyExtras(getIntent());

        mStrobeView = new View(this);
        mStrobeView.setBackgroundColor(Color.BLACK);
        setContentView(mStrobeView);
    }

    /** A re-launch while strobing just refreshes the configured rate/color. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        cancelStrobeNotification(this);
        applyExtras(intent);
    }

    private void applyExtras(final Intent intent) {
        if (intent == null) return;
        final double hz = intent.getFloatExtra(EXTRA_STROBE_HZ, (float) DEFAULT_STROBE_HZ);
        mColorOn = Color.BLACK | (intent.getIntExtra(EXTRA_COLOR, DEFAULT_COLOR) & 0xFFFFFF);
        mHalfPeriodMs = Math.max(MIN_HALF_PERIOD_MS, (long) (500.0 / hz));
    }

    @Override
    public void onStart() {
        super.onStart();
        App.activeActivity = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sStrobeRequested.get()) {
            finish();
            return;
        }
        mHandler.removeCallbacks(mStrobe);
        mHandler.post(mStrobe);
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(mStrobe);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mStrobe);
        if (sInstance == this) {
            sInstance = null;
        }
        // note: if the user dismissed the activity while the resource is
        // still logically on, the choke point keeps its state; the next
        // off/restore write still edges correctly.
        super.onDestroy();
    }
}
