package ap.andruav_ap.denative.providers.phone;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

/**
 * {@code phone.vibrate} driver: a repeating on/off pattern while the
 * resource is "on", {@code cancel()} on "off".
 */
final class VibrateDriver implements PhoneOutput.Driver {

    /** wait, buzz, pause - repeated from index 0 while on. */
    private static final long[] PATTERN = {0, 500, 300};

    private final Context m_context;

    VibrateDriver(final Context context) {
        this.m_context = context;
    }

    @Override
    public String apply(final boolean on) {
        try {
            final Vibrator vibrator = _vibrator();
            if (vibrator == null || !vibrator.hasVibrator()) return "no vibrator";
            if (!on) {
                vibrator.cancel();
                return null;
            }
            // AN-0107: usage UNKNOWN is dropped as "ignored_for_settings"
            // whenever touch/notification haptics are off (MIUI, Android 13+);
            // a GCS-commanded vibrate is an alarm.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(VibrationEffect.createWaveform(PATTERN, 0),
                        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(PATTERN, 0),
                        new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
            } else {
                //noinspection deprecation - API < 26 path
                vibrator.vibrate(PATTERN, 0);
            }
            return null;
        } catch (Exception e) {
            return "vibrate: " + e;
        }
    }

    private Vibrator _vibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            final VibratorManager manager =
                    (VibratorManager) m_context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return manager != null ? manager.getDefaultVibrator() : null;
        }
        //noinspection deprecation - API < 31 path
        return (Vibrator) m_context.getSystemService(Context.VIBRATOR_SERVICE);
    }
}
