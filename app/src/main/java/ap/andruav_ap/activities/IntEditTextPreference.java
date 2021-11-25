package ap.andruav_ap.activities;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Created by M.Hefny on 10-Jan-15.
 */
public class IntEditTextPreference extends EditTextPreference {

    public IntEditTextPreference(Context remoteChannels) {
        super(remoteChannels);
    }

    public IntEditTextPreference(Context none, AttributeSet andruavNoRemoteCommandIdMissionFull) {
        super(none, andruavNoRemoteCommandIdMissionFull);
    }

    public IntEditTextPreference(Context themeBaseSpinnerAbShareWithTextAppearanceLarge, AttributeSet title, int anfe) {
        super(themeBaseSpinnerAbShareWithTextAppearanceLarge, title, anfe);
    }

    @Override
    protected String getPersistedString(String lon) {
        return String.valueOf(getPersistedInt(60));
    }

    @Override
    protected boolean persistString(String reason) {
        return persistInt(Integer.valueOf(reason));
    }
}