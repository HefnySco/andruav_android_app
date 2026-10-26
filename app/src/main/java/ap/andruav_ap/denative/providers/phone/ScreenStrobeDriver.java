package ap.andruav_ap.denative.providers.phone;

import android.content.Context;

import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;

import ap.andruav_ap.activities.drone.FlashActivity;

/**
 * {@code phone.screen} driver: the full-screen strobe
 * {@link FlashActivity} (showWhenLocked + turnScreenOn, so it works on a
 * locked phone) launched straight from whatever context started us -
 * the module service, Emergency, anything.
 *
 * Also keeps the legacy {@code andruavWe7daBase.isFlashing} flag in sync:
 * it is what the GCS sees in the unit's ID message and what the SOS
 * button reflects.
 */
final class ScreenStrobeDriver implements PhoneOutput.Driver {

    private final Context m_context;
    private final double m_strobe_hz;
    private final int m_color;

    ScreenStrobeDriver(final Context context, final double strobe_hz, final int color) {
        this.m_context = context;
        this.m_strobe_hz = strobe_hz;
        this.m_color = color;
    }

    @Override
    public String apply(final boolean on) {
        try {
            if (on) {
                FlashActivity.requestStrobe(m_context, m_strobe_hz, m_color);
            } else {
                FlashActivity.stopStrobe();
            }
            final AndruavUnitBase unit = AndruavSettings.andruavWe7daBase;
            if (unit != null) {
                unit.setIsFlashing(on);
            }
            return null;
        } catch (Exception e) {
            return "screen strobe: " + e;
        }
    }
}
