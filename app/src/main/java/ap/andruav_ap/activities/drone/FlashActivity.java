package ap.andruav_ap.activities.drone;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

    public static final String EXTRA_STROBE_HZ = "strobe_hz";
    public static final String EXTRA_COLOR = "color";

    private static final double DEFAULT_STROBE_HZ = 4.0;
    private static final int DEFAULT_COLOR = 0xFFFFFF;
    private static final long MIN_HALF_PERIOD_MS = 40;

    /** Set by the choke point before launch / on stop; a stale launch self-finishes. */
    private static final AtomicBoolean sStrobeRequested = new AtomicBoolean(false);
    private static volatile FlashActivity sInstance;

    /** Turn the screen strobe on: mark it requested and bring this activity up. */
    public static void requestStrobe(final Context context, final double strobeHz, final int color) {
        sStrobeRequested.set(true);
        final Intent intent = new Intent(context, FlashActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_STROBE_HZ, (float) strobeHz)
                .putExtra(EXTRA_COLOR, color);
        context.startActivity(intent);
    }

    /** Turn the screen strobe off: finish the activity when it is up. */
    public static void stopStrobe() {
        sStrobeRequested.set(false);
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
